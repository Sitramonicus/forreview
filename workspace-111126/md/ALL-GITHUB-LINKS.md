# Every GitHub link in this project, and what it was for

A complete inventory of the GitHub repositories referenced anywhere in the MotorPH work — the target repository, the
peer projects studied, the libraries the build depends on, the tools used to build it, and the design sources that
happen to be hosted on GitHub Pages.

Sections 1–4 are the ones that **shaped the product**. Section 5 is third-party dependencies and tooling, listed so
that nothing is lost if the dependency tree is ever reconstructed. Commit, issue and pull-request URLs are collapsed
back to the repository they belong to: they came from `node_modules` changelogs and are not project references.

---

## 1. The repositories this project lives in

| Link | What it is | Why it matters |
|---|---|---|
| https://github.com/Sitramonicus/forreview | The review repository for this course | Holds the base build and every snapshot of the workspace |
| https://github.com/Sitramonicus/forreview/tree/main/motorph-ms2-main/motorph-ms2-main | **Claude's build** — the base being rebuilt | The starting point: this is the code this project is modifying |
| https://github.com/Sitramonicus/forreview/tree/main/workspace-081126 | **Backup of the agent workspace**, committed 2026-11-08 | The safety net taken before trimming the workspace |
| https://github.com/Sitramonicus/testingester | The peer submission corpus | The 14 student submissions used for the comparison and peer-parity work |

## 2. Peer and reference payroll projects studied

| Link | What it is | What was taken from it |
|---|---|---|
| https://github.com/frappe/hrms | Frappe HRMS | Studied for payroll/HR module structure while planning the ticketing and HR features |
| https://github.com/earnieacts/ph-payroll | `ph-payroll`, Philippine payroll library | Strongest statutory reference found: exact decimals, effective-dated rules, source circulars. Cited in `docs/candidate-policy-review.md`. **Licence: MIT** |
| https://github.com/earnieacts/ph-payroll/blob/main/SPEC.md | Its specification | The statutory component list and 2026 coverage check |
| https://github.com/tenzerodev/rdcruz-ph-payroll | Another Philippine payroll implementation | Cross-check on contribution and withholding-tax treatment |
| https://github.com/manuog1994/payroll-manager | Generic payroll manager | Comparison only; not adopted |
| https://github.com/wasilolly/Employee-Payroll-with-laravel-framework | Laravel payroll project | Comparison only; not adopted |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3 | **MotorPH OOP submission (Team 3)**, the closest peer build | The main comparison target: same brief, class models for `Employee`, `PayrollCalculator`, `SSSCalculator`, `PhilHealthCalculator`, `PagIbigCalculator`, `WithholdingTaxCalculator`, `PayrollRecord`, plus a Swing-style `Login`/`Dashboard`/`LeaveManagement` UI |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/README.md | Team 3's README | Feature claims and setup |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/README.CP2.md | Team 3's checkpoint-2 README | Checkpoint scope |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/SETUP.md | Team 3's setup guide | How they ran it |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/NOTES.md | Team 3's OOP notes | Their design reasoning |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/Employee.java | Team 3 `Employee` | Class-model comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/Person.java | Team 3 `Person` | Inheritance model |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/RegularEmployee.java | Team 3 `RegularEmployee` | Employment types |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/ProbationaryEmployee.java | Team 3 `ProbationaryEmployee` | Employment types |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/PayrollCalculator.java | Team 3 `PayrollCalculator` | Payroll formula comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/PayrollRecord.java | Team 3 `PayrollRecord` | Payslip data shape |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/SSSCalculator.java | Team 3 SSS | Statutory comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/PhilHealthCalculator.java | Team 3 PhilHealth | Statutory comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/PagIbigCalculator.java | Team 3 Pag-IBIG | Statutory comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/WithholdingTaxCalculator.java | Team 3 withholding tax | Statutory comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/GovernmentContributionCalculator.java | Team 3 government contributions | Statutory comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/AttendanceLog.java | Team 3 attendance | Attendance model |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/ConsoleBackendTest.java | Team 3 backend test | Their test approach |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/AttendanceRepository.java | Team 3 attendance repository | Persistence comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/Login.java | Team 3 login | Auth comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LoginHR.java | Team 3 HR login | Role comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/ChangeCredentials.java | Team 3 credential change | Password-change comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/Dashboard.java | Team 3 dashboard | UI comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LandingPage.java | Team 3 landing page | UI comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/Form.java | Team 3 form base | UI comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LoadingScreen.java | Team 3 loading screen | UI comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/MainMenu.java | Team 3 main menu | UI comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LeaveForm.java | Team 3 leave form | Workflow comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LeaveApproval.java | Team 3 leave approval | Workflow comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LeaveManagement.java | Team 3 leave management | Workflow comparison |
| https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LeaveService.java | Team 3 leave service | Workflow comparison |
| https://github.com/fdtan/MO-IT110_OOP-MS2_TanFD | MotorPH OOP MS2, another class submission | Peer-parity comparison |
| https://github.com/J0eychnpulpey/MO-IT103-A1101-CP2-Group-4 | MO-IT103 group 4 | Earlier-course comparison |
| https://github.com/Payroll-Engine/PayrollEngine | **PayrollEngine**, a mature open-source payroll platform (C#/.NET) | The strongest architectural reference: separation of wage types, payrun results, retro pay, calendar/time-data. Studied to sanity-check the engine's structure; not a dependency |

