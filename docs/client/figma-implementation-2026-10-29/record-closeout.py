"""Preserve the stopped implementation and source-only evidence for commit."""
import hashlib
import json
from datetime import datetime, timezone, timedelta
from pathlib import Path

root = Path(__file__).resolve().parents[3]
folder = Path(__file__).resolve().parent
stamp = datetime.now(timezone(timedelta(hours=8))).isoformat(timespec="seconds")

def read(name):
    return (root / name).read_text(encoding="utf-8")

def write(name, text):
    (root / name).write_text(text, encoding="utf-8", newline="\n")

def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")

syntax = json.loads((folder / "batch-004-syntax.json").read_text(encoding="utf-8"))
checks = json.loads((folder / "batch-004-static.json").read_text(encoding="utf-8"))
assert len(syntax["files"]) == 10 and all(row.get("exit_code") == 0 for row in syntax["files"])
assert len(checks["checks"]) == 2 and all(row["exit_code"] == 0 for row in checks["checks"])
files = [row["file"] for row in syntax["files"]] + ["web-client/src/styles.css"]
hashes = {file: hashlib.sha256(read(file).replace("\r\n", "\n").encode()).hexdigest() for file in files}
assert all(hashes[row["file"]] == row["sha256_lf"] for row in syntax["files"])
baseline_only = [row["file"] for row in syntax["files"] if row["syntax"].get("whole_file_verified") is False]
assert len(baseline_only) == 2
evidence = {
    "id": "004", "updated": stamp,
    "status": "partial_shared_source_written_static_reviewed_stopped_by_user",
    "figma_file": "3OFOC3vu18Kx9sRPjummOC",
    "fresh_design_contexts_with_images_observed": ["56:81", "56:82", "56:84"],
    "text_action_readback_nodes": ["56:81", "56:82", "56:83", "56:84"],
    "scope": ["shared initial read failure fixed actions", "adoption filter empty results", "protected data removal after denied reads", "shared spacing and empty-state typography"],
    "design_boundary": "56:84 is optional location permission / manual city, not account authorization. Existing city input retained; no new location flow in this batch.",
    "static_report": "batch-004-static.json", "syntax_report": "batch-004-syntax.json",
    "syntax_files_without_diagnostics": 8,
    "files_with_matching_preexisting_parser_diagnostics": baseline_only,
    "source_hashes_lf": hashes,
    "remaining": ["quiet recovery/clear action parity and FeatureColumn default spacing review", "account and remaining consumer/backend state review", "all platform rendered visual and runtime acceptance"],
    "whole_state_complete": False, "runtime_verified": False,
    "type_checked": False, "tests_executed": False,
    "user_direction": "Stop development, commit and push existing results. No further feature implementation."
}
save(folder / "batch-004-evidence.json", evidence)
ledger = json.loads((folder / "state-progress.json").read_text(encoding="utf-8"))
assert not any(batch["id"] == "004" for batch in ledger["batches"])
ledger["batches"].append(evidence)
for state in ledger["consumer_states"]:
    if state["key"] in {"offline", "noResults", "contentUnavailable"}:
        state["source_review"] = "shared_contract_partially_reviewed_batch_004"
        state["reviewed_findings"].append("Batch 004 shared failure/empty actions and data visibility; not whole-state completion.")
    elif state["key"] == "permission":
        state["reviewed_findings"].append("Batch 004 design read only: optional location permission and manual city; existing city flow retained.")
ledger.update(updated=stamp, execution_status="stopped_by_user_commit_and_push_authorized", execution_marker_released=True,
              next_step="Development stopped by user. On a future explicit resume, review quiet recovery/clear actions, then account and remaining states. Runtime and per-platform visual acceptance remain unverified.")
save(folder / "state-progress.json", ledger)

