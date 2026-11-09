"""Record the provider-result source batch and its actual static boundaries."""
import hashlib
import json
import subprocess
from pathlib import Path

folder = Path(__file__).resolve().parent
root = folder.parents[2]
def read(name):
    return (root / name).read_text(encoding="utf-8")
def save(file, value):
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
def digest(file):
    return hashlib.sha256(read(file).replace("\r\n", "\n").encode()).hexdigest()
syntax = json.loads((folder / "batch-008-syntax.json").read_text(encoding="utf-8"))
followup = json.loads((folder / "batch-008-syntax-followup.json").read_text(encoding="utf-8"))
latest = {row["file"]: row for row in syntax["files"] + followup["files"]}
unchanged = "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/ProviderAuthStore.kt"
baseline = subprocess.run(["git", "show", "HEAD:" + unchanged], cwd=root, capture_output=True, check=True).stdout.decode("utf-8").replace("\r\n", "\n")
assert baseline == read(unchanged).replace("\r\n", "\n")
for file, row in latest.items():
    assert row["sha256_lf"] == digest(file)
    if file != unchanged:
        assert row["exit_code"] == 0 and not row["syntax"]["errors"]
    else:
        assert row["syntax"]["errors"] == [{"type": "ERROR", "line": 7, "text": "fun interface ProviderLauncher"}]
