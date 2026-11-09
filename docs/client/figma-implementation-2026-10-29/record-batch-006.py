"""Save current source progress, keeping the low-memory grammar reads unverified."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[3]
folder = Path(__file__).resolve().parent
def read(file):
    return (root / file).read_text(encoding="utf-8")
def save(file, value):
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")

files = ["kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/" + name + ".kt" for name in
         ["AuthPages", "SecurityPages", "AccountPages", "ReadingPages"]]
files += ["kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/" + name + ".kt" for name in
          ["MobileStore", "MobileFeatureStore", "FeatureModels"]]
files += ["web-client/src/" + name for name in ["features/AuthDialog.tsx", "features/Account.tsx", "styles.css"]]
hashes = {file: hashlib.sha256(read(file).replace("\r\n", "\n").encode()).hexdigest() for file in files}
checks = json.loads((folder / "batch-006-checks.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in checks["files"])
syntax = json.loads((folder / "batch-006-syntax.json").read_text(encoding="utf-8"))
assert len(syntax["files"]) == 9 and all(row["status"] == "not_read_low_memory" for row in syntax["files"])
batch = {
    "id": "006", "updated": "2026-10-30", "status": "source_written_static_paths_reviewed_syntax_pending",
    "high_fidelity_design_nodes": ["238:371726", "238:371746", "460:17343", "460:17368"],
    "scope": ["dedicated auth failure presentation", "account/methods and eligibility views", "current-phone request/code/verified flow", "separate phone/email/password security changes", "durable Web unknown-result guard"],
    "source_hashes_lf": hashes, "static_report": "batch-006-checks.json", "syntax_report": "batch-006-syntax.json",
    "static_counts": {"public_ui": 45, "web_sources": 91, "web_imports": 413},
    "syntax_boundary": "All 9 grammar reads skipped before starting Node, with 552 MiB memory headroom. No final grammar or type-check success claim. An earlier 633-MiB skipped attempt remains batch-005-syntax-batch006.json.",
    "notes": [
        "An actual phone OTP receipt advances to code view; only a fresh eligibility contract v1 with phone_verified true advances to verified result. An already-verified current number does not trigger another OTP.",
        "Current-number verification fields and change-target fields use separate transient keys. Binding still uses the original sensitive scope/grant/confirmation and original operation recovery.",
        "Web lists account-change uncertain drafts before enabling/preparing another change. Matching successful response clears that saved draft; unknown results retain the original key and remain non-replayable.",
        "Logout runs in MobileStore-owned command scope so clearing the feature owner does not prematurely finish or cancel client cleanup. Existing private-draft clearing policy is retained.",
        "Network failure is distinguished from HTTP failure. Specific expired presentation is conditional on explicit server code; generic invalid/expired credentials do not falsely prove a distinct expiry reason.",
        "Batch-005-checks.json was briefly overwritten by the later 45-file check; its observed prior 44-file receipt was restored with an explicit historical note, and current checks retained here."
    ],
    "states": ["authAccounts", "phoneGate", "phoneCode", "phoneChange", "emailBinding", "authNetworkError", "authRateLimited"],
    "remaining": ["final grammar review when memory permits", "session expiration and definitive sensitive-change result pages", "dedicated logout presentation", "remaining consumer and backend states", "platform runtime and visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
save(folder / "batch-006-evidence.json", batch)
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "006" for row in ledger["batches"])
ledger["batches"].append(batch)
for state in ledger["consumer_states"]:
    if state["key"] in batch["states"]:
        state["source_review"] = "dedicated_source_written_static_paths_reviewed_syntax_pending"
        state["reviewed_findings"].append("Batch 006 dedicated source and owner/receipt/unknown-result guards; grammar reads deferred for low memory, runtime unverified.")
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Review final batch-006 grammar when memory permits; continue session-expiry and sensitive-change/logout result presentation, then remaining consumer/backend families. No automatic commit/push or runtime execution.")
save(ledger_file, ledger)
name = "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = read(name)
section = """## 批次006：认证失败、账号设置与当前手机号的独立流程

2026-10-30源码与路径／资源静态已记：[10份源码指纹及边界](figma-implementation-2026-10-29/batch-006-evidence.json)。最新语法因内存不足未执行，不能沿用005报告宣称当前文件已通过。

