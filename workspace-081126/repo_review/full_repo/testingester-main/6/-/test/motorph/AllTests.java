package motorph;

/**
 * Runs every backend test in build order (MS1 mentor advice): Employee,
 * AttendanceRecord, CSVDataHandler, each Deduction, PayrollProcessor, the
 * two-cutoff example, the CP2 comparison, and User permissions.
 * In NetBeans: right-click this file > Run File. Run from the project folder
 * so data/ and test/cp2-expected/ are found.
 */
public class AllTests {

    public static void main(String[] args) throws Exception {
        EmployeeTest.run();
        AttendanceRecordTest.run();
        CSVDataHandlerTest.run();
        DeductionTest.run();
        PayrollProcessorTest.run();
        TwoCutoffExample.run();
        Cp2ComparisonTest.run();
        UserTest.run();
        TestSupport.finish();
    }
}
