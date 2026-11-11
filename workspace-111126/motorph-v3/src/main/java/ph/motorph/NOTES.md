
lint pass -- 2026-11-08
added eslint + checkstyle + pmd + spotbugs + lighthouse, all through tools/lint.sh. not in test.sh on
purpose, lighthouse needs a server and a download.
had to strip most of the stock checkstyle rules. 1277 errors and ~1200 of them were "group your
declarations" and "put a brace on its own line", which is literally the opposite of what we were told
to do. kept the ones that catch bugs.
what it actually found: 18 unused imports, 24 toLowerCase() calls with no Locale (that one matters --
turkish locale turns I into a dotless i and breaks role matching), an abs(hashCode) that stays negative,
5 dead methods, 2 unguarded nulls, and a signin splash div that nothing ever unhid.
also: the landing page had inline style="" that our own CSP was blocking, so the payslip bars drew at
zero width. moved to classes. contrast was 2.6:1 on three labels, now 100 on lighthouse.
