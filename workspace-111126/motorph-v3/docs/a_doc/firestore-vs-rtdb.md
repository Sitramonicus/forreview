# Firestore or Realtime Database?

**Decision: stay on Firestore. No change needed.**

## What the 1 MiB limit actually is

It is a limit on a **single document**, not on the database
([Firestore quotas](https://docs.cloud.google.com/firestore/quotas)). There is no documented cap
on documents per collection. Firestore also caps one *field value* at 1 MiB − 89 bytes, and the
whole store is currently a single field, which is why chunking exists.

Realtime Database's numbers, for comparison
([RTDB limits](https://firebase.google.com/docs/database/usage/limits)):

| | Firestore | Realtime Database |
|---|---|---|
| Unit that is capped | 1 MiB per **document** | 256 MB per **read response**, 16 MB per write from an SDK (256 MB over REST) |
| Reads for a range query | indexed, cheap, paginated | download the whole subtree and filter in memory |
| Schema | none needed, but typed fields | one JSON tree, max 32 levels deep |
| Counts against us | 50k reads / 20k writes per day free | GB downloaded (priced per bandwidth) |

## Why chunking makes Firestore's limit a non-issue

The attendance store is 853,787 bytes at 7,421 rows, about **115 bytes per row**. Stored in
Firestore that becomes 2 chunks. Projected:

| Scale | Rows | Size | Chunks |
|---|---|---|---|
| today (34 employees) | 7,421 | 0.85 MB | 2 |
| 100 employees, one year | 10,800 | 1.2 MB | 2 |
| 500 employees, one year | 54,000 | 6.2 MB | 9 |
| 1,000 employees, one year | 108,000 | 12.4 MB | 18 |

Eighteen documents to hold a thousand employees' full year. The chunking code already handles
growing and shrinking (it deletes surplus chunks), and a round trip is byte-identical.

## Why Realtime Database would actually be worse here

1. **It rewards exactly what this codebase avoids.** RTDB is cheapest when the client downloads
   whole subtrees and keeps them live. This app never lets the browser touch the database — the
   server mediates every request, and role scoping means an employee sees their own rows only.
   Putting RTDB in would still be a fetch-the-subtree-then-filter design, server-side.
2. **No queries.** Range reads over attendance (a date window, one employee) are index lookups in
   Firestore. In RTDB the server pulls the subtree and filters in Java — more bytes, more CPU, no
   benefit.
3. **Security rules are coarser.** RTDB rules cascade down the tree by `auth.uid`. Our rules would
   have to express five roles and team scopes over a JSON tree, with no equivalent of the
   per-document path matching Firestore gives.
4. **It saves porting, not operating cost.** Switching means a second `StateStore` implementation,
   a second fake for the tests, and a second round-trip proof. It buys roughly 1.5 MB of headroom
   that nothing needs.

## The one honest caveat

`FirestoreStateStore.save()` writes **every** store on every change — the whole 886 KB document,
2 chunk writes for attendance plus 30 small ones. The free tier allows 20,000 writes per day, so
that is roughly 600 saves per day before it costs anything. A demo or a busy single office is
comfortably inside that. If this ever runs at hundreds of concurrent editors, the fix is to write
only the stores that changed and debounce, **not** to change database. That is a contained change
inside one class.

## What to do

Nothing. Configure the three environment variables and use it. If the day ever comes that a store
outgrows the chunking, the cheapest next step is writing only changed stores — still in Firestore.
