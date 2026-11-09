"""Record current/new-phone source flow, exact pending reads and a handoff."""
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
checks = json.loads((folder / "batch-020-checks-final.json").read_text(encoding="utf-8"))
syntax = json.loads((folder / "batch-020-syntax.json").read_text(encoding="utf-8"))
followup = json.loads((folder / "batch-020-syntax-followup.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"]) for row in checks["files"])
assert len(syntax["files"]) == 5 and all(row["exit_code"] == 0 and not row["syntax"]["errors"] for row in syntax["files"])
assert len(followup["files"]) == 5 and all(row["status"] == "not_read_low_memory" and row["headroom_mb"] < 1024 for row in followup["files"])
files = [row["file"] for row in syntax["files"]] + ["web-client/src/components/PurposeCheck.tsx"]
pending = [row["file"] for row in followup["files"]]
assert len(files) == 6
current_clean = [row["file"] for row in syntax["files"] if row["sha256_lf"] == digest(row["file"])]
assert current_clean == ["web-client/src/components/ui.tsx"]
batch = {
    "id": "020", "updated": "2026-10-30", "status": "source_written_path_asset_reviewed_final_grammar_pending",
    "high_fidelity_design_nodes": ["238:371750", "311:380004", "238:371751", "311:380008", "238:371745", "311:379984"],
    "scope": ["current-phone proof before new-phone entry", "receipt-bound new-phone input", "phone supplement task", "owner/epoch and eligibility-cache handoff guards"],
    "states": ["authChangePhone", "authNewPhone", "authPhoneSupplement"],
    "source_hashes_lf": {file: digest(file) for file in files},
    "contract_read_hashes_lf": {file: digest(file) for file in ["internal/identity/email.go", "internal/identity/service.go", "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileApi.kt", "web-client/src/core/query.ts"]},
    "static_report": "batch-020-checks-final.json",
    "syntax_reports": ["batch-020-syntax.json", "batch-020-syntax-final.json", "batch-020-syntax-followup.json"],
    "grammar_pending_source_hashes": {file: digest(file) for file in pending},
    "syntax_counts": {"initial_whole_clean": 5, "current_whole_clean": 1, "final_pending": 5, "minimum_headroom_mb": 1024, "final_skip_headroom_mb": [907, 746]},
    "static_counts": {"shared_sources": 33, "public_ui": 53, "web_sources": 100, "web_imports": 457},
    "notes": [
        "Six phone/Web high-fidelity contexts and screenshots inspected. Current-phone change step uses actual masked current phone, actual full-phone field required by the existing server, current-purpose code, notice, recovery assistance and explicit continue. New-phone step has a real phone/code form and the account change remains separately verified and explicitly confirmed.",
        "Current verification calls the existing verify_phone send purpose and auth/verify-phone, then reads actual eligibility and requires contract_version=1 plus phone_verified. Only that real response advances the step. A UI step flag is not a mutation grant; later account scope, exact target, version, reauthentication and final confirm remain mandatory. No new auth endpoint, local eligibility flag, autosignup or policy was added.",
        "Phone supplement carries the Figma purpose notice, actual phone/code fields, skip and persistent verify/return action. Already server-verified users retain their no-repeat path. Actual code verification enters the existing confirmed-return stage and never posts the original task.",
        "Current/new-phone receipts bind owner, address and purpose, require actual positive receipt ID/deadline, reject expired receipt for continuation, respect retry delay and stop timers after expiry/cooldown. Native new email form also uses the actual receipt key. A new message alone never becomes verification or eligibility.",
        "Web updates the query cache from the actual eligibility response rather than forcing a loading reload that would unmount the change form and lose its completed current-phone step. AccountChange is keyed/gated by owner, clears old render on epoch change, guards late prepare/draft work, and suppresses an old operation's navigation after another epoch. Original unknown-result records remain intact.",
        "Shared Field accepts optional native inputMode through its actual React attribute type. PurposeCheck keeps its original checkbox copy by default; binding/unbinding explicitly describes the final confirmation button instead. Original assets/tokens/components are reused, with no temporary asset URLs or fake example phone.",
        "Final path/asset reads pass33shared/53UI/100Web/457 local imports and diff whitespace. The first five grammar reads were clean; four were then adjusted. Final/followup reads were skipped at907/746MiB under the unchanged1024MiB floor. ui.tsx remains current-clean, five current files are explicitly pending (including the newly adjusted PurposeCheck). Account.tsx received a final late-navigation guard after the skip; current hashes are separate. No type checker, compiler, build, tests, application/browser/device launch or runtime/visual acceptance."
    ],
    "remaining": ["five current grammar files after normal headroom", "remaining auth and consumer/backend state fidelity", "typed partial-restriction contract", "runtime/visual/platform acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "020" for row in ledger["batches"])
save(folder / "batch-020-evidence.json", batch)
ledger["batches"].append(batch)
native_phone = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthPhonePages.kt"
native_new = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/SecurityPages.kt"
web = "web-client/src/features/AuthPhone.tsx"
for row in ledger["consumer_states"]:
    if row["key"] in batch["states"]:
        row["source_review"] = "dedicated_source_written_path_asset_reviewed_final_grammar_pending"
        row["reviewed_findings"].append("Batch020: actual current-phone proof/new phone/supplement tasks with receipt/address/purpose binding; actual eligibility response, separate later mutation scope and explicit confirm. Final five grammar files pending under memory floor; no runtime acceptance.")
        for platform in ["android", "ios", "desktop"]: row["sources"][platform] = native_new if row["key"] == "authNewPhone" else native_phone
        row["sources"]["web"] = web
ledger.update(updated="2026-10-30", execution_status="source_handoff_incomplete", execution_marker_released=True,
              next_step="Read five020 current grammar files only after normal >=1024MiB headroom; continue remaining auth deletion/session/return and all consumer/backend states. Task incomplete, no background execution or auto commit/push.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
nodes = {"authChangePhone": ["238:371750", "311:380004"], "authNewPhone": ["238:371751", "311:380008"], "authPhoneSupplement": ["238:371745", "311:379984"]}
for row in trace["states"]:
    if row["key"] in batch["states"]:
        for platform in ["android", "ios", "desktop"]: row[platform + "_source"] = native_new if row["key"] == "authNewPhone" else native_phone
        row["web_source"] = web
        row["implementation_status"] = "dedicated_source_path_asset_reviewed_final_grammar_runtime_unverified"
        row["note"] = "批次020：真实当前号验证／新号填写与交流前手机号验证，回执地址用途绑定；不替代后续变更用途验证或最终确认。最终5语法低内存待读，未类型／运行视觉。"
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + nodes[row["key"]]))
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items(): current["files"][file] = {"batch": "020", "sha256_lf": sha}
for file, row in current["files"].items(): assert digest(file) == row["sha256_lf"], file
save(current_file, current)
progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
value = progress.read_text(encoding="utf-8").replace("当前批次019已记录，用户再次继续，正在推进余项", "当前批次020已记录，本轮交接并释放执行标记，任务尚未全部完成")
section = """## 批次020：旧号验证、新号填写与交流前验证

2026-10-30：[六份源码及证据](figma-implementation-2026-10-29/batch-020-evidence.json)。手机／Web六稿上下文／图片已看。真实当前号码事实、旧号验证码与明确继续、新号码／验证码表单、独立恢复协助和交流前补充验证已接线。当前号采用现有verify_phone及真实资格读取；UI步进不等于变更授权，后续具体新号的用途范围／版本／再验证／明确最终确认全部保留。服务要求完整当前号码，所以在掩码事实之外保留真实输入，不伪造已发送。

实际回执、地址、用途与失效绑定；过期后需重新获取。验证后用真实资格回执替换Web缓存，防止加载重挂载丢失换号步骤；补上owner／epoch、迟到准备与旧操作跳转守卫。原生邮件变更同样绑定实际回执；用途提示区分最终按钮确认和仍需勾选的其他流程。

33shared／53UI／100Web／457引用及diff空白通过。初次五语法完整无诊断；之后四份改动及新增PurposeCheck调整的最终读取在907／746MiB低于1024门槛跳过，当前ui.tsx仍完整无诊断，五当前文件待读，最终指纹另记。未类型／编译／测试／启动／运行视觉。本轮交接，执行标记释放；全量任务未完成，无后台续做、未提交推送。

"""
value = value.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(row for row in value.splitlines() if row.startswith("当前恢复点："))
value = value.replace(old, "当前恢复点：批次001–020已记，执行标记释放、任务未完成。正常内存恢复后补020五份当前语法；继续认证删除／会话／返回状态及其余消费者／后台。部分受限合同与全量运行／视觉未完成，无后台任务。")
progress.write_text(value, encoding="utf-8", newline="\n")
replace_line("docs/ai/CONTEXT.md", "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`", "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 本轮交接、执行标记释放，任务未完成；main／48a3cfd，未提交。[005–020进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：短信／回执过期、关联／解除、旧号／新号／手机号补充验证已写。33shared／53UI／100Web／457引用通过；018补齐015–017，019四份语法通过；020初五完整后四变及PurposeCheck最终五份受907／746MiB门槛保护待读。未类型／编译／测试／启动。下一步补语法和认证删除／会话／返回及全量余项，无后台任务。")
replace_line("docs/ai/TASKS.md", "| AI-20261029-FIGMA-ALL-CODE ", "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 部分源码已写；本轮交接，未完成 | Codex / 2026-10-30；main／48a3cfd，执行标记释放，005–020未提交 | shared／公共Compose／Web、必要合同与逐批证据；不构建／测试／启动／自动提交推送 | [二十批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：旧号／新号与手机号补充验证；33shared／53UI／100Web／457引用；020最终五份语法低内存待读。补读后继续认证删除／会话／返回及全量余项，无后台任务 |")
replace_line("docs/CURRENT_BASELINE.md", "更新：", "更新：2026-10-30（Asia/Shanghai）。既有成果已在48a3cfd提交并推送；继续全部设计实现的批次005–020尚未提交。本轮交接、执行标记释放，任务未全部完成。本页取代更早入口数量，带日期交接保留当时结果。")
replace_line("docs/CURRENT_BASELINE.md", "当前进度：", "当前进度：[严格按Figma分批实施](client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)已记二十批；最新旧号／新号及手机号补充验证。33shared／53公共UI、100Web／457本地引用通过；019四语法通过，020初五完整后最终五份受内存门槛保护待读，旧015–017待读由018补齐。未类型／编译／测试／运行／全量视觉，任务未完成。")
replace_line("docs/client/README.md", "2026-10-30：", "2026-10-30：[批次005–020](FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)认证／账号安全、关联解除与旧号／新号／手机号补充验证；33shared／53公共UI／100Web／457引用通过。019四语法通过，020初五完整后最终五份低内存待读，旧待读由018补齐。未类型／编译／测试／启动／运行视觉；本轮执行标记释放，继续认证删除／会话／返回及全量余项。")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次020交接

- Codex，main／48a3cfd，保留全部未提交成果与原stash；本轮结束待接手、执行标记释放，全量未完成，无提交推送／后台任务。
- [六份源码证据](../client/figma-implementation-2026-10-29/batch-020-evidence.json)：手机／Web六稿已看；真实旧号验证、新号输入、交流前补充验证与回执／地址／用途绑定，后续新号变更范围／用途再验证／最终确认仍保留。修正资格缓存回读造成表单重挂载，以及owner／epoch／迟到操作与跳转边界。
- 33shared／53UI／100Web／457引用、diff空白通过；初五完整语法无诊断，后续四改＋PurposeCheck的最终五份在907／746MiB低于1024门槛跳过，当前指纹独立。未类型／编译／测试／启动／运行视觉。
- 下一步正常内存后补020五当前语法，再继续认证删除／会话／返回及消费者／后台余项；部分受限服务合同与全量运行视觉仍未完成。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "020", "source_files": len(files), "checkpoint_files": len(current["files"]), "grammar_pending": len(pending), "execution": "released_incomplete"}))
