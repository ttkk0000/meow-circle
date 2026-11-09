"""Record actual-task continuation without counting prototype examples as user data."""
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

syntax = json.loads((folder / "batch-012-syntax.json").read_text(encoding="utf-8"))
fragments = json.loads((folder / "batch-012-fragments.json").read_text(encoding="utf-8"))
checks = json.loads((folder / "batch-012-checks.json").read_text(encoding="utf-8"))
for report in [syntax, fragments, checks]:
    assert all(row.get("exit_code") == 0 and row["sha256_lf"] == digest(row["file"]) for row in report["files"])
assert len(syntax["files"]) == 5 and len(fragments["files"]) == 1
files = [row["file"] for row in syntax["files"]] + ["web-client/src/styles.css"]
batch = {
    "id": "012", "updated": "2026-10-30", "status": "source_written_static_reviewed_runtime_pending",
    "high_fidelity_design_nodes": ["238:371768", "311:380076", "238:371720", "238:371744"],
    "scope": ["actual-task continuation page", "Web auth owner/epoch change cleanup", "known/restricted contract gap review"],
    "source_hashes_lf": {file: digest(file) for file in files}, "static_report": "batch-012-checks.json",
    "syntax_reports": ["batch-012-syntax.json", "batch-012-fragments.json"],
    "syntax_counts": {"files": 5, "whole_files_without_diagnostics": 4, "same_baseline_diagnostic_signatures": 1, "clean_changed_fragments": 3},
    "static_counts": {"shared_sources": 31, "public_ui": 47, "web_sources": 94, "web_imports": 430},
    "states": ["authIntentReview"],
    "notes": [
        "Phone and Web intent review use existing 56px rows, 16px spacing and persistent 48px primary action. The product displays its actual suspended task, never the five illustrative prototype tasks or a review-board destination.",
        "Adoption application, circle editor, direct/group conversation, lost-pet task and nearby alerts derive labels from current typed native state or the actual Web route. Continue only closes auth/restores the original application form; it does not send, publish, approve or subscribe.",
        "Back from intent review returns to the actual login result. Current owner checks remain enforced; profile and return-review phases stay mutually exclusive. Web route changes dismiss the old login task without restoring an obsolete history entry.",
        "Web auth observes session epoch, clears previous identifiers, nickname, credentials, consent and completion on unrelated transitions, and ignores old OTP callbacks by generation. Expected password/email and provider login transitions retain their actual completion flow.",
        "authKnown was read but is not implemented: there is no verified remembered-identity/method contract for the illustrated pre-auth card. authRestricted was read but is not implemented: identity active() currently returns unauthorized for every non-active account, with no typed partial restriction/review summary. Eligibility phone/age reasons must not be represented as moderation sanctions.",
        "Five grammar reads: four whole files clean, MobileStore retains the same HEAD diagnostic signatures; three changed fragments are clean. No type checking, compilation, tests, browser or device operation was performed."
    ],
    "remaining": ["known/restricted typed presentation contracts", "provider linking and account recovery", "remaining consumer/backend states and all platform runtime/visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "012" for row in ledger["batches"])
save(folder / "batch-012-evidence.json", batch)
ledger["batches"].append(batch)
for row in ledger["consumer_states"]:
    if row["key"] == "authIntentReview":
        row["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        row["reviewed_findings"].append("Batch 012: real suspended task and explicit return; prototype example records and review-board navigation excluded. All platform runtime/visual acceptance remains open.")
        row["source_variants"] = {"shared_target": files[0], "web_task": "web-client/src/features/AuthReturnTask.tsx"}
    elif row["key"] in ["authKnown", "authRestricted"]:
        row["source_review"] = "design_read_contract_gap_not_implemented"
        row["reviewed_findings"].append(batch["notes"][4])
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue real provider existing-account verification/link confirmation, then account recovery and other consumer/backend states. Known/restricted contracts remain explicit gaps.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] in ["authIntentReview", "authKnown", "authRestricted"]:
        nodes = ["238:371768", "311:380076"] if row["key"] == "authIntentReview" else ["238:371720"] if row["key"] == "authKnown" else ["238:371744"]
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + nodes))
        if row["key"] == "authIntentReview":
            row["implementation_status"] = "dedicated_source_static_reviewed_runtime_unverified"
            row["note"] = "批次012：实际原任务专用返回页与明确继续；评审示例不作用户数据，未编译或运行／逐端视觉验收。"
        else:
            row["implementation_status"] = "design_read_contract_gap_not_implemented"
            row["note"] = batch["notes"][4]
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items():
    current["files"][file] = {"batch": "012", "sha256_lf": sha}
