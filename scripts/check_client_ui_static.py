"""只读核对公共页面迁移、平台端口和原始资源；不编译、不执行应用或测试。"""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
kmp = root / "kmp"
ui = kmp / "clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui"
assets = kmp / "clientUi/src/commonMain/composeResources"
shared = kmp / "shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client"
android = kmp / "androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile"
ios = kmp / "iosUi/src/iosMain/kotlin/com/ttkk0000/meowcircle/iosui"
desktop = kmp / "desktopApp/src/main/kotlin/com/ttkk0000/meowcircle/desktop"
sources = {p.name: p.read_text() for p in ui.glob("*.kt")}
all_ui = "\n".join(sources.values())
assert 'include(":clientUi")' in (kmp / "settings.gradle.kts").read_text()
for module in ["androidApp", "iosUi", "desktopApp"]:
    assert 'implementation(project(":clientUi"))' in (kmp / module / "build.gradle.kts").read_text(), module
for path in ui.glob("*.kt"):
    text = path.read_text()
    assert not re.search(r"^import (?:android\.|java\.|platform\.|androidx\.activity|androidx\.lifecycle)", text, re.M), path
    assert "https://www.figma.com/api/mcp/asset/" not in text, path
assert "androidResources.enable = true" in (kmp / "clientUi/build.gradle.kts").read_text()
assert "libs.kotlinxSerializationJson" in (kmp / "clientUi/build.gradle.kts").read_text()
assert "ConsumerApp(" in (android / "AndroidMobileApp.kt").read_text()
assert "ConsumerApp(" in (kmp / "iosUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/iosui/IosMobileApp.kt").read_text()
assert "ConsumerApp(" in (desktop / "Main.kt").read_text()
assert "MeowCircleSdk" not in (desktop / "Main.kt").read_text()
assert "fun main()" not in (desktop / "LegacyStitchApp.kt").read_text()
ports = ["Window", "BackHandler", "PhotoPicker", "DateField", "Map", "NotificationPermission", "decodeImage", "openLink"]
for path in [android / "AndroidClientPlatform.kt", ios / "IosClientPlatform.kt", desktop / "DesktopClientPlatform.kt"]:
    text = path.read_text()
    assert "ClientUiPlatform" in text
    for port in ports:
        assert re.search(r"override fun " + port + r"\(", text), (path, port)
assert "androidx.activity.compose.BackHandler(enabled, onBack)" in (android / "AndroidClientPlatform.kt").read_text()
assert "detectHorizontalDragGestures" in (ios / "IosClientPlatform.kt").read_text()
assert "Key.Escape" in (desktop / "DesktopClientPlatform.kt").read_text()
expected = set(re.findall(r"expect fun (\w+)\(", sources["ClientPlatform.kt"]))
for source_set in ["androidMain", "iosMain", "desktopMain"]:
    path = kmp / f"clientUi/src/{source_set}/kotlin/com/ttkk0000/meowcircle/clientui/PlatformText.kt"
    assert set(re.findall(r"actual fun (\w+)\(", path.read_text())) == expected, path
enum = (shared / "MobileFeatureStore.kt").read_text().split("enum class FeaturePage", 1)[1].split("data class FeatureRoute", 1)[0]
pages = set(re.findall(r'\b(\w+)\("', enum))
dispatch = "\n".join(sources[name] for name in ["SocialPages.kt", "CarePages.kt", "OfferPages.kt", "AccountPages.kt"])
assert pages == set(re.findall(r"FeaturePage\.(\w+)\s*->", dispatch))
# 子页面存在还须由公共主入口分派，不能只检查各业务文件的when分支。
for source, handler in [("SocialPages.kt", "SocialFeature"), ("CarePages.kt", "CareFeature"), ("OfferPages.kt", "OfferFeature")]:
    branches = re.findall(r"((?:FeaturePage\.\w+\s*,\s*)*FeaturePage\.\w+)\s*->", sources[source])
    expected_routes = {name for branch in branches for name in re.findall(r"FeaturePage\.(\w+)", branch)}
    root_branches = re.findall(r"((?:FeaturePage\.\w+\s*,\s*)*FeaturePage\.\w+)\s*->\s*" + handler, sources["FeaturePages.kt"])
    actual_routes = {name for branch in root_branches for name in re.findall(r"FeaturePage\.(\w+)", branch)}
    assert expected_routes <= actual_routes, (handler, expected_routes - actual_routes)
