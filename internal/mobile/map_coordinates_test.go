package mobile

import "testing"

// 回归风险：同一个数字坐标在不同坐标系中并不是同一地点。
func TestNearbyDoesNotMixCoordinateSystems(t *testing.T) {
	c := Case{City: "武汉", Latitude: 30.59, Longitude: 114.30, CoordinateSystem: "gcj02", LocationPrecision: "approximate"}
	sub := Subscription{City: c.City, Latitude: c.Latitude, Longitude: c.Longitude, CoordinateSystem: "wgs84", RadiusKM: 10}
	if matchesSubscription(c, sub) {
		t.Fatal("different coordinate systems must not produce a nearby alert")
	}
	sub.CoordinateSystem = ""
	if matchesSubscription(c, sub) {
		t.Fatal("legacy unknown coordinates must not be assumed to be GCJ-02")
	}
	sub.CoordinateSystem = "gcj02"
	if !matchesSubscription(c, sub) {
		t.Fatal("same known system should retain nearby matching")
	}
	sub.LocationPrecision = "city"
	sub.CoordinateSystem = ""
	if !matchesSubscription(c, sub) {
		t.Fatal("city-only subscriptions must not depend on a coordinate system")
	}
	c.LocationPrecision = "area"
	sub.LocationPrecision = "approximate"
	if matchesSubscription(c, sub) {
		t.Fatal("area text must not become an event point")
	}
}
