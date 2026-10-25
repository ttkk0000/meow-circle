"""只读检查Figma页面缺口的跨端接线；不执行应用、SDK、类型检查或测试。"""
from pathlib import Path
import ast
import hashlib
import json
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
shared = "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/"
ui = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/"
store = read(shared + "MobileStore.kt")
feature = read(shared + "MobileFeatureStore.kt")
state = read(shared + "ClientReadState.kt")
for status, name in [(401,"SignIn"),(403,"Forbidden"),(429,"Busy")]:
    assert f"{status} -> ClientReadFailure.{name}" in state
assert "404, 410 -> ClientReadFailure.Unavailable" in state
assert "is MobileConnectionException -> ClientReadFailure.Connection" in state
assert "readAt = clientReadTimestamp()" in store and "clear: Boolean = false" in store
assert "PageState(error = message, failure = failure)" in store
assert "inboxReadAt = if (e is MobileApiException" in store
assert "FeaturePage.PhotoHelp -> Unit" in feature
for name in ["LocationHelp", "NotificationHelp", "PhotoHelp"]:
    assert f"FeaturePage.{name} ->" in read(ui + "AccountPages.kt")
    assert f"FeaturePage.{name}" in read(ui + "FeaturePages.kt")

queue = store.split("fun queueNotification",1)[1].split("fun queueInboxNotification",1)[0]
assert "canonicalNotificationId(raw)" in queue and "api." not in queue
opening = store.split("fun openPendingNotification",1)[1].split("fun openNotification(body",1)[0]
for guard in ["api.notification(pending.id)", "record.ownerId != actor", "record.id != pending.id", "epoch == accountEpoch", "pending.owner != actor", "!canOpenNotification()"]:
    assert guard in opening, guard
assert "readNotification(" not in opening and "receiptWrite(" not in opening
assert "s.inboxReadAll.visible" in store and "features.age.state.value.busy" in store
assert 'get("notifications/$id")' in read(shared + "MobileApi.kt")
api = json.loads(read("docs/api/mobile-v2.openapi.json"))
assert "get" in api["paths"]["/api/v2/notifications/{id}"]
native = read("kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/MainActivity.kt")
assert native.count("consumeNotification(intent)") == 2
swift = read("kmp/iosApp/ComposeApp/NativeDevicePush.swift")
assert 'userInfo["notification_id"]' in swift and "UNNotificationDefaultActionIdentifier" in swift
assert "pendingNotificationID" in swift and 'raw as? String' in swift
assert "queueNotification" in read("kmp/iosUi/src/iosMain/kotlin/com/ttkk0000/meowcircle/iosui/MainViewController.kt")
assert "record.owner_id === session.user?.id" in read("web-client/src/features/NotificationPage.tsx")

query = read("web-client/src/core/query.ts")
assert "failure: readFailure(error)" in query and "readAt: denied ? undefined" in query
records = read("web-client/src/features/Records.tsx")
for guard in ["!page.error", "page.data.items.length", "page.next === 0n"]: assert guard in records
browser_upload = read("web-client/src/core/upload.ts")
for guard in ["xhr.withCredentials = true", "xhr.upload.onprogress", '"abort"', "new Response", "xhr.timeout"]: assert guard in browser_upload
assert "X-MND-CSRF" in read("web-client/src/core/api.ts")
assert "onUploadProgress" in read("web-client/src/core/api.ts")
assert "onUpload { sent, total -> progress(sent, total) }" in read(shared + "MobileApi.kt")
queue_ui = read(ui + "PageComponents.kt")
for guard in ["detectDragGesturesAfterLongPress", "rememberUpdatedState(onMove)", 'key(photo.key)', "photo.uploadPercent", "等待服务器确认", "onSaveOrder", "LocalPhotoPreview"]: assert guard in queue_ui
assert "store::moveReplyPhoto" in read(ui + "ConsumerApp.kt")
attachments = read("web-client/src/components/Attachments.tsx")
for guard in ["onPointerMove", "onDrop", "onSaveOrder", "等待服务器确认", "failed", "PhotoChoiceHelpContent"]: assert guard in attachments
for name in ["WorkflowEditor", "chat"]:
    text = read(f"web-client/src/features/{name}.tsx")
    assert "onUploadProgress" in text and "onSaveOrder={persist}" in text
    assert "photos: movePhoto(" in text

evidence = root / "docs/design/figma-study/page-gaps-2026-10-25"
contexts = list(evidence.glob("*.json"))
assert len(contexts) == 13
for path in contexts:
    item = json.loads(path.read_text())
    assert item["file_key"] == "3OFOC3vu18Kx9sRPjummOC" and item["screenshot_inspected"]
    assert any('data-node-id=' in block["text"] for block in item["content"])
asset = "state-paw-print.svg"
copies = [root / folder / asset for folder in ["web-client/public/assets/figma", "kmp/androidApp/src/main/assets/figma", "kmp/clientUi/src/commonMain/composeResources/files/figma"]]
assert len({hashlib.sha256(path.read_bytes()).hexdigest() for path in copies}) == 1
assert ET.parse(copies[0]).getroot().attrib["width"] == "24"
trace = json.loads(read("docs/client/figma-implementation-trace-2026-10-24.json"))
assert len(trace["states"]) == 740 and trace["counts"]["runtime_verified"] == 0
assert all(row["runtime_verified"] is False for row in trace["states"])
assert all((root / row["web_source"]).is_file() for row in trace["states"])
ast.parse(Path(__file__).read_text())
print(f"页面缺口只读核对：{len(contexts)}份设计上下文／截图、状态分类与缓存时间、三端帮助页、通知编号／账号／交互锁、媒体进度／原编号／排序／保存与740条索引；未执行类型检查、编译、测试或应用。")
