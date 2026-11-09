"""Record receipt-bound expiry/delivery help and recovered grammar reads only."""
import hashlib
import json
from pathlib import Path

folder = Path(__file__).resolve().parent
root = folder.parents[2]
def read(file):
    return (root / file).read_text(encoding="utf-8")
def digest(file):
    value = read(file)
    assert not value.startswith("\ufeff"), file
    return hashlib.sha256(value.replace("\r\n", "\n").encode()).hexdigest()
def save(file, value):
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
def replace_line(file, prefix, line):
    value = read(file)
    old = next(row for row in value.splitlines() if row.startswith(prefix))
    (root / file).write_text(value.replace(old, line, 1), encoding="utf-8", newline="\n")

checks = json.loads((folder / "batch-018-checks.json").read_text(encoding="utf-8"))
syntax = json.loads((folder / "batch-018-syntax.json").read_text(encoding="utf-8"))
fragments = json.loads((folder / "batch-018-fragments.json").read_text(encoding="utf-8"))
for report in [checks, syntax, fragments]:
    assert all(row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"]) for row in report["files"])
assert len(syntax["files"]) == 19 and len(fragments["files"]) == 3
baseline = [row for row in syntax["files"] if row["syntax"].get("errors")]
assert len(baseline) == 3 and all(row["syntax"]["baseline_same_error_signatures"] for row in baseline)
for row in fragments["files"]:
    assert all(not part["errors"] for part in row["fragment_syntax"]["fragments"])
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "018" for row in ledger["batches"])
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
files = [file for file, row in current["files"].items() if digest(file) != row["sha256_lf"]]
assert len(files) == 9
assert set(files) <= {row["file"] for row in syntax["files"]}
pending = json.loads((folder / "batch-017-evidence.json").read_text(encoding="utf-8"))["grammar_pending_source_hashes"]
assert set(pending) <= {row["file"] for row in syntax["files"]}
batch = {
    "id": "018", "updated": "2026-10-30", "status": "source_written_static_and_grammar_reviewed_runtime_unverified",
    "high_fidelity_design_nodes": ["238:371724", "311:379900", "238:371723", "311:379896"],
    "scope": ["SMS delivery help task", "actual receipt-bound OTP expiry task", "auth transient ownership and owner-transition render guards", "recover pending015–017 grammar reads"],
    "source_hashes_lf": {file: digest(file) for file in files},
    "static_report": "batch-018-checks.json", "syntax_reports": ["batch-018-syntax.json", "batch-018-fragments.json"],
    "syntax_counts": {"current_files_read": 19, "whole_clean": 16, "same_head_diagnostics": 3, "fragment_files": 3, "fragments_clean": sum(len(row["fragment_syntax"]["fragments"]) for row in fragments["files"]), "prior_pending_files_recovered": len(pending)},
    "grammar_pending_source_hashes": {},
    "static_counts": {"shared_sources": 33, "public_ui": 51, "web_sources": 98, "web_imports": 446},
    "states": ["authSmsDelayed", "authCodeExpired"],
    "notes": [
        "Four phone/Web contexts and screenshots inspected. Delivery help retains the actual phone and purpose, offers return-to-input and existing recovery assistance, and sends no verification or replacement request on navigation.",
        "Expiry uses a validated positive actual OTP receipt ID and parsed expires_at. Native binds its transient form to a unique delivery metadata key; Web binds address/mode and the actual receipt deadline. Local delivery keys are presentation metadata, never credentials. Expiry is sticky for that receipt and is not inferred from generic wrong-code responses.",
        "Retry is an explicit action, respects actual retry_after_seconds, clears the previous code, and awaits a fresh actual receipt before verification is enabled. The native countdown and Web interval terminate when both cooldown and this delivery expire; old delivery callbacks cannot alter a replacement receipt. RFC3339 fractional seconds are normalized to browser milliseconds without removing timezone.",
        "Known-provider transient fields now use the actual retained auth scope. Web root/provider render guards hide old-owner controls during a foreign transition while allowing their expected login transition to finish. No persistent remembered identity or private avatar fetch was added.",
        "Path/asset checks pass. Normal memory headroom allowed19 isolated current grammar reads:16 whole clean and3 unchanged HEAD diagnostic signatures; MobileApi/MobileStore/MobileFeatureStore changed fragment reads are clean. The earlier18 pending files and015 final provider/icon reads are covered. One fragment-reader boundary was corrected from a nonexistent marker before successful reads; this is not an application result.",
        "No build, type checker, tests, app/browser/device launch or visual/runtime acceptance performed. Server generic wrong/expired/locked-code behavior is unchanged. Partial restriction policy and remaining auth/consumer/backend states remain open."
    ],
    "remaining": ["binding/unbinding/change-phone task fidelity", "remaining auth/consumer/backend states", "typed partial-restriction contract", "all runtime and visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
save(folder / "batch-018-evidence.json", batch)
ledger["batches"].append(batch)
for row in ledger["consumer_states"]:
    if row["key"] in batch["states"]:
        row["source_review"] = "dedicated_source_static_grammar_reviewed_runtime_unverified"
        row["reviewed_findings"].append("Batch018: dedicated SMS delayed/actual receipt-expired task; original address/purpose preserved, explicit fresh receipt required, no generic-error guessing. Static/grammar only.")
        for platform in ["android", "ios", "desktop"]:
            row["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthCodePages.kt"
        row["sources"]["web"] = "web-client/src/features/AuthCodePages.tsx"
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue binding/unbinding/change-phone task fidelity and remaining states;015–017 pending grammar recovered in018, all runtime/visual and partial-restriction contract remain open.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] in batch["states"]:
        for platform in ["android", "ios", "desktop"]:
            row[platform + "_source"] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthCodePages.kt"
        row["web_source"] = "web-client/src/features/AuthCodePages.tsx"
        row["implementation_status"] = "dedicated_source_static_grammar_reviewed_runtime_unverified"
        row["note"] = "批次018：短信延迟／真实回执过期专用页，地址用途与倒计时绑定，重发明确操作并清旧码；静态／语法已读，未类型／运行／视觉验收。"
        nodes = ["238:371724", "311:379900"] if row["key"] == "authSmsDelayed" else ["238:371723", "311:379896"]
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + nodes))
trace["updated"] = "2026-10-30"
save(trace_file, trace)
for file in files: current["files"][file] = {"batch": "018", "sha256_lf": digest(file)}
for file, row in current["files"].items(): assert digest(file) == row["sha256_lf"], file
save(current_file, current)
progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
value = progress.read_text(encoding="utf-8").replace("当前批次017已记录", "当前批次018已记录")
section = """## 批次018：短信延迟与真实回执过期

2026-10-30：[九份当前源码与证据](figma-implementation-2026-10-29/batch-018-evidence.json)。手机／Web四稿上下文及截图已看。延迟页保留当前号码与用途，返回填写／既有恢复协助不自动发请求。过期页只取有效实际发送回执的expires_at及本次表单绑定；不把服务端通用错码拒绝猜成过期。重发遵守冷却、清旧码、等待真实新回执再验证；倒计时在本次回执和冷却都结束后停止，旧回执不能改写新回执。

修正旧身份授权输入的保留scope，以及Web账号切换期间的旧内容渲染守卫。路径／资源33shared／51UI／98Web／446引用通过；19份当前语法16完整无诊断＋3同HEAD旧诊断，三份大文件的7个变更片段无诊断，补齐015–017此前内存待读。仅源码／语法；未编译／类型检查／测试／启动／运行视觉。

"""
value = value.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(row for row in value.splitlines() if row.startswith("当前恢复点："))
value = value.replace(old, "当前恢复点：批次001–018已记，继续关联／解绑／换号及其余认证／消费者／后台。015–017语法待读已由018当前读取补齐；部分受限合同与全量运行／视觉未完成。")
progress.write_text(value, encoding="utf-8", newline="\n")
replace_line("docs/ai/CONTEXT.md", "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`", "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 执行中；main／48a3cfd，未提交。[005–018进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：短信延迟／真实回执过期、旧身份scope和账号切换渲染守卫已写；33shared／51UI／98Web／446引用通过。19当前语法16完整＋3同HEAD旧诊断，三文件7片段无诊断，015–017待读补齐。未编译／类型检查／测试／启动。继续关联／解绑／换号及全量余项，受限合同／运行视觉未完成，无后台任务。")
replace_line("docs/ai/TASKS.md", "| AI-20261029-FIGMA-ALL-CODE ", "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–018分批源码已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，未提交 | shared／公共Compose／Web、必要合同与逐批证据；不构建／测试／启动／自动提交推送 | [十八批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：短信延迟／真实回执过期、scope／账号切换守卫；33shared／51UI／98Web／446引用；19当前语法及7片段已读，旧待读补齐。继续关联／解绑／换号及全量余项 |")
replace_line("docs/CURRENT_BASELINE.md", "更新：", "更新：2026-10-30（Asia/Shanghai）。既有成果已在48a3cfd提交并推送；本次按用户要求继续全部设计实现，新增批次005–018尚未提交。本页取代更早入口数量，带日期交接保留当时结果。")
replace_line("docs/CURRENT_BASELINE.md", "当前进度：", "当前进度：[严格按Figma分批实施](client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)已记十八批；最新短信延迟／真实回执过期与账号scope／渲染守卫。33份shared／51份公共UI、98份Web／446本地引用；路径／资源通过。19当前语法16完整无诊断＋3同HEAD旧诊断，三大文件7片段无诊断，015–017旧待读已补齐。未编译／类型检查／测试／运行／全量视觉。")
replace_line("docs/client/README.md", "2026-10-30：", "2026-10-30：[批次005–018](FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)续作认证／账号安全及短信延迟／真实回执过期；33shared／51公共UI／98Web／446引用通过。19当前语法16完整＋3同HEAD旧诊断，三文件7片段无诊断，015–017旧待读补齐。未编译／类型检查／测试／启动／运行视觉；继续关联／解绑／换号与全量余项。")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次018

- Codex，main／48a3cfd，保留未提交成果；用户继续，执行中，无提交推送。
- [证据](../client/figma-implementation-2026-10-29/batch-018-evidence.json)：手机／Web四稿已看，短信延迟／真实回执过期、scope与账号切换渲染守卫已写；新发送明确操作、用途地址绑定，泛化错码不猜过期。
- 33shared／51UI／98Web／446引用通过；19当前语法16完整＋3同HEAD旧诊断，三文件7片段无诊断，补齐015–017待读。未编译／类型检查／测试／启动；继续关联／解绑／换号及全量余项，无后台任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "018", "source_files": len(files), "checkpoint_files": len(current["files"]), "grammar_pending": 0, "execution": "active"}))
