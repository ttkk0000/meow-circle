"""Record real OTP fields and callback-binding corrections; no runtime promotion."""
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
def replace_line(file, prefix, line):
    text = read(file)
    old = next(value for value in text.splitlines() if value.startswith(prefix))
    (root / file).write_text(text.replace(old, line, 1), encoding="utf-8", newline="\n")

checks = json.loads((folder / "batch-017-checks.json").read_text(encoding="utf-8"))
syntax = json.loads((folder / "batch-017-syntax.json").read_text(encoding="utf-8"))
assert all(row["exit_code"] == 0 and row["sha256_lf"] == digest(row["file"]) for row in checks["files"])
assert len(syntax["files"]) == 10 and all(row["status"] == "not_read_low_memory" and row["headroom_mb"] < 1024 for row in syntax["files"])
changed_after_skip = []
for row in syntax["files"]:
    if row["sha256_lf"] != digest(row["file"]): changed_after_skip.append(row["file"])
assert changed_after_skip == ["kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthCodePages.kt"]
files = [row["file"] for row in syntax["files"]] + ["web-client/src/styles.css"]
old_pending = json.loads((folder / "batch-016-syntax.json").read_text(encoding="utf-8"))["files"]
pending = sorted({row["file"] for row in old_pending + syntax["files"]})
batch = {
    "id": "017", "updated": "2026-10-30", "status": "source_written_path_asset_reviewed_grammar_pending",
    "high_fidelity_design_nodes": ["238:371721", "311:379888", "238:371722", "311:379892"],
    "scope": ["phone-format field error task", "actual OTP resend task and six-cell real input", "seven native callback argument corrections"],
    "source_hashes_lf": {file: digest(file) for file in files}, "static_report": "batch-017-checks.json", "syntax_reports": ["batch-017-syntax.json"],
    "grammar_pending_source_hashes": {file: digest(file) for file in pending}, "source_changed_after_skip": changed_after_skip,
    "syntax_counts": {"files_read": 0, "current_batch_skipped_low_memory": 10, "pending_files_including_016": len(pending), "headroom_mb": 864, "minimum_headroom_mb": 1024},
    "static_counts": {"shared_sources": 33, "public_ui": 51, "web_sources": 98, "web_imports": 446},
    "contract_read_hashes_lf": {file: digest(file) for file in ["internal/platform/phoneotp/phoneotp.go", "internal/identity/email.go", "internal/identity/service.go"]},
    "states": ["authPhoneError", "authCodeResent"],
    "notes": [
        "Four phone/Web high-fidelity contexts/screenshots were inspected. The phone-error page uses the actual entry value, error border/caption, policy controls where configured, corrected-field resend and a quiet method switch. The label remains Already-bound phone, matching the current no-SMS-auto-signup contract rather than imposing the prototype's Mainland-only example on the existing server normalization.",
        "Shared/Web format helpers mirror the existing server's ASCII-digit normalization and 10–15 digit predicate; codes match the server's actual six numeric characters. This is formatting only, never ownership, phone verification or eligibility. Failed formatting sends no request.",
        "Six visual cells use one real editable field for keyboard/paste/accessibility. Native keeps the input node rendered with transparent text rather than hiding its semantics with a zero-alpha layer; decorative cells are excluded from screen-reader duplication. Web has one labeled numeric/one-time-code input, with focus and paste normalization. No sixth-character or keyboard callback auto-submits.",
        "A resend state is entered only after a successful send receipt for the same address/mode. It clears the previous code, shows the real masked address and latest-code notice, and still requires explicit server verification. Reset/registration keep their original purpose and password checks; changing an address/method never reuses an unrelated receipt.",
        "Source call-contract review found seven native callbacks passed positionally into Modifier/loading slots in batches013–016. They now use named onClick in AuthGuardPages/AuthKnownPages/ProviderAuthPages/AuthPages. This is a source correction, not a claim that earlier grammar reads established type correctness. A read of current button call text found no remaining bare callback/reference in those optional positions.",
        "Both path/asset scripts and diff whitespace pass. Ten grammar reads were skipped at 864MiB under the unchanged 1024MiB floor; the union with unresolved016 files is eighteen, plus its skipped MobileStore fragment read. The native OTP transparent-text adjustment happened after the skip; current hashes are recorded separately. No parser retry, type checker, build, test, browser or device operation was performed. Native OS autofill/accessibility and rendered geometry remain runtime-unverified."
    ],
    "remaining": ["18 current grammar files and MobileStore changed fragments after normal headroom", "SMS delayed/expired and remaining auth/consumer/backend states", "typed partial restriction contract and all platform runtime/visual acceptance"],
    "runtime_verified": False, "type_checked": False, "tests_executed": False, "whole_state_complete": False
}
assert len(pending) == 18
ledger_file = folder / "state-progress.json"
ledger = json.loads(ledger_file.read_text(encoding="utf-8"))
assert not any(row["id"] == "017" for row in ledger["batches"])
save(folder / "batch-017-evidence.json", batch)
ledger["batches"].append(batch)
for row in ledger["consumer_states"]:
    if row["key"] in batch["states"]:
        row["source_review"] = "dedicated_source_written_path_asset_reviewed_grammar_pending"
        row["reviewed_findings"].append("Batch 017: real phone-format error/resend state, one real input and six visual OTP cells, actual receipt binding and explicit verification. Current grammar skipped below memory floor; no runtime/OS/autofill acceptance.")
        for platform in ["android", "ios", "desktop"]:
            row["sources"][platform] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthCodePages.kt"
        row["sources"]["web"] = "web-client/src/features/AuthCodePages.tsx"
