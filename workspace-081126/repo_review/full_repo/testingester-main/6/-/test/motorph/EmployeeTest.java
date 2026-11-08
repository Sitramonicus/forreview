package motorph;

import static motorph.TestSupport.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import motorph.model.Employee;
import motorph.model.ProbationaryEmployee;
import motorph.model.RegularEmployee;

/** Tests Employee validation, shared gross pay, and the overridden subclass methods. */
public class EmployeeTest {

    public static void main(String[] args) {
        run();
        finish();
    }

    static RegularEmployee sampleRegular() {
        return new RegularEmployee("10001", "Garcia", "Manuel III", LocalDate.of(1983, 10, 11),
                "Valero Carpark Building Valero Street 1227 Makati City", "966-860-270", "44-4506057-3",
                "820126853951", "442-605-657-000", "691295330870", "Chief Executive Officer", "N/A",
                new BigDecimal("90000"), new BigDecimal("1500"), new BigDecimal("2000"), new BigDecimal("1000"),
                new BigDecimal("45000"), new BigDecimal("535.71"));
    }

    static ProbationaryEmployee sampleProbationary() {
        return new ProbationaryEmployee("10021", "Lazaro", "Darlene", LocalDate.of(1993, 11, 27),
                "Cebu City", "171-912-344", "40-2556321-8", "100500001919", "513-021-889-000", "122313211443",
                "Account Rank and File", "Ilagan Hiroko", new BigDecimal("23250"), new BigDecimal("1500"),
                new BigDecimal("500"), new BigDecimal("500"), new BigDecimal("11625"), new BigDecimal("138.39"));
    }

    static void run() {
        section("Employee: valid objects and getters");
        RegularEmployee regular = sampleRegular();
        checkEquals("10001", regular.getEmployeeId(), "employee ID is stored");
        checkEquals("Manuel III Garcia", regular.getFullName(), "getFullName() returns first + last");
        checkEquals("Regular", regular.getStatus(), "Regular subclass sets status Regular");

        section("Employee: setter validation (MS1 Week 2 rules)");
        checkRejects(() -> regular.setLastName(""), "last name is required");
        checkRejects(() -> regular.setFirstName("Ann3"), "first name rejects digits");
        checkRejects(() -> regular.setPosition("Clerk 2"), "position rejects digits");
        checkRejects(() -> regular.setImmediateSupervisor("Boss1"), "supervisor rejects digits");
        checkRejects(() -> regular.setPhoneNumber("9668602700"), "phone must be ###-###-###");
        checkRejects(() -> regular.setSssNumber("44-4506057"), "SSS must be ##-#######-#");
        checkRejects(() -> regular.setPhilHealthNumber("82012685395"), "PhilHealth must be 12 digits");
        checkRejects(() -> regular.setTin("442-605-657"), "TIN must be ###-###-###-###");
        checkRejects(() -> regular.setPagIbigNumber("69129533087X"), "Pag-IBIG must be 12 digits");
        checkRejects(() -> regular.setStatus("Contractual"), "status only Regular or Probationary");
        checkRejects(() -> regular.setBasicSalary(BigDecimal.ZERO), "basic salary must be > 0");
        checkRejects(() -> regular.setHourlyRate(new BigDecimal("-1")), "hourly rate must be > 0");
        checkRejects(() -> regular.setRiceSubsidy(null), "rice subsidy is required");
        checkRejects(() -> regular.setBirthday(LocalDate.now().plusDays(1)), "birthday cannot be in the future");
        checkRejects(() -> new RegularEmployee("ABC", "Garcia", "Manuel", LocalDate.of(1983, 10, 11), "Makati",
                "966-860-270", "44-4506057-3", "820126853951", "442-605-657-000", "691295330870", "CEO", "N/A",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE),
                "employee ID must be a positive whole number");
        checkEquals("Garcia", regular.getLastName(), "rejected updates leave the old value in place");

        section("Employee: computeGrossPay (shared, final)");
        checkAmount("4285.68", regular.computeGrossPay(8), "8 h x 535.71 = 4,285.68");
        checkAmount("0.00", regular.computeGrossPay(0), "0 hours gives 0");
        checkAmount("40088.97", regular.computeGrossPay(4490 / 60.0),
                "4,490 min x 535.71 / 60 = 40,088.965 rounds HALF_UP to 40,088.97 (CP2 double gave .97 by luck)");
        checkRejects(() -> regular.computeGrossPay(-1), "negative hours rejected");

        section("Polymorphism: same Employee reference, different subclass behavior");
        List<Employee> employees = List.of(sampleRegular(), sampleProbationary());
        Employee first = employees.get(0);
        Employee second = employees.get(1);
        checkAmount("4500.00", first.computeAllowances(), "Regular allowances = 1,500 + 2,000 + 1,000");
        checkEquals("Regular", first.getEmploymentType(), "Regular getEmploymentType()");
        checkAmount("0.00", second.computeAllowances(), "Probationary allowances = 0 (project assumption)");
        checkEquals("Probationary", second.getEmploymentType(), "Probationary getEmploymentType()");
        checkAmount("1107.12", second.computeGrossPay(8), "inherited computeGrossPay works for Probationary: 8 h x 138.39");
    }
}