for file, row in current["files"].items():
    assert digest(file) == row["sha256_lf"], file
save(current_file, current)

progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = progress.read_text(encoding="utf-8").replace("当前批次011已记录", "当前批次012已记录")
section = """## 批次012：实际原任务专用返回与账号切换清理

2026-10-30：[六份源码指纹及证据](figma-implementation-2026-10-29/batch-012-evidence.json)。手机238:371768、Web311:380076上下文／图片已查看。登录结果可进入专用返回页，56行／16间距／固定48主操作；显示实际领养申请、圈子编辑、会话、求助、附近提醒等来源，只恢复原页面与申请表，不自动发送、发布、确认或订阅。Figma五个示例事项与“评审入口”不写进产品。Web另按epoch清理跨账号输入和旧OTP回调，保留本次真实登录转场；改变路由关闭旧登录任务，不覆盖历史。

另读手机authKnown／authRestricted：前者缺可核对的上次身份／登录方式合同，后者现有服务只有非active统一未授权，没有部分限制范围与复核状态合同；两项明确记为未实现，不能拿本地照片或手机号／年龄资格原因冒充受限处置。继续关联／恢复等可实施项。

31shared／47UI／94Web／430引用只读通过；5份语法4完整无诊断、MobileStore同HEAD旧诊断，3变更片段无诊断。未编译／类型检查／测试／启动，运行和逐端视觉均未验。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：批次001–012已记，执行中。专用资料／实际任务返回源码已写；继续第三方关联已有账号／用途确认、恢复与其他消费者／后台。已知身份／受限状态合同缺口明确未实现；748全量与运行／视觉未完成。")
progress.write_text(text, encoding="utf-8", newline="\n")
for file in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(file)
    prefix = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE "
    old = next(line for line in text.splitlines() if line.startswith(prefix))
    new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 执行中；main／48a3cfd，未提交。[005–012分批进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：专用资料、实际原任务返回和Web跨账号清理已写。最新31shared／47UI／94Web／430引用；5语法4完整／1旧诊断对照，3片段无诊断。继续第三方关联／恢复等；已知身份与部分受限合同缺口明确未实现，748及后台全量未完成。不编译／测试／启动／提交推送，无后台任务。" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–012源码／静态分批已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，未提交 | shared／公共Compose／Web、必要宿主／合同及证据；不构建／测试／启动／自动提交推送 | [十二批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：实际原任务返回与跨账号清理已写，31shared／47UI／94Web／430引用；5语法4完整／1旧诊断和3片段。继续关联／恢复，已知身份／部分受限合同缺口及全量运行／视觉待办保留 |"
    (root / file).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
for file in ["docs/CURRENT_BASELINE.md", "docs/client/README.md"]:
    text = read(file).replace("005–011", "005–012").replace("已记十一批", "已记十二批").replace("93份Web源码与47份公共UI，30份shared", "94份Web源码与47份公共UI，31份shared").replace("30 / 47", "31 / 47").replace("30shared／47公共UI／93Web／427引用", "31shared／47公共UI／94Web／430引用")
    (root / file).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次012

- Codex，main／48a3cfd，保留全部在途源码；用户继续，执行中，未提交推送。
- [证据](../client/figma-implementation-2026-10-29/batch-012-evidence.json)：专用实际任务返回页、明确继续与Web跨账号输入／旧回调清理。四个高保真上下文／截图已读，authKnown／authRestricted无可冒用合同，明确记录未实现；评审示例不作用户任务。
- 31shared／47UI／94Web／430引用只读通过；5语法4完整／1同HEAD旧诊断，3变更片段无诊断，当前源码指纹匹配。未编译／类型检查／测试／启动，继续关联与恢复及全量余项，无后台任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "012", "source_files": len(files), "checkpoint_files": len(current["files"]), "execution": "active"}))
