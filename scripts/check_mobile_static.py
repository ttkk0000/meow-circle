"""Read-only source/contract/asset checks. No Go execution, network or database."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
contract = json.loads((root / "docs/api/mobile-v2.openapi.json").read_text(encoding="utf-8"))
schemas = contract["components"]["schemas"]
refs = set(re.findall(r'#/components/schemas/(\w+)', json.dumps(contract)))
assert refs <= schemas.keys(), refs - schemas.keys()
operations = [v for path in contract["paths"].values() for k, v in path.items() if k in ("get", "post", "put", "patch", "delete")]
ids = [op["operationId"] for op in operations]
assert len(ids) == len(set(ids)), "duplicate operationId"
for op in operations:
    assert op["security"], op["operationId"]
assert not {"Session", "OTP", "Challenge", "Subject", "Account"} & schemas.keys(), "internal auth schemas exposed"

sql = (root / "migrations/007_mobile_modules.sql").read_text(encoding="utf-8")
policy_sql = (root / "migrations/008_mobile_policy_commands.sql").read_text(encoding="utf-8")
kinds_source = (root / "internal/mobile/repository.go").read_text(encoding="utf-8")
kinds = set(re.findall(r'\bKind\s*=\s*"([a-z_]+)"', kinds_source))
tables = set(re.findall(r'CREATE TABLE IF NOT EXISTS mobile_([a-z_]+)', sql))
tables |= set(re.search(r"FOREACH module IN ARRAY ARRAY\[(.*?)\]",policy_sql,re.S).group(1).replace("'", "").split(","))
assert kinds == tables, {"missing": kinds - tables, "extra": tables - kinds}
assert sql.count("BEGIN;") == 1 and sql.count("COMMIT;") == 1
for source in (root / "internal/mobile").glob("*.go"):
    if source.name.endswith("_test.go"):
        continue
    content = source.read_text(encoding="utf-8")
    assert not re.search(r'"(?:net/http|database/sql|github.com/)', content), source

catalogue = json.loads((root / "internal/mobile/catalog.json").read_text(encoding="utf-8"))
assert len(catalogue["emoji"]) == 128 and len(catalogue["stickers"]) == 24
all_ids = [v["id"] for v in catalogue["emoji"] + catalogue["stickers"]]
assert len(all_ids) == len(set(all_ids))
for entry in catalogue["emoji"] + catalogue["stickers"]:
    suffix = entry["asset_url"].removeprefix("/api/v2/expression-assets/")
    assert (root / "internal/platform/mobileapi/assets" / suffix).is_file(), suffix

sources = list((root / "internal/mobile").rglob("*")) + list((root / "internal/identity").rglob("*.go"))
sources += list((root / "internal/platform/mobileapi").glob("*.go")) + list((root / "internal/platform/mobileauth").glob("*.go"))
for file in sources:
    if file.is_file() and file.suffix in (".go", ".json"):
        assert not file.read_bytes().startswith(b"\xef\xbb\xbf"), file
print(f"Static only: {len(tables)} feature tables, {len(operations)} operations, {len(schemas)} schemas, 128/24 assets; no dangling references or BOM")
