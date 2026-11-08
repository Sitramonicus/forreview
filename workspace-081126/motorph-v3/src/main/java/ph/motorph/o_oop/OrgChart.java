package ph.motorph.o_oop;

import ph.motorph.sp_secpol.AccessPolicy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The reporting tree built from the "Immediate Supervisor" column, and the access scope that follows from it.
 *
 * The file names supervisors as "Last, First"; the chart resolves those to employee numbers once, at
 * construction, and refuses to guess: an unresolvable supervisor is recorded as a data problem, and the person
 * it pointed at keeps a self-only scope until somebody fixes the file.
 *
 * Two rules matter for access control:
 * <ol>
 *   <li>A supervisor's roster is <em>recursive</em>. Romualdez leads the account managers, Mata and De Leon lead
 *       the two account teams beneath him, so Romualdez's roster is both teams (16 people), not the two team
 *       leaders alone.</li>
 *   <li>Team membership comes from the reporting line only. Two people in the same branch are not teammates.</li>
 * </ol>
 */
public final class OrgChart {
    private final Map<String, Employee> byEid = new LinkedHashMap<>();
    /** supervisor eid → direct reports, in file order so lists are stable. */
    private final Map<String, List<String>> directReports = new LinkedHashMap<>();
    private final Map<String, String> supervisorOf = new LinkedHashMap<>();
    private final Map<String, AccessPolicy.Role> roles = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();

    public OrgChart(List<Employee> employees) {
        for (Employee employee : employees) {
            if (byEid.put(employee.getEid(), employee) != null) {
                problems.add("Duplicate employee number " + employee.getEid() + " — only the first row is used.");
            }
        }
        Map<String, String> eidByName = new LinkedHashMap<>();
        for (Employee employee : byEid.values()) eidByName.put(normalise(employee.getFullName()), employee.getEid());

        for (Employee employee : byEid.values()) {
            String raw = employee.getSupervisorEid();
            if (raw.isEmpty() || "N/A".equalsIgnoreCase(raw)) {
                continue;   // the CEO reports to the board, which is not an account in this system
            }
            // The importer stores an employee number; a hand-built record may still hold a name.
            String supervisorEid = byEid.containsKey(raw.trim()) ? raw.trim() : eidByName.get(normalise(raw));
            if (supervisorEid == null) {
                problems.add(employee.getEid() + " names a supervisor (" + raw + ") who is not in the employee file.");
                continue;
            }
            if (supervisorEid.equals(employee.getEid())) {
                problems.add(employee.getEid() + " is listed as their own supervisor; the line is ignored.");
                continue;
            }
            supervisorOf.put(employee.getEid(), supervisorEid);
            directReports.computeIfAbsent(supervisorEid, key -> new ArrayList<>()).add(employee.getEid());
        }
        for (String eid : supervisorOf.keySet()) {
            if (hasCycle(eid)) {
                problems.add(eid + " sits in a circular reporting line; the line is ignored.");
                String supervisor = supervisorOf.remove(eid);
                directReports.getOrDefault(supervisor, new ArrayList<>()).remove(eid);
            }
        }
        for (Employee employee : byEid.values()) roles.put(employee.getEid(), RolePolicy.forPosition(employee.getPosition()));
    }

    private boolean hasCycle(String start) {
        Set<String> seen = new HashSet<>();
        String current = start;
        while (current != null && !current.isEmpty()) {
            if (!seen.add(current)) return true;
            current = supervisorOf.get(current);
        }
        return false;
    }

