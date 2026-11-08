package ph.motorph.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The link between a Firebase account and the MotorPH person it belongs to.
 *
 * <p>This is the piece whose absence forced the awkward question "do I need five Firebase accounts to
 * test five roles?". The answer is no, and the reason is that <b>an account is not a person and a
 * person is not a role</b>:
 *
 * <ul>
 *   <li>One Firebase account can be linked to several employee numbers. A payroll clerk who also
 *       covers reception needs one login, two people, and a switch between them.</li>
 *   <li>One employee number can hold several seats (see {@link RoleAssignment}). 10006 is an HR
 *       Manager who also supervises somebody, so she switches between HR and Supervisor.</li>
 * </ul>
 *
 * <p>So a session is a triple: <b>account → person → seat being acted under</b>. The client picks
 * which person and which seat, exactly as a real "switch account" control would; the server refuses
 * any person or seat the account is not linked to.
 *
 * <p>In Firebase mode this record lives in Firestore under the account's own uid and is written by HR
 * (or by a one-time bootstrap for the demonstration account). The in-memory form below is used by the
 * isolated fixture runtime, which has no accounts to speak of.
 */
public final class SeatRegistry {

    /** One link: this account, acting as this employee, holding these seats. */
    public static final class Link {
        private final String accountId;
        private final String employeeNumber;
        private final List<RoleAssignment.Seat> seats;

        public Link(String accountId, String employeeNumber, List<RoleAssignment.Seat> seats) {
            this.accountId = accountId;
            this.employeeNumber = employeeNumber;
            this.seats = Collections.unmodifiableList(new ArrayList<>(seats));
        }

        public String accountId() {
            return accountId;
        }

        public String employeeNumber() {
            return employeeNumber;
        }

        public List<RoleAssignment.Seat> seats() {
            return seats;
        }

        /** The most senior seat, for when the client does not name one. */
        public RoleAssignment.Seat defaultSeat() {
            RoleAssignment.Seat best = null;
            for (RoleAssignment.Seat seat : seats) {
                if (best == null || seat.role().rank() > best.role().rank()) best = seat;
            }
            return best;
        }

        public boolean holds(Role role) {
            for (RoleAssignment.Seat seat : seats) {
                if (seat.role() == role) return true;
            }
            return false;
        }
    }

    /** Mutable so HR can add a link at runtime; reads are synchronised on the same lock. */
    private final Map<String, List<Link>> byAccount = new LinkedHashMap<>();

    public SeatRegistry(Map<String, List<Link>> byAccount) {
        this.byAccount.putAll(byAccount);
    }

