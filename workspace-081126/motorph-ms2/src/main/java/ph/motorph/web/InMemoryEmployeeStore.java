package ph.motorph.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Process-memory store holding the 34 rows of MotorPH's own Employee Details file.
 *
 * This is test/offline fixture data only: it is never written to Firestore, and the production
 * process refuses to start without Firebase configuration. Generated from the CSV by
 * tools/generate_fixtures.py - edit the CSV, re-run the generator, never hand-edit this file.
 */
public final class InMemoryEmployeeStore implements MutableEmployeeStore {
    private final Map<String, Employee> employees = new LinkedHashMap<>();

    public InMemoryEmployeeStore() {
        addFixture(new Employee("10001", "Manuel III Garcia", "Executive", "Chief Executive Officer", "Valero Carpark Building Valero Street 1227, Makati City", "Active",
            new BigDecimal("90000"), new BigDecimal("1500"), new BigDecimal("2000"), new BigDecimal("1000"), ""));
        addFixture(new Employee("10002", "Antonio Lim", "Executive", "Chief Operating Officer", "San Antonio De Padua 2, Block 1 Lot 8 and 2, Dasmarinas, Cavite", "Active",
            new BigDecimal("60000"), new BigDecimal("1500"), new BigDecimal("2000"), new BigDecimal("1000"), "10001"));
        addFixture(new Employee("10003", "Bianca Sofia Aquino", "Finance", "Chief Finance Officer", "Rm. 402 4/F Jiao Building Timog Avenue Cor. Quezon Avenue 1100, Quezon City", "Active",
            new BigDecimal("60000"), new BigDecimal("1500"), new BigDecimal("2000"), new BigDecimal("1000"), "10001"));
        addFixture(new Employee("10004", "Isabella Reyes", "Marketing", "Chief Marketing Officer", "460 Solanda Street Intramuros 1000, Manila", "Active",
            new BigDecimal("60000"), new BigDecimal("1500"), new BigDecimal("2000"), new BigDecimal("1000"), "10001"));
        addFixture(new Employee("10005", "Eduard Hernandez", "IT", "IT Operations and Systems", "National Highway, Gingoog,  Misamis Occidental", "Active",
            new BigDecimal("52670"), new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("1000"), "10002"));
        addFixture(new Employee("10006", "Andrea Mae Villanueva", "HR", "HR Manager", "17/85 Stracke Via Suite 042, Poblacion, Las Piñas 4783 Dinagat Islands", "Active",
            new BigDecimal("52670"), new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("1000"), "10002"));
        addFixture(new Employee("10007", "Brad San Jose", "HR", "HR Team Leader", "99 Strosin Hills, Poblacion, Bislig 5340 Tawi-Tawi", "Active",
            new BigDecimal("42975"), new BigDecimal("1500"), new BigDecimal("800"), new BigDecimal("800"), "10006"));
        addFixture(new Employee("10008", "Alice Romualdez", "HR", "HR Rank and File", "12A/33 Upton Isle Apt. 420, Roxas City 1814 Surigao del Norte", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10007"));
        addFixture(new Employee("10009", "Rosie Atienza", "HR", "HR Rank and File", "90A Dibbert Terrace Apt. 190, San Lorenzo 6056 Davao del Norte", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10007"));
        addFixture(new Employee("10010", "Roderick Alvaro", "Accounting", "Accounting Head", "#284 T. Morato corner, Scout Rallos Street, Quezon City", "Active",
            new BigDecimal("52670"), new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("1000"), "10003"));
        addFixture(new Employee("10011", "Anthony Salcedo", "Payroll", "Payroll Manager", "93/54 Shanahan Alley Apt. 183, Santo Tomas 1572 Masbate", "Active",
            new BigDecimal("50825"), new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("1000"), "10010"));
        addFixture(new Employee("10012", "Josie Lopez", "Payroll", "Payroll Team Leader", "49 Springs Apt. 266, Poblacion, Taguig 3200 Occidental Mindoro", "Active",
            new BigDecimal("38475"), new BigDecimal("1500"), new BigDecimal("800"), new BigDecimal("800"), "10011"));
        addFixture(new Employee("10013", "Martha Farala", "Payroll", "Payroll Rank and File", "42/25 Sawayn Stream, Ubay 1208 Zamboanga del Norte", "Active",
            new BigDecimal("24000"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10011"));
        addFixture(new Employee("10014", "Leila Martinez", "Payroll", "Payroll Rank and File", "37/46 Kulas Roads, Maragondon 0962 Quirino", "Active",
            new BigDecimal("24000"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10011"));
        addFixture(new Employee("10015", "Fredrick Romualdez", "Accounting", "Account Manager", "22A/52 Lubowitz Meadows, Pililla 4895 Zambales", "Active",
            new BigDecimal("53500"), new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("1000"), "10002"));
        addFixture(new Employee("10016", "Christian Mata", "Accounting", "Account Team Leader", "90 O'Keefe Spur Apt. 379, Catigbian 2772 Sulu", "Active",
            new BigDecimal("42975"), new BigDecimal("1500"), new BigDecimal("800"), new BigDecimal("800"), "10015"));
        addFixture(new Employee("10017", "Selena De Leon", "Accounting", "Account Team Leader", "89A Armstrong Trace, Compostela 7874 Maguindanao", "Active",
            new BigDecimal("41850"), new BigDecimal("1500"), new BigDecimal("800"), new BigDecimal("800"), "10015"));
        addFixture(new Employee("10018", "Allison San Jose", "Accounting", "Account Rank and File", "08 Grant Drive Suite 406, Poblacion, Iloilo City 9186 La Union", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10016"));
        addFixture(new Employee("10019", "Cydney Rosario", "Accounting", "Account Rank and File", "93A/21 Berge Points, Tapaz 2180 Quezon", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10016"));
        addFixture(new Employee("10020", "Mark Bautista", "Accounting", "Account Rank and File", "65 Murphy Center Suite 094, Poblacion, Palayan 5636 Quirino", "Active",
            new BigDecimal("23250"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10016"));
        addFixture(new Employee("10021", "Darlene Lazaro", "Accounting", "Account Rank and File", "47A/94 Larkin Plaza Apt. 179, Poblacion, Caloocan 2751 Quirino", "Active",
            new BigDecimal("23250"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10016"));
        addFixture(new Employee("10022", "Kolby Delos Santos", "Accounting", "Account Rank and File", "06A Gulgowski Extensions, Bongabon 6085 Zamboanga del Sur", "Active",
            new BigDecimal("24000"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10016"));
        addFixture(new Employee("10023", "Vella Santos", "Accounting", "Account Rank and File", "99A Padberg Spring, Poblacion, Mabalacat 3959 Lanao del Sur", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10016"));
        addFixture(new Employee("10024", "Tomas Del Rosario", "Accounting", "Account Rank and File", "80A/48 Ledner Ridges, Poblacion, Kabankalan 8870 Marinduque", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10016"));
        addFixture(new Employee("10025", "Jacklyn Tolentino", "Accounting", "Account Rank and File", "96/48 Watsica Flats Suite 734, Poblacion, Malolos 1844 Ifugao", "Active",
            new BigDecimal("24000"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10017"));
        addFixture(new Employee("10026", "Percival Gutierrez", "Accounting", "Account Rank and File", "58A Wilderman Walks, Poblacion, Digos 5822 Davao del Sur", "Active",
            new BigDecimal("24750"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10017"));
        addFixture(new Employee("10027", "Garfield Manalaysay", "Accounting", "Account Rank and File", "60 Goyette Valley Suite 219, Poblacion, Tabuk 3159 Lanao del Sur", "Active",
            new BigDecimal("24750"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10017"));
        addFixture(new Employee("10028", "Lizeth Villegas", "Accounting", "Account Rank and File", "66/77 Mann Views, Luisiana 1263 Dinagat Islands", "Active",
            new BigDecimal("24000"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10017"));
        addFixture(new Employee("10029", "Carol Ramos", "Accounting", "Account Rank and File", "72/70 Stamm Spurs, Bustos 4550 Iloilo", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10017"));
        addFixture(new Employee("10030", "Emelia Maceda", "Accounting", "Account Rank and File", "50A/83 Bahringer Oval Suite 145, Kiamba 7688 Nueva Ecija", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10017"));
        addFixture(new Employee("10031", "Delia Aguilar", "Accounting", "Account Rank and File", "95 Cremin Junction, Surallah 2809 Cotabato", "Active",
            new BigDecimal("22500"), new BigDecimal("1500"), new BigDecimal("500"), new BigDecimal("500"), "10017"));
        addFixture(new Employee("10032", "John Rafael Castro", "Sales", "Sales & Marketing", "Hi-way, Yati, Liloan Cebu", "Active",
            new BigDecimal("52670"), new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("1000"), "10004"));
        addFixture(new Employee("10033", "Carlos Ian Martinez", "Supply Chain", "Supply Chain and Logistics", "Bulala, Camalaniugan", "Active",
            new BigDecimal("52670"), new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("1000"), "10004"));
        addFixture(new Employee("10034", "Beatriz Santos", "Customer Success", "Customer Service and Relations", "Agapita Building, Metro Manila", "Active",
            new BigDecimal("52670"), new BigDecimal("1500"), new BigDecimal("1000"), new BigDecimal("1000"), "10004"));
    }

    @Override
    public synchronized List<Employee> findAll() {
        return new ArrayList<>(employees.values());
    }

    @Override
    public synchronized Optional<Employee> findById(String id) {
        return Optional.ofNullable(id == null ? null : employees.get(id.trim()));
    }

    @Override
    public synchronized Employee create(Employee employee) {
        if (employees.containsKey(employee.getId())) throw new IllegalStateException("Employee ID already exists.");
        employees.put(employee.getId(), employee);
        return employee;
    }

    @Override
    public synchronized Employee update(Employee employee) {
        if (!employees.containsKey(employee.getId())) throw new NoSuchElementException("Employee not found.");
        employees.put(employee.getId(), employee);
        return employee;
    }

    private void addFixture(Employee employee) {
        employees.put(employee.getId(), employee);
    }
}
