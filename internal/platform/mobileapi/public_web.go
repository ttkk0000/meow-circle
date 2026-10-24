package mobileapi

import (
	"bytes"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"html/template"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"

	"kitty-circle/internal/mobile"
)

// The fixed stylesheet follows the approved Figma V2 default light tokens.
// Fonts fall back locally; a public page never contacts a third-party font host.
const publicShareCSS = `:root{color-scheme:light;--brand:#C84B3D;--on-brand:#FFFEFC;--canvas:#F7F8FA;--surface:#FFFEFC;--ink:#202329;--muted:#616771;--line:#E1E4E8}*{box-sizing:border-box}body{margin:0;background:var(--canvas);color:var(--ink);font:400 16px/1.65 "Noto Sans SC",system-ui,sans-serif}a{color:var(--brand);text-underline-offset:.2em}a:focus-visible{outline:3px solid var(--brand);outline-offset:4px}header,main,footer{width:min(100% - 32px,720px);margin-inline:auto}header{display:flex;align-items:baseline;gap:12px;padding-block:24px}header strong{font:700 24px/1.4 Manrope,system-ui,sans-serif}header span,footer,p.meta{color:var(--muted);font-size:14px}article{overflow:hidden;background:var(--surface);border:1px solid var(--line);border-radius:16px}article img{display:block;max-width:100%;width:100%;height:auto;max-height:440px;object-fit:contain;background:var(--canvas)}.copy{padding:24px}h1{margin:8px 0 16px;font-size:clamp(24px,4vw,32px);line-height:1.4;overflow-wrap:anywhere}.summary{white-space:pre-wrap;overflow-wrap:anywhere;margin:0 0 24px}.actions{display:flex;gap:12px;flex-wrap:wrap}.button{display:inline-flex;min-height:48px;align-items:center;justify-content:center;padding:10px 20px;border:1px solid var(--brand);border-radius:16px;text-decoration:none;font-weight:600}.primary{background:var(--brand);color:var(--on-brand)}footer{padding-block:20px 32px}footer p{margin:6px 0}@media(max-width:420px){.copy{padding:20px}.actions{flex-direction:column}}`

var publicShareTemplate = template.Must(template.New("public-share").Parse(`<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{{.Title}} · M&amp;D</title><meta name="description" content="{{.Summary}}">
<meta property="og:type" content="article"><meta property="og:site_name" content="M&amp;D · meow &amp; doggie">
<meta property="og:title" content="{{.Title}}"><meta property="og:description" content="{{.Summary}}">
{{if .PageURL}}<meta property="og:url" content="{{.PageURL}}"><link rel="canonical" href="{{.PageURL}}">{{end}}
{{if .ImageURL}}<meta property="og:image" content="{{.ImageURL}}">{{end}}
<style>` + publicShareCSS + `</style></head>
<body><header><strong>M&amp;D</strong><span>meow &amp; doggie</span></header><main>
<article>{{if .ImageURL}}<img src="{{.ImageURL}}" alt="分享内容的公开图片" loading="lazy" decoding="async">{{end}}
<div class="copy"><p class="meta">{{.Label}}</p><h1>{{.Title}}</h1><p class="summary">{{.Summary}}</p>
{{if .Origin}}<p class="meta">{{.Origin}}</p>{{end}}
<nav class="actions" aria-label="下一步">{{if .AppURL}}<a class="button primary" href="{{.AppURL}}">在 App 中查看</a>{{end}}{{if .WebURL}}<a class="button" href="{{.WebURL}}">在浏览器继续查看</a>{{end}}<a class="button" href="/safety.html">安全求助与权利反馈</a></nav>
</div></article></main><footer><p>无需安装 App，即可阅读本页公开摘要并提交安全求助。</p>
{{if .AppURL}}<p>App 按钮需要设备已安装 M&amp;D；未安装时可继续使用本页。</p>{{end}}
<p>内容的可见范围和状态可能发生变化，请以当前页面为准。</p></footer></body></html>`))

type publicShareView struct {
	Title, Summary, Label, Origin, PageURL, ImageURL, WebURL string
	AppURL                                                   template.URL
}

func canonicalShareID(raw string) (int64, bool) {
	id, e := strconv.ParseInt(raw, 10, 64)
	return id, e == nil && id > 0 && strconv.FormatInt(id, 10) == raw
}