    /**
     * Links an account to a person, or replaces that person's seats if the link already exists.
     *
     * <p>Called by HR through {@code POST /api/me/links}. Adding a <b>second</b> person to an account is
     * the case that makes one login enough for somebody who does two jobs; adding a second time for the
     * same person is how a seat is granted or revoked.
     *
     * @return the link that is now in force
     */
    public synchronized Link link(String accountId, String employeeNumber, List<RoleAssignment.Seat> seats) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("A link needs an account id.");
        }
        if (employeeNumber == null || !employeeNumber.trim().matches("[0-9]{5}")) {
            throw new IllegalArgumentException("A link needs a five-digit employee number.");
        }
        if (seats == null || seats.isEmpty()) {
            throw new IllegalArgumentException("A link needs at least one seat.");
        }
        Link link = new Link(accountId.trim(), employeeNumber.trim(), seats);
        List<Link> existing = new ArrayList<>(byAccount.getOrDefault(link.accountId(),
            Collections.emptyList()));
        existing.removeIf(candidate -> candidate.employeeNumber().equals(link.employeeNumber()));
        existing.add(link);
        byAccount.put(link.accountId(), Collections.unmodifiableList(existing));
        return link;
    }

    /** Removes one person from an account. Returns whether anything was removed. */
    public synchronized boolean unlink(String accountId, String employeeNumber) {
        List<Link> existing = byAccount.get(accountId == null ? "" : accountId.trim());
        if (existing == null) return false;
        List<Link> remaining = new ArrayList<>(existing);
        boolean removed = remaining.removeIf(candidate -> candidate.employeeNumber().equals(employeeNumber));
        if (remaining.isEmpty()) byAccount.remove(accountId.trim());
        else byAccount.put(accountId.trim(), Collections.unmodifiableList(remaining));
        return removed;
    }

    /** Every link in force, for an HR view of who can act as whom. */
    public synchronized List<Link> allLinks() {
        List<Link> all = new ArrayList<>();
        byAccount.values().forEach(all::addAll);
        return all;
    }

    public synchronized List<Link> linksOf(String accountId) {
        List<Link> links = byAccount.get(accountId == null ? "" : accountId);
        return links == null ? Collections.emptyList() : links;
    }

    public boolean isLinked(String accountId) {
        return !linksOf(accountId).isEmpty();
    }

    /**
     * An unlinked account is a supported state, not an error: the person has signed in but HR has not
     * given them an employee number yet. It can read nothing and can ask for access - which is better
     * than refusing the sign-in outright and better than granting Employee by default.
     */
    public AccessControl.Actor actorFor(String accountId, String employeeNumber, String seat, RoleAssignment fallback) {
        if (!isLinked(accountId)) return new AccessControl.Actor("", Role.APPLICANT);
        List<Link> links = linksOf(accountId);
        Link link = null;
        if (employeeNumber != null && !employeeNumber.trim().isEmpty()) {
            for (Link candidate : links) {
                if (candidate.employeeNumber().equals(employeeNumber.trim())) link = candidate;
            }
            if (link == null) {
                throw new AccessControl.Denied("This account is not linked to employee "
                    + employeeNumber.trim() + ". It may act as: " + describePeople(links) + ".", 403);
            }
        } else {
            link = links.get(0);
        }
        RoleAssignment.Seat chosen = resolveSeat(link, seat);
        if (chosen == null && fallback != null) {
            chosen = fallback.defaultSeatOf(link.employeeNumber());
        }
        if (chosen == null) chosen = link.defaultSeat();
        return new AccessControl.Actor(link.employeeNumber(), chosen.role());
    }

    private RoleAssignment.Seat resolveSeat(Link link, String requested) {
        if (requested == null || requested.trim().isEmpty()) return null;
        String wanted = requested.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (RoleAssignment.Seat seat : link.seats()) {
            if (seat.role().name().equals(wanted)) return seat;
        }
        throw new AccessControl.Denied("You do not hold the " + requested.trim()
            + " seat as " + link.employeeNumber() + ". You hold: "
            + RoleAssignment.describe(link.seats()) + ".", 403);
    }

    private static String describePeople(List<Link> links) {
        StringBuilder text = new StringBuilder();
        for (Link link : links) {
            if (text.length() > 0) text.append(", ");
            text.append(link.employeeNumber());
        }
        return text.toString();
    }

    /**
     * The isolated-fixture registry.
     *
     * <p>In fixture mode there is no account to link, so every available person is reachable from the
     * one synthetic account. That is not a permission hole: the seat each person holds is still decided
     * by {@code RoleAssignment}, so 10018 is an Employee and nothing else however they are reached.
     *
     * <p>Only ever installed by the test-mode runtime; the production entry point refuses to start
     * without Firebase, so this registry cannot exist there.
     */
    public static SeatRegistry fixtureAllSeatsEverywhere() {
        java.util.List<RoleAssignment.Seat> all = java.util.List.of(
            new RoleAssignment.Seat(Role.SYSTEM_ADMIN, "Fixture session - any seat"),
            new RoleAssignment.Seat(Role.HR, "Fixture session - any seat"),
            new RoleAssignment.Seat(Role.PAYROLL, "Fixture session - any seat"),
            new RoleAssignment.Seat(Role.SUPERVISOR, "Fixture session - any seat"),
            new RoleAssignment.Seat(Role.EMPLOYEE, "Fixture session - any seat"));
        Map<String, List<Link>> seed = new LinkedHashMap<>();
        seed.put("fixture-account", List.of(
            new Link("fixture-account", RoleAssignment.DEMONSTRATION_EMPLOYEE, all)));
        return new SeatRegistry(seed);
    }

    public static SeatRegistry fixtureSeatHolder() {
        List<RoleAssignment.Seat> allSeats = new ArrayList<>();
        allSeats.add(new RoleAssignment.Seat(Role.SYSTEM_ADMIN, "Fixture account - all five seats"));
        allSeats.add(new RoleAssignment.Seat(Role.HR, "Fixture account - all five seats"));
        allSeats.add(new RoleAssignment.Seat(Role.PAYROLL, "Fixture account - all five seats"));
        allSeats.add(new RoleAssignment.Seat(Role.SUPERVISOR, "Fixture account - all five seats"));
        allSeats.add(new RoleAssignment.Seat(Role.EMPLOYEE, "Fixture account - all five seats"));

        Map<String, List<Link>> seed = new LinkedHashMap<>();
        // A synthetic person who really exists in the roster, so the matrix has real rows to work on.
        seed.put("fixture-account", List.of(
            new Link("fixture-account", RoleAssignment.DEMONSTRATION_EMPLOYEE, allSeats)));
        return new SeatRegistry(seed);
    }
}
