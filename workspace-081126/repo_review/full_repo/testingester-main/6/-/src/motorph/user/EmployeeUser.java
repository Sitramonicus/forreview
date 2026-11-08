package motorph.user;

/**
 * An employee account. May only look up their own employee number, name, and
 * birthday (least-privilege access, matches CP2's EmployeeLookupFrame).
 */
public class EmployeeUser extends User {

    public EmployeeUser(String username, String password) {
        super(username, password, "Employee");
    }

    @Override
    public boolean hasPermission(String action) {
        return VIEW_OWN_RECORD.equals(action);
    }
}
