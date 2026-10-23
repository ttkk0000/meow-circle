"""派生Web字段、表单schema与消费者路由；默认只检查，不编译或执行应用。"""
import argparse
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
document = json.loads((root / 'docs/api/mobile-v2.openapi.json').read_text())
schemas = dict(document['components']['schemas'])
for name, path in [('LeadInput', '/api/v2/cases/{id}/leads'), ('ReviewSupplement', '/api/v2/reviews/{id}/supplement')]:
    schemas[name] = document['paths'][path]['post']['requestBody']['content']['application/json']['schema']


def ts(schema):
    if '$ref' in schema:
        return 'DTO' + schema['$ref'].split('/')[-1]
    if 'anyOf' in schema:
        return ' | '.join(ts(item) for item in schema['anyOf'])
    kind = schema.get('type')
    if kind == 'integer':
        return 'bigint'
    if kind in ('boolean', 'number', 'string', 'null'):
        return kind
    if kind == 'array':
        return 'Array<' + ts(schema['items']) + '>'
    if 'properties' in schema:
        required = schema.get('required', [])
        return '{ ' + '; '.join(json.dumps(key) + ('' if key in required else '?') + ': ' + ts(value) for key, value in schema['properties'].items()) + ' }'
    if kind == 'object':
        return '{ [key: string]: ' + ts(schema.get('additionalProperties') if isinstance(schema.get('additionalProperties'), dict) else {}) + ' }'
    return 'unknown'


header = '/** 从现行OpenAPI派生；整数为bigint。字段可选性只反映schema，业务必填项与资格仍由服务端校验。 */\n'
types = header + ''.join(f'export type DTO{name} = {ts(schema)};\n' for name, schema in schemas.items())
routes = []
for path, methods in document['paths'].items():
    if not path.startswith('/api/v2/') or path.startswith('/api/v2/ops/'):
        continue
    for method in methods:
        if method not in ['get', 'post', 'put', 'patch', 'delete', 'head']:
            continue
        template = path[len('/api/v2/'):]
        parts = []
        for part in template.split('/'):
            if part.startswith('{'):
                name = part[1:-1]
                parts.append('[1-9][0-9]{0,18}' if name in ['id', 'user_id'] else '[A-Za-z0-9_.-]{1,255}')
            else:
                parts.append(re.escape(part))
        routes.append((method.upper(), '^' + '/'.join(parts) + '$'))
route_source = '/** 消费者v2路径白名单；运营接口不进入浏览器客户端。由sync_web_contract.py维护。 */\n'
route_source += 'const routes: Array<[string, RegExp]> = [\n' + ''.join('  [' + json.dumps(method) + ', new RegExp(' + json.dumps(pattern) + ')],\n' for method, pattern in routes) + '];\n'
route_source += 'export function routeAllowed(method: string, path: string) { return routes.some(([verb, pattern]) => verb === method && pattern.test(path.split("?")[0])); }\n'
outputs = {'web-client/src/core/schemas.json': json.dumps(schemas, ensure_ascii=False, indent=2) + '\n',
           'web-client/src/core/contract.ts': types, 'web-client/src/core/routes.ts': route_source}
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--write', action='store_true')
args = parser.parse_args()
for name, source in outputs.items():
    path = root / name
    if args.write:
        path.write_text(source)
    else:
        assert path.read_text() == source, f'{name}与现行OpenAPI不一致；用--write同步派生文件'
print(f'Web合同派生：{len(schemas)}份schema、{len(routes)}个消费者操作；只读检查' if not args.write else f'Web合同派生文件已同步：{len(schemas)}份schema、{len(routes)}个消费者操作；未编译。')
