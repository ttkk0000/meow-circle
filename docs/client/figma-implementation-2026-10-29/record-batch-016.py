"""Record typed account guards and preserve the current low-memory grammar gap."""
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

checks = json.loads((folder / "batch-016-checks.json").read_text(encoding="utf-8"))
syntax = json.loads((folder / "batch-016-syntax.json").read_text(encoding="utf-8"))
fragments = json.loads((folder / "batch-016-fragments.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"]) for row in checks["files"])
assert len(syntax["files"]) == 13 and all(row["status"] == "not_read_low_memory" and row["headroom_mb"] < 1024 and row["sha256_lf"] == digest(row["file"]) for row in syntax["files"])
assert all(row["status"] == "not_read_low_memory" for row in fragments["files"])
files = [row["file"] for row in syntax["files"]] + ["internal/identity/service.go"]
batch = {
    "id": "016", "updated": "2026-10-30", "status": "source_written_path_asset_reviewed_grammar_pending",
    "high_fidelity_design_nodes": ["238:371739", "311:379960", "238:371749", "311:380000"],
    "scope": ["dedicated bound-identity conflict page", "server-confirmed last-login-method guard page", "known-denial local request cleanup"],
    "source_hashes_lf": {file: digest(file) for file in files}, "static_report": "batch-016-checks.json",
    "syntax_reports": ["batch-016-syntax.json", "batch-016-fragments.json"],
    "syntax_counts": {"files_read": 0, "skipped_low_memory": 13, "changed_fragment_files_skipped": 1, "headroom_mb_range": [1000, 1001], "minimum_headroom_mb": 1024},
    "static_counts": {"shared_sources": 32, "public_ui": 50, "web_sources": 96, "web_imports": 441},
    "states": ["authConflict", "authLastMethod"],
    "notes": [
        "Four phone/Web high-fidelity contexts/images were inspected. Conflict pages use the approved warning panel, 28/40 title, secondary return, quiet recovery help and fixed other-login action. Only the actual identity_in_use response/provider stage triggers this page; no other account identity is disclosed.",
        "Native root login, provider and sensitive account flows and Web equivalents share the dedicated guard content. Recovery opens the actual assistance flow, and the root native auth task is made visible when recovery is entered from an already logged-in provider/sensitive flow. Returning does not alter the current account; other-login still requires real verification.",
        "The server's existing atomic last-usable-method check is preserved. Its generic Invalid retain-another-method error becomes the named RuleError last_login_method (HTTP403 under the existing handler) so clients can render the dedicated design. No local method count or provider availability guess decides this state. The API error schema already carries a free-form error_code; no new route/DTO/qualification policy was added.",
        "The last-method page closes the denied sensitive attempt and offers actual account settings/available-method entry. Phone/email association entries were added alongside existing provider controls. Each still requires original-account scope verification and its existing new-identifier challenge.",
        "After explicit identity_in_use/last_login_method denial, Web deletes only its exact submitted draft revision before leaving the uncertain state. A local version/storage failure keeps the original reconciliation guard. Genuine unknown outcomes remain closed and are never cleaned as a denial.",
        "Both path/asset scripts and diff whitespace read pass. Thirteen grammar files and the MobileStore fragment read were skipped at 1000–1001MiB, below the unchanged 1024MiB floor; no grammar parser was started. The server change is one reviewed return expression with the existing atomic predicate unchanged, without Go execution/compilation/tests. Prior batch015 final icon/provider grammar gaps remain represented here."
    ],
    "remaining": ["current 13-file grammar and changed-fragment reads after normal headroom", "remaining auth/consumer/backend states and typed partial restrictions", "all platform compilation/runtime/visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "016" for row in ledger["batches"])
save(folder / "batch-016-evidence.json", batch)
ledger["batches"].append(batch)
for row in ledger["consumer_states"]:
    if row["key"] in batch["states"]:
        row["source_review"] = "dedicated_source_written_path_asset_reviewed_grammar_pending"
        row["reviewed_findings"].append("Batch 016: dedicated actual-conflict and server-named last-method guard pages, recovery/return/add method actions; no local permission/count inference. Current grammar skipped below 1024MiB; runtime/visual acceptance remains open.")
        for platform in ["android", "ios", "desktop"]:
            row["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthGuardPages.kt"
        row["sources"]["web"] = "web-client/src/components/AccountConflict.tsx"
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue remaining auth/consumer/backend states. Preserve 016/015 grammar gaps until normal headroom returns and do not represent partial restrictions without a typed server contract.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] in batch["states"]:
        for platform in ["android", "ios", "desktop"]:
            row[platform + "_source"] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthGuardPages.kt"
        row["web_source"] = "web-client/src/components/AccountConflict.tsx"
        row["implementation_status"] = "dedicated_source_path_asset_reviewed_grammar_runtime_unverified"
        row["note"] = "批次016：真实identity_in_use冲突／服务端last_login_method专用页及返回、添加与恢复帮助已写。13语法及片段低于内存门槛跳过，未编译／测试／运行／视觉验收。"
        nodes = ["238:371739", "311:379960"] if row["key"] == "authConflict" else ["238:371749", "311:380000"]
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + nodes))
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items():
    current["files"][file] = {"batch": "016", "sha256_lf": sha}
