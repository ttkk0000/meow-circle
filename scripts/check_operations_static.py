"""只读核对技术运维合同、源码引用和权限／资源边界，不构建或运行应用。"""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
def read(path): return (root/path).read_text(encoding="utf-8")
contract = json.loads(read("docs/api/operations-v1.openapi.json"))
assert contract["servers"] == [{"url": "/api/ops/v1"}]
assert contract["security"] == [{"OperatorToken": []}]
assert contract["components"]["securitySchemes"]["OperatorToken"]["name"] == "X-Operator-Token"
paths = contract["paths"]
assert len(paths) == 13 and sum(len(ops) for ops in paths.values()) == 15
assert not any(re.search(r"execute|restart|rollback|retry|sql", path) for path in paths)
for methods in paths.values():
    for operation in methods.values():
        assert operation["x-required-role"]
        assert {"200", "400", "401", "403", "404", "409", "503"} <= operation["responses"].keys()
schemas = contract["components"]["schemas"]
for source, names in [
    ("internal/operations/service.go", ["Run", "Incident", "Event", "Release", "Audit", "Check", "Preflight", "Snapshot", "Command", "IncidentInput", "ReleaseUpdateInput"]),
    ("internal/operations/migrations.go", ["Migration"]),
    ("internal/platform/opsapi/handler.go", ["Context", "Capabilities"]),
]:
    text = read(source)
    for name in names:
        body = re.search(r"type " + name + r" struct \{([\s\S]*?)\n\}", text)
        assert body, (source, name)
        fields = set(re.findall(r'json:"([^",]+)(?:,[^"]+)?"', body[1]))
        assert fields == set(schemas[name]["properties"]), (name, fields, schemas[name]["properties"].keys())
assert schemas["Preflight"]["properties"]["can_execute"]["enum"] == [False]
for name in ["Run", "Incident", "Release", "Audit"]:
    document = schemas[name+"Document"]["properties"]
    assert document["version"]["type"] == "string"
    assert schemas[name+"Page"]["properties"]["next_cursor"]["type"] == "string"

handler = read("internal/platform/opsapi/handler.go")
for guard in ["subtle.ConstantTimeCompare", "op.Environments", "scope &&", "ops_oncall", "ops_release", "Origin", "32*1024+1", "DisallowUnknownFields", "caps.IncidentWrite", "caps.ReleaseWrite", "private, no-store"]:
    assert guard in handler, guard
assert "op.Consumers" not in handler
assert 'r.mux.Handle("/api/ops/v1/", r.operationsAPI)' in read("internal/platform/api/router.go")
assert 's.Enabled = false' in read("internal/platform/api/operations.go")
service = read("internal/operations/service.go")
for guard in ['tx.Get("commands", receiptID)', "receipt.Hash != expectedHash", 'tx.Save("commands", &receipt)', 'tx.Save("audit", &a)', "r.Version != in.Version", "d.Assignee != actor", 'v.Outcome == "idle" || v.Outcome == "continuation"', "v.Processed > 0", "v.FinishedAt.After(r.CreatedAt)", "in.RecordVersion", 'r.State != "blocked"', '"reference_only"']:
    assert guard in service, guard
assert "CanExecute:" not in service
for path in ["internal/operations/postgres.go", "internal/operations/memory.go"]:
    text = read(path)
    assert all('kind == "'+kind+'"' in text for kind in ["audit", "commands", "runs"])
postgres = read("internal/operations/postgres.go")
assert "environment=$1" in postgres and "version=$6" in postgres and "pgx.Serializable" in postgres
observer = read("internal/platform/mobileapi/job_observer.go")
assert 'out.RetryAfterSeconds == 30' in observer and '"continuation_pending"' in observer
assert "!errors.As(err, &invalid)" in observer and "op.permitsConsumer(job)" in observer
assert "job_failed" in observer and "err.Error()" not in observer
sql = read("migrations/010_operations_console.sql")
assert "PRIMARY KEY(environment,kind,record_id)" in sql
assert sql.strip().endswith("COMMIT;") and "INSERT INTO" not in sql

directory = root/"web-client/src/operations"
sources = list(directory.glob("*.ts")) + list(directory.glob("*.tsx"))
joined = "\n".join(p.read_text() for p in sources)
assert not re.search(r"\b(?:localStorage|sessionStorage|indexedDB|eval)\b", joined)
assert "https://www.figma.com/api/mcp/asset/" not in joined
client = read("web-client/src/operations/client.ts")
for guard in ['credentials: "omit"', 'cache: "no-store"', '"X-Operator-Token": this.token', "generation", "controller.abort()", "request_timeout"]:
    assert guard in client, guard
assert 'lazy(() => import("./operations/OpsApp"))' in read("web-client/src/main.tsx")
app = read("web-client/src/operations/OpsApp.tsx")
assert "session.client.epoch" in app and 'demo ? demoSnapshot : session.snapshot' in app
for view in ["overview", "services", "workers", "releases", "migrations", "alerts", "audit", "access", "release", "worker", "incident", "confirm", "disconnected", "forbidden"]:
    assert re.search(r'"'+view+r'"|\b'+view+r"\s*:", app), view
forms = read("web-client/src/operations/ActionForms.tsx")
assert "crypto.randomUUID()" in forms and "pending ??" in forms and "!pending" in forms
assert 'return client.request<Document<T>>' in read("web-client/src/operations/useOperations.ts")
css = read("web-client/src/operations/operations.css")
assert "216px minmax(0, 1fr)" in css and "80px minmax(920px, 1fr)" in css
assert "overflow-x: auto" in css and "@media (max-width: 599px)" in css

manifest = json.loads(read("web-client/public/assets/ops/manifest.json"))
assert len(manifest["assets"]) == 10
for asset in manifest["assets"]:
    path = root/asset["file"]
    data = path.read_bytes()
    assert data and hashlib.sha256(data).hexdigest() == asset["sha256"], path
    svg = ET.fromstring(data)
    assert (svg.get("width"), svg.get("height"), svg.get("viewBox")) == ("24", "24", "0 0 24 24"), path
    assert (asset["width"], asset["height"]) == (24,24)
for path in sources+[root/"docs/server/OPERATIONS_CONSOLE_2026_10_25.md"]:
    raw = path.read_bytes(); text = raw.decode("utf-8")
    assert not raw.startswith(b"\xef\xbb\xbf")
    assert not re.search(r'^(?:<{7}|={7}|>{7})(?:\s|$)', text, re.M), path
print(f"运维静态核对：14类页面、{len(paths)}路径／15操作／{len(schemas)}schemas、环境与角色／回执／版本守卫、010迁移、10枚24px原SVG；未编译、类型检查、测试或启动。")