    private static String normalise(String name) {
        return name == null ? "" : name.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    // ── lookups ─────────────────────────────────────────────────────────────────

    public Optional<Employee> employee(String eid) { return Optional.ofNullable(byEid.get(eid)); }
    public List<Employee> employees() { return Collections.unmodifiableList(new ArrayList<>(byEid.values())); }
    public boolean has(String eid) { return byEid.containsKey(eid); }
    public int size() { return byEid.size(); }
    public List<String> problems() { return Collections.unmodifiableList(problems); }

    public Optional<String> supervisorOf(String eid) { return Optional.ofNullable(supervisorOf.get(eid)); }
    public List<String> directReports(String eid) {
        return Collections.unmodifiableList(directReports.getOrDefault(eid, Collections.emptyList()));
    }

    /** Every person below {@code eid} at any depth, breadth-first so the order reads like an org chart. */
    public List<String> allReports(String eid) {
        List<String> out = new ArrayList<>();
        Deque<String> queue = new ArrayDeque<>(directReports(eid));
        Set<String> seen = new HashSet<>();
        seen.add(eid);
        while (!queue.isEmpty()) {
            String next = queue.removeFirst();
            if (!seen.add(next)) continue;
            out.add(next);
            queue.addAll(directReports(next));
        }
        return out;
    }

    /** Supervisors that lead at least one other supervisor — the people whose roster spans several teams. */
    public boolean hasSubordinateSupervisors(String eid) {
        for (String report : directReports(eid)) if (!directReports(report).isEmpty()) return true;
        return false;
    }

    /** A leaf team is led by someone who supervises people but no other supervisors. */
    public boolean leadsLeafTeam(String eid) { return !directReports(eid).isEmpty() && !hasSubordinateSupervisors(eid); }

    /** The person and everyone beneath them; this is a supervisor's visibility. */
    public Set<String> scopeOf(String eid) {
        Set<String> scope = new LinkedHashSet<>();
        scope.add(eid);
        scope.addAll(allReports(eid));
        return scope;
    }

    public AccessPolicy.Role roleOf(String eid) { return roles.getOrDefault(eid, AccessPolicy.Role.EMPLOYEE); }

    /** Direct reports first, then reports-of-reports, for a roster view that opens at the nearest layer. */
    public List<Employee> rosterOf(String eid) {
        List<Employee> out = new ArrayList<>();
        for (String id : scopeOf(eid)) {
            Employee employee = byEid.get(id);
            if (employee != null) out.add(employee);
        }
        return out;
    }

    /** Adds a hire to the chart so scope lookups include them immediately. */
    public synchronized void add(Employee employee) {
        if (byEid.containsKey(employee.getEid())) throw new IllegalArgumentException("Employee " + employee.getEid() + " already exists.");
        byEid.put(employee.getEid(), employee);
        roles.put(employee.getEid(), RolePolicy.forPosition(employee.getPosition()));
        String raw = employee.getSupervisorEid();
        if (!raw.isEmpty() && byEid.containsKey(raw)) {
            supervisorOf.put(employee.getEid(), raw);
            directReports.computeIfAbsent(raw, key -> new ArrayList<>()).add(employee.getEid());
        }
    }

    /**
     * Moves a person to a different supervisor at runtime (HR onboarding or a reorganisation).
     * Guarded against self-reporting and against creating a cycle; both would make scope meaningless.
     */
    public synchronized void reassign(String eid, String supervisorEid) {
        if (eid == null || !byEid.containsKey(eid)) throw new IllegalArgumentException("Employee not found.");
        String newSupervisor = supervisorEid == null ? "" : supervisorEid.trim();
        if (newSupervisor.equals(eid)) throw new IllegalArgumentException("Nobody becomes their own supervisor.");
        if (!newSupervisor.isEmpty() && !byEid.containsKey(newSupervisor)) throw new IllegalArgumentException("Supervisor not found.");
        if (!newSupervisor.isEmpty() && scopeOf(newSupervisor).contains(eid) && newSupervisor.equals(eid)) {
            throw new IllegalArgumentException("That move would create a reporting cycle.");
        }
        String previous = supervisorOf.remove(eid);
        if (previous != null) directReports.getOrDefault(previous, new ArrayList<>()).remove(eid);
        if (newSupervisor.isEmpty()) return;
        supervisorOf.put(eid, newSupervisor);
        directReports.computeIfAbsent(newSupervisor, key -> new ArrayList<>()).add(eid);
        if (hasCycle(eid)) {
            supervisorOf.remove(eid);
            directReports.getOrDefault(newSupervisor, new ArrayList<>()).remove(eid);
            throw new IllegalArgumentException("That move would create a reporting cycle.");
        }
    }

    /** The role-override table lives in AccessPolicy; this lets the roster export see the effective role. */
    public Map<String, AccessPolicy.Role> rolesSnapshot() { return Collections.unmodifiableMap(new LinkedHashMap<>(roles)); }

    public Map<String, Integer> headcountByBranch() {
        Map<String, Integer> counts = new TreeMap<>();
        for (Employee employee : byEid.values()) counts.merge(employee.getBranchName(), 1, Integer::sum);
        return counts;
    }

    public Map<String, Integer> headcountByDepartment() {
        Map<String, Integer> counts = new TreeMap<>();
        for (Employee employee : byEid.values()) counts.merge(employee.getDepartment(), 1, Integer::sum);
        return counts;
    }
}