assert "val features = MobileFeatureStore(api, scope, localWorkspaces)" in (shared / "MobileStore.kt").read_text()
assert "MobileStore(" not in sources["ConsumerApp.kt"]
ios_client = (ios / "MainViewController.kt").read_text()
assert "ownsApi = false" in ios_client and "localWorkspaces = workspaces" in ios_client and "localReplies = replies" in ios_client
assert 'IosComposeDraftFiles("$name-guest", 1024uL * 1024uL)' in ios_client
vault = (desktop / "DesktopStorage.kt").read_text()
for guard in ["Keyring.create()", '"AES/GCM/NoPadding"', "SecureRandom()", "GCMParameterSpec(128", "cipher.updateAAD(aad)", "ATOMIC_MOVE", "NOFOLLOW_LINKS"]:
    assert guard in vault, guard
assert 'Files.write(file, value' not in vault
for name in ["manrope", "noto_sans_sc"]:
    font = assets / f"font/{name}.ttf"
    assert font.read_bytes() == (kmp / f"androidApp/src/main/res/font/{name}.ttf").read_bytes()
    assert (assets / f"files/fonts/{name}-OFL.txt").is_file()
manifest = json.loads((root / "web-client/public/assets/figma/manifest.json").read_text())
for item in manifest["assets"]:
    path = assets / "files/figma" / Path(item["file"]).name
    raw = path.read_bytes()
    assert item["sha256"] in {hashlib.sha256(raw).hexdigest(), hashlib.sha256(raw.replace(b"\r\n", b"\n")).hexdigest()}, path
catalog = json.loads((root / "internal/mobile/catalog.json").read_text())
for item in catalog["emoji"]:
    assert re.fullmatch(r"/api/v2/expression-assets/v1/emoji_u[a-f0-9_]+\.svg", item["asset_url"])
    name = item["asset_url"].split("/")[-1]
    path = assets / "files/emoji" / name
    assert path.read_bytes().replace(b"\r\n", b"\n") == (root / "internal/platform/mobileapi/assets/v1" / name).read_bytes().replace(b"\r\n", b"\n"), name
    assert ET.parse(path).getroot().attrib.get("viewBox"), path
assert 'EmojiPhoto(single,Modifier.size(72.dp))' in sources["ClientAssets.kt"]
assert "Modifier.size(MobileUi.stickerSize)" in sources["ClientAssets.kt"]
assert (assets / "files/mnd_cat_stickers.png").read_bytes() == (root / "internal/platform/mobileapi/assets/v1/mnd-cat-stickers-atlas-v1.png").read_bytes()
operator = "\n".join(path.read_text() for path in sorted((root / "web-client/src/operator").glob("*.ts*")))
for guard in ['credentials: "omit"', '"X-Operator-Token": this.token', 'epoch !== this.generation', 'cache: "no-store"', 'setPrepared', 'versionOf(latest) !== pending.version']:
    assert guard in operator, guard
assert not re.search(r"\b(localStorage|sessionStorage|indexedDB)\b", operator)
assert "Token" not in (root / "internal/platform/mobileapi/ops_context.go").read_text().split("type OperatorContext struct", 1)[1]
trace = json.loads((root / "docs/client/figma-implementation-trace-2026-10-24.json").read_text())
assert trace["counts"]["baseline_states"] == 740 and trace["counts"]["incremental_states"] == 8
assert len(trace["states"]) == trace["counts"]["states"] == trace["counts"]["baseline_states"] + trace["counts"]["incremental_states"]
assert trace["counts"]["runtime_verified"] == 0 and all(row["runtime_verified"] is False for row in trace["states"])
for row in trace["states"]:
    for key in ["android_source", "ios_source", "desktop_source", "web_source"]:
        if row[key]: assert (root / row[key]).is_file(), (row["key"], key)
print(f"公共UI只读核对：{len(sources)}份公共源码、{len(pages)}个功能分派、3套平台端口、{len(catalog['emoji'])}枚Noto原图、{len(manifest['assets'])}枚原SVG、{len(trace['states'])}条源码索引（740基线＋8协作增量）；未编译／类型检查／测试／逐屏运行。")
