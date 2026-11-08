package ph.motorph.o_oop;

/** Owner-sandbox writes for fictional directory records; no production HR scope is implied. */
public interface MutableEmployeeStore extends EmployeeStore {
    Employee create(Employee employee);
    Employee update(Employee employee);
}
