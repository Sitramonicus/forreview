package ph.motorph.o_oop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The imported roster, indexed by employee number — the one place the rest of the application looks up a person.
 *
 * It is read-mostly: importing is a start-up act, and the only runtime change is HR adding a hire, which is
 * appended here so pay lookups see it immediately.
 */
public final class EmpRecords {
    private final Map<String, Employee> byEid = new LinkedHashMap<>();
    private final OrgChart chart;

    public EmpRecords(List<Employee> employees) {
        for (Employee employee : employees) byEid.put(employee.getEid(), employee);
        this.chart = new OrgChart(employees);
    }

    public static EmpRecords fromClasspath() {
        EmployeeImport imported = EmployeeImport.fromClasspath();
        return new EmpRecords(imported.employees());
    }

    public Optional<Employee> find(String employeeId) {
        return employeeId == null ? Optional.empty() : Optional.ofNullable(byEid.get(employeeId.trim()));
    }

    public Employee require(String employeeId) {
        return find(employeeId).orElseThrow(() -> new java.util.NoSuchElementException("No employee " + employeeId + " in the imported roster."));
    }

    public List<Employee> all() { return List.copyOf(byEid.values()); }
    public int size() { return byEid.size(); }
    public OrgChart orgChart() { return chart; }

    /** Records a new hire so pay, attendance and scope lookups can see them without a restart. */
    public synchronized void add(Employee employee) {
        byEid.put(employee.getEid(), employee);
        chart.add(employee);
    }

    /** What the employee file states, for the branch and org reports. */
    public List<String> importNotes() { return EmployeeImport.fromClasspath().notes(); }
}
