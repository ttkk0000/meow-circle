"""准备Figma增量原生Specs载荷；只读仓库源与Git，不调用构建或应用。"""
from pathlib import Path
import hashlib
import json
import posixpath
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
BASE = 'b269504ad0cd0e179d8108fe60b9073ab97e9c99'
FILE_KEY = 'JiFXEy785BcGEvaFBSswJ3'


def digest(value):
    return hashlib.sha256(value).hexdigest()


def split_markdown(markdown, limit=14500):
    """优先按空段切分；不切断代码块或表格。超长表格单独一卷。"""
    blocks = []
    current = []
    fence = None
    for line in markdown.splitlines(keepends=True):
        marker = re.match(r'^\s*(`{3,}|~{3,})', line)
        if marker:
            token = marker.group(1)[0]
            fence = None if fence == token else token if fence is None else fence
        current.append(line)
        if not line.strip() and fence is None:
            blocks.append(''.join(current)); current = []
    if current: blocks.append(''.join(current))
    parts = []; chunk = ''
    for block in blocks:
        if chunk and len(chunk) + len(block) > limit:
            parts.append(chunk.rstrip() + '\n'); chunk = ''
        chunk += block
    if chunk: parts.append(chunk.rstrip() + '\n')
    return parts


def prepare(paths):
    result = []
    for source in paths:
        raw = (ROOT / source).read_bytes()
        text = raw.decode('utf-8')
        assert not raw.startswith(b'\xef\xbb\xbf')
        title = next((line[2:].strip() for line in text.splitlines() if line.startswith('# ')), source)
        parts = split_markdown(text)
        result.append({'path': source, 'sha256': digest(raw), 'title': title,
                       'parts': [{'part': i+1, 'parts': len(parts), 'body': body} for i, body in enumerate(parts)]})
    return result


def resolve_content(files, node_map):
    old = json.loads((ROOT / 'docs/figma-sync/manifest.json').read_text())
    links = {row['path']: row['volumes'][0]['url'] for row in old['files']}
    links.update({row['path']: row['url'] for row in old['images']})
    links.update({row['source']: row['url'] for row in old['diagrams']})
    links.update({path: rows[0]['url'] for path, rows in node_map.items()})
    links.update({
        'docs/figma-sync/delta-2026-10-29/README.md': 'https://www.figma.com/design/JiFXEy785BcGEvaFBSswJ3?node-id=24-3',
        'docs/design/figma-study/admin-roles-2026-10-28/README.md': 'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=888-1403',
        'docs/design/figma-study/admin-task-interaction-2026-10-29/README.md': 'https://www.figma.com/design/3OFOC3vu18Kx9sRPjummOC?node-id=932-2803',
    })
    unpublished = set()

    def rewrite(source, match):
        image, label, target = match.groups()
        if re.match(r'^(?:https?:|mailto:|#|data:)', target): return match.group(0)
        relative = target.split('#')[0]
        if not relative: return match.group(0)
        path = posixpath.normpath(posixpath.join(posixpath.dirname(source), relative))
        if path in links:
            return f'[{label}]({links[path]})'
        try:
            old_bytes = subprocess.check_output(['git', 'show', f'{BASE}:{path}'], cwd=ROOT, stderr=subprocess.DEVNULL)
        except subprocess.CalledProcessError:
            old_bytes = None
        current = ROOT / path
        if old_bytes is not None and current.is_file() and digest(old_bytes) == digest(current.read_bytes()):
            return f'[{label}](https://github.com/ttkk0000/meow-circle/blob/{BASE}/{path})'
        unpublished.add(path)
        return f'{label}（仓库证据：`{path}`；本轮未发布）'

    for row in files:
        nodes = node_map[row['path']]
        for i, part in enumerate(row['parts']):
            body = re.sub(r'(!?)\[([^\]]+)\]\(([^)]+)\)', lambda match: rewrite(row['path'], match), part['body'])
            # 后续分卷仍保留自己的H1；原正文不丢弃，首卷保留原H1。
            if i:
                body = f"# {row['title']} · 第{i+1}/{len(nodes)}卷\n\n" + body
            footer = f"\n\n---\n\n来源：`{row['path']}`；SHA-256：`{row['sha256']}`。2026-10-29阅读快照，源文件是维护输入；设计、源码、静态、运行及发布分别记账。\n\n"
            footer += '分卷：' + ' · '.join(f"[第{x['part']}/{len(nodes)}卷]({x['url']})" for x in nodes)
            part['content'] = body.rstrip() + footer + '\n'
            part.update(nodes[i])
            part['content_sha256'] = digest(part['content'].encode())
    return sorted(unpublished)


def main():
    mode = sys.argv[1]
    if mode == 'prepare':
        paths = json.loads((OUT / 'sources.json').read_text())
        files = prepare(paths)
        (OUT / 'payload.json').write_text(json.dumps(files, ensure_ascii=False, indent=2) + '\n')
        print(json.dumps({'files':len(files),'volumes':sum(len(x['parts']) for x in files)}, ensure_ascii=False))
    elif mode == 'resolve':
        files = json.loads((OUT / 'payload.json').read_text())
        mapping = json.loads((OUT / 'nodes.json').read_text())
        for row in files:
            assert digest((ROOT / row['path']).read_bytes()) == row['sha256'], f"源已变：{row['path']}"
        unpublished = resolve_content(files, mapping)
        (OUT / 'payload.json').write_text(json.dumps(files, ensure_ascii=False, indent=2)+'\n')
        (OUT / 'unpublished-references.json').write_text(json.dumps(unpublished, ensure_ascii=False, indent=2)+'\n')
        print(json.dumps({'unpublished_references':unpublished}, ensure_ascii=False))
    else:
        raise ValueError(mode)


if __name__ == '__main__': main()
