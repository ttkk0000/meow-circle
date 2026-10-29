"""逐卷比较原生GFM回读与写入载荷，并记录实际分页/源摘要。"""
from pathlib import Path
import hashlib
import json
import re
import sys

BASE = Path(__file__).resolve().parent
ROOT = BASE.parents[2]


def normalize(markdown):
    # Specs规范化表头强调、软换行及空白；不删除正文字符、链接或代码标识。
    return re.sub(r'\s+', '', markdown.replace('**', '').replace('\\_', '_'))


def main():
    payload = json.loads((BASE / sys.argv[1]).read_text())
    manifest = json.loads((BASE / 'manifest.json').read_text())
    previous = {row['nodeId']: row for row in manifest.get('volumes', [])}
    failures = []
    pending = []
    for entry in payload:
        path = BASE / 'readbacks' / (entry['nodeId'].replace(':', '-') + '.json')
        if not path.exists():
            if '--partial' in sys.argv: pending.append(entry['nodeId'])
            else: failures.append(entry['nodeId'] + ': missing readback')
            continue
        result = json.loads(path.read_text())
        pages = result['pages']
        actual = '\n'.join(page['markdown'] for page in pages)
        complete = bool(pages) and pages[0]['offset'] == 0 and not pages[-1]['hasMore']
        complete = complete and all(pages[i+1]['offset'] == page['offset'] + page['linesReturned'] for i, page in enumerate(pages[:-1]))
        equivalent = normalize(entry['content']) == normalize(actual)
        source_matches = hashlib.sha256((ROOT / entry['path']).read_bytes()).hexdigest() == entry['sha256']
        record = {key: entry[key] for key in ['path', 'sha256', 'nodeId', 'pageId', 'part', 'parts', 'url', 'content_sha256']}
        record.update({'readbackFile': str(path.relative_to(ROOT)), 'pages':len(pages), 'lines':pages[-1]['totalLines'],
                       'paginationComplete': complete, 'normalizedContentMatches': equivalent,
                       'sourceShaMatches':source_matches, 'actualMarkdownSha256':hashlib.sha256(actual.encode()).hexdigest()})
        previous[entry['nodeId']] = record
        if not (complete and equivalent and source_matches): failures.append(entry['nodeId'])
    manifest['volumes'] = sorted(previous.values(), key=lambda row: (row['path'], row['part']))
    all_complete = not failures and len(previous) == manifest.get('plannedVolumes', -1)
    manifest['status'] = ('complete_native_fulltext_readback' if manifest.get('mainNavigationEditCount') else 'fulltext_readback_complete_pending_navigation') if all_complete else ('partial_fulltext_readback_complete' if not failures else 'readback_mismatch_requires_review')
    manifest['syncedFiles'] = len(set(row['path'] for row in previous.values()))
    manifest['syncedVolumes'] = len(previous)
    (BASE / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+'\n')
    validation = json.loads((BASE / 'validation.json').read_text())
    validation.update({'status':manifest['status'],'source_freeze':True if all_complete else 'partial','fulltext_written':True,
                       'synced_files':manifest['syncedFiles'],'synced_volumes':len(previous),
                       'synced_volumes_readback_complete':not failures,'pending_volumes':pending,'failures':failures})
    (BASE / 'validation.json').write_text(json.dumps(validation, ensure_ascii=False, indent=2)+'\n')
    print(json.dumps({'files':manifest['syncedFiles'],'volumes':len(previous),'failures':failures},ensure_ascii=False))
    if failures:raise SystemExit(1)


if __name__ == '__main__':main()
