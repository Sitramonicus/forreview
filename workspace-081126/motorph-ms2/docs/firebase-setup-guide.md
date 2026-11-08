# Firebase connection checkpoint — MotorPH MS2

## Current project state

The Firebase project and web app already exist; do **not** create duplicates.

- Project ID: `motorph-jd`
- Web app nickname: `motorphdev`
- Auth domain: `motorph-jd.firebaseapp.com`
- Storage bucket: `motorph-jd.firebasestorage.app`
- Messaging sender ID: `63361272264`
- Web app ID: `1:63361272264:web:61ec17db13d69f4b0c98fd`
- Firebase Analytics is not used.
- Email/Password is enabled. The supplied Firebase Web API key is installed in the assistant-managed `.env.local` file. Live Identity Toolkit and browser sign-in were verified with disposable synthetic accounts, which were then deleted.
- The user published the updated `docs/firestore.rules`; the assistant made no Firebase Console changes. Live smoke tests with disposable accounts verified synthetic employee create/update, leave and ticket create/cancel persistence, and cross-user denial. Both test accounts and employee docs were deleted; one cancelled leave (`LV-3502C01F0E60`) and one cancelled ticket (`TK-F05DD2B0D6CC`) remain under their deleted test UIDs because workflow deletion is denied.
- The existing Firestore database is reachable; its exact location was not independently confirmed (the user recalled Taiwan or Singapore). Do not create a duplicate database.

## App integration

The prototype now has:

1. Browser email/password sign-in and dev-account creation using Firebase Authentication.
2. In-memory browser auth persistence; this prototype does not persist the Firebase session in local storage.
3. A Java REST adapter that forwards the signed-in ID token to Firestore. Firestore Security Rules remain the access gate; there is no Admin SDK or service-account credential.
4. A per-user development sandbox: `users/{uid}/demoEmployees`, `demoEmployeeSchedules`, `demoAttendance`, `demoLeaveRequests`, and `demoTickets`; the published workflow rules were live-smoke-tested for leave create/cancel.
5. An explicit **Add synthetic demo data** action that creates fictional employee, pre-existing fixture schedule links, and attendance records only. The Payroll workspace remains disabled behind a policy gate; it calculates no amounts.

## 1. Web API key — configured

The valid Web API key you supplied has been set in `.env.local` by the assistant, and `./run.sh` rebuilt the app in Firebase-authenticated mode. You do not need to edit this file. The Web API key is a public client identifier, not an Admin/service-account credential; never put a private key, ID token, password, or service-account JSON in the browser config.

If this key ever needs to be changed, share the Web API key shown for `motorphdev` in Firebase Console → Project settings → General → Your apps, and the assistant will update `.env.local` and rebuild. Firebase access is authorized by Auth and Security Rules, not by concealing this public key.

The other values should remain:

```text
VITE_FIREBASE_AUTH_DOMAIN=motorph-jd.firebaseapp.com
VITE_FIREBASE_PROJECT_ID=motorph-jd
VITE_FIREBASE_STORAGE_BUCKET=motorph-jd.firebasestorage.app
VITE_FIREBASE_MESSAGING_SENDER_ID=63361272264
VITE_FIREBASE_APP_ID=1:63361272264:web:61ec17db13d69f4b0c98fd
```

## 2. Confirm Email/Password sign-in

In Firebase Console → Authentication → Sign-in method, confirm **Email/Password** (the basic password provider) is enabled. The app includes a dev-account creation option and a password-reset email action; use only an account you control. If reset messages do not arrive, check the Firebase email action/template settings. If the provider is off, Firebase will return `auth/operation-not-allowed`.

