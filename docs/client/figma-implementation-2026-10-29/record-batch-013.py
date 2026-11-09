"""Record provider-link UI alignment, retaining the actual low-memory skip."""
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

checks = json.loads((folder / "batch-013-checks.json").read_text(encoding="utf-8"))
syntax = json.loads((folder / "batch-013-syntax.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"]) for row in checks["files"])
assert len(syntax["files"]) == 2
assert all(row["status"] == "not_read_low_memory" and row["headroom_mb"] < 1024 and row["sha256_lf"] == digest(row["file"]) for row in syntax["files"])
files = [row["file"] for row in syntax["files"]]
batch = {
    "id": "013", "updated": "2026-10-30", "status": "source_written_path_asset_reviewed_grammar_pending",
    "high_fidelity_design_nodes": ["238:371737", "238:371738", "311:379952", "311:379956"],
    "scope": ["existing-account verification layout", "dedicated provider-link confirmation", "cancel/back clears scoped proof"],
    "source_hashes_lf": {file: digest(file) for file in files}, "static_report": "batch-013-checks.json",
    "syntax_reports": ["batch-013-syntax.json"], "syntax_counts": {"files_read": 0, "skipped_low_memory": 2, "headroom_mb": 629, "minimum_headroom_mb": 1024},
    "static_counts": {"shared_sources": 31, "public_ui": 47, "web_sources": 94, "web_imports": 430},
    "states": ["authLinkExisting", "authLinkConfirm"],
    "notes": [
        "Both phone and Web high-fidelity context/screenshots were inspected. Existing-account verification now starts with the illustrated phone channel and uses a 28/40 heading, explanation, fixed-label fields, quiet method switches and fixed primary action. Server capabilities govern which methods are usable; password/email alternatives remain available only when actually enabled.",
        "Confirmation uses the actual current account avatar and identity, 56px/radius16 image, 12px card spacing, the real provider method in a fact row, notice, quiet cancel and explicit fixed confirm. The prototype phone number/provider example is not hardcoded. Identity caption uses the actual username because MobileUser/User do not supply a verified masked phone here.",
        "The extra local confirmation checkbox was replaced by the explicit Confirm link button shown in the design. This calls the same shared confirmation method with true only on the user gesture; original account login, current purpose-scope reauthentication and server resolution remain separate, with no automatic linking.",
        "Back/cancel from the account-link pages returns to account choice and clears the short-lived proof. It does not log out the already verified account. A current-owner guard is checked again before Web proof consumption. Unknown create/link results still close automatic retries and require official readback.",
        "Both path/asset scripts pass. Grammar reads were skipped at 629MiB headroom, below the existing 1024MiB floor; the floor was not reduced, no parser was started and no retry was attempted. Existing earlier grammar results cannot be promoted to this revision. No compilation, type checking, tests or runtime/visual acceptance was performed."
    ],
    "remaining": ["grammar reads after normal memory recovery", "recovery entry/receipt and known/restricted contract gaps", "remaining consumer/backend states and platform acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "013" for row in ledger["batches"])
save(folder / "batch-013-evidence.json", batch)
ledger["batches"].append(batch)
for row in ledger["consumer_states"]:
    if row["key"] in batch["states"]:
        row["source_review"] = "dedicated_source_written_path_asset_reviewed_grammar_pending"
        row["reviewed_findings"].append("Batch 013: high-fidelity existing verification and actual-account/provider confirmation, explicit cancel/confirm with existing purpose-scope enforcement. Grammar skipped at 629MiB/1024MiB floor; runtime/visual acceptance pending.")
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Read recovery entry/receipt and actual anonymous assistance contracts; preserve 013 low-memory grammar skip, known/restricted gaps and whole/runtime false flags.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] in batch["states"]:
        nodes = ["238:371737", "311:379952"] if row["key"] == "authLinkExisting" else ["238:371738", "311:379956"]
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + nodes))
        row["implementation_status"] = "dedicated_source_path_asset_reviewed_grammar_runtime_unverified"
        row["note"] = "批次013：原账号验证／真实账号关联确认布局已写，继续原用途凭证／显式确认；路径资源通过，629MiB低于1024MiB门槛跳过语法，未编译或运行／视觉验收。"
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items():
    current["files"][file] = {"batch": "013", "sha256_lf": sha}
for file, row in current["files"].items():
    assert digest(file) == row["sha256_lf"], file
save(current_file, current)

progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = progress.read_text(encoding="utf-8").replace("当前批次012已记录", "当前批次013已记录")
section = """## 批次013：原账号验证与明确关联确认

2026-10-30：[两份源码及边界](figma-implementation-2026-10-29/batch-013-evidence.json)。手机238:371737／238:371738、Web311:379952／311:379956上下文与图片已查看。原账号验证使用手机号默认／真实能力约束和密码、邮箱quiet切换；补28/40标题、说明与固定标签字段。确认页改为真实账号56头像／16圆角、12间距、待关联实际方式事实行、说明、取消和底部明确确认。身份说明用合同实际账号名，不写示例手机号／猫图。确认按钮承接原共享confirmLink(true)，没有绕过原账号登录、本次scope验证与服务端关联；取消／顶部返回清除本次proof后回账号选择，不退出已验证账号。

31shared／47UI／94Web／430引用只读通过。两份语法因629MiB余量低于1024MiB门槛跳过，没有启动语法器或降低门槛；旧报告不能抵扣本版。语法待正常内存恢复后接续，未编译／类型检查／测试／启动／逐端视觉。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：批次001–013已记，继续恢复入口／真实回执与匿名协助合同；013低内存语法待接续。已知身份／部分受限合同缺口、其余消费者／后台与运行／视觉未完成。")
progress.write_text(text, encoding="utf-8", newline="\n")
for file in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(file)
    prefix = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE "
    old = next(line for line in text.splitlines() if line.startswith(prefix))
    new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 执行中；main／48a3cfd，未提交。[005–013进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：资料、实际任务返回、跨账号清理及原账号验证／真实关联确认源码已写。31shared／47UI／94Web／430引用通过；013两语法在629MiB低于1024门槛跳过，待正常内存接续。继续恢复入口／回执及全量余项；已知身份／部分受限合同缺口未实现。不编译／测试／启动／提交推送，无后台任务。" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–013源码分批已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，未提交 | shared／公共Compose／Web及必要合同与证据；不构建／测试／启动／自动提交推送 | [十三批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：原账号验证／关联确认源码已写，31shared／47UI／94Web／430引用通过；013两语法629MiB低于门槛跳过。继续恢复／回执、已知身份／受限合同与全量余项 |"
    (root / file).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
for file in ["docs/CURRENT_BASELINE.md", "docs/client/README.md"]:
    text = read(file).replace("005–012", "005–013").replace("已记十二批", "已记十三批")
    (root / file).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次013

- Codex，main／48a3cfd，保留全部在途成果；用户继续，执行中，未提交推送。
- [证据](../client/figma-implementation-2026-10-29/batch-013-evidence.json)：手机／Web四张高保真已读，原账号验证与真实身份／方式关联确认、quiet取消及固定确认操作已写；保留原scope验证与服务端权限，不新建本地验证标记。
- 31shared／47UI／94Web／430引用只读通过；两语法因629MiB低于1024门槛跳过，未启动语法器或重试，不计旧结果抵扣。当前源码指纹匹配，未编译／类型检查／测试／启动，继续恢复合同及全量余项，无后台任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "013", "source_files": len(files), "checkpoint_files": len(current["files"]), "grammar": "skipped_low_memory", "execution": "active"}))
