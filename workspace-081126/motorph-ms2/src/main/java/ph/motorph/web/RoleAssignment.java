package ph.motorph.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which seats a person holds.
 *
 * <p>A person is not their role. MotorPH's own file proves that: 10006 is an HR Manager who also has a
 * direct report, so in one day they act as HR for the roster and as a Supervisor for that person.
 * 10011 is a Payroll Manager who is also somebody's supervisor. The previous model gave each employee
 * exactly one role, which forced a choice that the business does not actually make — and it made
 * testing impossible without inventing one Firebase account per role.
 *
 * <p>So the model is: an employee number holds a <b>set of seats</b>, and each request is made
 * <b>under one seat</b>. The matrix is consulted for the seat being exercised, never for the person.
 *
 * <p>The seed below is a <b>demonstration assignment, not MotorPH policy</b>, flagged as such in
 * {@code /api/me}. It follows the positions in their own employee file so the matrix can be exercised
 * with real names and numbers.
 */
public final class RoleAssignment {

    /** One seat: the role, plus a short label explaining why this person holds it. */
    public static final class Seat {
        private final Role role;
        private final String basis;

        public Seat(Role role, String basis) {
            if (role == null) throw new IllegalArgumentException("A seat needs a role.");
            this.role = role;
            this.basis = basis == null ? "" : basis;
        }

        public Role role() {
            return role;
        }

        /** Why this person holds it, e.g. "HR Manager (10006)". Shown in the seat picker. */
        public String basis() {
            return basis;
        }

        @Override
        public String toString() {
            return role.name() + (basis.isEmpty() ? "" : " (" + basis + ")");
        }
    }

    private final Map<String, List<Seat>> seats;

    public RoleAssignment(Map<String, List<Seat>> seats) {
        Map<String, List<Seat>> copy = new LinkedHashMap<>();
        seats.forEach((employeeNumber, held) -> copy.put(employeeNumber,
            Collections.unmodifiableList(new ArrayList<>(held))));
        this.seats = Collections.unmodifiableMap(copy);
    }

    /**
     * Every seat this employee number holds, in a stable order.
     *
     * <p>Never empty for a linked person: everybody who works here holds at least the Employee seat,
     * because that is the floor of the model rather than a special grant. An employee number with no
     * record at all also resolves to Employee, which is why an unassigned number cannot read the
     * roster - Employee's only read is {@code R (Self)}.
     */
    public List<Seat> seatsOf(String employeeNumber) {
        String key = employeeNumber == null ? "" : employeeNumber.trim();
        List<Seat> held = seats.get(key);
        if (held != null && !held.isEmpty()) return held;
        if (key.isEmpty()) return Collections.emptyList();
        return List.of(new Seat(Role.EMPLOYEE, "Employee"));
    }

    /**
     * The seat to act under when the caller does not name one: the most senior seat held.
     *
     * <p>Senior rather than junior because a screen that forgets to send a seat should degrade to what
     * the person is, not silently strip them to Employee and show an empty app.
     */
    public Seat defaultSeatOf(String employeeNumber) {
        List<Seat> held = seatsOf(employeeNumber);
        if (held.isEmpty()) return null;
        Seat best = held.get(0);
        for (Seat candidate : held) {
            if (candidate.role().rank() > best.role().rank()) best = candidate;
        }
        return best;
    }

    /**
     * Resolves the seat to use for one request.
     *
     * @param requested the seat the client asked for, or null/empty to take the default
     * @throws AccessControl.Denied if the person does not hold that seat - which is the whole point:
     *                              asking to act as System Admin is refused with 403, not honoured
     */
    public Seat resolve(String employeeNumber, String requested) {
        List<Seat> held = seatsOf(employeeNumber);
        if (held.isEmpty()) {
            // Everybody who works here is at least an Employee: that is the floor, not a grant.
            // seatsOf() below returns it too, so a screen and the enforcement code never disagree
            // about how many seats a person has.
            if (requested != null && !requested.trim().isEmpty()
                && !"EMPLOYEE".equals(normalise(requested))) {
                throw new AccessControl.Denied("You do not hold the " + requested.trim()
                    + " seat. Seats are granted by MotorPH, not chosen by the browser.", 403);
            }
            return new Seat(Role.EMPLOYEE, "Employee");
        }
        String wanted = normalise(requested);
        if (wanted.isEmpty()) return defaultSeatOf(employeeNumber);
        for (Seat seat : held) {
            if (seat.role().name().equals(wanted)) return seat;
        }
        throw new AccessControl.Denied("You do not hold the " + requested.trim()
            + " seat. You hold: " + describe(held) + ".", 403);
    }

