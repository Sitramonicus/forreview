# Setting up MotorPH on your own computer

Part 1 gets you running in a minute with no cloud account. Part 2 is optional: it keeps the same
dataset in Firestore instead of a local file, so you can inspect it in the Firebase console.

## 1. Run it (no Firebase needed)

**Install first:**
- **JDK 11 or newer**: `java -version` and `javac -version` must both work in a terminal.
- **Node.js 20.19 or newer** (npm comes with it).
- **Windows only:** **Git for Windows**, so you can run the `.sh` scripts from **Git Bash**.

**Then, in the `motorph-ms2` folder:**

```sh
npm install
./run.sh
```

Open <http://localhost:8080>. Sign in with an employee number, not an email:

| Employee no. | Who | Password |
|---|---|---|
| `10005` | System Administrator | `MotorPH-demo-2026` |
| `10006` | HR Staff | `MotorPH-demo-2026` |
| `10011` | Payroll Staff | `MotorPH-demo-2026` |
| `10015` | Supervisor | `MotorPH-demo-2026` |
| `10018` | Employee | `MotorPH-demo-2026` |

- **2FA:** after the password, type the six-digit code. The demo prints it under the input,
  because there is no SMS gateway wired up. It is not secure 2FA. It expires in five minutes and
  dies after three wrong tries.
- **Password:** all accounts start with the same configured initial password. You can change it any time from **My account**; first sign-in goes straight to the workspace after the code step. The System Admin can restore the shared initial password and manage account access from **System config → Account access & passwords**.
- **Where the data lives:** `data/local-demo.json` — 34 employees and 7,421 attendance rows.
  Backups go to `data/backups`, old audit entries to `data/archive`. If the file is ever
  damaged the server starts from the newest backup.
- **Start over:** signed in as `10005`, use **System config → Reset demo data**.
- **Hot reload while coding:** `./run-api.sh` in one terminal, `npm run dev` in another, then
  open <http://localhost:5173>.

**Checks:**
```sh
./test.sh          # Java suites, web unit tests and browser e2e
npm run test:web   # just the browser logic tests
npm run test:e2e   # just the browser test; needs `npx playwright-core install chromium` once
```
`./test.sh` skips the browser suite cleanly if Chromium isn't installed, or if you set
`MOTORPH_SKIP_E2E=1`.

## 2. Use Firestore

The server uses refreshable Google Application Default Credentials, not a separate token per employee.
Follow [the Firestore setup guide](docs/firebase-setup-guide.md) for IAM, deployment, backups and
limits. In brief, run `gcloud auth application-default login` on a development server, set
`FIREBASE_PROJECT_ID` and a stable secret `MOTORPH_PEPPER` of at least 32 characters, then run
`./run.sh`. For production hosting use an attached service account or workload identity; do not
copy credentials into this repository. Leave `MOTORPH_FIRESTORE_TOKEN` unset so ADC can refresh.

Firestore writes are acknowledged after the atomic commit. The current snapshot design requires a
single app instance, has request-size limits, and has not been validated for production. The former
live preview was forcibly interrupted; export and inspect its remote data before writing to it.

## If something goes wrong

- **Sign-in or password:** employee numbers are five digits. An administrator can reset another
  account's password; all of that account's sessions are revoked.
- **Firestore authentication or permission errors:** verify ADC and IAM for the server's identity.
  The server fails startup rather than silently falling back when Firestore is configured.
- **Save failure:** writes stop until restart. A timeout can be ambiguous, so inspect the database
  and backup before retrying. See the Firestore guide.
- **Java or browser tests:** install a JDK (not just a JRE); browser tests also need Chromium.