ledger.update(updated="2026-10-30", execution_status="active_resumed_by_user", execution_marker_released=False,
              next_step="Continue SMS delayed/expired and remaining states; current 18 grammar files and MobileStore fragments stay pending below 1024MiB. Seven callback argument mistakes were corrected by actual signature review.")
save(ledger_file, ledger)
trace_file = root / "docs/client/figma-implementation-trace-2026-10-24.json"
trace = json.loads(trace_file.read_text(encoding="utf-8"))
for row in trace["states"]:
    if row["key"] in batch["states"]:
        for platform in ["android", "ios", "desktop"]:
            row[platform + "_source"] = "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/AuthCodePages.kt"
        row["web_source"] = "web-client/src/features/AuthCodePages.tsx"
        row["implementation_status"] = "dedicated_source_path_asset_reviewed_grammar_runtime_unverified"
        row["note"] = "批次017：实际号码格式错误／真实发送回执后的重发、单真实输入与六格展示已写；不代替资格或服务验证。语法低内存待读，未编译／测试／运行／视觉验收。"
        nodes = ["238:371721", "311:379888"] if row["key"] == "authPhoneError" else ["238:371722", "311:379892"]
        row["remote_screenshots_inspected"] = list(dict.fromkeys(row["remote_screenshots_inspected"] + nodes))
trace["updated"] = "2026-10-30"
save(trace_file, trace)
current_file = folder / "current-source-checkpoint.json"
current = json.loads(current_file.read_text(encoding="utf-8"))
for file, sha in batch["source_hashes_lf"].items(): current["files"][file] = {"batch": "017", "sha256_lf": sha}
for file, row in current["files"].items(): assert digest(file) == row["sha256_lf"], file
save(current_file, current)

