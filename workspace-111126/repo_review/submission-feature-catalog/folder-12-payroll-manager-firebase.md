# Folder 12 — `manuog1994/payroll-manager` (Firebase payroll-document portal)

**Upstream:** [manuog1994/payroll-manager](https://github.com/manuog1994/payroll-manager)  
**Snapshot:** `main`, `d3f2b180390262794e301ef5be3dfd8ecf548a82`; GitHub metadata checked 2026-11-04 (4 stars, 1 fork).  
**Local source:** `repo_review/full_repo/testingester-main/12/-/payroll-manager/`  
**Evidence boundary:** static review of the Vue app, router, Vuex, Firebase calls and README. The README calls this a payroll manager, but the code is principally an authenticated, per-user repository for uploading and managing payroll **PDFs**. It does not calculate payroll amounts.

## Major-feature map

| Major | Feature | Minor features and sub-functions |
|---|---|---|
| 1 | Application boot and authentication state | Start Vue/Vuex/router; subscribe to Firebase Auth state; show login/register while signed out; render routed app when signed in. |
| 2 | Account registration and login | Email/password registration; confirm password; Yup validation; Firebase email/password sign-up; email/password login; login/register form toggle; loading and validation feedback. |
| 3 | Payroll document library | Query current user's Firestore collection; sort by date descending; display month/year and download URL; upload PDF to Firebase Storage; store file metadata in Firestore; delete metadata row and reload. |
| 4 | Account profile and credentials | Change display name; change email after reauthentication; change password after reauthentication; sign out after password update; account page navigation. |
| 5 | Shared layout/navigation and sign-out | Payroll and account routes; menu links; Firebase sign-out; reactive identity state. |

## Feature pseudocode


## High-level pseudocode — each major feature

### Major 1 — App boot and auth-state gate

```text
PROCEDURE MAJOR_1_APP_BOOT_AND_AUTH_STATE_GATE
    initialize Firebase Auth observer
    store current user state
    show authentication view or authenticated router view
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_1.1  // Initialize app state
        CALL MINOR_1.2  // Select signed-in/signed-out screen
END PROCEDURE
```

### Major 2 — Registration and login

```text
PROCEDURE MAJOR_2_REGISTRATION_AND_LOGIN
    validate sign-in or registration form
    call Firebase Auth operation
    react to auth-state change and update visible app
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_2.1  // Toggle sign-in/register form
        CALL MINOR_2.2  // Register with email/password
        CALL MINOR_2.3  // Login with email/password
END PROCEDURE
```

### Major 3 — Payroll PDFs

```text
PROCEDURE MAJOR_3_PAYROLL_PDFS
    query signed-in user UID collection
    upload selected PDF and record metadata
    list/download/delete that user’s payroll-document entries
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_3.1  // Read current user's payroll metadata
        CALL MINOR_3.2  // Validate upload selection
        CALL MINOR_3.3  // Upload and register a PDF
        CALL MINOR_3.4  // Download/view payroll PDF
        CALL MINOR_3.5  // Delete payroll list entry
END PROCEDURE
```

### Major 4 — Account settings

```text
PROCEDURE MAJOR_4_ACCOUNT_SETTINGS
    render account settings
    validate profile or credential change
    reauthenticate where required
    update Firebase profile and report resulting session state
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_4.1  // Change display name
        CALL MINOR_4.2  // Change email
        CALL MINOR_4.3  // Change password
END PROCEDURE
```

### Major 5 — Navigation and sign-out

```text
PROCEDURE MAJOR_5_NAVIGATION_AND_SIGN_OUT
    route between payroll and account views
    render shared menu
    sign out and return to authentication gate when requested
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_5.1  // Navigate payroll/account pages
        CALL MINOR_5.2  // Sign out
END PROCEDURE
```

### Major 1 — App boot and auth-state gate

**Minor 1.1 — Initialize app state** — sub-functions: mount Vue app, attach store/router, subscribe to Firebase Auth state, commit user identity.

```text
START Vue application with Vuex and router
ON mounted:
    subscribe to Firebase onAuthStateChanged
    WHEN auth state changes:
        commit user (or null) to Vuex state
```

**Minor 1.2 — Select signed-in/signed-out screen** — sub-functions: undefined means auth is still unresolved; null means signed out; user object means show routed pages.

```text
IF user is undefined
    render neither protected app nor signed-out form until auth resolves
ELSE IF user is null
    render authentication view
ELSE
    render current router view
```

### Major 2 — Registration and login

**Minor 2.1 — Toggle sign-in/register form** — sub-functions: switch visible form while preserving one auth page.

```text
ON switch-form action
    showLogin ← NOT showLogin
    render corresponding form
```

**Minor 2.2 — Register with email/password** — sub-functions: collect email/password/repeated password; validate email and required fields; check confirmation equality; create Firebase Auth user; expose validation errors and loading state.

```text
ON register submit
    set loading; clear errors
    validate email, password, repeated password, and equality
    IF invalid THEN map field errors; stop
    ELSE call Firebase createUserWithEmailAndPassword(email,password)
         authentication state listener updates Vuex user
    log Firebase error as implemented
    clear loading
```

**Minor 2.3 — Login with email/password** — sub-functions: collect credentials; validate required email/password; Firebase sign-in; listener updates authenticated user; show field errors and loading state.

```text
ON login submit
    set loading; clear errors
    validate email format and password presence
    IF invalid THEN show field errors
    ELSE call Firebase signInWithEmailAndPassword
         auth listener updates Vuex state on success
    log sign-in exception as implemented
    clear loading
```

### Major 3 — Payroll PDFs

**Minor 3.1 — Read current user's payroll metadata** — sub-functions: query collection named by current Firebase UID; order by date descending; attach Firestore document ID; populate list.

```text
uid ← Firebase current user UID
documents ← Firestore collection(uid).orderBy(date descending)
FOR each document
    copy fields and add document ID
set payroll list to result
```

**Minor 3.2 — Validate upload selection** — sub-functions: choose local file; accept only browser MIME `application/pdf`; capture payroll date; toggle form; show missing/invalid-file messages.

```text
ON file selected
    IF file MIME is application/pdf THEN retain file; clear error
    ELSE clear/deny file; show invalid-file message
ON date changed: retain selected date
ON form toggle: switch open/closed state
```

**Minor 3.3 — Upload and register a PDF** — sub-functions: require file and date; create UUID file name; upload under `<uid>/<uuid>.pdf`; retrieve download URL; create Firestore metadata under UID with URL/date/dateString; refresh list.

```text
IF file missing OR date missing THEN show required-fields error; stop
set loading
uid ← current user's UID
key ← random UUID + '.pdf'
upload file to Firebase Storage path uid/key
url ← get download URL
add Firestore document to collection(uid): {url, selected date, date string}
reload payroll metadata
clear selected inputs/form and loading state
```

**Minor 3.4 — Download/view payroll PDF** — sub-functions: open stored URL in a new tab using list link.

```text
ON Download click
    open payroll.payrollUrl in a new browser tab
```

**Minor 3.5 — Delete payroll list entry** — sub-functions: delete Firestore document by ID from current UID collection; reload list.

```text
uid ← current user's UID
delete Firestore collection(uid).document(payrollId)
reload payroll list
```

### Major 4 — Account settings

**Minor 4.1 — Change display name** — sub-functions: validate new name; update Firebase Auth profile displayName; expose validation/loading feedback.

```text
validate new name
IF valid THEN auth.currentUser.updateProfile(displayName=newName)
show success/error feedback and clear loading
```

**Minor 4.2 — Change email** — sub-functions: collect current password and new email; reauthenticate current email/password credential; update Firebase Auth email; report errors.

```text
validate required fields and email format
reauthenticate current user with current password
IF reauthentication succeeds THEN update current user's email
ELSE show auth error
```

**Minor 4.3 — Change password** — sub-functions: collect current/new/repeated password; require equality; reauthenticate; update password; sign out after update; show provider errors.

```text
validate all password fields and new-password confirmation
reauthenticate current user with current password
IF successful THEN update password; sign out user
ELSE show error
```

### Major 5 — Navigation and sign-out

**Minor 5.1 — Navigate payroll/account pages** — sub-functions: route `/` to payroll list and `/account` to settings; shared layout displays route menu.

```text
ON navigation selection
    router navigates to configured route
render route inside shared layout
```

**Minor 5.2 — Sign out** — sub-functions: call Firebase signOut; auth listener changes Vuex state to signed out; application returns to auth screen.

```text
call Firebase signOut
on auth-state change set Vuex user to null
render authentication view
```

## Source caveats / confidence

- There is no employee directory, wage calculation, attendance, tax/contribution calculation, payroll-period processing, approval, payslip generation, or salary payment engine in the inspected app. Its functional unit is a PDF uploaded by an authenticated user.
- Firestore collection name is the UID, and Storage paths are UID-prefixed. This indicates user partitioning in the client path; the downloaded source does not include Firebase Rules, so server-side isolation/access is not verified. The delete handler removes the Firestore metadata document only; it does not delete the corresponding Storage PDF, so orphaned files can remain.
- Upload accepts a browser-reported MIME type of PDF but the static review found no server-side content inspection/size policy. Errors in several Firebase operations are logged rather than consistently surfaced in the UI. Upload handles failure in a broad catch but then clears the form.
- Password changes sign out the user after update. Email change explicitly reauthenticates; do not infer equivalent recent-login protections for every operation.
- The repository's Firebase project configuration is public client configuration, not a service-account credential. No production Firebase rules, access rights, or live data were tested.
- No dependency install, test, or build was run. README notes Vue CLI `yarn install`, `yarn serve`, and `yarn build`.
