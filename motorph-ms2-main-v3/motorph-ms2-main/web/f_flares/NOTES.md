# web/f_flares

Everything with numbers or motion in it: payroll views, calendars, workflow widgets.

- `PayrollPreview2026.js` is the contribution/tax calculator, checked against sweldongpinoy.com to
  the centavo on 25 cases. If you change a rate here, re-run `StatutoryCalculator2026Test`.
- `PayrollCutoff.js` drives the cutoff panel. It needs attendance in the store or it will say
  "No paid attendance in this cutoff" — which is correct, not a bug.
- Export helpers build CSV client-side. Filenames don't carry "demo" any more.
