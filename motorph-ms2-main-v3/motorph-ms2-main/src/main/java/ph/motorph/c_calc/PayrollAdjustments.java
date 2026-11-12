package ph.motorph.c_calc;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class PayrollAdjustments {
    public static final List<String> EARNING_TYPES = Collections.unmodifiableList(Arrays.asList("Bonus", "Incentive", "Adjustment"));
    
    public static final List<String> ALLOWANCE_TYPES = Collections.unmodifiableList(
        Arrays.asList("Rice subsidy adjustment", "Phone allowance adjustment", "Clothing allowance adjustment"));

    
    public static final List<String> OTHER_NON_TAXABLE = Collections.unmodifiableList(Arrays.asList("Reimbursement", "Cash advance"));

    public static boolean isTaxable(String type) { return !ALLOWANCE_TYPES.contains(type) && !OTHER_NON_TAXABLE.contains(type); }
    public static final List<String> DEDUCTION_TYPES = Collections.unmodifiableList(
        Arrays.asList("Company loan", "Insurance", "Benefit contribution", "Cash advance repayment", "Other voluntary"));
    
    public static final String RECOVERY_TYPE = "Overpayment recovery";
    private static final long MAX_CENTS = 100_000_000; 

    public static final class Earning {
        public final String id, employeeId, type, description, createdBy, createdAt;
        public final YearMonth month;
        public final int cutoff;
        public final long amountCents;

        Earning(String id, String employeeId, YearMonth month, int cutoff, String type, String description, long amountCents, String createdBy) {
            this.id = id; this.employeeId = employeeId; this.month = month; this.cutoff = cutoff; this.type = type;
            this.description = description; this.amountCents = amountCents; this.createdBy = createdBy; this.createdAt = Instant.now().toString();
        }
    }

    public static final class Deduction {
        public final String id, employeeId, type, description, createdBy, createdAt;
        public final YearMonth fromMonth, toMonth; 
        public final long perCutoffCents;
        
        public final int onlyCutoff;

        Deduction(String id, String employeeId, String type, String description, long perCutoffCents, YearMonth fromMonth, YearMonth toMonth, String createdBy) {
            this(id, employeeId, type, description, perCutoffCents, fromMonth, toMonth, createdBy, 0);
        }

        Deduction(String id, String employeeId, String type, String description, long perCutoffCents, YearMonth fromMonth, YearMonth toMonth,
                String createdBy, int onlyCutoff) {
            this.onlyCutoff = onlyCutoff;
            this.id = id; this.employeeId = employeeId; this.type = type; this.description = description; this.perCutoffCents = perCutoffCents;
            this.fromMonth = fromMonth; this.toMonth = toMonth; this.createdBy = createdBy; this.createdAt = Instant.now().toString();
        }

        public boolean appliesTo(YearMonth month) {
            return !month.isBefore(fromMonth) && (toMonth == null || !month.isAfter(toMonth));
        }
    }

    private final Map<String, Earning> earnings = new LinkedHashMap<>();
    private final Map<String, Deduction> deductions = new LinkedHashMap<>();

    public synchronized Earning addEarning(String employeeId, YearMonth month, int cutoff, String type, String description, long amountCents, String createdBy) {
        if (cutoff != 1 && cutoff != 2) throw new IllegalArgumentException("Cutoff must be 1 (1-15) or 2 (16-end).");
        if (!EARNING_TYPES.contains(type) && !ALLOWANCE_TYPES.contains(type) && !OTHER_NON_TAXABLE.contains(type)) {
            throw new IllegalArgumentException("Earning type must be one of " + EARNING_TYPES + ", an allowance correction " + ALLOWANCE_TYPES + ", or " + OTHER_NON_TAXABLE + ".");
        }
        checkAmount(amountCents);
        Earning item = new Earning("ER-" + suffix(), employeeId, month, cutoff, type, text(description), amountCents, createdBy);
        earnings.put(item.id, item);
        return item;
    }

    public synchronized Deduction addDeduction(String employeeId, String type, String description, long perCutoffCents,
            YearMonth fromMonth, YearMonth toMonth, String createdBy) {
        if (!DEDUCTION_TYPES.contains(type)) throw new IllegalArgumentException("Deduction type must be one of " + DEDUCTION_TYPES + ".");
        checkAmount(perCutoffCents);
        if (toMonth != null && toMonth.isBefore(fromMonth)) throw new IllegalArgumentException("The last month must not be before the first month.");
        Deduction item = new Deduction("VD-" + suffix(), employeeId, type, text(description), perCutoffCents, fromMonth, toMonth, createdBy);
        deductions.put(item.id, item);
        return item;
    }

    
    public synchronized Earning addAllowanceCorrection(String employeeId, YearMonth month, int cutoff, String type, String description, long signedCents, String createdBy) {
        if (!ALLOWANCE_TYPES.contains(type)) throw new IllegalArgumentException("Not an allowance correction type.");
        if (signedCents == 0 || Math.abs(signedCents) > MAX_CENTS) throw new IllegalArgumentException("The correction must be non-zero and at most ₱1,000,000.");
        Earning item = new Earning("ER-" + suffix(), employeeId, month, cutoff, type, text(description), signedCents, createdBy);
        earnings.put(item.id, item);
        return item;
    }

    
    public synchronized Deduction addOneCutoffDeduction(String employeeId, YearMonth month, int cutoff, long cents, String description, String createdBy) {
        checkAmount(cents);
        Deduction item = new Deduction("VD-" + suffix(), employeeId, "Other voluntary", text(description), cents, month, month, createdBy, cutoff);
        deductions.put(item.id, item);
        return item;
    }

    
    public synchronized Deduction addRecovery(String employeeId, YearMonth month, int cutoff, long cents, String description, String createdBy) {
        checkAmount(cents);
        Deduction item = new Deduction("VD-" + suffix(), employeeId, RECOVERY_TYPE, text(description), cents, month, month, createdBy, cutoff);
        deductions.put(item.id, item);
        return item;
    }

    
    public synchronized String remove(String id) {
        Earning earning = earnings.remove(id);
        if (earning != null) return earning.employeeId;
        Deduction deduction = deductions.remove(id);
        if (deduction != null) return deduction.employeeId;
        throw new NoSuchElementException("Payroll item was not found.");
    }

    public synchronized String ownerOf(String id) {
        if (earnings.containsKey(id)) return earnings.get(id).employeeId;
        if (deductions.containsKey(id)) return deductions.get(id).employeeId;
        throw new NoSuchElementException("Payroll item was not found.");
    }

    public synchronized Earning earning(String id) { return earnings.get(id); }

    public synchronized List<Earning> earningsFor(String employeeId, YearMonth month, int cutoff) {
        List<Earning> result = new ArrayList<>();
        for (Earning item : earnings.values()) {
            if (item.employeeId.equals(employeeId) && item.month.equals(month) && item.cutoff == cutoff) result.add(item);
        }
        return result;
    }

    
    public synchronized List<Deduction> deductionsFor(String employeeId, YearMonth month) {
        List<Deduction> result = new ArrayList<>();
        for (Deduction item : deductions.values()) {
            if (item.employeeId.equals(employeeId) && item.appliesTo(month)) result.add(item);
        }
        return result;
    }

    public synchronized List<Earning> allEarnings() { return new ArrayList<>(earnings.values()); }
    public synchronized List<Deduction> allDeductions() { return new ArrayList<>(deductions.values()); }

    private static void checkAmount(long cents) {
        if (cents <= 0 || cents > MAX_CENTS) throw new IllegalArgumentException("Amount must be more than ₱0 and at most ₱1,000,000.");
    }

    private static String text(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() > 120) throw new IllegalArgumentException("Description must be 120 characters or fewer.");
        return trimmed;
    }

    private static String suffix() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT); }
}
