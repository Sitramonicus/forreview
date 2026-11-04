package motorphpayrollsystem;

/**
 * Thin alias kept for backward compatibility.
 * All nav() callers now use PayrollAdminUI directly — this class is unused
 * but retained so legacy references (e.g. editProfileUI) still compile.
 */
public class PayrollUI extends PayrollAdminUI {

    public PayrollUI(String empNo, String[] emp) {
        super(empNo, emp);
    }
}
