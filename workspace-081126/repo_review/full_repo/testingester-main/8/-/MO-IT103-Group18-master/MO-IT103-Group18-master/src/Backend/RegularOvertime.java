package Backend;

public class RegularOvertime extends OvertimeRecord {
    private static final double RATE_MULTIPLIER = 1.25;

    public RegularOvertime(String employeeId, String date, String timeIn, String timeOut, double hourlyRate) {
        super(employeeId, date, timeIn, timeOut, hourlyRate);
    }

    // hoursWorked x hourlyRate x 1.25 (approved overtime only)
    @Override
    public double computeOvertimePay() {
        if (!isApproved()) {
            return 0;
        }
        return getOvertimeHours() * getHourlyRate() * RATE_MULTIPLIER;
    }

    public static void main(String[] args) {
        // 08:00-19:00 = 11 hrs - 1 hr break = 10 hrs worked -> 2 overtime hours
        RegularOvertime ot = new RegularOvertime("10001", "10-26-2026", "08:00", "19:00", 100);
        System.out.println("Overtime Hours: " + ot.getOvertimeHours());
        System.out.println("Pay while Pending: " + ot.computeOvertimePay());
        ot.setApprovalStatus("Approved");
        System.out.println("Regular Overtime Pay (Approved): " + ot.computeOvertimePay());
    }
}