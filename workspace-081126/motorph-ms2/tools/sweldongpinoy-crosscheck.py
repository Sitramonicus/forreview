"""Differential test: our payroll engine vs the live sweldongpinoy.com endpoint.

Not a build step. Requires network access and a running API (MOTORPH_TEST_MODE=true).
The site has an undocumented internal endpoint (POST form -> JSON); see
docs/sweldongpinoy-crosscheck.md for what it validates and what it does not.
"""
import json, csv, urllib.request, urllib.parse, http.cookiejar, time
URL="https://www.sweldongpinoy.com/"; API="http://127.0.0.1:8183/api/payroll/preview"  # set to your running API
jar=http.cookiejar.CookieJar()
op=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
op.addheaders=[("User-Agent","Mozilla/5.0"),("Origin","https://www.sweldongpinoy.com"),("Referer",URL)]
op.open(URL,timeout=30)

def money(s): return float(str(s).replace(",","").replace("₱","").strip() or 0)

def site(basic, taxable, nontaxable):
    p={"employment_type":"Private Employee","pay_period":"Monthly","payroll_period_daily_days":"20",
       "basic_pay":f"{basic:,.2f}","taxable_allowance":f"{taxable:,.2f}",
       "nontaxable_allowance":f"{nontaxable:,.2f}","night_differential_hours":"",
       "night_differential_rate":"","overtime_hours":"","overtime_rate":"","work_schedule":"5"}
    r=op.open(urllib.request.Request(URL,data=urllib.parse.urlencode(p).encode(),
        headers={"Content-Type":"application/x-www-form-urlencoded"}),timeout=30)
    d=json.loads(r.read().decode())['result']
    return dict(sss=money(d['sss_gsis_contribution']), ph=money(d['philhealth_contribution']),
                pi=money(d['pagibig_contribution']), tax=money(d['withholding_tax']),
                net=money(d['net_pay']), gross=money(d['gross_pay']))

def ours(basic, rice, phone, cloth):
    body={"employeeId":"X","payPeriod":"MONTHLY","periodStart":"2026-01-01","periodEnd":"2026-01-31",
          "monthlyBasic":f"{basic:.2f}","daysWorked":20,"riceSubsidy":f"{rice:.2f}",
          "phoneAllowance":f"{phone:.2f}","clothingAllowance":f"{cloth:.2f}",
          "taxBasis":"BASIC_ONLY","statutoryBasis":"FULL_MONTHLY_EACH_PERIOD"}
    req=urllib.request.Request(API,data=json.dumps(body).encode(),headers={"Content-Type":"application/json"})
    d=json.loads(urllib.request.urlopen(req,timeout=20).read())['payslip']
    return dict(sss=float(d['sssEmployee']), ph=float(d['philHealthEmployee']),
                pi=float(d['pagibigEmployee']), tax=float(d['withholdingTax']),
                net=float(d['takeHomePay']), gross=float(d['grossIncome']))

rows=list(csv.DictReader(open("/home/user/uploads/Copy of MotorPH_Employee Data - Employee Details.csv", encoding="utf-8-sig")))
print(f"{'#':>6} {'basic':>9} | {'SSS s/o':>15} {'PH s/o':>15} {'PI s/o':>13} {'tax s/o':>17} {'net s/o':>21} | match")
bad=[]
for r in rows:
    basic=money(r['Basic Salary']); rice=money(r['Rice Subsidy'])
    phone=money(r['Phone Allowance']); cloth=money(r['Clothing Allowance'])
    s=site(basic, 0.0, rice+phone+cloth)
    o=ours(basic, rice, phone, cloth)
    diffs={k: round(s[k]-o[k],2) for k in s if abs(s[k]-o[k])>0.005}
    tag="OK" if not diffs else "DIFF "+str(diffs)
    if diffs: bad.append((r['Employee #'],r['Last Name'],basic,diffs))
    print(f"{r['Employee #']:>6} {basic:>9,.0f} | {s['sss']:>7,.2f}/{o['sss']:>7,.2f} {s['ph']:>7,.2f}/{o['ph']:>7,.2f} "
          f"{s['pi']:>6,.2f}/{o['pi']:>6,.2f} {s['tax']:>7,.2f}/{o['tax']:>7,.2f} {s['net']:>9,.2f}/{o['net']:>9,.2f} | {tag}")
    time.sleep(0.1)
print(f"\n{len(rows)-len(bad)}/{len(rows)} employees: OUR ENGINE == SITE, exact")
for b in bad: print("  DIFF:", b)