Browser sign-in has been verified from `localhost`. The current Arena preview host is `8080-is3s38ic4lxn7e4e77g8b.e2b.app`, but Firebase authorization from that exact origin could not be tested from the sandbox because the Arena proxy requires a traffic-access token injected only in the user's preview session. If the preview reports `auth/unauthorized-domain`, add the exact host shown in the browser address bar under Authentication → Settings → Authorized domains (for this preview, `8080-is3s38ic4lxn7e4e77g8b.e2b.app`). Preview hosts may change between sessions. Do not enable unrelated providers just for this prototype.

## 3. Firestore development rules — published and API-smoke-tested

The project owner published `/home/user/motorph-ms2/docs/firestore.rules`; the assistant made no Firebase Console changes. Live API testing with disposable accounts verified synthetic employee create/update, leave and ticket create/cancel/read-back, and cross-user path denial (`403`). Both disposable Auth accounts and test employee documents were deleted. One cancelled leave (`LV-3502C01F0E60`) and one cancelled ticket (`TK-F05DD2B0D6CC`) remain under their deleted test UIDs because Firestore rules intentionally deny workflow deletion. Rules Emulator/Console Playground testing was not performed.

The rules are development-only. If `docs/firestore.rules` changes later, the project owner must review and publish the new full ruleset; the assistant does not make Firebase Console changes. The Java API fails closed when Security Rules deny a request.

Earlier live verification used disposable synthetic accounts through the app's Java API:

- Seeded 3 employees and 6 attendance documents; repeating the seed created 0 new documents.
- Employee list/search/detail and an inclusive one-day raw attendance query returned the expected synthetic data.
- Unauthenticated requests, UID spoof/cross-user access, payroll paths, and an extra salary field were denied. Invalid reversed date range returned `400`; an unknown employee returned `404`.
- The earlier employee/attendance test's temporary documents and Auth accounts were deleted; no credentials were retained. The two latest workflow smoke records are documented above.

The previously published rules permit authenticated users to access only their own `users/{uid}/demoEmployees` and `users/{uid}/demoAttendance` records. The updated candidate additionally permits owner-only workflow create/read/cancel in `demoLeaveRequests` and `demoTickets`, and recognizes server-issued five-digit employee numbers (`^[0-9]{5}$`, e.g. `10015` and new records from `10035`) so those records can be referenced by workflows. Workflow deletes and reviewer actions remain denied. The directory editor uses only the existing owner-scoped employee collection and its allowlisted fields (identity fields plus the four pay inputs payroll reads). Both rulesets are development-only, not a production employee/HR authorization policy. No collection needs to be created manually.

## 4. Run and verify

From the project folder:

```sh
npm install
./run.sh
```

Open `http://localhost:8080`. For hot reload, use two terminals: `./run-api.sh` and `npm run dev`, then open `http://localhost:5173`.

After Firebase config, Auth, and Rules are ready:

1. Sign in with the dev account, or create one.
2. Choose **Add synthetic demo data** in the empty directory.
3. Confirm the fictional employee and attendance records appear.
4. Try attendance filtering and employee search.
5. Confirm the Payroll workspace displays the configuration gate and no payroll estimate or calculator is available. The current `docs/firestore.rules` must be published before the demo seed can write schedule-mapping documents.
6. With the published rules, create synthetic leave/ticket requests only if you accept that cancellation does not delete them. Reload to verify persistence and confirm there are no reviewer/approval controls; test cross-user denial only with disposable accounts.

The backend forwards ID tokens to Firestore, where these requests are evaluated under Security Rules. No token is written to the repository. Do not add real employee/payroll data. The prototype server is not hardened for public deployment.

## Pause points

- If the console prompts to enable billing or upgrade, stop and report the prompt; do not add payment details.
- If the database location is unknown, inspect Firestore Database → database details/settings; `asia-east1` means Taiwan and `asia-southeast1` means Singapore. The location is durable and cannot be changed later. Do not create another database just to resolve the check. See [Firestore locations](https://firebase.google.com/docs/firestore/locations).
- If rules show open access (`if true`) or Test mode, do not seed data; restore deny-all or ask for review first.
- Never generate a service-account key for this design.
