# Sweldong Pinoy: captured form probe and safe use

A user-provided Network capture shows a browser request to `POST https://www.sweldongpinoy.com/` using `application/x-www-form-urlencoded`. The form submits these 11 keys: `employment_type`, `pay_period`, `payroll_period_daily_days`, `basic_pay`, `taxable_allowance`, `nontaxable_allowance`, `night_differential_hours`, `night_differential_rate`, `overtime_hours`, `overtime_rate`, and `work_schedule`. The captured response contains `status`, `result`, and `altresult`. A `JSESSIONID` cookie was present in the browser capture.

A session cookie does not, by itself, require a browser. `tools/sweldong_pinoy_probe.py` uses Python's standard-library HTTP client and cookie jar: it GETs the homepage, retains any session cookie, then POSTs the form in the same session. It does not open a browser, use the copied session cookie, or print cookie values, and it refuses redirects rather than forwarding amounts to a new target. Its default amounts are the user's captured **test figures**. It validates response fields and arithmetic before reporting success and prints the raw response for diagnostics; treat the output as confidential compensation data.

Run with Python 3:

```sh
python3 tools/sweldong_pinoy_probe.py
```

Example inputs can be varied using flags, e.g.:

```sh
python3 tools/sweldong_pinoy_probe.py \
  --employment-type 'Private Employee' --pay-period Monthly \
  --daily-days 20 --basic-pay 10000 --taxable-allowance 1000 \
  --nontaxable-allowance 0 --night-hours 20 --night-rate 30 \
  --overtime-hours 40 --overtime-rate 50 --work-schedule 5
```

The selected `pay_period` is sent as submitted: do not pre-normalize or double the salary in MotorPH and then send it. The user's captures show that the selected period affects the site's returned salary basis; the remote calculation can return a normalized amount different from the posted amount. Compare only after confirming both systems mean the same period and basis. `result` / `altresult` and the endpoint are observed page behavior, not a published stable API contract.

## Production boundary

The website does not publish a supported API contract or service guarantee. Its endpoint, session requirements, fields and response may change; the service may rate-limit or disallow automated use. The employee My Pay reference remains MotorPH's internal fixed-rate estimate; it is not fetched from Sweldong Pinoy. A separate admin-only calibration panel is available under **Payroll workspace → External reference**. It never updates payroll and is not enabled for automatic requests or employee-specific records.

## Integrated admin comparison

The optional panel is limited to 2026, `Private Employee`, `Monthly`, the captured 20-day / 5-day schedule defaults, and no night differential or overtime. It accepts basic pay plus taxable and non-taxable allowances. The form requires explicit consent for each one-time transfer; it sends only those amounts and the fixed form fields, never an employee ID/name or government number. Use synthetic amounts unless external transmission of actual compensation has been approved.

The backend:

1. Validates/normalizes the form values and the local month; unsupported employment types, periods, premiums, or form defaults are rejected before any external request.
2. Calculates the MotorPH comparison with the app's selected 2026 rules/rates version and issues a fresh homepage GET, cookie jar and form POST. It never reuses the cookie from the attached cURL.
3. Validates the external status, output fields, gross/deduction/net arithmetic, and that the returned earning components still match the submitted amounts. It preserves the raw successful JSON, normalized request fields, validated result, optional `altresult`, input scope, checked time, MotorPH ruleset/rates version, and the unknown provider-version caveat.
4. Displays all seven employee-side components and centavo deltas. A mismatch is just a review signal; neither side becomes authoritative automatically.
5. Keeps MotorPH selected by default. An eligible, validated and unambiguous Sweldong result can be selected only by a separate explicit admin action. That selection is a **reference preview only**; `appliesToPayroll` is always false, and it does not create or change a payslip. The source choice/reason is written to the app's admin audit log.
6. Offers a downloadable JSON evidence file containing the normalized inputs and raw/validated comparison. The app audit stores actor, route, time, outcome, and a short comparison ID, not the compensation values/raw result. Keep the downloaded report appropriately protected. In memory-only mode, the audit event is process-local.

The integrated route is `POST /api/admin/sweldong/compare` with the confirmation value `SEND_TO_SWELDONG`; it is admin- and CSRF-protected. A separate `POST /api/admin/sweldong/decision` records `MOTORPH` or an eligible `SWELDONG` reference choice. An external request runs outside the app's state lock so a slow provider does not block normal app transactions. The official provider URL is fixed in production code; test injection permits only loopback endpoints.

The Java adapter/API tests use a local HTTP server with a synthetic `JSESSIONID` and canned responses. They verify the form/cookie flow, validation, mismatches, access control, opt-in, explicit fallback and audit decision. They do **not** establish live-site reachability, permission to automate, provider terms, live formula parity, uptime, or production suitability. Do not use this undocumented endpoint as a final payroll calculation service without the provider's approval, a privacy review and an operational contract.
