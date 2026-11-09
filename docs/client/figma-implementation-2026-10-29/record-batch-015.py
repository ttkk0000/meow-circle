"""Record verified in-process identity presentation without inventing persistence/authentication."""
import hashlib
import json
from pathlib import Path

folder = Path(__file__).resolve().parent
root = folder.parents[2]
def read(file):
    return (root / file).read_text(encoding="utf-8")
def digest(file):
    text = read(file)
    assert not text.startswith("\ufeff"), file
    return hashlib.sha256(text.replace("\r\n", "\n").encode()).hexdigest()
def save(file, value):
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")

checks = json.loads((folder / "batch-015-checks-final.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"]) for row in checks["files"])
initial = json.loads((folder / "batch-015-syntax.json").read_text(encoding="utf-8"))
skipped = json.loads((folder / "batch-015-syntax-followup.json").read_text(encoding="utf-8"))
assert len(initial["files"]) == 7 and all(row["exit_code"] == 0 for row in initial["files"])
assert all(row["status"] == "not_read_low_memory" and row["headroom_mb"] < 1024 for row in skipped["files"])
pending_files = {row["file"] for row in skipped["files"]}
for row in initial["files"]:
    if row["file"] not in pending_files:
        assert digest(row["file"]) == row["sha256_lf"]
files = list(dict.fromkeys([row["file"] for row in initial["files"] + skipped["files"]] + ["web-client/src/styles.css"]))
batch = {
    "id": "015", "updated": "2026-10-30", "status": "source_written_path_asset_reviewed_grammar_partially_pending",
    "high_fidelity_design_nodes": ["238:371720", "311:379884"],
    "scope": ["known-account welcome page", "server-confirmed in-process identity/method metadata", "consent/real provider flow and phone fallback"],
    "source_hashes_lf": {file: digest(file) for file in files}, "static_report": "batch-015-checks-final.json",
    "syntax_reports": ["batch-015-syntax.json", "batch-015-syntax-followup.json"],
    "syntax_counts": {"current_whole_files_without_diagnostics": 5, "same_baseline_diagnostic_signatures": 1, "current_grammar_pending": 2, "headroom_at_skip_mb": 623, "minimum_headroom_mb": 1024},
    "grammar_pending_source_hashes": {file: digest(file) for file in sorted(pending_files)},
    "static_counts": {"shared_sources": 32, "public_ui": 49, "web_sources": 96, "web_imports": 439},
    "states": ["authKnown"],
    "notes": [
        "The previously recorded missing remembered-identity source is now supplied by actual successful server login/verified session/profile responses, only in the current API instance's memory. It is not serialized into the vault, localStorage, routes or drafts. Failed cached restoration creates no remembered identity. No restart persistence is claimed.",
        "The phone screenshot from batch012 and current Web context/image were inspected. Dedicated native/Web welcome pages show the actual previous nickname and known method, 56px/radius16 identity slot, notice, agreement/official policy links, provider actions, phone fallback and other real login methods.",
        "Signing out supplies no permission to fetch the former account's private avatar, so the existing neutral image placeholder is used with no media id. No illustrative cat is presented as a user identity. Successful account-closure session endings clear the memory; ordinary logout/expiry can retain this presentation metadata.",
        "Remembered data never selects or authenticates the previous owner. Every provider/phone/password/email action reuses actual server verification; a different returned identity remains a different account. No nickname-based merging or local qualification was added. Created-account review and original-task return remain unchanged.",
        "Known provider controls use 12px gaps, 20px original marks and 14/22 labels. Native iOS Apple is a separate black/white action using a display color filter on the unchanged SVG. Web Apple leads through the existing actual-unavailable check while its Web configuration is absent; no functioning Web Apple SDK is claimed. Other native platforms retain their existing supported provider choices.",
        "The first seven grammar reads were clean except the same baseline MobileApi diagnostics. Final provider/layout and optional icon-color-filter edits were guarded at 623MiB, below 1024MiB, so two current files remain unread. One provider heading/size adjustment followed the skip; its current hash is recorded separately instead of reusing the old hash as validation. Five current whole files and one baseline comparison are valid; earlier provider clean output does not validate this latest revision. Path/asset scripts pass; no build/type checker/test/app/browser/device was run."
    ],
    "remaining": ["final ProviderAuthPages/ClientAssets grammar reads after normal headroom", "typed partial restriction contract and remaining auth/consumer/backend families", "runtime/visual/platform acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "015" for row in ledger["batches"])
save(folder / "batch-015-evidence.json", batch)
ledger["batches"].append(batch)
for row in ledger["consumer_states"]:
    if row["key"] == "authKnown":
        row["source_review"] = "dedicated_source_written_path_asset_reviewed_grammar_partially_pending"
        row["reviewed_findings"].append("Batch 015 supersedes the remembered-source gap for the current process only: server-confirmed identity/method display, no auth grant or private-avatar fetch. Two final provider/icon files remain grammar-pending after the memory guard; runtime/persistence/visual acceptance unverified.")
        for platform in ["android", "ios", "desktop"]:
            row["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthKnownPages.kt"
        row["sources"]["web"] = "web-client/src/features/AuthKnown.tsx"
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue remaining auth/consumer/backend states and typed partial restrictions. Keep final provider/icon grammar pending until normal headroom returns; do not treat remembered identity as authentication or durable preference.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] == "authKnown":
        for platform in ["android", "ios", "desktop"]:
            row[platform + "_source"] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthKnownPages.kt"
        row["web_source"] = "web-client/src/features/AuthKnown.tsx"
        row["implementation_status"] = "dedicated_source_path_asset_reviewed_grammar_partially_pending"
        row["note"] = "批次015：仅进程内服务确认的旧身份／方式展示与真实再验证，不持久记忆、不凭旧头像取私密图；两处最终样式／图标语法因低内存待读，未编译／运行／视觉验收。"
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + batch["high_fidelity_design_nodes"]))
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items():
    current["files"][file] = {"batch": "015", "sha256_lf": sha}
