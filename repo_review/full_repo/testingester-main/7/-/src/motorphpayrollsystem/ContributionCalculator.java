
package motorphpayrollsystem;

public class ContributionCalculator {

    public ContributionCalculator() {
        // Helper class for payroll contributions and withholding tax.
    }

    // SSS contribution for one semi-monthly pay period.
    // Uses the original CP2 SSS contribution table.
    public double calculateSSS(double salary) {
        validateAmount(salary);

        double monthly = salary * 2.0;
        double monthlySSS;

        if (monthly < 3250) monthlySSS = 135.00;
        else if (monthly < 3750) monthlySSS = 157.50;
        else if (monthly < 4250) monthlySSS = 180.00;
        else if (monthly < 4750) monthlySSS = 202.50;
        else if (monthly < 5250) monthlySSS = 225.00;
        else if (monthly < 5750) monthlySSS = 247.50;
        else if (monthly < 6250) monthlySSS = 270.00;
        else if (monthly < 6750) monthlySSS = 292.50;
        else if (monthly < 7250) monthlySSS = 315.00;
        else if (monthly < 7750) monthlySSS = 337.50;
        else if (monthly < 8250) monthlySSS = 360.00;
        else if (monthly < 8750) monthlySSS = 382.50;
        else if (monthly < 9250) monthlySSS = 405.00;
        else if (monthly < 9750) monthlySSS = 427.50;
        else if (monthly < 10250) monthlySSS = 450.00;
        else if (monthly < 10750) monthlySSS = 472.50;
        else if (monthly < 11250) monthlySSS = 495.00;
        else if (monthly < 11750) monthlySSS = 517.50;
        else if (monthly < 12250) monthlySSS = 540.00;
        else if (monthly < 12750) monthlySSS = 562.50;
        else if (monthly < 13250) monthlySSS = 585.00;
        else if (monthly < 13750) monthlySSS = 607.50;
        else if (monthly < 14250) monthlySSS = 630.00;
        else if (monthly < 14750) monthlySSS = 652.50;
        else if (monthly < 15250) monthlySSS = 675.00;
        else if (monthly < 15750) monthlySSS = 697.50;
        else if (monthly < 16250) monthlySSS = 720.00;
        else if (monthly < 16750) monthlySSS = 742.50;
        else if (monthly < 17250) monthlySSS = 765.00;
        else if (monthly < 17750) monthlySSS = 787.50;
        else if (monthly < 18250) monthlySSS = 810.00;
        else if (monthly < 18750) monthlySSS = 832.50;
        else if (monthly < 19250) monthlySSS = 855.00;
        else if (monthly < 19750) monthlySSS = 877.50;
        else if (monthly < 20250) monthlySSS = 900.00;
        else if (monthly < 20750) monthlySSS = 922.50;
        else if (monthly < 21250) monthlySSS = 945.00;
        else if (monthly < 21750) monthlySSS = 967.50;
        else if (monthly < 22250) monthlySSS = 990.00;
        else if (monthly < 22750) monthlySSS = 1012.50;
        else if (monthly < 23250) monthlySSS = 1035.00;
        else if (monthly < 23750) monthlySSS = 1057.50;
        else if (monthly < 24250) monthlySSS = 1080.00;
        else if (monthly < 24750) monthlySSS = 1102.50;
        else monthlySSS = 1125.00;

        return monthlySSS / 2.0;
    }

    // PhilHealth contribution for one semi-monthly pay period.
    // Preserves the original CP2 formula and cap.
    public double calculatePhilHealth(double salary) {
        validateAmount(salary);

        double monthly = salary * 2.0;
        double monthlyPhilHealth = Math.min(monthly * 0.015, 900.00);

        return monthlyPhilHealth / 2.0;
    }

    // Pag-IBIG contribution for one semi-monthly pay period.
    // Preserves the original CP2 percentage and cap.
    public double calculatePagIBIG(double salary) {
        validateAmount(salary);

        double monthly = salary * 2.0;
        double monthlyPagIBIG;

        if (monthly <= 1500) {
            monthlyPagIBIG = monthly * 0.01;
        } else {
            monthlyPagIBIG = Math.min(monthly * 0.02, 100.00);
        }

        return monthlyPagIBIG / 2.0;
    }

    // Adds the three government contributions.
    public double calculateTotalContributions(
            double sss,
            double philHealth,
            double pagIBIG) {

        validateAmount(sss);
        validateAmount(philHealth);
        validateAmount(pagIBIG);

        return sss + philHealth + pagIBIG;
    }

    // Withholding tax for one semi-monthly pay period.
    // Input must already be taxable income AFTER government contributions.
    // Uses the original CP2 withholding-tax brackets.
    public double calculateWithholdingTax(double taxableIncome) {
        validateAmount(taxableIncome);

        double taxable = taxableIncome * 2.0;
        double monthlyTax;

        if (taxable <= 20833) {
            monthlyTax = 0.0;
        } else if (taxable <= 33333) {
            monthlyTax = (taxable - 20833) * 0.20;
        } else if (taxable <= 66667) {
            monthlyTax = 2500 + (taxable - 33333) * 0.25;
        } else if (taxable <= 166667) {
            monthlyTax = 10833 + (taxable - 66667) * 0.30;
        } else if (taxable <= 666667) {
            monthlyTax = 40833 + (taxable - 166667) * 0.32;
        } else {
            monthlyTax = 200833 + (taxable - 666667) * 0.35;
        }

        return monthlyTax / 2.0;
    }

    // Prevent invalid amounts from entering payroll calculations.
    private void validateAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException(
                    "Payroll amount must be valid and nonnegative."
            );
        }
    }
}