name = "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = read(name)
lines = text.splitlines()
lines[2] = "任务 `AI-20261029-FIGMA-ALL-CODE`，2026-10-29，main／a6b3653 起点。用户要求全部严格落实并分批记录；现已明确停止开发，提交并推送现有成果。开发执行标记释放，无后台或定时续做。"
text = "\n".join(lines) + "\n"
section = """## 批次004：全局读取与空结果的共用合同（部分完成后停止）

读取Figma手机56:81／56:82／56:84高保真上下文及图片，另回读56:83文本与动作；[本批证据](figma-implementation-2026-10-29/batch-004-evidence.json)记录11份源码及边界。

- 原生根列表改用固定操作宿主，首读失败提供社区返回；筛选无结果提供调整、清空与回到领养列表。刷新失败保留已读内容，不覆盖有效任务主操作；底部动作不依赖Lazy列表项是否可见。
- Web首读错误卡注册固定社区返回，多卡仍只保留一处；弹窗与带缓存刷新不夺取父任务底栏。领养空结果可聚焦原筛选并清空条件。
- 读取被拒绝后收起受保护表单，清除偏好、订阅、表情、认证方式、资格、监护与旧通知成功回执等受限缓存；本机草稿和原未知请求继续保留。表单间距、列表行与空态文字作共用对齐。
- 56:84是可选定位权限与手填城市，现有城市输入继续保留；本批未新增定位流程，不能将其记作账号授权页完成。恢复／清空的quiet按钮、FeatureColumn默认间距、账号与其余状态尚待后续审查。

提交前只读核对：[两项静态脚本](figma-implementation-2026-10-29/batch-004-static.json)通过；[10份语法读取](figma-implementation-2026-10-29/batch-004-syntax.json)正常退出，8份无诊断，ConsumerApp／MobileFeatureStore仍有与HEAD一致的旧语法器诊断及局部读取，不宣称全文件语法或类型检查通过。先前内存不足与PowerShell启动失败未计通过；内存恢复后执行此次只读核对。未编译、测试、lint或启动应用。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
paragraph = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(paragraph, "当前恢复点：批次001–003已有源码／静态记录，批次004共用读取／空态合同部分已写并记录；用户已停止开发。后续仅在用户明确恢复时，先审查quiet恢复动作及默认间距，再推进账号、其余业务与后台。748全量未完成，不能把共享样式或文件存在当作完成。")
paragraph = next(line for line in text.splitlines() if line.startswith("未运行编译，遵循"))
text = text.replace(paragraph, "未运行编译，遵循[全局不自行编译规则](C:/Users/ttkk0/AGENTS.md)。本次未构建、类型检查、测试、lint、启动应用／浏览器／设备、迁移或部署；运行与逐端视觉验收未验证。用户已明确授权提交和推送现有成果；本记录随成果提交，实际同步以Git历史、工作树及本轮推送回执为准。")
text += f"\n停止开发并授权提交／推送：{stamp}。保留未完成项，释放开发执行标记，无后台执行或自动续做。\n"
write(name, text)

name = "docs/ai/CONTEXT.md"
text = read(name)
old = next(line for line in text.splitlines() if line.startswith("- 2026-10-29T17:54:06"))
new = f"- {stamp} `AI-20261029-FIGMA-ALL-CODE` 用户明确停止开发、提交并推送现有成果，开发执行标记已释放：[四批进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)。批次001–003源码／静态已记；004全局读取／空态与受限缓存收起部分已写，2只读脚本通过，10语法读取含2份基线旧诊断对照。748及后台全量未完成，恢复先审quiet动作与默认间距，再推进账号／其余状态。未编译／类型检查／测试／启动，无后台或自动续做；成果随本记录所在提交保存，推送结果以Git回执为准。"
write(name, text.replace(old, new, 1))
name = "docs/ai/TASKS.md"
text = read(name)
old = next(line for line in text.splitlines() if line.startswith("| AI-20261029-FIGMA-ALL-CODE "))
new = f"| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 用户停止开发；现有成果提交交接，开发执行标记释放 | Codex / {stamp}；main，本记录所在成果提交 | 现有源码、设计／审查证据、四批实施记录与自身跟踪；用户明确授权提交推送；无后续开发／构建／测试／启动 | [分批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：001–003源码／静态已记，004读取／空态合同部分已写并静态核对；748与后台全量未完成，运行均未验。后续需用户明确恢复，先审quiet动作／默认间距与账号；无后台或自动续做，实际同步以Git回执为准 |"
write(name, text.replace(old, new, 1))
name = "docs/ai/TASK_LOG.md"
text = read(name)
entry = f"""## AI-20261029-FIGMA-ALL-CODE / {stamp} / 停止开发与提交交接

- 来源为本轮实际操作；Codex，main／a6b3653起点。用户澄清“停止开发，提交并推送现有成果”，已停止业务实现，仅保存交接并做提交前只读核对。
- 批次004部分完成共用首读失败固定操作、领养无结果筛选动作、失权受限缓存收起、间距／文字；定位权限稿仅回读，未新增定位流程。[批次证据](../client/figma-implementation-2026-10-29/batch-004-evidence.json)及[完整进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)保留未完成项。
- 2份只读脚本通过；10份语法读取正常退出，8份无诊断、2份基线旧诊断／片段对照。未编译、类型检查、测试、lint、启动、迁移或部署；748与后台全量未完成。
- 提交范围包含此前协作40稿设计细化／截图、代码完整性审查、四批源码与证据。既有3个本地提交随用户授权的main推送同步；本次fetch后远端仍为35914a0，无新提交需要合并，旧stash f0714b5保留。未强推或改历史。
- 本记录随成果提交；提交哈希与推送结果以Git回执为准。执行状态：用户停止开发，开发执行标记已释放；无后台或自动续做。后续必须等用户明确恢复，先审quiet恢复动作／默认间距，再推进账号及其余状态。

"""
write(name, text.replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1))
print(json.dumps({"updated": stamp, "batch": "004", "source_files": len(hashes), "clean_syntax_reads": 8, "baseline_diagnostic_comparisons": 2, "execution_marker_released": True}, ensure_ascii=False))
