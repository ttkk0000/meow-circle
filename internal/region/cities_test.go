package region

import "testing"

// Source-only regressions; execution requires separate authorization.
func TestMainlandCityAliasesAndBoundaries(t *testing.T) {
	for _, pair := range [][2]string{{"武汉", "武汉市"}, {"北京", "110000"}, {"石河子市", "659001"}, {"延吉", "延吉市"}} {
		if !Same(pair[0], pair[1]) {
			t.Errorf("alias mismatch: %v", pair)
		}
	}
	for _, pair := range [][2]string{{"苏州", "宿州"}, {"吉林", "吉林省"}, {"恩施市", "恩施土家族苗族自治州"}, {"上海", "武汉"}} {
		if Same(pair[0], pair[1]) {
			t.Errorf("different cities merged: %v", pair)
		}
	}
	for _, value := range []string{"香港", "澳门", "台北", "全国", "湖北省", "不存在的城市"} {
		if _, ok := Lookup(value); ok {
			t.Errorf("unexpected selectable region: %s", value)
		}
	}
}
