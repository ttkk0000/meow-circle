"""Record read-only auth information UI and sync the inspected source ownership."""
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
reports = ["batch-009-syntax.json", "batch-009-syntax-followup.json", "batch-009-syntax-final.json", "batch-009-syntax-final-followup.json"]
latest = {}
for name in reports:
    latest.update({row["file"]: row for row in json.loads((folder / name).read_text(encoding="utf-8"))["files"]})
assert len(latest) == 6
for file, row in latest.items():
    assert row["exit_code"] == 0 and not row["syntax"]["errors"] and row["sha256_lf"] == digest(file)
checks = json.loads((folder / "batch-009-checks.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in checks["files"])
batch = {
    "id": "009", "updated": "2026-10-30", "status": "source_written_static_reviewed_runtime_pending",
    "high_fidelity_design_nodes": ["238:371765", "238:371766", "243:369071", "238:371767"],
    "scope": ["agreement entry", "terms and privacy read-only summaries", "provider information", "consent task navigation"],
    "source_hashes_lf": {file: digest(file) for file in [*latest, "web-client/src/styles.css"]},
    "static_report": "batch-009-checks.json", "syntax_reports": reports,
    "static_counts": {"public_ui": 46, "web_sources": 92, "web_imports": 418},
    "notes": [
        "Four information pages use Figma task header/fixed footer and fact rows with 80px labels, 8px gap, 12/20 label and 16/26 body. They are summaries, not invented legal text; configured official full documents remain separate links.",
        "Reading, back navigation and external privacy links cannot accept agreements, request codes, register or initialize a provider. Only the explicit authorization action starts the existing SDK flow.",
        "Entry/detail history returns within the information task; fixed return-to-login exits the summary. Managing methods keeps the security route and selects password login for a guest instead of leaving registration selected.",
        "Source scans of current Android manifest and iOS declarations found no contact-reading permission. Existing platform constraints and provider availability remain unchanged.",
        "Six final grammar reads are clean. Initial JSX URL-attribute diagnostics were corrected to an explicit string expression; the local open helper was renamed openPolicy after a grammar diagnostic. Initial reports are preserved, and no compiler or runtime was run.",
        "Reviewed source ownership from batches 005-009 is synchronized into the 748-state canonical trace. No state count, design root, backend count or runtime flag is promoted."
    ],
    "states": ["authTerms", "authTermsDetails", "authPrivacyDetails", "authSDKInfo"],
    "remaining": ["first/known/success and optional profile auth states", "remaining consumer/backend families", "platform runtime and visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False,
}
save(folder / "batch-009-evidence.json", batch)
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "009" for row in ledger["batches"])
ledger["batches"].append(batch)
security = {"authAccounts", "phoneGate", "phoneCode", "phoneChange", "emailBinding"}
for state in ledger["consumer_states"]:
    if state["key"] in batch["states"]:
        state["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        state["reviewed_findings"].append("Batch 009: read-only information page and navigation reviewed; official full documents remain configured links, no implicit consent. Runtime/visual acceptance open.")
        for platform in ["android", "ios", "desktop"]:
            state["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthInformation.kt"
        state["sources"]["web"] = "web-client/src/features/AuthInformation.tsx"
    if state["key"] in security | {"authNetworkError", "authRateLimited"}:
        state["source_review"] = "dedicated_source_written_static_reviewed_runtime_pending"
        state["reviewed_findings"].append("Batch 006's historical grammar deferral is superseded by current reads in batches 007 and 009; runtime/visual acceptance remains open.")
        if state["key"] in security:
            for platform in ["android", "ios", "desktop"]:
                state["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/SecurityPages.kt"
            state["sources"]["web"] = "web-client/src/features/Account.tsx"
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue first/known/success and optional-profile auth states, remaining consumer/backend families; runtime/visual acceptance remains open. No automatic commit or execution.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
reviewed = {key for row in ledger["batches"] if row["id"] in {"005", "006", "007", "008", "009"} for key in row.get("states", [])}
by_key = {row["key"]: row for row in ledger["consumer_states"]}
for row in trace["states"]:
    if row["key"] not in reviewed:
        continue
    state = by_key[row["key"]]
    for platform in ["android", "ios", "desktop", "web"]:
        row[platform + "_source"] = state["sources"][platform]
    row["implementation_status"] = "dedicated_source_static_reviewed_runtime_unverified"
    row["note"] = "2026-10-30批次005–009的具体源码／静态与差异已审查；逐端视觉、类型及运行未验收，见FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md。"
trace["updated"] = "2026-10-30"
save(trace_file, trace)

name = "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = read(name).replace("当前批次008已记录", "当前批次009已记录")
section = """## 批次009：协议入口、隐私摘要与第三方登录说明

2026-10-30源码及允许静态已记录：[7份当前源码指纹及边界](figma-implementation-2026-10-29/batch-009-evidence.json)。四份高保真上下文／图片为协议入口238:371765、服务摘要238:371766、隐私摘要243:369071、第三方说明238:371767。

- 原生AuthInformation／Web同名组件提供四个独立只读任务页面，使用80宽标签／8间距、12/20说明和16/26正文、16边距／12事实间距及固定底部操作。保留正式全文配置链接，页面摘要不冒充新拟法律正文。
- 协议入口／详情有各自返回范围；第三方说明可进入账号设置，游客切回登录表单，不停在注册。普通登录和官方授权前均可读取；阅读不接受协议、不发送验证码、不创建账号、不初始化SDK。原生授权同意改为独立任务页明确确认。
- 当前声明扫描无通讯录权限；未添加位置、照片或第三方新权限／服务配置。原图标／字体沿用，Apple在未启用平台仍不可授权。
- 将005–009已审查状态的实际源码归属同步至748主索引；不增加状态总数、不改设计根或运行标记。006旧语法待项由007／009当前读取接续，历史跳过记录保留。

两项路径／资源静态通过：46公共UI／48分派、92Web／418引用；6份修改／新增文件最终完整语法无诊断。初次JSX URL属性和本地open命名的语法器诊断已修正／改名并复读，初始报告保留。未编译／类型检查／测试／lint／启动；其他全量仍待继续。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：用户于2026-10-30恢复，批次001–009已记；继续首次／已知账号／登录成功和可选资料状态，以及其他消费者与独立后台状态。748全量未完成，运行／视觉未验，不把归属或部分合同审查当完成。")
(root / name).write_text(text, encoding="utf-8", newline="\n")
for name in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(name)
    if name.endswith("CONTEXT.md"):
        old = next(line for line in text.splitlines() if line.startswith("- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`"))
        new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 用户恢复全部按设计实现；main／48a3cfd已推送，本轮未提交。[批次005–009](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)补分步认证／恢复、安全与当前号、会话／退出结果、官方异常／关联OTP、协议与隐私四页。最新46公共UI／92Web／418引用只读通过；009六份语法无诊断，007接续006历史内存跳过（8完整／4旧诊断对照）。普通退出按Figma保留账号草稿；安全变更按服务合同登录后回设置；748主索引更新实际归属，所有运行／全量完成标记仍false。执行中，继续首次／成功／可选资料及其余全量；不编译／测试／启动，无后台或自动续做。"
    else:
        old = next(line for line in text.splitlines() if line.startswith("| AI-20261029-FIGMA-ALL-CODE "))
        new = "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–009源码／静态分批已记 | Codex / 2026-10-30；main／48a3cfd，本轮未提交 | 现行公共Compose／shared／Web、必要后端及逐批证据；不构建／测试／启动／自动提交推送 | [九批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：分步认证／安全／会话与退出／官方异常／协议隐私已写；最新46UI／92Web／418引用，009六份完整语法无诊断。继续首次／成功／可选资料及其他全量；748与后台仍未全量验收，无后台或定时任务 |"
    (root / name).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
baseline = root / "docs/CURRENT_BASELINE.md"
text = baseline.read_text(encoding="utf-8").replace("005–006", "005–009").replace("已记六批；本轮补分步认证、账号与手机号验证、安全变更及未知结果门槛，91份Web源码与45份公共UI。006路径／资源检查通过，最终语法因内存不足未执行；不等于全量视觉或运行通过。", "已记九批；本轮补分步认证、安全／当前号、会话及退出、官方授权异常和协议隐私说明，92份Web源码与46份公共UI。009路径／资源与六份最终语法读取通过；007已接续006旧内存跳过。4份大Kotlin文件仍为同HEAD旧语法诊断对照，不等于类型、全量视觉或运行通过。").replace("29 / 45", "29 / 46").replace("新增账号／安全专用页面", "新增账号／安全与协议说明专用页面")
baseline.write_text(text, encoding="utf-8", newline="\n")
name = "docs/client/README.md"
text = read(name)
old = next(line for line in text.splitlines() if line.startswith("2026-10-30：[批次005"))
new = "2026-10-30：[批次005–009](FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)续作分步认证／密码恢复、安全／当前号、会话及退出、官方授权异常与协议隐私说明。46公共UI／92Web／418引用只读通过，009六份语法无诊断；007当前读取接续006旧内存跳过。不编译或运行，不把源码／路径与语法计作全量验收。"
(root / name).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次009

- Codex，main／48a3cfd，本轮未提交；用户继续全部按设计实现，执行中。[本批证据](../client/figma-implementation-2026-10-29/batch-009-evidence.json)：协议入口、服务／隐私摘要与第三方说明四页，原生独立授权确认；游客管理方式切回登录，阅读不自动接受协议或启动SDK。四份高保真图／上下文已读。
- 46公共UI／92Web／418引用路径资源静态通过；六份当前完整语法无诊断，初次属性和命名诊断修正并复读，初始记录保留。748主索引仅同步已审查的实际归属；所有运行与全量完成标记false，006历史内存跳过由007／009当前读取接续。
- 未编译／类型检查／测试／lint／启动／提交推送。继续首次／已知／成功与可选资料及其他消费者／后台；全量未完成，无后台或定时任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "009", "current_sources": 7, "grammar_files": 6, "trace_states": len(trace["states"]), "execution": "active"}))
