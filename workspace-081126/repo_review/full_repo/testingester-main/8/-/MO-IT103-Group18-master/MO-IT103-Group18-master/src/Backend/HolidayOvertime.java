package Backend;

public class HolidayOvertime extends OvertimeRecord {
    private static final double RATE_MULTIPLIER = 2.0;

    public HolidayOvertime(String employeeId, String date, String timeIn, String timeOut, double hourlyRate) {
        super(employeeId, date, timeIn, timeOut, hourlyRate);
    }

    // hoursWorked x hourlyRate x 2.0 (approved overtime only)
    @Override
    public double computeOvertimePay() {
        if (!isApproved()) {
            return 0;
        }
        return getOvertimeHours() * getHourlyRate() * RATE_MULTIPLIER;
    }

    public static void main(String[] args) {
        HolidayOvertime ot = new HolidayOvertime("10001", "12-25-2026", "08:00", "19:00", 100);
        ot.setApprovalStatus("Approved");
        System.out.println("Overtime Hours: " + ot.getOvertimeHours());
        System.out.println("Holiday Overtime Pay: " + ot.computeOvertimePay());
    }
}
