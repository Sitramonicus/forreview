package ph.motorph.web;

/** Owner-sandbox writes for fictional directory records; no production HR scope is implied. */
public interface MutableEmployeeStore extends EmployeeStore {
    Employee create(Employee employee);
    Employee update(Employee employee);
}
