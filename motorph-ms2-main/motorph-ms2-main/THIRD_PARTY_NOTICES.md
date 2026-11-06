# Third-party notices and design references

## Included in the prototype

- **Gson 2.13.2** — https://github.com/google/gson. Used for Firestore REST response parsing in the Java backend. License: Apache License 2.0; full text is in `third_party/licenses/Gson-Apache-2.0.txt`.
- **Firebase JavaScript SDK 12.9.0** — https://github.com/firebase/firebase-js-sdk. Used only for browser Firebase Authentication. License: Apache License 2.0; text is in `third_party/licenses/Firebase-Apache-2.0.txt`. No Firebase Admin SDK or privileged credentials are used.
- **Vite 8.3.5** — https://github.com/vitejs/vite. Used as the local frontend development server and production bundler. License: MIT; text is in `third_party/licenses/Vite-MIT.txt`.
- **Tailwind CSS 4.3.4** (with `@tailwindcss/vite`) — https://github.com/tailwindlabs/tailwindcss. Builds all interface styles in `web/main.css`. License: MIT; text is in `third_party/licenses/TailwindCSS-MIT.txt`.
- **Inter** font, via `@fontsource-variable/inter` — https://github.com/rsms/inter. Self-hosted interface font. License: SIL Open Font License 1.1; text is in `third_party/licenses/Inter-OFL-1.1.txt`.
- **Montserrat** font, via `@fontsource-variable/montserrat` — https://github.com/JulietaUla/Montserrat. Self-hosted font for the MotorPH wordmark and payslip letterhead. License: SIL Open Font License 1.1; text is in `third_party/licenses/Montserrat-OFL-1.1.txt`.

The MotorPH logo (`web/public/motorph-logo.png`, plus `motorph-logo-transparent.png` with the white background removed) was supplied by the project owner from the MotorPH course materials (2026-11-05). The letterhead company name, address, phone and email are the fictional MotorPH details from the course materials.

## Design references only (no code or assets copied)

- system.css — https://github.com/sakofchit/system.css (its striped title bar was adapted in the earlier stylesheet; removed in the Tailwind redesign; license kept in `third_party/licenses/system.css-MIT.txt`)

- NES.css — https://github.com/nostalgic-css/NES.css (not selected as the interface base; its game aesthetic is outside the intended professional direction)
- IBM Carbon — https://github.com/carbon-design-system/carbon (potential reference for enterprise interaction/accessibility patterns)
- Retro Design System — https://github.com/NovusGFX/retro-design-system
- classic.css — https://github.com/npjg/classic.css
- PSone.css — https://github.com/micah5/PSone.css (not selected as the interface base; game-console styling is outside the intended direction)
- BOOTSTRA.386 — https://github.com/kristopolous/BOOTSTRA.386
- Poolside FM player — https://github.com/JoaoTMDias/poolside-fm-player (visual reference only; no code or brand assets copied)
- Poolsuite — https://poolsuite.net (visual/mood reference only; no brand assets copied)

Before incorporating any additional code, font, icon, or image, inspect its exact license and dependency notices, confirm permitted use, and record the attribution here. Do not copy proprietary logos or brand artwork.
