# sp_secpol

Passwords, sessions, and who may read what.

- `PasswordHasher` is Argon2id + per-account salt + server pepper. Pepper comes from
  `MOTORPH_PEPPER` and must be ≥32 chars. No pepper = the server refuses to start. That is on
  purpose; a pepper in the repo is not a pepper.
- `PasswordHasher` throws `IllegalStateException` for a short pepper, not `IllegalArgumentException`.
- `SessionCookie`: `motorph_session` is HttpOnly, `motorph_csrf` is readable (the page has to read
  it), both `SameSite=Strict`, `Max-Age=28800`.
- Idle 15 min, absolute 8 h, checked lazily on each request. No timer thread.
- Bumping `lastActive` counts as activity — if you test the idle boundary, make a fresh untouched
  token, or the act of asking resets the clock and the test lies to you.
- `AccessPolicy.READ` is the matrix. `web/b_basic/Roles.js` mirrors the *company-wide* part of it so
  the page stops asking for things the server will refuse. If you change one, change both, or the
  console fills with red 403s that look like bugs.
- TEAM scope is recursive for reads, direct reports only for writes.

## two-factor, back in

- `TwoFactor.java` — challenge store. `AuthService.signIn()` hands back a challenge id in the `token`
  field, never a session. `verifyCode()` is the only place `sessions.issue()` runs now.
- 3 wrong codes kills the challenge. Codes are single use (deleted on success) and expiry-checked on
  read. 16-byte SecureRandom challenge ids, 6-digit SecureRandom codes.
- Constructor takes a `java.time.Clock` so the tests can advance it. First cut hardcoded the wall clock
  and the expiry test failed for the wrong reason — pass the same clock the rest of the stack uses.
- `AppServer` had a leftover `private final TwoFactor twoFactor = new TwoFactor(clockService::now)`
  field from the removed version. `Clock` isn't a functional interface, so it broke the build after
  the class landed. Deleted along with the four dead `X-Demo-Session` call sites.
- The code is returned in the signin JSON and drawn on the screen because there is no SMS gateway.
  That is the one deliberate hole; an SMS or TOTP sender drops in at `TwoFactor.start()`.

## member notation, first files

`PasswordPolicy` and `Account` are on the `pw_` / `acc_` prefixes now. The three public constants on
`PasswordPolicy` were renamed (`pw_sInitial`, `pw_iMinLen`, `pw_sPepperVar`) so that gave 22 call sites
in 3 files to update. Nothing else about the policy changed.

`Account` is top-level and stays top-level — don't nest it in `CredentialStore` again, that was a build
error once already. Its fields are package-private-mutated by `CredentialStore` (`replaceHash`,
`recordFailure`, `recordSuccess`, `locked`), so a rename here is never a one-file edit.

The convention doc is `docs/a_doc/member-notation.md`, prefix table included. Read it before touching
`AccessPolicy` — that one is deliberately not converted.

government details was unreachable -- 2026-11-08
two guards stacked wrong in handleGovernment:
1. every per-employee route demanded the id start with DEMO- , then looked the id up on the real roster.
   nothing satisfies both, so /api/government/10001 was a dead 404. took the DEMO- gate out, the roster
   lookup on the next line is already the authority.
2. requireGovernmentRead sat in front of the whole handler, so ADMIN -- the only role allowed to approve --
   got 403 before it reached the approve route. now the approve route checks requireGovernmentAdmin and
   everything else checks requireGovernmentRead.
checked by hand: payroll files a change on 10001, admin approves, numbers move, all of it survives a restart.