for file, row in current["files"].items():
    assert digest(file) == row["sha256_lf"], file
save(current_file, current)
progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = progress.read_text(encoding="utf-8").replace("当前批次014已记录", "当前批次015已记录")
section = """## 批次015：真实进程内身份的欢迎回来页

2026-10-30：[九份源码与边界](figma-implementation-2026-10-29/batch-015-evidence.json)。手机238:371720沿用012已读上下文与图片，Web311:379884实时读取并查看。原“已知身份来源”缺口以当前进程内服务确认的登录／会话／资料响应补齐；不写保险库、浏览器普通持久存储或草稿，不宣称重启后保留。专用页使用实际昵称与已知方式、56身份槽、协议与快捷入口、手机和其他登录方式。退出后不请求旧账号私密头像，用现有中性占位；注销请求退出清除此记忆。

旧身份只展示，所有快捷、手机、密码／邮箱仍由服务端重新验证；返回其他账号不按昵称合并。iOS Apple单独黑白入口，Web Apple配置缺失仍进入真实不可用说明，不宣称SDK已可用。公共图标仅增可选显示colorFilter，原SVG未改。

32shared／49UI／96Web／439引用只读通过。当前5完整语法、1同HEAD旧MobileApi诊断对照；最后ProviderAuthPages／ClientAssets在623MiB低于1024门槛跳过，provider后续标题／尺寸微调的当前hash单记，不能拿此前读取当本版通过。语法待正常余量后接续，未编译／类型检查／测试／启动／逐端视觉。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：批次001–015已记，欢迎回来只展示当前进程服务确认的旧身份，仍需再验证。继续剩余认证／消费者／后台；两份最终样式图标语法低内存待读，部分受限合同与全量运行／视觉待办保留。")
progress.write_text(text, encoding="utf-8", newline="\n")
for file in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(file)
    prefix = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE "
    old = next(line for line in text.splitlines() if line.startswith(prefix))
    new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 执行中；main／48a3cfd，未提交。[005–015进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：专用资料／任务返回／关联／恢复协助及真实进程内旧身份欢迎页已写。32shared／49UI／96Web／439引用通过；015当前5完整／1旧诊断，最终provider／图标两份623MiB低于门槛待读，013已在014补读。继续认证及全量余项，部分受限合同／运行视觉未完成。不编译／测试／启动／提交推送，无后台任务。" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–015分批源码已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，未提交 | shared／公共Compose／Web及必要合同、逐批证据；不构建／测试／启动／自动提交推送 | [十五批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：真实进程内旧身份欢迎页已写，32shared／49UI／96Web／439引用；5完整／1旧诊断、最终2语法低内存待读。继续认证／消费者／后台，部分受限合同与全量运行／视觉保留 |"
    (root / file).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
for file in ["docs/CURRENT_BASELINE.md", "docs/client/README.md"]:
    text = read(file).replace("005–014", "005–015").replace("已记十四批", "已记十五批").replace("95份Web源码与48份公共UI，32份shared", "96份Web源码与49份公共UI，32份shared").replace("32 / 48", "32 / 49").replace("32shared／48公共UI／95Web／435引用", "32shared／49公共UI／96Web／439引用")
    (root / file).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次015

- Codex，main／48a3cfd，保留在途成果；用户继续，执行中，未提交推送。
- [证据](../client/figma-implementation-2026-10-29/batch-015-evidence.json)：服务确认的当前进程旧身份欢迎页、真实快捷与手机／其他方式、明确协议；不持久记忆、不直接认证／合并，不读取旧账号私密头像，注销退出清除记忆。手机上下文沿用012已读，Web高保真已看。
- 32shared／49UI／96Web／439引用通过；当前5全文件／1同HEAD旧诊断，2最终provider／图标语法623MiB低于门槛跳过，之后provider细调当前hash另记，旧读取不抵扣。继续其余状态，未编译／类型检查／测试／启动，全量运行／视觉及受限合同待办保留，无后台任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "015", "source_files": len(files), "checkpoint_files": len(current["files"]), "grammar_pending": 2, "execution": "active"}))
