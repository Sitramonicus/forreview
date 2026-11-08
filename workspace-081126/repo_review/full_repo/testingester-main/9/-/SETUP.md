# Setup — how to run this project

**MO-IT110 · Team 3 · MotorPH OOP refactor**

Two IDEs are supported. Pick one — the code is the same, only the project settings differ:

| I need to… | Use |
|---|---|
| Write / refactor code, run the console tests, debug | **VS Code** or **Eclipse** |
| Just run the app and check a feature | either one, or the terminal commands below |

---

## 1. Prerequisites

| Requirement | Version | Note |
|---|---|---|
| **JDK** | **21** (Temurin / Adoptium recommended) | Do **not** use JDK 25. Eclipse bundles a newer runtime for itself, but the project must compile to **21** so every member and the instructor can run it (`class file version 65`). |
| **Git** | any | `git clone git@github.com:J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3.git` |
| **IDE** | VS Code 1.9x+ **or** Eclipse IDE for Enterprise Java and Web Developers 2024-09+ | Java support required (Extension Pack for Java / Eclipse JDT) |

---

## 2. Repository layout — two source roots (important)

| Path | What it is | Source root? |
|---|---|---|
| `src/motorphemployee/` | The **CP2 baseline GUI** (`MainMenu`, `Dashboard`, `Login`, …) — the code being refactored | ✅ **yes** |
| `oop/src/motorphemployee/oop/` | The **new OOP backend** (`Person`, `Employee`, calculators, `PayrollRecord`, …) | ✅ **yes** |
| `oop/*.txt`, `oop/NOTES.md` | Console test + CP2 parity **evidence** | — |
| `src/motorphemployee/*.csv` | Sample data (fictional MotorPH dataset) | — |
| `nbproject/`, `build.xml` | NetBeans/Ant metadata from the CP2 baseline — ignored by both IDEs | — |

> ⚠️ **The #1 setup mistake:** adding only `src`. If `oop/src` is missing from the build path, every `motorphemployee.oop` import shows as an error and `PayrollCalculator` appears not to exist.

---

## 3. VS Code

1. Install the extension **Extension Pack for Java** (`vscjava.vscode-java-pack`).
2. `File → Open Folder…` → the repository root (the folder containing `src` and `oop`).
3. Create `.vscode/settings.json` (this folder is git-ignored, so each member keeps their own):

   ```json
   {
     "java.project.sourcePaths": ["src", "oop/src"],
     "java.project.outputPath": "build/classes",
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-21",
         "path": "C:\\Program Files\\Eclipse Adoptium\\jdk-21",
         "default": true
       }
     ]
   }
   ```

   Change `path` to your own JDK 21 folder.

4. Create `.vscode/launch.json`:

   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Run app (LoadingScreen)",
         "request": "launch",
         "mainClass": "motorphemployee.LoadingScreen",
         "cwd": "${workspaceFolder}",
         "console": "internalConsole"
       },
       {
         "type": "java",
         "name": "Console backend test",
         "request": "launch",
         "mainClass": "motorphemployee.oop.ConsoleBackendTest",
         "cwd": "${workspaceFolder}",
         "console": "integratedTerminal"
       }
     ]
   }
   ```

   > `cwd` **must** be `${workspaceFolder}`: the baseline loads its data with the relative path `src/motorphemployee/data.csv`.

5. Press `Ctrl+Shift+D`, pick a configuration, press `F5`.
   First launch takes 30–60 s while the Java language server indexes the project.

**Troubleshooting (VS Code)**

| Symptom | Fix |
|---|---|
| `motorphemployee.oop` unresolved | `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace**, then reopen the folder |
| `Could not find or load main class` | Confirm both source paths are listed in `settings.json`; or use the terminal commands in section 5 |
| Login says *"Incorrect employee number, username, or password"* even with the sample credentials | The working directory is wrong — the app cannot read `src/motorphemployee/data.csv`. Set `"cwd": "${workspaceFolder}"` in the launch configuration |

---

## 4. Eclipse

