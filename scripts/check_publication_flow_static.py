"""只读核对独立声明／发布／分享进入的跨端接线；不执行源码、测试、迁移或应用。"""
from pathlib import Path
import ast
import json
import re

root = Path(__file__).resolve().parents[1]
def read(path): return (root / path).read_text(encoding="utf-8")
shared = "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/"
ui = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/"
paths = json.loads(read("docs/api/mobile-v2.openapi.json"))["paths"]
assert set(paths["/api/v2/source_declarations"]) == {"get", "post"}
assert set(paths["/api/v2/source_declarations/{id}"]) == {"get"}
source = read("internal/mobile/source_declarations.go")
for guard in ["s.command(ctx, actor", '"source.declare"', "s.active(tx, actor)", "owner(previous, actor)", "preview.ManifestHash", "RightsConfirmed", "out = Record{", "requireSourceRecord", "owner(r, actor)"]:
    assert guard in source, guard
body = source.split("type SourceDeclarationBody struct {",1)[1].split("}",1)[0]
assert set(re.findall(r'json:"([a-z_]+)"',body)) == {"manifest_hash", "origin", "previous_id"}
assert "tx.Save(SourceDeclarations" not in source and "s.SaveEntry" not in source
assert "s.requireSourceRecord(tx, actor, in.Declaration)" in read("internal/mobile/social.go")
service = read("internal/mobile/service.go")
assert 'if k == SourceDeclarations' in service and "f.OwnerID = actor" in service
for path in ["internal/mobile/data_requests.go", "internal/mobile/erasure.go"]: assert "Drafts, SourceDeclarations, Media" in read(path)
assert "mobile_source_declarations(owner_id,state,id)" in read("migrations/013_source_declarations.sql")
assert "mobile_source_declarations" in read("internal/store/postgres/mobile.go")

store = read(shared + "MobileStore.kt")
for guard in ["api.prepareSourceDeclaration", "api.declareSource(prepared)", "api.reconcileOperation(key)", 'receipt.command != "source.declare"', "acceptPublicationSource", "publicationSourceRequest != null", "sourceRequestId = s.publicationSourceRequest", "publicationSourceRecord = draft?.sourceRecord"]: assert guard in store, guard
assert "sourceRecord: MobileRecord<SourceDeclarationBody>?" in read(shared + "LocalComposeDrafts.kt")
page = read(ui + "PublicationPages.kt")
for guard in ["PublicationPreviewPage", "state.publicationSourceRequest", "state.publicationSourceResult", "rightsConfirmed", "store::queryPublicationSource", "maxWidth >= 600.dp", "360.dp else 240.dp"]: assert guard in page, guard
shell = read(ui + "ConsumerApp.kt")
assert "PublicationPreviewPage(store,state)" in shell and "PublicationDialog(" not in shell

web = read("web-client/src/features/WorkflowEditor.tsx")
for guard in ["publicationFingerprint", 'path: "source_declarations"', "sourceRequestSent", "sourceCommitted", "record_id: record.id", "PublicationSourceResult", "PublicationSourcePending", "querySource", 'receipt.command !== "source.declare"']: assert guard in web, guard
assert "draftCommandKey(draft)" in read("web-client/src/features/Recovery.tsx")
assert "draftCommandKey(draft)" in read("web-client/src/components/RetrySubmission.tsx")
assert 'scrollScope="publication-preview"' in read("web-client/src/features/PublicationPreview.tsx")

links = read(shared + "TaskLinks.kt")
assert "httpsAuthority" in links and '"adoptions"' in links and "toLongOrNull()" in links
assert "parseTaskLink" in read("kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/MobileViewModel.kt")
ios = read("kmp/iosUi/src/iosMain/kotlin/com/ttkk0000/meowcircle/iosui/MainViewController.kt")
assert "parseTaskLink(url, baseUrl)" in ios and "queueTaskLink" in ios
swift = read("kmp/iosApp/ComposeApp/MndComposeApp.swift")
assert "if !providerFactory.handle(url: url)" in swift and "client.openTaskLink" in swift
assert "MND_SHARE_ASSOCIATED_DOMAIN" in read("scripts/mobile.py")
ast.parse(read("scripts/mobile.py"))
contexts = list((root / "docs/design/figma-study/publication-flow-2026-10-26").glob("*.json"))
assert len(contexts) == 10 and all(json.loads(p.read_text())["screenshot_inspected"] for p in contexts)
print("声明／分享只读核对：10份设计上下文，独立追加记录／原回执／私密范围／跨语言DTO接线，三端公共预览与来源状态、双端链接解析及Swift回调优先；未编译／测试／迁移／运行。")