progress = root / "docs/client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md"
text = progress.read_text(encoding="utf-8").replace("当前批次016已记录", "当前批次017已记录")
section = """## 批次017：手机号错误、真实重发与六格验证码

2026-10-30：[十一份源码及边界](figma-implementation-2026-10-29/batch-017-evidence.json)。四个手机／Web高保真上下文／图片已读。专用手机号错误页、字段红框与说明、修改后重发和切换方式；格式沿用服务端ASCII规范化／10–15位，标签为当前已绑定手机号，不强行把大陆示例变成新的地区准入。六格验证码由单真实输入控件承载键盘／粘贴，装饰格不自动提交。原生真实输入用透明文字保持语义节点，Web保留one-time-code和统一焦点；实际OS／无障碍行为待运行。

重发状态只在同地址／同方式的实际发送回执之后进入，清旧输入并要求最新验证码和明确验证；不把格式或发送当手机号资格。另按真实组件签名纠正此前七处原生回调误占Modifier／loading位置，统一具名onClick；这是源码调用修正，不能用语法读取抵扣类型正确。

33shared／51UI／98Web／446引用和diff空白读取通过。10语法在864MiB低于1024门槛跳过，与016合计18当前文件及MobileStore片段待读；透明文字微调发生于跳过之后，当前hash另记。未编译／类型检查／测试／启动／逐端视觉。

"""
text = text.replace("## 全量追踪及继续顺序", section + "## 全量追踪及继续顺序", 1)
old = next(line for line in text.splitlines() if line.startswith("当前恢复点："))
text = text.replace(old, "当前恢复点：批次001–017已记，继续短信延迟／过期与其余认证／消费者／后台。七处回调参数已纠正，当前18语法及片段低内存待读；部分受限合同与全量运行／视觉未完成。")
progress.write_text(text, encoding="utf-8", newline="\n")
replace_line("docs/ai/CONTEXT.md", "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE`", "- 2026-10-30 `AI-20261029-FIGMA-ALL-CODE` 执行中；main／48a3cfd，未提交。[005–017进度](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：专用资料／返回／关联／恢复／旧身份／账号守卫及号码错误／真实重发六格输入已写；七处回调位置修正。33shared／51UI／98Web／446引用通过；当前18语法及MobileStore片段低于1024MiB待读，未编译／类型检查／测试／启动。继续短信延迟／过期及全量余项，部分受限合同／运行视觉未完成，无后台任务。")
replace_line("docs/ai/TASKS.md", "| AI-20261029-FIGMA-ALL-CODE ", "| AI-20261029-FIGMA-ALL-CODE 全部Figma对应代码严格落实 | 进行中；005–017分批源码已记 | Codex / 2026-10-30；main／48a3cfd，执行标记占用，未提交 | shared／公共Compose／Web、必要合同与逐批证据；不构建／测试／启动／自动提交推送 | [十七批记录](../client/FIGMA_IMPLEMENTATION_PROGRESS_2026_10_29.md)：号码错误／真实重发六格、七处回调修正；33shared／51UI／98Web／446引用；18语法及片段低内存待读。继续短信与全量余项，受限合同／全量运行视觉保留 |")
for file in ["docs/CURRENT_BASELINE.md", "docs/client/README.md"]:
    text = read(file).replace("005–016", "005–017").replace("已记十六批", "已记十七批").replace("32份shared／50份公共UI、96份Web／441本地引用", "33份shared／51份公共UI、98份Web／446本地引用").replace("32 / 50", "33 / 51").replace("32shared／50公共UI／96Web／441引用", "33shared／51公共UI／98Web／446引用").replace("当前13份语法及片段", "当前18份语法及片段")
    (root / file).write_text(text, encoding="utf-8", newline="\n")
log = root / "docs/ai/TASK_LOG.md"
entry = """## AI-20261029-FIGMA-ALL-CODE / 2026-10-30 / 批次017

- Codex，main／48a3cfd，保留全部成果；用户继续，执行中，未提交推送。
- [证据](../client/figma-implementation-2026-10-29/batch-017-evidence.json)：四张高保真已看，手机号字段错误／真实回执后的重发与单输入六格已写；保留服务端格式及用途绑定、不自动验证。实际签名复核纠正七处原生按钮回调误占参数，具名onClick。
- 33shared／51UI／98Web／446引用和diff空白通过；10语法864MiB低于1024门槛跳过，累计18当前文件及片段待读，后续透明文字微调hash另记。未编译／类型检查／测试／启动，继续短信延迟／过期与全量余项，无后台任务。

"""
log.write_text(log.read_text(encoding="utf-8").replace("# AI 工作结果日志\n", "# AI 工作结果日志\n\n" + entry, 1), encoding="utf-8", newline="\n")
print(json.dumps({"batch": "017", "source_files": len(files), "checkpoint_files": len(current["files"]), "grammar_pending": len(pending), "execution": "active"}))