- 原生／Web认证失败使用独立标题、说明、返回原任务与固定重试动作；HTTP频控与实际连接异常分开。验证码明确过期仅在有对应服务端错误码时显示，不把通用拒绝都写成已过期。重试不自动重发已清除的密码或验证码。
- 新增公共SecurityPages，账号头像／真实昵称与登录方式、资格、当前手机号、换绑邮箱／手机和密码各自呈现。当前号与新标识字段和用途隔离；新密码补二次确认，继续复用原用途授权、原编号和真实安全变更。
- 当前手机号发送回执后进入验证码页，写入后再读服务端资格，仅合同v1且当前号码已验证才显示结果；已有有效验证不强迫重复短信。返回仍需原业务再次主动确认，不自动发布。
- Web安全变更在创建新操作前核对持久的未知请求；未知原编号保留并关闭新变更，成功回执后清空该快照。原生退出由MobileStore自己的命令执行，避免FeatureStore清理账号时抢先结束清理。
- 高保真上下文与图片：认证网络失败238:371726、账号方式238:371746、交流验证460:17343、换号460:17368。头像继续使用账号动态媒体，原图标／字体不替换。

两项只读检查通过：45公共UI／48分派、91Web／413引用；差异空白无错误。最新9份语法读取全部在启动Node前因552MiB余量跳过，先前633MiB跳过记录另存；不降低内存门槛或运行构建。Python原生语法包及现有纯JS替代均未找到，未安装。未编译、类型检查、测试、lint或启动；会话过期、敏感变更结果／独立退出页和其他全量继续推进。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：用户于2026-10-30恢复，批次001–006已记；006源码及路径检查已写，最终语法因内存不足待补。继续会话过期、敏感变更结果／退出及其余消费者／后台状态。748全量未完成，不把源码归属当作逐端验收。")
(root / name).write_text(text, encoding="utf-8", newline="\n")
for name in ["docs/ai/CONTEXT.md", "docs/ai/TASKS.md"]:
    text = read(name)
    if name.endswith("CONTEXT.md"):
        old = next(line for line in text.splitlines() if line.startswith("- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`"))
        new = "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 用户恢复全部按设计实现；main／48a3cfd已推送，本轮未提交。[批次005–006](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)补分步登录／密码恢复、独立认证错误、账号／当前手机号与安全变更。45公共UI／91Web／413引用的只读检查通过；006最新9语法在552MiB余量下全部跳过，不计通过。执行中，继续会话过期、真实安全变更结果／退出与其余全量；不编译／测试／启动，无后台或自动续做。748及后台全量未完成。"
    else:
        old = next(line for line in text.splitlines() if line.startswith("| AI-20261029-FIGMA-ALL-CODE "))
        new = "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005已记，006源码／路径已记、语法因内存待补 | Codex / 2026-10-30；main／48a3cfd，本轮未提交 | 现行公共Compose／shared／Web、必要后端及逐批证据；不构建／测试／启动／自动提交推送 | [六批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：账号／当前手机号分步、未知结果保护已写；006最终语法未验。继续会话过期与变更／退出结果及其他全量，748和后台逐项仍未完成；无后台或定时任务 |"
    (root / name).write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次006

- Codex，main／48a3cfd，本轮未提交；用户多次明确继续全部按设计实现，执行中，无后台／定时任务。
- [本批证据](../client/figma-implementation-2026-10-29/batch-006-evidence.json)：独立认证失败页、账号与登录方式、当前号发送／验证／实际资格回读、独立安全变更、Web未知请求门槛与成功快照清理。换绑仍走原用途授权与回执，不以当前会话直接接管。
- 4份高保真上下文／图片已读；2只读脚本通过，45公共UI／91Web／413引用，差异空白通过。最新9份语法均因552MiB余量跳过，不执行Node；先前633MiB记录保留。未找到既有低内存替代包，未安装。
- 005静态报告曾被新调用覆盖，已据此前实际44文件输出恢复并注明历史来源；当前45文件结果独立保留。005源码指纹保持当时快照，不当作006当前验证。
- 未编译／类型检查／测试／lint／启动／迁移／部署。继续会话失效、安全变更真实结果／退出及其余全量，006语法待内存允许再补；不宣称748或后台全量完成。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "006", "source_files": len(hashes), "syntax": "not_run_low_memory", "execution": "active"}, ensure_ascii=False))