checks = json.loads((folder / "batch-008-checks.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in checks["files"])
batch = {
    "id": "008", "updated": "2026-10-30", "status": "source_written_static_reviewed_runtime_pending",
    "high_fidelity_design_nodes": ["238:371728", "238:371727", "238:371729", "460:17333"],
    "scope": ["provider cancelled/unavailable/expired task results", "provider popup timeout reason", "owner-memory OTP address and purpose matching for linking", "account-choice contract review"],
    "source_hashes_lf": {file: digest(file) for file in latest},
    "static_report": "batch-008-checks.json", "syntax_reports": ["batch-008-syntax.json", "batch-008-syntax-followup.json"],
    "notes": [
        "Three modified files have final whole-file grammar reads without diagnostics. The unmodified ProviderAuthStore is byte-equivalent after LF normalization to HEAD and retains a grammar limitation at fun interface; its exit 1 is retained, not relabelled a full pass.",
        "The first Web ProviderSignIn read found a JSX attribute error; the attribute was corrected to an explicit JS string expression and read cleanly again. Initial failure remains in the report.",
        "Cancel/unavailable/expiry use dedicated task titles, 28/40 headlines, notice, quiet return to original task and fixed return-to-login action. They do not directly rerun authorization. Unknown create/link results keep the separate no-replay flow.",
        "Popup expiry is only assigned from the actual timer or denied official challenge. Missing provider setup/blocked popup cannot display a working provider. Apple is labelled correctly when reached on Web but remains unavailable without an implemented Web configuration.",
        "OTP fields are bound to actual send receipt/address/purpose in current owner memory. Changing channel/address clears the binding and code. Password remains a separate verification option. No authorization, resolution or grant is persisted or bypassed.",
        "accountChoice is only a partial contract review: the Figma email example does not prove a new email-resolution server contract. Existing provider resolution can explicitly choose original/new account, while email login/register remain explicit separate paths. No fake email-verification or automatic account merge was added."
    ],
    "states": ["authProviderCancelled", "authProviderUnavailable", "authCallbackExpired"],
    "partial_states": ["accountChoice"],
    "remaining": ["official authorization disclosure and remaining auth states", "remaining consumer/backend families", "type/platform runtime and visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False,
}
save(folder / "batch-008-evidence.json", batch)
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "008" for row in ledger["batches"])
ledger["batches"].append(batch)
for state in ledger["consumer_states"]:
    if state["key"] in batch["states"]:
        state["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        state["reviewed_findings"].append("Batch 008: dedicated provider result and actual popup reason reviewed; runtime/visual acceptance open.")
        for platform in ["android", "ios", "desktop"]:
            state["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ProviderAuthPages.kt"
        state["sources"]["web"] = "web-client/src/features/ProviderSignIn.tsx"
    elif state["key"] in batch["partial_states"]:
        state["source_review"] = "partial_contract_review"
        state["reviewed_findings"].append("Batch 008: explicit provider account choice reviewed; email-specific resolution contract remains open, no simulated verification or automatic merging.")
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue official authorization disclosure, other auth states and consumer/backend families against actual designs; keep whole-state/runtime acceptance open.")
save(ledger_file, ledger)
name = "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = read(name).replace("当前批次007已记录", "当前批次008已记录")
section = """## 批次008：官方授权的取消、不可用、失效与关联验证码

2026-10-30源码与允许静态已记录：[4份源码指纹及边界](figma-implementation-2026-10-29/batch-008-evidence.json)。已读取高保真图片／上下文：取消238:371728、不可用238:371727、失效238:371729、账号选择460:17333。

- 原生／Web三个独立结果使用28/40标题、说明、返回原任务及底部返回登录；不在取消／失效结果上直接重跑官方授权。Web超时／弹窗不可打开使用明确原因，未配置方式不呈现可工作授权；Apple名称正确但Web未配置仍关闭。
- 关联原账号的邮箱／手机号验证码及本次关联用途验证码，只在真实发送回执后与地址、渠道／用途匹配；修改地址或方式清空旧验证码。服务端授权、原账号登录、用途核验、最终主动确认及未知结果不重发的机制保留。
- 账号选择仅合同部分审查：官方身份已有明确resolution选择，邮箱示例不能凭空变成服务端已验证的新邮箱分流合同；保持邮箱登录／注册显式选择，不伪造验证或合并。此项未标独立状态完成。

两份路径／资源静态通过（45公共UI、91Web、413引用）。3份修改文件最终全文件语法无诊断；未改动ProviderAuthStore与HEAD逐字相同，仅旧语法器fun interface限制，退出1保留、不计整文件通过。初次Web JSX属性语法发现已修正并干净复读；初始报告保留。未编译／类型检查／测试／lint／启动；全量继续。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：用户于2026-10-30恢复，批次001–008已记；继续官方授权隐私说明、其余认证／消费者与独立后台状态。748全量未完成，运行／视觉未验，不把归属或部分合同审查当完成。")
(root / name).write_text(text, encoding="utf-8", newline="\n")
for name in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(name)
    if name.endswith("CONTEXT.md"):
        old = next(line for line in text.splitlines() if line.startswith("- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`"))
        new = old.replace("005–007", "005–008").replace("执行中，继续认证／授权及其余全量", "008再补官方取消／不可用／失效任务页和关联OTP回执匹配，3修改文件最终语法无诊断，共享授权旧fun interface诊断单列。执行中，继续授权说明及其余全量")
    else:
        old = next(line for line in text.splitlines() if line.startswith("| AI-20261029-FIGMA-ALL-CODE "))
        new = old.replace("005–007", "005–008").replace("七批记录", "八批记录").replace("继续认证／官方授权及其他全量", "008授权取消／不可用／失效和OTP匹配已写。继续授权说明及其他全量")
    (root / name).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次008

- Codex，main／48a3cfd，本轮未提交；用户继续全部按设计实现，执行中。[本批证据](../client/figma-implementation-2026-10-29/batch-008-evidence.json)：官方授权三类独立结果、真实弹窗超时原因和关联OTP发送回执与地址／用途绑定。四份高保真上下文及图片已读；邮箱账号选择仅合同部分审查，不伪造新服务合同。
- 两项路径／资源脚本通过；3修改文件最终语法无诊断，初次Web属性错误修正并复读。未改动共享ProviderAuthStore与HEAD相同，fun interface旧语法诊断保留exit1单列，不计全通过。
- 未编译／类型检查／测试／lint／启动／提交推送。继续授权说明和其余消费者／后台；全量、运行及逐端视觉仍未完成，无后台或定时任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "008", "modified_source_files": 3, "unchanged_baseline_grammar_limit": 1, "execution": "active"}))
