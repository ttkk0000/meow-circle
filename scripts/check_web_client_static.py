"""只读核对Web源码引用、合同、资源和隐私边界；不运行客户端或类型检查。"""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
web = root / 'web-client'
files = sorted((web / 'src').rglob('*.ts')) + sorted((web / 'src').rglob('*.tsx'))
exports = {}
for file in files:
    source = file.read_text()
    assert not source.startswith('\ufeff'), file
    assert not re.search(r'^(?:<{7}|={7}|>{7})(?: |$)', source, re.M), file
    exports[file] = set(re.findall(r'export\s+(?:declare\s+)?(?:async\s+)?(?:function|const|class|interface|type|enum)\s+(\w+)', source))
    assert not re.search(r'\b(?:localStorage|sessionStorage)\b', source), ('凭据与私密输入不能写通用storage', file)
    assert 'https://www.figma.com/api/mcp/asset/' not in source, file

imports = 0
for file in files:
    source = file.read_text()
    for match in re.finditer(r'import\s+([^;]*?)\s+from\s+["\'](\.[^"\']+)["\']', source):
        base = file.parent / match[2]
        candidates = [Path(str(base) + suffix).resolve() for suffix in ['', '.ts', '.tsx', '.json', '/index.ts', '/index.tsx']]
        target = next((p for p in candidates if p.is_file()), None)
        assert target is not None, (file, match[2])
        imports += 1
        named = re.search(r'\{([^}]+)\}', match[1])
        if named and target.suffix in ('.ts', '.tsx'):
            for part in named[1].split(','):
                name = re.sub(r'^type\s+', '', part.strip()).split(' as ')[0].strip()
                if name: assert name in exports[target], ('本地导出缺失', file, target, name)

manifest = json.loads((web / 'public/assets/figma/manifest.json').read_text())
names = set()
android = root / 'kmp/androidApp/src/main/assets/figma'
for item in manifest['assets']:
    file = root / item['file']
    assert hashlib.sha256(file.read_bytes()).hexdigest() == item['sha256'], file
    assert file.read_bytes() == (android / file.name).read_bytes(), ('两端原SVG不同', file)
    svg = ET.parse(file).getroot()
    assert svg.attrib.get('width') == '24' and svg.attrib.get('height') == '24', file
    names.add(item['name'])
icons = set()
for file in files:
    icons.update(re.findall(r'<Icon\s+name="([^"]+)"', file.read_text()))
assert icons <= names, ('图标缺失', icons - names)
for font in ('noto_sans_sc', 'manrope'):
    file = web / f'public/assets/fonts/{font}.ttf'
    assert file.read_bytes() == (root / f'kmp/androidApp/src/main/res/font/{font}.ttf').read_bytes(), file
    assert (web / f'public/assets/fonts/{font}-OFL.txt').is_file()
assert (web / 'public/assets/mnd-cat-stickers.png').read_bytes() == (root / 'kmp/androidApp/src/main/res/drawable-nodpi/mnd_cat_stickers.png').read_bytes()
package = json.loads((web / 'package.json').read_text())
lock = json.loads((web / 'package-lock.json').read_text())
for group in ('dependencies', 'devDependencies'):
    assert package[group] == lock['packages'][''][group]
    for name, version in package[group].items():
        assert lock['packages']['node_modules/' + name]['version'] == version, name
workflows = (web / 'src/features/workflows.ts').read_text()
schemas = json.loads((web / 'src/core/schemas.json').read_text())
for name in re.findall(r'schema:\s*"(\w+)"', workflows):
    assert name in schemas, name
api = (web / 'src/core/api.ts').read_text()
assert 'routeAllowed(method, path)' in api and 'X-MND-CSRF' in api and 'options.owner' in api
wire = (web / 'src/core/wire.ts').read_text()
assert 'return BigInt(token)' in wire and 'eval(' not in wire
drafts = (web / 'src/core/drafts.ts').read_text()
assert 'current?.revision' in drafts and 'tx.abort()' in drafts and 'ownerKey(owner)' in drafts
assert 'reauth_token' in drafts and 'query_token' in drafts and 'uncertain' in drafts
print(f'Web只读检查：{len(files)}份TS/TSX、{imports}个本地引用、{len(schemas)}份schema、{len(names)}枚原SVG、字体／贴纸与依赖锁一致；未执行类型检查、测试或应用。')
