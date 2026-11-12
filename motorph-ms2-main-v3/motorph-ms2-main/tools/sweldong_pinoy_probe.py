#!/usr/bin/env python3
"""Browser-free diagnostic for the captured Sweldong Pinoy form POST.

This is an operator-run probe, not a supported API client or a MotorPH payroll fallback.
It sends the amounts passed on the command line to Sweldong Pinoy. Use test figures unless
external transmission of actual payroll inputs is approved.
"""
from __future__ import annotations

import argparse
import http.cookiejar
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from typing import Any
from urllib.parse import urlsplit

SITE_URL = "https://www.sweldongpinoy.com/"
USER_AGENT = "MotorPH-Reference-Probe/1.0"
MAX_RESPONSE_BYTES = 256_000
EMPLOYMENT_TYPES = (
    "Private Employee", "Government Employee", "Self-Employed Individual",
    "Minimum Wage Employee", "Domestic Worker", "RHQ/ROHQ Employee",
)
PAY_PERIODS = ("Daily", "Weekly", "Semi-Monthly", "Monthly")
REQUIRED_RESULT = (
    "basic_pay", "taxable_allowance", "nontaxable_allowance", "night_differential",
    "overtime_pay", "gross_pay", "sss_gsis_contribution", "philhealth_contribution",
    "pagibig_contribution", "withholding_tax", "total_deductions", "net_pay",
)


class ProbeError(Exception):
    """An endpoint, transport, or response-validation failure."""


