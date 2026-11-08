# Ticketing: what the three systems do, and what we should copy

You asked me to look at frappe/helpdesk, opensupports and osTicket and work out how ours should be
built. This is that answer, then the design that follows from it.

---

## 1. The three systems, side by side

| | **Frappe Helpdesk** | **OpenSupports** | **osTicket** |
|---|---|---|---|
| Stack | Python + Vue (Frappe framework) | PHP + React | PHP + MySQL, procedural/OO mix |
| Licence | AGPL-3.0 (open) | GPL-3.0 (open) | GPL-2.0 (open) |
| Last release | Active — commits this month | Last commit **2022**, effectively dormant | Active |
| Core idea | Ticket = a record with an **SLA**, a **priority**, an **assignment rule** | Ticket = a conversation with a **status** and **department** | Ticket = a **thread** inside a **department** |
| Statuses | Open / Replied / Resolved / Closed | Open / Pending / Closed | Open / **Pending** / Resolved / Closed (plus custom) |
| Routing | Assignment rules by priority, type, workload | Department | **Department + Help Topic + Filter rules** |
| Conversation | Replies + internal notes | Replies + internal notes | Thread entries: `M` (message), `R` (response), `N` (internal note) |
| Audit trail | Activity log per ticket | Limited | **`ost_thread_event`** — every assignment, transfer, status change |
| Attachments | Yes | Yes | Yes |
| SLA / due dates | **First-class**, with breach tracking | No | **First-class** (`ost_sla`) |
| Roles | Agent / Customer portal (2 kinds) | User / Agent / Admin | User / Agent / **Department Manager** / Admin |

**What all three agree on.** Every one of them separates four things that our current code smashes
together:

1. **A ticket has a type** (why it was opened) — and the type decides the queue.
2. **A ticket belongs to a queue** (department / help topic), not to a person.
3. **A ticket has a conversation** made of typed entries, where *internal notes are a different
   type from public replies*.
4. **Every state change is recorded** with who did it and when, separately from the ticket itself.

**What none of them are.** All three are *customer support* tools. They route an anonymous person's
problem to an agent. Ours is different in one decisive way: **a payroll ticket is a request for a
change to a payroll record**, and that change must be approved by a named person and then applied by
the engine. osTicket's "close the ticket" is not enough — we need "the ticket approved a ₱200 rice
subsidy increase, and payroll run PR-… applied it".

So: copy the structure, not the purpose.

---

## 2. What our design document already says

From MO-IT108 §2.2.2 and the use-case diagram you sent, the tickets we need are:

| Ticket type | Raised by | Handled by | Ends in |
|---|---|---|---|
| Overtime Request | Employee | Supervisor | attendance + earnings |
| Leave Request | Employee | Supervisor | leave calendar + attendance |
| Timesheet Change | Employee | Supervisor | attendance correction |
| Rate Change Request | HR | Payroll | **salary history row** |
| Employee Detail Update | HR | Payroll | employee record |
| Payroll Dispute | Employee | Payroll Staff | adjustment form → payslip |
| Tech Support | Employee | System Admin | none (just resolved) |
| General Inquiry | Employee | HR | none |

That is a **different ticket per workflow**, not one generic "issue". The use-case diagram shows
`Tickets` as a single use case with **extension points** — which is exactly the right model: one
table, a `type` column, and one handler per type.

---

## 3. What we have today, honestly

| Capability | State |
|---|---|
| Create a ticket | **Works** — `POST /api/workflows/tickets` |
| List my tickets | **Works** — `GET /api/workflows` |
| Cancel a ticket | **Works** — `POST /api/workflows/tickets/{id}/cancel` |
| Assign to a queue | Stored as a **label** (`queue`), never enforced |
| Route to a supervisor | **No** — there is no routing |
| Reply / conversation | **No** — one free-text summary field |
| Internal notes | **No** |
| Assign to an agent | **No** |
| Status beyond Open/Cancelled | **No** |
| Approval that changes payroll | **No** — a ticket never affects a payslip |
| Audit trail | **No** |

So the ticket feature today is a **filing cabinet with one drawer**. The design below is what turns
it into a system.

---

## 4. The design to build

### 4.1 One table, typed, with a thread

Modelled on osTicket's shape (`ticket` + `thread` + `thread_event`), because that is the part all
three systems agree on and it maps cleanly onto Firestore.

```
tickets/{ticketId}
  ticketId        "TK-2026-000123"      # sequential per year, readable by a human
  type            "OVERTIME" | "LEAVE" | "TIMESHEET_CHANGE" | "RATE_CHANGE"
                | "EMPLOYEE_DETAIL" | "PAYROLL_DISPUTE" | "TECH_SUPPORT" | "INQUIRY"
  subject         free text, <= 120
  employeeId      who it is about (the requester may differ: HR raises a rate change for someone)
  requestedBy     who opened it
  queue           derived from type, never chosen by the client (§4.2)
  assigneeId      employee number of the handler, null until assigned
  status          OPEN | IN_PROGRESS | PENDING_APPROVAL | RESOLVED | CANCELLED
  priority        LOW | NORMAL | HIGH
  effectiveFrom   date the change should take effect (the payroll-relevant part)
  payload         the type-specific fields, validated per type (§4.3)
  createdAt / updatedAt / resolvedAt / resolvedBy

  entries[]       the conversation, append-only
    - type        MESSAGE (from the requester) | REPLY (public) | NOTE (internal only)
    - authorId, body, at

  events[]        the audit trail, append-only
    - what changed, from, to, by whom, when
```

