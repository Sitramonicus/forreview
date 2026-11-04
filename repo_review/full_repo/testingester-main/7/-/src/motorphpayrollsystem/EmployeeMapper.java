
package motorphpayrollsystem;

public class EmployeeMapper {

    public static FullTimeEmployee fromCSV(String[] emp) {

        if (emp == null || emp.length < 19) {
            throw new IllegalArgumentException(
                    "Employee CSV record is incomplete."
            );
        }

        int employeeID = Integer.parseInt(emp[0].trim());

        double basicSalary = parseAmount(emp[13]);
        double riceSubsidy = parseAmount(emp[14]);
        double phoneAllowance = parseAmount(emp[15]);
        double clothingAllowance = parseAmount(emp[16]);
        double hourlyRate = parseAmount(emp[18]);

        return new FullTimeEmployee(
                employeeID,
                emp[1],
                emp[2],
                emp[6],
                emp[7],
                emp[8],
                emp[9],
                emp[10],
                emp[12],
                emp[11],
                riceSubsidy,
                phoneAllowance,
                clothingAllowance,
                hourlyRate,
                basicSalary
        );
    }

    private static double parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        return Double.parseDouble(
                value.replace(",", "").trim()
        );
    }
}