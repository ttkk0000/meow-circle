"""Read source/configuration only. No Kotlin/Gradle, tests, dependency resolution, network or device calls."""
from pathlib import Path
import json
import re
import tomllib
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
kmp = root / "kmp"
catalog = tomllib.loads((kmp / "gradle/libs.versions.toml").read_text(encoding="utf-8"))
for group in ("libraries", "plugins"):
    for name, definition in catalog[group].items():
        version = definition.get("version")
        if isinstance(version, dict):
            assert version["ref"] in catalog["versions"], name
for file in kmp.glob("**/build.gradle.kts"):
    if "build" in file.relative_to(kmp).parts[:-1]:
        continue
    source = file.read_text(encoding="utf-8")
    for kind, name in re.findall(r"libs\.(plugins\.)?(\w+)", source):
        assert name in catalog["plugins" if kind else "libraries"], (file, name)

shared = kmp / "shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client"
android = kmp / "androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile"
ios = kmp / "iosUi"
files = list(shared.glob("*.kt")) + list(android.glob("*.kt")) + list(ios.rglob("*.kt"))
files += list((kmp / "shared/src/commonTest").rglob("*.kt")) + list((kmp / "iosApp/ComposeApp").glob("*.swift"))

def delimiters(source, file):
    # Lexical delimiter check only. It does not claim Kotlin/Swift parsing, overload resolution or compilation.
    source = re.sub(r'/\*.*?\*/|//[^\n]*|""".*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', "", source, flags=re.S)
    stack = []
    for char in source:
        if char in "([{": stack.append(char)
        elif char in ")]}":
            assert stack and stack.pop() == {")": "(", "]": "[", "}": "{"}[char], file
    assert not stack, file

for file in files:
    assert not file.read_bytes().startswith(b"\xef\xbb\xbf"), file
    text = file.read_text(encoding="utf-8")
    delimiters(text, file)
    assert "/api/v1" not in text, file
    if file.parent == shared:
        assert not re.search(r"import (?:android\.|androidx\.compose|platform\.UIKit)", text), file

manifest = ET.parse(kmp / "androidApp/src/main/AndroidManifest.xml").getroot()
ns = "{http://schemas.android.com/apk/res/android}"
activity = manifest.find("application/activity")
assert activity is not None and ns + "screenOrientation" not in activity.attrib
assert 'minSdk = 26' in (kmp / "androidApp/build.gradle.kts").read_text(encoding="utf-8")
assert 'targetSdk = 37' in (kmp / "androidApp/build.gradle.kts").read_text(encoding="utf-8")
assert 'compileSdk = 37' in (kmp / "shared/build.gradle.kts").read_text(encoding="utf-8")
assert 'androidTarget' not in (kmp / "shared/build.gradle.kts").read_text(encoding="utf-8")
assert 'androidKmpLibrary' in (kmp / "shared/build.gradle.kts").read_text(encoding="utf-8")
assert 'include(":iosUi")' in (kmp / "settings.gradle.kts").read_text(encoding="utf-8")
project = (kmp / "iosApp/project.yml").read_text(encoding="utf-8")
assert "- path: ComposeApp" in project and "- path: MndIOS\n" not in project
for expected in ("MndMobileUI.framework", ":iosUi:embedAndSignAppleFrameworkForXcode", "UIRequiresFullScreen: false", "CADisableMinimumFrameDurationOnPhone: true", "UISupportedInterfaceOrientations~ipad"):
    assert expected in project, expected

# Request fields are compared with the checked-in server contract, independently of UI implementation.
schemas = json.loads((root / "docs/api/mobile-v2.openapi.json").read_text(encoding="utf-8"))["components"]["schemas"]
expected_fields = {
    "PasswordInput": {"username", "phone", "email", "password", "device"},
    "CommentInput": {"content", "reply_to", "idempotency_key"},
    "MessageInput": {"content", "media_ids", "idempotency_key"},
    "EntryInput": {"entry", "version", "media_ids", "client_request_id", "declaration"},
    "PetInput": {"pet", "version", "media_ids", "client_request_id"},
    "AdoptionInput": {"pet_id", "message", "client_request_id", "adoption_cycle"},
}
for name, fields in expected_fields.items():
    assert fields == set(schemas[name]["properties"]), (name, schemas[name])
store = (shared / "MobileStore.kt").read_text(encoding="utf-8")
assert "requireInteractionContract" in store and "qualification.interaction" in store and "api.eligibility()" in store
assert "B04 is absent" not in store
assert 'api.login(form.phone' not in store, "Legacy phone auto-signup re-exposed"
print(f"Static source/configuration checks passed: {len(files)} new Kotlin/Swift files, catalog aliases, API 26/37, iOS Compose wiring, request field contracts. No compilation/tests/device execution.")
