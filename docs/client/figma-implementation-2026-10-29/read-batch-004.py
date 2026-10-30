"""Inspect source / capture static evidence only; never builds or runs application/tests."""
import ctypes
import hashlib
import json
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[3]
folder = Path(__file__).resolve().parent
mode = sys.argv[1]

def sha(path):
    return hashlib.sha256((root / path).read_text(encoding="utf-8").replace("\r\n", "\n").encode()).hexdigest()

def headroom():
    class Memory(ctypes.Structure):
        _fields_ = [("length", ctypes.c_ulong), ("load", ctypes.c_ulong)] + [(name, ctypes.c_ulonglong) for name in
            ["total_physical", "available_physical", "total_commit", "available_commit", "total_virtual", "available_virtual", "extended_virtual"]]
    memory = Memory()
    memory.length = ctypes.sizeof(memory)
    if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)):
        return 0
    return min(memory.available_physical, memory.available_commit) // (1024 * 1024)

if mode == "inspect":
    text = (root / "web-client/src/components/ui.tsx").read_text(encoding="utf-8")
    start = text.index("export function Button")
    print(text[start:start + 850])
    text = (root / "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/FeaturePages.kt").read_text(encoding="utf-8")
    print("\n".join(line for line in text.splitlines() if "fun FeatureColumn" in line))
    print("Available source-reader headroom MB:", headroom())
elif mode == "checks":
    rows = []
    for file in ["scripts/check_client_ui_static.py", "scripts/check_web_client_static.py"]:
        result = subprocess.run([sys.executable, "-X", "utf8", str(root / file)], cwd=root, capture_output=True, text=True, encoding="utf-8")
        rows.append({"file": file, "sha256_lf": sha(file), "exit_code": result.returncode, "stdout": result.stdout, "stderr": result.stderr})
        print(file, "exit", result.returncode, result.stdout.strip(), result.stderr.strip())
    (folder / "batch-004-static.json").write_text(json.dumps({"checks": rows, "runtime_verified": False}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    raise SystemExit(any(row["exit_code"] for row in rows))
elif mode == "syntax":
    files = ["kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/" + name + ".kt" for name in
        ["ConsumerApp", "PageComponents", "ReadStatePages", "LoadStates", "FeaturePages", "ClientMetrics"]]
    files += ["kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileFeatureStore.kt"]
    files += ["web-client/src/" + name for name in ["components/ui.tsx", "features/Records.tsx", "features/WorkflowEditor.tsx"]]
    reader = root / "docs/design/figma-study/collaboration-exit-2026-10-28/syntax-reader.cjs"
    modules = r"J:\node\node_modules\@colbymchenry\codegraph\node_modules\@colbymchenry\codegraph-win32-x64\lib\node_modules"
    rows = []
    for file in files:
        available = headroom()
        if available < 1024:
            rows.append({"file": file, "status": "not_run_memory_headroom", "headroom_mb": available, "sha256_lf": sha(file)})
            print(file, "not parsed: low memory headroom", available)
            continue
        result = subprocess.run([r"J:\node\node-18.14.1\node.exe", "--max-old-space-size=384", "--no-wasm-tier-up", "--wasm-num-compilation-tasks=1", str(reader), str(root), modules, file], cwd=root, capture_output=True, text=True, encoding="utf-8")
        row = {"file": file, "headroom_mb": available, "exit_code": result.returncode, "sha256_lf": sha(file)}
        try:
            row["syntax"] = json.loads(result.stdout)
        except json.JSONDecodeError:
            row["output"] = result.stdout
        if result.stderr:
            row["stderr"] = result.stderr
        rows.append(row)
        print(file, "exit", result.returncode)
        if result.returncode:
            print(result.stdout[:1200], result.stderr[:600])
    (folder / "batch-004-syntax.json").write_text(json.dumps({"type_checked": False, "runtime_verified": False, "files": rows}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    raise SystemExit(any(row.get("exit_code", 0) for row in rows))
else:
    raise SystemExit("Unknown source-reader mode")
