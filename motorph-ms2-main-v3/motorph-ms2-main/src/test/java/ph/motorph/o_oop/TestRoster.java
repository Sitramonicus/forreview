package ph.motorph.o_oop;

public final class TestRoster {
    private static EmployeeImport imported;

    private TestRoster() {}

    
    public static synchronized EmployeeImport imported() {
        if (imported == null) imported = EmployeeImport.fromClasspath();
        return imported;
    }

    
    public static InMemoryEmployeeStore store() {
        InMemoryEmployeeStore store = new InMemoryEmployeeStore();
        for (Employee employee : imported().employees()) store.seed(employee);
        return store;
    }

    
    public static EmployeeService employees() {
        return new EmployeeService(store());
    }

    
    public static InMemoryEmployeeScheduleStore schedules() {
        InMemoryEmployeeScheduleStore schedules = new InMemoryEmployeeScheduleStore();
        for (Employee employee : imported().employees()) schedules.assign(employee.getId());
        return schedules;
    }

    
    public static Employee employee(String employeeId) {
        return imported().employees().stream().filter(e -> e.getEid().equals(employeeId)).findFirst()
            .orElseThrow(() -> new AssertionError("The employee file has no employee " + employeeId + "."));
    }

    
    public static java.math.BigDecimal monthlyBasic(String employeeId) {
        return employee(employeeId).getMonthlyBasic();
    }
}