func publicSharePath(path string) (mobile.Kind, int64, bool) {
	if !strings.HasPrefix(path, "/share/") {
		return "", 0, false
	}
	parts := strings.Split(strings.TrimPrefix(path, "/share/"), "/")
	if len(parts) != 2 {
		return "", 0, false
	}
	kind := mobile.Kind(parts[0])
	if !mobile.HasKind([]mobile.Kind{mobile.Entries, mobile.Pets, mobile.Cases, mobile.Offers}, kind) {
		return "", 0, false
	}
	id, ok := canonicalShareID(parts[1])
	return kind, id, ok
}

func publicShareImage(path string) string {
	const prefix, suffix = "/api/v2/media/", "/content"
	if !strings.HasPrefix(path, prefix) || !strings.HasSuffix(path, suffix) {
		return ""
	}
	id := strings.TrimSuffix(strings.TrimPrefix(path, prefix), suffix)
	if _, ok := canonicalShareID(id); !ok {
		return ""
	}
	return path
}

// PublicSharePage uses the same guest ACL and safe projection as the JSON API.
// Neither Host nor forwarded headers can become metadata, links or image URLs.
func (h *Handler) PublicSharePage(w http.ResponseWriter, r *http.Request) {
	cssHash := sha256.Sum256([]byte(publicShareCSS))
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "private, no-store, max-age=0")
	w.Header().Set("Pragma", "no-cache")
	w.Header().Set("Referrer-Policy", "no-referrer")
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("X-Frame-Options", "DENY")
	w.Header().Set("X-Robots-Tag", "noindex, noarchive")
	w.Header().Set("Content-Security-Policy", "default-src 'none'; img-src 'self'; style-src 'sha256-"+base64.StdEncoding.EncodeToString(cssHash[:])+"'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'")
	view := publicShareView{Title: "内容暂不可用", Summary: "该内容可能已调整可见范围、撤回或失效。你仍可通过下方入口提交安全求助或权利反馈。", Label: "公开分享"}
	status := http.StatusNotFound
	kind, id, valid := publicSharePath(r.URL.Path)
	if r.Method != http.MethodGet && r.Method != http.MethodHead {
		w.Header().Set("Allow", "GET, HEAD")
		status = http.StatusMethodNotAllowed
	} else if valid && h.Service != nil {
		card, e := h.Service.PublicShare(r.Context(), kind, id)
		if e == nil {
			status = http.StatusOK
			idText := strconv.FormatInt(id, 10)
			view.Title, view.Summary = card.Title, card.Summary
			view.Label = map[mobile.Kind]string{mobile.Entries: "社区公开分享", mobile.Pets: "宠物公开档案", mobile.Cases: "寻宠公开信息", mobile.Offers: "宠物公开披露"}[kind]
			view.PageURL = "/share/" + string(kind) + "/" + idText
			view.ImageURL = publicShareImage(card.ImageURL)
			view.WebURL = "/app/" + string(kind) + "/" + idText
			if origin, e := url.Parse(os.Getenv("MND_WEB_ORIGIN")); e == nil && origin.Scheme == "https" && origin.Host != "" && origin.User == nil && origin.Path == "" && origin.RawQuery == "" && origin.Fragment == "" {
				view.PageURL = origin.String() + view.PageURL
				if view.ImageURL != "" {
					view.ImageURL = origin.String() + view.ImageURL
				}
			}
			// Both components came from the fixed whitelist and canonical int64
			// parser. No untrusted URL is promoted to template.URL.
			view.AppURL = template.URL("mnd://" + string(kind) + "/" + idText)
			switch card.ContentOrigin {
			case "ai_generated":
				view.Origin = "发布者声明：含生成内容"
			case "ai_modified":
				view.Origin = "发布者声明：含辅助编辑内容"
			case "unknown":
				view.Origin = "发布者未明确声明内容来源"
			}
		} else if !errors.Is(e, mobile.ErrNotFound) && !errors.Is(e, mobile.ErrForbidden) && !errors.Is(e, mobile.ErrUnauthorized) {
			status = http.StatusServiceUnavailable
		}
	} else if valid {
		status = http.StatusServiceUnavailable
	}
	var page bytes.Buffer
	if e := publicShareTemplate.Execute(&page, view); e != nil {
		http.Error(w, "页面暂不可用，请稍后重试", http.StatusInternalServerError)
		return
	}
	w.WriteHeader(status)
	if r.Method != http.MethodHead {
		_, _ = w.Write(page.Bytes())
	}
}
