"""Record inspected static checks and file preservation; never compile or execute tests."""
import ast
import datetime
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote
root=Path(__file__).resolve().parents[3]
folder=Path(__file__).resolve().parent

def run(args):
    result=subprocess.run(args,cwd=root,capture_output=True,text=True,encoding="utf-8")
    return {"command":args,"exit_code":result.returncode,"stdout":result.stdout,"stderr":result.stderr}
def sha(name,lf=False):
    raw=(root/name).read_bytes()
    if lf:raw=raw.decode("utf-8").replace("\r\n","\n").encode()
    return hashlib.sha256(raw).hexdigest()
modified=subprocess.check_output(["git","-c","core.quotepath=false","diff","--name-only"],cwd=root,text=True,encoding="utf-8").splitlines()
new_files=["internal/mobile/notification_anchor_test.go","web-client/src/core/notification.ts","docs/client/NOTIFICATION_ALIGNMENT_2026_11_06.md"]
files=sorted(set(modified+new_files+[str(p.relative_to(root)).replace("\\","/") for p in folder.iterdir() if p.is_file() and p.name!="checks.json"]))
report={"task":"AI-20261106-NOTIFICATION-ALIGN","time":datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=8))).isoformat(),"mode":"static_only","type_checked":False,"compiled":False,"tests_executed":False,"runtime_verified":False,"commands":[]}
report["git"]={"branch":subprocess.check_output(["git","branch","--show-current"],cwd=root,text=True).strip(),"head":subprocess.check_output(["git","rev-parse","HEAD"],cwd=root,text=True).strip(),"origin_main":subprocess.check_output(["git","rev-parse","origin/main"],cwd=root,text=True).strip(),"ahead_behind":subprocess.check_output(["git","rev-list","--left-right","--count","HEAD...origin/main"],cwd=root,text=True).strip(),"fetch_and_fast_forward":"Previously completed in this task; ff-only returned Already up to date; no new fetch in this recorder."}
# gofmt parses and formats only the changed Go source, without invoking the Go driver.
go_files=[name for name in files if name.endswith(".go")]
for name in go_files:
    raw=(root/name).read_bytes()
    result=subprocess.run([r"C:\Program Files\Go\bin\gofmt.exe"],input=raw,capture_output=True)
    if result.returncode:raise SystemExit(name+result.stderr.decode())
    if raw!=result.stdout:(root/name).write_bytes(result.stdout)
report["go_syntax_format"]={"files":go_files,"count":len(go_files),"status":"parsed_and_formatted_only"}
# Re-derive the contract once and prove formatting did not alter its meaning or output.
contract_file="docs/api/mobile-v2.openapi.json"
before=sha(contract_file,True)
report["commands"].append(run([sys.executable,"-X","utf8","scripts/export_mobile_contract.py"]))
report["contract_regeneration_unchanged"]=before==sha(contract_file,True)
for script in ["scripts/sync_web_contract.py","scripts/check_server_shared_contract_static.py","scripts/check_client_ui_static.py","scripts/check_web_client_static.py"]:
    report["commands"].append(run([sys.executable,"-X","utf8",script]))
report["commands"].append(run(["git","-c","core.safecrlf=false","diff","--check"]))
original=json.loads(folder.joinpath("existing-untracked.json").read_text(encoding="utf-8"))
differences=[name for name,digest in original.items() if not (root/name).is_file() or sha(name)!=digest]
report["existing_untracked"]={"count":len(original),"changed_or_missing":differences,"status":"preserved" if not differences else "differences"}
encoding=[];links=[];python_files=[]
for name in files:
    raw=(root/name).read_bytes()
    try:source=raw.decode("utf-8")
    except UnicodeDecodeError:encoding.append({"file":name,"problem":"invalid_utf8"});continue
    if raw.startswith(b"\xef\xbb\xbf"):encoding.append({"file":name,"problem":"bom"})
    if name.endswith(".py"):ast.parse(source,filename=name);python_files.append(name)
    if not name.endswith(".md"):continue
    for match in re.finditer(r"!?\[[^\]]*\]\(([^)]+)\)",source):
        target=match.group(1).strip().split(' "')[0].strip("<>")
        if re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:",target) or target.startswith("#"):continue
        relative=unquote(target.split("#",1)[0])
        destination=(root/name).parent/relative
        if relative and not destination.exists() and destination.resolve()!=folder.joinpath("checks.json").resolve():links.append({"file":name,"target":target})
report["encoding_issues"]=encoding;report["local_link_issues"]=links;report["python_ast_files"]=python_files
report["source_sha256_lf"]={name:sha(name,True) for name in files if Path(name).suffix in {".go",".kt",".ts",".tsx",".py",".cjs",".json"}}
report["syntax_evidence"]={name:sha(str(folder.joinpath(name).relative_to(root))) for name in ["syntax.json","fragments.json"]}
report["passed"]=all(c["exit_code"]==0 for c in report["commands"]) and report["contract_regeneration_unchanged"] and not differences and not encoding and not links
folder.joinpath("checks.json").write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
for item in report["commands"]:print(Path(item["command"][-1]).name,item["exit_code"],item["stdout"].strip())
print("Go syntax files",len(go_files),"preserved untracked",len(original),"changed",len(differences),"encoding",len(encoding),"links",len(links),"passed",report["passed"])
if links:print(json.dumps(links,ensure_ascii=False))
raise SystemExit(0 if report["passed"] else 1)
