"""Source/asset and isolated grammar reads only; no builds, type checks or tests."""
import ctypes
import hashlib
import json
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[3]
folder = Path(__file__).resolve().parent

def digest(file):
    return hashlib.sha256((root / file).read_text(encoding="utf-8").replace("\r\n", "\n").encode()).hexdigest()

def headroom():
    class Memory(ctypes.Structure):
        _fields_ = [("length", ctypes.c_ulong), ("load", ctypes.c_ulong)] + [(name, ctypes.c_ulonglong) for name in
            ["total_physical", "available_physical", "total_commit", "available_commit", "total_virtual", "available_virtual", "extended_virtual"]]
    memory = Memory()
    memory.length = ctypes.sizeof(memory)
    return min(memory.available_physical, memory.available_commit) // 1048576 if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)) else 0

mode = sys.argv[1]
batch = mode.rsplit("-batch", 1)[1] if "-batch" in mode else "005"
assert batch in {"005", "006", "007", "008", "009", "010", "011", "012", "013", "014", "015", "016", "017", "018", "019", "020"}
rows = []
if mode.startswith("checks"):
    for file in ["scripts/check_client_ui_static.py", "scripts/check_web_client_static.py"]:
        run = subprocess.run([sys.executable, "-X", "utf8", str(root / file)], cwd=root, capture_output=True, text=True, encoding="utf-8")
        rows.append({"file": file, "sha256_lf": digest(file), "exit_code": run.returncode, "stdout": run.stdout, "stderr": run.stderr})
        print(file, run.returncode, run.stdout.strip(), run.stderr.strip())
elif mode.startswith("syntax"):
    files = ["kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/" + name + ".kt" for name in
        ["AuthPages", "Components", "ConsumerApp", "FeaturePages", "ReadStatePages", "TaskDialogs"]]
    files += ["kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/" + name + ".kt" for name in ["MobileFeatureStore", "MobileStore"]]
    files += ["web-client/src/" + name for name in ["features/AuthDialog.tsx", "components/ui.tsx", "features/Records.tsx"]]
    if len(sys.argv) > 2:
        files = sys.argv[2:]
    reader = root / "docs/design/figma-study/collaboration-exit-2026-10-28/syntax-reader.cjs"
    modules = r"J:\node\node_modules\@colbymchenry\codegraph\node_modules\@colbymchenry\codegraph-win32-x64\lib\node_modules"
    for file in files:
        available = headroom()
        if available < 1024:
            rows.append({"file": file, "sha256_lf": digest(file), "status": "not_read_low_memory", "headroom_mb": available})
            print(file, "not read: low memory", available)
            continue
        run = subprocess.run([r"J:\node\node-18.14.1\node.exe", "--max-old-space-size=384", "--no-wasm-tier-up", "--wasm-num-compilation-tasks=1", str(reader), str(root), modules, file], cwd=root, capture_output=True, text=True, encoding="utf-8")
        row = {"file": file, "sha256_lf": digest(file), "exit_code": run.returncode, "headroom_mb": available}
        try:
            row["syntax"] = json.loads(run.stdout)
        except json.JSONDecodeError:
            row["output"] = run.stdout
        if run.stderr:
            row["stderr"] = run.stderr
        rows.append(row)
        print(file, "exit", run.returncode)
        if run.returncode:
            print(run.stdout[:2500], run.stderr[:1000])
elif mode.startswith("fragments"):
    modules = r"J:\node\node_modules\@colbymchenry\codegraph\node_modules\@colbymchenry\codegraph-win32-x64\lib\node_modules"
    for name in (["MobileApi", "MobileStore", "MobileFeatureStore"] if batch == "018" else ["MobileApi", "MobileStore", "AndroidPhotoPicker"] if batch == "011" else ["MobileStore"] if batch in {"010", "012", "014", "016"} else ["MobileApi", "MobileStore", "MobileFeatureStore"] if batch == "007" else ["MobileFeatureStore", "MobileStore"]):
        file = ("kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidPhotoPicker.kt" if name == "AndroidPhotoPicker" else "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/" + name + ".kt")
        if headroom() < 1024:
            rows.append({"file": file, "status": "not_read_low_memory"})
            continue
        reader = folder / ("read-profile-fragments.cjs" if batch in {"011", "012", "014", "016", "018"} else "read-session-fragments.cjs" if batch in {"007", "010"} else "read-auth-syntax.cjs")
        run = subprocess.run([r"J:\node\node-18.14.1\node.exe", "--max-old-space-size=384", "--no-wasm-tier-up", "--wasm-num-compilation-tasks=1", str(reader), modules, str(root / file)], cwd=root, capture_output=True, text=True, encoding="utf-8")
        row = {"file": file, "sha256_lf": digest(file), "exit_code": run.returncode, "stderr": run.stderr}
        try:
            row["fragment_syntax"] = json.loads(run.stdout)
        except json.JSONDecodeError:
            row["output"] = run.stdout
        rows.append(row)
        print(file, "fragment exit", run.returncode)
else:
    raise SystemExit("Unknown source-reader mode")
(folder / ("batch-" + batch + "-" + mode.split("-batch", 1)[0] + ".json")).write_text(json.dumps({"files": rows, "type_checked": False, "tests_executed": False, "runtime_verified": False}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
raise SystemExit(any(row.get("exit_code", 1) for row in rows))
