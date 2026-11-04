# Folder 14 — Frappe HRMS / Frappe HR

**Upstream:** [frappe/hrms](https://github.com/frappe/hrms)  
**Snapshot:** `develop`, `159548fa676d51e0bc7ce85c6b6516d3d7b7316a`; GitHub metadata checked 2026-11-04 (9,017 stars, 2,866 forks; GPL-3.0).  
**Local source:** `repo_review/full_repo/testingester-main/14/-/hrms/`  
**Evidence boundary:** upstream README and current `develop` source tree were indexed. This is a large, actively changing Frappe app, installed on the Frappe Framework and normally integrated with ERPNext; it is not a single standalone payroll executable. Inventory aggregates the source's Doctypes, reports, controllers, hooks, frontend/mobile app and named modules. Pseudocode is domain-level workflow pseudocode, not a line-by-line reimplementation of all controllers.

## 1. Top-level capability inventory

The checked snapshot contains **164 DocType JSON definitions, 449 DocType Python files and 68 report Python files** (simple file counts; they are not test or feature counts). The app organizes work in HR, HR Setup, Recruitment, Leaves, Shift & Attendance, Payroll, Tax & Benefits, Expenses, Performance, Tenure, and regional/configuration packages.

| Major | Feature area | Minor features / sub-functions and representative source entities |
|---:|---|---|
| 1 | People, organization and employee records | Employee profile, department, designation, grade, employment type, branch/location, reporting lines, skills, identity-document type, employee property/history, organization chart, employee info reports. |
| 2 | Employee lifecycle: onboarding, job change, separation | Onboarding templates, boarding activities, onboarding; appointment letters/templates; promotions, transfers; separation templates, full-and-final statement/assets/outstanding items; exit interview. |
| 3 | Recruitment | Staffing plan and requisition; job-opening template/opening; job-applicant/source; public job application web form; interview/type/interviewer/round/feedback; job offer and terms/templates/components; appointment letter; recruitment analytics. |
| 4 | Leave and leave balances | Leave types/periods/policies/assignments; allocations; applications; approval and cancellation; leave ledger and adjustments; block lists/holidays; compensatory leave; encashment; balance reports. |
| 5 | Shifts, attendance and time | Shift type/schedule/assignment; shift requests and assignment tool; employee check-ins (including check-in/out and configured geolocation capture); attendance generation/requests; holiday lists; overtime; attendance reports and tool. |
| 6 | Payroll and salary configuration | Salary components/accounts; salary structures and effective-dated assignments; payroll periods/settings; payroll entries; salary slips; additions/deductions; incentives; arrears; timesheet/attendance/leave/loan inputs; payment modes and accounting links. |
| 7 | Tax, employee benefits and final compensation | Income-tax slabs and other charges; tax exemption category/subcategory/declaration/proofs; employee other income; benefit application/claim/ledger; salary withholding/cycles; gratuity rules/slabs; retention bonus; payroll correction. Regional/localization code provides country-specific support. |
| 8 | Expenses, advances and travel | Employee advance; expense claim/types/details/taxes/accounts/advances; claim approval and payment state; travel request/itinerary/costing/purpose; vehicle log/service/expense reporting. |
| 9 | Performance, goals and feedback | Appraisal cycle/template/appraisal; KRA/goals and appraisal goals; employee/self feedback; feedback criteria/ratings; appraisal overview and charts. |
| 10 | Skills, learning and training | Skills/expected skill sets/designation skills; employee skills/maps/assessment; training programs/events/attendees/results/feedback. |
| 11 | Tenure and workforce management | Employee referrals, grievances/types, staffing plan, employee grade/type, promotion/transfer history; attrition/lifecycle metrics. |
| 12 | Employee self-service and mobile/PWA | Employee portal/profile; salary slip views; leave/compensatory-leave request; attendance and shift request/check-in; expense/advance forms; notifications; password/profile settings; installable PWA/mobile experience. |
| 13 | Reports, dashboards and analytics | Reports for employee information/analytics, attendance/shift/leave, leave balance/ledger, payroll register/tax/payment/CTC/accrual, expenses/advances, recruitment, performance, fleet; number cards/charts/workspaces. |
| 14 | Workflow, notifications, permissions and integrations | Frappe DocType lifecycle hooks, role/permission checks, approval workflows, email/desktop/mobile notifications, REST/API/controllers, ERPNext accounting/payments, regional setup, migrations/patches and configuration. |

## 2. Pseudocode by major and minor feature


## High-level pseudocode — each major feature

### Major 1 — Employee and organization records

```text
PROCEDURE MAJOR_1_EMPLOYEE_AND_ORGANIZATION_RECORDS
    authenticate and validate employee/organization references
    save employee and organization relationships
    maintain dated skill/property/history and refresh linked views
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_1.1  // Create/update employee master
        CALL MINOR_1.2  // Maintain organization structure and assignments
        CALL MINOR_1.3  // Skills and employee properties
END PROCEDURE
```

### Major 2 — Employee lifecycle

```text
PROCEDURE MAJOR_2_EMPLOYEE_LIFECYCLE
    choose onboarding/separation template or employee change
    create and route lifecycle tasks/approval
    record effective-dated change and supporting documents
    close lifecycle process when required steps finish
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_2.1  // Onboard employee
        CALL MINOR_2.2  // Appointment letters
        CALL MINOR_2.3  // Promotion and transfer
        CALL MINOR_2.4  // Separation and full-and-final
END PROCEDURE
```

### Major 3 — Recruitment

```text
PROCEDURE MAJOR_3_RECRUITMENT
    create and approve vacancy
    receive/link applicant
    schedule interview and collect feedback
    issue offer and convert accepted applicant to employee
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_3.1  // Plan and open a vacancy
        CALL MINOR_3.2  // Receive and manage applicants
        CALL MINOR_3.3  // Interview and feedback
        CALL MINOR_3.4  // Offer and appointment
END PROCEDURE
```

### Major 4 — Leave management

```text
PROCEDURE MAJOR_4_LEAVE_MANAGEMENT
    resolve leave policy, period, holidays and balance
    submit and route application
    approve/reject/cancel with ledger effects
    report balance and history
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_4.1  // Configure leave types, periods and policies
        CALL MINOR_4.2  // Allocate and adjust leave
        CALL MINOR_4.3  // Apply, review and cancel leave
        CALL MINOR_4.4  // Compensatory leave and encashment
END PROCEDURE
```

### Major 5 — Shift and attendance

```text
PROCEDURE MAJOR_5_SHIFT_AND_ATTENDANCE
    assign effective shift schedule
    capture check-ins/requests and validate inputs
    produce authorized attendance/overtime records
    report attendance by period
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_5.1  // Configure shifts and assign schedules
        CALL MINOR_5.2  // Capture employee check-in/out
        CALL MINOR_5.3  // Produce attendance
        CALL MINOR_5.4  // Overtime
        CALL MINOR_5.5  // Attendance reports
END PROCEDURE
```

### Major 6 — Payroll and salary processing

```text
PROCEDURE MAJOR_6_PAYROLL_AND_SALARY_PROCESSING
    configure salary components/structures and effective assignments
    select eligible employees for payroll period
    calculate draft slips with configured inputs
    submit/pay/account and expose approved slip
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_6.1  // Configure salary components and structures
        CALL MINOR_6.2  // Configure payroll period/settings and payroll entry
        CALL MINOR_6.3  // Calculate salary slip
        CALL MINOR_6.4  // Additional salary, incentives and arrears
        CALL MINOR_6.5  // Payment and accounting integration
        CALL MINOR_6.6  // Correct payroll
END PROCEDURE
```

### Major 7 — Tax, benefits and final compensation

```text
PROCEDURE MAJOR_7_TAX_BENEFITS_AND_FINAL_COMPENSATION
    resolve configured regional tax/benefit/compensation rules
    collect declarations/proofs/claims
    review and approve inputs
    calculate/link applicable tax, benefit or final settlement records
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_7.1  // Configure tax and exemption declarations
        CALL MINOR_7.2  // Employee benefits and claims
        CALL MINOR_7.3  // Withholding, gratuity, retention and other compensation
        CALL MINOR_7.4  // Regional/localized rules
END PROCEDURE
```

### Major 8 — Expenses, advances and travel

```text
PROCEDURE MAJOR_8_EXPENSES_ADVANCES_AND_TRAVEL
    submit advance, expense or travel request
    route approval and collect receipts/costs
    record payment/accounting and balance changes
    report costs/fleet activity
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_8.1  // Employee advance
        CALL MINOR_8.2  // Expense claim
        CALL MINOR_8.3  // Travel request and costing
        CALL MINOR_8.4  // Fleet records
END PROCEDURE
```

### Major 9 — Performance and feedback

```text
PROCEDURE MAJOR_9_PERFORMANCE_AND_FEEDBACK
    configure appraisal cycle/goals/KRAs
    collect self/reviewer feedback
    review and save appraisal outcome
    report cycle results
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_9.1  // Configure appraisal cycle/templates/KRAs
        CALL MINOR_9.2  // Set goals and collect feedback
        CALL MINOR_9.3  // Appraise and report
END PROCEDURE
```

### Major 10 — Skills and training

```text
PROCEDURE MAJOR_10_SKILLS_AND_TRAINING
    maintain skill catalogue and expected skill needs
    assess employee skill
    schedule training and record attendance/result
    update skill/training history
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_10.1  // Maintain skill profile and assessment
        CALL MINOR_10.2  // Plan and record training
END PROCEDURE
```

### Major 11 — Tenure, workforce and employee relations

```text
PROCEDURE MAJOR_11_TENURE_WORKFORCE_AND_EMPLOYEE_RELATIONS
    record referral/grievance/workforce plan
    apply configured confidentiality and review flow
    aggregate lifecycle and headcount data for authorized views
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_11.1  // Referral, grievance and workforce planning
END PROCEDURE
```

### Major 12 — Employee self-service/mobile/PWA

```text
PROCEDURE MAJOR_12_EMPLOYEE_SELF_SERVICE_MOBILE_PWA
    resolve signed-in user to employee record
    serve only permitted profile and self-service forms
    route employee requests/check-ins through server authorization
    notify user of status changes
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_12.1  // Employee login and profile
        CALL MINOR_12.2  // Employee self-service requests
        CALL MINOR_12.3  // Push and workflow notifications
END PROCEDURE
```

### Major 13 — Reports and analytics

```text
PROCEDURE MAJOR_13_REPORTS_AND_ANALYTICS
    authorize report filters
    query module-specific employee/payroll/leave/attendance/expense data
    aggregate according to report definition
    return rows/charts/cards
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_13.1  // HR/leave/attendance reports
        CALL MINOR_13.2  // Payroll/tax/benefit reports
        CALL MINOR_13.3  // Recruitment/performance/expense/fleet analytics
END PROCEDURE
```

### Major 14 — Workflow, security and integrations

```text
PROCEDURE MAJOR_14_WORKFLOW_SECURITY_AND_INTEGRATIONS
    authenticate and authorize action
    validate document state transition and domain inputs
    run configured workflow/hooks/integration
    persist audit/result and notify relevant user
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_14.1  // Document lifecycle and approvals
        CALL MINOR_14.2  // API/controller integrations
        CALL MINOR_14.3  // ERPNext accounting/payroll integration
        CALL MINOR_14.4  // Regional setup, migrations and patches
END PROCEDURE
```

### Major 1 — Employee and organization records

**Minor 1.1 — Create/update employee master** — sub-functions: validate required identity and company; set department/designation/grade/type/manager/location; validate employment dates and unique identity; save; trigger linked profile/user/lifecycle hooks.

```text
ON employee save
    validate required employee and company fields
    validate linked department/designation/type/manager references
    validate date and identifier constraints
    apply configured employee lifecycle hooks
    save employee record and permitted related data
    refresh employee profile/organization reporting views
```

**Minor 1.2 — Maintain organization structure and assignments** — sub-functions: create department/designation/grade/type; link employees and reporting manager; render organization chart; query employee directory/reports.

```text
validate organization record
save department/designation/grade/type
when employee assignment changes:
    validate selected organization references
    save new assignment and its effective date/history if configured
build chart/report from employee-manager and department links
```

**Minor 1.3 — Skills and employee properties** — sub-functions: define skill; record employee skill and proficiency/assessment; maintain employee properties and history; query for role/analytics use.

```text
validate skill or property definition
attach dated skill/property record to employee
on assessment, store outcome and assessor/context
return current or historical employee capability data
```

### Major 2 — Employee lifecycle

**Minor 2.1 — Onboard employee** — sub-functions: select onboarding template; create onboarding record; instantiate activities; assign responsible people/due dates; complete/check activities; track status and notifications.

```text
create onboarding from employee and selected template
for each template activity:
    create employee boarding activity with owner and due date
notify responsible users
on activity update:
    validate completion/status
    save evidence/status and update onboarding progress
when required steps are complete:
    mark onboarding complete
```

**Minor 2.2 — Appointment letters** — sub-functions: select letter template/content; merge employee/job/compensation fields; generate and attach/print document; retain record.

```text
select employee, appointment template and effective terms
resolve template fields from employee/offer data
render letter document
save/attach generated letter to employee lifecycle record
```

**Minor 2.3 — Promotion and transfer** — sub-functions: select employee, new designation/department/manager/terms and effective date; validate; record change; update current employee assignment; retain history.

```text
validate effective-dated employee change
append promotion or transfer event
update employee's current assignment at effective transition
keep prior assignment/history available
notify relevant parties if configured
```

**Minor 2.4 — Separation and full-and-final** — sub-functions: initiate employee separation from template; record reason/date; run configured exit process; gather assets/outstanding items; build final statement; schedule exit interview; complete separation.

```text
create separation record and selected checklist
collect required handover/asset/outstanding entries
compute final statement using configured payroll/leave/accounting rules
route required approvals
record exit interview and assets return
on authorized completion, set employee separated and close process
```

### Major 3 — Recruitment

**Minor 3.1 — Plan and open a vacancy** — sub-functions: staffing plan and positions; job requisition; review/approval; create job opening from template; publish vacancy.

```text
create staffing plan/requisition with role, headcount and dates
validate budget/approval requirements configured in site
on approval create or activate job opening
publish on selected internal/public channel
```

**Minor 3.2 — Receive and manage applicants** — sub-functions: public job application form; collect applicant/contact/resume/source; create applicant; associate opening; filter/status pipeline; avoid duplicate applications as configured.

```text
validate application and required attachments
create applicant and application linked to job opening/source
set initial pipeline status
notify recruitment owner
```

**Minor 3.3 — Interview and feedback** — sub-functions: schedule interview/type/round; assign interviewer; record attendance and feedback; update applicant stage.

```text
select applicant and opening
create interview with time, type, round and interviewer(s)
notify participants
after interview collect feedback and ratings
update applicant pipeline status according to authorized workflow
```

**Minor 3.4 — Offer and appointment** — sub-functions: prepare offer from template and components/terms; review/approve; send; record response; convert accepted candidate to employee; create appointment letter.

```text
create job offer from approved role and candidate terms
validate offer components and approval route
send offer and record accept/reject/expiry response
IF accepted THEN create employee/onboarding records and appointment letter
```

### Major 4 — Leave management

**Minor 4.1 — Configure leave types, periods and policies** — sub-functions: define leave type; leave period; policy details/entitlement; assign policy to employees; create holiday list/block list; effective dates.

```text
validate leave configuration and date span
save leave type and period
create policy entitlements and eligibility conditions
assign policy to employee/group for effective dates
attach holiday/block-list rules to applicable group
```

**Minor 4.2 — Allocate and adjust leave** — sub-functions: allocate leave; adjust credits; generate leave ledger entries; compute available/used/expired balances; report balance.

```text
identify employee, leave type and applicable period
calculate allocation from configured policy inputs
record allocation/adjustment in leave ledger
sum applicable credits and debits as of requested date
return balance and ledger detail
```

**Minor 4.3 — Apply, review and cancel leave** — sub-functions: employee requests dates/type/partial days/reason; validate balance/calendar/overlap/eligibility; route to leave approver; approve/reject; post ledger effect; cancel and reverse according to rules.

```text
validate employee, leave type, date range, overlap and policy rules
calculate requested day units using configured holidays/half-day logic
create pending leave application
route to configured approver(s)
ON approve: revalidate eligibility; change status; create ledger debit
ON reject: set rejected status without debit
ON cancel: validate cancellability; reverse ledger effect if previously posted
```

**Minor 4.4 — Compensatory leave and encashment** — sub-functions: create comp-leave request from eligible attendance/work; approve; credit leave; encash eligible leave; record payment/status.

```text
validate qualifying work/earned-leave schedule
create compensatory request
route for approval
on approval add appropriate ledger credit
for encashment, validate policy/available balance and approval
create encashment record, update ledger and payment state
```

### Major 5 — Shift and attendance

**Minor 5.1 — Configure shifts and assign schedules** — sub-functions: shift type/time/holiday/rest settings; recurring shift schedule; employee assignment; bulk assignment tool; shift request/change approval.

```text
validate shift times and effective dates
create or update shift type/schedule
assign schedule to employee(s) for period
on employee shift request:
    validate request and overlap
    route to authorized reviewer
    on approval update effective assignment
```

**Minor 5.2 — Capture employee check-in/out** — sub-functions: receive check-in event; attach timestamp and optional geolocation/device; associate employee and shift; allow configured correction request; preserve event trail.

```text
authenticate employee context
capture check-in/check-out timestamp and configured location metadata
validate event against allowed check-in policy and sequence
save check-in event
if correction requested, create attendance request for review
```

**Minor 5.3 — Produce attendance** — sub-functions: turn check-ins into attendance record; apply assigned shift/holiday; mark status/late/early/half-day under configured settings; prevent duplicate attendance; attendance request workflow.

```text
load employee check-ins, shift assignment and holiday list for date
apply site attendance-generation rules
validate duplicate/overlap constraints
create or update attendance record and status
route requested correction for approval; write authorized final attendance
```

**Minor 5.4 — Overtime** — sub-functions: define overtime type/components; record overtime details; connect approved overtime with payroll component/slip.

```text
record overtime interval/type for employee
validate date and approval requirements
on approval map hours/value to configured salary component
make approved overtime input available to salary-slip calculation
```

**Minor 5.5 — Attendance reports** — sub-functions: daily/monthly sheet, shift attendance, holiday work, attendance-count charts; filters by employee/date/shift.

```text
validate report filters
query attendance/check-in/shift data for period
apply report aggregation and configured status definitions
return rows and totals
```

### Major 6 — Payroll and salary processing

**Minor 6.1 — Configure salary components and structures** — sub-functions: define earning/deduction component; formula/condition/account mapping; create salary structure; assign to employee with effective dates; bulk assignment; validate overlap.

```text
validate component type, formula/settings and account mapping
save salary component
compose ordered structure from components
create employee structure assignment with effective interval
reject or resolve conflicting assignments per system constraints
```

**Minor 6.2 — Configure payroll period/settings and payroll entry** — sub-functions: configure period/frequency/date/accounting settings; choose employees/filters; create payroll entry; calculate employee detail rows; submit/process/cancel lifecycle.

```text
validate payroll period, company and payment settings
select eligible employees with effective salary structure
create payroll entry for period and employees
for each employee calculate draft salary slip
show per-employee success/errors
on authorized submit, finalize entry/slips and downstream accounting/payment documents
```

**Minor 6.3 — Calculate salary slip** — sub-functions: resolve effective structure and salary basis; consider configured attendance, leave, holidays, timesheet, timesheet salary, salary components, additions/deductions, loans and benefit inputs; compute earnings/deductions/net; create detailed slip and print format.

```text
load employee, effective salary structure and payroll period
load configured attendance/leave/timesheet and approved pay inputs
apply salary structure component formulas and configured tax/benefit/loan inputs
calculate earnings, deductions and net under the configured currency/rounding rules
create draft salary slip with component breakdown and source references
validate totals; submit only through authorized payroll workflow
```

**Minor 6.4 — Additional salary, incentives and arrears** — sub-functions: create one-time/recurring additional earning or deduction; incentive; arrear; effective date/payroll inclusion; cancel/reverse.

```text
validate employee, component, amount, date and recurrence
create additional salary/incentive/arrear record
include it in eligible salary slip according to date and settings
on cancellation record reversal/stop future inclusion as permitted
```

**Minor 6.5 — Payment and accounting integration** — sub-functions: payment mode/bank remittance; salary payments; accounting posting/ERPNext link; payment status; salary slip distribution.

```text
select submitted payroll/slips and configured payment mode
validate payable balances and bank/account mapping
create payment/remittance or accounting entries through ERPNext integration
record payment status/reference
expose finalized salary slip to authorized employee
```

**Minor 6.6 — Correct payroll** — sub-functions: payroll correction record; identify affected employee/slip; calculate correction/difference; approval; retain original and correction history.

```text
create correction referencing affected payroll result
validate reason and correction scope
recalculate only selected inputs under configured policy
calculate difference from prior submitted result
route for authorization and persist an auditable adjustment
```

### Major 7 — Tax, benefits and final compensation

**Minor 7.1 — Configure tax and exemption declarations** — sub-functions: tax slabs/other charges; employee tax declaration/category/subcategory; proof submissions; review proof; include accepted tax data in calculation.

```text
configure effective tax slab and charges for applicable context
employee submits declaration and supporting proof
validate category, dates and mandatory proof
route for review; record accept/reject status
expose approved tax data to payroll calculation for applicable period
```

**Minor 7.2 — Employee benefits and claims** — sub-functions: benefit details/application; approval; claim; ledger; payment/payroll/accounting mapping.

```text
employee applies for configured benefit
validate eligibility and required evidence
route to configured approver
on approval create benefit entitlement/ledger entry
on claim validate available amount and receipts
record claim and payout/payroll treatment as configured
```

**Minor 7.3 — Withholding, gratuity, retention and other compensation** — sub-functions: salary withholding/cycle; gratuity rule/slabs/applicable components; retention bonus; other income; apply effective data in payroll/final settlement.

```text
load applicable rule/configuration and employee record
validate effective date and qualifying components
calculate or record withholding/gratuity/retention/other income using configured formula
create linked payroll/final-settlement record and audit details
```

**Minor 7.4 — Regional/localized rules** — sub-functions: country-specific setup/utilities/fixtures; regional fields and calculation hooks; maintain local tax/benefit setup.

```text
resolve employee/company jurisdiction and regional settings
load region-specific configuration and hooks
execute only configured regional calculation path
record rule/config version and calculation details
```

**Minor 7.5 — Employee health-insurance records** — sub-functions: link health-insurance information/coverage records to an employee; update the employee's recorded plan details; retrieve the linked record in employee workflows. The Doctype indicates employee health-insurance record management; the inventory does not infer insurer adjudication or claims processing.

```text
select employee and health-insurance record
validate linked employee and record fields
save or update the employee-linked insurance record
retrieve it only within the authorized employee/HR context
```

### Major 8 — Expenses, advances and travel

**Minor 8.1 — Employee advance** — sub-functions: request/create; approval; disbursement; balance/status; repayment/recovery through payroll where configured.

```text
validate employee, amount, purpose and advance rules
create advance request and route for approval
on approval record disbursement and outstanding balance
apply repayment entries through configured payment/payroll integration
```

**Minor 8.2 — Expense claim** — sub-functions: claim details/receipts/taxes/currency; apply advance; submit; approve/reject; partial/unpaid/paid status; accounting posting.

```text
validate expense items, dates, currency, receipt and category
calculate claim total and applicable tax
apply selected advance balance
submit to configured approver workflow
on approval create payable/accounting record
record partial/full payment and update claim/advance balances
```

**Minor 8.3 — Travel request and costing** — sub-functions: request purpose/dates; itinerary; costing; approval; link expense claim/advance as configured.

```text
create travel request with purpose and itinerary
validate travel policy and cost estimates
route for approval
on approval expose approved trip for advances and subsequent expenses
```

**Minor 8.4 — Fleet records** — sub-functions: vehicle log; service and service items; operating costs; vehicle expense report.

```text
record vehicle usage/service and cost details
validate vehicle/date/mileage fields
save history and link expense transactions
query service/cost history for fleet report
```

### Major 9 — Performance and feedback

**Minor 9.1 — Configure appraisal cycle/templates/KRAs** — sub-functions: define cycle dates; template goals/KRAs; participant selection; launch cycle.

```text
create appraisal cycle and template
select employee population, goals and KRA weights/criteria
validate dates and template links
launch cycle and notify participants/reviewers
```

**Minor 9.2 — Set goals and collect feedback** — sub-functions: create goals; assign owner/period; employee self-evaluation; manager/peer feedback; criteria and ratings.

```text
create employee goal with period and linked KRA
collect self and reviewer feedback using configured criteria
validate rating/comment fields and submission state
save feedback and workflow history
```

**Minor 9.3 — Appraise and report** — sub-functions: consolidate ratings; reviewer decision; appraisal status; appraisal-overview report/chart.

```text
load goal progress and submitted feedback
apply configured appraisal aggregation
route appraisal for final review
store outcome and status
aggregate cycle results for authorized report/chart
```

### Major 10 — Skills and training

**Minor 10.1 — Maintain skill profile and assessment** — sub-functions: skill catalogue; expected role skill set; employee skill mapping; assessments and outcomes.

```text
select employee and skill
record level/evidence/assessment result
compare employee profile with expected designation skills
return skills gap view
```

**Minor 10.2 — Plan and record training** — sub-functions: training program/event; enrolled employees; attendance/feedback/results; training history.

```text
create training program and event
assign target employees and schedule
record participation, feedback and result
update employee training history and analytics
```

### Major 11 — Tenure, workforce and employee relations

**Minor 11.1 — Referral, grievance and workforce planning** — sub-functions: employee referral; grievance/type and case details; staffing plan; hiring/attrition lifecycle counts.

```text
record referral or grievance with owner and status
apply configured confidentiality/permission rules
for staffing plan, compare approved headcount with current positions
aggregate hiring, transfer, promotion, exit and attrition data for authorized reports
```

### Major 12 — Employee self-service/mobile/PWA

**Minor 12.1 — Employee login and profile** — sub-functions: authenticated employee identity; employee-linked profile; profile/password settings; notification list; PWA responsive shell.

```text
authenticate session
resolve employee linked to current user
load only authorized employee profile and notifications
render mobile/PWA navigation and settings
```

**Minor 12.2 — Employee self-service requests** — sub-functions: view salary slips; submit leave/compensatory leave; submit attendance/shift requests; check in/out; create expense/advance requests; follow status.

```text
verify signed-in user is linked to employee record
validate requested self-service action
create only employee-owned request/check-in/claim
route through server-side policy and approval flow
return current request state and notifications
```

**Minor 12.3 — Push and workflow notifications** — sub-functions: create notification for request submitted/status changed/training scheduled/exit interview; deliver through enabled channel; mark/read or deep-link.

```text
workflow event occurs
resolve recipient and notification template
create notification and deliver through configured channel
open linked document after recipient authorization
```

### Major 13 — Reports and analytics

**Minor 13.1 — HR/leave/attendance reports** — sub-functions: employee information/analytics, birthday/exits, attendance sheet, holiday work, shift attendance, leave balances and ledger, project/timesheet utilization.

```text
validate report parameters and access
query employee, attendance, leave or timesheet records
apply selected filters and date grouping
calculate report columns/totals from stored/configured data
return result for table/export
```

**Minor 13.2 — Payroll/tax/benefit reports** — sub-functions: salary register, accrued earnings, bank remittance, salary payment by mode, ECS, employee cost-to-company, income-tax computation/deductions, benefit/tax-exemption cards.

```text
validate period/company/payment/report filters
query finalized payroll, tax, benefit and payment records
aggregate values using report definition
return employee-level rows and totals to authorized payroll role
```

**Minor 13.3 — Recruitment/performance/expense/fleet analytics** — sub-functions: candidate pipeline/source/time-to-fill; appraisal; claims by type/department; advance status; vehicle expenses; dashboards and number cards.

```text
load filtered recruiting, performance, expense or vehicle records
aggregate counts/amounts by configured dimensions and interval
render report/charts/number cards
```

### Major 14 — Workflow, security and integrations

**Minor 14.1 — Document lifecycle and approvals** — sub-functions: draft/submit/cancel states; role permissions; configured multi-step approvers; validate transitions; persist audit metadata.

```text
authenticate current user
check document-level permission for requested action
validate allowed current-state → next-state transition
run DocType validation/business hooks
save transition and audit data
notify next approver or affected user
```

**Minor 14.2 — API/controller integrations** — sub-functions: expose controller methods/REST routes; validate inputs; enforce session/role/document permissions; call service logic; serialize result/errors.

```text
receive API request
authenticate and authorize caller
validate request fields and linked-record scope
execute domain operation
return structured success or validation/authorization error
```

**Minor 14.3 — ERPNext accounting/payroll integration** — sub-functions: account/cost-center mapping; salary/payment/expense postings; payroll payment documents; reconcile references/status.

```text
verify connected ERPNext and mapped accounts/cost centers
build accounting/payment document from approved HR record
submit through ERPNext transaction rules
store document reference and resulting status on HR record
```

**Minor 14.4 — Regional setup, migrations and patches** — sub-functions: install/hook configuration; country fixtures; schema/data patches; upgrade consistency; version compatibility.

```text
on install/upgrade
    check required framework/ERPNext version
    apply ordered schema and data patches
    load permitted regional fixtures/setup
    validate migrated records and report failures
```

## 3. Cautions for re-use

- Frappe HRMS models a configurable, integrated HR suite, not a ready-to-copy payroll ruleset for the Philippines. Salary formulas depend on configured components, tax slabs, benefits, company settings and regional/localization code. Never port a formula without jurisdiction/date/source review.
- The app's README lists modules; the repository is much broader than the README feature bullets. This inventory maps the checked-in module families, but a release-level function-by-function inventory should be generated from each DocType/controller/report and checked against upstream docs because `develop` changes continuously.
- Frappe/ERPNext framework supplies database, auth, workflow, API and accounting primitives. Pseudocode intentionally names those platform boundaries; the HRMS app is not standalone.
- Permission and lifecycle behavior is generally configured per site/DocType/workflow. Do not assume one globally identical employee/HR/payroll approval matrix from repository folder names.
- GPL-3.0 has obligations distinct from the MIT-licensed reference projects. Review license compatibility before copying or linking code.
- Static inventory only: no bench, Frappe server, database, frontend build, migration, or test suite was run.
