# Firestore setup

## Short version

1. Firebase console > Project settings > Service accounts > Generate new private key. Keep the JSON outside the project folder.
2. Copy `.env.example` to `.env` and fill in `GOOGLE_APPLICATION_CREDENTIALS`, `FIREBASE_PROJECT_ID` and `MOTORPH_PEPPER`.
3. `npm install`, then `sh run.sh`. The banner should say `State store: Firestore`.

`run.sh` loads `.env` on its own. The server renews its Google access by itself, so there is no hourly token to paste. With no `.env` the app keeps its data in `data/local-demo.json`.

The rest of this page is the long version (gcloud sign-in, emulator, limits).

Employees sign in with an employee number and password. They do **not** need a Google account or a Google token. The Java server uses its own Google identity to access Firestore.

## Local development

1. Install the Google Cloud CLI. Sign in **once** on the computer that runs the server:

   ```sh
   gcloud auth application-default login
   ```

2. Give that identity the necessary Firestore access. Set the project and a stable secret pepper, then start the app:

   ```sh
   export FIREBASE_PROJECT_ID=motorph-jd
   export MOTORPH_PEPPER='<your own secret, at least 32 characters>'
   unset MOTORPH_FIRESTORE_TOKEN
   npm install
   sh run.sh
   ```

   Keep the same pepper across restarts: changing it makes existing password hashes unverifiable. Store it in a protected secret store or environment, not in source control. The server refuses to start with Firestore if this secret is missing or short.

The Google authentication library finds Application Default Credentials (ADC) and refreshes the server's short-lived access token as needed. You normally do not need to issue or paste tokens manually. A local user login may occasionally need to be renewed by its owner. `MOTORPH_FIRESTORE_TOKEN` is an optional fixed-token override for diagnostics; it takes precedence over ADC and **does not refresh**, so leave it unset for normal operation.

The startup banner says `State store: Firestore (motorphState collection)`. If Firestore or credentials cannot be loaded, the server fails to start instead of silently switching to a local file or replacing the database with seed data.

After startup, sign in as System Admin, open **System configuration → Database**, confirm the backend is `firestore`, then run **Verify database read / write / read**. A passing result confirms the loaded state matches Firestore, an unchanged snapshot committed through the Firestore state store, and read-back matched. It does not certify project selection, data correctness, backups, IAM scope, multi-instance behavior or production availability. Details and failure handling: [Storage verification](a_doc/storage-verification.md).

## Deployed server

On Google Cloud, attach a dedicated service account to the compute resource and grant it only the Firestore permissions required. ADC obtains and refreshes the server's credentials through the environment. Do not use a developer's personal login or a downloaded service-account key for a deployed service. For other hosting environments, configure workload identity federation where available. The exact deployment steps depend on the host and IAM administrator.

## Persistence and limits

- Firestore is the shared source of state. Employees' views load from the server's in-memory copy; changes from a different running server are **not** automatically pulled into that copy.
- A write response is held until a Firestore atomic commit succeeds. Modified state chunks, index, metadata and any chunk deletions are committed together. An update-time precondition rejects a stale writer instead of silently overwriting another server's changes. Unchanged chunks are not rewritten.
- If the save fails or times out, the API returns 503, rolls back its in-memory state where possible, and refuses further writes until the operator investigates and restarts. A timeout can be ambiguous: the database may have accepted the commit even though the response was lost. Inspect the database before retrying.
- Run **one application instance** for now. The stale-writer guard prevents silent overwrites but does not synchronise the in-memory copies or provide multi-instance sessions.
- The entire state is still held in memory and saved as chunked documents, not individual employee/payroll records. One atomic request is limited to 10 MiB; this implementation rejects payloads above 9.5 MB. It is not a scalable, row-oriented database design.
- Firestore mode ignores `MOTORPH_DEFER_SAVES`; deferred writes are not used for Firestore. Local-file mode can opt in, but that means success can precede persistence.
- A manual admin backup is stored in `motorphBackups` and `motorphBackupChunks`; audit archives go to `motorphArchives`. Protect all of these collections as sensitive data. Set up and verify an independent cloud backup/export and a retention policy as well; the application backup is not a substitute for disaster recovery.

## First use and existing state

If the Firestore state is truly empty, the server seeds it from `MOTORPH_SEED_FILE` (default `data/local-demo.json`). An existing state is loaded; it is not overwritten on startup. The saved sample attendance is generated test data, not actual payroll history. Back it up and validate it before any migration or use.

The prior live preview was forcibly stopped while using the old non-atomic writer. Its current remote state has not been rechecked. **Before writing to that particular database**, inspect/export its existing data and verify the snapshot. Do not upload a replacement on top of it blindly.

## Inspection and copying

The following command reads counts without changing either side (on Windows, use semicolons instead of colons in the classpath):

```sh
sh tools/build.sh
java -cp 'target/classes:lib/gson-2.13.2.jar:lib/bcprov-jdk18on-1.78.1.jar:lib/google/*' tools/FirestoreSync.java diff
```

`FirestoreSync.java upload UPLOAD` and `download REPLACE` overwrite their destination; the second argument is an explicit confirmation. Stop all running app instances, make and verify a backup first, then run the appropriate command only if replacement is intended. The in-app whole-dataset copy route is blocked while the server is using Firestore.

## Emulator

Set `FIRESTORE_EMULATOR_HOST=127.0.0.1:8099` and `FIREBASE_PROJECT_ID=demo-motorph`. The app uses the local emulator without Google credentials. Never point a production service at an emulator, and never open its unauthenticated development rules to the internet.

## Permissions

Server-side Google credentials use IAM; an authenticated server principal needs access to the project/database. Browser Firebase security rules are not a substitute for protecting server credentials or implementing application-level authorisation. Do not make these collections public. Employees never receive the server's Google credentials.
