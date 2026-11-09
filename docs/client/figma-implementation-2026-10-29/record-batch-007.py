"""Record inspected source/grammar receipts; never imply runtime acceptance."""
import hashlib
import json
from pathlib import Path

folder = Path(__file__).resolve().parent
root = folder.parents[2]
def read(name):
    return (root / name).read_text(encoding="utf-8")
def save(file, value):
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
def digest(file):
    return hashlib.sha256(read(file).replace("\r\n", "\n").encode()).hexdigest()

syntax = json.loads((folder / "batch-007-syntax.json").read_text(encoding="utf-8"))
followup = json.loads((folder / "batch-007-syntax-followup.json").read_text(encoding="utf-8"))
latest = {row["file"]: row for row in syntax["files"] + followup["files"]}
assert len(latest) == 12
for file, row in latest.items():
    assert row["exit_code"] == 0 and row["sha256_lf"] == digest(file)
checks = json.loads((folder / "batch-007-checks.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in checks["files"])
fragments = json.loads((folder / "batch-007-fragments.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in fragments["files"])
hashes = {file: digest(file) for file in [*latest, "web-client/src/styles.css"]}
batch = {
    "id": "007", "updated": "2026-10-30", "status": "source_written_static_reviewed_runtime_pending",
    "high_fidelity_design_nodes": ["238:371730", "238:371752", "238:371756", "238:371755"],
    "scope": ["session expiry result", "definitive account-change result", "logout confirmation and completion", "owner-scoped saved drafts on explicit logout", "stale session response guards"],
    "source_hashes_lf": hashes, "static_report": "batch-007-checks.json",
    "syntax_reports": ["batch-007-syntax.json", "batch-007-syntax-followup.json", "batch-007-fragments.json"],
    "syntax_counts": {"files": 12, "whole_files_without_diagnostics": 8, "same_baseline_diagnostic_signatures": 4, "changed_session_security_fragments_without_diagnostics": 5},
    "static_counts": {"public_ui": 45, "web_sources": 91, "web_imports": 413},
    "notes": [
        "Session-end reason/action and masked phone are in-memory presentation metadata only. No raw target, credential, token or grant is persisted by this metadata.",
        "Security-change/logout results require a successful server response. Browser invalidation follows the adapter's confirmed cookie-clearing contract (internal/platform/mobileapi/browser.go); expiry and close-account request remain distinct.",
        "The existing service ends sessions after a security change, so the phone result primary action explicitly says log in and return to settings. This contract-driven difference from the Figma return-only label is recorded rather than pretending the old session is valid.",
        "Explicit logout now retains saved owner-scoped native drafts as required by authLogoutConfirm. Account-change/close-request cleanup remains separate. Guest and other owners cannot load the prior owner's saved drafts.",
        "Native logout cannot clear a new owner/generation; browser session and refresh responses cannot restore an old owner after an epoch change. Failed credentials are not classified as session expiry.",
        "Memory recovered to more than 7 GiB during the grammar reads. Batch 006's historical low-memory skips remain unchanged; current grammar coverage is recorded here.",
        "ReadingPages, MobileApi, MobileStore and MobileFeatureStore retain the same pre-existing grammar diagnostic signatures as HEAD; they are not marked wholly verified. Five changed method fragments are clean. The final cancelLogin guard was read again in the followup; earlier fragment bodies are unchanged."
    ],
    "states": ["authSessionExpired", "authPhoneChanged", "authLoggedOut", "authLogoutConfirm"],
    "remaining": ["remaining auth/provider states", "all remaining consumer/backend state review and implementation", "type and platform runtime/visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False,
}
save(folder / "batch-007-evidence.json", batch)
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "007" for row in ledger["batches"])
ledger["batches"].append(batch)
for state in ledger["consumer_states"]:
    if state["key"] in batch["states"]:
        state["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        state["reviewed_findings"].append("Batch 007: actual session/result guards and Figma task presentation reviewed; runtime/visual acceptance remains open.")
        if state["key"] == "authLogoutConfirm":
            for platform in ["android", "ios", "desktop"]:
                state["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/SecurityPages.kt"
            state["sources"]["web"] = "web-client/src/features/Account.tsx"
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue remaining auth/provider states and consumer/backend families using actual Figma contexts; keep runtime and whole-state acceptance open. No automatic commit/push or execution.")
save(ledger_file, ledger)

name = "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = read(name).replace("当前批次005执行中", "当前批次007已记录，继续全量实现")
section = """## 批次007：会话失效、安全变更及退出的真实结果

2026-10-30源码与允许的静态已记录：[13份当前源码指纹与边界](figma-implementation-2026-10-29/batch-007-evidence.json)。4份高保真上下文及图片为会话过期238:371730、手机号更新238:371752、退出结果238:371756、退出确认238:371755；复用已有原图标与字体，无新增临时资源。

- 原生／Web区分登录失效、主动退出、实际安全变更及注销请求。结果只接真实成功回执；展示原因、动作和脱敏号码仅存内存，不能授予权限、跳过用途验证或重放请求。
- 手机号／邮箱／密码／解绑成功后按真实服务合同使会话失效，再明确登录并返回设置；设计原“返回账号设置”因服务要求改为“重新登录并返回设置”，不伪装原会话仍有效。普通过期保留原任务，不自动发送草稿。
- 两端退出确认使用独立任务页与固定主操作，失败保持当前账号。按本次Figma明确的“草稿按账号保留”修正原生旧退出清理；已保存草稿继续按账号／服务隔离，游客不可读；敏感变更和注销请求的既有独立处理未混同普通退出。
- 退出和异步会话回读补账号代次保护；旧退出／刷新结果不能清掉或恢复期间切换的新账号。敏感写入期间不能关闭登录结果；原生账号变更Complete层在真实结果出现后清理，避免遮挡。

两项路径／资源脚本通过（45公共UI／48分派，91Web／413引用）；内存恢复后12份最新语法读取正常退出，8份全文件无诊断，4份为与HEAD相同旧诊断对照，另5个变更方法片段无诊断。最终返回保护2文件已再次读取；不当作类型检查或运行。006原552MiB跳过记录保留，当前覆盖记在007。差异空白通过；未编译／测试／lint／启动。其余全量继续。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：用户于2026-10-30恢复，批次001–007已记；006的内存语法待项已由007当前覆盖接续。继续其余认证／官方授权、消费者和独立后台状态。748全量未完成，不把源码归属当作逐端验收。")
text = text.replace("用户已明确授权提交和推送现有成果；本记录随成果提交，实际同步以Git历史、工作树及本轮推送回执为准。", "此前001–004已按用户授权随48a3cfd推送；恢复后的005起成果未提交推送，不沿用已履行的提交授权。")
(root / name).write_text(text, encoding="utf-8", newline="\n")
for name in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(name)
    if name.endswith("CONTEXT.md"):
        old = next(line for line in text.splitlines() if line.startswith("- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`"))
        new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 用户恢复全部按设计实现；main／48a3cfd已推送，本轮未提交。[批次005–007](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)补分步认证／恢复、账号与当前号、安全变更和会话／退出独立结果。45公共UI／91Web／413引用只读通过；007内存恢复后12当前语法含8完整／4同基线诊断及5变更片段，接续006历史跳过。按Figma修正普通退出保留账号草稿；安全变更按实际会话合同重新登录后回设置。执行中，继续认证／授权及其余全量；不编译／测试／启动，无后台或自动续做。748及后台全量未完成。"
    else:
        old = next(line for line in text.splitlines() if line.startswith("| AI-20261029-FIGMA-ALL-CODE "))
        new = "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–007源码／静态分批已记 | Codex / 2026-10-30；main／48a3cfd，本轮未提交 | 现行公共Compose／shared／Web、必要后端及逐批证据；不构建／测试／启动／自动提交推送 | [七批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：分步认证、账号／当前号、会话过期／变更／退出结果及草稿保留已写；007当前语法8完整／4旧诊断对照。继续认证／官方授权及其他全量，748和后台逐项仍未完成；无后台或定时任务 |"
    (root / name).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次007

- Codex，main／48a3cfd，本轮未提交；用户继续全部按设计实现，执行中。
- [本批证据](../client/figma-implementation-2026-10-29/batch-007-evidence.json)：会话过期／安全变更真实结果、退出独立确认和结果、普通退出按Figma保留已保存的账号草稿；仍隔离游客和其他账号。服务清Cookie与会话失效合同已核对，安全变更需重新登录后回设置，不误报注销完成。
- 四份高保真上下文／图片已读。两项路径／资源静态通过；内存恢复后12当前语法读取正常退出（8完整无诊断，4同HEAD旧诊断对照），5个会话／安全变更方法片段无诊断，最终2文件保护变更复读。接续006未执行语法的事实，不修改历史跳过报告。
- 13当前源码指纹保存，差异空白通过；未编译／类型检查／测试／lint／启动／迁移／部署／提交推送。其余认证／授权和消费者／后台全量继续；无后台或定时任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "007", "source_files": len(hashes), "grammar_files": len(latest), "execution": "active"}))
