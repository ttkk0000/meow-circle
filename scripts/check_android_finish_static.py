"""Read-only integration checks. No builds, test execution, browser or HTTP requests."""
from html.parser import HTMLParser
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
android = root / "kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile"
client_ui = root / "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui"
shared = root / "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client"

class Page(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids, self.refs, self.labels, self.csp = [], [], [], ""

    def handle_starttag(self, tag, values):
        attrs = dict(values)
        if "id" in attrs:
            self.ids.append(attrs["id"])
        if tag in ("script", "link"):
            self.refs.append(attrs.get("src", attrs.get("href", "")))
        if tag == "label" and "for" in attrs:
            self.labels.append(attrs["for"])
        if tag == "meta" and attrs.get("http-equiv") == "Content-Security-Policy":
            self.csp = attrs["content"]

page = Page()
page.feed((root / "web/safety.html").read_text(encoding="utf-8"))
js = (root / "web/safety.js").read_text(encoding="utf-8")
assert len(page.ids) == len(set(page.ids)), "duplicate DOM IDs"
assert set(page.labels) <= set(page.ids), "unbound form labels"
assert set(re.findall(r"\$\('([^']+)'\)", js)) <= set(page.ids), "missing JS control"
assert page.refs and all(ref.startswith("/") and not ref.startswith("//") for ref in page.refs)
assert all((root / "web" / ref.lstrip("/")).is_file() for ref in page.refs)
assert "default-src 'none'" in page.csp and "connect-src 'self'" in page.csp
assert "credentials: 'omit'" in js and "referrerPolicy: 'no-referrer'" in js
assert "localStorage" not in js.split("*/", 1)[1] and "innerHTML" not in js

schema = json.loads((root / "docs/api/mobile-v2.openapi.json").read_text(encoding="utf-8"))
for path in ("/api/v2/inbox/read-all/snapshot", "/api/v2/inbox/read-all"):
    assert "post" in schema["paths"][path] and "get" not in schema["paths"][path]
for name, fields in {"InboxReadSnapshot": ("snapshot_id", "version"), "InboxReadAllInput": ("snapshot_id", "version"), "RightsReceipt": ("id", "version")}.items():
    for field in fields:
        assert schema["components"]["schemas"][name]["properties"][field]["format"] == "int64"
for name in ("Case", "Subscription"):
    assert "location_precision" in schema["components"]["schemas"][name]["properties"]

ui = (client_ui / "ConsumerApp.kt").read_text(encoding="utf-8")
assert ui.count("InboxReadAllDialog(store,state)") == 1, "read-all dialog must have one owner"
assert "restartInboxReadAll" in (client_ui / "InboxReadAll.kt").read_text(encoding="utf-8")
care = (client_ui / "CarePages.kt").read_text(encoding="utf-8")
assert "requireAppointmentContext(request.contextKind,request.contextId,request.peerId)" in care
assert 'FormField(f,s,"latitude"' not in care and 'FormField(f,s,"longitude"' not in care
assert "requireCurrentAudience" in (shared / "MobileStore.kt").read_text(encoding="utf-8")
assert "uncertainReplay" in (shared / "PolicyModels.kt").read_text(encoding="utf-8")

ns = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(root / "kmp/androidApp/src/main/AndroidManifest.xml").getroot()
assert any(data.get(ns + "scheme") == "https" and data.get(ns + "host") == "${appLinkHost}" and data.get(ns + "pathPrefix") == "/share/"
           for data in manifest.findall("application/activity/intent-filter/data"))
assert 'HandleFunc("/share/", r.mobileAPI.PublicSharePage)' in (root / "internal/platform/api/router.go").read_text(encoding="utf-8")
print(f"Static integration passed: {len(page.ids)} guest page controls, local assets, int64 contracts, Android single-owner/selection and share wiring. No runtime or browser validation.")
