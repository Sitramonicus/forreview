# web/b_basic

The screens and the glue.

- `Index.html` — one page, all views, toggled by `hidden`. Nav is role-filtered but the server is
  what actually enforces anything.
- `App.js` — sign-in, workspace bootstrap, the table rendering. It's big; it's the app.
- `Roles.js` — client mirror of the access matrix. Server is the authority.
- The sidebar footer used to say "Prototype data only. Not for payroll processing." It does
  payroll now, so it doesn't say that.
- `Styles.css` and `Workflow.css` are dead weight from before the Tailwind theme. `main.css` (at
  `web/`) is the real stylesheet and it compiles to `web/app.css`. Don't edit `app.css` by hand.

## ui pass, this one

- `Landing.html` is new and now owns `/`. `/signin` + `/login` serve `Index.html`. The app used to be `/`.
- `RailNav.js` — collapse button, remembered in localStorage, hover flyout when collapsed. Icon-only
  nav is a guessing game otherwise.
- Sign-out button in the topbar never had a click handler. It does now, and it goes to `/` not a reload.
- `showWorkspace()` was called as `showWorkspace(state.user)` from `enterAs()`, and `state.user` was
  never assigned — so the sign-out button stayed `hidden` and the operator label stayed "FIREBASE
  SANDBOX". Pass the account.
- The `auth-stage` wrapper accidentally wrapped the *whole* app shell at first; shell was 440px wide
  inside the login grid. Stage now closes before `#app-shell`, and `showWorkspace` hides it.
- `.auth-gate` still had `min-h-screen bg-zinc-50` from the older layer → big grey slab behind the
  login card until `.auth-stage .auth-gate` neutralised it. Watch for that pattern: old rules win over
  new ones when the new block sits earlier in the cascade.
- Forms: one correction form got restructured by hand into `.form-section` (header/body/actions). The
  other 28 `payroll-preview-form`s are skinned by CSS instead — restructuring all of them was asking
  for a broken selector.
- `.tab-cards.iso` gives "Your pages" the extruded tile look. `grid-template-columns: none` was the
  fix; the base 4-col template kept the extra tiles off-flow otherwise.
- Copy sweep: Firebase/Firestore out of the UI. `friendlyAuthError()` deleted (no callers, all its
  cases were Firebase console URLs). `FIREBASE_VIEWS` → `UNSIGNED_VIEWS`.

## §12 checklist, point by point

Read the section properly this pass instead of guessing from the titles. State now:

- **12.1 landing** — done. `/` serves `Landing.html`, `/signin` + `/login` serve the app, both CTAs work.
- **12.2 icons** — done. Kubai-style path icons in the sidebar. Bell and arrows were emoji/glyphs and
  are SVG now; `Notifications.js` builds the bell with `createElementNS` instead of `textContent = '🔔'`.
  The `→` still in a few `f_flares` files is prose ("A → B"), not an icon; left alone.
- **12.3 sidebar buttons** — the sample is a perspective tilt on hover, not the extrusion I built first.
  Redone: `perspective(900px) rotateX(4deg) rotateY(-3deg)`, icon scales and turns orange.
- **12.4 collapsible** — done. Logo stays, labels collapse, hover/focus flyout, remembered, touch-safe.
- **12.5 payslip** — done. Letterhead kept, print button kept, sheet now has the rule/band treatment and
  an A4 print sheet. Verified by rendering the PDF: no nav, no buttons, two pages, all figures present.
- **12.6 logout** — Appendix B's press-button, scoped to `.push-btn`.
- **12.7 Your Pages** — Appendix C's faces, 400ms shift, reduced-motion opt-out. The plane itself is NOT
  rotated 30°: eight two-word destinations with a sentence each stop being readable. Documented in the
  CSS comment so nobody "fixes" it back.
- **12.8 My Pay switch** — `.pay-switch`, keyboard reachable, `aria-pressed`, note line under it.
- **12.9 small header component** — `.pulse-widget` (dot + spark bars + LIVE). Hides under 1100px.
- **12.10 / 12.11 layered colour + background** — the `land-bg` rotated planes on landing/sign-in/404.

Trap worth remembering: an old `.tab-cards.iso` rule survived a python slice because the anchor matched
an *earlier* duplicate comment. Symptoms were a 30° rotated grid I could not explain from the rule I could
see. `grep -c` the selector in the built `app.css`, not just the source.

## responsive

Checked 1440 / 1024 / 768 / 390 on the landing page and the workspace. Landing was stacking the CTA
under a broken two-column grid on phones — added breakpoints at 980 and 620.

Workspace had three horizontal-overflow sources, all real:
1. `.rail-toggle` sits at `right: -3px` on the sidebar → off-screen on a phone. Hidden under 760px.
2. `.topbar-right` had eight controls in one un-wrappable flex row. Now wraps, push button shrinks.
3. `.payroll-breakdown-table` is 680px at minimum → let `.table-wrap` scroll instead of the page.

Mobile nav is a horizontal scroller now, not a 4-up grid that wrapped to two rows.
