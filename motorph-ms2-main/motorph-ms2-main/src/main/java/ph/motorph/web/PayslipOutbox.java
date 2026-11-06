package ph.motorph.web;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Requirement 10: payslips are emailed to each employee when pay is released, with retry for failed deliveries.
 * shortcut: simulated: nothing leaves the computer. Messages go to a synthetic work address (@motorph.example, a reserved
 * test domain), and "Simulate mail outage" makes sends fail so the retry can be shown. A real system uses an SMTP service.
 */
public final class PayslipOutbox {
    public static final class Message {
        public final String id, payslipId, batchId, employeeId, to, subject, body;
        public String status, lastAttemptAt, reason;
        public int attempts;
        /** The payslip PDF sent with the email ("" for the payroll team's report). */
        public String attachment = "";

        Message(String id, String payslipId, String batchId, String employeeId, String to, String subject, String body) {
            this.id = id;
            this.payslipId = payslipId;
            this.batchId = batchId;
            this.employeeId = employeeId;
            this.to = to;
            this.subject = subject;
            this.body = body;
        }
    }

    private final List<Message> messages = new ArrayList<>();
    private boolean outage;
    private int next = 1;

    /** firstname.lastname@motorph.example, the same rule as the roster's synthetic contact (web/team-roster.js). */
    public static String workEmail(String name) {
        String plain = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z\\s]", "").trim();
        return (plain.isEmpty() ? "employee" : String.join(".", plain.split("\\s+"))) + "@motorph.example";
    }

    public synchronized boolean outage() { return outage; }

    public synchronized void setOutage(boolean on) { outage = on; }

    /** One message per released payslip, sent at once. */
    public synchronized List<Message> sendForBatch(DisbursementStore.Batch batch, String periodLabel, String now) {
        List<Message> sent = new ArrayList<>();
        for (DisbursementStore.Line line : batch.lines) {
            String net = String.format(Locale.ROOT, "₱%,.2f", java.math.BigDecimal.valueOf(line.netCents, 2));
            Message message = new Message(String.format("EM-%04d", next++), line.payslipId, batch.id, line.employeeId, workEmail(line.employeeName),
                "Your MotorPH payslip for " + periodLabel,
                "Hi " + line.employeeName.split("\\s+")[0] + ", your payslip " + line.payslipId + " for " + periodLabel + " is ready: net pay " + net
                    + " by " + line.method + ". Open MotorPH > My pay to review it, and file a payroll inquiry ticket if something looks wrong.");
            message.attachment = line.payslipId + ".pdf";
            attempt(message, now);
            messages.add(message);
            sent.add(message);
        }
        return sent;
    }

    /** Design doc: after distribution, "sends a collated report to the payroll team". */
    public synchronized Message sendTeamReport(DisbursementStore.Batch batch, String periodLabel, List<Message> emails, String now) {
        long sent = emails.stream().filter(item -> "Sent".equals(item.status)).count();
        StringBuilder body = new StringBuilder("Distribution " + batch.id + " for " + periodLabel + ": " + batch.lines.size() + " payslip(s), net total "
            + String.format(Locale.ROOT, "₱%,.2f", java.math.BigDecimal.valueOf(batch.totalCents, 2)) + "; " + sent + " emailed, " + (emails.size() - sent) + " failed.");
        for (DisbursementStore.Line line : batch.lines) {
            body.append(" ").append(line.payslipId).append(" ").append(line.employeeName).append(" ")
                .append(String.format(Locale.ROOT, "₱%,.2f", java.math.BigDecimal.valueOf(line.netCents, 2))).append(" (").append(line.method).append(");");
        }
        Message report = new Message(String.format("EM-%04d", next++), "", batch.id, TEAM, "payroll@motorph.example",
            "Payroll distribution report " + batch.id, body.toString());
        attempt(report, now);
        messages.add(report);
        return report;
    }

    /** Pseudo employee ID for the payroll team's report: only Payroll Staff, HR and the System Admin can read it. */
    public static final String TEAM = "PAYROLL-TEAM";

    /** "Distributed" once every payslip email of the batch is sent; otherwise how many still failed. */
    public synchronized String batchStatus(String batchId) {
        long failed = messages.stream().filter(item -> batchId.equals(item.batchId) && !TEAM.equals(item.employeeId) && "Failed".equals(item.status)).count();
        return failed == 0 ? "Distributed" : "Released, " + failed + " email(s) failed";
    }

    public synchronized Message resend(String id, String now) {
        for (Message message : messages) {
            if (message.id.equals(id)) {
                attempt(message, now);
                return message;
            }
        }
        throw new java.util.NoSuchElementException("Email not found.");
    }

    /** Retries every failed message; returns how many are now sent. */
    public synchronized int retryFailed(String now) {
        int fixed = 0;
        for (Message message : messages) {
            if (!"Failed".equals(message.status)) continue;
            attempt(message, now);
            if ("Sent".equals(message.status)) fixed++;
        }
        return fixed;
    }

    public synchronized List<Message> list() {
        List<Message> copy = new ArrayList<>(messages);
        Collections.reverse(copy); // newest first
        return copy;
    }

    private void attempt(Message message, String now) {
        message.attempts++;
        message.lastAttemptAt = now;
        message.status = outage ? "Failed" : "Sent";
        message.reason = outage ? "Mail server unreachable (simulated outage)" : "";
    }
}
