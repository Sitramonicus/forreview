package motorph;

import static motorph.TestSupport.*;

import java.util.List;
import motorph.user.EmployeeUser;
import motorph.user.PayrollStaff;
import motorph.user.User;

/** Tests login and the full action-to-role matrix through User references. */
public class UserTest {

    public static void main(String[] args) {
        run();
        finish();
    }

    static void run() {
        User employee = new EmployeeUser("employee", "motorph123");
        User staff = new PayrollStaff("payroll_staff", "motorph123");

        section("User: authenticate()");
        check(employee.authenticate("employee", "motorph123"), "employee logs in with correct password");
        check(staff.authenticate("payroll_staff", "motorph123"), "payroll staff logs in with correct password");
        check(!staff.authenticate("payroll_staff", "12345"), "old CP2 password no longer works");
        check(!staff.authenticate("employee", "motorph123"), "username must match this account");
        check(!employee.authenticate("", "motorph123"), "blank username rejected");
        check(!employee.authenticate("employee", ""), "blank password rejected");
        check(!employee.authenticate(null, null), "missing fields rejected");

        section("User: setPassword() (min 8 characters, MS1 rule)");
        checkRejects(() -> new EmployeeUser("employee", "12345"), "5-character password rejected");
        checkRejects(() -> staff.setPassword("        "), "blank password rejected");
        checkRejects(() -> new PayrollStaff(" ", "motorph123"), "username required");
        checkEquals("Payroll Staff", staff.getRole(), "PayrollStaff role name");
        checkEquals("Employee", employee.getRole(), "EmployeeUser role name");

        section("User: hasPermission() action-to-role matrix");
        List<String> staffOnly = List.of(User.VIEW_EMPLOYEES, User.MANAGE_EMPLOYEES, User.COMPUTE_PAYROLL,
                User.VIEW_PAYROLL_SUMMARY, User.SAVE_PAYROLL_RESULTS);
        check(employee.hasPermission(User.VIEW_OWN_RECORD), "EmployeeUser: VIEW_OWN_RECORD = Yes");
        check(!staff.hasPermission(User.VIEW_OWN_RECORD), "PayrollStaff: VIEW_OWN_RECORD = No");
        for (String action : staffOnly) {
            check(!employee.hasPermission(action), "EmployeeUser: " + action + " = No");
            check(staff.hasPermission(action), "PayrollStaff: " + action + " = Yes");
        }
        check(!staff.hasPermission("DELETE_DATABASE") && !staff.hasPermission(null), "unknown or missing action = No");
    }
}
