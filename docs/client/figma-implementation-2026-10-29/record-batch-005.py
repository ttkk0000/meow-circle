"""Persist this batch's source evidence in the existing implementation ledger."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[3]
folder = Path(__file__).resolve().parent

def read(file):
    return (root / file).read_text(encoding="utf-8")

def save(file, value):
    file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")

reports = [json.loads((folder / name).read_text(encoding="utf-8")) for name in
           ["batch-005-syntax.json", "batch-005-syntax-followup.json", "batch-005-fragments.json"]]
final = {row["file"]: row for report in reports for row in report["files"]}
assert len(final) == 11 and all(row.get("exit_code") == 0 for row in final.values())
for file, row in final.items():
    assert hashlib.sha256(read(file).replace("\r\n", "\n").encode()).hexdigest() == row["sha256_lf"], file
checks = json.loads((folder / "batch-005-checks.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 for row in checks["files"])
hashes = {file: row["sha256_lf"] for file, row in final.items()}
hashes["web-client/src/styles.css"] = hashlib.sha256(read("web-client/src/styles.css").replace("\r\n", "\n").encode()).hexdigest()
states = ["emailStart", "emailCode", "authPassword", "authPasswordRecovery", "authPasswordReset", "authPasswordDone"]
partial = ["emailError", "authCodeExpired", "authCodeResent", "authSmsDelayed", "authRateLimited", "authNetworkError", "accountRecovery"]
batch = {
    "id": "005", "updated": "2026-10-30", "status": "source_written_static_reviewed",
    "scope": "quiet actions and shared spacing; stepwise email/phone login and explicit registration; atomic password recovery UI",
    "high_fidelity_design_nodes": ["460:17318", "460:17323", "238:371740", "238:371742"],
    "text_action_design_nodes": ["460:17328", "460:17333", "460:17373", "238:371723", "238:371724", "238:371725", "238:371726", "238:371741", "238:371742", "238:371743", "238:371730"],
    "states": states, "partial_contract_states": partial,
    "source_hashes_lf": hashes,
    "static_report": "batch-005-checks.json", "syntax_reports": ["batch-005-syntax.json", "batch-005-syntax-followup.json", "batch-005-fragments.json"],
    "syntax_boundary": "8 final complete files without diagnostics; 3 files compared with existing grammar diagnostics. The two modified shared methods additionally read as fragments, one with the identical preexisting catch diagnostic.",
    "repair_history": "AuthPages nested conditional when branches were braced after initial grammar read failed. Final AuthPages and AuthDialog rereads are clean. Recovery help navigation uses existing workflowURL(safety), not a nonexistent route.",
    "auth_boundary": "OTP receipt alone does not log in or create an account. Recovery code and new password are verified together by existing recover endpoints; entering the password form is not a successful verification claim. Secrets remain transient. Provider/eligibility policy unchanged.",
    "remaining": ["dedicated authentication error and session-expiration presentation", "security method and sensitive-change pages", "remaining consumer/backend state review", "all platform runtime and visual acceptance"],
    "whole_state_complete": False, "runtime_verified": False, "type_checked": False, "tests_executed": False,
}
save(folder / "batch-005-evidence.json", batch)
file = folder / "state-progress.json"
ledger = json.loads(file.read_text(encoding="utf-8"))
assert not any(row["id"] == "005" for row in ledger["batches"])
ledger["batches"].append(batch)
for state in ledger["consumer_states"]:
    if state["key"] in states:
        state["source_review"] = "dedicated_source_written_static_reviewed"
        state["reviewed_findings"].append("Batch 005 explicit UI steps, actual OTP/write receipt and return actions reviewed; runtime/whole-state acceptance remains unverified.")
    elif state["key"] in partial:
        state["source_review"] = "partial_contract_review"
        state["reviewed_findings"].append("Batch 005 shared code/recovery/help and actual error behavior; dedicated presentation still to review.")
    elif state["key"] in {"offline", "noResults", "contentUnavailable"}:
        state["reviewed_findings"].append("Batch 005 quiet recovery/clear actions and default FeatureColumn spacing aligned.")
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue dedicated auth error/session presentation, security methods and remaining consumers/backend states. No runtime acceptance or automatic commit/push.")
save(file, ledger)
name = "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = read(name)
section = """## 批次005：共用弱操作、验证码分步与密码恢复

2026-10-30源码与静态核对已记：[12份源码指纹及证据](figma-implementation-2026-10-29/batch-005-evidence.json)。本轮从已推送的48a3cfd恢复，未提交推送。

- 补原生quiet按钮的14/22标签、48触控与焦点／禁用语义；接清空条件、草稿恢复与其他领养入口，Web对应弱操作字号；FeatureColumn默认间距统一16。
- 原生／Web登录改为地址、验证码、密码设置、帮助和真实回执结果的明确步骤。发送成功回执后才进入验证码页；修改标识清理原验证码，发送按登录／注册／恢复用途隔离，重发倒计时保留。注册仍须主动选择、明确协议和服务端允许，不恢复短信自动建号。
- 密码恢复补二次确认、独立设置新密码页及回执后的返回登录。现有接口在最终保存时一次核对验证码与密码，因此进入设置页不显示“已验证成功”；失败保留原账号及任务，密码／验证码只在账号内存短时保存。
- 手机原生固定操作与安全区、Web独立滚动任务弹层和底部主操作接入；读取登录能力失败可原位重试，帮助使用已有免注册安全流程。静态资产继续复用已校验的原图标，没有临时Figma URL进入应用。

高保真上下文及图片已读取邮箱入口460:17318、验证码460:17323、密码238:371740、新密码238:371742；另11份错误／恢复文字与动作回读。一次三节点并行读取有一份Transport closed；一次动作输出过大已缩减为只读紧凑返回，未改远端设计。

两份只读脚本通过，44公共UI／91Web／413引用。最终11份语法覆盖：8份完整无诊断、ConsumerApp／MobileStore／MobileFeatureStore为基线诊断对照，后两项新增方法另读片段；不计类型检查。初次AuthPages语法读取失败后补分支花括号并干净复读，初始失败报告保留。未编译／测试／lint／启动。独立错误页、安全设置与其余全量继续实施，运行／五布局视觉验收仍未完成。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：用户已于2026-10-30恢复，批次001–005已记；继续独立认证错误／会话失效、安全设置与其余消费者／后台状态。748全量未完成，不把共享样式或源码归属当作逐端验收。")
(root / name).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次005

- 用户恢复全部按设计实现；Codex，main／48a3cfd，开始时工作区干净且与origin一致。保留旧四批证据，未拉取／提交推送。
- 共用quiet动作／16间距、原生及Web登录地址→验证码→密码设置／恢复／真实结果页已写；OTP发送回执、用途／标识隔离、原倒计时、二次确认、能力重读、帮助出口与短时秘密保持。现有恢复接口仍在最终保存时一并验证，不假称提前验证成功。
- [本批证据](../client/figma-implementation-2026-10-29/batch-005-evidence.json)：12源码指纹，2只读脚本；11最终语法覆盖含3份旧诊断对照、2份方法片段另读。初次AuthPages分支语法读取失败已修正并复读，旧报告保留。Figma仅回读，部分请求失败／过大后缩减，不记成设计修改。
- 执行中：继续独立错误／会话失效、安全设置与其余全量。未编译／类型检查／测试／lint／启动／迁移／部署，未自动提交推送；没有后台或定时任务，不宣布全量完成。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "005", "source_hashes": len(hashes), "states": len(states), "partial_states": len(partial), "runtime_verified": False}, ensure_ascii=False))
