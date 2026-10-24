"""Read UI source/configuration only. No Kotlin execution, builds, test/device or network calls."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
android = root / "kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp"
ui = android / "mobile"
source = {p.name: p.read_text(encoding="utf-8") for p in ui.glob("*.kt")}
session = source["AndroidUiSessionState.kt"]
for forbidden in ("import android.", "SavedStateHandle", "rememberSaveable", "TextFieldValue.Saver"):
    assert forbidden not in session, ("private UI state must stay in values-only memory", forbidden)
for guard in ("old.generation == generation", "owner != value", "values.clear()", 'scope.startsWith("transient:") && scope !in active'):
    assert guard in session, guard
assert "val uiSession = AndroidUiSessionState()" in source["MobileViewModel.kt"]
assert "uiSession.clear(); store.close()" in source["MobileViewModel.kt"]
assert "MobileMviViewModel<MobileHostState, MobileHostIntent, MobileHostEffect>" in source["MobileViewModel.kt"]
assert "abstract class MobileMviViewModel" in source["AndroidMviBase.kt"] and "MviViewModel<State, Intent, Effect>" in source["AndroidMviBase.kt"]
for mvi_helper in ("fun MviStatusBanner(", "fun PendingLinkBanner(", "fun <S : Any, I : Any, E : Any> MviStoreHost(", "fun <E : Any> CollectMviEffects("):
    assert mvi_helper in source["AndroidMviBase.kt"], mvi_helper
for page_comp in ("fun <T> ChoiceChipBar(", "fun <T> SegmentedTextBar(", "fun PhotoQueueList(", "fun OtpVerificationFields("):
    assert page_comp in source["AndroidPageComponents.kt"], page_comp
shared_client = root / "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client"
assert "abstract class MviStore<State : Any, Intent : Any, Effect : Any>" in (shared_client / "MviStore.kt").read_text(encoding="utf-8")
for store_file, base_type in (
    ("MobileStore.kt", "MviStore<MobileState, MobileStoreIntent, MobileStoreEffect>"),
    ("MobileFeatureStore.kt", "MviStore<FeatureState, FeatureStoreIntent, FeatureStoreEffect>"),
    ("SensitiveActionStore.kt", "MviStore<SensitiveActionState, SensitiveStoreIntent, SensitiveStoreEffect>"),
):
    assert base_type in (shared_client / store_file).read_text(encoding="utf-8"), (store_file, base_type)
assert "appViewModel.uiSession" in (android / "MainActivity.kt").read_text(encoding="utf-8")
shell = source["AndroidMobileApp.kt"]
assert "key(owner)" in shell and "session.useOwner(owner)" in shell
assert "retainTransientScopes" in shell and 'add("transient:auth")' in shell
assert "movableContentOf<MobileState>" in shell and shell.count("home(state)") == 2
assert shell.count("HomeContent(store,") == 1, "one home content definition must move between panes"
fields = source["AndroidDateFields.kt"]
assert "rememberUpdatedState(first)" in fields and "rememberUpdatedState(second)" in fields
assert fields.count("movableContentOf") == 2
assert "rememberSessionUiState" in fields and "pendingHour" in fields and "pendingMinute" in fields
assert "dialog.setOnDismissListener(null); dialog.dismiss()" in fields
assert "rememberSessionUiState" in source["AndroidPeopleFields.kt"]
assert "rememberPrivateEditor(text" in source["AndroidFeatureContent.kt"]
assert "TextFieldValue.Saver" not in "\n".join(source.values())

components = source["MobileComponents.kt"]
assert "com.ttkk0000.meowcircle.client" not in components
assert not re.search(r"\b(?:MobileStore|MobileApi|LaunchedEffect|rememberSaveable)\b", components)
names = set(re.findall(r"fun\s+(Mnd\w+)\s*\(", components))
assert names == {"MndForm", "MndSection", "MndPrimaryButton", "MndSecondaryButton", "MndTextField", "MndRichTextField", "MndChoiceGroup", "MndCheckLine", "MndScreenTitle", "MndActionCard"}
all_ui = "\n".join(source.values())
assert set(re.findall(r"\b(Mnd\w+)\s*\(", all_ui)) <= names | {"MndApplication"}
metrics = source["MobileUi.kt"]
for value in ("actionHeight = 48.dp", "cardRadius = 16.dp", "contentWidth = 820.dp", "stickerSize = 144.dp"):
    assert value in metrics, value
assert "Modifier.size(MobileUi.stickerSize)" in source["AndroidComposeControls.kt"]
assert "ContentScale.Fit" in source["AndroidComposeControls.kt"]
manifest = ET.parse(root / "kmp/androidApp/src/main/AndroidManifest.xml").getroot()
ns = "{http://schemas.android.com/apk/res/android}"
activity = manifest.find("application/activity")
assert ns + "screenOrientation" not in activity.attrib
assert ns + "configChanges" not in activity.attrib, "retain state; do not bypass Activity configuration handling"
print(f"Static UI check passed: {len(names)} shared primitives, metrics/theme wiring, ViewModel UI session and movable form/home composition. No compilation, runtime or device validation.")