for file, row in current["files"].items():
    assert digest(file) == row["sha256_lf"], file
save(current_file, current)

progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = progress.read_text(encoding="utf-8").replace("当前批次015已记录", "当前批次016已记录")
section = """## 批次016：冲突与最后一种登录方式专用页

2026-10-30：[十四份源码及边界](figma-implementation-2026-10-29/batch-016-evidence.json)。手机238:371739／238:371749、Web311:379960／311:380000上下文与图片已查看。新增公共AuthGuardPages、扩展Web AccountConflict组件：真实冲突显示警告、返回当前账号／原页面、恢复帮助与固定其他登录；登录／官方授权／敏感变更共用实际错误分支，不展示另一个账号资料。恢复入口复用014，已登录来源也明确打开原生auth任务。

原服务端事务中的最后方式判断保留，只把通用Invalid改具名RuleError last_login_method（现行handler为403），使两端按真实拒绝显示设计页面；不以本地计数决定可否解除。添加其他方式进入真实手机／邮箱／官方授权入口，仍需用途验证。Web对两种具名明确拒绝仅清理原提交草稿的准确版本，成功后才解除不确定守卫；本机清理失败仍留原记录核对，真正未知结果不清理。

32shared／50UI／96Web／441引用与diff空白读取通过。13语法及MobileStore片段因1000–1001MiB低于1024门槛跳过，未启动语法器；015最终两文件仍在此待核对。Go仅审一行返回及原判断／handler映射，没有Go执行。未编译／类型检查／测试／启动／全量视觉。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：批次001–016已记，继续其余认证／消费者／后台。当前13语法及片段低内存待读，包含015最后两文件；全量运行／视觉和部分受限合同未完成。")
progress.write_text(text, encoding="utf-8", newline="\n")
for file in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(file)
    prefix = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE "
    old = next(line for line in text.splitlines() if line.startswith(prefix))
    new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 执行中；main／48a3cfd，未提交。[005–016进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：资料／返回／关联／恢复／进程旧身份及冲突／最后登录方式专用页已写，最后方式事务判断保留并具名返回last_login_method。32shared／50UI／96Web／441引用通过；当前13语法及片段1000–1001MiB低于门槛待读，包含015两文件。继续其余状态，部分受限合同／全量运行视觉未完成。不编译／测试／启动／提交推送，无后台任务。" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–016分批源码已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，未提交 | shared／公共Compose／Web、必要服务合同与证据；不构建／测试／启动／自动提交推送 | [十六批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：真实冲突／最后登录方式专用页与具名拒绝已写，32shared／50UI／96Web／441引用；13语法及片段低内存待读。继续其余认证／消费者／后台，全量与部分受限合同未完成 |"
    (root / file).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
for file in ["docs/CURRENT_BASELINE.md", "docs/client/README.md"]:
    text = read(file).replace("005–015", "005–016").replace("已记十五批", "已记十六批").replace("96份Web源码与49份公共UI，32份shared", "96份Web源码与50份公共UI，32份shared").replace("32 / 49", "32 / 50").replace("32shared／49公共UI／96Web／439引用", "32shared／50公共UI／96Web／441引用")
    if file.endswith("CURRENT_BASELINE.md"):
        old = next(line for line in text.splitlines() if line.startswith("当前进度："))
        text = text.replace(old, "当前进度：[严格按Figma分批实施](client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)已记十六批；最新为冲突／最后登录方式专用页与具名拒绝。32份shared／50份公共UI、96份Web／441本地引用；路径／资源静态通过。当前13份语法及片段受1024MiB内存门槛保护而未读，包含015最后图标／provider两文件。旧批次报告只说明当时源指纹，不抵扣本次；未编译／类型检查／测试／运行／全量视觉。")
    (root / file).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次016

- Codex，main／48a3cfd，保留全部成果；用户继续，执行中，未提交推送。
- [证据](../client/figma-implementation-2026-10-29/batch-016-evidence.json)：四张手机／Web高保真已读，真实冲突与最后方式专用页、恢复／其他登录／添加方式已写。服务端原事务判断不改，改具名last_login_method（403）；Web仅在明确拒绝且准确本机草稿清理成功后解除未知守卫。
- 32shared／50UI／96Web／441引用与diff空白读取通过；13语法及片段1000–1001MiB低于1024门槛跳过，不启动语法器，不拿015旧读取当最新通过。Go仅审一行及handler，不执行。继续认证与消费者／后台余项，未编译／类型检查／测试／启动／视觉验收，无后台任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "016", "source_files": len(files), "checkpoint_files": len(current["files"]), "grammar_pending": 13, "execution": "active"}))
