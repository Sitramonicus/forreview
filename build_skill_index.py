from pathlib import Path
import csv, re, hashlib

source = Path('/home/user/skills_compilation.md')
index = Path('/home/user/skills_index.csv')
data = source.read_bytes()
start_re = re.compile(rb'<!-- BEGIN SOURCE: ([^|]+) \| sha256=([0-9a-f]{64}) -->\n')
rows = []
for n, marker in enumerate(start_re.finditer(data), 1):
    source_label = marker.group(1).decode('utf-8').strip()
    expected_sha = marker.group(2).decode()
    end_marker = f'\n<!-- END SOURCE: {source_label} -->'.encode()
    end = data.find(end_marker, marker.end())
    if end < 0:
        raise SystemExit(f'Missing end marker: {source_label}')
    body = data[marker.end():end]
    if hashlib.sha256(body).hexdigest() != expected_sha:
        raise SystemExit(f'Hash mismatch: {source_label}')
    text = body.decode('utf-8', errors='replace')
    repo, relpath = source_label.split('/', 1)
    name = description = ''
    fm = re.match(r'---\s*\n(.*?)\n---(?:\s*\n|$)', text, re.S)
    if fm:
        lines = fm.group(1).splitlines()
        for i, line in enumerate(lines):
            nm = re.match(r'^name:\s*(.*)$', line)
            if nm:
                name = nm.group(1).strip().strip('"\'')
            dm = re.match(r'^description:\s*(.*)$', line)
            if dm:
                vals = [dm.group(1).strip()]
                j = i + 1
                while j < len(lines) and (lines[j].startswith(' ') or lines[j].startswith('\t')):
                    vals.append(lines[j].strip())
                    j += 1
                description = ' '.join(x for x in vals if x not in {'>', '|', '>-', '|-'}).strip().strip('"\'')
                break
    if not name:
        h = re.search(r'(?m)^#\s+(.+)$', text)
        name = h.group(1).strip() if h else Path(relpath).parent.name
    rows.append({
        'id': f'S{n:04d}', 'repository': repo, 'path': relpath,
        'name': name, 'description': description[:700],
        'start_byte': marker.end(), 'end_byte': end,
        'start_line': data.count(b'\n', 0, marker.end()) + 1,
        'end_line': data.count(b'\n', 0, end) + 1,
    })
with index.open('w', encoding='utf-8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=['id','repository','path','name','description','start_byte','end_byte','start_line','end_line'])
    writer.writeheader()
    writer.writerows(rows)
print(f'Indexed {len(rows)} skills in {index} ({index.stat().st_size:,} bytes)')
