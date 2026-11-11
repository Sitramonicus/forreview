# l_log

Audit trail. One file, `AuditLog`.

Actor, action, target, time, outcome. No passwords, no raw session tokens, no home addresses.
If it isn't safe to show HR, it doesn't go in the log.

## member notation

`AuditLog` and `AuditLogTest` both converted in one go — a converted class and an unconverted test
cannot compile. `lg_iKeep`, `lg_lEntries`, `lg_eRow` etc. Nested `Entry`'s public fields are prefixed
too, so `entry.actor` reads `entry.lg_sActor` at the call sites in `AppServer`.
