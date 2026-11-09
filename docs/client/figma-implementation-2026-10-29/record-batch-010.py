"""Record explicit creation review and actual login completion; keep optional UI partial."""
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
syntax = json.loads((folder / "batch-010-syntax.json").read_text(encoding="utf-8"))
assert len(syntax["files"]) == 7
for row in syntax["files"]:
    assert row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"])
checks = json.loads((folder / "batch-010-checks.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in checks["files"])
fragments = json.loads((folder / "batch-010-fragments.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in fragments["files"])
files = [row["file"] for row in syntax["files"]] + ["web-client/src/styles.css"]
batch = {
    "id": "010", "updated": "2026-10-30", "status": "source_written_static_reviewed_runtime_pending",
    "high_fidelity_design_nodes": ["238:371735", "238:371734", "238:371736"],
    "scope": ["explicit new-account review", "real login completion", "optional profile editor entry"],
    "source_hashes_lf": {file: digest(file) for file in files}, "static_report": "batch-010-checks.json",
    "syntax_reports": ["batch-010-syntax.json", "batch-010-fragments.json"],
    "static_counts": {"public_ui": 46, "web_sources": 92, "web_imports": 418},
    "notes": [
        "Email registration now requires a separate branded first-account review after local form preparation, before any create request. The original code/password are verified by the server only on final create. Official provider creation reuses this review without bypassing agreement or account-choice verification.",
        "The old Figma phone-create example is adapted to the approved current email/provider create contracts. SMS remains login-only for an existing binding; no legacy SMS auto-signup is restored.",
        "Success uses actual session owner and the existing server created flag. Native LoginSession.created already exists; the Web adapter strips tokens but retains created. Neither client infers a new account from nickname, avatar or a local verification flag.",
        "Successful login keeps an explicit result page. Returning to the pending application happens after the user continues; no write or publication is automatically submitted. The actual dynamic avatar/name are used.",
        "Only a confirmed created account gets the optional profile-editor entry. The existing versioned profile editor remains in use; the dedicated authProfileOptional nickname/avatar/skip screen is still partial, and is not marked completed.",
        "Seven syntax reads completed: six whole files without diagnostics; MobileStore has the same pre-existing grammar signatures as HEAD, with four account/logout/completion/model fragments read cleanly. This is not a type check or runtime result."
    ],
    "states": ["authFirst", "authSuccess"], "partial_states": ["authProfileOptional"],
    "remaining": ["dedicated optional profile layout and return path", "known/restricted/intent/link/recovery auth states", "remaining consumer/backend families", "platform runtime and visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False,
}
save(folder / "batch-010-evidence.json", batch)
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "010" for row in ledger["batches"])
ledger["batches"].append(batch)
for state in ledger["consumer_states"]:
    if state["key"] in batch["states"]:
        state["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        state["reviewed_findings"].append("Batch 010: explicit email/provider create review and actual owner/created login result; current phone-create example is superseded by the approved login-only SMS contract. Runtime/visual acceptance open.")
        state["source_variants"] = {
            "native_create": "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ProviderAuthPages.kt",
            "web_create": "web-client/src/features/ProviderSignIn.tsx",
        }
    elif state["key"] in batch["partial_states"]:
        state["source_review"] = "partial_contract_review"
        state["reviewed_findings"].append("Batch 010: real created flag enables the existing profile editor; dedicated optional nickname/avatar/skip screen and exact return behavior remain to implement.")
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Implement the dedicated optional profile layout/return and known/restricted/intent/link/recovery states, then remaining consumer/backend families. Preserve all incomplete/runtime flags; no automatic commit or execution.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] in batch["states"]:
        row["implementation_status"] = "dedicated_source_static_reviewed_runtime_unverified"
        row["note"] = "2026-10-30批次010：邮箱／官方方式显式创建确认；真实owner及created回执驱动成功页。旧手机号自动建号示例按现行合同保持关闭。未运行或逐端视觉验收。"
trace["updated"] = "2026-10-30"
save(trace_file, trace)

# Current source checkpoint merges historical batches without rewriting their receipts.
current = {}
for previous in ledger["batches"]:
    if previous["id"] in {"005", "006", "007", "008", "009", "010"}:
        for file, sha in previous.get("source_hashes_lf", {}).items():
            current[file] = {"batch": previous["id"], "sha256_lf": sha}
for file, row in current.items():
    assert digest(file) == row["sha256_lf"], (file, row["batch"])
save(folder / "current-source-checkpoint.json", {
    "updated": "2026-10-30", "base_head": "48a3cfdf51c97546153d230bea379c9a880f2d30", "files": current,
    "meaning": "Current source fingerprints only. Per-batch grammar/static limitations remain in original receipts; no type/runtime or whole-state completion.",
    "runtime_verified": False, "whole_state_complete": False,
})
name = "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = read(name).replace("当前批次009已记录", "当前批次010已记录")
section = """## 批次010：显式创建确认与真实登录成功结果

2026-10-30源码与允许静态已记录：[8份当前源码指纹及边界](figma-implementation-2026-10-29/batch-010-evidence.json)。高保真上下文／图片为首次创建238:371734、成功238:371735、可选资料238:371736；第三项仅部分合同审查，专用表单未完成。

- 邮箱注册填写后先进入独立品牌／登录方式／首次使用确认页，最终主动确认才调用创建；验证码／密码仍在最终请求由服务验证，不把本地步骤当已验证。官方新账号复用此确认，保留协议、原账号关联与用途验证。旧稿手机号建号示例按现行批准合同适配邮箱／官方方式，短信不自动建号。
- 登录成功使用真实owner与既有服务端created标记，动态头像／昵称、保留内容说明和固定返回操作独立呈现。原生待办申请到用户主动返回后才继续打开核对，绝不自动提交。
- 仅真实created账号提供现有头像／昵称编辑入口；专用authProfileOptional表单、跳过与原任务返回仍待实现，单列部分审查。未通过昵称、头像或本地认证标志猜测账号／资格。

两项路径／资源静态通过：46公共UI／48分派、92Web／418引用。7份语法读取含6完整无诊断、MobileStore同HEAD旧诊断对照，4个账号／退出／完成模型方法片段无诊断；未类型检查／编译／测试／lint／启动。005–010全部当前源码指纹汇总至[current-source-checkpoint.json](figma-implementation-2026-10-29/current-source-checkpoint.json)，历史回执不重写。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：用户于2026-10-30恢复，批次001–010已记。下一步为专用可选资料表单／原任务返回、已知账号／受限／意图／关联与恢复状态，再继续其余消费者与独立后台。748全量未完成，运行／视觉未验，不把归属或部分合同审查当完成。")
(root / name).write_text(text, encoding="utf-8", newline="\n")
for name in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(name)
    if name.endswith("CONTEXT.md"):
        old = next(line for line in text.splitlines() if line.startswith("- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`"))
        new = old.replace("005–009", "005–010").replace("协议与隐私四页", "协议与隐私四页、显式创建确认／真实成功页").replace("009六份语法无诊断", "010语法6完整／1同旧诊断及4变更片段").replace("执行中，继续首次／成功／可选资料及其余全量", "执行中，继续专用可选资料、已知／受限／关联／恢复及其余全量")
    else:
        old = next(line for line in text.splitlines() if line.startswith("| AI-20261029-FIGMA-ALL-CODE "))
        new = old.replace("005–009", "005–010").replace("九批记录", "十批记录").replace("协议隐私已写", "协议隐私／显式创建／真实成功已写").replace("009六份完整语法无诊断", "010语法6完整／1旧诊断对照，4变更片段无诊断").replace("继续首次／成功／可选资料及其他全量", "继续专用可选资料／已知／受限／关联／恢复及其他全量")
    (root / name).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
for name in ["docs/CURRENT_BASELINE.md", "docs/client/README.md"]:
    text = read(name).replace("005–009", "005–010").replace("已记九批", "已记十批")
    if name.endswith("CURRENT_BASELINE.md"):
        text = text.replace("和协议隐私说明，92份", "和协议隐私、显式创建与真实成功说明，92份").replace("009路径／资源与六份最终语法读取通过", "010路径／资源和语法6完整／1旧诊断对照、4变更片段已记")
    else:
        text = text.replace("与协议隐私说明。46", "与协议隐私、显式创建／真实成功说明。46").replace("009六份语法无诊断", "010语法6完整／1旧诊断对照及4变更片段无诊断")
    (root / name).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次010

- Codex，main／48a3cfd，本轮未提交；用户继续全部按设计实现。[本批证据](../client/figma-implementation-2026-10-29/batch-010-evidence.json)：邮箱／官方新账号的显式创建确认、真实owner／created驱动登录成功页与主动返回，新增账号可进入既有资料编辑。三份高保真上下文／图片已读；专用可选资料页仅部分，短信自动建号维持关闭。
- 46公共UI／92Web／418引用路径资源静态通过；7语法含6完整／1同HEAD旧诊断，4个完成／账号／退出片段无诊断；005–010当前源码指纹汇总并逐一匹配。未编译／类型检查／测试／lint／启动／提交推送。
- 全量与逐端视觉／运行未完成；下一步专用可选资料／返回、已知／受限／意图／关联／恢复，再继续其他消费者和后台。执行标记在本轮交接记录中释放，无后台或定时任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "010", "current_checkpoint_files": len(current), "whole_state_complete": False, "runtime_verified": False}))