Firestore keeps `entries` and `events` as **subcollections**, not arrays, so appending never rewrites
the ticket and two people cannot overwrite each other's reply.

### 4.2 Routing is derived, not chosen

The client cannot pick a queue. `type` decides it, and the matrix decides who may work that queue:

| Type | Queue | Who may handle |
|---|---|---|
| OVERTIME, LEAVE, TIMESHEET_CHANGE | Team of the requester's supervisor | That supervisor (`SUPERVISOR`) |
| RATE_CHANGE, EMPLOYEE_DETAIL | HR | HR |
| PAYROLL_DISPUTE | Payroll | Payroll Staff |
| TECH_SUPPORT | System | System Admin |
| INQUIRY | HR | HR |

The supervisor is found from `Employee.getSupervisorId()` — the `Immediate Supervisor` column of
your CSV. No routing table to maintain.

### 4.3 Payload is validated per type

A ticket is not free text. Each type has a schema, checked server-side, so an overtime request can
never carry an unparseable date:

| Type | Required payload |
|---|---|
| OVERTIME | `date`, `hours` (0.5–12), `reason` |
| LEAVE | `leaveType`, `startDate`, `endDate`, `reason` — end ≥ start |
| TIMESHEET_CHANGE | `date`, `timeIn`, `timeOut`, `reason` |
| RATE_CHANGE | `monthlyBasic`, `effectiveFrom`, `reason` |
| EMPLOYEE_DETAIL | `field`, `newValue` — field whitelist only |
| PAYROLL_DISPUTE | `payslipId`, `field`, `claimedAmount`, `reason` |

### 4.4 Approval is a transition, and it is what changes the record

This is the part the customer-support systems do not have and we must not skip:

```
REQUESTED ──approve──▶ APPROVED ──applied by payroll──▶ APPLIED
    └──────reject──▶ REJECTED
```

- Only the routed queue's role may approve (matrix §2.2.1).
- **Nobody approves their own ticket** — the approver's employee number must differ from the
  requester's.
- Approving a `RATE_CHANGE` writes a **new `SalaryRate` row** with `effectiveFrom` from the payload.
  That is the link from ticket → salary history → payslip.
- Approving a `TIMESHEET_CHANGE` writes an attendance correction; approving `LEAVE` writes the leave.
- The approval records `approvedBy` and `approvedAt` on the ticket and an `event`.

### 4.5 What we copy from each

| Take | From | Why |
|---|---|---|
| Typed thread entries `MESSAGE / REPLY / NOTE` | osTicket | Internal notes are essential: a supervisor must be able to discuss a dispute without the employee reading it |
| `thread_event` audit log | osTicket | A payroll change needs a who/what/when trail, permanently |
| Help-topic → queue routing | osTicket | Maps exactly onto our eight ticket types |
| SLA + due date | Frappe Helpdesk | A payroll dispute must be resolved before the next cutoff, so it needs a deadline |
| Assignment rules | Frappe Helpdesk | Route to the supervisor by reporting line, automatically |
| Two-portal split (requester vs agent) | Frappe Helpdesk | Employee sees their tickets; handlers see a queue |
| Simple status set | OpenSupports | Keep the status list short; long status lists rot |

### 4.6 What we deliberately do not copy

| Skip | Why |
|---|---|
| Email ingestion | MotorPH tickets start in the app; no IMAP pipeline needed |
| Knowledge base | Not a payroll requirement |
| Customer organisations / companies | Single employer |
| Plugin system | Complexity with no payoff here |
| Free-form custom fields | We validate per type instead |

---

## 5. Order of work, and why it is this order

Each step is useful on its own; nothing later is required to make an earlier step work.

| # | Step | Unblocks |
|---|---|---|
| 1 | `tickets` with type, queue, status, entries, events | Everything below |
| 2 | Routing by `supervisorId` + the 5-role matrix (already built) | Supervisors see a real queue |
| 3 | Approve / reject transitions, with no self-approval | Leave and overtime become real |
| 4 | Approval writes the record (rate → history, timesheet → attendance) | A ticket changes payroll |
| 5 | SLA / due dates per type | Nothing sits past a cutoff |
| 6 | Payroll dispute → adjustment form → regenerated payslip | The design doc's §Payroll Supervision |

**Step 1 is the one that matters.** Everything else is a rule on top of a ticket that has a type, a
queue, a conversation and an audit trail.

---

## 6. Honest status

**None of §4 is built.** What exists today is the create / list / cancel of a single free-text
ticket with a `queue` label that nothing enforces. The design above is the plan; the current code is
the filing cabinet. I am not going to describe the plan as if it were the product.
