# One account, five roles — and the security hole that was hiding behind the question

You said you can't have five Firebase accounts just to test five roles. That objection was right, and
it uncovered something worse than an inconvenience.

---

## 1. The bug the question exposed

The server was deciding identity like this:

```java
boolean authenticated = firebaseProjectId.isEmpty()
    || exchange.getRequestHeaders().getFirst("Authorization") != null;
String employeeId = exchange.getRequestHeaders().getFirst("X-Firebase-EID");
```

Read the first line carefully. **Any** non-empty `Authorization` header counted as "signed in". The
token was never verified — not its signature, not its expiry, not which project minted it. The second
line meant the employee number — and therefore the role — came from a header the caller controlled.

So the honest description of what shipped yesterday is: *anyone who could send two HTTP headers could
be the System Admin.* Every claim I made about "the browser cannot promote itself" was true of the
browser and false of `curl`.

That is why one account per role felt necessary: with no real identity layer, an account was only a
label, and the only way to make the labels distinct was to make more of them.

## 2. What identity is now

Three things, in this order:

| Step | Where it comes from | What the client can do about it |
|---|---|---|
| **Account** | A Firebase ID token, verified against Google's `accounts:lookup` (signature, expiry, project) | Nothing — it either verifies or it doesn't |
| **Person** | The seat registry: which employee numbers this account is linked to | Choose *among the links it actually has*; anything else is `403` |
| **Seat** | `RoleAssignment`: which roles that person holds | Choose *among the seats it actually holds*; anything else is `403` |

`FirebaseTokenVerifier` does the verification and caches the answer for two minutes. `SeatRegistry`
holds the account→person links. `RoleAssignment` holds person→seats.

Two states the old code got wrong, both now explicit:

- **Unlinked account** (`APPLICANT`) — signed in, but HR has not given it an employee number. It can
  read nothing and ask for access. It used to be treated as Employee, which handed payroll data to
  anyone who could register an email.
- **A UID header that disagrees with the token** — refused with `403`, because in Firebase mode that
  header decides whose Firestore documents are touched.

### Startup now fails without a verifier

```
FIREBASE_API_KEY is required. Without it the server cannot verify a Firebase ID token,
and an unverified token is the same as no authentication at all.
```

Set `FIREBASE_API_KEY` to the project's Web API key (the same value as `VITE_FIREBASE_API_KEY`; it is
not a secret, but it must be present or the process refuses to start).

## 3. Why five accounts were never needed

**An account is not a person, and a person is not a role.**

- One account can be linked to **several people** — a clerk covering two roles gets one login and
  switches person.
- One person can hold **several seats** — an HR Manager who supervises somebody acts as HR for the
  roster and as Supervisor for their team, without a second login.

MotorPH's own file has five such people. From the `Immediate Supervisor` column:

| Person | Seats |
|---|---|
| 10006 Andrea Mae Villanueva (HR Manager) | **HR** + **Supervisor** (report: 10007) |
| 10011 Anthony Salcedo (Payroll Manager) | **Payroll Staff** + **Supervisor** (3 reports) |
| 10005 Eduard Hernandez | System Admin |
| 10015 Fredrick Romualdez (Account Manager) | Supervisor (2 reports) |
| 10018 Allison San Jose | Employee only |

So role switching was already a real business need, not a testing convenience. Making each person
exactly one role was the modelling mistake.

## 4. Testing all five roles: one identity, five seats

There is now a single demonstration identity, **10999**, that holds all five seats at once. It is not a
MotorPH position — no real person is both Payroll Staff and their own supervisor's employee — so it is
attached to a synthetic number that does not appear in the employee file, and it is labelled as
synthetic everywhere it appears.

### In fixture mode (no Firebase project at all)

The preview runs in fixture mode. The top bar has two controls:

- **PERSON** — which seeded employee to act as.
- **ACTING AS** — which seat of that person. It only appears when the person holds more than one.

Pick **10999**, then switch the ACTING AS control through all five:

| Seat | Directory rows | What it can do |
|---|---|---|
| System Admin | 34 | everything, including the tax tables |
| HR | 34 | create/update employees, read the roster |
| Payroll Staff | 34 | payroll runs, salary history, payslips |
| Supervisor | 1 | themselves — 10999 is nobody's supervisor |
| Employee | 1 | themselves only |

Then pick a real person to see it behave as their position implies: **10006** offers HR and
Supervisor, and switching between them changes the directory from 34 rows to 2.

### In Firebase mode (your real project)

Sign in **once**, with any account. Then:

1. `GET /api/me` reports `seats` and `people` — everything that login may act as.
2. Send `X-MotorPH-Employee` and `X-MotorPH-Seat` to switch. The server validates both.
3. `POST /api/me/links` (or the seat registry's Firestore document) adds a person to an account; that
   is how one login is given a second person, and it is an HR action, not a self-service one.

So: **one Firebase account is enough.** The five roles are seats on it, not accounts.

## 5. What the demo seed is, and is not

`RoleAssignment.demoSeed()` maps the five roles onto real positions in the supplied file. It is a
**demonstration assignment, not MotorPH policy**, and `/api/me` returns
`roleSeedIsDemonstration: true` so nothing downstream mistakes it for one.

Real MotorPH would replace the seed with whatever HR decides, in the same structure.

## 6. Verified

| Suite | Result |
|---|---|
| `./test.sh` | **614 checks PASS** |
| `npm run test:web` | **62 / 62 PASS** |
| `npm run test:e2e` | **17 PASS** |

New coverage for this work:

- `IdentityAndSeatTest` (45 checks) — one identity walking all five seats; a seat you do not hold
  refused with `403`; a seat that does not exist refused rather than defaulted; an unverified token
  never accepted; a verified account unable to borrow another employee's identity.
- `AppServerIntegrationTest.firebaseModeVerifiesTheTokenAndRefusesAForgedIdentity` — the server asks
  the verifier (a fake Identity Toolkit records the tokens presented), a rejected token is `401`, a
  UID disagreeing with the token is `403`.
- The statutory ruleset is no longer readable without a session; an employee sees the rates in force
  but the withholding bracket tables are withheld and the response says so, rather than the client
  having to infer that an absent key means "not allowed".

## 7. Linking an account to a person — built

```
POST /api/me/links
{ "accountId": "<firebase uid>", "employeeNumber": "10016" }
→ 201 { "link": { "accountId": "...", "employeeNumber": "10016", "seats": "SUPERVISOR" } }
```

- **HR and System Admin only.** Payroll Staff and employees are refused with `403`.
- **Nobody may edit their own links.** An account that could grant itself a seat would make the seat
  model decorative; this is refused with the reason rather than silently ignored.
- **Calling it again with a second employee number is how one login covers two jobs.** The seat
  switcher then offers both people.
- The link inherits the seats that employee actually holds, taken from `RoleAssignment` — so it cannot
  be used to grant a role the person does not have.
- A non-existent employee is `404`; a malformed number is `400`.

## 8. Still open

- **The seat registry does not persist.** It is in process memory, so links are lost on restart. In
  Firebase mode it needs a Firestore collection (`seatLinks` keyed by uid), which is the next piece of
  work. Until then, a rebooted production server has no links and every account is an applicant —
  correct, but inconvenient.
- **The role seed is still a demonstration.** Replacing it is a policy decision, not a code one.
- `docs/firestore.rules` does not yet describe the `seatLinks` collection.
