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

public final class OrgChart {
    private final Map<String, Employee> byEid = new LinkedHashMap<>();
    
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
                continue;   
            }
            
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
        for (Employee employee : byEid.values()) roles.put(employee.getEid(), Policies.forPosition(employee.getPosition()));
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
        return name == null ? "" : name.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
    }

    

    public Optional<Employee> employee(String eid) { return Optional.ofNullable(byEid.get(eid)); }
    public List<Employee> employees() { return Collections.unmodifiableList(new ArrayList<>(byEid.values())); }
    public boolean has(String eid) { return byEid.containsKey(eid); }
    public int size() { return byEid.size(); }
    public List<String> problems() { return Collections.unmodifiableList(problems); }

    public Optional<String> supervisorOf(String eid) { return Optional.ofNullable(supervisorOf.get(eid)); }
    public List<String> directReports(String eid) {
        return Collections.unmodifiableList(directReports.getOrDefault(eid, Collections.emptyList()));
    }

    
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

    
    public boolean hasSubordinateSupervisors(String eid) {
        for (String report : directReports(eid)) if (!directReports(report).isEmpty()) return true;
        return false;
    }

    
    public boolean leadsLeafTeam(String eid) { return !directReports(eid).isEmpty() && !hasSubordinateSupervisors(eid); }

    
    public Set<String> scopeOf(String eid) {
        Set<String> scope = new LinkedHashSet<>();
        scope.add(eid);
        scope.addAll(allReports(eid));
        return scope;
    }

    public AccessPolicy.Role roleOf(String eid) { return roles.getOrDefault(eid, AccessPolicy.Role.EMPLOYEE); }

    
    public List<Employee> rosterOf(String eid) {
        List<Employee> out = new ArrayList<>();
        for (String id : scopeOf(eid)) {
            Employee employee = byEid.get(id);
            if (employee != null) out.add(employee);
        }
        return out;
    }

    
    public synchronized void add(Employee employee) {
        if (byEid.containsKey(employee.getEid())) throw new IllegalArgumentException("Employee " + employee.getEid() + " already exists.");
        byEid.put(employee.getEid(), employee);
        roles.put(employee.getEid(), Policies.forPosition(employee.getPosition()));
        String raw = employee.getSupervisorEid();
        if (!raw.isEmpty() && byEid.containsKey(raw)) {
            supervisorOf.put(employee.getEid(), raw);
            directReports.computeIfAbsent(raw, key -> new ArrayList<>()).add(employee.getEid());
        }
    }

    
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
            if (previous != null) {
                supervisorOf.put(eid, previous);
                directReports.computeIfAbsent(previous, key -> new ArrayList<>()).add(eid);
            }
            throw new IllegalArgumentException("That move would create a reporting cycle.");
        }
    }

    
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
