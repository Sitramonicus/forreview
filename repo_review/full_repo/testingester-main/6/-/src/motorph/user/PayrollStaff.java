package motorph.user;

import java.util.Set;

/**
 * A payroll staff account. May view, add, edit, and delete employees, and
 * compute payroll, view the summary, and save results (matches CP2's
 * DashboardFrame, EmployeeFrame, and SalaryComputationFrame).
 */
public class PayrollStaff extends User {

    private static final Set<String> ALLOWED_ACTIONS = Set.of(
        VIEW_EMPLOYEES, MANAGE_EMPLOYEES, COMPUTE_PAYROLL, VIEW_PAYROLL_SUMMARY, SAVE_PAYROLL_RESULTS
    );

    public PayrollStaff(String username, String password) {
        super(username, password, "Payroll Staff");
    }

    @Override
    public boolean hasPermission(String action) {
        return action != null && ALLOWED_ACTIONS.contains(action);
    }
}
