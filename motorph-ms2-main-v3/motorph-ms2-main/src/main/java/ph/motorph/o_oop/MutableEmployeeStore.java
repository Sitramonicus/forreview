package ph.motorph.o_oop;

public interface MutableEmployeeStore extends EmployeeStore {
    Employee create(Employee employee);
    Employee update(Employee employee);
}
