# §12 UI components — what is built

Every clause in `MotorPH_Optimised_Implementation_Prompt_v2.md` §12, against what is now in the tree.
The brief's rule throughout: adapt the reference's *interaction*, keep MotorPH's palette, logo and layout
dimensions, and scope every selector to its component.

| § | Item | Where | State |
|---|---|---|---|
| 12.1 | Landing page | `web/b_basic/Landing.html`, `main.css` `.land*` | built |
| 12.2 | Icons and animation | `Index.html` nav + `.nav-ico`, `Notifications.js` `paintBell()` | built |
| 12.3 | 3D sidebar buttons | `main.css` `.nav-item:hover` | built |
| 12.4 | Collapsible sidebar | `RailNav.js`, `main.css` `.rail` | built |
| 12.5 | Payslip presentation | `main.css` `.payslip*`, `@media print` | built |
| 12.6 | Logout button | `Index.html` `#sign-out-button`, `.push-btn` | built |
| 12.7 | Your Pages isometric tiles | `main.css` `.tab-cards.iso` | built, adapted |
| 12.8 | My Pay switch | `Index.html` `.pay-switch`, `App.js` `[data-pay-mode]` | built |
| 12.9 | Small header component | `.pulse-widget` in the topbar | built |
| 12.10 | Layered colour effect | `.land-bg` planes | built |
| 12.11 | Background effect | `.land-bg` + drift keyframes | built |

## 12.1 Landing

`/` now serves `Landing.html`; `/signin` and `/login` serve the app. **Login** opens the employee
number form, **Contact Help** opens `/help`. Both are real routes with real targets. The example
imagery is replaced by a payslip summary card drawn from the same numbers the engine produces
(net ₱22,711.67 for employee 10015, cutoff 1 of 2026-01 — the golden payslip), not stock art.
Accent is the existing MotorPH flame (`--color-flame-600/700`), not the reference's palette.
CTA sits above the fold at 1440, 1024, 768 and 390 — measured, not assumed.

## 12.2 Icons

Sidebar icons are inline 24×24 SVG paths in the Kubai flat style. Nothing in the UI uses an emoji or
a bare Unicode character as an icon any more: the notification bell was `textContent = '🔔'` in
`Notifications.js` and is now built with `createElementNS`; the sidebar card arrows were `→` and the
dialog close was `×`, both SVG now. The `→` that remains in four `f_flares` files is prose
("10001 → 10002"), not an icon, and was left alone.

Animation is used in three places only — landing plane drift, the live dot, and the nav icon tilt —
with `@media (prefers-reduced-motion: reduce)` killing all three. No animation library is loaded.

## 12.3 Sidebar buttons

Appendix A's sample is a **perspective tilt**, not an extruded card. The first attempt here built an
extrusion instead, which was wrong. Corrected to:

```css
.nav-item:hover {
  transform: perspective(900px) rotateX(4deg) rotateY(-3deg) translateX(3px) translateY(-1px);
}
```

with the icon at `scale(1.18) rotate(6deg)` and the fill turning flame. Selectors are `.nav-item`,
scoped to the sidebar; the sample's global `body` and `svg` rules were not copied. Labels stay the
application's own.

## 12.4 Collapsible sidebar

Three states, as specified:

- **Expanded** — logo, icons, labels, the workspace caption and the build stamp.
- **Collapsed** — 76px rail, logo retained, icons only. Remembered in `localStorage`.
- **Preview** — hovering or focusing an icon pops `.rail-flyout` with the full label, so an icon is
  never the only cue. Restores on exit.

There is a dedicated toggle (`#rail-toggle`), `aria-expanded` and a changing `aria-label` on it, and
each collapsed button keeps its own accessible name. Under 1000px the rail starts collapsed. On touch
(there is no hover) the label is still reachable — the flyout also opens on `:focus`.

## 12.5 Payslip

Letterhead, address block, logo, employee/pay-period/department details and **Print payslip** are all
retained. What was added is the sheet treatment: navy top rule with a flame segment, the details in a
tinted band, ruled earnings/deductions lines, a heavier net-pay rule and a generation stamp.

Print is a real A4 sheet. `@page { size: A4; margin: 14mm }`, everything hidden except
`#payroll-payslip`, buttons and flags suppressed, `page-break-inside: avoid` on the table. Verified by
rendering the print media to PDF and extracting its text: it contains the payslip figures and none of
`Sign out`, `Switch account`, `Overview`, `LIVE` or `Print payslip`.

## 12.6 Logout

Appendix B's press button, scoped to `.push-btn` and its `push-outer` / `push-inner` children. No
global `body` or `button` rules copied. It performs the real sign-out: `POST /api/auth/signout`, clears
the stored account and session, then navigates to `/`.

It also had no click handler at all before this pass — the button rendered and did nothing, because
`showWorkspace()` was being called with `state.user` (never assigned) instead of the account and the
button stayed `hidden` anyway. Both fixed.

## 12.7 Your Pages

Appendix C's geometry: front face `skewY(41.25deg) scaleX(.5)`, side face `skewX(49deg) scaleY(.5)`,
each growing to full scale on hover, a `translate` shift on the tile, `400ms ease`, and the active
state collapsing both faces back to zero.

**One deliberate departure.** The sample rotates the entire plane `scaleX(.866) rotateZ(30deg)
skewX(150deg)`. With two-word labels ("My Pay") that reads fine. With this application's eight
destinations and a description sentence each, the rotated plane makes the text unreadable — measured,
not guessed: the tiles overlapped and the counter-rotated content collapsed. So the plane stays square
and only the tiles extrude. The CSS comment says so, so it does not get "fixed" back.

Every tile keeps its real `data-open-view` target, is a real `<button>`, and is reachable by keyboard
with a flame focus ring.

## 12.8 My Pay switch

`.pay-switch` is a two-button `role="group"` with `aria-pressed` on each and a live note underneath
that changes with the selection. It is above the monthly contribution preview on the Payroll page, and
it preserves the employee/period context because it does not touch the form — it only relabels what
the numbers are being compared against. Both buttons are real `<button>`s, so keyboard and screen
reader behaviour is native.

## 12.9 Small header component

`.pulse-widget` — a live dot, a seven-bar sparkline and the word LIVE — sits after the breadcrumb and
before the operator name. It is 24px tall against a 56px topbar, muted to `rgba(255,255,255,.05)` with
a `white/15` border, so it stays subordinate to the logo and the nav. It hides under 760px, and the
operator name, avatar and dividers hide with it, so nothing crowds the sign-out control at narrow
widths.

## 12.10 / 12.11 Layered colour and background

`.land-bg` holds four absolutely-positioned, rotated, skewed planes at opacities .55 / .35 / .22 / .30
behind a radial-gradient wash. They drift on a 26s alternate loop, offset from each other, and stop
entirely under `prefers-reduced-motion`. They sit behind the content with `pointer-events: none` and
never cross text. Under 620px the planes are removed rather than shrunk, since a phone does not need to
pay for them.

## Responsive

Measured at 1440 / 1024 / 768 / 390 on landing, sign-in and workspace: no horizontal page overflow at
any width, CTA above the fold at every width. Three real overflow sources were found and fixed — the
rail toggle hanging off the sidebar edge, an eight-control topbar cluster that could not wrap, and the
680px minimum payroll table (now scrolls inside `.table-wrap` instead of widening the page).

## Not yet done

`§12` is complete. What remains from the wider brief is §4 member notation and §16's remaining items,
plus the e2e rewrite decision.