class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    """Never forward payroll amounts to a redirect target."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


@dataclass(frozen=True)
class ProbeReply:
    status_code: int
    content_type: str
    cookie_names: tuple[str, ...]
    result: dict[str, Any]
    altresult: dict[str, Any] | None
    altresult_matches_result: bool | None
    raw_response_text: str
    raw_response: dict[str, Any]


def money_text(value: str, name: str) -> str:
    text = str(value or "").strip().replace(",", "")
    if not re.fullmatch(r"\d{1,10}(?:\.\d{1,2})?", text):
        raise ProbeError(f"{name} must be a non-negative peso amount with up to two decimals.")
    amount = Decimal(text)
    return f"{amount:,.2f}"


def quantity_text(value: str, name: str, maximum: Decimal = Decimal("1000000")) -> str:
    text = str(value or "").strip()
    if text == "":
        return ""
    if not re.fullmatch(r"\d{1,8}(?:\.\d{1,2})?", text):
        raise ProbeError(f"{name} must be a non-negative number with up to two decimals.")
    amount = Decimal(text)
    if amount > maximum:
        raise ProbeError(f"{name} is outside the supported probe range.")
    return f"{amount:.2f}"


def make_payload(args: argparse.Namespace) -> dict[str, str]:
    if args.employment_type not in EMPLOYMENT_TYPES:
        raise ProbeError("Unsupported employment type.")
    if args.pay_period not in PAY_PERIODS:
        raise ProbeError("Unsupported payroll period.")
    if not 1 <= args.daily_days <= 31:
        raise ProbeError("Daily-day count must be 1 to 31.")
    if args.work_schedule not in (5, 6, 7):
        raise ProbeError("Work schedule must be 5, 6, or 7 days.")
    nd_hours = quantity_text(args.night_hours, "Night differential hours")
    nd_rate = quantity_text(args.night_rate, "Night differential rate", Decimal("100"))
    ot_hours = quantity_text(args.overtime_hours, "Overtime hours")
    ot_rate = quantity_text(args.overtime_rate, "Overtime rate", Decimal("100"))
    if bool(nd_hours) != bool(nd_rate):
        raise ProbeError("Set both night-differential hours and rate, or leave both blank.")
    if bool(ot_hours) != bool(ot_rate):
        raise ProbeError("Set both overtime hours and rate, or leave both blank.")
    return {
        "employment_type": args.employment_type,
        "pay_period": args.pay_period,
        "payroll_period_daily_days": str(args.daily_days),
        "basic_pay": money_text(args.basic_pay, "Basic pay"),
        "taxable_allowance": money_text(args.taxable_allowance, "Taxable allowance"),
        "nontaxable_allowance": money_text(args.nontaxable_allowance, "Non-taxable allowance"),
        "night_differential_hours": nd_hours,
        "night_differential_rate": nd_rate,
        "overtime_hours": ot_hours,
        "overtime_rate": ot_rate,
        "work_schedule": str(args.work_schedule),
    }


def validate_endpoint(url: str) -> str:
    parsed = urlsplit(url)
    if parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise ProbeError("Endpoint URL must not contain credentials, query parameters, or a fragment.")
    if parsed.path not in ("", "/"):
        raise ProbeError("The captured calculator posts to the homepage path only.")
    if parsed.scheme == "https" and parsed.hostname == "www.sweldongpinoy.com":
        return url if url.endswith("/") else url + "/"
    if parsed.scheme == "http" and parsed.hostname in ("127.0.0.1", "localhost") and parsed.port:
        return url if url.endswith("/") else url + "/"
    raise ProbeError("Use the official HTTPS homepage; HTTP overrides are allowed only for localhost tests.")


def cents(value: Any, name: str) -> int:
    try:
        amount = Decimal(str(value).replace(",", "")).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    except (InvalidOperation, AttributeError, ValueError) as exc:
        raise ProbeError(f"Response field {name} is not a valid monetary value.") from exc
    if not amount.is_finite() or amount < 0 or amount > Decimal("10000000000"):
        raise ProbeError(f"Response field {name} is outside the supported monetary range.")
    return int(amount * 100)


def result_amounts(value: dict[str, Any]) -> dict[str, int]:
    return {field: cents(value[field], field) for field in REQUIRED_RESULT}


def validate_result(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ProbeError(f"Response {label} is missing or is not an object.")
    for field in REQUIRED_RESULT:
        if field not in value:
            raise ProbeError(f"Response {label} is missing {field}.")
        cents(value[field], field)
    amount = {field: cents(value[field], field) for field in REQUIRED_RESULT}
    gross_parts = sum(amount[name] for name in (
        "basic_pay", "taxable_allowance", "nontaxable_allowance", "night_differential", "overtime_pay"))
    contributions = sum(amount[name] for name in (
        "sss_gsis_contribution", "philhealth_contribution", "pagibig_contribution", "withholding_tax"))
    if abs(amount["gross_pay"] - gross_parts) > 1:
        raise ProbeError(f"Response {label} does not balance gross pay against its returned components.")
    if abs(amount["total_deductions"] - contributions) > 1:
        raise ProbeError(f"Response {label} does not balance total deductions against its returned components.")
    if abs(amount["net_pay"] - (amount["gross_pay"] - amount["total_deductions"])) > 1:
        raise ProbeError(f"Response {label} does not balance net pay against gross and deductions.")
    return value


def read_limited(response: Any) -> bytes:
    body = response.read(MAX_RESPONSE_BYTES + 1)
    if len(body) > MAX_RESPONSE_BYTES:
        raise ProbeError("Calculator response exceeded the 256 KB safety limit.")
    return body


def origin(url: str) -> str:
    parsed = urlsplit(url)
    return f"{parsed.scheme}://{parsed.netloc}"


def request_calculation(url: str, payload: dict[str, str], timeout: float = 20.0,
                        opener: urllib.request.OpenerDirector | None = None) -> ProbeReply:
    endpoint = validate_endpoint(url)
    cookie_jar = http.cookiejar.CookieJar()
    client = opener or urllib.request.build_opener(
        NoRedirectHandler(), urllib.request.HTTPCookieProcessor(cookie_jar))
    if opener is not None:
        cookie_processor = getattr(opener, "_motorph_cookie_jar", None)
        if cookie_processor is not None:
            cookie_jar = cookie_processor

    get_request = urllib.request.Request(endpoint, headers={
        "User-Agent": USER_AGENT,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    })
    try:
        with client.open(get_request, timeout=timeout) as page:
            if not 200 <= page.status < 300:
                raise ProbeError(f"Homepage GET failed with HTTP {page.status}.")
            if origin(page.geturl()) != origin(endpoint):
                raise ProbeError("Homepage redirected to a different origin; refusing to send the payload.")
            read_limited(page)
    except urllib.error.HTTPError as exc:
        if 300 <= exc.code < 400:
            raise ProbeError("Homepage GET requested a redirect; refusing to continue or transmit amounts.") from exc
        raise ProbeError(f"Homepage GET failed with HTTP {exc.code}.") from exc
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        raise ProbeError(f"Homepage GET failed: {exc.reason if isinstance(exc, urllib.error.URLError) else exc}") from exc

    body = urllib.parse.urlencode(payload).encode("ascii")
    post_request = urllib.request.Request(endpoint, data=body, method="POST", headers={
        "User-Agent": USER_AGENT,
        "Accept": "application/json",
        "Origin": origin(endpoint),
        "Referer": endpoint,
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
    })
    try:
        with client.open(post_request, timeout=timeout) as response:
            status_code = response.status
            content_type = response.headers.get("Content-Type", "")
            final_url = response.geturl()
            response_body = read_limited(response)
    except urllib.error.HTTPError as exc:
        if 300 <= exc.code < 400:
            raise ProbeError("Calculator POST requested a redirect; response rejected without following it.") from exc
        body_preview = exc.read(1500).decode("utf-8", "replace")
        raise ProbeError(f"Calculator POST failed with HTTP {exc.code}; body preview: {body_preview}") from exc
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        raise ProbeError(f"Calculator POST failed: {exc.reason if isinstance(exc, urllib.error.URLError) else exc}") from exc

    if origin(final_url) != origin(endpoint):
        raise ProbeError("Calculator POST redirected to a different origin; response rejected.")
    if not 200 <= status_code < 300:
        raise ProbeError(f"Calculator POST failed with HTTP {status_code}.")
    try:
        decoded = json.loads(response_body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        preview = response_body[:1500].decode("utf-8", "replace")
        raise ProbeError(f"Calculator did not return JSON. Body preview: {preview}") from exc
    if not isinstance(decoded, dict) or decoded.get("status") != "OK":
        raise ProbeError("Calculator response did not contain status OK.")
    if "result" not in decoded:
        raise ProbeError("Calculator response is missing its primary result.")

    result = validate_result(decoded.get("result"), "result")
    alt = decoded.get("altresult")
    if alt is not None:
        alt = validate_result(alt, "altresult")
    result_match: bool | None = None
    if alt is not None:
        result_match = result_amounts(result) == result_amounts(alt)
    cookie_names = tuple(sorted({cookie.name for cookie in cookie_jar}))
    raw_text = response_body.decode("utf-8")
    return ProbeReply(status_code, content_type, cookie_names, result, alt, result_match,
                      raw_text, decoded)


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description="Submit a synthetic payroll input to the captured Sweldong Pinoy form without a browser.")
    result.add_argument("--url", default=SITE_URL, help=argparse.SUPPRESS)
    result.add_argument("--timeout", type=float, default=20.0)
    result.add_argument("--employment-type", choices=EMPLOYMENT_TYPES, default="Private Employee")
    result.add_argument("--pay-period", choices=PAY_PERIODS, default="Monthly")
    result.add_argument("--daily-days", type=int, default=20)
    result.add_argument("--basic-pay", default="10000.00")
    result.add_argument("--taxable-allowance", default="1000.00")
    result.add_argument("--nontaxable-allowance", default="0.00")
    result.add_argument("--night-hours", default="")
    result.add_argument("--night-rate", default="")
    result.add_argument("--overtime-hours", default="")
    result.add_argument("--overtime-rate", default="")
    result.add_argument("--work-schedule", type=int, choices=(5, 6, 7), default=5)
    return result


def printable(reply: ProbeReply) -> dict[str, Any]:
    return {
        "httpStatus": reply.status_code,
        "contentType": reply.content_type,
        "sessionCookieNames": list(reply.cookie_names),
        "result": reply.result,
        "altresult": reply.altresult,
        "altresultMatchesResult": reply.altresult_matches_result,
        "rawResponseText": reply.raw_response_text,
        "rawResponse": reply.raw_response,
    }


def main(argv: list[str] | None = None) -> int:
    args = parser().parse_args(argv)
    try:
        payload = make_payload(args)
        if args.timeout <= 0 or args.timeout > 60:
            raise ProbeError("Timeout must be greater than 0 and no more than 60 seconds.")
        print("Browser-free Sweldong Pinoy reference probe; use approved test figures only.")
        print("A GET establishes a cookie session; the form POST follows in the same HTTP session.")
        reply = request_calculation(args.url, payload, args.timeout)
        print(json.dumps(printable(reply), indent=2, ensure_ascii=False))
        print("PASS: a browserless form request returned a balanced result. This is not proof of a supported public API or payroll correctness.")
        return 0
    except ProbeError as exc:
        print(f"NOT VERIFIED: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
