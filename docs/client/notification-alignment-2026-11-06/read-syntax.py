"""Isolated grammar reads, never builds, type checks or tests."""
import ctypes
import hashlib
import json
import subprocess
from pathlib import Path
root = Path(__file__).resolve().parents[3]
folder = Path(__file__).resolve().parent
files = [
    "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/Models.kt",
    "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/WorkspaceApi.kt",
    "kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client/MobileStore.kt",
    "kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui/ConsumerApp.kt",
    "web-client/src/core/notification.ts", "web-client/src/App.tsx",
    "web-client/src/features/NotificationPage.tsx", "web-client/src/features/ResourcePage.tsx",
]
class Memory(ctypes.Structure):
    _fields_ = [("length",ctypes.c_ulong),("load",ctypes.c_ulong)] + [(name,ctypes.c_ulonglong) for name in ["total_physical","available_physical","total_commit","available_commit","total_virtual","available_virtual","extended_virtual"]]
rows=[]
for name in files:
    memory=Memory();memory.length=ctypes.sizeof(memory)
    available=min(memory.available_physical,memory.available_commit)//1048576 if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)) else 0
    row={"file":name,"sha256_lf":hashlib.sha256((root/name).read_text(encoding="utf-8").encode()).hexdigest(),"headroom_mb":available}
    if available<1024:
        row["status"]="not_read_low_memory"
    else:
        run=subprocess.run([r"J:\node\node-18.14.1\node.exe","--max-old-space-size=384","--no-wasm-tier-up","--wasm-num-compilation-tasks=1",str(root/"docs/design/figma-study/collaboration-exit-2026-10-28/syntax-reader.cjs"),str(root),r"J:\node\node_modules\@colbymchenry\codegraph\node_modules\@colbymchenry\codegraph-win32-x64\lib\node_modules",name],cwd=root,capture_output=True,text=True,encoding="utf-8")
        row["exit_code"]=run.returncode
        try: row["syntax"]=json.loads(run.stdout)
        except json.JSONDecodeError: row["output"]=run.stdout[:1500]
        if run.stderr: row["stderr"]=run.stderr[:1500]
    rows.append(row)
    print(name,row.get("exit_code",row.get("status")))
folder.joinpath("syntax.json").write_text(json.dumps({"files":rows,"type_checked":False,"tests_executed":False,"runtime_verified":False},ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
memory=Memory();memory.length=ctypes.sizeof(memory)
available=min(memory.available_physical,memory.available_commit)//1048576 if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)) else 0
fragment_row={"headroom_mb":available}
if available<1024:
    fragment_row["status"]="not_read_low_memory"
else:
    run=subprocess.run([r"J:\node\node-18.14.1\node.exe","--max-old-space-size=384","--no-wasm-tier-up","--wasm-num-compilation-tasks=1",str(folder/"read-fragments.cjs"),str(root),r"J:\node\node_modules\@colbymchenry\codegraph\node_modules\@colbymchenry\codegraph-win32-x64\lib\node_modules"],cwd=root,capture_output=True,text=True,encoding="utf-8")
    fragment_row["exit_code"]=run.returncode
    try: fragment_row["syntax"]=json.loads(run.stdout)
    except json.JSONDecodeError: fragment_row["output"]=run.stdout[:2000]
    if run.stderr: fragment_row["stderr"]=run.stderr[:2000]
folder.joinpath("fragments.json").write_text(json.dumps(fragment_row,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("Targeted fragments:",fragment_row.get("exit_code",fragment_row.get("status")))
raise SystemExit(any(row.get("exit_code",1) for row in rows) or fragment_row.get("exit_code",1))