The repository ships **`.project`**, **`.classpath`** and **`.settings/`** already configured with both source folders and **Java 21** compliance — so no manual setup.

1. **Import:** `File → Import… → General → Existing Projects into Workspace` → **Select root directory** = the cloned repository → `Finish`.
   *(Do **not** use `New → Java Project` — the project description is already in the repository.)*
2. **Verify the build path:** right-click the project → `Build Path → Configure Build Path… → Source` tab. You must see **both**:
   - `MO-IT110-MotorPH-OOP-Team3/src`
   - `MO-IT110-MotorPH-OOP-Team3/oop/src`
3. **Verify the JRE:** same dialog, `Libraries` tab → the entry should read **JavaSE-21**.
   If it shows *unbound*: `Add Library… → JRE System Library → Alternate JRE` (or Workspace default) and point it at your JDK 21. Then confirm `Project → Properties → Java Compiler → Compiler compliance level: 21`.
4. **Run the app:** `Run → Run Configurations… → Java Application → New`
   - **Main class:** `motorphemployee.LoadingScreen`
   - **Working directory** (Arguments tab): the **project root** (`${workspace_loc:MO-IT110-MotorPH-OOP-Team3}`) — this is the default, keep it.
5. **Run the console tests:** same dialog, main class `motorphemployee.oop.ConsoleBackendTest`.

**Troubleshooting (Eclipse)**

| Symptom | Fix |
|---|---|
| `bin/` shows as untracked in Git | It is git-ignored; never commit it. Use `Project → Clean…` if the output looks stale |
| Unbound `JavaSE-21` classpath entry | Section 4, step 3 |
| Red X on `oop/src` package | `Project → Clean…`, then `Build Path → Configure Build Path → Source`, remove and re-add `oop/src` |
| Console output empty | Check the **Console** view is on the process you started (`Display Selected Console`) |

---

## 5. Terminal only (no IDE)

From the **repository root**:

```bash
# backend only (OOP package)
javac -encoding UTF-8 -Xlint:all -d build/classes $(find oop/src -name '*.java')
java -cp build/classes motorphemployee.oop.ConsoleBackendTest

# whole application (baseline GUI + backend)
javac -encoding UTF-8 -d build/classes $(find src oop/src -name '*.java')
java -cp build/classes motorphemployee.LoadingScreen
```

Expected console-test output ends with:

```
=== SUMMARY: 91 passed, 0 failed ===
RESULT: PASS
```

---

## 6. Sample login (fictional data)

| Screen | Fields | Value |
|---|---|---|
| Employee login (`Login`) | Employee # · Username · Password | `1`–`34` · `admin` · `password` |
| HR login (`LoginHR`) | Username · Password | `admin` · `password` |

All 34 employee records share the same demo credentials, and passwords are stored in plaintext in `data.csv` / `HRLogin.csv`. That is **CP2 baseline behaviour**. The MS1 design defines a `Credentials` class for this; the migration is not part of the current refactor and is recorded as an open item in the MS2 Known Issues list.

---

## 7. Evidence files

Keep these in sync with the worksheet — they are the proof quoted in the MS2 tabs:

| File | Content |
|---|---|
| `oop/console-test-output.txt` | Backend assertions (91/91 PASS, sections A–I) |
| `oop/parity-report.txt` | OOP vs CP2 computation comparison |
| `oop/cp2-parity-output.txt` | Raw CP2 reference output |
| `oop/matrix-verification.md` | The four statutory formulas checked against the official matrices (per-input tables) |
| `oop/NOTES.md` | Scope, adjustments, known issues (Issues 1 to 5) |

---

## 8. Ground rules

- **Never commit** `build/`, `bin/`, `*.class`, `dist/`, `nbproject/private/`.
- `.vscode/` is git-ignored (machine-specific). `.project`, `.classpath`, `.settings/` **are** committed — they only contain portable settings, and they make Eclipse setup one click.
- **GUI = presentation only.** All payroll computation belongs to the backend (`motorphemployee.oop`).
- The app, the worksheet, the test evidence and this repository must tell the **same story** (MS2 consistency requirement).
