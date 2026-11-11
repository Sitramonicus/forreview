# Folder 11 — Foodya Restaurant POS

**Upstream:** [Shahzaib-Awann/Foodya-Restaurant](https://github.com/Shahzaib-Awann/Foodya-Restaurant)  
**Snapshot:** `main`, `698ac13942aad709aa31659d0f61107dff6204ef`; GitHub metadata checked 2026-11-04 (12 stars, 3 forks).  
**Local source:** `repo_review/full_repo/testingester-main/11/-/Foodya-Restaurant/`  
**Evidence boundary:** README, App Router pages/API routes, Drizzle CRUD functions and schemas were read statically. Restaurant operations are the central product; employee salary handling is one module. This is **not** a payroll-rule reference.

## Major-feature map

| Major | Feature | Minor features and sub-functions |
|---|---|---|
| 1 | Public restaurant website/menu | Home/about pages; public menu categories; public menu items; category filter; item cards and public data routes. |
| 2 | Login, sessions, protected application and RBAC | Credentials sign-in; session; middleware-gated restaurant routes; user CRUD/activation; role CRUD; module/page registry; permission assignments; permission-aware navigation and page guards; database health/error route. |
| 3 | POS dashboard and navigation | Dashboard/quick actions; responsive sidebar; keyboard shortcuts; DB availability indicator. |
| 4 | Menu catalog management | Category CRUD; menu-item CRUD; variants/options/sizes; category association; item image; server-side validation. |
| 5 | Table inventory and booking | Table CRUD/status; booking CRUD; optional deposit/advance; time-window state synchronization; manual check-in/check-out; cancellation, completion, expiry/no-show; table state maintenance. |
| 6 | Order/cart lifecycle | Build cart; choose order type; dine-in table selection; fetch existing order; create/update order and line items; update related booking/table states; transactional order-item replacement. |
| 7 | Invoice and receipt lifecycle | Generate invoice from order/cart; customer and payment details; subtotal/discount/advance/grand total; retrieve/edit/view invoice; print receipt/slip; invoice history. |
| 8 | Employee records and compensation | Employee CRUD; personal/record/salary forms; employment history and salary changes; monthly payroll row generation; bonuses/penalties/reasons; pending/paid state; salary history/print. |
| 9 | Financial transaction tracking | Transaction categories; manual income; expenses; transaction list and date-range/filter queries. |
| 10 | Reports and analytics | Header metric cards; transaction table; monthly/yearly income/expense and profit trends; dashboard aggregates. |

## Feature pseudocode


## High-level pseudocode — each major feature

### Major 1 — Public website/menu

```text
PROCEDURE MAJOR_1_PUBLIC_WEBSITE_MENU
    render website pages
    load public categories and menu
    filter visible items by selected category
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_1.1  // Public landing/about pages
        CALL MINOR_1.2  // Browse public menu
END PROCEDURE
```

### Major 2 — Authentication and RBAC

```text
PROCEDURE MAJOR_2_AUTHENTICATION_AND_RBAC
    authenticate credentials and establish session
    verify route and module/page permission
    allow user/role/module/permission administration only to authorized account
    return 403 or database error where required
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_2.1  // Sign in and session
        CALL MINOR_2.2  // Protect application routes
        CALL MINOR_2.3  // User, role and module administration
        CALL MINOR_2.4  // Database status and failure page
END PROCEDURE
```

### Major 3 — POS dashboard/navigation

```text
PROCEDURE MAJOR_3_POS_DASHBOARD_NAVIGATION
    load dashboard view
    render permitted quick actions
    apply responsive navigation and shortcut behavior
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_3.1  // Dashboard quick actions
        CALL MINOR_3.2  // Keyboard shortcuts and responsive navigation
END PROCEDURE
```

### Major 4 — Menu catalog

```text
PROCEDURE MAJOR_4_MENU_CATALOG
    validate catalog mutations
    save category, item, variant and image relationships
    refresh menu data
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_4.1  // Category CRUD
        CALL MINOR_4.2  // Menu items and variants
END PROCEDURE
```

### Major 5 — Tables and bookings

```text
PROCEDURE MAJOR_5_TABLES_AND_BOOKINGS
    load table and booking state
    validate reservation or table mutation
    synchronize time-based statuses or apply manual check-in/out
    persist related table/booking changes
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_5.1  // Restaurant table management
        CALL MINOR_5.2  // Create/update booking
        CALL MINOR_5.3  // Time-driven booking/table synchronization
        CALL MINOR_5.4  // Manual table check-in/out
        CALL MINOR_5.5  // Booking cancellation
END PROCEDURE
```

### Major 6 — Orders and cart

```text
PROCEDURE MAJOR_6_ORDERS_AND_CART
    collect order type, table and cart lines
    validate nonempty order
    create/update order and line items transactionally
    return saved order for invoicing
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_6.1  // Build and edit cart
        CALL MINOR_6.2  // Load existing table order
        CALL MINOR_6.3  // Save order
END PROCEDURE
```

### Major 7 — Invoices and receipts

```text
PROCEDURE MAJOR_7_INVOICES_AND_RECEIPTS
    load order and booking/cart values
    validate invoice and payment input
    persist invoice totals and line detail
    render or print receipt
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_7.1  // Create invoice
        CALL MINOR_7.2  // Retrieve/update/view invoice
        CALL MINOR_7.3  // Print invoice slip
END PROCEDURE
```

### Major 8 — Employees and salary records

```text
PROCEDURE MAJOR_8_EMPLOYEES_AND_SALARY_RECORDS
    maintain employee and employment/salary history
    refresh missing payroll-month rows
    summarize and update payment status
    show employee salary history
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_8.1  // Employee CRUD/profile
        CALL MINOR_8.2  // Employment and salary change history
        CALL MINOR_8.3  // Generate monthly payroll records
        CALL MINOR_8.4  // Payroll summaries and payment state
END PROCEDURE
```

### Major 9 — Financial transactions

```text
PROCEDURE MAJOR_9_FINANCIAL_TRANSACTIONS
    validate transaction category/income/expense input
    persist transaction
    refresh filtered transaction data
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_9.1  // Transaction categories
        CALL MINOR_9.2  // Record incomes and expenses
END PROCEDURE
```

### Major 10 — Reports/analytics

```text
PROCEDURE MAJOR_10_REPORTS_ANALYTICS
    collect date-range data from finance and employee records
    calculate summary cards and chart series
    render analytical report and transaction table
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_10.1  // Dashboard metric cards and chart
        CALL MINOR_10.2  // Transaction report
END PROCEDURE
```

### Major 1 — Public website/menu

**Minor 1.1 — Public landing/about pages** — sub-functions: render website shell, home page, project/author/about pages.

```text
ON public route request
    render the selected public page in public layout
```

**Minor 1.2 — Browse public menu** — sub-functions: GET public categories/items; show category navigation and item cards; filter visible items by selected category.

```text
categories ← GET public category endpoint
items ← GET public menu endpoint
render menu categories and item cards
ON category selection:
    show items matching selected category
```

### Major 2 — Authentication and RBAC

**Minor 2.1 — Sign in and session** — sub-functions: accept credentials; authenticate through Auth.js; create session; redirect to protected app; handle failed sign-in.

```text
ON login submission(credentials)
    authenticate credentials with Auth.js
    IF authentication fails THEN show sign-in error
    ELSE establish session and navigate into restaurant application
```

**Minor 2.2 — Protect application routes** — sub-functions: middleware checks session; anonymous request goes to login; signed-in user proceeds; route-level page access is checked by module permission helpers.

```text
ON protected request
    session ← read Auth.js session
    IF session missing THEN redirect to /login
    ELSE continue to route
    pagePermission ← resolve current user's role/module permission
    IF not permitted THEN show 403 page
```

**Minor 2.3 — User, role and module administration** — sub-functions: list/create/edit/activate/deactivate users; create/update/delete roles; define app modules/pages; assign a role's allowed modules/pages; load permissions for navigation.

```text
FOR each submitted admin-panel form
    authenticate caller and validate input schema
    IF action = user change THEN insert/update user or active flag
    IF action = role change THEN insert/update/delete role
    IF action = module change THEN insert/update/delete module/page definition
    IF action = permission change THEN save role-module/page grant
    reload permitted navigation for affected session
```

**Minor 2.4 — Database status and failure page** — sub-functions: health endpoint; UI hook polls/checks DB state; show database-unavailable error instead of normal workflow.

```text
ON health check
    try database query
    return healthy or unavailable result
UI maps unavailable result to service/database error state
```

### Major 3 — POS dashboard/navigation

**Minor 3.1 — Dashboard quick actions** — sub-functions: protected landing; render operational summary/quick navigation.

```text
verify session and dashboard permission
load dashboard view data
render cards and quick actions
```

**Minor 3.2 — Keyboard shortcuts and responsive navigation** — sub-functions: register listed Alt+letter shortcuts; navigate to module route; adapt layout on small screens; hide links without permissions.

```text
ON configured shortcut key
    if destination permission is granted THEN navigate to destination
    else do not open protected module
render responsive menu with only permitted destinations
```

### Major 4 — Menu catalog

**Minor 4.1 — Category CRUD** — sub-functions: list categories; validate and create/update category; remove category while protecting/unlinking dependent items as implemented by its API.

```text
GET categories → return category rows
ON category write:
    parse with Zod schema
    authorize module action
    insert/update requested category
ON delete:
    apply repository's item-unlink/delete handling
    delete category record
```

**Minor 4.2 — Menu items and variants** — sub-functions: list/search/manage items; attach category; price/name/description/image; maintain options/variants and their prices; create/update/delete; validate payload.

```text
ON item create/update
    parse item and option data with schema
    persist item fields and category reference
    insert/update associated options/variants
ON item delete
    apply dependent order-history policy in source
    remove/soft-delete item as implemented
```

### Major 5 — Tables and bookings

**Minor 5.1 — Restaurant table management** — sub-functions: list tables; create/update/delete table; store table number/capacity/status; expose availability/occupied/booked state.

```text
GET tables → return table list/status
ON table write:
    validate fields
    insert or update table
ON table delete:
    apply associated booking protection and deletion policy
```

**Minor 5.2 — Create/update booking** — sub-functions: customer/table/date/start/end/advance fields; validate form; persist booking; list with joined table/user labels; update or delete.

```text
ON booking submit
    validate booking fields and time window
    save customer, table, reservation interval, optional advance, status
ON booking list
    join table and booking-user information
    return normalized booking rows
```

**Minor 5.3 — Time-driven booking/table synchronization** — sub-functions: scheduled→booked within 30 minutes; active reservations; grace period up to 2 hours after end for booked no-show; expire and free table; respect cancelled/completed/processing; promote to processing when occupied.

```text
now ← current timestamp
FOR each booking with its table
    IF cancelled/completed THEN preserve terminal state; cancelled frees table
    ELSE IF occupied table and processing booking THEN preserve manual processing
    ELSE IF occupied table AND reservation active AND status booked/scheduled
        set booking processing
    ELSE IF booked no-show past end but within 2h grace
        keep booking/table states
    ELSE IF booked no-show past grace
        set booking expired; set table available
    ELSE IF starts > 30m away
        set booking scheduled
    ELSE IF starts within 30m and table available
        set booking booked; set table booked
    ELSE IF reservation currently active and table available
        set booking booked; set table booked
    ELSE IF reservation end passed
        set booking expired; free table
```

**Minor 5.4 — Manual table check-in/out** — sub-functions: check-in finds eligible nearest booking and marks processing, table occupied; check-out completes processing booking and frees table.

```text
IF mode = check-in
    locate eligible booked/scheduled booking for table
    if found set booking processing
    set table occupied
ELSE IF mode = check-out
    locate processing booking for table; if found set completed
    set table available
```

**Minor 5.5 — Booking cancellation** — sub-functions: confirmation dialog; PATCH status; release table where applicable; refresh list.

```text
ask for cancellation confirmation
IF confirmed THEN update booking status to cancelled
                 synchronize table to available where policy applies
                 refresh booking list
```

### Major 6 — Orders and cart

**Minor 6.1 — Build and edit cart** — sub-functions: browse items by category; choose variants; set quantity; calculate line subtotals and discount; select dine-in/takeaway/drive-thru; select table for dine-in.

```text
cart ← empty
ON add item/variant: add or increment selected cart line
ON quantity change: update line quantity and line total
subtotal ← sum(line quantity × selected price)
total ← apply entered discount using UI's calculation
collect order type and optional table
```

**Minor 6.2 — Load existing table order** — sub-functions: GET by table; reconstruct cart and order metadata; show booking/advance information.

```text
GET orders for selected table
IF order exists THEN load order header and line items into editable cart
ELSE start empty order
```

**Minor 6.3 — Save order** — sub-functions: reject missing table/order type/empty items; create or update order; replace order-item rows transactionally on edit; associate booking/table status; return created/updated identifier.

```text
validate table (when required), order type, and at least one item
BEGIN transaction
IF editing THEN update order header; delete old order item rows
ELSE insert order header
insert each current cart line with item/variant/name/quantity/price
update associated booking/table state as requested
COMMIT; return order ID
ON error: rollback and return error
```

### Major 7 — Invoices and receipts

**Minor 7.1 — Create invoice** — sub-functions: use current order/cart; customer/payment/order details; calculate subtotals, discount, advances and amount due; validate invoice fields; persist invoice and lines.

```text
load order/cart and optional booking advance
validate invoice/customer/payment data
subtotal ← sum current order lines
calculate displayed discount/total/advance/grand-total using app's current form logic
persist invoice header and detail lines
return invoice identifier
```

**Minor 7.2 — Retrieve/update/view invoice** — sub-functions: retrieve invoice with order and lines; populate view/edit; update or delete invoice records; keep history accessible.

```text
invoice ← query invoice by ID with associated order and lines
IF view THEN render read-only invoice
IF edit THEN validate edited fields; persist invoice changes
IF delete THEN apply transaction/invoice dependent delete ordering used by API
```

**Minor 7.3 — Print invoice slip** — sub-functions: format receipt HTML with items and totals; invoke browser print.

```text
build receipt markup from persisted invoice and order lines
include quantities, prices, subtotal, discount, advance, grand total
open print dialog for generated receipt
```

### Major 8 — Employees and salary records

**Minor 8.1 — Employee CRUD/profile** — sub-functions: list/create/edit employee; personal fields; designation and salary form; image/reference fields; detail pages; employee salary/history tabs.

```text
ON employee write
    validate personal, employment, and salary fields
    insert or update employee record
    return employee list/detail
ON detail view
    load employee, latest employment record, salary changes, and profile sections
```

**Minor 8.2 — Employment and salary change history** — sub-functions: append employment records with designation/join/resign metadata; append salary changes; read record history for employee.

```text
ON new employment/salary event
    validate employee and change data
    append dated history entry (rather than silently overwrite prior event)
ON history request
    read employee's events in chosen order
```

**Minor 8.3 — Generate monthly payroll records** — sub-functions: determine each employee's employment interval; create missing employee-month rows through last complete month for active employees or resignation month for resigned employees; use employee base salary; avoid creating existing months.

```text
FOR each employee
    latestEmployment ← newest employment record
    IF none THEN continue
    startMonth ← month(joinedAt)
    endMonth ← month(resignedAt) if resigned
                ELSE month(last month)
    months ← inclusive months(startMonth, endMonth)
    existing ← set of payroll months for employee
    FOR each missing month
        IF salary > 0 THEN insert pending payroll row with basicPay=salary,
                              totalPay=salary and employee snapshot fields
```

**Minor 8.4 — Payroll summaries and payment state** — sub-functions: join employees/latest designation; fetch payrolls; list pending months; sum pending balances; show base salary and current-month salary after bonus/penalty; filter per-employee payroll by status/month; update paid state and paid-at metadata; print salary history.

```text
FOR each employee
    payrolls ← rows for employee
    unpaid ← rows whose status is pending
    previousBalance ← sum(unpaid.totalPay)
    latest ← latest month row
    displayedThisMonth ← baseSalary + latest.bonus - latest.penalty
    return employee summary with unpaid months and status
ON payroll filter(employee,status,from,to)
    return matching payroll rows
ON mark paid
    update state/paid timestamp using the selected payroll action
```

### Major 9 — Financial transactions

**Minor 9.1 — Transaction categories** — sub-functions: list/add/edit/delete named categories; validate.

```text
validate category fields
insert or update selected category
on delete apply dependent-record handling before removal
```

**Minor 9.2 — Record incomes and expenses** — sub-functions: enter amount/date/category/description; create/update/delete records; display lists.

```text
validate transaction direction, amount, date, category and description
persist income or expense record
return refreshed transaction list
```

### Major 10 — Reports/analytics

**Minor 10.1 — Dashboard metric cards and chart** — sub-functions: aggregate transactions for requested reporting interval; compute monthly/yearly income/expenses/profit; show active/total employees; chart.

```text
load reporting interval transactions
income ← sum income rows
expense ← sum expense rows
profit ← income - expense
load active and total employee counts
render summary cards and date-bucketed trends
```

**Minor 10.2 — Transaction report** — sub-functions: retrieve/filter date/transaction rows; display table and detail/hover affordances.

```text
parse report filters
query transactions in filter interval/category/type
return rows and aggregates
render table and chart
```

## Source caveats / confidence

- Payroll is described and coded as salary-record management (monthly generation, bonuses/penalties, pending/paid), not as a jurisdiction-compliant tax/contribution calculator. Salary workflow must not be confused with the payroll framework in folder 13 or the statutory rules in MotorPH project docs.
- README's phrase “secure RBAC” is the project's description. The inventory records the implemented RBAC pages/helpers, but this static pass did not prove every server action enforces permissions correctly.
- Salary summary source treats latest employment-record ID as latest designation and calculates displayed this-month salary from base + bonus − penalty; it is not a vetted financial calculation model. The app includes currency strings/formatting that need independent validation, including decimal precision and negative-value handling.
- Booking state synchronization uses hard-coded thresholds (30-minute pre-start and two-hour no-show grace) and a detailed status flow. These are the repository's own rules only.
- The README mentions Take-away and Drive-thru. UI/API read-through and runtime validation would be needed to verify all order types work equally; no end-to-end run was performed here.
- No dependencies were installed and no test/build command was run.
