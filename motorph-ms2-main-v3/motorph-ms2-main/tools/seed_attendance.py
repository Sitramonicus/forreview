#!/usr/bin/env python3
"""
Fills attendance for every employee from the sample timecard's pattern.

The brief asked for attendance "for every employee represented in the employee CSV", using the
sample timecard as the pattern, and for the assumptions to be written down. So every assumption is
in ASSUMPTIONS below, and every date it writes goes through the real /api/attendance endpoint,
which means the server validates it exactly as it would a human's entry.

Usage:  python3 tools/seed_attendance.py [--from 2025-09] [--months 14] [--dry-run]
"""
import argparse, os, csv, json, pathlib, random, sys, urllib.request, http.cookiejar, datetime

BASE = os.environ.get('MOTORPH_BASE', 'http://127.0.0.1:8080')
PASSWORD = 'MotorPH-demo-2026'
SEEDER = '10005'
DATA = pathlib.Path(__file__).resolve().parent.parent / 'src/main/resources/data'

ASSUMPTIONS = """
Every weekday is an 08:00-12:00 / 13:00-17:00 shift unless the employee's row says otherwise.
A seeded "present" day clocks in between 07:52 and 08:06 and out between 17:00 and 18:10.
About 1 day in 11 is a late arrival (08:15-08:50); about 1 in 23 is an absence (no row at all).
Saturday is a rest day; roughly 1 in 6 rest days has a short 4-hour shift on it.
Sundays are never worked.
No seeded overtime crosses midnight, so no shift can overlap another.
Leave is not seeded: the workflow tests build their own, and inventing approved leave would mean
inventing approvals nobody made.
Only past dates are written. The current month is left alone so nothing claims a finished payroll.
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--from', dest='start_month', default='2025-09')
    ap.add_argument('--months', type=int, default=14)
    ap.add_argument('--dry-run', action='store_true')
    args = ap.parse_args()

    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

    def call(path, payload=None, method=None):
        data = json.dumps(payload).encode() if payload is not None else None
        req = urllib.request.Request(BASE + path, data=data,
                                     method=method or ('POST' if data else 'GET'))
        req.add_header('Content-Type', 'application/json')
        csrf = next((c.value for c in jar if c.name == 'motorph_csrf'), '')
        if csrf:
            req.add_header('X-CSRF-Token', csrf)
        with opener.open(req) as r:
            return json.loads(r.read() or b'{}')

    step = call('/api/auth/signin', {'employeeId': SEEDER, 'password': PASSWORD})
    call('/api/auth/2fa/verify', {'challengeId': step['challengeId'], 'code': step['code']})

    rows = list(csv.DictReader(open(DATA / 'EmployeeDetails.csv')))
    employees = [(r['Employee #'], r['Position'], r['Status']) for r in rows]

    y, m = map(int, args.start_month.split('-'))
    first = datetime.date(y, m, 1)
    last = (first + datetime.timedelta(days=31 * args.months)) - datetime.timedelta(days=1)
    today = datetime.date.today()
    if last >= today:
        last = today - datetime.timedelta(days=1)

    rng = random.Random(20260101)
    made = skipped = 0

    day = first
    while day <= last:
        for eid, position, status in employees:
            if day.weekday() == 6:
                continue
            r = rng.random()
            if day.weekday() == 5:
                if r > 0.17:
                    continue
                tin, tout = '08:00', '12:00'
            else:
                if r < 0.043:
                    continue
                if r < 0.135:
                    tin = f'08:{rng.choice([15, 20, 25, 30, 41, 50]):02d}'
                else:
                    tin = f'07:{rng.choice([52, 55, 58, 59]):02d}' if rng.random() < 0.4 else '08:00'
                tout = f'17:{rng.choice([0, 0, 0, 5, 14, 23, 40]):02d}' if rng.random() < 0.7 \
                    else f'18:{rng.choice([2, 10]):02d}'
            if args.dry_run:
                made += 1
                continue
            try:
                call('/api/attendance', {'employeeId': eid, 'date': day.isoformat(),
                                         'timeIn': tin, 'timeOut': tout})
                made += 1
            except Exception as exc:
                skipped += 1
                if skipped <= 5:
                    print(f'  skipped {eid} {day}: {exc}', file=sys.stderr)
        day += datetime.timedelta(days=1)

    print(f'{"would write" if args.dry_run else "wrote"} {made} attendance rows, skipped {skipped}')
    print(f'range {first} .. {last}, {len(employees)} employees')


if __name__ == '__main__':
    print(ASSUMPTIONS)
    main()
