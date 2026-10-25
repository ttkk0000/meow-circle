"""只读核对官方授权源码边界；不执行SDK、应用、类型检查或测试。"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
shared = root / "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client"
ui = root / "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui"
android = root / "kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp"
web = root / "web-client/src"

def contains(path, *guards):
    text = (root / path).read_text()
    for guard in guards:
        assert guard in text, (path, guard)
    return text

store = (shared / "ProviderAuthStore.kt").read_text()
state = store.split("data class ProviderAuthState",1)[1].split("enum class ProviderAuthIntent",1)[0]
assert not re.search(r"resolution|credential|grant|nonce|password|token",state,re.I)
for guard in ["private var resolution", "private var grant", "private var challenge", "if (!acceptedTerms", "if (!confirmed", "ProviderAuthStage.ResultUnknown", "ProviderAuthStage.CreateReview", "api.providerLinkScope(token)", "if (!expectingLogin) clear()"]:
    assert guard in store, guard
stages = set(re.findall(r"\b(\w+)\b",store.split("enum class ProviderAuthStage {",1)[1].split("}",1)[0]))
assert stages <= set(re.findall(r"ProviderAuthStage\.(\w+)", (ui / "ProviderAuthPages.kt").read_text()))
for file in ["AuthPages.kt","AccountPages.kt"]:
    assert "ProviderEntry(store," in (ui / file).read_text(),file
contains("kmp/androidApp/build.gradle.kts","wechat-sdk-android:6.8.40","open_sdk_3.5.19_r9483ffc7_lite.jar","6272d4123d8327400a655c48f1fb7eb24b83fd5beeb527951ed100b219192b9b","check(jar == null)")
native = (android / "mobile/AndroidProviderAuth.kt").read_text()
for guard in ["Tencent.setIsPermissionGranted(true", "loginServerSide", "pending[challenge.state]", "requestState", "it.provider != \"qq\" || it.listener == null", "WeakReference<Activity>"]:
    assert guard in native,guard
assert "AndroidProviderAuth.complete(\"wechat\",auth.state" in (android / "wxapi/WXEntryActivity.kt").read_text()
manifest = ET.parse(root / "kmp/androidApp/src/main/AndroidManifest.xml").getroot()
ns = "{http://schemas.android.com/apk/res/android}"
activities = {n.attrib[ns+"name"]: n for n in manifest.findall("application/activity")}
assert activities[".mobile.OfficialProviderActivity"].attrib[ns+"exported"] == "false"
assert activities[".wxapi.WXEntryActivity"].attrib[ns+"exported"] == "true"
contains("kmp/iosApp/Podfile","WechatOpenSDK-XCFramework', '2.0.9'",":podspec => './TencentOpenAPI.podspec'")
contains("kmp/iosApp/TencentOpenAPI.podspec","3.6.20","77c4c9e4b0f064c5a304e00d036280f6cf47d43ae0fc24ab99785d4659ce6198","s.vendored_frameworks")
contains("kmp/iosApp/ComposeApp/NativeProviderAuth.swift","credential.state == state","credential.authorizationCode",'completion("success", code, token)',"request.nonce = nonce","pending?.state == state",'pending?.provider == "qq"',"pending?.callback = nil")
contains("kmp/iosApp/ComposeApp/MndProviderBridge.m","setIsUserAgreedAuthorization:YES","kAuthModeServerSideCode","getServerSideCode")
popup = contains("web-client/src/core/provider.ts","redirect.origin !== location.origin",'redirect.protocol !== "https:"',"event.origin !== location.origin","value.state !== state",'new BroadcastChannel(`mnd-provider-${state}`)',"history.replaceState",'"response_type", "code"')
flow = contains("web-client/src/features/ProviderSignIn.tsx","popup.opener = null","attempt !== generation.current",'setStage("verifying")','"link-ready"',"重新核对本次关联用途", "actions={")
for path in ["features/AuthDialog.tsx","features/Account.tsx"]:
    assert "<ProviderSignIn" in (web / path).read_text()
assert not re.search(r"\b(?:localStorage|sessionStorage|indexedDB)\b", popup + flow + native)
contains("internal/platform/mobileapi/browser.go","h.webProviderBody(w, r, path)","h.takeProviderLogin(w, response)",'path != "auth/oauth-resolve"')
contains("internal/platform/mobileapi/browser_provider.go",'credential["client"]',"h.webProviderID(\"wechat\")","h.takeLogin(w, response)", 'result["login"] = sanitized["data"]')
contains("internal/platform/mobileauth/tencent_apps.go",'credential.Client != "web"','u.Path != "/app/provider-callback"',"previous.client.Secret != secret")
contains("internal/identity/service.go",'Code: "identity_in_use"',"if actor > 0 && linked.OwnerID != actor",'if actor <= 0 || (r.OwnerID > 0 && r.OwnerID != actor)',"mobile.ConsumePurpose")
# 公众号回调是独立的服务器协议，不得混用App登录或将消息作为账号关联证明。
official = root / "internal/platform/wechatofficial"
production = [file for file in official.glob("*.go") if not file.name.endswith("_test.go")]
assert {p.name for p in production} == {"config.go", "handler.go", "protocol.go"}
for file in production:
    text = file.read_text()
    assert '"kitty-circle/' not in text, (file, "公众号回调不能依赖身份／业务存储")
    assert not re.search(r'\b(?:log|fmt)\.(?:Print|Fatal|Panic)', text), file
config = contains("internal/platform/wechatofficial/config.go", 'const CallbackPath = "/integrations/wechat/official/callback"', 'if !config.Enabled', 'enabled == "1"')
names = set(re.findall(r'os.Getenv\("(MND_WECHAT_OFFICIAL_[A-Z_]+)"\)', config))
assert len(names) == 5
runtime = (root / "deploy/runtime.example.env").read_text()
environment = (root / "docs/server/environment.example").read_text()
for name in names:
    assert name + "=" in environment, name
    assert name + "=" in runtime or name + "_FILE=" in runtime, name
    for directory in [root / "web-client/src", root / "kmp"]:
        for file in directory.rglob("*"):
            if file.is_file() and file.suffix in {".kt", ".swift", ".ts", ".tsx"}:
                assert name not in file.read_text(), (file, "公众号凭据不能进入客户端")
assert 'MND_WECHAT_OFFICIAL_ENABLED=0' in runtime and 'MND_WECHAT_OFFICIAL_ENABLED=0' in environment
contains("internal/platform/api/router.go", 'wechatofficial.FromEnvironment()', 'r.mux.Handle(wechatofficial.CallbackPath, r.officialAccount)')
contains("internal/platform/envfile/secrets.go", 'Load("MND_WECHAT_OFFICIAL_TOKEN", "MND_WECHAT_OFFICIAL_ENCODING_AES_KEY")')
contains("internal/platform/wechatofficial/handler.go", 'query.Get("encrypt_type") != "aes"', 'http.MaxBytesReader', 'now.Add(-5*time.Minute)', 'outer["ToUserName"] != h.config.OriginalID', 'message["ToUserName"] != h.config.OriginalID')
contains("internal/platform/wechatofficial/protocol.go", 'subtle.ConstantTimeCompare', 'padding > 32', 'depth > 16', 'case xml.Directive:', 'data[end:], []byte(h.config.AppID)')
print("官方授权静态边界：原生SDK固定来源、原挑战回跳、用途／显式确认、HttpOnly适配及独立公众号安全回调／默认关闭／服务端Secret接线；仅源码核对，未执行SDK／类型检查／测试。")
