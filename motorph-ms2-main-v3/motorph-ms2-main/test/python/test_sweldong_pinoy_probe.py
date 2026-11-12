import json
import sys
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import sweldong_pinoy_probe as probe


class ProbeHandler(BaseHTTPRequestHandler):
    invalid_net = False
    redirect_get = False
    redirect_post = False
    post_seen = None
    post_paths = []

    def log_message(self, _format, *_args):
        pass

    def do_GET(self):
        if self.path == "/moved":
            self.send_response(200)
            self.end_headers()
            return
        if ProbeHandler.redirect_get:
            self.send_response(302)
            self.send_header("Location", "/moved")
            self.end_headers()
            return
        self.send_response(200)
        self.send_header("Set-Cookie", "JSESSIONID=test-session; Path=/; HttpOnly")
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(b"<html>test homepage</html>")

    def do_POST(self):
        ProbeHandler.post_paths.append(self.path)
        raw = self.rfile.read(int(self.headers.get("Content-Length", "0"))).decode()
        ProbeHandler.post_seen = {
            "cookie": self.headers.get("Cookie", ""),
            "content_type": self.headers.get("Content-Type", ""),
            "origin": self.headers.get("Origin", ""),
            "fields": parse_qs(raw, keep_blank_values=True),
        }
        if ProbeHandler.redirect_post and self.path == "/":
            self.send_response(307)
            self.send_header("Location", "/moved")
            self.end_headers()
            return
        result = {
            "basic_pay": "10000.00", "taxable_allowance": "1000.00", "nontaxable_allowance": "0.00",
            "night_differential": "0.00", "overtime_pay": "0.00", "gross_pay": "11000.00",
            "sss_gsis_contribution": "500.00", "philhealth_contribution": "250.00",
            "pagibig_contribution": "200.00", "withholding_tax": "0.00",
            "total_deductions": "950.00", "net_pay": "10050.00",
        }
        if ProbeHandler.invalid_net:
            result["net_pay"] = "10051.00"
        body = json.dumps({"status": "OK", "result": result, "altresult": result}).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


class SweldongProbeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), ProbeHandler)
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.url = f"http://127.0.0.1:{cls.server.server_port}/"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=2)

    def setUp(self):
        ProbeHandler.invalid_net = False
        ProbeHandler.redirect_get = False
        ProbeHandler.redirect_post = False
        ProbeHandler.post_seen = None
        ProbeHandler.post_paths = []

    def test_browserless_cookie_session_posts_exact_form(self):
        args = probe.parser().parse_args([])
        payload = probe.make_payload(args)
        reply = probe.request_calculation(self.url, payload, timeout=3)
        self.assertEqual(reply.status_code, 200)
        self.assertEqual(reply.cookie_names, ("JSESSIONID",))
        self.assertEqual(reply.result["net_pay"], "10050.00")
        self.assertTrue(reply.altresult_matches_result)
        self.assertEqual(reply.raw_response, json.loads(reply.raw_response_text))
        self.assertEqual(reply.raw_response["result"]["basic_pay"], "10000.00")
        sent = ProbeHandler.post_seen
        self.assertIn("JSESSIONID=test-session", sent["cookie"])
        self.assertIn("application/x-www-form-urlencoded", sent["content_type"])
        self.assertEqual(sent["fields"]["pay_period"], ["Monthly"])
        self.assertEqual(sent["fields"]["basic_pay"], ["10,000.00"])
        self.assertEqual(sent["fields"]["night_differential_hours"], [""])
        self.assertEqual(len(sent["fields"]), 11)

    def test_rejects_unbalanced_response(self):
        ProbeHandler.invalid_net = True
        args = probe.parser().parse_args([])
        with self.assertRaisesRegex(probe.ProbeError, "does not balance net pay"):
            probe.request_calculation(self.url, probe.make_payload(args), timeout=3)

    def test_get_redirect_is_rejected_before_any_amounts_are_posted(self):
        ProbeHandler.redirect_get = True
        args = probe.parser().parse_args([])
        with self.assertRaisesRegex(probe.ProbeError, "Homepage GET requested a redirect"):
            probe.request_calculation(self.url, probe.make_payload(args), timeout=3)
        self.assertIsNone(ProbeHandler.post_seen)
        self.assertEqual(ProbeHandler.post_paths, [])

    def test_post_redirect_is_not_followed_with_payroll_payload(self):
        ProbeHandler.redirect_post = True
        args = probe.parser().parse_args([])
        with self.assertRaisesRegex(probe.ProbeError, "Calculator POST requested a redirect"):
            probe.request_calculation(self.url, probe.make_payload(args), timeout=3)
        self.assertEqual(ProbeHandler.post_paths, ["/"])

    def test_only_official_https_or_loopback_test_endpoint_is_allowed(self):
        self.assertEqual(probe.validate_endpoint(probe.SITE_URL), probe.SITE_URL)
        with self.assertRaisesRegex(probe.ProbeError, "localhost tests"):
            probe.validate_endpoint("http://example.com/")
        with self.assertRaisesRegex(probe.ProbeError, "credentials"):
            probe.validate_endpoint("https://user:pass@www.sweldongpinoy.com/")

    def test_optional_hour_and_rate_fields_must_be_paired(self):
        args = probe.parser().parse_args(["--night-hours", "4"])
        with self.assertRaisesRegex(probe.ProbeError, "both night-differential"):
            probe.make_payload(args)


if __name__ == "__main__":
    unittest.main()