## 3. Sources recovered, extracted or cloned for this build

| Link | What it is | Why it matters |
|---|---|---|
| https://github.com/kubai087/kubai-icons | **Kubai Icons** — source-available icon engine | The brief requires these icons. `src/engine.js`, `src/icons.js`, `src/app.js`, `index.html` and `scripts/` may be copied with copyright headers kept; `PROMPT.md`/`SKILL.md` must not ship and the icons must not be redistributed as a pack. Icons are JS definitions rendered by the engine — no SVG files ship |
| https://github.com/Sitramonicus/www-sweldongpinoy-com-extracted | Extracted source of **sweldongpinoy.com** | The mentor named sweldongpinoy.com as the statutory reference; this is the recovered calculation source used to check the 2026 statutory calculator |
| https://github.com/Sitramonicus/www-sweldongpinoy-com-extracted/archive/refs/heads/main.zip | Direct archive download | How it was retrieved |
| https://github.com/NovusGFX/retro-design-system | Retro design system | Candidate for the layered-colour / tactile UI direction; evaluated, not adopted |
| https://github.com/micah5/PSone.css | PlayStation-1 CSS framework | Retro UI option; evaluated, not adopted |
| https://github.com/nostalgic-css/NES.css | NES.css framework | Retro UI option; evaluated, not adopted |
| https://github.com/npjg/classic.css | Classic Mac CSS | Retro UI option; evaluated, not adopted |
| https://github.com/kristopolous/BOOTSTRA.386 | BOOTSTRA.386 | Retro UI option; evaluated, not adopted |
| https://github.com/sakofchit/system.css | system.css | Retro UI option; evaluated, not adopted |
| https://github.com/JoaoTMDias/poolside-fm-player | Poolside FM player | Studied for the "tiny header widget" interaction idea |
| https://github.com/Shahzaib-Awann/Foodya-Restaurant | Foodya restaurant app | UI/interaction reference |
| https://github.com/CodifyCanvas/foodya.git | Foodya (second copy) | Same reference |
| https://github.com/aashaexo/soundshuman | soundshuman | Sound/interaction reference for the tactile logout |
| https://github.com/JuliusBrussee/caveman | caveman | Text-terseness reference |
| https://github.com/pbakaus/impeccable | impeccable — design polish checklist | Used as a UI quality checklist |
| https://github.com/jalaalrd/anti-ai-slop-writing | anti-ai-slop-writing | Writing-style reference for the documentation |
| https://github.com/warpdotdev/common-skills | common-skills | Agent skill reference |
| https://github.com/OthmanAdi/langsmith-fetch-skill | langsmith-fetch-skill | Agent skill reference |
| https://github.com/DietrichGebert/ponytail | ponytail | Agent skill reference |
| https://github.com/JulietaUla/Montserrat | Montserrat typeface | Font candidate for the MotorPH look |
| https://github.com/rsms/inter | Inter typeface | Font candidate for the MotorPH look |
| https://github.com/carbon-design-system/carbon | IBM Carbon design system | Design-system comparison |

## 4. Design sources that live on GitHub Pages

These are not repositories but are hosted on `github.io`, and the brief cites them directly.

| Link | What it is | Where it is used |
|---|---|---|
| https://sarthology.github.io/Animatopy/ | **Animatopy** | The brief's animation source; supplies the entry/attention animations |
| https://ustymukhman.github.io/Dynamic.css/public/ | **Dynamic.css** | The brief's CSS effects/animations source |

## 5. Libraries, tools and build dependencies

Everything below came from the dependency manifests and HTML, not from the design work. Kept for completeness so a
rebuild can find each one.

**Runtime libraries actually shipped**

| Link | What it is |
|---|---|
| https://github.com/google/gson | Gson — the JSON library the Java API uses (`lib/gson-2.13.2.jar`) |
| https://github.com/firebase/firebase-js-sdk | Firebase JS SDK — the browser SDK for the persistence layer |

**Fonts and styling**

| Link | What it is |
|---|---|
| https://github.com/google/fonts | Google Fonts (source of the display faces) |
| https://github.com/tailwindlabs/tailwindcss | Tailwind — considered for the styling layer |
| https://github.com/vitejs/vite | Vite — the bundler (`npm run build`) |
| https://github.com/GoogleChrome/web-vitals | web-vitals — the Lighthouse/performance measurement library |
| https://github.com/microsoft/playwright | Playwright — browser automation for the checks |
| https://github.com/DefinitelyTyped/DefinitelyTyped | TypeScript type definitions |

