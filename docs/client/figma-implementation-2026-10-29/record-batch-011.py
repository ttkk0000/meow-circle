"""Record the optional-profile source batch without promoting runtime acceptance."""
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
    value = read(file)
    assert not value.startswith("\ufeff"), file
    return hashlib.sha256(value.replace("\r\n", "\n").encode()).hexdigest()

reports = ["batch-011-syntax.json", "batch-011-syntax-followup.json", "batch-011-syntax-final.json"]
latest = {}
for name in reports:
    latest.update({row["file"]: row for row in json.loads((folder / name).read_text(encoding="utf-8"))["files"]})
assert len(latest) == 15
fragments = json.loads((folder / "batch-011-fragments.json").read_text(encoding="utf-8"))
assert len(fragments["files"]) == 3 and all(row["exit_code"] == 0 for row in fragments["files"])
for row in fragments["files"]:
    assert row["sha256_lf"] == digest(row["file"])
for file, row in latest.items():
    assert row["sha256_lf"] == digest(file)
    if file.endswith("AndroidPhotoPicker.kt"):
        old = next(r for r in fragments["files"] if r["file"] == file)
        assert old["fragment_syntax"]["baseline_same_error_signatures"]
    else:
        assert row["exit_code"] == 0
checks = json.loads((folder / "batch-011-checks.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in checks["files"])
files = [*latest, "web-client/src/styles.css"]
batch = {
    "id": "011", "updated": "2026-10-30", "status": "source_written_static_reviewed_runtime_pending",
    "high_fidelity_design_nodes": ["238:371736", "311:379948"],
    "scope": ["dedicated optional nickname/avatar page", "save/skip/original-task return", "one-click native photo-picker trigger", "versioned write and uncertain-result readback"],
    "source_hashes_lf": {file: digest(file) for file in files}, "static_report": "batch-011-checks.json",
    "syntax_reports": reports + ["batch-011-fragments.json"],
    "syntax_counts": {"files": 15, "whole_files_without_diagnostics": 12, "same_baseline_diagnostic_signatures": 3, "clean_changed_fragments": 5},
    "static_counts": {"shared_sources": 30, "public_ui": 47, "web_sources": 93, "web_imports": 427},
    "notes": [
        "Only an actual server-created account enters optional setup. The nickname/avatar/quiet skip/fixed save layout matches the inspected phone and Web context; identity imagery stays dynamic, and original local chevrons are reused.",
        "Setup stays inside the auth task. Saving or skipping returns to the actual login-result page, and final continuation uses the original feature/browser route; no generic editor navigation or new history entry replaces the pending task.",
        "Selecting an avatar only prepares/previews the selected image. Explicit save uploads it under the same in-memory idempotency key and then sends the exact current profile version, keeping biography/theme/background unchanged.",
        "Readback requires matching owner, newer version and exact nickname/avatar/preserved fields. An ambiguous write disables editing/resending and offers a read-only check. An unchanged read does not prove the write failed. Skip cannot retract a request already sent.",
        "Native AuthProfileStore owns transient business state, guards owner changes, serializes read/write, and disposes selected bytes. All three platform pickers accept a custom row trigger while retaining their original processing/default-button behavior. Preparation blocks save/back until it finishes.",
        "The server still enforces current phone/interaction eligibility for profile changes. Denial preserves input and allows skip; no local qualification flag, automatic verification or policy bypass was added.",
        "Fifteen final grammar reads comprise twelve whole files without diagnostics and three old-diagnostic comparisons (MobileStore, MobileApi, AndroidPhotoPicker); five changed fragments are clean. Initial new-store catch diagnostics were resolved by joining the catch layout and read cleanly. The Android old catch diagnostic remains and is explicitly compared to HEAD.",
        "The source uses existing profile/media contracts and real response data. Builds, type checking, tests, native pickers, browser operation and all platform visual acceptance have not run."
    ],
    "states": ["authProfileOptional"],
    "remaining": ["other auth known/restricted/intent/link/recovery states", "remaining consumer/backend families", "platform compilation/runtime/visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False,
}
save(folder / "batch-011-evidence.json", batch)
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "011" for row in ledger["batches"])
ledger["batches"].append(batch)
for state in ledger["consumer_states"]:
    if state["key"] == "authProfileOptional":
        state["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        state["reviewed_findings"].append("Batch 011 replaces the generic editor entry with dedicated owner-scoped optional setup, explicit save/skip, readback and original-task continuation. Server qualification enforcement and all runtime/visual checks remain open.")
        for platform in ["android", "ios", "desktop"]:
            state["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthProfilePages.kt"
        state["sources"]["web"] = "web-client/src/features/AuthProfile.tsx"
        state["source_variants"] = {"shared_owner": "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/AuthProfileStore.kt"}
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue remaining known/restricted/intent/link/recovery auth states and consumer/backend families. Preserve incomplete/runtime flags; no automatic commit or execution.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] == "authProfileOptional":
        for platform in ["android", "ios", "desktop"]:
            row[platform + "_source"] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthProfilePages.kt"
        row["web_source"] = "web-client/src/features/AuthProfile.tsx"
        row["implementation_status"] = "dedicated_source_static_reviewed_runtime_unverified"
        row["note"] = "2026-10-30批次011：专用可选昵称／头像、明确保存／跳过、原任务返回与当前资料核对；资格仍由服务端核验。未编译或运行／逐端视觉验收。"
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + batch["high_fidelity_design_nodes"]))
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current = json.loads((folder / "current-source-checkpoint.json").read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items():
    current["files"][file] = {"batch": "011", "sha256_lf": sha}
for file, row in current["files"].items():
    assert digest(file) == row["sha256_lf"], file
current["updated"] = "2026-10-30"
save(folder / "current-source-checkpoint.json", current)

name = "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = read(name).replace("当前批次010已记录，本轮执行标记已释放，余项待接续", "当前批次011已记录，用户再次继续，正在推进余项")
section = """## 批次011：专用可选资料页与原任务返回

2026-10-30源码及允许静态已记录：[16份当前源码指纹及边界](figma-implementation-2026-10-29/batch-011-evidence.json)。实时高保真上下文和图片：手机238:371736、Web311:379948；头像仍为账号／所选照片，返回与列表原SVG沿用，没有把示例猫图写成用户身份。

- 新增shared AuthProfileStore、公共AuthProfilePages及Web AuthProfile。真实created账号进入专用昵称／头像页；保存或跳过回登录成功结果，再主动继续原任务。整个过程不导航到通用编辑器，不替换原FeatureRoute或浏览器历史。
- 头像先在当前账号内存准备／预览，明确保存才上传，当前流程重试复用原上传编号。原生三套选择器增加自定义行触发槽，仍使用已有系统选择器与处理；准备中禁止先保存旧头像或退出。布局复用16间距／56头像及16圆角、56行、固定底部48操作。
- 资料写入带真实版本，保留原介绍／主题／背景。结果不确定时关闭重复保存，只有当前账号／更新版本及全部目标字段匹配才继续；读到旧值不等于已失败。跳过不新发写入，也不撤回已经发出的保存。切换账号清理短时数据，迟到响应不能更新新账号。
- 服务端昵称／头像修改仍执行当前手机号与交流资格检查；拒绝时保留输入并允许跳过，不改资格策略或本地放行。正常资料读取可更新同一账号显示身份，不改变权限。

两项路径／资源静态通过：30shared、47公共UI／48分派、93Web／427引用。15份当前语法中12份完整无诊断，MobileStore／MobileApi／AndroidPhotoPicker为同HEAD旧诊断对照，另5变更片段无诊断；新store初次catch布局诊断已调整并复读，旧报告保留。不计编译／类型／测试／系统选择器或逐端视觉通过。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：批次001–011已记，用户继续执行中。专用可选资料与原任务返回源码已写；下一步已知账号／受限／意图／关联与恢复状态，再继续其余消费者与独立后台。748全量未完成，运行／视觉未验，不把源码或部分合同审查当完成。")
(root / name).write_text(text, encoding="utf-8", newline="\n")
for name in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(name)
    if name.endswith("CONTEXT.md"):
        old = next(line for line in text.splitlines() if line.startswith("- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`"))
        new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 用户再次继续；main／48a3cfd，本轮未提交。[批次005–011](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)已记认证／安全／会话／授权异常／协议隐私／显式创建／真实成功及专用可选资料。011当前15语法含12完整／3同HEAD旧诊断和5变更片段；30shared／47UI／93Web／427引用只读通过。资料选择仅本机预览，保存／跳过回真实成功后原任务，未知结果只读核对；服务资格不绕过。执行中，继续已知／受限／意图／关联／恢复和其余消费者／后台；748主索引只更新实际归属，全量与运行标记仍false。不编译／测试／启动／提交推送，无后台或自动续做。"
    else:
        old = next(line for line in text.splitlines() if line.startswith("| AI-20261029-FIGMA-ALL-CODE "))
        new = "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–011源码／静态分批已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，本轮未提交 | 现行公共Compose／shared／Web及必要平台端口、后端与逐批证据；不构建／测试／启动／自动提交推送 | [十一批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：专用可选资料／原任务返回已写，15语法12完整／3旧诊断对照及5片段；30shared／47UI／93Web／427引用。继续已知／受限／意图／关联／恢复与其他全量；748及后台未全量验收，无后台或定时任务 |"
    (root / name).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
for name in ["docs/CURRENT_BASELINE.md", "docs/client/README.md"]:
    text = read(name).replace("005–010", "005–011").replace("已记十批", "已记十一批")
    if name.endswith("CURRENT_BASELINE.md"):
        text = text.replace("92份Web源码与46份公共UI", "93份Web源码与47份公共UI，30份shared").replace("29 / 46", "30 / 47")
        text = text.replace("010路径／资源和语法6完整／1旧诊断对照、4变更片段已记", "011专用可选资料／返回、路径资源和15语法（12完整／3旧诊断对照）／5变更片段已记")
    else:
        text = text.replace("46公共UI／92Web／418引用", "30shared／47公共UI／93Web／427引用").replace("010语法6完整／1旧诊断对照及4变更片段无诊断", "011专用可选资料／返回和语法12完整／3旧诊断对照及5变更片段已记")
    (root / name).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次011

- Codex，main／48a3cfd，保留上轮26份当前源码指纹且开始核对一致；用户再次继续，执行中，本轮未提交推送。
- [本批证据](../client/figma-implementation-2026-10-29/batch-011-evidence.json)：手机／Web高保真已读，专用可选昵称／头像、真实created进入、明确保存／跳过与原任务返回。shared owner负责版本／上传原编号／未知结果只读核对，三套平台选择器复用并提供行触发槽，准备中保护返回与保存；服务端交流资格继续执行，不本地放行。
- 30shared／47UI／93Web／427引用静态通过；15当前语法12完整、3同HEAD旧诊断，5变更片段无诊断。新store初次catch布局已调整复读，初始与Android旧诊断保留。当前源码指纹已合并匹配，748主索引只更新实际源码归属。
- 未编译／类型检查／测试／lint／浏览器或原生启动／迁移／部署。继续其他认证与消费者／后台全量，运行／视觉未验；无后台或定时任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "011", "source_files": len(files), "current_checkpoint_files": len(current["files"]), "execution": "active"}))
