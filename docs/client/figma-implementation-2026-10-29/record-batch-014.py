"""Record recovery assistance and the resumed provider grammar reads."""
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

reports = ["batch-014-syntax.json", "batch-014-syntax-followup.json", "batch-014-syntax-final.json", "batch-013-syntax-followup.json", "batch-013-syntax-final.json"]
latest = {}
for file in reports:
    latest.update({row["file"]: row for row in json.loads((folder / file).read_text(encoding="utf-8"))["files"]})
assert len(latest) == 10
for file, row in latest.items():
    assert row["exit_code"] == 0 and row["sha256_lf"] == digest(file), file
fragments = json.loads((folder / "batch-014-fragments.json").read_text(encoding="utf-8"))
checks = json.loads((folder / "batch-014-checks.json").read_text(encoding="utf-8"))
for report in [fragments, checks]:
    assert all(row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"]) for row in report["files"])
batch = {
    "id": "014", "updated": "2026-10-30", "status": "source_written_static_reviewed_runtime_pending",
    "high_fidelity_design_nodes": ["238:371753", "238:371754", "311:380016", "311:380020"],
    "scope": ["dedicated account-access assistance form and actual receipt", "restricted original-request lookup and supplement", "provider grammar follow-up after normal memory recovery"],
    "source_hashes_lf": {file: digest(file) for file in latest}, "static_report": "batch-014-checks.json", "syntax_reports": reports + ["batch-014-fragments.json"],
    "syntax_counts": {"files": 10, "whole_files_without_diagnostics": 9, "same_baseline_diagnostic_signatures": 1, "clean_changed_fragments": 3},
    "static_counts": {"shared_sources": 32, "public_ui": 48, "web_sources": 95, "web_imports": 435},
    "states": ["authRecovery", "authRecoveryPending"],
    "notes": [
        "Four phone/Web high-fidelity context and images were inspected. Initial transport errors for the entry contexts were retried once successfully. The form uses original-account/contact/explanation fixed labels and placeholders, existing 16px spacing/radius, and fixed 48px submit. Actual receipt/status facts and the 56px supplement row replace prototype receipt values.",
        "The existing anonymous safety assistance contract carries a clearly titled account-access assistance message. No new recovery grant, identity ownership, account merge, password reset, confirmed-contact flag or local eligibility flag is manufactured. Operator review and actual account recovery remain separate from receiving a ticket.",
        "The shared AuthRecoveryStore owns native request/owner state and keeps input/token in memory. Native secure restricted references and Web private capability records retain only the original request/query capability before submission; account/contact/explanation are not placed in ordinary routes, logs or durable draft values.",
        "Unknown results disable resend/editing and use the original request lookup. A missing/old lookup does not prove failure. Actual success clears input; the receipt keeps the restricted token and exact server version for the existing supplementary-material flow. Leaving or changing accounts cannot promote a late response to a new owner.",
        "Existing Compose and Web field primitives gained optional placeholders; prior callers retain their default behavior. Native field argument texts were read to confirm existing optional arguments use names and the trailing icon remains last.",
        "Normal headroom returned to 1332–1458MiB during recovery reads. The two provider files skipped in 013 were then read: native clean; one JSX M&D title and the new recovery placeholder were changed to expression strings and reread cleanly. Initial skip/diagnostic reports remain. Ten final grammar reads comprise nine clean whole files and MobileStore with the same HEAD diagnostic signatures; three changed fragments are clean. No type checker, build, test, browser, device or business endpoint was executed."
    ],
    "remaining": ["known-identity presentation and typed partial restrictions", "remaining auth/consumer/backend states", "actual recovery operations and all platform runtime/visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "014" for row in ledger["batches"])
save(folder / "batch-014-evidence.json", batch)
ledger["batches"].append(batch)
for row in ledger["consumer_states"]:
    if row["key"] in batch["states"]:
        row["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        row["reviewed_findings"].append("Batch 014: dedicated anonymous assistance form/actual receipt, original request lookup and restricted supplement. A ticket never restores access or merges accounts; actual handling and runtime acceptance remain open.")
        for platform in ["android", "ios", "desktop"]:
            row["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthRecoveryPages.kt"
        row["sources"]["web"] = "web-client/src/features/AuthRecovery.tsx"
        row["source_variants"] = {"shared_owner": "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/AuthRecoveryStore.kt"}
    elif row["key"] in ["authLinkExisting", "authLinkConfirm"]:
        row["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        row["reviewed_findings"].append("Batch 014 follow-up: normal memory headroom restored; both 013 provider files now have current clean grammar reads, JSX title expression fixed. The original low-memory skip is retained as historical evidence.")
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue known-identity and remaining auth/consumer/backend states; retain typed partial restriction and all runtime/visual gaps.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] in batch["states"]:
        for platform in ["android", "ios", "desktop"]:
            row[platform + "_source"] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthRecoveryPages.kt"
        row["web_source"] = "web-client/src/features/AuthRecovery.tsx"
        row["implementation_status"] = "dedicated_source_static_reviewed_runtime_unverified"
        row["note"] = "批次014：专用恢复协助／真实回执／原请求核对及受限补充源码已写；复用匿名协助，不直接恢复权限或合并账号。未编译或运行／逐端视觉验收。"
        nodes = ["238:371753", "311:380016"] if row["key"] == "authRecovery" else ["238:371754", "311:380020"]
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + nodes))
    elif row["key"] in ["authLinkExisting", "authLinkConfirm"]:
        row["implementation_status"] = "dedicated_source_static_reviewed_runtime_unverified"
        row["note"] += " 批次014内存恢复后已补语法读取，均无诊断；不计运行验收。"
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items():
    current["files"][file] = {"batch": "014", "sha256_lf": sha}
for file, row in current["files"].items():
    assert digest(file) == row["sha256_lf"], file
save(current_file, current)

progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = progress.read_text(encoding="utf-8").replace("当前批次013已记录", "当前批次014已记录")
section = """## 批次014：恢复协助、真实回执与原请求补充

2026-10-30：[十份当前源码指纹及证据](figma-implementation-2026-10-29/batch-014-evidence.json)。手机238:371753／238:371754、Web311:380016／311:380020均读取上下文并查看截图；入口首次传输错误后各单次重试成功。新增shared AuthRecoveryStore、公共AuthRecoveryPages、Web AuthRecovery，登录帮助接专用原账号／联系／说明表单与真实回执。复用现有匿名协助队列，原账号和联系方式只作核对线索，不创建恢复权限、验证标记或合并操作。收到申请不代表恢复账号，后续人工核对单列。

明确提交前先保存原请求受限查询引用，不把申请正文写普通路由／日志／持久草稿值；未知结果只按原请求读取，未查到不等于失败。实际成功清空输入；补充入口沿用原受限凭证和真实版本进入既有材料流程。两端固定标签字段增加可选placeholder；旧调用默认行为保留。返回、账号切换与迟到响应均按当前owner／epoch守卫。

32shared／48UI／95Web／435引用静态通过。内存恢复到1332–1458MiB，013两文件已补读；两个JSX含M&D文本改表达式后复读通过。10份最终语法9完整无诊断／MobileStore同HEAD旧诊断，3变更片段无诊断。013低内存和初次诊断保留，不计编译／类型／测试／运行／逐端视觉。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：批次001–014已记，继续已知账号与其余认证／消费者／后台；013低内存语法已在014补读。恢复协助可收真实回执，不等于恢复账号权限；部分受限合同和全量运行／视觉待办保留。")
progress.write_text(text, encoding="utf-8", newline="\n")
for file in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(file)
    prefix = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE "
    old = next(line for line in text.splitlines() if line.startswith(prefix))
    new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 执行中；main／48a3cfd，未提交。[005–014进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：专用资料／实际任务返回／跨账号清理／关联确认与匿名恢复协助、真实回执／受限补充源码已写。32shared／48UI／95Web／435引用通过；10最终语法9完整／1旧诊断对照及3片段，013低内存已补读。继续已知账号及全量余项，部分受限合同与运行／视觉未验。不编译／测试／启动／提交推送，无后台任务。" if file.endswith("CONTEXT.md") else "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–014源码／静态分批已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，未提交 | shared／公共Compose／Web及必要合同与逐批证据；不构建／测试／启动／自动提交推送 | [十四批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：恢复协助／真实回执与受限补充已写，32shared／48UI／95Web／435引用；10语法9完整／1旧诊断与3片段，013低内存已补读。继续已知身份与全量余项，部分受限合同及运行／视觉保留 |"
    (root / file).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
for file in ["docs/CURRENT_BASELINE.md", "docs/client/README.md"]:
    text = read(file).replace("005–013", "005–014").replace("已记十三批", "已记十四批").replace("94份Web源码与47份公共UI，31份shared", "95份Web源码与48份公共UI，32份shared").replace("31 / 47", "32 / 48").replace("31shared／47公共UI／94Web／430引用", "32shared／48公共UI／95Web／435引用")
    (root / file).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次014

- Codex，main／48a3cfd，保留全部在途源码；用户再次继续，执行中，未提交推送。
- [证据](../client/figma-implementation-2026-10-29/batch-014-evidence.json)：四张手机／Web高保真已读，专用恢复协助表单／真实回执／受限补充与原请求只读核对；复用匿名协助，不把回执当恢复账号权限，不自动合并。输入不进路由／日志／普通草稿，私密查询能力沿用现有存储。
- 32shared／48UI／95Web／435引用通过；10最终语法9完整／1同HEAD旧诊断、3片段无诊断。内存正常恢复后013两语法已补读，两个JSX文本表达式已修正，旧跳过／失败报告保留。当前源码指纹一致，继续已知账号及其余全量；未编译／类型检查／测试／启动，无后台任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "014", "source_files": len(latest), "checkpoint_files": len(current["files"]), "execution": "active"}))
