package ph.motorph.o_oop;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    
    public synchronized void add(Employee employee) {
        byEid.put(employee.getEid(), employee);
        chart.add(employee);
    }

    
    public List<String> importNotes() { return EmployeeImport.fromClasspath().notes(); }
}
