package mediablob

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/xml"
	"io"
	"net/http"
	"net/url"
	"os"
	"regexp"
	"strings"
	"time"
)

// S3采用HTTPS路径式私有桶。签名只在服务端使用，客户端仍经原有鉴权媒体路由读取。
type S3 struct {
	endpoint                                        *url.URL
	bucket, prefix, region, access, secret, session string
	client                                          *http.Client
}

func S3FromEnvironment() (*S3, error) {
	u, err := url.Parse(os.Getenv("MND_S3_ENDPOINT"))
	if err != nil || u.Scheme != "https" || u.Hostname() == "" || u.User != nil || u.RawQuery != "" || u.Fragment != "" || u.Path != "" && u.Path != "/" {
		return nil, ErrUnavailable
	}
	s := &S3{endpoint: u, bucket: os.Getenv("MND_S3_BUCKET"), prefix: os.Getenv("MND_S3_PREFIX"), region: os.Getenv("MND_S3_REGION"), access: os.Getenv("MND_S3_ACCESS_KEY"), secret: os.Getenv("MND_S3_SECRET_KEY"), session: os.Getenv("MND_S3_SESSION_TOKEN")}
	if !regexp.MustCompile(`^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$`).MatchString(s.bucket) ||
		!regexp.MustCompile(`^[a-z0-9-]{1,80}$`).MatchString(s.region) ||
		!regexp.MustCompile(`^(?:[A-Za-z0-9_-]+/){1,8}$`).MatchString(s.prefix) || len(s.prefix) > 120 ||
		!regexp.MustCompile(`^[A-Za-z0-9_-]{8,200}$`).MatchString(s.access) || len(s.secret) < 16 || len(s.secret) > 4096 || strings.ContainsAny(s.session, "\r\n") {
		return nil, ErrUnavailable
	}
	s.client = &http.Client{Timeout: 25 * time.Second, CheckRedirect: func(*http.Request, []*http.Request) error { return ErrUnavailable }}
	return s, nil
}
func hash(data []byte) string { sum := sha256.Sum256(data); return hex.EncodeToString(sum[:]) }
func mac(key []byte, value string) []byte {
	h := hmac.New(sha256.New, key)
	h.Write([]byte(value))
	return h.Sum(nil)
}
func (s *S3) request(ctx context.Context, method, name string, query url.Values, data []byte) (*http.Response, error) {
	if name != "" && !Managed.MatchString(name) {
		return nil, ErrUnavailable
	}
	u := *s.endpoint
	u.Path = "/" + s.bucket
	if name != "" {
		u.Path += "/" + s.prefix + name
	}
	u.RawPath = ""
	u.RawQuery = strings.ReplaceAll(query.Encode(), "+", "%20")
	req, err := http.NewRequestWithContext(ctx, method, u.String(), bytes.NewReader(data))
	if err != nil {
		return nil, ErrUnavailable
	}
	at := time.Now().UTC()
	stamp, day, payload := at.Format("20060102T150405Z"), at.Format("20060102"), hash(data)
	req.Header.Set("X-Amz-Date", stamp)
	req.Header.Set("X-Amz-Content-Sha256", payload)
	headers := "host:" + u.Host + "\nx-amz-content-sha256:" + payload + "\nx-amz-date:" + stamp + "\n"
	signed := "host;x-amz-content-sha256;x-amz-date"
	if s.session != "" {
		req.Header.Set("X-Amz-Security-Token", s.session)
		headers += "x-amz-security-token:" + s.session + "\n"
		signed += ";x-amz-security-token"
	}
	canonical := method + "\n" + u.EscapedPath() + "\n" + u.RawQuery + "\n" + headers + "\n" + signed + "\n" + payload
	scope := day + "/" + s.region + "/s3/aws4_request"
	key := mac(mac(mac(mac([]byte("AWS4"+s.secret), day), s.region), "s3"), "aws4_request")
	signature := hex.EncodeToString(mac(key, "AWS4-HMAC-SHA256\n"+stamp+"\n"+scope+"\n"+hash([]byte(canonical))))
	req.Header.Set("Authorization", "AWS4-HMAC-SHA256 Credential="+s.access+"/"+scope+",SignedHeaders="+signed+",Signature="+signature)
	req.Header.Set("Content-Type", "application/octet-stream")
	response, err := s.client.Do(req)
	if err != nil {
		return nil, ErrUnavailable
	}
	return response, nil
}
func (s *S3) unversioned(ctx context.Context) error {
	response, err := s.request(ctx, http.MethodGet, "", url.Values{"versioning": []string{""}}, nil)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	if response.StatusCode != 200 {
		return ErrUnavailable
	}
	var setting struct {
		XMLName xml.Name `xml:"VersioningConfiguration"`
		Status  string   `xml:"Status"`
	}
	data, err := io.ReadAll(io.LimitReader(response.Body, 8193))
	if err != nil || len(data) > 8192 || xml.Unmarshal(data, &setting) != nil || setting.Status != "" {
		return ErrUnavailable
	}
	return nil
}
func (s *S3) privateBucket(ctx context.Context) error {
	response, err := s.request(ctx, http.MethodGet, "", url.Values{"publicAccessBlock": []string{""}}, nil)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	if response.StatusCode != 200 {
		return ErrUnavailable
	}
	var setting struct {
		XMLName               xml.Name `xml:"PublicAccessBlockConfiguration"`
		BlockPublicAcls       bool     `xml:"BlockPublicAcls"`
		IgnorePublicAcls      bool     `xml:"IgnorePublicAcls"`
		BlockPublicPolicy     bool     `xml:"BlockPublicPolicy"`
		RestrictPublicBuckets bool     `xml:"RestrictPublicBuckets"`
	}
	data, err := io.ReadAll(io.LimitReader(response.Body, 8193))
	if err != nil || len(data) > 8192 || xml.Unmarshal(data, &setting) != nil || !setting.BlockPublicAcls || !setting.IgnorePublicAcls || !setting.BlockPublicPolicy || !setting.RestrictPublicBuckets {
		return ErrUnavailable
	}
	return nil
}
func (s *S3) Put(ctx context.Context, name string, data []byte) error {
	if len(data) == 0 || len(data) > MaxBytes {
		return ErrUnavailable
	}
	if err := s.privateBucket(ctx); err != nil {
		return err
	}
	if err := s.unversioned(ctx); err != nil {
		return err
	}
	response, err := s.request(ctx, http.MethodPut, name, nil, data)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	if response.StatusCode < 200 || response.StatusCode >= 300 || response.Header.Get("X-Amz-Version-Id") != "" {
		return ErrUnavailable
	}
	return nil
}
func (s *S3) Read(ctx context.Context, name string) ([]byte, time.Time, error) {
	if err := s.privateBucket(ctx); err != nil {
		return nil, time.Time{}, err
	}
	response, err := s.request(ctx, http.MethodGet, name, nil, nil)
	if err != nil {
		return nil, time.Time{}, err
	}
	defer response.Body.Close()
	if response.StatusCode == 404 {
		return nil, time.Time{}, ErrNotFound
	}
	if response.StatusCode != 200 || response.ContentLength > MaxBytes {
		return nil, time.Time{}, ErrUnavailable
	}
	data, err := io.ReadAll(io.LimitReader(response.Body, MaxBytes+1))
	if err != nil || len(data) > MaxBytes {
		return nil, time.Time{}, ErrUnavailable
	}
	modified, err := http.ParseTime(response.Header.Get("Last-Modified"))
	if err != nil {
		return nil, time.Time{}, ErrUnavailable
	}
	return data, modified, nil
}
func (s *S3) Remove(ctx context.Context, name string) error {
	if err := s.unversioned(ctx); err != nil {
		return err
	}
	response, err := s.request(ctx, http.MethodDelete, name, nil, nil)
	if err != nil {
		return err
	}
	defer response.Body.Close()
	// Delete marker不是物理清除，不允许记成已删除。
	if response.Header.Get("X-Amz-Delete-Marker") == "true" || response.Header.Get("X-Amz-Version-Id") != "" {
		return ErrUnavailable
	}
	if response.StatusCode == 404 {
		return nil
	}
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return ErrUnavailable
	}
	return nil
}
func (s *S3) List(ctx context.Context, after string) ([]Object, string, error) {
	query := url.Values{"list-type": []string{"2"}, "max-keys": []string{"100"}, "prefix": []string{s.prefix + "mnd-"}}
	if after != "" {
		if !Managed.MatchString(after) {
			return nil, "", ErrUnavailable
		}
		query.Set("start-after", s.prefix+after)
	}
	response, err := s.request(ctx, http.MethodGet, "", query, nil)
	if err != nil {
		return nil, "", err
	}
	defer response.Body.Close()
	if response.StatusCode != 200 {
		return nil, "", ErrUnavailable
	}
	var page struct {
		XMLName   xml.Name `xml:"ListBucketResult"`
		Truncated bool     `xml:"IsTruncated"`
		Contents  []struct {
			Key      string    `xml:"Key"`
			Size     int64     `xml:"Size"`
			Modified time.Time `xml:"LastModified"`
		} `xml:"Contents"`
	}
	data, err := io.ReadAll(io.LimitReader(response.Body, 256*1024+1))
	if err != nil || len(data) > 256*1024 || xml.Unmarshal(data, &page) != nil || len(page.Contents) > 100 {
		return nil, "", ErrUnavailable
	}
	objects := []Object{}
	last := after
	for _, item := range page.Contents {
		name := strings.TrimPrefix(item.Key, s.prefix)
		if !strings.HasPrefix(item.Key, s.prefix) || !Managed.MatchString(name) || name <= last || item.Modified.IsZero() || item.Size < 0 {
			return nil, "", ErrUnavailable
		}
		objects = append(objects, Object{name, item.Modified, item.Size})
		last = name
	}
	next := ""
	if page.Truncated {
		if last == after {
			return nil, "", ErrUnavailable
		}
		next = last
	}
	return objects, next, nil
}
