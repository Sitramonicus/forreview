# Folder 10 — Employee Payroll with Laravel Framework

**Upstream:** [wasilolly/Employee-Payroll-with-laravel-framework](https://github.com/wasilolly/Employee-Payroll-with-laravel-framework)  
**Snapshot:** `master`, `b9d3d39882da4ad197a54afb30d9e9119be2ddbf`; GitHub metadata checked 2026-11-04 (41 stars, 33 forks).  
**Local source:** `repo_review/full_repo/testingester-main/10/-/Employee-Payroll-with-laravel-framework/`  
**Evidence boundary:** static review of README, web routes, controllers, Eloquent models, migrations and Blade views. This is a feature map, not a claim that every path was exercised. Repository is an older Laravel app; no current jurisdiction/statutory policy is established.

## Major-feature map

| Major | Feature | Minor features and sub-functions (source-grounded) |
|---|---|---|
| 1 | Web access, registration and account authentication | Landing page; register account; validate name/email/password confirmation; hash registered password; login/logout; password reset email/token; authenticated route gate. |
| 2 | Dashboard | Load counts for employees, roles, departments; show a small employee sample and recent payroll sample. |
| 3 | Organization setup: departments and roles | Department list/pagination, create/show/edit/update/delete; department slug; roles per department; role name, salary and department association; role list/detail/edit/delete. |
| 4 | Employee record administration | Paginated employee listing; create with identity/contact/address, full-time flag and role; employee detail/edit/update; soft-delete; deleted bin; restore; permanent delete and associated-payroll handling. |
| 5 | Payroll entry and gross-pay calculation | Select employee; enter hours, rate, overtime flag; create/update payroll; calculate gross via employee employment type and role salary; list employee payrolls; edit and soft-delete payroll. |
| 6 | Payroll recycle-bin lifecycle | List soft-deleted payroll; restore; permanent removal. |
| 7 | PDF output | Generate all-payroll PDF for an employee; generate single-payroll PDF. |

## Feature pseudocode


## High-level pseudocode — each major feature

### Major 1 — Web access and authentication

```text
PROCEDURE MAJOR_1_WEB_ACCESS_AND_AUTHENTICATION
    route to public entry or auth controller
    call registration/login/logout/reset flow required by request
    require an authenticated session for payroll administration
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_1.1  // Show public landing page
        CALL MINOR_1.2  // Register account
        CALL MINOR_1.3  // Login and logout
        CALL MINOR_1.4  // Password recovery
END PROCEDURE
```

### Major 2 — Dashboard

```text
PROCEDURE MAJOR_2_DASHBOARD
    require authenticated user
    load small employee and payroll previews
    count employees, roles and departments
    render dashboard
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_2.1  // Dashboard summary
END PROCEDURE
```

### Major 3 — Departments and roles

```text
PROCEDURE MAJOR_3_DEPARTMENTS_AND_ROLES
    create departments before dependent roles
    for requested department action call minor 3.1
    for requested role action call minor 3.2
    preserve relationship deletion behavior recorded in source
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_3.1  // Department CRUD
        CALL MINOR_3.2  // Role CRUD and compensation metadata
END PROCEDURE
```

### Major 4 — Employee administration

```text
PROCEDURE MAJOR_4_EMPLOYEE_ADMINISTRATION
    load role options and selected employee
    dispatch list/create/update/detail/delete/restore/permanent-delete action
    persist through Eloquent and redirect with message
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_4.1  // List and view
        CALL MINOR_4.2  // Create employee
        CALL MINOR_4.3  // Update employee
        CALL MINOR_4.4  // Soft-delete, restore, permanent delete
END PROCEDURE
```

### Major 5 — Payroll entry and gross pay

```text
PROCEDURE MAJOR_5_PAYROLL_ENTRY_AND_GROSS_PAY
    select employee and collect hours, rate and overtime flag
    validate inputs
    call gross-pay branch logic
    save payroll row and show employee payroll history
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_5.1  // Open employee payroll history/create form
        CALL MINOR_5.2  // Create payroll row
        CALL MINOR_5.3  // Calculate gross pay (actual branch structure)
        CALL MINOR_5.4  // Edit payroll
        CALL MINOR_5.5  // Delete payroll
END PROCEDURE
```

### Major 6 — Payroll recycle bin

```text
PROCEDURE MAJOR_6_PAYROLL_RECYCLE_BIN
    load deleted payroll rows
    dispatch restore or force-delete action
    return to configured destination
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_6.1  // Browse deleted payroll
        CALL MINOR_6.2  // Restore or permanently remove
END PROCEDURE
```

### Major 7 — PDF output

```text
PROCEDURE MAJOR_7_PDF_OUTPUT
    select employee payroll history or one payroll record
    load corresponding model/view data
    render selected PDF template
    stream generated document
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_7.1  // Employee payroll PDF
        CALL MINOR_7.2  // Single payroll PDF
END PROCEDURE
```

### Major 1 — Web access and authentication

**Minor 1.1 — Show public landing page** — sub-functions: route `/`; render welcome view.

```text
ON GET /
    render welcome page
```

**Minor 1.2 — Register account** — sub-functions: collect name/email/password/confirmation; validate required fields, email uniqueness, minimum password length and confirmation; store hashed password; redirect to home.

```text
ON registration submit
    validate name, email, unique email, password length >= configured minimum,
             and confirmation matches
    IF invalid THEN return validation errors
    ELSE create user with bcrypt(password)
         sign user in using framework registration flow
         redirect to /home
```

**Minor 1.3 — Login and logout** — sub-functions: use Laravel Auth routes; require authentication for protected controllers; route authenticated user to home; terminate session on logout.

```text
ON login submit
    pass credentials to Laravel authentication
    IF credentials accepted THEN establish session and redirect to intended/home page
    ELSE return framework login validation error
ON logout
    invalidate authenticated session using framework Auth route
```

**Minor 1.4 — Password recovery** — sub-functions: request reset link, deliver mail, validate token, accept replacement password, finish reset through framework traits. Exact mail delivery requires environment configuration.

```text
ON forgot-password request
    ask framework password broker to issue and email reset token
ON reset submit(token, newPassword)
    validate token and new credentials
    IF token valid THEN save framework-hashed new password and invalidate token
    ELSE show reset error
```

### Major 2 — Dashboard

**Minor 2.1 — Dashboard summary** — sub-functions: fetch up to four employees and payroll rows; count employees, roles and departments; render cards/lists.

```text
REQUIRE authenticated user
employeesPreview ← first 4 employee rows
payrollPreview ← first 4 payroll rows
counts ← total employees, roles, departments
render home dashboard with previews and counts
```

### Major 3 — Departments and roles

**Minor 3.1 — Department CRUD** — sub-functions: paginate; show create/edit/detail forms; require name; derive slug; persist updates; hard-delete related roles and department through the controller path.

```text
ON department create/update
    validate name is present
    set name and slug(name)
    save department
    flash success; redirect to department list
ON department detail
    find by slug; render detail
ON department delete
    load department
    for each related role: soft-delete role
    soft-delete department
    redirect with success message
```

**Minor 3.2 — Role CRUD and compensation metadata** — sub-functions: list/paginate; require an existing department before role creation; collect role name, salary and department; derive slug; show/edit; delete role and related employees per controller logic.

```text
ON role create form
    IF no department exists THEN flash prerequisite message; return
    ELSE show department choices
ON role create/update
    validate name, salary field and department ID
    save role name, salary, department ID, derived slug
ON role detail
    find role by slug; render
ON role delete
    find role
    force-delete each related employee as implemented
    delete role
    redirect
```

### Major 4 — Employee administration

**Minor 4.1 — List and view** — sub-functions: paginate employee records; find employee; load related role/payroll relation for view.

```text
REQUIRE authenticated user
ON list: fetch employees page (5 rows/page); render
ON detail(id): find employee or fail; render employee
```

**Minor 4.2 — Create employee** — sub-functions: ensure at least one role exists; validate name/email/address/full-time/role fields; generate slug; create employee; create an associated initially blank payroll row; redirect.

```text
ON create form
    IF no roles THEN show prerequisite message; return
    ELSE show roles
ON submit
    validate required employee fields and email format
    create employee with slug(name), contact/address, fullTime, roleId
    create associated Payroll row with employeeId
    save and redirect to employee list
```

**Minor 4.3 — Update employee** — sub-functions: load employee and role options; validate; update all editable identity/contact/employment fields; recompute slug; save.

```text
employee ← find by ID or fail
validate submitted fields
copy allowed fields to employee; set slug from name
save; flash status; redirect to employee list
```

**Minor 4.4 — Soft-delete, restore, permanent delete** — sub-functions: delete employee into soft-deleted state; browse deleted bin; restore; permanently delete related payroll records and employee.

```text
ON delete(id): soft-delete employee
ON bin: list only soft-deleted employees
ON restore(id): find including deleted; restore; redirect
ON permanent delete(id):
    find employee including deleted
    iterate related payrolls and call delete as implemented
    force-delete employee
```

### Major 5 — Payroll entry and gross pay

**Minor 5.1 — Open employee payroll history/create form** — sub-functions: load employee by ID; render payroll form or employee payroll page.

```text
ON payroll page(employeeId)
    employee ← findOrFail(employeeId)
    render payroll history view for employee
ON new payroll(employeeId)
    employee ← findOrFail(employeeId)
    render create form
```

**Minor 5.2 — Create payroll row** — sub-functions: require hours/rate/overtime boolean; associate row to employee; invoke model gross-pay method; save and redirect.

```text
validate hours, rate, overtime flag
payroll ← create(employeeId, hours, rate, overtime)
payroll.gross ← payroll.grossPay()
save; redirect to that employee's payroll page
```

**Minor 5.3 — Calculate gross pay (actual branch structure)** — sub-functions: full-time regular salary; full-time with overtime; non-full-time/overtime branch; fallback zero. This is only the model's implemented gross calculation, not a complete payslip/net-pay algorithm.

```text
IF employee.fullTime AND NOT payroll.overtime
    gross ← employee.role.salary
ELSE IF employee.fullTime AND payroll.overtime
    gross ← payroll.hours * payroll.rate + employee.role.salary
ELSE IF payroll.overtime OR NOT payroll.fullTime
    gross ← payroll.hours * payroll.rate
ELSE
    gross ← 0
RETURN gross
```

**Minor 5.4 — Edit payroll** — sub-functions: load row; validate inputs; update hours/rate/overtime; recompute gross; persist; redirect to employee payroll list.

```text
payroll ← findOrFail(id)
validate hours, rate, overtime flag
update inputs
payroll.gross ← payroll.grossPay()
save; redirect to payrolls for payroll.employeeId
```

**Minor 5.5 — Delete payroll** — sub-functions: soft-delete selected row; return to prior page.

```text
payroll ← findOrFail(id)
soft-delete payroll
redirect back with success message
```

### Major 6 — Payroll recycle bin

**Minor 6.1 — Browse deleted payroll** — sub-functions: query `onlyTrashed`; render bin.

```text
rows ← payrolls where deletedAt is not null
render payroll bin
```

**Minor 6.2 — Restore or permanently remove** — sub-functions: load including trashed; restore; or force-delete.

```text
row ← find payroll including deleted by id
IF action = restore THEN restore row
ELSE IF action = permanent-delete THEN force-delete row
redirect as the controller specifies
```

### Major 7 — PDF output

**Minor 7.1 — Employee payroll PDF** — sub-functions: load employee; render all-payroll Blade template; stream PDF.

```text
employee ← find employee by id
pdf ← render all-payroll PDF template(employee)
stream PDF response
```

**Minor 7.2 — Single payroll PDF** — sub-functions: load payroll; render single-payroll template; stream PDF.

```text
payroll ← find payroll by id
pdf ← render single-payroll PDF template(payroll)
stream PDF response
```

## Source caveats / confidence

- Payroll captures hours, rate and a boolean overtime flag, but the inspected model exposes **gross only**. No statutory withholding, net-pay engine, pay-period lifecycle or approval workflow was found in these paths.
- Payroll routes/controllers use authentication middleware. The README advertises granting/revoking admin privileges and deleting threads/users, but the inspected source models `Role` as a department-linked job role (with salary), not an access-control role, and no thread model/route was found. The reviewed controllers do not visibly enforce a distinct admin role at each action; treat those README statements as advertised capabilities, not proof of a complete authorization matrix.
- A potential defect is preserved above: the third gross-pay branch tests `$this->full_time` on the payroll model rather than the associated employee, and the role update path shown does not call `save()` before redirect. Do not silently correct these in “what it does” pseudocode.
- Destructive relationship behavior differs: department deletion hard-deletes its roles and department; role deletion force-deletes related employees; employee permanent deletion first soft-deletes related payroll rows, then force-deletes the employee. These are not safe to adapt without reviewing FK/cascade expectations.
- No project tests/build were run. Source is an older Laravel version; installation should be isolated and dependency/security compatibility reviewed before executing it.
