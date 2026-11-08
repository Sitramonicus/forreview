# Setting up MotorPH on your own computer

A quick start for setting up the project from scratch, including your own Firebase project. You can stop after part 1 if you only need the local demo.

## 1. Run the local demo (no Firebase)

**Install first:**
- **JDK 11 or newer**: `java -version` and `javac -version` must both work in a terminal.
- **Node.js 20.19 or newer** (npm comes with it).
- **Windows only:** **Git for Windows**, so you can run the `.sh` scripts from **Git Bash**.

**Then, in the `motorph-ms2` folder:**

```sh
npm install
./run.sh
```

Open <http://localhost:8080>, then pick a demo account, for example Lia (System Admin) or Jae (Employee).

- **Password:** every demo account starts with the shared demo password `MotorPH-demo-2026`. Change it after signing in (Overview → My account). The System Admin can reset it back from System config → Security settings.
- **2FA code:** next, type the 6-digit code. The demo shows it on screen, because there is no phone.
- Each account opens the workspace for its role.
- **Backups** are kept in `data/backups` and old audit entries in `data/archive`. If `data/local-demo.json` is ever damaged, the server starts from the newest backup.

- Local demo data is saved in `data/local-demo.json`. Signed in as Lia, use **System config → Reset demo data** to start over.
- If you have a `.env.local` file with a real Firebase key, `./run.sh` starts in Firebase mode instead. To get back to the local demo, rename or delete that file.
- **Hot reload while coding:** run `./run-api.sh` in one terminal and `npm run dev` in another, then open <http://localhost:5173>.

**Checks:**
- `./test.sh` runs the Java checks.
- `npm run test:web` runs the browser logic tests.
- `npm run test:e2e` runs the full browser test. Before the first run, do `npx playwright-core install chromium` once.

## 2. Create your own Firebase project

The free **Spark** plan is enough. If Firebase asks for billing, stop: this project doesn't need it.

1. **Create the project.** Go to <https://console.firebase.google.com> → **Add project**. Pick any name. Google Analytics can be turned **off**.
2. **Add a web app.** On the project overview, click the **`</>` (Web)** icon. Give it a nickname, and leave Firebase Hosting unticked. Firebase then shows a `firebaseConfig` block: keep that page open for step 6.
3. **Turn on sign-in.** Go to **Build → Authentication → Get started → Sign-in method**, and enable **Email/Password**. Leave the passwordless "Email link" option off.
4. **Create the database.** Go to **Build → Firestore Database → Create database**.
   - Choose **production mode**, not test mode.
   - Pick a location close to you, such as `asia-southeast1` (Singapore). You can't change the location later.
5. **Publish the rules.** Go to **Firestore Database → Rules**, delete what's there, and paste in the whole of `docs/firestore.rules`. Then click **Publish**.
   - These rules let each signed-in user read and write only their own sandbox, under `users/{their uid}/…`.
   - Never switch to rules that say `allow read, write: if true`.
6. **Connect the app.** In `motorph-ms2`, copy `.env.example` to `.env.local`. Then fill in the six values from the `firebaseConfig` block in step 2:

   ```text
   VITE_FIREBASE_API_KEY=<apiKey>
   VITE_FIREBASE_AUTH_DOMAIN=<authDomain>
   VITE_FIREBASE_PROJECT_ID=<projectId>
   VITE_FIREBASE_STORAGE_BUCKET=<storageBucket>
   VITE_FIREBASE_MESSAGING_SENDER_ID=<messagingSenderId>
   VITE_FIREBASE_APP_ID=<appId>
   ```

   - The web API key is a public identifier, not a password: Firebase protects your data with the sign-in and the rules.
   - Still, never put a service-account key, a password or any real employee data in this project.
7. **Run it:** `./run.sh`, then open <http://localhost:8080>.
   - `localhost` is allowed by Firebase by default.
   - If you open the app from another address and see `auth/unauthorized-domain`, add that host under **Authentication → Settings → Authorized domains**.

## 3. First sign-in (Firebase mode)

1. Enter an email and a password, then click **Create dev account**. Use an email you own, so password resets reach you.
2. Your sandbox starts empty. Click **Add synthetic demo people** to add the fake employees and attendance, or click **Open the empty sandbox** to look around without adding anything.
3. Choose who to act as. Each person opens their role's workspace, and the server checks every permission. Use **Switch account** to pick someone else, and **Sign out** when you're done.

**What works in Firebase mode:**
- the directory, attendance (read only), leave requests, tickets, reports and the payroll estimate, all saved in your Firestore sandbox

**What only works in the local demo** (it would need more Firestore rules):
- approvals and overtime
- schedules and timesheet corrections
- HR tools and government details
- payroll processing, saved payslips and corrections
- clock in/out and System config

**Live sign-in test (optional):** `npm run test:firebase-auth` creates a throwaway account in your Firebase project, signs in and out, then deletes the account.

## If something goes wrong

| Message | Fix |
|---|---|
| `auth/operation-not-allowed` | Email/Password isn't enabled (step 3). |
| `auth/invalid-api-key` | Check the `VITE_FIREBASE_API_KEY` value in `.env.local`, then run `./run.sh` again. |
| `auth/unauthorized-domain` | Add the host you're using under Authorized domains (step 7). |
| `403` / permission denied after sign-in | The rules weren't published, or not all of them were pasted (step 5). |
| Still shows the local demo chooser | `.env.local` is missing or still has the placeholder `replace-…` values (step 6). |
| `javac: command not found` | Install a JDK (not just a JRE) and reopen the terminal. |
