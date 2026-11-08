package motorph.deduction;

import java.math.BigDecimal;

/**
 * SSS contribution from the compensation range table used in CP2.
 * The first threshold the monthly gross is below sets the contribution;
 * at 24,750 and above the contribution is 1,125.
 */
public class SSSDeduction extends Deduction {

    private static final BigDecimal[] THRESHOLDS = amounts(
        "3250", "3750", "4250", "4750", "5250", "5750", "6250", "6750", "7250", "7750",
        "8250", "8750", "9250", "9750", "10250", "10750", "11250", "11750", "12250", "12750",
        "13250", "13750", "14250", "14750", "15250", "15750", "16250", "16750", "17250", "17750",
        "18250", "18750", "19250", "19750", "20250", "20750", "21250", "21750", "22250", "22750",
        "23250", "23750", "24250", "24750"
    );

    private static final BigDecimal[] CONTRIBUTIONS = amounts(
        "135", "157.5", "180", "202.5", "225", "247.5", "270", "292.5", "315", "337.5",
        "360", "382.5", "405", "427.5", "450", "472.5", "495", "517.5", "540", "562.5",
        "585", "607.5", "630", "652.5", "675", "697.5", "720", "742.5", "765", "787.5",
        "810", "832.5", "855", "877.5", "900", "922.5", "945", "967.5", "990", "1012.5",
        "1035", "1057.5", "1080", "1102.5"
    );

    private static final BigDecimal MAX_CONTRIBUTION = new BigDecimal("1125");

    public SSSDeduction() {
        super("SSS");
    }

    /** @param baseAmount combined monthly gross */
    @Override
    public BigDecimal computeDeduction(BigDecimal baseAmount) {
        validateBaseAmount(baseAmount);
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (baseAmount.compareTo(THRESHOLDS[i]) < 0) {
                return CONTRIBUTIONS[i];
            }
        }
        return MAX_CONTRIBUTION;
    }

    private static BigDecimal[] amounts(String... values) {
        BigDecimal[] result = new BigDecimal[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = new BigDecimal(values[i]);
        }
        return result;
    }
}
