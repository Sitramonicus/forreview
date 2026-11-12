# svc

`AppServer` — the whole HTTP layer in one file. Yes, one file, on purpose; it's the JDK's
built-in server and splitting it would mean passing twenty dependencies around.

- Every route goes through `context(...)`, which is where CSRF is enforced once so no endpoint
  can forget it. `/api/auth/signin` is the only exemption, and only because you have no cookie
  yet to protect.
- `actorFromCookie` resolves the session. No cookie, no actor.
- Writes run inside `synchronized (stateLock)` and save the data file after, so a crash can't
  leave half a request applied.
- Serving the UI: `/` is `/b_basic/Index.html`. Unknown path gets the 404 page **with a real 404
  status** — a pretty page returning 200 is a soft 404 and it's worse than nothing.

gov handler + DEMO- gate -- 2026-11-08
see sp_secpol/NOTES.md for the two bugs. the fix landed here in handleGovernment.
also: loading the state file now has a guard test -- o_oop/StateFileRoundTripTest. it reads the real
data/local-demo.json, deserialises it with the same gson setup, and fails if any attendance date or
employee name comes back null. that is the check the old suite was missing: everything else builds the
roster from the CSV and never touches the saved file.