**Transitive packages named in `package-lock.json`** (each appears only as a dependency of one of the above):
`7rulnik/source-map-js`, `Rich-Harris/estree-walker`, `Rich-Harris/is-reference`, `Rich-Harris/magic-string`,
`Rich-Harris/periscopic`, `Rich-Harris/zimmerframe`, `SuperchupuDev/tinyglobby`, `Qix-/color-convert`,
`STRML/async-limiter`, `TooTallNate/util-deprecate`, `TrySound/postcss-value-parser`, `G-Rath/babel-plugin-replace-ts-export-assignment`,
`Hanks10100/weex-native-directive`, `LinusU/buffer-from`, `Jimbly/string_decoder`,
`EddyVerbruggen/Custom-URL-scheme`, `ExE-Boss`, `Renegade334`, `Semigradsky`, `WilcoBakker`, `KhafraDev`,
`Microsoft/tslib`, `Microsoft/TypeScript-wiki`, `Microsoft/TypeScript`, `NodeJS`, `Azure`, `Polymer/polymer-bundler`,
`WICG/indexed-db-observers`, `faye/websocket-extensions-node`, `ljharb/shell-quote`,
`troygoode/node-require-directory`, `DynamicODataToSQL/DynamicODataToSQL`, `FastReports/FastReport`,
`DietrichGebert/ponytail`, `NovusGFX/retro-design-system`, `GoogleChromeLabs/first-input-delay`.

**PayrollEngine sub-repositories** (a single reference project, split across many repos):
`Payroll-Engine/PayrollEngine.Backend`, `.Client.Core`, `.Client.Scripting`, `.Client.Services`, `.Client.Test`,
`.Core`, `.Document`, `.Mcp.Server`, `.PayrollConsole`, `.Serilog`, `.WebApp`.

---

## 6. Non-GitHub sources that belong in the same inventory

Recorded here because the link list would be misleading without them.

**Philippine statutory authorities (these decide the arithmetic)**

| Link | What it governs |
|---|---|
| https://www.sss.gov.ph/pay-contribution/ | SSS contribution schedule |
| https://www.philhealth.gov.ph/advisories/2025/PA2025-0002.pdf | PhilHealth CY2025 employer advisory |
| https://www.philhealth.gov.ph/advisories/2026/PA2026-0042.pdf | PhilHealth 2026 advisory |
| https://www.pagibigfund.gov.ph/documents/Circular%20No.%20460%20-%20Guidelines%20on%20the%20Pag-IBIG%20Fund | Pag-IBIG Circular 460 (₱10,000 MFS) |
| https://bir-cdn.bir.gov.ph/local/pdf/Annex%20E%20RR%2011-2018.pdf | BIR Annex E — withholding tax table |
| https://blr.dole.gov.ph/2014/12/11/book-iii-conditions-of-employment/ | DOLE Book III — Labor Code conditions of employment (Art. 94 holiday pay) |
| https://bwc.dole.gov.ph/wp-content/uploads/2024/10/Workers-Statutory-Monetary-Benefits-Handbook-2024-Edition.pdf | DOLE Workers' Statutory Monetary Benefits Handbook (2024) |
| https://www.sweldongpinoy.com/ | The mentor's named statutory reference |

**Design sources (CodePen)** — the brief cites these by pen ID

| Link | Effect it supplies |
|---|---|
| https://codepen.io/shshaw/pen/aZXzzE | The new login / help start page |
| https://codepen.io/ainalem/pen/pXrVmQ | Collapsible sidebar header treatment |
| https://codepen.io/vajkri/pen/PrBQxg | **3D-fold payslip** — the receipt fold, `perspective:1000px`, `rotate3d(1,0,0,±90deg)` pseudo-panels |
| https://codepen.io/ksenia-k/pen/poYZpyQ | The "My Pay" switch |
| https://codepen.io/ericadu/pen/GRZJjjE | The tiny header widget |
| https://codepen.io/supah/pen/mMMdxG | Layered colour treatment |
| https://codepen.io/mubangadv/pen/oNaBGLW | Tamed background |

**Firebase documentation**

| Link | What it covers |
|---|---|
| https://console.firebase.google.com | The console used to create the project |
| https://firebase.google.com/docs/firestore/locations | Firestore location choice |
| https://firebase.google.com/docs/firestore/security/get-started | Security rules basics |
| https://firebase.google.com/docs/firestore/security/rules-fields | Field-level rule conditions |
| https://firebase.google.com/docs/reference/rules/rules.String | `rules.String` reference |

---

### How this list was built

Every `github.com`, `codepen.io`, `github.io` and statutory-authority URL was extracted from the workspace's Markdown,
Java, JavaScript, HTML and CSS sources with a regular expression, then de-duplicated. Links found only inside
`node_modules` changelogs were collapsed to their repository and quarantined in section 5, because they are
transitive dependencies rather than project references. Nothing was added from memory that could not be found in the
tree.
