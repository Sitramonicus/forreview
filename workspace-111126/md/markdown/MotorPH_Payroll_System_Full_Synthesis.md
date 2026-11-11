# MotorPH Payroll System — Complete Source Synthesis

**Purpose:** A comprehensive, text-first reconstruction of the three supplied MO-IT107 documents, prepared so that an agent which cannot inspect the original PDFs or their embedded diagrams can still work from the same substantive information.

**System:** MotorPH Payroll System  
**Author shown in the submissions:** Jether Daniel A. Torres  
**Programme shown in the worksheets:** BSIT – Software Development  
**Academic year shown:** A.Y. 2024–2025  
**Course:** MO-IT107, Information Management  

> **Source fidelity rule:** This synthesis reports what the three supplied documents say. It does not silently repair schema differences, infer missing database constraints, invent payroll formulas, or replace the submitted design with a different design. Where the documents disagree, both versions are recorded and the disagreement is explained. Interpretations that connect two parts of the documents are identified as interpretations rather than direct source statements.

---

## Table of contents

1. [What this reference contains](#1-what-this-reference-contains)
2. [Source documents and their roles](#2-source-documents-and-their-roles)
3. [System overview and functional scope](#3-system-overview-and-functional-scope)
4. [Visual reconstruction of the database schema diagram](#4-visual-reconstruction-of-the-database-schema-diagram)
5. [Visual reconstruction of the data-flow diagrams](#5-visual-reconstruction-of-the-data-flow-diagrams)
6. [Database table inventory and functional grouping](#6-database-table-inventory-and-functional-grouping)
7. [Complete data dictionary: Employee Details](#7-complete-data-dictionary-employee-details)
8. [Complete data dictionary: Logging](#8-complete-data-dictionary-logging)
9. [Complete data dictionary: Government Details](#9-complete-data-dictionary-government-details)
10. [Complete data dictionary: Payroll](#10-complete-data-dictionary-payroll)
11. [Complete data dictionary: Static-Update-Only tables](#11-complete-data-dictionary-static-update-only-tables)
12. [Additional table in the database-design submission: Rest Day](#12-additional-table-in-the-database-design-submission-rest-day)
13. [Relationship catalogue](#13-relationship-catalogue)
14. [End-to-end functional traces](#14-end-to-end-functional-traces)
15. [Constraints, access controls, retention, and operational rules](#15-constraints-access-controls-retention-and-operational-rules)
16. [Cross-document discrepancies and unresolved design questions](#16-cross-document-discrepancies-and-unresolved-design-questions)
17. [What the documents do not specify](#17-what-the-documents-do-not-specify)
18. [Page-by-page source map](#18-page-by-page-source-map)
19. [Working instructions for a downstream agent](#19-working-instructions-for-a-downstream-agent)
20. [Data Dictionary glossary and acronym definitions](#20-data-dictionary-glossary-and-acronym-definitions)

---

## 1. What this reference contains

This file synthesises three documents supplied together:

1. **Data Dictionary for MotorPH Payroll System Database** — the Week 7, Worksheet No. 4 submission for logical schema design. It provides the table inventory, groups the tables by function, describes the columns, assigns data types and lengths or precision, and records field-level constraints.
2. **Database Design for the MotorPH Payroll System** — the second-milestone database-design submission. It provides the system overview, the visual schema diagram, detailed table definitions and table-level constraints, a relationship catalogue with cardinalities, and a short references section.
3. **Data Flow Diagram of MotorPH Payroll System** — the Week 11, Worksheet No. 6 submission. It provides a context-level DFD, a Level 1 DFD, an employee-management Level 2 decomposition, a payroll-processing Level 2 decomposition, and an archival Level 2 decomposition.

The synthesis is intentionally more than a summary. It converts information that is carried by tables, diagram labels, connector arrows, colour keys, and page layouts into readable text. It retains field names, spellings, type declarations, identifiers, stated cardinalities, process names, data-store names, and flow labels. The intention is that a receiving agent can answer detailed questions about the documents without having to reopen the PDFs.

### 1.1 Source hierarchy used in this synthesis

The documents overlap, but they do not constitute one perfectly consistent specification. The following handling rules are applied:

- The **Data Dictionary** is treated as the main source for the column-by-column catalogue, including data types, lengths, precision, descriptions, and field-level constraints.
- The **Database Design** submission is treated as the main source for table-level constraints, the relationship matrix, the extra `Rest Day` table, and the ERD/schema-diagram presentation.
- The **DFD** submission is treated as the main source for system actors, processes, data stores, directional data flows, payroll sub-processes, and archival stages.
- The **ERD image** is transcribed as a separate visual source. Where the image displays a key or column not stated in the corresponding prose table, that difference is retained.
- The **relationship matrix** is transcribed as written. It is not silently expanded to include every foreign key that appears elsewhere in the documents.
- The **DFD labels** are kept close to their original wording. Abbreviations such as `1YO`, `2YO`, and `opt*` are not expanded because the DFD does not define them.

### 1.2 Conventions used below

- `PK` means primary key, `FK` means foreign key, and `Unique` means a stated uniqueness rule.
- `NOT NULL`, `NULL allowed`, `DEFAULT(CURRENT_TIMESTAMP)`, `Min(0)`, `Max(31)`, and similar expressions are reported in the same sense as the source.
- `Decimal(19,4)` means precision 19 and scale 4 where the source provides those values. `Decimal(4,2)` means precision 4 and scale 2.
- `Varchar(n)` is used as a compact rendering of the source's `Varchar` plus `Size(n)` notation.
- When the source gives only a broad type such as `Enum`, `SET`, `DateTime`, `Varbinary`, or `Integer`, this synthesis does not invent an underlying SQL dialect or a more specific implementation type.
- `No constraint stated` means the relevant source table shows a dash or says `None`, or does not identify a constraint for that field. It does not establish that a real implementation should have no constraint.
- The source documents use both `Pag-ibig` and `Pag-IBIG` in different places. This synthesis generally uses `Pag-ibig` when reproducing field labels and `Pag-IBIG` when discussing the government programme in prose.
- `Government PII` is the DFD's wording for government-related personally identifiable information, which includes government identifiers held in the `Government` table.

---

## 2. Source documents and their roles

### 2.1 Data Dictionary for MotorPH Payroll System Database

**Document identity:**

- Course: MO-IT107, Information Management.
- Week: 7.
- Topic: Logical Schema Design.
- Worksheet: No. 4, Data Dictionary for MotorPH Payroll System Database.
- Business Analyst: Jether Daniel A. Torres.
- Programme and year level: BSIT – Software Development; A.Y. 2024–2025.
- Main document title: *Data Dictionary for MotorPH Payroll System Database*.

The document explains that a data dictionary describes the significance, organisation, and context of data items by describing fields in a dataset. Its instructions say to access the team’s Milestone 1 output, use that information to fill the table requirements, replace the template's red text, and remove the instruction note before submission.

Its substantive contents are:

1. **Table Definitions** — a list of database tables grouped as Employee Details, Logging, Government Details, Payroll, and Static-Update Only.
2. **Column Definitions** — a field catalogue containing column names, descriptions, data types, lengths or precision, and constraints.

The Data Dictionary describes 29 named tables. It does not include a separate `Rest Day` table, although it does describe a `restDay` field in `Schedule` as a `SET`.

### 2.2 Database Design for the MotorPH Payroll System

**Document identity:**

- Title: *Database Design for the MotorPH Payroll System*.
- Prepared and presented by: Torres, Jether Daniel A.
- Programme: Bachelor of Science in Information Technology.
- Term and year: 2nd Term – A.Y. 2024–2025.

The system overview describes a centralised payroll database intended to automate, organise, and streamline payroll operations. The submission presents five functional areas:

1. Employee Management.
2. Payroll Processing.
3. Attendance and Leave Tracking.
4. Government Contributions and Compliance.
5. Security and Access Control.

It then shows a schema diagram, lists table definitions and constraints, presents a relationship matrix, and cites an Entity-Relationship Diagram, a Data Dictionary, and a Schema Diagram in its references section.

The Database Design submission describes 30 tables in its table catalogue because it includes a separate `Rest Day` table. It also shows some fields in its ERD that are absent from its own prose table listings, and some fields that appear in the Data Dictionary but not in the prose table listings.

### 2.3 Data Flow Diagram of MotorPH Payroll System

**Document identity:**

- Course: MO-IT107, Information Management.
- Week: 11.
- Topic: Data Flow Diagrams.
- Worksheet: No. 6, Data Flow Diagram of MotorPH Payroll System.
- Business Analyst: Jether Daniel A. Torres.
- Programme and year level: BSIT – Software Development; A.Y. 2024–2025.

The worksheet defines a data-flow diagram as a tool for understanding movement of data inside a system or process. The instructions direct the student to consult two business documents:

1. *Business Requirement Specification Document for Payroll System*.
2. *MotorPH Existing Business Processes – Payroll*.

The DFD is required to use **Gene & Sarson notation** and to be decomposed at least to **Level 2**. The submitted DFD contains a context-level diagram, a Level 1 diagram, an employee-management Level 2 view, a payroll-processing Level 2 view, and an archival Level 2 view.

The references table names the same two business documents as the sources for the DFD. Their full text is not included in the three uploaded PDFs, so their requirements and process details cannot be independently reconstructed from this source set.

---

## 3. System overview and functional scope

### 3.1 Overall purpose

The Database Design submission describes the MotorPH Payroll System database as a centralised, secure, and scalable repository for most payroll-related data. Its stated intended effects are:

- automate and organise payroll operations;
- reduce manual effort in payroll handling;
- support faster retrieval and processing of payroll-related data;
- enable timely salary computation;
- support tax management;
- support legally required government contributions and compliance;
- improve data derivation and reduce unnecessary effort; and
- accommodate future organisational growth through mechanisms for data expansion.

The system is presented as a repository that connects employee, attendance, leave, compensation, deductions, government contribution, payroll, and summary information. The Employee record is described as the primary reference point for most modules, with nearly every module eventually tied to its unique identifier.

### 3.2 Employee Management

The Employee Management function captures, updates, and maintains employee records. The Database Design overview says these include:

- employment status;
- department affiliation;
- salary structure; and
- personal information.

It is described as the primary reference for other modules, with most other modules connected to the employee's unique identifier.

The DFD divides employee-side operations into three Level 2 processes:

- `1.1 Employee Information Handling`;
- `1.2 Schedule and Time Tracking`; and
- `1.3 Government Info Access`.

The associated stores are employee address, job profile, personally identifiable information, attendance, leave, and changelog records.

### 3.3 Payroll Processing

Payroll Processing is described as automated computation that accounts for:

- attendance records;
- salary adjustments;
- overtime hours;
- tax deductions; and
- government-mandated contributions, specifically SSS, PhilHealth, Pag-IBIG, and withholding tax.

The overview says the system provides a **semi-monthly payslip for each employee** and a **monthly summary for each payroll processing cycle**. The schema has a `Payslip` table containing the pay-period start and end dates, while the DFD has a payslip-generation process and a monthly-summary compilation process.

The DFD decomposes payroll into seven processes:

- `2.1 Timesheet Validation`;
- `2.2 Leave Monetisation Process`;
- `2.3 Wage and Adjustments`;
- `2.4 Government Deductibles`;
- `2.5 Basic Payroll Computation`;
- `2.6 Payslip Generation & Distribution`; and
- `2.7 Monthly Summary Compilation`.

### 3.4 Attendance and Leave Tracking

The overview says the system integrates with an employee attendance logging system to fetch clock-in and clock-out data automatically. It also records and catalogues leave applications.

The purpose of this function is to enable payroll calculations to:

- account for approved absences;
- account for active work presence during holidays and rest days; and
- determine applicable leave benefits.

The schema stores attendance using `Attendance`, linked to `Schedule`. Leave requests are stored in `Leave`, linked to `Leave Balance` and leave type. Leave accruals are recorded in `Leave Accrual History`, and leave monetisation is represented in `Leave Monetisation`.

### 3.5 Government Contributions and Compliance

The overview says the system calculates and maintains records of statutory government-related organisational contributions, including employer and employee deductions for government agencies. It is also said to generate tax reports and government contribution summaries to assist regulatory compliance.

The government-related data model contains:

- `Government`, holding government identifiers for an employee;
- `Government Deductibles`, holding employer contribution, employee contribution, and withholding-tax amounts by government record and month;
- `Contributions`, holding specific PhilHealth, Pag-ibig, and SSS contribution values by contributor;
- `Government Tally`, holding overall contribution totals by contributor; and
- `Summary`, which contains combined contribution and tax totals.

The source documents do not provide the actual statutory rates or bracket values. They define tables intended to hold rates and tax brackets.

### 3.6 Security and Access Control

The Database Design overview describes role-based access control (RBAC) with hierarchical or tiered access for:

- System administrators;
- Human Resources personnel;
- Payroll staff; and
- employees.

The stated security objectives are to restrict modifications to authorised personnel, reduce unauthorised changes, and make the account responsible for a modification traceable through audit logs.

The document further states that access to an employee's personally identifiable information is barred unless two authorised personnel approve it. It says that only the corresponding contribution values are normally visible and accessible. The DFD reflects this with `View PII (On Approval)`, `Update PII (On Approval)`, and `PII Access Approval` flows, and a distinct `1.3 Government Info Access` process.

The ERD/schema diagram uses a red highlight for the `Government` entity, and its legend labels red as **Additional Security Required**. This visual emphasis is consistent with the prose statement that government identifiers require special access control.

---

## 4. Visual reconstruction of the database schema diagram

**Source location:** Database Design PDF, page 5, heading `2. Schema Diagram`.

The image is a dense entity-and-relationship schema. It contains the named tables below, their principal fields, PK/FK markings, and connector lines. The diagram is not merely decorative: it shows fields and foreign-key links that are not fully represented in the prose table definitions.

### 4.1 Diagram legend

The diagram includes a colour key with four meanings:

| Colour shown in diagram | Legend meaning |
|---|---|
| Pale orange / yellow | Update, Append, Read |
| Pale purple / lavender | Update, Read |
| Pale cyan / teal | Append, Read |
| Red | Additional Security Required |

The `Government` entity is the red-highlighted entity in the diagram. The source does not provide a separate written access-control matrix mapping each colour to each user role, so the legend should be understood as the diagram's own operation/security key rather than as a complete permissions specification.

### 4.2 Tables visible in the schema diagram

The diagram visibly contains the following entities and key columns. This inventory is a transcription of the diagram, not a replacement for the table-by-table field catalogue later in this file.

#### Employee and organisational structure

- **Employee** — `employeeID` (PK), `addressID` (FK), `positionID` (FK), `supervisorID` (FK), `firstName`, `lastName`, `contactNumber`, `birthdate`, `employmentStatus`.
- **Address** — `addressID` (PK), `cityID` (FK), `regionID` (FK), `building`, `street`, `postalCode`.
- **Department** — `departmentID` (PK), `departmentName`.
- **Position** — `positionID` (PK), `departmentID` (FK), `positionName`.
- **Supervisor** — `supervisorID` (PK), `employeeID` (FK), `departmentID` (FK).
- **Schedule** — `scheduleID` (PK), `employeeID` (FK), `restDayID` (FK), `startTime`, `requiredDailyHours`.
- **Rest Day** — `restDayID` (PK), plus Boolean fields `sunday`, `monday`, `tuesday`, `wednesday`, `thursday`, `friday`, and `saturday`.

#### Leave and attendance

- **Leave Balance** — `balanceID` (PK), `leaveType` (FK), `scheduleID` (FK), `currentBalance`, `lastUpdated`.
- **Leave Type** — `leaveType` (PK), `mandatory`, `defaultAccrualRate`, `accrualFrequency`, `maxAccrual`.
- **Leave** — `leaveID` (PK), `balanceID` (FK), `leaveType` (FK), `scheduleID` (FK), `dateStart`, `hourDuration`, `leaveReason`, `leaveStatus`, `requestLeaveCredit`, `remainingLeaveCredit`, `dateRequested`, `dateApproved`, `approverID`.
- **Leave Accrual History** — `accrualID` (PK), `balanceID` (FK), `leaveType` (FK), `accrualDate`, `creditAccrued`, `oldBalance`, `newBalance`.
- **Leave Monetisation** — `monetisationID` (PK), `leaveType` (FK as represented in the ERD), `balanceID` (FK), `creditsMonetised`, `monetaryValuePer`, `totalConverted`, `dateMonetised`, `approverID`.
- **Attendance** — `attendanceID` (PK), `scheduleID` (FK), `date`, `timeIn`, `timeOut`, `dayType`.
- **Changelog** — `changelogNo` (PK), `employeeID` (FK), `createdAt`, `appenderID`, `updatedAt`, `editorID`, `fieldCode`, `dataType`, `oldValue`, `newValue`.
- **Holiday** — `holidayID` (PK), `holidayName`, `holidayDate`.

#### Government identifiers and deductions

- **Government** — `governmentID` (PK), `employeeID` (FK), `TIN`, `sssNo`, `philHealthNo`, `pagIbigNo`, `encryptedRef`. The entire entity is highlighted red in the schema image, matching the `Additional Security Required` key.
- **Government Deductibles** — `monthID` and `governmentID` are both shown with PK/FK markings; fields also include `employerContribution`, `employeeContribution`, and `withholdingTax`.
- **Contributions** — `governmentID` and `contributor` are shown as composite PK fields; `governmentID` is also an FK. The amounts are `philHealthContribution`, `pagIbigContribution`, and `sssContribution`.
- **Government Tally** — `summaryID` and `contributor` are shown as key fields, with `summaryID` also marked as an FK; amounts are `overallPagIbig`, `overallPhilHealth`, and `overallSSS`.

#### Payroll and reporting

- **Payroll** — `payrollID` (PK), `employeeID` (FK), `rebateID` (FK), `wageID` (FK), `summaryID` (FK), `monthID`, `ptoMax`, `hourlyRate`, `dailyRate`, `monthlyRate`, `netPay`.
- **Payslip** — `payslipID` (PK), `payrollID` (FK), `periodStartDate`, `periodEndDate`, `regularHours`, `overtimeHours`, `holidayOvertimeHours`, `ptoHours`, `undertimeHours`, `daysWorked`, `holidaysWorked`, `restDaysWorked`, `grossIncome`.
- **Earnings** — `wageID` (PK), `overtimePay`, `ptoPay`, `holidayPay`, `restDayPay`, `riceSubsidy`, `phoneStipend`, `clothesStipend`, `otherIncentTotal`, `totalBenefits`.
- **Deductibles** — `rebateID` (PK), `governmentID` (FK), `otherDeductTotal`, `totalDeductibles`.
- **Other Transactions** — `transactionID` (PK), `payrollID` (FK), `transactionType`, `transactionAmount`, `description`.
- **Summary** — `summaryID` (PK), `monthID` (FK), `overallGrossIncome`, `overallBenefits`, `employeeDisbursementTotal`, `employerDisbursementTotal`, `overallPITotal`, `overallPHTotal`, `overallSSSTotal`, `overallTaxTotal`, `governmentDisbursementTotal`.

#### Static/reference tables

- **Regions** — `regionID` (PK), `regionName`.
- **Cities** — `cityID` (PK), `regionID` (FK), `cityName`.
- **Months** — `monthID` (PK), `monthlyWorkdays`.
- **Rates** — `implementationYear` (shown as PK in the diagram), `piEmployerRate`, `piEmployeeRate`, `piMaxContribution`, `phRate`, `sssRate`.
- **Tax Table** — `taxBracket` (PK), `minCompensation`, `maxCompensation`, `bracketWidth`, `taxRate`.

### 4.3 What the ERD visually communicates

The diagram's connector network places `Employee` near the right side and connects it to `Address`, `Position`, and `Supervisor`. `Position` is connected to `Department`, while `Supervisor` links back to `Employee`. `Schedule` links to `Employee` and to the separate `Rest Day` table. `Attendance` links to `Schedule`.

The leave cluster places `Leave`, `Leave Balance`, `Leave Type`, `Leave Accrual History`, and `Leave Monetisation` near one another. Their connectors depict the leave-request, leave-balance, leave-type, accrual-history, and monetisation associations.

The payroll cluster links `Payroll` with `Earnings`, `Deductibles`, and `Summary`. `Payslip` is connected to `Payroll`. `Other Transactions` is connected to `Payroll`. `Government Deductibles` and `Contributions` connect government records and contributor-specific amounts, while `Government Tally` is attached to monthly `Summary` totals.

The reference-data portion places `Address` with `Cities` and `Regions`, `Cities` with `Regions`, and the tax/rate/month/holiday/leave-type entities around the calculation and reference tables.

### 4.4 Important visual-only schema details

The image makes several details especially clear:

1. `Schedule` contains `restDayID` rather than the `restDay` `SET` column described in the Data Dictionary and prose table list.
2. A standalone `Rest Day` table exists in the ERD, with seven weekday Boolean fields.
3. `Employee` in the ERD does not visibly contain `departmentID` or `governmentID`, although these are in the Data Dictionary's Employee field list.
4. `Address` in the ERD visibly contains `regionID`, although the Database Design prose definition of Address lists only `addressID`, `cityID`, `building`, `street`, and `postalCode`.
5. `Supervisor` in the ERD visibly contains `departmentID`, although the Database Design prose definition of Supervisor lists only `supervisorID` and `employeeID`.
6. `Rates` visibly marks `implementationYear` as the primary key, although the Database Design's table constraint section says the Rates table has no primary key, and the Data Dictionary does not list `implementationYear` at all.
7. `Government Deductibles` visibly shows both `monthID` and `governmentID` as key fields, consistent with a composite key.
8. `Contributions` visibly shows `(governmentID, contributor)` as a composite key.
9. `Government Tally` visibly shows `summaryID` and `contributor` as key fields, although the Data Dictionary calls `summaryID` its primary key “(assumed)”.

The exact consequences of these discrepancies are not resolved by the three documents. See [Section 16](#16-cross-document-discrepancies-and-unresolved-design-questions).

---

## 5. Visual reconstruction of the data-flow diagrams

**Source locations:** Data Flow Diagram PDF, pages 2–6. The actual diagrams are image-based; the following sections convert their labels, arrows, and visual groupings into text.

### 5.1 DFD notation and reading conventions

The worksheet expressly calls for **Gene & Sarson notation**. The diagrams use:

- rounded or rectangular cyan/teal shapes for external entities such as Employee, Supervisor, Human Resources, Payroll Staff, and System Admin;
- yellow/orange-topped process shapes for system processes, numbered `0.0`, `1.0`, `2.0`, `3.0`, and the decomposed Level 2 process numbers;
- lavender/purple data-store shapes, generally labelled with a `D` identifier and a store name; and
- directed connectors, with text labels describing the data or action moving along each connector.

The diagrams are process-and-data-flow views. They do not show every physical database column, and a data store in a DFD is not necessarily a one-to-one representation of a single relational table. The DFD uses larger logical stores such as `Employee Repository`, `Logs`, and `Payroll`, then subdivides them into named components in the Level 2 diagrams.

### 5.2 External entities

Five external actors appear across the DFDs:

| Actor | Role represented in the diagrams |
|---|---|
| **Employee** | Supplies attendance and time-in/time-out information; views employee information and payslips; views personal payslip information in the payroll Level 2 view. |
| **Supervisor** | Views team-member information and team payslips; adjusts team attendance; handles leave requests; handles or initiates leave monetisation; submits team-member wage and adjustment information; views team benefits/deductions. |
| **Human Resources** | Updates employee details; views employee and payroll details; participates in PII access approval and PII viewing/updating on approval; views relevant employee, attendance, and leave information. |
| **Payroll Staff** | Views and verifies employee and payroll details; views employee government PII on approval; updates payroll, payslip, benefits, and deductibles; validates and verifies payroll details and monthly reports. |
| **System Admin** | Manages configurations and users; views all employee information; participates in PII access approval; handles backup and archival; initiates the annual archival prompt; updates optional/system-controlled values in the detailed payroll diagram. |

The actors are distinct. The diagrams do not collapse HR, Payroll Staff, Supervisor, and System Admin into one generic administrator role.

### 5.3 Data stores in the DFD

| Identifier | Store name | Diagram placement / contents represented |
|---|---|---|
| `D1` | Employee Repository | Level 1 store for employee information. |
| `D1.1` | Employee Address | Level 2 subdivision for employee address data. |
| `D1.2` | Job Profile | Level 2 subdivision for employee job-profile data. |
| `D1.3` | Personally Identifiable Information | Level 2 subdivision for PII, shown beside Government Info Access. |
| `D2` | Logs | Level 1 store for operational logs, expanded into attendance, leave, changelog, and leave-monetisation logs. |
| `D2.1` | Attendance / Attendance Logs | Attendance and time activity used for work-hour derivation and timesheet validation. |
| `D2.2` | Leave / Leave Logs | Leave records used to derive paid-leave details and support leave management. |
| `D2.3` | Changelog / Changelogs | Employee-information change history. |
| `D2.4` | Leave Monetisation Logs | History of leave credits converted to monetary value. |
| `D3` | Payroll | Level 1 store containing wage, payroll, payslip, and benefits/deductibles information. |
| `D3.1` | Wage | Basic remunerative values and wage values. |
| `D3.2` | Benefits & Deductibles | Benefit and deduction values, including adjustments and computed deductions. |
| `D3.3` | Tax Table & Rates | Tax table and rates used to compute contributions. |
| `D3.4` | Payslip | Generated payslip records. |
| `D3.5` | Monthly Summary Report | Generated monthly summary report. |
| `D4` | Cold Storage | Destination for old logs and payroll records held for later compression. |

The Level 1 diagram names `D1 Employee Repository`, `D2 Logs`, `D3 Payroll`, and `D4 Cold Storage`. The Level 2 diagrams split the first three into more granular stores. These identifiers should be preserved when discussing the submitted diagrams.

### 5.4 Context-level DFD (`LV0: Context Diagram`)

**Source image:** DFD PDF, page 2. The context diagram represents the entire `MotorPH Payroll System` as one process, numbered `0.0`, surrounded by the five external entities.

#### 5.4.1 Employee flows

- **Employee → MotorPH Payroll System:** `Attendance and Time In/Out`.
- **MotorPH Payroll System → Employee:** `View Employee Information` (the information is supplied by the system in response to the employee's view/access activity).
- **MotorPH Payroll System → Employee:** `View Payslip`.

The diagram therefore presents the employee as both an input source for attendance/time data and a recipient of employee information and payslip output.

#### 5.4.2 Supervisor flows

- **MotorPH Payroll System → Supervisor:** `View Team Payslips`.
- **Supervisor → MotorPH Payroll System:** `Adjust Team Attendance`.
- **Supervisor → MotorPH Payroll System:** `Handle Leave Requests and Monetisation`.
- **Supervisor → MotorPH Payroll System:** `Handling Team Member Benefits / Deductions`.
- **MotorPH Payroll System → Supervisor:** `View Team Members' Employee Info`.

The diagram distinguishes the supervisor's ability to view team information from the supervisor's submission or handling of team attendance, leave, monetisation, and benefits/deductions matters.

#### 5.4.3 Human Resources flows

- **Human Resources → MotorPH Payroll System:** `Update Employee Details`.
- **Human Resources → MotorPH Payroll System:** `Update PII (On Approval)`.
- **MotorPH Payroll System → Human Resources:** `View PII (On Approval)`.
- **MotorPH Payroll System → Human Resources:** `View Employee and Payroll Details`.
- **Human Resources → MotorPH Payroll System:** `PII Access Approval`.

The approval condition is part of the flow labels. The diagram does not present PII viewing or updating as unrestricted operations.

#### 5.4.4 Payroll Staff flows

- **MotorPH Payroll System → Payroll Staff:** `View and Verify Employee and Payroll Details`.
- **MotorPH Payroll System → Payroll Staff:** `View Employees' Government PII (On Approval)`.
- **Payroll Staff → MotorPH Payroll System:** `Update Payroll, Payslip, and Benefits/Deductibles`.

The government PII view has an explicit approval condition. Payroll Staff also have a separate update flow for payroll-related records.

#### 5.4.5 System Admin flows

- **System Admin → MotorPH Payroll System:** `Configuration & User Management`.
- **MotorPH Payroll System → System Admin:** `View All Employee Information`.
- **System Admin ↔ MotorPH Payroll System:** `Archival Handling` (the diagram presents the archival interaction between the system and the administrator).
- **System Admin → MotorPH Payroll System:** `PII Access Approval`.

The context diagram shows the system administrator as responsible for configuration and user management, broad employee-information viewing, archival handling, and an approval role in the PII access path.

### 5.5 Level 1 DFD: major processes and data stores

**Source image:** DFD PDF, page 3. The Level 1 DFD decomposes the single context process into three major processes:

- `1.0 Employee Management`;
- `2.0 Payroll Processing`; and
- `3.0 Archival`.

The major data stores are `D1 Employee Repository`, `D2 Logs`, `D3 Payroll`, and `D4 Cold Storage`.

#### 5.5.1 Process 1.0 — Employee Management

`1.0 Employee Management` operates on the `D1 Employee Repository` and the `D2 Logs` store.

**Employee Repository (`D1`) flows:**

- `Append/Modify Employee Info` — information is written to or modified in the employee repository.
- `Retrieve Employee Information` — employee information is retrieved from the repository for the process.

**Employee Management and Logs (`D2`) flows shown in the diagram:**

- `View Leave and Attendance Details`.
- `View Employee Changelog`.
- `Append/Modify Leave and Attendance`.
- `Append Employee Info Changes`.

**Employee Management process labels and access-related flows:**

- `PII Access Approval`.
- `Configurations & User Management`.
- `View All Employee Information`.
- `Update Employee Details`.
- `View Employee Info`.
- `View PII (On Approval)`.
- `Update PII (On Approval)`.

The Level 1 diagram connects Employee Management with Employee, Supervisor, Human Resources, Payroll Staff, and System Admin. The employee and supervisor interactions include attendance/time-in/time-out, employee-information viewing, team-member employee-information viewing, attendance adjustment, and leave-request handling. HR performs employee-detail updates and participates in approved PII operations. Payroll Staff view and verify employee information and may view government PII on approval. System Admin handles configuration/user management, broad employee-information viewing, and PII-related approval.

#### 5.5.2 Process 2.0 — Payroll Processing

`2.0 Payroll Processing` handles payroll-related calculations and updates. It connects with `D3 Payroll` and with relevant records in `D2 Logs`.

**Payroll store (`D3`) flows:**

- `Update Wage, Payroll, Payslip, and Benefits/Deductibles` — updated payroll information is written to the payroll store.
- `Retrieve Wage, Payroll, Payslip, and Benefits/Deductibles` — payroll information is retrieved for processing or viewing.

**Log-related flows:**

- `Derive work hours` — work-hour information is derived from attendance logs.
- `Append/Modify Leave Monetisation History` — leave-monetisation history is recorded or modified.
- `Retrieve (2YO) Payroll Details` — the Level 1 diagram shows this label in the archival/log retrieval path associated with payroll processing.

**Actor-facing flows:**

- Supervisor: `Handle Leave Monetisation`, `View Team Payslips`, and `Handling Team Member Wage and Adjustments`.
- Payroll Staff: `View and Verify Payroll Details` and `Update Payroll, Payslip, and Benefits/Deductibles`.
- Human Resources: `View Payroll Details`.
- System Admin: `Configurations & User Management` and the system-level archival/configuration interactions.

The diagram's label `2YO` is reproduced exactly. The document does not define whether it means “two years old”, “two-year-old”, or another retention code, so no expansion is imposed here.

#### 5.5.3 Process 3.0 — Archival

`3.0 Archival` is shown as a separate process connected to `D2 Logs`, `D3 Payroll`, `D4 Cold Storage`, and System Admin.

Its visible labels include:

- `Retrieve (1YO) Attendance, Update, and Leave Monetisation Logs` — information is retrieved from the logs for archival handling.
- `Store Old Logs and Payroll for later compression` — archival output is stored in `D4 Cold Storage`.
- `Backup and Archival` — the administrator participates in the archival process.

The diagram contains a `1YO` label in the log retrieval path and a `2YO` payroll-related label in the process-2.0 path. The more detailed archival Level 2 diagram later makes the record groups explicit: it compiles `1YO` records from the operational logs and `2YO` records from Payslip.

### 5.6 Level 2 DFD: Employee Management (`1.0` decomposition)

**Source image:** DFD PDF, page 4. This diagram expands Employee Management into three processes and six data stores.

#### 5.6.1 Process 1.1 — Employee Information Handling

This process deals with employee address, job-profile, employee-information, and changelog activity.

**Employee-information stores:**

- `D1.1 Employee Address`.
  - `Append/Modify Employee Address` — process 1.1 writes or changes employee address information.
  - `Retrieve Employee Address` — process 1.1 retrieves address information.
- `D1.2 Job Profile`.
  - `Append/Modify Employee Job Profile` — process 1.1 writes or changes job-profile information.
  - `Retrieve Employee Job Profile` — process 1.1 retrieves job-profile information.
- `D2.3 Changelog`.
  - `Append on Employee Info Update` — employee-information updates are logged.
  - `View` — the changelog is available for viewing.

**Actor-facing labels:**

- Payroll Staff: `View and Verify Employee Details`.
- Payroll Staff: `View Employees' Government PII (On Approval)`.
- Supervisor: `View Team Members' Employee Info`.
- Employee: `View Employee Info`.
- Human Resources: `Update Employee Details`, `View Employee Info`, and the employee-information update path.
- System Admin: `Configurations & User Management` and `View All Employee Information`.

The diagram shows HR updating employee information, with an employee-information update being appended to the changelog. It also presents employee-information viewing separately for an employee, a supervisor, payroll staff, HR, and the system administrator.

#### 5.6.2 Process 1.2 — Schedule and Time Tracking

This process handles attendance and leave-related schedule activity.

**Inputs from external actors:**

- Employee → Process 1.2: `Attendance and Time In/Out`.
- Supervisor → Process 1.2: `Handle Leave Requests`.
- Supervisor → Process 1.2: `Adjust Team Attendance`.

**Data-store operations:**

- `D2.1 Attendance` — `View` and `Append/Modify` flows connect the process with the Attendance store.
- `D2.2 Leave` — `View` and `Append/Modify` flows connect the process with the Leave store.

The diagram positions attendance and leave as the operational records that are read and updated by Schedule and Time Tracking. The Level 1 diagram also shows work-hour derivation from logs as an input to payroll processing.

#### 5.6.3 Process 1.3 — Government Info Access

This process is dedicated to government information and PII access.

**Data store:**

- `D1.3 Personally Identifiable Information`.
  - `View` — the process reads PII from the store.
  - `Append/Modify` — the process writes or changes PII in the store.

**Access-control labels around the process:**

- `Access Approval`.
- `Update PII (On Approval)`.
- `View PII (On Approval)`.
- `Record Modification`.
- A second `Access Approval` flow is also drawn in the access path.

The process connects with Human Resources and System Admin in the approved-access area. The precise internal authorisation sequence is not defined in prose, but the visual labels clearly require approval for PII viewing and updating, and show a record-modification path. This is consistent with the database overview's statement that PII requires approval from two authorised personnel.

### 5.7 Level 2 DFD: Payroll Processing (`2.0` decomposition)

**Source image:** DFD PDF, page 5. This is the most detailed payroll-processing diagram. It decomposes process `2.0 Payroll Processing` into seven processes and five payroll stores, while also using attendance, leave, and leave-monetisation logs.

#### 5.7.1 Process 2.1 — Timesheet Validation

**Input stores:**

- `D2.1 Attendance Logs`.
- `D2.2 Leave Logs`.

**Flow labels:**

- `Derive work hours and other attendance data` — attendance logs supply the work-hour and other attendance information.
- `Derive details for paid leaves` — leave logs supply paid-leave details.
- `Get Time Activity` — timesheet validation passes time-activity information into basic payroll computation.

Process 2.1 is the point where attendance and paid-leave information is consolidated into time activity for the payroll calculation. The diagram does not state an algorithm for validating time entries or handling missing/overlapping entries.

#### 5.7.2 Process 2.2 — Leave Monetisation Process

**Input:** Supervisor → Process 2.2: `Process Approved Leave Monetisation`.

**Outputs:**

- `Append` — writes the monetisation event to `D2.4 Leave Monetisation Logs`.
- `Add to Benefits` — sends the monetary result into Process 2.3 Wage and Adjustments.

The process therefore depicts monetisation as a supervisor-handled, approved event, logged separately, with the resulting value added to benefits. The DFD does not state the conversion formula or define the leave categories eligible for monetisation.

#### 5.7.3 Process 2.3 — Wage and Adjustments

**Inputs and outputs:**

- Supervisor → Process 2.3: `Send Team Member Wage and Adjustments`.
- Process 2.2 → Process 2.3: `Add to Benefits`.
- Process 2.3 → `D3.1 Wage`: `Update Wage (opt*)`.
- Process 2.3 → `D3.2 Benefits & Deductibles`: `Append Adjustments`.
- System Admin → Process 2.3: `Update Values (opt*)`.

The diagram shows wage information and adjustments feeding payroll calculation. It explicitly distinguishes a wage-store update from appending adjustments to the Benefits & Deductibles store. The `opt*` marker is part of the submitted diagram, but no legend or note defines it.

#### 5.7.4 Process 2.4 — Government Deductibles

**Inputs and outputs:**

- System Admin → Process 2.4: `Update Values (opt*)`.
- System Admin / rate maintenance path → `D3.3 Tax Table & Rates`: `Update Rates (opt*)`.
- `D3.3 Tax Table & Rates` → Process 2.4: `Utilise Rates to Compute Contributions`.
- Process 2.4 → `D3.2 Benefits & Deductibles`: `Append to Deductibles`.
- Process 2.4 → Process 2.7: `Collate`.

Process 2.4 uses the tax table and rate store to compute contributions and appends the resulting amounts to deductibles. It also contributes information to monthly summary compilation. The DFD does not provide specific formulas, statutory rate values, thresholds, rounding rules, or calculation dates.

#### 5.7.5 Process 2.5 — Basic Payroll Computation

**Inputs:**

- Process 2.1 → Process 2.5: `Get Time Activity`.
- `D3.1 Wage` → Process 2.5: `Get Basic Remunerative Values`.
- `D3.2 Benefits & Deductibles` → Process 2.5: `Apply Adjustments`.

**Outputs:**

- Process 2.5 → Process 2.6: `Get Net Pay and Constituents`.
- Process 2.5 ↔ Payroll Staff: `View, Validate, and Verify Payroll Details`.

This is the central calculation process. It combines time activity, basic remunerative values, and adjustments. The output is described as net pay and its constituents. The process is also subject to payroll-staff review, validation, and verification.

The term `Basic Payroll Computation` is the process name. The source does not give a mathematical formula for gross pay, deductions, net pay, overtime, holiday pay, undertime, or any other component.

#### 5.7.6 Process 2.6 — Payslip Generation & Distribution

**Inputs:** Process 2.5 → Process 2.6: `Get Net Pay and Constituents`.

**Output store:**

- Process 2.6 → `D3.4 Payslip`: `Append`.

**User-facing flows:**

- Employee: `View Personal` — employee views their personal payslip information.
- Supervisor: `View Team` — supervisor views team payslip information.
- Human Resources: `View` — HR views payslip/report information.

The process creates payslip records from the net-pay calculation and makes relevant payslip information available to the employee, supervisor, and HR. The Level 1 diagram also describes a `View Payslip` flow at the context level.

#### 5.7.7 Process 2.7 — Monthly Summary Compilation

**Inputs:**

- Process 2.4 → Process 2.7: `Collate`.
- `D3.4 Payslip` → Process 2.7: `Collate`.

**Outputs:**

- Process 2.7 → `D3.5 Monthly Summary Report`: `Generate`.
- Payroll Staff ↔ Process 2.7: `View, Validate, and Verify Monthly Report`.

Monthly Summary Compilation collates payroll/payslip and government-deduction/contribution information, produces a Monthly Summary Report, and allows Payroll Staff to view, validate, and verify the report.

### 5.8 Level 2 DFD: Archival (`3.0` decomposition)

**Source image:** DFD PDF, page 6. This diagram decomposes the Level 1 Archival process into two processes:

- `3.1 Appending`;
- `3.2 Deleting`.

It shows four operational log stores, one payslip store, and cold storage:

- `D2.1 Attendance Logs`;
- `D2.2 Leave Logs`;
- `D2.3 Changelogs`;
- `D2.4 Leave Monetisation Logs`;
- `D3.4 Payslip`; and
- `D4 Cold Storage`.

#### 5.8.1 Process 3.1 — Appending

- System Admin → Process 3.1: `Trigger on Annual Prompt`.
- The process receives `Compile (1YO) Records` from the group of log stores: Attendance Logs, Leave Logs, Changelogs, and Leave Monetisation Logs.
- The process receives `Compile (2YO) Records` from `D3.4 Payslip`.
- Process 3.1 → `D4 Cold Storage`: `Store Old Logs and Payroll for later compression`.

The diagram thus depicts a scheduled archival event initiated by a system administrator, compilation of one group of records marked `1YO`, compilation of payslip records marked `2YO`, and transfer of old logs/payroll to cold storage for later compression.

#### 5.8.2 Process 3.2 — Deleting

- Process 3.2 is labelled `Deleting`.
- Its outgoing data-removal action is labelled `Remove Relevant Records`.
- The process is connected to the operational log stores `D2.1` through `D2.4` and to `D3.4 Payslip`.
- The diagram contains a `Trigger on Finish` connector between the archival stages.

The intended ordering shown is archival compilation/storage followed by deletion of the corresponding relevant source records after the preceding stage finishes. The diagram does not define the verification checks, recovery procedure, failure behaviour, legal retention basis, or exact calendar calculation behind the `1YO` and `2YO` labels.

### 5.9 End-to-end DFD interpretation

Across all DFD levels, the main operational chain is:

1. Employee and Supervisor interactions provide attendance, time, leave, and team information.
2. Employee Management maintains the employee repository, job profile, address, PII, attendance, leave, and changelog information.
3. Payroll Processing obtains validated time activity, leave details, wage information, adjustments, and statutory rates.
4. Basic Payroll Computation derives net pay and its constituents.
5. Payslip Generation & Distribution records and exposes payslip information.
6. Monthly Summary Compilation collates the period's payslip and government-related amounts into a monthly report.
7. Archival compiles older logs and payslip records, stores them in cold storage, and then removes relevant records from the operational stores.

This is a synthesis of the process and data-store structure shown in the diagrams. The source does not specify the exact timing or execution schedule of each payroll run, except that the overview describes semi-monthly payslips and monthly summaries, and the archival diagram identifies an annual prompt.

---

## 6. Database table inventory and functional grouping

The Data Dictionary arranges its 29 tables into five functional groups. The Database Design submission includes all of these, with the additional `Rest Day` table. The grouping below reproduces the Data Dictionary's organisation.

### 6.1 Group A — Employee Details

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Employee` | Preserves detailed records of all employees, incorporating unique identifiers, departmental affiliations, positional data, government-linked information, and supervisory relationships. |
| `Address` | Maintains discrete records of employees' physical addresses, incorporating city and regional identifiers, building details, street information, and postal codes. |
| `Department` | Delineates operational units, providing unique identifiers and names that define each departmental segment. |
| `Position` | Registers individual job roles by associating a unique position identifier with corresponding departmental information. |
| `Supervisor` | Documents supervisory assignments by linking supervisor identifiers with employee records and departmental associations. |
| `Schedule` | Records employee scheduling data, including start times, required daily working hours, and assigned weekly rest days, to support workforce management and payroll performance calculations. |
| `Leave Balance` | Tracks employee leave entitlements, maintaining balances per leave type and timestamps for the latest updates to support accrual management. |

### 6.2 Group B — Logging

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Changelog` | Archives modifications to employee records, including timestamps, field identifiers, previous values, and updated values for audit purposes. |
| `Attendance` | Registers attendance information, documenting clock-in and clock-out times, attendance dates, and day classification. |
| `Leave` | Records leave requests, including leave types, durations, justifications, approval statuses, and scheduled start dates. |
| `Leave Accrual History` | Records leave-accrual progression over time, including accrual dates, credited amounts, and balance transitions. |

### 6.3 Group C — Government Details

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Government` | Consolidates government-related identifiers for each employee. |
| `Government Deductibles` | Itemises monthly government-related deductions, listing employer and employee contributions alongside withholding taxes. |
| `Contributions` | Records individual government contributions, detailing employer and employee shares for PhilHealth, Pag-ibig, and SSS. |

### 6.4 Group D — Payroll

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Payroll` | Compiles payroll information by linking employee identifiers with wage, rebate, and summary records, while incorporating monthly identifiers and detailed salary rates. |
| `Payslip` | Generates individual payslip records within the pay period, including hour metrics such as overtime and undertime, and gross and net income figures. |
| `Leave Monetisation` | Documents the conversion of accrued leave into monetary value. |
| `Earnings` | Aggregates additional remuneration components such as overtime, paid time off, holiday pay, and stipends. |
| `Deductibles` | Details payroll deductions by listing rebate identifiers and other deduction totals, and aggregates these values to yield overall deductibles per payroll cycle. |
| `Other Transactions` | Records miscellaneous payroll-related financial transactions such as bonuses, advances, reimbursements, and additional deductions. |
| `Government Tally` | Consolidates government contribution totals over each payroll period, providing Pag-ibig, PhilHealth, and SSS totals according to contributor. |
| `Summary` | Organises a monthly financial summary of payroll disbursements, benefits, tax withholdings, and government disbursements. |

### 6.5 Group E — Static-Update Only

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Regions` | Maintains static region records using a unique identifier and region name. |
| `Cities` | Lists cities with unique identifiers and links them to corresponding regions. |
| `Holiday` | Enumerates public and recognised organisational holidays using unique identifiers, names, and dates. |
| `Months` | Defines monthly periods using month identifiers and a record of the number of workdays. |
| `Rates` | Specifies statutory contribution rates, including employer and employee Pag-ibig rates, maximum contributions, PhilHealth rates, and SSS rates. |
| `Tax Table` | Outlines tax brackets through income ranges, bracket widths, and associated tax rates. |
| `Leave Type` | Classifies leave categories, indicating whether each type is mandatory and specifying default accrual rates, accrual frequencies, and maximum accrual limits. |

**Grouping note:** The Data Dictionary table inventory labels the final category `E. Static-Update Only`, while the heading above its column definitions labels the same category `F. Static-Update Only`. The table group has seven listed entities in either case.

---

## 7. Complete data dictionary: Employee Details

This section reproduces every field specified in the Data Dictionary's Employee Details group. Field descriptions are retained in meaning, with line wraps repaired for readability. The final column reproduces the source's stated constraints. Table-level constraint details from the Database Design submission are added after each table where useful.

### 7.1 `Employee`

**Purpose:** Employee identity, employment status, organisational placement, address, government record, and supervisory linkage.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `employeeID` | `SmallInt`, size 5 | Unique employee identifier. | Primary Key; Unique. |
| `addressID` | `SmallInt`, size 5 | Linked address identifier. | Foreign Key. |
| `departmentID` | `TinyInt` | Identifier for the department. | Foreign Key. |
| `positionID` | `TinyInt` | Identifier for the position. | Foreign Key. |
| `governmentID` | `SmallInt`, size 5 | Linked government record identifier. | Foreign Key. |
| `supervisorID` | `SmallInt`, size 5 | Employee's immediate supervisor identifier. | Foreign Key. |
| `firstName` | `Varchar(255)` | Employee's given name. | No additional constraint stated. |
| `lastName` | `Varchar(255)` | Employee's surname. | No additional constraint stated. |
| `contactNumber` | `Varchar(15)` | Employee's phone number. | No additional constraint stated. |
| `birthdate` | `Date` | Employee's date of birth. | `CHECK (NOT NULL)` and `CHECK (birthdate < CURRENT_DATE)`. |
| `employmentStatus` | `Enum` | Current employment status. | No additional constraint stated. |

**Database Design prose version:** The table definition lists `employeeID`, `addressID`, `positionID`, `supervisorID`, `firstName`, `lastName`, `contactNumber`, `birthdate`, and `employmentStatus`. It does not list `departmentID` or `governmentID` among its columns. Its constraint section lists `employeeID` as PK; `addressID`, `positionID`, and `supervisorID` as FKs; `employeeID` as unique; and requires `birthdate` to be non-null and earlier than the current date.

**Database Design relationship-matrix version:** The Employee-to-Address, Employee-to-Position, and Employee-to-Supervisor relationships are specified, while the relationship matrix also refers to Government-to-Employee and Position-to-Department. The matrix does not show Employee's `departmentID` or `governmentID` as separate foreign-key entries in the Employee relationship rows, but the Data Dictionary does.

**Interpretation:** `Employee` is the core reference record. The source documents do not settle whether department membership is stored directly in `Employee`, derived through `Position`, or represented in both places. They also do not settle whether the `Government` relationship is held as `Employee.governmentID`, `Government.employeeID`, or both.

### 7.2 `Address`

**Purpose:** Employee address information, including city and region identifiers and the physical address components.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `addressID` | `SmallInt`, size 5 | Unique address record identifier. | Primary Key; Unique. |
| `cityID` | `SmallInt`, size 3 | Linked city identifier. | Foreign Key. |
| `regionID` | `TinyInt`, size 1 | Linked region identifier. | Foreign Key. |
| `building` | `Varchar(100)` | Building name or number. | No additional constraint stated. |
| `street` | `Varchar(150)` | Street name. | No additional constraint stated. |
| `postalCode` | `Varchar(5)` | Postal or ZIP code. | No additional constraint stated. |

**Database Design prose version:** The Address table lists `addressID`, `cityID`, `building`, `street`, and `postalCode`; it omits `regionID`. It lists `addressID` as PK and `cityID` as FK, with no check constraint.

**ERD image version:** The diagram contains both `cityID` and `regionID` as foreign keys in Address. It also shows `Cities.regionID` linking Cities to Regions. The relationship matrix specifies Address → Cities as N:1, but does not separately list Address → Regions.

### 7.3 `Department`

**Purpose:** Identifies an organisational department.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `departmentID` | `TinyInt` | Unique department identifier. | Primary Key; Unique. |
| `departmentName` | `Varchar(100)` | Name of the department. | No additional constraint stated. |

**Database Design prose version:** Identical two-column inventory. `departmentID` is the PK and unique identifier. There is no FK and no check constraint. The relationship matrix connects `Position.departmentID` to `Department.departmentID`, cardinality N:1.

### 7.4 `Position`

**Purpose:** Identifies an employee's position and the department to which the position belongs.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `positionID` | `TinyInt` | Unique position identifier. | Primary Key; Unique. |
| `departmentID` | `TinyInt` | Linked department identifier. | Foreign Key. |
| `positionName` | `Varchar(100)` | Name of the position. | No additional constraint stated. |

**Database Design prose version:** Identical field inventory. `positionID` is PK; `departmentID` is FK; `positionID` uniquely identifies each position; no check constraint is stated. The relationship matrix gives Position → Department a cardinality of N:1.

### 7.5 `Supervisor`

**Purpose:** Records a supervisor identity, the employee record associated with that supervisor, and departmental association.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `supervisorID` | `SmallInt`, size 5 | Unique supervisor record identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key. |
| `departmentID` | `TinyInt` | Linked department identifier. | Foreign Key. |

**Database Design prose version:** The table definition lists only `supervisorID` and `employeeID`. It gives `supervisorID` as PK, `employeeID` as FK, and says the supervisor identifier uniquely identifies each supervisor record. It does not list `departmentID` in that prose column table.

**ERD image version:** The Supervisor entity contains `supervisorID` (PK), `employeeID` (FK), and `departmentID` (FK). The relationship matrix says Supervisor → Employee is 1:1 and describes supervisors as employees themselves. The Data Dictionary's purpose description also mentions departmental associations.

### 7.6 `Schedule`

**Purpose:** Employee work schedule, including start time, required daily work hours, and weekly rest day.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `scheduleID` | `SmallInt`, size 5 | Unique schedule identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key; Unique. |
| `startTime` | `Time` | Scheduled work start time. | `NOT NULL`. |
| `requiredDailyHours` | `TinyInt`, size 2 | Number of work hours required per day. | `Min(1)`, `Max(24)`, `NOT NULL`. |
| `restDay` | `SET` | Assigned weekly rest day. | `NOT NULL`. |

**Database Design prose version:** It lists the same five columns and says `scheduleID` is PK, `employeeID` is FK, and a schedule uniquely represents each unique `employeeID` record. Its check-constraint wording says `startTime` and `restDay` must be supplied (`NOT NULL`) and `requiredDailyHours` must be between 1 and 24.

**ERD image version:** The diagram instead shows `restDayID` (FK) in Schedule and a separate `Rest Day` entity, with `restDayID` as its PK and seven Boolean weekday columns. This conflicts with the `restDay SET` field in the prose and Data Dictionary. See the detailed discrepancy note in Section 16.

### 7.7 `Leave Balance`

**Purpose:** Maintains the available balance of a particular leave type for an employee, using the employee's schedule as the employee association.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `balanceID` | `Integer` | Unique leave balance identifier. | Primary Key; Unique. |
| `leaveType` | `Enum` | Linked leave type identifier. | Foreign Key. |
| `scheduleID` | `SmallInt`, size 5 | Linked schedule identifier for the employee. | Foreign Key. |
| `currentBalance` | `Decimal(4,2)` | Balance of the leave type under the employee. | `Min(0)`. |
| `lastUpdated` | `DateTime` | Update timestamp. | No additional constraint stated. |

**Database Design additions:**

- PK: `balanceID`.
- FKs: `leaveType` and `scheduleID`.
- Unique: the combination `(leaveType, scheduleID)` must be unique, so the same leave type is not represented more than once for the same schedule.
- Check: `currentBalance` must be greater than or equal to zero.

The Database Design description says `balanceID` uniquely identifies each leave type balance for each `scheduleID`. The diagram represents Leave Balance as connected to Leave Type and Schedule.

---

## 8. Complete data dictionary: Logging

The Logging group records employee changes, attendance, leave requests, and accrual history. These records supply information used by Schedule and Time Tracking, employee audit review, leave management, and payroll calculations.

### 8.1 `Changelog`

**Purpose:** Audit history of changes to employee-linked information.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `changelogNo` | `Integer` | Unique changelog entry identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key. |
| `createdAt` | `DateTime` | Record creation timestamp. | `NOT NULL`; default `CURRENT_TIMESTAMP`. |
| `appenderID` | `SmallInt`, size 5 | Identifier for the entry creator. | No additional constraint stated. |
| `updatedAt` | `DateTime` | Timestamp of the last update. | `NOT NULL`; default `CURRENT_TIMESTAMP`. |
| `editorID` | `SmallInt`, size 5 | Identifier for the entry editor. | No additional constraint stated. |
| `fieldCode` | `Varchar(10)` | Shortened identifier for a table and column, e.g. `P_NP` for `Payroll.NetPay`. | No additional constraint stated. |
| `dataType` | `Enum` | Defines the type of the old and new values. | No additional constraint stated. |
| `oldValue` | `Varchar(255)` | Previous value before the change, stored as text. | No additional constraint stated. |
| `newValue` | `Varchar(255)` | Updated value after the change, stored as text. | No additional constraint stated. |

**Database Design additions:** `changelogNo` is PK; `employeeID` is FK; `changelogNo` uniquely identifies each change logged for employee-tied information. The check-constraint prose says `createdAt` and `updatedAt` must be non-null and have `CURRENT_TIMESTAMP` assigned if empty. The Data Dictionary represents the same timestamp rule as field-level `NOT NULL` plus default `CURRENT_TIMESTAMP`.

The DFD has a `D2.3 Changelog` store. Employee-information updates are appended to the changelog, and the Level 1/Level 2 diagrams show viewing and appending changelog information. The archival DFD includes Changelogs in the `Compile (1YO) Records` group.

### 8.2 `Attendance`

**Purpose:** Attendance time records tied to an employee's schedule.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `attendanceID` | `Integer` | Unique attendance record identifier. | Primary Key; Unique. |
| `scheduleID` | `SmallInt`, size 5 | Linked schedule identifier. | Foreign Key. |
| `date` | `Date` | Attendance date. | Unique in combination with `scheduleID`. |
| `timeIn` | `Time` | Time when the employee clocked in. | Check: `timeIn <= timeOut`. |
| `timeOut` | `Time` | Time when the employee clocked out. | Check: `timeIn <= timeOut`. |
| `dayType` | `Enum` | Type of day. | No additional constraint stated. |

**Database Design additions:** `attendanceID` is PK; `scheduleID` is FK; `(scheduleID, date)` must be unique; `timeIn` must be less than or equal to `timeOut`.

**DFD role:** Attendance is stored as `D2.1 Attendance` in the employee-management Level 2 view and `D2.1 Attendance Logs` in the payroll and archival views. It receives employee attendance and time-in/time-out data. The payroll Level 2 diagram derives work hours and other attendance data from these logs for Timesheet Validation. The archival diagram compiles Attendance Logs as part of `1YO` records.

### 8.3 `Leave`

**Purpose:** Employee leave requests, including the leave type, duration, justification, status, credits, request timestamp, approval timestamp, and approving supervisor identifier.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `leaveID` | `Integer` | Unique leave record identifier. | Primary Key; Unique. |
| `balanceID` | `Integer` | Linked leave balance record identifier. | Foreign Key. |
| `leaveType` | `Enum` | Category of leave. | Foreign Key. |
| `scheduleID` | `SmallInt`, size 5 | Linked schedule identifier. | Foreign Key. |
| `dateStart` | `Date` | Leave start date. | `NOT NULL`; check `dateStart >= CURRENT_DATE`. |
| `hourDuration` | `SmallInt` | Duration of leave in hours. | `Min(0)`. |
| `leaveReason` | `Varchar(150)` | Justification for the leave request. | No additional constraint stated. |
| `leaveStatus` | `Enum` | Approval status. | No additional constraint stated. |
| `requestLeaveCredit` | `Decimal(4,2)` | Credits that will be consumed. | No additional constraint stated. |
| `remainingLeaveCredit` | `Decimal(4,2)` | Credits remaining after use. | No additional constraint stated. |
| `dateRequested` | `DateTime` | Timestamp of leave-request submission. | Default `CURRENT_TIMESTAMP`. |
| `dateApproved` | `DateTime` | Timestamp that remains null until the request is approved. | `NULL` allowed. |
| `approverID` | `SmallInt`, size 5 | Tracks the Supervisor ID who approved the leave. | No additional constraint stated. |

**Database Design version:** It lists all the above fields except `scheduleID`. It lists `leaveID` as PK and `balanceID` plus `leaveType` as FKs. Its check says `dateStart` must be non-null and on or after the current date. The Data Dictionary includes a `scheduleID` FK which is absent from the Database Design prose column list and table-level FK summary.

**DFD role:** `D2.2 Leave` is used by Schedule and Time Tracking, and is read to derive paid-leave details for Timesheet Validation. Supervisors handle leave requests. The archival DFD includes Leave Logs in the `1YO` compilation group.

### 8.4 `Leave Accrual History`

**Purpose:** Historical record of leave-credit accrual and the balance before and after each accrual.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `accrualID` | `Integer` | Unique leave accrual record identifier. | Primary Key; Unique. |
| `balanceID` | `Integer` | Linked leave balance record identifier. | Foreign Key. |
| `leaveType` | `Enum` | Category of leave for accrual purposes. | `NOT NULL`. |
| `accrualDate` | `Date` | Date when the leave accrual is recorded and applied. | `NOT NULL`; check `accrualDate <= CURRENT_DATE`. |
| `creditAccrued` | `Decimal(4,2)` | Number of leave credits accrued until the accrual date. | `Min(0)`. |
| `oldBalance` | `Decimal(4,2)` | Leave balance prior to the accrual. | `Min(0)`. |
| `newBalance` | `Decimal(4,2)` | Leave balance after the accrual. | `Min(0)`. |

**Database Design additions:** `accrualID` is PK; `balanceID` is FK; the unique identifier distinguishes each accrual instance; `accrualDate` must be present and no later than the current date; `creditAccrued`, `oldBalance`, and `newBalance` must each be non-negative.

**Relationship notes:** The relationship matrix lists Leave Accrual History → Leave Balance as N:1 and Leave Accrual History → Leave Type as N:1. The `balanceID` FK is listed in the table's constraint section, while the `leaveType` association is named in the matrix and the ERD. The Data Dictionary makes `leaveType` non-null but does not explicitly mark it as an FK in the field constraint column.

---

## 9. Complete data dictionary: Government Details

### 9.1 `Government`

**Purpose:** Holds each employee's government-related identifiers and an encrypted reference field.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `governmentID` | `SmallInt`, size 5 | Unique government record identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key. |
| `TIN` | `Varchar(15)` | Taxpayer Identification Number. | No additional constraint stated. |
| `sssNo` | `Varchar(12)` | Social Security System number. | No additional constraint stated. |
| `philHealthNo` | `Varchar(12)` | PhilHealth identifier. | No additional constraint stated. |
| `pagIbigNo` | `Varchar(12)` | Pag-ibig fund identifier. | No additional constraint stated. |
| `encryptedRef` | `Varbinary(Max)` | Encrypted reference data. | No additional constraint stated. |

**Database Design additions:** `governmentID` is PK; `employeeID` is FK; `governmentID` uniquely identifies each government record; no check constraint is stated. The relationship matrix specifies Government → Employee as 1:1. The schema diagram colours Government red, corresponding to `Additional Security Required` in the legend.

**Security relevance:** The overview says employee PII should be inaccessible unless two authorised personnel approve it, and that only corresponding contribution values are normally visible and accessible. The DFD has a separate Government Info Access process and PII data store, with access approval, on-approval viewing, on-approval updating, and record-modification flows. The source does not state the encryption algorithm, key-management procedure, masking format, or exact fields that qualify for ordinary versus approval-controlled display.

### 9.2 `Government Deductibles`

**Purpose:** Holds monthly employer-side and employee-side contribution amounts and withholding tax for a government record.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `governmentID` | `SmallInt`, size 5 | Linked government record identifier. | Primary Key; Foreign Key. |
| `monthID` | `Enum` | Linked month identifier. | Foreign Key. |
| `employerContribution` | `Decimal(19,4)` | Employer contribution amount. | `Min(0)`. |
| `employeeContribution` | `Decimal(19,4)` | Employee contribution amount. | `Min(0)`. |
| `withholdingTax` | `Decimal(19,4)` | Amount of tax withheld. | `Min(0)`. |

**Database Design key definition:** The Database Design explicitly states the composite primary key `(governmentID, monthID)`. Both columns are treated as foreign keys in its constraint table, referencing Government and Months respectively. Its unique-constraint description repeats that `(governmentID, monthID)` uniquely identifies deductibles under each government record. Contribution amounts must be greater than or equal to zero.

**Relationship matrix:** Government Deductibles → Months is N:1; Government Deductibles → Government is 1:1. The ERD marks both `monthID` and `governmentID` as key fields, and both are marked as foreign keys.

**DFD role:** This information is computed in process `2.4 Government Deductibles`, using the tax table and rates, then appended to `D3.2 Benefits & Deductibles` and collated into the monthly summary process.

### 9.3 `Contributions`

**Purpose:** Records PhilHealth, Pag-ibig, and SSS amounts for a named contributor associated with a government record.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `governmentID` | `SmallInt`, size 5 | Linked government record identifier. | Composite Primary Key; Foreign Key. |
| `contributor` | `Enum` | Identifier for the contributor. | Composite Primary Key. |
| `philHealthContribution` | `Decimal(19,4)` | PhilHealth contribution amount. | `Min(0)`. |
| `pagIbigContribution` | `Decimal(19,4)` | Pag-ibig contribution amount. | `Min(0)`. |
| `sssContribution` | `Decimal(19,4)` | SSS contribution amount. | `Min(0)`. |

**Database Design constraints:** Composite PK `(governmentID, contributor)`; `governmentID` is an FK; the composite pair uniquely identifies a government contribution per employee and employer; all contribution amounts must be greater than or equal to zero.

**Relationship matrix:** Contributions → Government Deductibles is shown as N:1, linked by `governmentID`, with the purpose that this is the reference location where computed contributions are collated. The wording does not explain how the `contributor` values are enumerated or whether employee and employer are the only allowed enum members, though the surrounding descriptions refer to both employee and employer shares.

---

## 10. Complete data dictionary: Payroll

The Payroll group contains the main payroll record, payslip, leave monetisation, earnings, deductions, miscellaneous financial transactions, government tallies, and monthly summary records.

### 10.1 `Payroll`

**Purpose:** Main employee payroll record linking an employee, wage calculation, deduction/rebate calculation, summary, payroll month, salary rates, PTO maximum, and net pay.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `payrollID` | `Integer` | Unique payroll record identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key. |
| `rebateID` | `Integer` | Linked rebate record identifier. | Foreign Key. |
| `wageID` | `Integer` | Linked wage record identifier. | Foreign Key. |
| `summaryID` | `Integer` | Linked summary record identifier. | Foreign Key. |
| `monthID` | `Enum` | Identifier for the payroll month. | No additional constraint stated in the Data Dictionary. |
| `ptoMax` | `Integer` | Maximum allowed paid time off (PTO). | `Min(0)`. |
| `hourlyRate` | `Decimal(19,4)` | Pay rate per hour. | `Min(0)`; `NOT NULL`. |
| `dailyRate` | `Decimal(19,4)` | Fixed daily pay rate. | `Min(0)`; `NOT NULL`. |
| `monthlyRate` | `Decimal(19,4)` | Monthly salary rate. | `Min(0)`. |
| `netPay` | `Decimal(19,4)` | Final pay after deductions. | `Min(0)`; `NOT NULL`. |

**Database Design constraints:** PK `payrollID`; FKs `employeeID`, `rebateID`, `wageID`, and `summaryID`; `payrollID` is described as uniquely identifying each payroll per employee per period; `hourlyRate`, `dailyRate`, and `netPay` must be present (`NOT NULL`); `ptoMax` must be greater than or equal to zero.

**Relationship matrix:**

- Payroll → Employee: N:1.
- Payroll → Deductibles: 1:1 through `rebateID`.
- Payroll → Earnings: 1:1 through `wageID`.
- Payroll → Summary: N:1 through `summaryID`.
- Payroll → Months: N:1 through `monthID`.

The table's FK summary does not list `monthID` as an FK, although the relationship matrix explicitly connects Payroll to Months through that field. This discrepancy is retained in Section 16.

### 10.2 `Payslip`

**Purpose:** Stores the pay-period record and the employee's hours, days, gross income, and payslip identifier.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `payslipID` | `Integer` | Unique payslip identifier. | Primary Key; Unique. |
| `payrollID` | `Integer` | Linked payroll record identifier. | Foreign Key. |
| `periodStartDate` | `Date` | Start date of the pay period. | `NOT NULL`. |
| `periodEndDate` | `Date` | End date of the pay period. | Unique in combination with `payrollID`. |
| `regularHours` | `SmallInt` | Standard work hours. | `Min(0)`; `NOT NULL`. |
| `overtimeHours` | `SmallInt` | Overtime hours worked. | `Min(0)`. |
| `holidayOvertimeHours` | `SmallInt` | Overtime during holidays. | `Min(0)`. |
| `ptoHours` | `SmallInt` | Total paid time off hours taken. | `Min(0)`. |
| `undertimeHours` | `SmallInt` | Hours below expected work time. | `Min(0)`. |
| `daysWorked` | `TinyInt` | Total days worked. | `Min(0)`. |
| `holidaysWorked` | `TinyInt` | Number of holidays worked. | `Min(0)`. |
| `restDaysWorked` | `TinyInt` | Number of rest days worked. | `Min(0)`. |
| `grossIncome` | `Decimal(19,4)` | Total earnings before deductions. | `Min(0)`. |

**Database Design constraints:** PK `payslipID`; FK `payrollID`; `(payrollID, periodEndDate)` must be unique; hours and days worked must be greater than or equal to zero. The design text also says the payslip identifier uniquely identifies each payslip given to an employee per payroll.

**Relationship matrix:** Payslip → Payroll is 1:1 through `Payslip.payrollID`. The DFD's process `2.6 Payslip Generation & Distribution` appends generated payslip records to `D3.4 Payslip`, and gives Employee, Supervisor, and HR viewing flows. The Database Design overview says a semi-monthly payslip is provided for each employee.

### 10.3 `Leave Monetisation`

**Purpose:** Records accrued leave credits converted into monetary value, including the balance source, conversion amount, assigned monetary value, total, date, and approver.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `monetisationID` | `Integer` | Unique leave monetisation record identifier. | Primary Key; Unique. |
| `leaveType` | `Enum` | Type of leave being monetised. | `NOT NULL`. |
| `balanceID` | `Integer` | Linked leave balance record identifier for the monetisation. | Foreign Key. |
| `creditsMonetised` | `Decimal(4,2)` | Number of leave credits converted to monetary value. | `Min(0)`. |
| `monetaryValuePer` | `Decimal(19,4)` | Monetary value assigned per leave credit. | `Min(0)`. |
| `totalConverted` | `Decimal(19,4)` | Aggregate monetary value resulting from conversion. | `Min(0)`. |
| `dateMonetised` | `Date` | Date when leave monetisation was effected. | No additional constraint stated. |
| `approverID` | `SmallInt`, size 5 | Identifier for the approver responsible for validating the monetisation. | No additional constraint stated. |

**Database Design constraints:** PK `monetisationID`; FK `balanceID`; leave type must be provided (`NOT NULL`); `creditsMonetised`, `monetaryValuePer`, and `totalConverted` must be non-negative. The relationship matrix also lists Leave Monetisation → Leave Type as N:1, even though the table-level FK summary lists only `balanceID`.

**DFD role:** Supervisor → Process 2.2 is labelled `Process Approved Leave Monetisation`; Process 2.2 appends to `D2.4 Leave Monetisation Logs` and sends `Add to Benefits` to Process 2.3 Wage and Adjustments. The DFD thus presents a leave-monetisation event as an approved operation that is logged and then incorporated into employee benefits.

### 10.4 `Earnings`

**Purpose:** Holds the wage-side benefit and remuneration components.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `wageID` | `Integer` | Unique wage record identifier. | Primary Key; Unique. |
| `overtimePay` | `Decimal(19,4)` | Payment for overtime hours. | `Min(0)`. |
| `ptoPay` | `Decimal(19,4)` | Paid time off compensation. | `Min(0)`. |
| `holidayPay` | `Decimal(19,4)` | Payment for holiday work. | `Min(0)`. |
| `restDayPay` | `Decimal(19,4)` | Payment for work on rest days. | `Min(0)`. |
| `riceSubsidy` | `Decimal(19,4)` | Subsidy provided for rice. | `Min(0)`. |
| `phoneStipend` | `Decimal(19,4)` | Stipend for phone expenses. | `Min(0)`. |
| `clothesStipend` | `Decimal(19,4)` | Stipend for clothing expenses. | `Min(0)`. |
| `otherIncentTotal` | `Decimal(19,4)` | Sum of other incentives. | `Min(0)`. |
| `totalBenefits` | `Decimal(19,4)` | Total benefit amount. | `Min(0)`. |

**Database Design constraints:** PK `wageID`; no FK is listed on Earnings; `wageID` uniquely identifies benefit calculations per payroll; all monetary amounts must be greater than or equal to zero. Payroll refers to Earnings through `Payroll.wageID`, and the relationship matrix says Payroll → Earnings is 1:1.

### 10.5 `Deductibles`

**Purpose:** Stores the other-deduction total and aggregate deductible amount, associated with a government record.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `rebateID` | `Integer` | Unique rebate record identifier. | Primary Key; Unique. |
| `governmentID` | `SmallInt`, size 5 | Linked government record identifier. | Foreign Key. |
| `otherDeductTotal` | `Decimal(19,4)` | Sum of other deductions. | `Min(0)`. |
| `totalDeductibles` | `Decimal(19,4)` | Overall deductions sum. | `Min(0)`. |

**Database Design constraints:** PK `rebateID`; FK `governmentID`; `rebateID` uniquely identifies deductible calculations per payroll, including government deductibles; deduction amounts must be non-negative.

**Relationship matrix:** Payroll → Deductibles is 1:1 through `Payroll.rebateID`. Deductibles → Government Deductibles is also shown as 1:1 through `Deductibles.governmentID`, with the purpose that government deductibles are needed to compute total deductibles.

### 10.6 `Other Transactions`

**Purpose:** Stores miscellaneous payroll-related financial transactions such as bonuses, advances, reimbursements, and additional deductions.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `transactionID` | `Integer` | Unique transaction record identifier. | Primary Key; Unique. |
| `payrollID` | `Integer` | Payroll identifier for the transaction. | Foreign Key. |
| `transactionType` | `Enum` | Type of transaction. | No additional constraint stated. |
| `transactionAmount` | `Decimal(19,4)` | Amount of the transaction. | `Min(0)`. |
| `description` | `Varchar(150)` | Description of the transaction. | No additional constraint stated. |

**Database Design constraints:** PK `transactionID`; FK `payrollID`; the identifier uniquely distinguishes miscellaneous calculations per payroll; `transactionAmount` must be non-negative. The relationship matrix gives Other Transactions → Payroll a cardinality of N:1.

### 10.7 `Government Tally`

**Purpose:** Stores the overall Pag-ibig, PhilHealth, and SSS totals for each contributor within a monthly summary.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `summaryID` | `Integer` | Linked summary record identifier. | Primary Key is marked as “assumed” in the Data Dictionary. |
| `contributor` | `Enum` | Identifier for the contributor. | No additional constraint stated in the Data Dictionary. |
| `overallPagIbig` | `Decimal(19,4)` | Overall Pag-ibig contribution between this month's payrolls. | `Min(0)`. |
| `overallPhilHealth` | `Decimal(19,4)` | Overall PhilHealth contribution between this month's payrolls. | `Min(0)`. |
| `overallSSS` | `Decimal(19,4)` | Overall SSS contribution between this month's payrolls. | `Min(0)`. |

**Database Design constraints and ERD:** Composite PK `(summaryID, contributor)`; `summaryID` is an FK; the pair uniquely identifies overall totals for each government contribution per employee and employer; all overall contribution amounts must be non-negative. The ERD visually marks both `summaryID` and `contributor` as key fields, with `summaryID` also marked as an FK.

**Relationship matrix:** Government Tally → Summary is N:1 through `Government Tally.summaryID`. The stated purpose is to sum government-related disbursements within a summary.

**Important conflict:** The Data Dictionary labels `summaryID` as an assumed primary key, while the Database Design prose and ERD show a composite key including `contributor`. The composite-key version is clearly represented in the latter two sources, but this synthesis does not overwrite the Data Dictionary's recorded assumption.

### 10.8 `Summary`

**Purpose:** Stores monthly aggregated payroll, benefit, employee/employer disbursement, government contribution, tax, and government-disbursement totals.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `summaryID` | `Integer` | Unique summary record identifier. | Primary Key; Unique. |
| `monthID` | `Enum` | Linked month identifier. | No additional constraint stated in the Data Dictionary. |
| `overallGrossIncome` | `Decimal(19,4)` | Total gross income summed. | `Min(0)`. |
| `overallBenefits` | `Decimal(19,4)` | Total benefits summed. | `Min(0)`. |
| `employeeDisbursementTotal` | `Decimal(19,4)` | Total disbursements to employees. | `Min(0)`. |
| `employerDisbursementTotal` | `Decimal(19,4)` | Total amount disbursed by the employer. | `Min(0)`. |
| `overallPITotal` | `Decimal(19,4)` | Combined Pag-ibig total. | `Min(0)`. |
| `overallPHTotal` | `Decimal(19,4)` | Combined PhilHealth total. | `Min(0)`. |
| `overallSSSTotal` | `Decimal(19,4)` | Combined SSS total. | `Min(0)`. |
| `overallTaxTotal` | `Decimal(19,4)` | Total tax withheld summed. | `Min(0)`. |
| `governmentDisbursementTotal` | `Decimal(19,4)` | Total government disbursement summed. | `Min(0)`. |

**Database Design constraints:** PK `summaryID`; FK `monthID`; `summaryID` uniquely identifies monthly enumerative reports; all monetary totals must be non-negative.

**Relationship matrix:** Payroll → Summary is N:1; Government Tally → Summary is N:1. The monthly report process collates information and generates the `D3.5 Monthly Summary Report` store. The Database Design overview calls for a monthly summary for each payroll processing cycle.

---

## 11. Complete data dictionary: Static-Update-Only tables

These entities hold reference values, calendar data, contribution-rate data, tax-bracket data, and leave-type definitions. The Data Dictionary calls this group `E. Static-Update Only`; its column-definition subsection labels it `F. Static-Update Only`.

### 11.1 `Regions`

| Column | Data type / size | Description | Constraint |
|---|---|---|---|
| `regionID` | `TinyInt`, size 1 | Unique region identifier. | Primary Key; Unique. |
| `regionName` | `Varchar(100)` | Name of the region. | No additional constraint stated. |

**Database Design:** Same two columns. `regionID` is PK, no FK, unique by identifier, and no check constraint is stated.

### 11.2 `Cities`

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `cityID` | `SmallInt`, size 3 | Unique city identifier. | Primary Key; Unique. |
| `regionID` | `TinyInt` | Linked region identifier. | Foreign Key. |
| `cityName` | `Varchar(100)` | Name of the city. | Unique. |

**Database Design:** Same three columns; PK `cityID`; FK `regionID`; `cityName` must have only one instance between records; no check constraint. Relationship matrix does not list a separate `Cities → Regions` row, but the `regionID` foreign-key column is present in the table and ERD.

### 11.3 `Holiday`

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `holidayID` | `TinyInt` | Unique holiday record identifier. | Primary Key; Unique. |
| `holidayName` | `Varchar(255)` | Name of the holiday. | Unique. |
| `holidayDate` | `Date` | Date of the holiday. | Unique. |

**Database Design version:** PK `holidayID`; no FK; `holidayID` uniquely identifies each listed holiday; `(holidayName, holidayDate)` must be unique as a combination; no check constraint.

**Important difference:** The Data Dictionary marks `holidayName` and `holidayDate` individually as unique fields. The Database Design instead specifies a composite unique combination of name and date. These are not equivalent rules: independent uniqueness prevents two holidays from sharing a name or date, while pair uniqueness prevents only a duplicate name/date pair.

### 11.4 `Months`

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `monthID` | `Enum` | Unique month identifier. | The field is described as an enum; no additional field-level constraint stated. |
| `monthlyWorkdays` | `Integer`, size 2 | Number of workdays in the month. | `Min(1)`, `Max(31)`. |

**Database Design constraints:** PK `monthID`; no FK; no separate unique constraint because `monthID` is already an Enum; `monthlyWorkdays` must be between 1 and 31.

### 11.5 `Rates`

**Purpose:** Holds statutory rate values for Pag-ibig, PhilHealth, and SSS.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `piEmployerRate` | `Decimal(5,3)` | Global employer Pag-ibig rate. | `Min(0)`; `NOT NULL`. |
| `piEmployeeRate` | `Decimal(5,3)` | Global employee Pag-ibig rate. | `Min(0)`; `NOT NULL`. |
| `piMaxContribution` | `Decimal(5,3)` | Maximum Pag-ibig contribution. | `Min(0)`; `NOT NULL`. |
| `phRate` | `Decimal(5,3)` | Global PhilHealth rate. | `Min(0)`; `NOT NULL`. |
| `sssRate` | `Decimal(5,3)` | Global SSS rate. | `Min(0)`; `NOT NULL`. |

**Database Design additional field:** The Database Design prose table and ERD include `implementationYear`, data type `SmallInt` (shown as size 4 in the diagram), described as the year the rates were implemented. The Data Dictionary omits this field.

**Database Design constraint conflict:** Its prose table says PK `None`, FK `None`, Unique `None`; the check-constraint text says all fields must be provided (`NOT NULL`) and rates must be greater than or equal to zero. The ERD, however, visibly marks `implementationYear` as `PK`. The documents do not resolve whether implementation year is intended to identify each rate set.

**DFD role:** `D3.3 Tax Table & Rates` provides the rates used by Process `2.4 Government Deductibles` to compute contributions. The System Admin has an `Update Rates (opt*)` flow to the store.

### 11.6 `Tax Table`

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `taxBracket` | `Integer` | Identifier for the tax bracket. | `Min(1)`; Primary Key. |
| `minCompensation` | `Decimal(19,4)` | Minimum income for the bracket. | `Min(0)`; `NOT NULL` in the Database Design constraints. |
| `maxCompensation` | `Decimal(19,4)` | Maximum income for the bracket. | `Min(0)`; `NOT NULL` in the Database Design constraints. |
| `bracketWidth` | `Decimal(19,4)` | Income range width for the bracket. | `Min(0)`. |
| `taxRate` | `Decimal(19,4)` | Tax rate for the bracket. | `Min(0)`; `NOT NULL` in the Database Design constraints. |

**Database Design constraints:** PK `taxBracket`; no FK; `taxBracket` uniquely identifies the withholding-tax criteria and consequent requirement. The check-constraint wording says `taxBracket` must “at least have and be greater than 1” (the source wording is unclear), that `minCompensation`, `maxCompensation`, and `taxRate` must be present, and that monetary values must be non-negative.

**Unresolved wording:** The Data Dictionary's `Min(1)` indicates a minimum of 1. The Database Design's prose has a malformed phrase that could be read as “at least have and be greater than 1”. It is unsafe to silently convert this to `>= 1` or `> 1` without an implementation decision.

### 11.7 `Leave Type`

**Purpose:** Defines the leave categories and their accrual rules.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `leaveType` | `Enum` | Classification of leave. | `NOT NULL`. |
| `mandatory` | `Boolean` | Indicates whether the leave type is mandatory. | No additional constraint stated. |
| `defaultAccrualRate` | `Decimal(4,2)` | Standard rate at which leave accrues for this type. | `Min(0)`. |
| `accrualFrequency` | `Enum` | Frequency of leave accrual. | No additional constraint stated. |
| `maxAccrual` | `Decimal(4,2)` | Maximum allowable accrual for the leave type. | `Min(0)`. |

**Database Design constraints:** PK `leaveType`; no FK; no separate unique constraint because `leaveType` is already an Enum; `defaultAccrualRate` and `maxAccrual` must be greater than or equal to zero.

---

## 12. Additional table in the database-design submission: `Rest Day`

**Source:** Database Design PDF, Section 3.7, and schema diagram on page 5. This table is not included in the Data Dictionary's table inventory or column-definition section.

| Column | Data type / size | Description | Database Design constraint |
|---|---|---|---|
| `restDayID` | `SmallInt(5)` | Unique identifier for the rest-day record. | Primary Key; Unique. |
| `sunday` | `Boolean` | Indicates whether Sunday is designated as a rest day. | No check constraint stated. |
| `monday` | `Boolean` | Indicates whether Monday is designated as a rest day. | No check constraint stated. |
| `tuesday` | `Boolean` | Indicates whether Tuesday is designated as a rest day. | No check constraint stated. |
| `wednesday` | `Boolean` | Indicates whether Wednesday is designated as a rest day. | No check constraint stated. |
| `thursday` | `Boolean` | Indicates whether Thursday is designated as a rest day. | No check constraint stated. |
| `friday` | `Boolean` | Indicates whether Friday is designated as a rest day. | No check constraint stated. |
| `saturday` | `Boolean` | Indicates whether Saturday is designated as a rest day. | No check constraint stated. |

**Constraints:** PK `restDayID`; no FK; the identifier uniquely identifies each record in the Rest Day table; no check constraint is stated.

**Schema role:** The ERD shows `Schedule.restDayID` as an FK to `Rest Day.restDayID`. This makes a schedule point to a row that can flag one or more weekdays as rest days. That is structurally different from the Data Dictionary's single `Schedule.restDay` field of type `SET`. The Database Design prose for Schedule also lists `restDay` rather than `restDayID`, so this remains an internal inconsistency in the database-design submission itself.

---

## 13. Relationship catalogue

This section reproduces the relationship matrix in the Database Design submission. The source calls out the relationship, cardinality, foreign-key column, and purpose. The cardinalities below are not recalculated from the column types; they are the cardinalities stated in the document.

### 13.1 Employee, address, position, and supervision relationships

| Relationship | Cardinality stated | Foreign-key field | Purpose stated |
|---|---:|---|---|
| Employee → Address | 1:1 | `Employee.addressID` | Each employee is assigned an address. |
| Employee → Position | N:1 | `Employee.positionID` | Every employee holds a specific position. |
| Employee → Supervisor | N:1 | `Employee.supervisorID` | References the supervisor of the employee. |
| Address → Cities | N:1 | `Address.cityID` | Each address entry is located within a specific city. |
| Position → Department | N:1 | `Position.departmentID` | Positions are assigned within departments. |
| Supervisor → Employee | 1:1 | `Supervisor.employeeID` | Supervisors are employees themselves, connecting both tables directly. |
| Schedule → Employee | 1:1 | `Schedule.employeeID` | Each employee is assigned a schedule. |
| Changelog → Employee | N:1 | `Changelog.employeeID` | Tracks changes made to employee information. |

The Employee → Supervisor and Supervisor → Employee entries describe two directions of the supervisor/employee association. The relationship matrix gives N:1 for an Employee's supervisor reference and 1:1 for the Supervisor record's link to its Employee record.

### 13.2 Leave and attendance relationships

| Relationship | Cardinality stated | Foreign-key field | Purpose stated |
|---|---:|---|---|
| Leave Balance → Leave Type | N:1 | `LeaveBalance.leaveType` | Indicates the leave type applicable for an employee leave balance. |
| Leave Balance → Schedule | N:1 | `LeaveBalance.scheduleID` | Leave balances are determined based on an employee's schedule. |
| Attendance → Schedule | N:1 | `Attendance.scheduleID` | Attendance records are linked to the schedule. |
| Leave → Leave Type | N:1 | `Leave.leaveType` | Indicates the leave type used for the request. |
| Leave → Leave Balance | N:1 | `Leave.balanceID` | Leave requests are validated against available leave balances. |
| Leave Accrual History → Leave Balance | N:1 | `LeaveAccrualHistory.balanceID` | Accrual records adjust the leave balance over time. |
| Leave Accrual History → Leave Type | N:1 | `LeaveAccrualHistory.leaveType` | Indicates the leave type that has been accrued. |
| Leave Monetisation → Leave Type | N:1 | `LeaveMonetisation.leaveType` | Indicates the leave type requested for conversion. |
| Leave Monetisation → Leave Balance | N:1 | `LeaveMonetisation.balanceID` | Converts leave credits into monetary value based on the leave balance. |

The Data Dictionary's `Leave` field list additionally shows `scheduleID` as a foreign key, but the relationship matrix does not include a separate Leave → Schedule row. The relationship matrix also lists Leave Monetisation → Leave Type, while the table-level constraint summary lists only `balanceID` as the foreign key. These differences are not resolved by the source.

### 13.3 Government and payroll relationships

| Relationship | Cardinality stated | Foreign-key field | Purpose stated |
|---|---:|---|---|
| Government → Employee | 1:1 | `Government.employeeID` | Links each government record with an employee. |
| Payroll → Employee | N:1 | `Payroll.employeeID` | Links payroll records to employees. |
| Payroll → Deductibles | 1:1 | `Payroll.rebateID` | Deduction computations of payroll. |
| Payroll → Earnings | 1:1 | `Payroll.wageID` | Benefit computations of payroll. |
| Payroll → Summary | N:1 | `Payroll.summaryID` | Collates payroll data into a monthly summary record. |
| Payroll → Months | N:1 | `Payroll.monthID` | References payroll month and its corresponding working days. |
| Government Deductibles → Months | N:1 | `GovernmentDeductibles.monthID` | References the month these deductibles are for. |
| Government Deductibles → Government | 1:1 | `GovernmentDeductibles.governmentID` | Associates deductibles with a government record. |
| Contributions → Government Deductibles | N:1 | `Contributions.governmentID` | References the location where computed contribution values are collated. |
| Deductibles → Government Deductibles | 1:1 | `Deductibles.governmentID` | Government deductibles are needed to compute total deductibles. |
| Payslip → Payroll | 1:1 | `Payslip.payrollID` | Each payslip is uniquely tied to a payroll record. |
| Other Transactions → Payroll | N:1 | `OtherTransactions.payrollID` | Connects miscellaneous transactions to payroll records. |
| Government Tally → Summary | N:1 | `GovernmentTally.summaryID` | Sums government-related disbursements within a summary. |

### 13.4 Relationship diagram in compact textual form

The matrix can be read in the following direction, where the arrow points from the source entity named in the matrix to the referenced entity:

```text
Employee --1:1--> Address
Employee --N:1--> Position --N:1--> Department
Employee --N:1--> Supervisor --1:1--> Employee
Schedule --1:1--> Employee
Address --N:1--> Cities
Changelog --N:1--> Employee

Leave Balance --N:1--> Leave Type
Leave Balance --N:1--> Schedule
Attendance --N:1--> Schedule
Leave --N:1--> Leave Type
Leave --N:1--> Leave Balance
Leave Accrual History --N:1--> Leave Balance
Leave Accrual History --N:1--> Leave Type
Leave Monetisation --N:1--> Leave Type
Leave Monetisation --N:1--> Leave Balance

Government --1:1--> Employee
Payroll --N:1--> Employee
Payroll --1:1--> Deductibles
Payroll --1:1--> Earnings
Payroll --N:1--> Summary
Payroll --N:1--> Months
Government Deductibles --N:1--> Months
Government Deductibles --1:1--> Government
Contributions --N:1--> Government Deductibles
Deductibles --1:1--> Government Deductibles
Payslip --1:1--> Payroll
Other Transactions --N:1--> Payroll
Government Tally --N:1--> Summary
```

This compact representation is only a readable restatement of the prose matrix. It does not add relationships to `Rest Day`, `Regions`, or any other table that is not explicitly included in that matrix.

### 13.5 Relationships present in fields or diagrams but absent from the matrix

Several links are visible elsewhere in the source set but are not listed in the relationship matrix:

- `Address.regionID` → `Regions.regionID` appears in the Data Dictionary and ERD.
- `Cities.regionID` → `Regions.regionID` appears in the Cities field definition and ERD.
- `Schedule.restDayID` → `Rest Day.restDayID` appears in the ERD; no Rest Day relationship is stated in the matrix.
- `Employee.governmentID` → `Government.governmentID` appears in the Data Dictionary, but the Database Design instead lists `Government.employeeID` → `Employee.employeeID` as the 1:1 relationship.
- `Employee.departmentID` → `Department.departmentID` appears in the Data Dictionary, but the Database Design's prose Employee table omits the field, and the relationship matrix instead links Position → Department.
- `Supervisor.departmentID` → `Department.departmentID` appears in the Data Dictionary and ERD, but is not separately identified in the relationship matrix.
- `Leave.scheduleID` → `Schedule.scheduleID` appears in the Data Dictionary, but not as a separate relationship-matrix row.
- `Leave Monetisation.leaveType` → `Leave Type.leaveType` appears in the relationship matrix and ERD, although the table-level FK summary only lists `balanceID`.

### 13.6 Foreign-key direction and cardinality cautions

The written matrix uses labels such as `Employee → Address` and `Schedule → Employee`. The FK column shown in each row belongs to the first entity named in that relationship, so the arrow is a convenient way of reproducing the matrix's wording. Do not infer that the arrow means the source entity “owns” or creates the referenced entity.

The matrix also does not always provide enough detail to distinguish optional from mandatory participation. For example, `1:1` alone does not tell the receiving agent whether a record must exist before another record can be created, whether the foreign key may be null, or whether deletion cascades. Those implementation details are not in the uploaded documents.

---

## 14. End-to-end functional traces

This section combines the schema, field definitions, relationship matrix, overview text, and DFD. The traces are helpful for understanding how records are intended to be used together. They are not substitutes for the original labels or a complete transaction specification.

### 14.1 Employee record establishment and maintenance

1. An employee is represented by a unique `employeeID` in `Employee`.
2. The Employee record links to an `Address` record through `addressID` in the Data Dictionary and Database Design relationship matrix.
3. Position and department information are represented through `Position` and `Department`. The Data Dictionary places `departmentID` directly in Employee; the Database Design prose omits it and the relationship matrix connects Position to Department.
4. A supervisor relationship is recorded through `supervisorID` in Employee and an associated Supervisor record. The relationship matrix says a Supervisor is an Employee and gives Supervisor → Employee a 1:1 cardinality.
5. A `Government` record stores TIN, SSS, PhilHealth, and Pag-ibig identifiers, along with an encrypted reference field. The relationship matrix gives Government → Employee a 1:1 association.
6. The employee's schedule is recorded in `Schedule`, linked to Employee. A schedule records start time, required daily hours, and rest-day information. The representation of rest days differs between the Data Dictionary (`SET`) and the ERD (`restDayID` pointing to Rest Day).
7. Employee details are maintained through Employee Management. The DFD shows employee address and job-profile retrieval/update flows, plus employee-information changes appended to Changelog.
8. If government PII is viewed or changed, the DFD routes it through Government Info Access and labels the operation as approval-controlled.

### 14.2 Attendance capture and timesheet validation

1. Employee attendance and time-in/time-out data enter the system.
2. The Employee Management Level 2 diagram routes the data through `1.2 Schedule and Time Tracking`.
3. The process views and appends/modifies `D2.1 Attendance` and `D2.2 Leave`.
4. The relational schema stores attendance rows in `Attendance`, each linked to a `Schedule` record using `scheduleID`.
5. The Attendance design requires `(scheduleID, date)` to be unique and requires `timeIn <= timeOut`.
6. In the payroll Level 2 diagram, `2.1 Timesheet Validation` reads Attendance Logs and derives work hours and other attendance data.
7. The same process reads Leave Logs and derives details for paid leaves.
8. The process sends `Get Time Activity` to `2.5 Basic Payroll Computation`.

The source documents do not describe a complete attendance validation algorithm. They do not state how to handle an overnight shift where `timeIn` occurs on one date and `timeOut` on the next, missing clock events, multiple clock-ins, or manual corrections beyond the fact that supervisors may adjust team attendance and the store can be appended/modified.

### 14.3 Leave request, balance, and accrual trace

1. Leave categories are defined in `Leave Type`, with a leave-type identifier, mandatory flag, default accrual rate, accrual frequency, and maximum accrual.
2. `Leave Balance` stores the current balance for a leave type and schedule.
3. A leave request is recorded in `Leave`, linking the leave balance and leave type. The Data Dictionary also includes `scheduleID` as a foreign key.
4. The leave record includes the request start date, duration in hours, reason, status, credits to consume, remaining credits, request time, approval time, and approver identifier.
5. The `dateStart` constraint requires a non-null date on or after the current date.
6. The request may have an approval timestamp that remains null until approval; the Data Dictionary explicitly allows `dateApproved` to be null.
7. Supervisors handle leave requests in the Employee Management DFD. Leave and attendance stores are appendable/modifiable and viewable.
8. `Leave Accrual History` stores the accrual date, credit accrued, old balance, and new balance. The accrual date must not be later than the current date, and all credit/balance figures must be non-negative.
9. The leave balance is associated with the employee schedule and leave category, and the Database Design adds a unique combination of `(leaveType, scheduleID)`.

The source does not enumerate the actual leave categories, define enum members, identify the statutory accrual rules for each category, or specify the order of leave approval and credit reservation. It stores the fields required for those operations without providing a full business-rule specification.

### 14.4 Leave monetisation trace

1. A supervisor handles the approved leave-monetisation request.
2. Process `2.2 Leave Monetisation Process` records the event by appending to `D2.4 Leave Monetisation Logs`.
3. The process sends `Add to Benefits` to `2.3 Wage and Adjustments`.
4. The relational table `Leave Monetisation` stores leave type, balance ID, credits monetised, monetary value per credit, total converted, date monetised, and approver ID.
5. Wage and Adjustments updates wage information (`D3.1 Wage`) and appends adjustments to `D3.2 Benefits & Deductibles`.
6. The DFD shows the leave-monetisation log as part of the later `1YO` archival group.

The data fields support recording a conversion, but the PDFs do not provide the formula for `totalConverted`, the source of `monetaryValuePer`, eligible leave types, maximum monetisable credits, or the treatment of partial credits. No formula should be invented.

### 14.5 Wage, benefit, and adjustment trace

1. The Employee and organisational records establish which employee receives the payroll calculation.
2. Process `2.3 Wage and Adjustments` receives `Send Team Member Wage and Adjustments` from Supervisor.
3. It also receives monetised leave value from Process `2.2` using the `Add to Benefits` flow.
4. It updates `D3.1 Wage` through the flow labelled `Update Wage (opt*)`.
5. It appends adjustments to `D3.2 Benefits & Deductibles`.
6. The `Earnings` table stores overtime pay, PTO pay, holiday pay, rest-day pay, rice subsidy, phone stipend, clothes stipend, other incentives, and total benefits.
7. The `Payroll` record links to the wage record through `wageID`.

The source uses both `Wage` and `Earnings` as naming conventions: `D3.1 Wage` is the DFD store name, while the relational table is called `Earnings` and holds `wageID`. The relationship matrix states Payroll → Earnings is 1:1.

### 14.6 Government deduction and contribution trace

1. The employee's Government record holds the identifiers used for government-related payroll processing.
2. `D3.3 Tax Table & Rates` stores the tax bracket and contribution-rate reference information.
3. System Admin has an `Update Rates (opt*)` flow in the Payroll Level 2 DFD.
4. Process `2.4 Government Deductibles` utilises rates to compute contributions.
5. It appends computed information to `D3.2 Benefits & Deductibles` through `Append to Deductibles`.
6. The relational `Government Deductibles` table holds employer contribution, employee contribution, and withholding tax by government record and month.
7. The `Contributions` table stores specific PhilHealth, Pag-ibig, and SSS amounts with a contributor identifier.
8. `Deductibles` stores the government link and total deductions.
9. `Government Tally` holds overall contribution totals by contributor and is linked to `Summary`.
10. The monthly summary compilation process collates payroll and contribution information and generates a Monthly Summary Report.

The table names and relationships distinguish individual government identifiers, monthly deductible amounts, contributor-specific contribution values, and monthly tally totals. The documents do not give the exact reconciliation formula between Government Deductibles, Contributions, Deductibles, Government Tally, and Summary, so the precise arithmetic relationship must not be assumed.

### 14.7 Basic payroll computation and payslip generation

1. Process `2.5 Basic Payroll Computation` receives Time Activity from Timesheet Validation.
2. It retrieves basic remunerative values from `D3.1 Wage`.
3. It applies adjustments from `D3.2 Benefits & Deductibles`.
4. It computes `Net Pay and Constituents` and sends them to `2.6 Payslip Generation & Distribution`.
5. Payroll Staff can `View, Validate, and Verify Payroll Details` at the computation stage.
6. Process `2.6` appends the payslip to `D3.4 Payslip`.
7. Employee can view personal payslip information, Supervisor can view team information, and HR can view payslip/report information.
8. The `Payslip` relational table records pay-period start and end dates, standard hours, overtime hours, holiday overtime hours, PTO hours, undertime, days worked, holidays worked, rest days worked, and gross income.
9. `Payroll.netPay` records final pay after deductions; the Database Design says `hourlyRate`, `dailyRate`, and `netPay` must be non-null.

The database overview describes semi-monthly payslips. The source does not specify whether the system runs twice per month on fixed calendar dates, on a particular cut-off schedule, or through any other schedule. It also does not provide the gross-pay or net-pay formula.

### 14.8 Monthly summary and reporting trace

1. Process `2.7 Monthly Summary Compilation` receives information to collate from Government Deductibles and the Payslip store.
2. It collates payroll/payslip and government-related totals.
3. It generates `D3.5 Monthly Summary Report`.
4. Payroll Staff views, validates, and verifies the monthly report.
5. The `Summary` table holds total gross income, total benefits, employee disbursements, employer disbursements, combined Pag-ibig, PhilHealth, and SSS totals, total tax withheld, and total government disbursement.
6. `Government Tally` stores overall government contribution totals by contributor and references Summary.

The database overview describes one monthly summary per payroll processing cycle. The data model has `monthID` but the provided fields do not clearly define a year-plus-month key for Summary. The Rates table contains an `implementationYear` in the Database Design source, but no equivalent year field is defined for Summary in the Data Dictionary.

### 14.9 Archival and deletion trace

1. System Admin triggers Process `3.1 Appending` using an `Annual Prompt`.
2. The process compiles `1YO` records from Attendance Logs, Leave Logs, Changelogs, and Leave Monetisation Logs.
3. The process compiles `2YO` records from Payslip.
4. It stores old logs and payroll in `D4 Cold Storage` for later compression.
5. A `Trigger on Finish` connector links the archival stages.
6. Process `3.2 Deleting` removes relevant records from the operational stores.

This creates a visible distinction between operational records and older data transferred to cold storage. It is important not to turn the `1YO` and `2YO` labels into a more specific policy than the source supplies. The diagram does not define a retention schedule in a formal policy sense, does not say whether the labels mean “records older than one/two years”, and does not describe legal holds, backup validation, restoration testing, or the effect of a failed archive operation.

---

## 15. Constraints, access controls, retention, and operational rules

This section consolidates the rules that are scattered across field tables, table-level constraints, overview prose, and diagram labels.

### 15.1 Identifier and key rules

- `employeeID` is the unique Employee identifier and PK.
- `addressID`, `departmentID`, `positionID`, `supervisorID`, `scheduleID`, `balanceID`, and the other entity-specific IDs are used to identify their corresponding records as described in their table definitions.
- `Government Deductibles` uses `(governmentID, monthID)` as the composite PK in the Database Design submission and ERD.
- `Contributions` uses `(governmentID, contributor)` as the composite PK.
- `Government Tally` uses `(summaryID, contributor)` as the composite PK in the Database Design submission and ERD, while the Data Dictionary marks `summaryID` as PK “(assumed)”.
- `Leave Balance` has a unique combination of `(leaveType, scheduleID)` in the Database Design submission.
- `Attendance` has a unique combination of `(scheduleID, date)`.
- `Payslip` has a unique combination of `(payrollID, periodEndDate)`.
- `Holiday` uses a combined `(holidayName, holidayDate)` uniqueness rule in the Database Design prose, whereas the Data Dictionary marks both columns individually unique.
- `City.cityName` is unique in both the Data Dictionary and Database Design prose.
- The Database Design submission says a Payroll record is unique per employee per period, but its prose constraint does not expressly state the fields constituting that composite uniqueness rule.

### 15.2 Date and time rules

The source gives the following date/time checks:

- Employee `birthdate` must not be null and must be earlier than `CURRENT_DATE`.
- Schedule `startTime` must be present.
- `requiredDailyHours` must be between 1 and 24, inclusive, and must be present.
- Schedule rest-day information must be present; whether it is a `SET` or a `restDayID` is inconsistent between sources.
- Attendance `timeIn` must be less than or equal to `timeOut`.
- Leave `dateStart` must be present and greater than or equal to `CURRENT_DATE`.
- Leave `dateRequested` defaults to `CURRENT_TIMESTAMP` in the Data Dictionary.
- Leave `dateApproved` is allowed to be null until approval.
- Changelog `createdAt` and `updatedAt` must be present and default to `CURRENT_TIMESTAMP`.
- Leave Accrual History `accrualDate` must be present and less than or equal to `CURRENT_DATE`.
- Payslip `periodStartDate` must be present. The source also defines a unique `(payrollID, periodEndDate)` pair, but does not state an ordering check requiring `periodEndDate >= periodStartDate`.
- `dateMonetised` is stored as a Date, but no temporal check is stated for it.
- Holiday dates are stored as Date values, without a separate check constraint.

### 15.3 Non-negative numeric rules

The source defines non-negative checks for several financial and balance values. The principal groups are:

- Leave Balance: `currentBalance >= 0`.
- Leave: `hourDuration >= 0`.
- Leave Accrual History: `creditAccrued`, `oldBalance`, and `newBalance` are non-negative.
- Government Deductibles: contribution/deduction monetary amounts are non-negative.
- Contributions: PhilHealth, Pag-ibig, and SSS amounts are non-negative.
- Payroll: `ptoMax >= 0`; hourly rate, daily rate, monthly rate, and net pay have non-negative field constraints in the Data Dictionary, while the Database Design's explicit table-level check calls out `ptoMax >= 0` and the non-null requirements for hourly rate, daily rate, and net pay.
- Payslip: hours and days worked are non-negative; gross income has a `Min(0)` field constraint in the Data Dictionary.
- Leave Monetisation: credits, per-credit value, and total converted amount are non-negative.
- Earnings: all monetary amounts are non-negative.
- Deductibles: deduction amounts are non-negative.
- Other Transactions: transaction amount is non-negative.
- Government Tally: all contribution totals are non-negative.
- Summary: all monetary totals are non-negative.
- Months: `monthlyWorkdays` must be between 1 and 31.
- Rates: rates must be non-negative and, according to the Database Design prose, all fields must be supplied.
- Tax Table: monetary values must be non-negative; minimum compensation, maximum compensation, and tax rate must be non-null according to the Database Design constraints.
- Leave Type: `defaultAccrualRate` and `maxAccrual` are non-negative.

### 15.4 Auditability and change history

The Database Design overview says modifications should be restricted to authorised personnel and traceable to the account responsible. The `Changelog` table supports this by storing:

- the affected employee (`employeeID`);
- creation and last-update timestamps (`createdAt`, `updatedAt`);
- the account/identifier associated with the append (`appenderID`);
- the account/identifier associated with the edit (`editorID`);
- a shortened table-and-column identifier (`fieldCode`);
- a data type (`dataType`); and
- previous and updated values (`oldValue`, `newValue`).

The example `P_NP` means `Payroll.NetPay`. The DFD adds the operational use: employee-information updates are appended to Changelog, the log can be viewed, and Changelogs are later included in archival compilation.

### 15.5 Personally identifiable information access

The security design contains several distinct statements:

1. Access is role-based, with separate roles for System Admin, HR, Payroll Staff, and Employee.
2. Modifications are limited to authorised personnel.
3. Audit logs should make modifications traceable to the responsible account.
4. Access to an employee's personally identifiable information requires approval from two authorised personnel.
5. Only the corresponding contribution values are normally visible and accessible.
6. The `Government` entity is highlighted with the schema legend's `Additional Security Required` colour.
7. The DFD has a dedicated `1.3 Government Info Access` process and a `D1.3 Personally Identifiable Information` store.
8. PII viewing and updating are explicitly labelled `On Approval`.
9. `PII Access Approval` appears in the context and Level 1 diagrams; the Level 2 employee-management diagram shows approval and record-modification paths.

The documents do not specify the exact identity-verification method, the two approver roles, approval evidence format, session controls, field masking, encryption algorithm, access-request expiry, or revocation policy. Do not invent these details.

### 15.6 Data retention and archival handling

The archival Level 2 diagram contains these exact labels:

- `Trigger on Annual Prompt`;
- `Compile (1YO) Records`;
- `Compile (2YO) Records`;
- `Store Old Logs and Payroll for later compression`;
- `Trigger on Finish`; and
- `Remove Relevant Records`.

It places Attendance Logs, Leave Logs, Changelogs, and Leave Monetisation Logs in the group associated with `1YO` records. It places Payslip in the group associated with `2YO` records. System Admin triggers the annual prompt; Process 3.1 appends/archives the records into Cold Storage; Process 3.2 deletes relevant records from the operational stores.

These are diagrammed retention/archival labels, not a complete records-retention policy. The source does not define:

- what the abbreviation `YO` stands for;
- the exact date calculation for the `1YO` or `2YO` thresholds;
- whether the threshold is based on creation date, last update, pay-period date, or another date;
- whether all records or only selected records are archived;
- whether Cold Storage has a separate backup or replication layer;
- how the archive is validated before deletion;
- how archived records are retrieved or restored;
- whether legal holds or disputes suspend deletion; or
- whether the word “later” in “later compression” has a defined schedule.

### 15.7 Payroll period and report cadence

The Database Design overview states that each employee receives a **semi-monthly payslip** and that each payroll processing cycle has a **monthly summary**. The database stores period dates in Payslip and a month identifier in Payroll and Summary. However, it does not define the precise cut-off calendar, payroll processing dates, or a year-month identifier for every monthly report.

---

## 16. Cross-document discrepancies and unresolved design questions

The documents are complementary but not fully harmonised. This section is critical for any agent using the synthesis to answer design questions, revise the schema, generate SQL, or evaluate whether the diagrams match the table definitions.

| No. | Topic | Data Dictionary | Database Design prose / relationship matrix | ERD or DFD image | Why it matters |
|---:|---|---|---|---|---|
| 1 | Total table count | 29 named tables. | 30 named tables because `Rest Day` is included. | ERD visibly includes `Rest Day`. | Do not report one count without explaining which source inventory is being counted. |
| 2 | Rest-day structure | `Schedule.restDay` is a `SET`, `NOT NULL`. No `Rest Day` table is listed. | Schedule prose also lists `restDay` as `SET`, but a separate Rest Day table is included. | ERD uses `Schedule.restDayID` as FK to a `Rest Day` table containing seven weekday Booleans. | A relational implementation cannot use both representations as though they were identical without a deliberate design decision. |
| 3 | Employee columns | Includes `departmentID` and `governmentID` as FKs. | Employee prose table omits both. The relationship matrix separately lists Position → Department and Government → Employee. | Employee ERD box omits `departmentID` and `governmentID`. | Department and government references may be direct Employee FKs, derived from Position/Government, or an older version of the design. The PDFs do not resolve this. |
| 4 | Address columns | Includes `regionID` as FK. | Address prose table omits `regionID`; only `cityID` appears in the table's FK constraint summary. | Address ERD box includes `regionID` as FK; Cities also has `regionID`. | The design must decide whether region is stored directly on Address or derived through Cities. |
| 5 | Supervisor columns | Includes `departmentID` as FK. | Supervisor prose table lists only `supervisorID` and `employeeID`. | Supervisor ERD box includes `departmentID` as FK. | A schema built from only the prose table would omit a field visibly present in both the Data Dictionary and ERD. |
| 6 | Schedule fields | Includes `restDay` as `SET`. | Schedule prose also includes `restDay` as `SET`. | ERD instead includes `restDayID` as FK. | Same conflict as No. 2; the prose and ERD disagree inside the Database Design submission itself. |
| 7 | Leave columns | Includes `scheduleID` as FK. | Leave prose table and table-level FK list omit `scheduleID`. | ERD Leave entity includes `scheduleID` as FK. | The Data Dictionary and ERD agree, while the prose table omits the field. |
| 8 | Employee-government link | Employee contains `governmentID` as FK. | Government contains `employeeID` as FK; relationship matrix says Government → Employee is 1:1. | ERD Government contains `employeeID`; Employee does not visibly show `governmentID`. | Both directions could be represented in a physical schema, but doing so may duplicate the same association. The design does not specify the intended FK location. |
| 9 | Employee-department link | Employee contains `departmentID` FK. | Employee prose omits it; Position contains `departmentID`, and Position → Department is N:1. | Employee does not show departmentID; Position does. | The department may be obtained through Position, but the Data Dictionary also declares a direct Employee FK. |
| 10 | Government Deductibles key | Field table marks `governmentID` as PK and FK; `monthID` as FK. | Composite PK is `(governmentID, monthID)`. | Both `governmentID` and `monthID` are visibly marked as PK/FK. | Use the composite key if reproducing the Database Design/ERD version, while retaining that the Data Dictionary's field-level table is less explicit. |
| 11 | Government Tally key | `summaryID` is marked `Primary Key (assumed)`; `contributor` has no key declaration in its Data Dictionary field table. | Composite PK `(summaryID, contributor)`. | Both `summaryID` and `contributor` are marked as key fields; `summaryID` also FK. | Composite key is the explicit Database Design/ERD definition; “assumed” in the Data Dictionary signals uncertainty. |
| 12 | Rates table key and year | Lists five rate fields and no `implementationYear`. | Lists `implementationYear` as a field, but says the table has no PK, FK, or unique constraint. | `implementationYear` is visibly marked PK in the ERD. | The identifier for rate versions is not settled. The ERD and prose contradict each other, and the Data Dictionary lacks the year column. |
| 13 | Holiday uniqueness | `holidayName` unique and `holidayDate` unique individually. | Composite unique rule `(holidayName, holidayDate)`. | ERD does not visibly annotate the unique constraints. | These are materially different integrity rules. |
| 14 | Payroll month key | `monthID` is listed in Payroll but not marked FK. | Payroll table-level FK list omits `monthID`, but relationship matrix states Payroll → Months through `monthID`. | ERD shows monthID in Payroll, but the key marker is not as clear as its explicit FK fields. | The prose relationship says the month link exists even though the FK constraint list is incomplete. |
| 15 | Leave Monetisation leave-type link | `leaveType` is `NOT NULL`, but its field constraint does not clearly label it FK. | Table-level FK summary lists only `balanceID`. | Relationship matrix and ERD show Leave Monetisation → Leave Type. | The association is present conceptually and visually but missing from the table-level FK list. |
| 16 | Contributions relationship | `governmentID` is composite PK/FK. | Relationship matrix says Contributions → Government Deductibles N:1 via `governmentID`. | ERD visibly shows Contributions related to Government Deductibles and Government-side data. | The textual relationship target may not match the table FK declaration, which describes `governmentID` as a government record identifier. |
| 17 | `Address` to `Regions` | Address has `regionID` FK. | Relationship matrix lists Address → Cities, not Address → Regions. | ERD shows Address.regionID and Cities.regionID, with both tied into the region/city area. | The matrix is incomplete relative to the listed fields and ERD. |
| 18 | `Cities` to `Regions` | Cities has `regionID` FK. | No separate Cities → Regions row appears in the relationship matrix. | ERD visibly connects Cities and Regions. | A foreign-key field exists without an explicit relationship-matrix row. |
| 19 | `Schedule` to `Rest Day` | No such table or relationship. | A Rest Day table exists, but no explicit Rest Day relationship row is present. | ERD shows Schedule.restDayID. | The ERD has a foreign-key path that the written relationship matrix omits. |
| 20 | Static group label | Table inventory calls the group E. | Column-definition subsection calls it F. | No corresponding numbering in ERD. | Editorial inconsistency, not a data-model difference. |
| 21 | Database Design table-of-contents numbering | Not applicable. | TOC says Section 2 is Tables, 3 Relationships, 4 References, while body uses 2 Schema Diagram, 3 Tables, 4 Relationships, 5 References. | Schema diagram is on page 5. | Page/section references should follow actual body headings, not the inconsistent TOC. |
| 22 | `opt*` in payroll DFD | Not applicable. | No prose explanation. | DFD labels `Update Wage (opt*)`, `Update Values (opt*)`, and `Update Rates (opt*)`. | Marker must be retained literally until its meaning is known. |
| 23 | `1YO` / `2YO` in archival DFD | Not specified in the Data Dictionary. | No prose explanation. | Archival DFD uses `Compile (1YO) Records` and `Compile (2YO) Records`. | These labels should not be expanded into a formal retention period without confirmation. |
| 24 | DFD external business-source materials | The data dictionary does not include them. | The database design does not include their text. | DFD reference list names the Business Requirement Specification and Existing Business Processes – Payroll. | The three supplied PDFs do not contain the full source business requirements that informed the DFD. |

### 16.1 Highest-impact reconciliation questions

For actual implementation or formal correction, the following questions have the largest effect on table structure and referential integrity:

1. **Rest Day:** Should `Schedule` store a `SET` of weekdays, or should it store `restDayID` referencing a separate `Rest Day` record with seven Boolean flags?
2. **Employee–Government relationship:** Should `Government.employeeID` be the sole FK for the 1:1 association, or should `Employee.governmentID` also exist?
3. **Department placement:** Is department assignment stored directly in Employee, derived exclusively through Position, or duplicated in both places?
4. **Address–Region relationship:** Is `Address.regionID` a stored FK, or should region be derived through `Address.cityID` → `Cities.regionID`?
5. **Supervisor department:** Is `Supervisor.departmentID` required in the physical table?
6. **Leave scheduling:** Should `Leave.scheduleID` be present as in the Data Dictionary and ERD?
7. **Rates versioning:** Is `implementationYear` the primary key for Rates, and if so, why does the table constraint section say no PK?
8. **Composite keys:** Should `Government Deductibles` and `Government Tally` use the composite keys shown in the Database Design/ERD?
9. **Holiday uniqueness:** Should the uniqueness rule apply separately to name and date, or only to the combined name/date pair?
10. **Missing FK declarations:** Should `Payroll.monthID`, `LeaveMonetisation.leaveType`, and the region/rest-day links be added to the written FK list and relationship matrix?

A receiving agent should treat these as open specification issues, not as permission to choose whichever implementation seems most convenient.

---

## 17. What the documents do not specify

This section separates the system's documented elements from details that are absent in the three PDFs. It is especially important for preventing a downstream agent from filling gaps with generic payroll-system assumptions.

### 17.1 No full SQL implementation specification

The documents provide logical column definitions and constraints, but do not identify:

- the target database management system or SQL dialect;
- complete `CREATE TABLE` statements;
- exact enum members for employment status, leave type, leave status, day type, transaction type, contributor, month, accrual frequency, or changelog data type;
- cascade rules for update or delete;
- nullability for every column not explicitly constrained;
- indexes beyond keys and unique rules that are stated;
- generated/identity/auto-increment behaviour for IDs;
- default values other than those explicitly stated for certain timestamps; or
- physical storage, partitioning, replication, or backup configuration.

### 17.2 No payroll formulas

The documents identify components and process names, but do not provide complete formulas for:

- gross income;
- net pay;
- hourly, daily, or monthly salary conversion;
- ordinary overtime;
- holiday overtime;
- holiday pay;
- rest-day pay;
- paid time off compensation;
- undertime;
- benefits totals;
- deduction totals;
- withholding tax;
- SSS, PhilHealth, or Pag-ibig contributions;
- employer versus employee contribution shares;
- government disbursement totals; or
- employee/employer disbursement totals.

The schema provides fields where amounts are stored. The DFD identifies which processes use those amounts. Neither gives the arithmetic rules.

### 17.3 No actual rate schedules or tax brackets

The `Rates` and `Tax Table` tables define the places where rates and brackets are held. The PDFs do not provide actual statutory percentages, amounts, income thresholds, bracket widths, tax formulas, effective dates, or the date on which rate changes take effect.

### 17.4 No complete business rules for leave

The data model provides leave categories, accrual frequency, accrual rate, maximum accrual, leave balances, requests, approvals, and monetisation. It does not list the actual leave categories or define:

- how leave is earned within a period;
- when accrual is posted;
- whether accrual is prorated;
- how leaves are approved or rejected;
- how requests that exceed the balance are handled;
- whether a request reserves credits before approval;
- how overlapping leave requests are handled;
- which leave types can be monetised;
- how monetisation is valued; or
- how an approved leave request affects attendance data and payroll.

### 17.5 No complete security implementation

The overview identifies RBAC and a two-person approval requirement for PII, but does not provide a complete role-permission matrix, specific permission names, authentication factors, encryption method, key custody, logging retention, masking rules, access review cycle, or PII access-request workflow.

### 17.6 No complete archival policy

The DFD shows the labels `1YO`, `2YO`, an annual prompt, cold storage, later compression, and subsequent deletion. It does not define the precise age calculation, file format, compression method, archive integrity check, restore process, backup schedule, legal exceptions, or audit evidence.

### 17.7 No contents of the referenced business requirements

The DFD worksheet says it was based on the Business Requirement Specification Document for Payroll System and MotorPH Existing Business Processes – Payroll. The DFD PDF's references table lists the two documents by name, but the supplied source set does not include either complete document. The DFD may therefore reflect rules that are not fully explained in the three files. This synthesis cannot recover requirements that are only contained in those absent references.

---

## 18. Page-by-page source map

The following map allows a downstream agent to locate a fact in the originals if access is later restored.

### 18.1 Data Dictionary PDF — 28 pages

| PDF page | Main content |
|---:|---|
| 1 | MO-IT107 Week 7, Logical Schema Design, Worksheet 4 metadata and explanation of a data dictionary. |
| 2 | Instructions and start of the Data Dictionary table inventory; Employee Details group begins. |
| 3 | Table inventory continuation: Employee Details and Logging group. |
| 4 | Table inventory continuation: Government Details and Payroll group. |
| 5 | Table inventory continuation: Payroll and Static-Update Only group; start of column definitions. |
| 6 | Employee table fields. |
| 7 | Employee continuation, Address, and Department fields. |
| 8 | Position, Supervisor, and Schedule fields. |
| 9 | Schedule continuation, Leave Balance, and Logging heading. |
| 10 | Changelog fields. |
| 11 | Attendance and Leave fields. |
| 12 | Leave continuation and Leave Accrual History fields. |
| 13 | Government fields and Government Deductibles fields. |
| 14 | Government Deductibles continuation and Contributions fields. |
| 15 | Contributions continuation and Payroll fields. |
| 16 | Payroll continuation, Payslip fields, and start of Leave Monetisation. |
| 17 | Payslip continuation, Leave Monetisation continuation, and Earnings fields. |
| 18 | Earnings continuation, Deductibles, Other Transactions, and Government Tally fields. |
| 19 | Government Tally continuation and Summary fields. |
| 20 | Summary continuation and start of Static-Update-Only fields; Regions and Cities. |
| 21 | Holiday and Months fields. |
| 22 | Rates fields. |
| 23 | Tax Table and Leave Type fields. |
| 24 | Relationships matrix: Employee, Address, Position, Supervisor, Schedule, and Changelog relationships. |
| 25 | Relationships matrix continuation: Leave Balance, Attendance, Leave, Leave Accrual History, Leave Monetisation, Government, and Payroll links. |
| 26 | Relationships matrix continuation: payroll, government deductibles, contributions, deductions, payslips, other transactions, Government Tally; start of Definition of Terms. |
| 27 | Definition of Terms: TIN, SSS, PhilHealth, PagIbig, PTO, PI, fieldCode, Varchar, Varbinary, Enum, TinyInt. |
| 28 | Definition of Terms continuation: SmallInt, Precision, Scale, accrual, monetisation, bracketWidth, contributor. |

### 18.2 Database Design PDF — 25 pages

| PDF page | Main content |
|---:|---|
| 1 | Title page, author, programme, and term. |
| 2 | Table of contents. |
| 3 | Database overview and the first four system functions. |
| 4 | Security and Access Control continuation. |
| 5 | Full-page Schema Diagram. |
| 6 | Employee and Address tables and constraints. |
| 7 | Department and Position tables and constraints. |
| 8 | Supervisor and Schedule tables and constraints. |
| 9 | Rest Day and Leave Balance tables and constraints. |
| 10 | Changelog and Attendance tables and constraints. |
| 11 | Leave fields and constraints. |
| 12 | Leave Accrual History fields and constraints. |
| 13 | Government and Government Deductibles fields and constraints. |
| 14 | Contributions and the start of Payroll. |
| 15 | Payroll continuation and Payslip. |
| 16 | Leave Monetisation. |
| 17 | Earnings and Deductibles. |
| 18 | Other Transactions and Government Tally. |
| 19 | Summary continuation. |
| 20 | Summary continuation, Regions, and Cities. |
| 21 | Holiday, Months, and Rates. |
| 22 | Tax Table and Leave Type. |
| 23 | Relationship section heading and the first page of the relationship matrix. |
| 24 | Continuation of the relationship matrix. |
| 25 | References: Entity-Relationship Diagram, Data Dictionary, and Schema Diagram. |

### 18.3 DFD PDF — 6 pages

| PDF page | Main content |
|---:|---|
| 1 | MO-IT107 Week 11, Data Flow Diagrams, Worksheet 6 metadata, instructions, Gene & Sarson notation requirement, and Level 2 requirement. |
| 2 | Context-level (`LV0`) diagram showing Employee, Supervisor, HR, Payroll Staff, System Admin, and process `0.0 MotorPH Payroll System`. |
| 3 | Level 1 diagram showing `1.0 Employee Management`, `2.0 Payroll Processing`, `3.0 Archival`, and stores `D1` through `D4`. |
| 4 | Employee Management Level 2 diagram: processes `1.1`, `1.2`, `1.3`; stores `D1.1`–`D1.3` and `D2.1`–`D2.3`. |
| 5 | Payroll Processing Level 2 diagram: processes `2.1`–`2.7` and stores `D2.1`, `D2.2`, `D2.4`, `D3.1`–`D3.5`. |
| 6 | Archival Level 2 diagram: processes `3.1 Appending` and `3.2 Deleting`, stores `D2.1`–`D2.4`, `D3.4`, and `D4 Cold Storage`; references table. |

---

## 19. Working instructions for a downstream agent

The following instructions are included so the synthesis can be used as the receiving agent's primary context file.

### 19.1 Treat this synthesis as a source digest, not as an updated design

Use this file to understand what the three uploaded PDFs contain. Do not interpret it as confirmation that all table definitions are mutually consistent or implementation-ready. It is a comprehensive digest of the submitted material, including its disagreements.

### 19.2 Preserve source labels

When discussing the design, retain the original names unless the user explicitly asks for renaming:

- `Employee Management`, `Payroll Processing`, `Archival`;
- `Employee Information Handling`, `Schedule and Time Tracking`, `Government Info Access`;
- `Timesheet Validation`, `Leave Monetisation Process`, `Wage and Adjustments`, `Government Deductibles`, `Basic Payroll Computation`, `Payslip Generation & Distribution`, `Monthly Summary Compilation`;
- `Appending`, `Deleting`;
- `Employee Repository`, `Logs`, `Payroll`, `Cold Storage`; and
- all `D1`–`D4` and `D1.1`–`D3.5` identifiers.

Do not rename `Earnings` to `Wage` or `Government Tally` to `Contribution Summary` merely because an alternative label may appear more intuitive. `Wage` is the DFD store name, while `Earnings` is the relational table name.

### 19.3 Do not invent unspecified values or formulas

No exact payroll formula, statutory percentage, tax bracket, enum member, encryption method, or archival age rule is included. When asked about these, state that the supplied documents do not specify them. Ask for the referenced Business Requirement Specification or Existing Business Processes document if its contents are necessary to answer.

### 19.4 Keep document variants separate when answering schema questions

When a user asks “what does the Data Dictionary say?”, answer from the Data Dictionary version. When asked “what does the ERD show?”, answer from the visual ERD transcription. When asked “what does the Database Design table definition say?”, answer from that prose definition. If the question asks for the combined design, disclose the relevant conflict and show both variants.

### 19.5 Do not silently resolve the key discrepancies

In particular, do not silently decide:

- `Schedule.restDay SET` versus `Schedule.restDayID` plus `Rest Day`;
- Employee direct `departmentID` and `governmentID` versus links through Position and Government;
- Address direct `regionID` versus region derived through City;
- `Supervisor.departmentID` presence or absence;
- `Leave.scheduleID` presence or absence;
- `Rates.implementationYear` as a field and primary key versus no primary key; or
- the composite key for `Government Tally` versus the Data Dictionary's assumed `summaryID` key.

If a task requires producing SQL or a corrected ERD, identify these as decisions that must be made. Do not claim that the PDFs have already chosen one unambiguously.

### 19.6 Distinguish schema structure from process flows

The database schema describes where records and values are stored and how tables are related. The DFD describes how actors and processes move or update those records. A DFD store such as `D2 Logs` can encompass several relational tables, and a process such as `2.5 Basic Payroll Computation` can consume several stores. Do not assume a DFD data store is exactly one database table unless a source statement confirms it.

### 19.7 Distinguish stated security intent from implemented security detail

The documents state RBAC, authorisation, auditability, and two-authorised-person PII approval. They do not give a complete access-control matrix or implementation mechanism. Do not infer which exact roles can perform every operation unless the diagrams name the flow or the user supplies the missing policy.

### 19.8 Preserve the archival labels literally

The DFD uses `1YO`, `2YO`, `Annual Prompt`, `later compression`, and `Remove Relevant Records`. Do not expand `YO`, derive an exact number of days, or claim that the archive/deletion process is fully specified.

### 19.9 Suggested source-grounded response pattern

For a detailed question, answer in this order:

1. state the source document and page or diagram level;
2. quote or reproduce the relevant table, field, process, flow, or cardinality;
3. compare another document only if it addresses the same element;
4. identify any mismatch plainly; and
5. distinguish source fact from interpretation or unresolved implementation choice.

### 19.10 Source documents

The synthesis is based only on these three supplied PDFs:

1. `MO-IT107 Homework_ Data Dictionary for MotorPH Payroll System Database H2101 Torres, J.D..pdf` — 28 pages.
2. `MO-IT107 - 2nd Milestone_ Database Design for the MotorPH Payroll System H2101 TORRES, J.D..pdf` — 25 pages.
3. `MO-IT107 Homework_ Data Flow Diagram of MotorPH Payroll System H2101 Torres, J.D..pdf` — 6 pages.

The Data Flow Diagram also names two external references, but those full documents were not among the three supplied PDFs:

- `Business Requirement Specification Document for Payroll System`.
- `MotorPH Existing Business Processes - Payroll`.

---

## Final scope statement

This synthesis covers the three supplied PDFs' system overview, all table inventories, all column definitions visible in the Data Dictionary, the Database Design table definitions and table-level constraints, the schema diagram's visual content, the stated relationships and cardinalities, all visible DFD actors/processes/data stores/flows, and the archival sequence. It also records the important omissions and contradictions that a downstream agent would otherwise be unable to see from a text-only excerpt.

It does **not** claim to contain the full contents of the two external business-requirement documents named in the DFD references, nor does it add payroll formulas, statutory rate values, SQL implementation decisions, or business rules absent from the supplied files.

### 13.7 Data Dictionary relationship matrix (separate source version)

**Source location:** Data Dictionary PDF, pages 24–26. This is a second relationship matrix, separate from the Database Design relationship matrix reproduced in Sections 13.1–13.3. It contains a broader set of declared FK links and differs on certain cardinalities.

The Data Dictionary matrix has these columns: `Foreign Key Table`, `Foreign Key Column`, `Referenced Table`, `Referenced Primary Key`, and `Description of Relationship`.

| Foreign-key table | FK column | Referenced table / PK | Cardinality stated in Data Dictionary | Description stated |
|---|---|---|---:|---|
| Employee | `addressID` | Address.`addressID` | 1:1 | Each employee is assigned an address. |
| Employee | `departmentID` | Department.`departmentID` | N:1 | Employees are categorised by their department. |
| Employee | `positionID` | Position.`positionID` | N:1 | The employee holds a specific position. |
| Employee | `governmentID` | Government.`governmentID` | 1:1 | Government records are uniquely linked to each employee. |
| Employee | `supervisorID` | Supervisor.`supervisorID` | N:1 | The system references the supervisor of the employee. |
| Address | `cityID` | Cities.`cityID` | 1:1 | Each address entry is located within a specific city. |
| Address | `regionID` | Regions.`regionID` | 1:1 | Addresses are also linked to a region. |
| Position | `departmentID` | Department.`departmentID` | N:1 | Positions are assigned within departments. |
| Supervisor | `departmentID` | Department.`departmentID` | N:1 | Departments have at least one supervisor. |
| Supervisor | `employeeID` | Employee.`employeeID` | 1:1 | Supervisors are employees themselves, connecting both tables directly. |
| Schedule | `employeeID` | Employee.`employeeID` | 1:1 | Each employee is assigned a schedule. |
| Changelog | `employeeID` | Employee.`employeeID` | N:1 | Tracks changes made by the employee. |
| Leave Balance | `leaveType` | Leave Type.`leaveType` | N:1 | Indicates the leave type applicable for an employee leave balance. |
| Leave Balance | `scheduleID` | Schedule.`scheduleID` | N:1 | Leave balances are determined based on an employee's schedule. |
| Attendance | `scheduleID` | Schedule.`scheduleID` | N:1 | Attendance records are linked to the schedule. |
| Leave | `scheduleID` | Schedule.`scheduleID` | N:1 | Leave requester identifier. |
| Leave | `leaveType` | Leave Type.`leaveType` | N:1 | Indicates the leave type used for the request. |
| Leave | `balanceID` | Leave Balance.`balanceID` | N:1 | Leave requests are validated against available leave balances. |
| Leave Accrual History | `balanceID` | Leave Balance.`balanceID` | N:1 | Accrual records adjust the leave balance over time. |
| Leave Accrual History | `leaveType` | Leave Type.`leaveType` | N:1 | Indicates the leave type that has been accrued. |
| Leave Monetisation | `leaveType` | Leave Type.`leaveType` | N:1 | Indicates the leave type requested for conversion. |
| Leave Monetisation | `balanceID` | Leave Balance.`balanceID` | N:1 | Converts leave credits into monetary value based on the leave balance. |
| Government | `employeeID` | Employee.`employeeID` | 1:1 | Links each government record with an employee. |
| Payroll | `employeeID` | Employee.`employeeID` | N:1 | Links payroll records to employees. |
| Payroll | `rebateID` | Deductibles.`rebateID` | 1:1 | Deduction computations of payroll. |
| Payroll | `wageID` | Earnings.`wageID` | 1:1 | Benefit computations of payroll. |
| Payroll | `summaryID` | Summary.`summaryID` | N:1 | Collates payroll data into a monthly summary record. |
| Payroll | `monthID` | Months.`monthID` | 1:1 | References payroll month and its corresponding working days. |
| Government Deductibles | `monthID` | Months.`monthID` | N:1 | References the month these deductibles are for. |
| Government Deductibles | `governmentID` | Government.`governmentID` | 1:1 | Associates deductibles with a government record. |
| Contributions | `governmentID` | Government Deductibles.`governmentID` | N:1 | Reference location where computed contribution will be collated. |
| Deductibles | `governmentID` | Government Deductibles.`governmentID` | 1:1 | Needs government deductibles to compute total deductibles. |
| Payslip | `payrollID` | Payroll.`payrollID` | 1:1 | Each payslip is uniquely tied to a payroll record. |
| Other Transactions | `payrollID` | Payroll.`payrollID` | N:1 | Connects miscellaneous transactions to payroll records. |
| Government Tally | `summaryID` | Summary.`summaryID` | N:1 | Aggregates government-related disbursements within a summary. |

**Important Data Dictionary-specific readings:**

- Employee has direct links to Address, Department, Position, Government, and Supervisor.
- Address has separate links to Cities and Regions.
- Supervisor has a Department link as well as its Employee link.
- Leave has a Schedule link, a Leave Type link, and a Leave Balance link.
- Payroll → Months is recorded as **1:1** in this matrix.
- Contributions is shown as referencing Government Deductibles by `governmentID` in this matrix, despite the field definition describing `Contributions.governmentID` as a linked government record identifier and the schema diagram showing Government-side links.

### 13.8 Comparing the two relationship matrices

The Data Dictionary matrix and Database Design matrix should be treated as two distinct source versions:

| Relationship topic | Data Dictionary matrix (PDF pp. 24–26) | Database Design matrix (PDF pp. 24–25) |
|---|---|---|
| Employee → Department | Explicit Employee.`departmentID` → Department, N:1. | Not listed as a direct Employee relationship; Position → Department is listed. |
| Employee → Government | Explicit Employee.`governmentID` → Government, 1:1. | Government → Employee is listed using Government.`employeeID`, 1:1. |
| Address → Regions | Explicit Address.`regionID` → Regions, 1:1. | Not listed. |
| Supervisor → Department | Explicit Supervisor.`departmentID` → Department, N:1; purpose says each department has at least one supervisor. | Not listed as a separate relationship. |
| Leave → Schedule | Explicit Leave.`scheduleID` → Schedule, N:1; described as Leave requester identifier. | Not listed. |
| Payroll → Months | N/A in the Database Design field table, but the relationship matrix itself lists N:1. | N:1. |
| Contributions → Government Deductibles | Listed as N:1. | Also listed as N:1. |
| Address → Cities | 1:1. | N:1. |

The Address → Cities and Payroll → Months cardinalities differ between the two matrices. Do not treat these as harmless wording variations: the stated cardinalities have direct implications for the intended relationship model.

The Data Dictionary matrix also includes the relationship `Address.regionID` → Regions and `Supervisor.departmentID` → Department. Those links are not in the Database Design matrix, even though the ERD shows the relevant columns. The database-design relationship matrix is therefore shorter than the set of links represented across the other source materials.

---

## 20. Data Dictionary glossary and acronym definitions

**Source location:** Data Dictionary PDF, pages 26–28, Section 3, `Definition of Terms`.

The Data Dictionary explicitly includes a glossary, which is part of the source content and should be retained by any agent using this synthesis.

| Term | Definition given in the Data Dictionary |
|---|---|
| `TIN` | Taxpayer Identification Number — a unique number assigned to individuals or entities for tax purposes. |
| `SSS` | Social Security System Number — an identifier used to track social security contributions and benefits. |
| `PhilHealth` | Identifier for the Philippine Health Insurance Corporation, used to manage health-insurance records and contributions. |
| `PagIbig` | Identifier for the Pag-IBIG Fund (Home Development Mutual Fund), used for housing savings and related benefits. |
| `PTO` | Paid Time Off — the policy allowing employees to take leave while still receiving their regular pay. |
| `PI` | The Data Dictionary says, “No, not Archimedes' constant.” It is the abbreviation used in rate fields (`piEmployerRate`, `piEmployeeRate`, `piMaxContribution`) and refers to Pag-IBIG-related contribution rates. |
| `fieldCode` | A shortened identifier used in logging to represent specific table and column names concisely, e.g. `P_NP` for `Payroll.NetPay`. |
| `Varchar` | Variable Character — a data type for storing text strings of varying lengths. |
| `Varbinary` | Variable Binary — a data type for storing binary data that does not have a fixed length. |
| `Enum` | Enumeration — a data type that defines a set of predefined values. |
| `TinyInt` | Tiny Integer — a data type representing a small-range integer, with values that typically do not reach beyond 127. |
| `SmallInt` | Small Integer — a numeric data type used to store integers within a limited range, with values that typically do not reach beyond 32,767. |
| `Precision` | The total number of digits allowed in a decimal number, both to the left and right of the decimal point. |
| `Scale` | The number of digits allowed to the right of the decimal point in a decimal number. |
| `accrual` | The process by which leave credits are accumulated over time, typically based on a defined rate or frequency. |
| `monetisation` | The conversion of leave credits into monetary value, allowing employees to receive compensation for unused leave. |
| `bracketWidth` | Represents the income range covered by a tax bracket, defining the span between the minimum and maximum compensation for that bracket. |
| `contributor` | Identifier for the party, such as an employee or employer, contributing to a benefit or deduction in payroll and related calculations. |

**Glossary note:** The glossary describes `TinyInt` and `SmallInt` using typical upper-range descriptions, but the PDFs do not specify the actual database engine. The wording should be retained as the document's explanation, not treated as a complete SQL-dialect declaration.

---
