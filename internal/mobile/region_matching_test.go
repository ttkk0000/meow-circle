package mobile

import "testing"

// Source-only privacy regression cases. Tests have not been executed.
func TestTextRegionNeverBecomesAnExactNearbyPoint(t *testing.T) {
	c := Case{City: "武汉", Area: "武昌", LocationPrecision: "area"}
	city := Subscription{City: "武汉", LocationPrecision: "city", Enabled: true}
	if !matchesSubscription(c, city) {
		t.Fatal("city subscription missed an area-only case")
	}
	other := city
	other.City = "上海"
	if matchesSubscription(c, other) {
		t.Fatal("city subscription leaked into another city")
	}
	point := Subscription{City: "武汉", Latitude: 0, Longitude: 0, RadiusKM: 100}
	if matchesSubscription(c, point) {
		t.Fatal("area-only placeholder was treated as a point at 0/0")
	}
	known := Case{City: "武汉", Latitude: 30.5, Longitude: 114.3, LocationPrecision: "approximate"}
	if !matchesSubscription(known, city) {
		t.Fatal("city subscription excluded a real coarse-point case")
	}
}

func TestCityAliasesShareOnlyTheSameSubscriptionScope(t *testing.T) {
	c := Case{City: "成都市", LocationPrecision: "area"}
	if !matchesSubscription(c, Subscription{City: "成都", LocationPrecision: "city"}) {
		t.Fatal("official city name missed legacy short name")
	}
	if matchesSubscription(c, Subscription{City: "重庆", LocationPrecision: "city"}) {
		t.Fatal("city scope broadened")
	}
	c.City = "恩施市"
	if matchesSubscription(c, Subscription{City: "恩施土家族苗族自治州", LocationPrecision: "city"}) {
		t.Fatal("county city and prefecture were merged")
	}
}
