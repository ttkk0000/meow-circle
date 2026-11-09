"""Record the purpose-verified binding/unbinding confirmation source batch."""
import hashlib
import json
from pathlib import Path
folder = Path(__file__).resolve().parent
root = folder.parents[2]
def digest(file):
    value = (root / file).read_text(encoding="utf-8")
    assert not value.startswith("\ufeff"), file
    return hashlib.sha256(value.replace("\r\n", "\n").encode()).hexdigest()
def save(file, value):
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
def replace_line(file, prefix, line):
    path = root / file
    value = path.read_text(encoding="utf-8")
    old = next(row for row in value.splitlines() if row.startswith(prefix))
    path.write_text(value.replace(old, line, 1), encoding="utf-8", newline="\n")
checks = json.loads((folder / "batch-019-checks.json").read_text(encoding="utf-8"))
syntax = json.loads((folder / "batch-019-syntax.json").read_text(encoding="utf-8"))
for report in [checks, syntax]:
    assert all(row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"]) for row in report["files"])
assert len(syntax["files"]) == 4 and all(not row["syntax"]["errors"] for row in syntax["files"])
batch = {
    "id": "019", "updated": "2026-10-30", "status": "source_written_static_and_grammar_reviewed_runtime_unverified",
    "high_fidelity_design_nodes": ["238:371747", "311:379992", "238:371748", "311:379996"],
    "scope": ["dedicated binding confirmation after purpose verification", "dedicated unlink confirmation with actual associated methods"],
    "states": ["authBindConfirm", "authUnbindConfirm"],
    "source_hashes_lf": {row["file"]: digest(row["file"]) for row in syntax["files"]},
    "contract_read_hashes_lf": {file: digest(file) for file in ["internal/identity/service.go", "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/IdentityModels.kt", "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/SensitiveActionStore.kt"]},
    "static_report": "batch-019-checks.json", "syntax_reports": ["batch-019-syntax.json"],
    "static_counts": {"shared_sources": 33, "public_ui": 52, "web_sources": 99, "web_imports": 450},
    "notes": [
        "Four phone/Web high-fidelity contexts and screenshots inspected. Confirmation pages use actual account identity (dynamic avatar or existing neutral placeholder), method facts, notice, cancel/keep action and persistent explicit footer. No Figma sample identity/phone or ephemeral asset is embedded.",
        "Native ReadyToSubmit and Web actual reauthentication grant select the dedicated confirmation; completing verification never submits. Explicit footer confirmation uses the same scoped grant, version, client request ID and server endpoint. Cancelling drops the current review without changing a binding. Replacement is labeled update rather than falsely describing an additional association.",
        "Unlink facts come from actual auth/methods: password_set, masked phone and active linked identities, excluding the selected identity. A missing selected identity blocks confirmation; no local verified flag or eligibility is invented. Server last-usable-method predicate remains authoritative and the016 named last_login_method denial still routes to its own guard task.",
        "Confirmed mutations retain existing durable unknown-result guards and account logout behavior. Failed/unknown requests do not become locally successful. Four whole grammar reads and both path/asset scripts pass; button signatures and footer secondary style were read at actual callsites. No type checking, compilation, tests, app/browser/device launch or runtime/visual acceptance."
    ],
    "remaining": ["change-phone/current-phone flow fidelity", "remaining auth/consumer/backend states", "typed partial-restriction contract", "runtime and visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "019" for row in ledger["batches"])
save(folder / "batch-019-evidence.json", batch)
ledger["batches"].append(batch)
native = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthBindingPages.kt"
web = "web-client/src/features/AuthBinding.tsx"
for row in ledger["consumer_states"]:
    if row["key"] in batch["states"]:
        row["source_review"] = "dedicated_source_static_grammar_reviewed_runtime_unverified"
        row["reviewed_findings"].append("Batch019: actual identity/method facts and dedicated final binding/unlink tasks after scoped verification; explicit footer only, no automatic mutation. Static/grammar reviewed, no runtime acceptance.")
        for platform in ["android", "ios", "desktop"]: row["sources"][platform] = native
        row["sources"]["web"] = web
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False, next_step="Continue change-phone/current-phone flow fidelity and remaining states; all runtime/visual and partial-restriction contract remain open.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] in batch["states"]:
        for platform in ["android", "ios", "desktop"]: row[platform + "_source"] = native
        row["web_source"] = web
        row["implementation_status"] = "dedicated_source_static_grammar_reviewed_runtime_unverified"
        row["note"] = "批次019：真实方式事实与关联／解除专用确认页；用途验证后明确点击、原版本／编号／服务端最后方式保护保留。仅静态／语法，未运行视觉。"
        nodes = ["238:371747", "311:379992"] if row["key"] == "authBindConfirm" else ["238:371748", "311:379996"]
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + nodes))
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items(): current["files"][file] = {"batch": "019", "sha256_lf": sha}
for file, row in current["files"].items(): assert digest(file) == row["sha256_lf"], file
save(current_file, current)
progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
value = progress.read_text(encoding="utf-8").replace("当前批次018已记录", "当前批次019已记录")
section = """## 批次019：关联与解除的专用确认页

2026-10-30：[四份源码及证据](figma-implementation-2026-10-29/batch-019-evidence.json)。手机／Web四稿上下文及图片已看。用途验证后的关联确认采用真实账号56头像位、方式／目标事实与固定底部明确确认；解除页显示真实选中方式及其余服务返回的密码／掩码手机／关联方式，保留关联为次按钮、确认解除为底部次按钮。缺少选中方式时禁用，最终可否移除仍由服务端原子守卫决定；不声称其他方式都已验证。

确认继续使用原用途凭证、版本、编号与未知结果保护；验证不会自动提交，取消不改绑定。33shared／52UI／99Web／450引用及四份完整语法读取通过；未类型／编译／测试／启动／运行视觉。继续换号／当前号及全量余项。

"""
value = value.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(row for row in value.splitlines() if row.startswith("当前恢复点："))
value = value.replace(old, "当前恢复点：批次001–019已记，继续换号／当前手机号与其余认证／消费者／后台。015–017语法待读已由018补齐；部分受限合同与全量运行／视觉未完成。")
progress.write_text(value, encoding="utf-8", newline="\n")
replace_line("docs/ai/CONTEXT.md", "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`", "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 执行中；main／48a3cfd，未提交。[005–019进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：短信／回执过期、账号守卫及关联／解除专用最终确认已写；33shared／52UI／99Web／450引用与019四份完整语法通过。015–017待读由018补齐。未编译／类型检查／测试／启动。继续换号／当前号与全量余项，部分受限合同／运行视觉未完成，无后台任务。")
replace_line("docs/ai/TASKS.md", "| AI-20261029-FIGMA-ALL-CODE ", "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–019分批源码已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，未提交 | shared／公共Compose／Web、必要合同与逐批证据；不构建／测试／启动／自动提交推送 | [十九批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：用途验证后专用关联／解除明确确认；33shared／52UI／99Web／450引用及四份完整语法；继续换号／当前号及全量余项 |")
replace_line("docs/CURRENT_BASELINE.md", "更新：", "更新：2026-10-30（Asia/Shanghai）。既有成果已在48a3cfd提交并推送；按用户要求继续全部设计实现，新增批次005–019尚未提交。本页取代更早入口数量，带日期交接保留当时结果。")
replace_line("docs/CURRENT_BASELINE.md", "当前进度：", "当前进度：[严格按Figma分批实施](client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)已记十九批；最新关联／解除专用最终确认。33shared／52公共UI、99Web／450本地引用通过；019四份完整语法通过，015–017旧待读由018补齐。未编译／类型检查／测试／运行／全量视觉。")
replace_line("docs/client/README.md", "2026-10-30：", "2026-10-30：[批次005–019](FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)续作认证／账号安全与用途验证后关联／解除专用确认；33shared／52公共UI／99Web／450引用及019四份完整语法通过，旧待读由018补齐。未类型／编译／测试／启动／运行视觉；继续换号／当前号与全量余项。")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次019

- Codex，main／48a3cfd，用户继续，执行中，保留全部未提交成果。
- [四份源码证据](../client/figma-implementation-2026-10-29/batch-019-evidence.json)：手机／Web四稿已看；真实账号／方式与关联／解除专用最终确认，验证后不自动提交；取消不改绑定，原版本／编号／服务端最后方式与未知结果保护保留。
- 33shared／52UI／99Web／450引用、四份完整语法读取通过；未类型／编译／测试／启动／运行视觉。继续换号／当前号与全量余项，无后台任务，未提交推送。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "019", "source_files": 4, "checkpoint_files": len(current["files"]), "execution": "active"}))