    public static String describe(List<Seat> held) {
        if (held.isEmpty()) return "none";
        StringBuilder text = new StringBuilder();
        for (Seat seat : held) {
            if (text.length() > 0) text.append(", ");
            text.append(seat.role().name());
        }
        return text.toString();
    }

    private static String normalise(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    /**
     * The demonstration assignment: the five account types mapped onto MotorPH's own positions.
     *
     * <p>Two people hold <b>more than one</b> seat, on purpose, because that is what their positions
     * actually are — and because it is the case that proves a single account can exercise two different
     * parts of the matrix without a second login:
     *
     * <ul>
     *   <li><b>10006 Andrea Mae Villanueva</b>, HR Manager, also has a direct report, so she is a
     *       Supervisor for that person too.</li>
     *   <li><b>10011 Anthony Salcedo</b>, Payroll Manager, likewise supervises three people.</li>
     * </ul>
     *
     * <p>{@link #demonstrationAllSeats()} is a separate, explicitly-labelled account used only for
     * exercising the matrix from a single sign-in.
     */
    public static RoleAssignment demoSeed() {
        Map<String, List<Seat>> seed = new LinkedHashMap<>();

        // System Admin - IT Operations and Systems.
        seed.put("10005", seats(new Seat(Role.SYSTEM_ADMIN, "IT Operations and Systems")));

        // HR - HR Manager, HR Team Leader, HR Rank and File. The Manager supervises 10007.
        seed.put("10006", seats(new Seat(Role.HR, "HR Manager"),
            new Seat(Role.SUPERVISOR, "HR Manager, supervises 10007")));
        seed.put("10007", seats(new Seat(Role.HR, "HR Team Leader")));
        seed.put("10008", seats(new Seat(Role.HR, "HR Rank and File")));
        seed.put("10009", seats(new Seat(Role.HR, "HR Rank and File")));

        // Payroll - Accounting Head, Payroll Manager, Team Leader, Rank and File.
        seed.put("10010", seats(new Seat(Role.PAYROLL, "Accounting Head")));
        seed.put("10011", seats(new Seat(Role.PAYROLL, "Payroll Manager"),
            new Seat(Role.SUPERVISOR, "Payroll Manager, supervises 3")));
        seed.put("10012", seats(new Seat(Role.PAYROLL, "Payroll Team Leader")));
        seed.put("10013", seats(new Seat(Role.PAYROLL, "Payroll Rank and File")));

        // Supervisors - the people with direct reports in the employee file.
        seed.put("10001", seats(new Seat(Role.SUPERVISOR, "Chief Executive Officer")));
        seed.put("10002", seats(new Seat(Role.SUPERVISOR, "Chief Operating Officer")));
        seed.put("10003", seats(new Seat(Role.SUPERVISOR, "Chief Finance Officer")));
        seed.put("10004", seats(new Seat(Role.SUPERVISOR, "Chief Marketing Officer")));
        seed.put("10015", seats(new Seat(Role.SUPERVISOR, "Account Manager")));
        seed.put("10016", seats(new Seat(Role.SUPERVISOR, "Account Team Leader")));

        return new RoleAssignment(seed);
    }

    /**
     * The demonstration assignment plus a single account that holds <b>all five seats at once</b>.
     *
     * <p>This exists for exactly one reason: to look at what each of the five roles sees without
     * creating five Firebase accounts. It is <b>not</b> a MotorPH position — no real person is both
     * Payroll Staff and their own supervisor's employee — so it is deliberately attached to a
     * synthetic employee number that does not appear in the employee file, and {@code /api/me}
     * reports {@code seatSeedIsDemonstration: true} for it.
     */
    public static final String DEMONSTRATION_EMPLOYEE = "10999";

    public static RoleAssignment withDemonstrationSeatHolder() {
        RoleAssignment base = demoSeed();
        Map<String, List<Seat>> seed = new LinkedHashMap<>();
        for (String employeeNumber : base.employees()) {
            seed.put(employeeNumber, base.seatsOf(employeeNumber));
        }
        seed.put(DEMONSTRATION_EMPLOYEE, seats(
            new Seat(Role.SYSTEM_ADMIN, "Demonstration account - all five seats"),
            new Seat(Role.HR, "Demonstration account - all five seats"),
            new Seat(Role.PAYROLL, "Demonstration account - all five seats"),
            new Seat(Role.SUPERVISOR, "Demonstration account - all five seats"),
            new Seat(Role.EMPLOYEE, "Demonstration account - all five seats")));
        return new RoleAssignment(seed);
    }

    /** Every employee number that holds at least one seat. */
    public List<String> employees() {
        return new ArrayList<>(seats.keySet());
    }

    private static List<Seat> seats(Seat... held) {
        List<Seat> list = new ArrayList<>();
        Collections.addAll(list, held);
        return list;
    }
}
