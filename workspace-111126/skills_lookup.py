#!/usr/bin/env python3
"""Find relevant source skills and display their verbatim text from skills_compilation.md."""
import argparse
import csv
import re
from pathlib import Path

BASE = Path(__file__).resolve().parent
INDEX = BASE / 'skills_index.csv'
COMPILATION = BASE / 'skills_compilation.md'


def tokens(text):
    return [x for x in re.findall(r'[a-z0-9]+', text.lower()) if len(x) > 1]


def read_skill(row):
    data = COMPILATION.read_bytes()
    return data[int(row['start_byte']):int(row['end_byte'])].decode('utf-8', errors='replace')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('query', nargs='*', help='Task or topic to match against skill names and descriptions')
    parser.add_argument('--show', metavar='ID', help='Print one full verbatim skill by ID, e.g. S0004')
    parser.add_argument('--top', type=int, default=8, help='Number of candidates to list (default: 8)')
    parser.add_argument('--deep', action='store_true', help='Also search full skill text; slower but still scans only 4 MB')
    args = parser.parse_args()

    with INDEX.open(encoding='utf-8', newline='') as f:
        rows = list(csv.DictReader(f))

    if args.show:
        row = next((r for r in rows if r['id'].upper() == args.show.upper()), None)
        if not row:
            raise SystemExit(f'Unknown skill ID: {args.show}')
        print(f"<!-- {row['id']} | {row['repository']}/{row['path']} -->")
        print(read_skill(row), end='')
        return

    if not args.query:
        parser.error('Provide a search query or --show ID')
    terms = set(tokens(' '.join(args.query)))
    ranked = []
    for row in rows:
        name = set(tokens(row['name']))
        path = set(tokens(row['repository'] + ' ' + row['path']))
        desc = set(tokens(row['description']))
        score = 5 * len(terms & name) + 3 * len(terms & path) + len(terms & desc)
        if args.deep and score == 0:
            body = set(tokens(read_skill(row)))
            score = sum(1 for term in terms if term in body)
        if score:
            ranked.append((score, row))
    ranked.sort(key=lambda x: (-x[0], x[1]['repository'], x[1]['name'], x[1]['id']))
    for score, row in ranked[:max(1, args.top)]:
        desc = row['description'].replace('\n', ' ').strip()
        print(f"{row['id']}  [{row['repository']}] {row['name']}  (match {score})")
        if desc:
            print(f"    {desc[:240]}")
        print(f"    path: {row['path']}")
    print(f"\n{min(len(ranked), max(1,args.top))} of {len(ranked)} matches. Use --show ID to read a full source skill.")


if __name__ == '__main__':
    main()
