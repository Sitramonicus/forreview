package ph.motorph.o_oop;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeSet;

public final class HrRecords {
    public static final List<String> ONBOARDING_TASKS = Collections.unmodifiableList(
        Arrays.asList("Contract signed", "Government IDs submitted", "Payroll set up", "Orientation done"));
    public static final List<String> OFFBOARDING_TASKS = Collections.unmodifiableList(
        Arrays.asList("Equipment returned", "Clearance signed", "Final pay released", "Certificate of employment issued"));
    public static final List<String> PAY_METHODS = Collections.unmodifiableList(Arrays.asList("Direct deposit", "Check"));

    
    public static final class Task {
        public final String name, doneBy, doneAt;

        Task(String name, String doneBy, String doneAt) { this.name = name; this.doneBy = doneBy; this.doneAt = doneAt; }

        public boolean isDone() { return !doneBy.isEmpty(); }
    }

    public static final class Employment {
        public final String employeeId, onboardedBy, payMethod;
        public final LocalDate startDate;
        public final long[] monthlyAllowanceCents;
        public LocalDate lastDay;
        public String separationReason = "", offboardedBy = "";
        public final List<Task> onboarding = new ArrayList<>();
        public final List<Task> offboarding = new ArrayList<>();

        Employment(String employeeId, LocalDate startDate, long[] allowances, String payMethod, String onboardedBy) {
            this.employeeId = employeeId;
            this.startDate = startDate;
            this.monthlyAllowanceCents = allowances.clone();
            this.payMethod = payMethod;
            this.onboardedBy = onboardedBy;
        }

        
        public boolean employedOn(LocalDate day) {
            return (startDate == null || !day.isBefore(startDate)) && (lastDay == null || !day.isAfter(lastDay));
        }
    }

    private final TreeSet<String> departments = new TreeSet<>(Arrays.asList("Finance", "IT", "Operations", "People Operations"));
    private final TreeSet<String> positions = new TreeSet<>(Arrays.asList(
        "HR Coordinator", "Operations Associate", "Operations Lead", "Payroll Associate", "System Administrator"));
    private final Map<String, Employment> employment = new LinkedHashMap<>();

    public synchronized List<String> departments() { return new ArrayList<>(departments); }
    public synchronized List<String> positions() { return new ArrayList<>(positions); }

    public synchronized boolean hasDepartment(String name) { return departments.contains(name); }
    public synchronized boolean hasPosition(String name) { return positions.contains(name); }

    
    public synchronized void addCatalogItem(String kind, String name) {
        String clean = catalogName(name);
        if (!list(kind).add(clean)) throw new IllegalStateException(capitalize(kind) + " \"" + clean + "\" already exists.");
    }

    
    public synchronized void renameCatalogItem(String kind, String from, String to) {
        TreeSet<String> items = list(kind);
        String clean = catalogName(to);
        if (!items.contains(from)) throw new NoSuchElementException(capitalize(kind) + " \"" + from + "\" was not found.");
        if (items.contains(clean)) throw new IllegalStateException(capitalize(kind) + " \"" + clean + "\" already exists.");
        items.remove(from);
        items.add(clean);
    }

    private TreeSet<String> list(String kind) {
        if ("department".equals(kind)) return departments;
        if ("position".equals(kind)) return positions;
        throw new IllegalArgumentException("Kind must be department or position.");
    }

    private static String catalogName(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty() || clean.length() > 60) throw new IllegalArgumentException("Name is required (60 characters max).");
        return clean;
    }

    private static String capitalize(String kind) { return Character.toUpperCase(kind.charAt(0)) + kind.substring(1); }

    
    public synchronized Employment onboard(String employeeId, LocalDate startDate, long[] allowances, String payMethod, String by) {
        if (!PAY_METHODS.contains(payMethod)) throw new IllegalArgumentException("Pay method must be Direct deposit or Check.");
        for (long cents : allowances) if (cents < 0 || cents > 10_000_000) throw new IllegalArgumentException("Allowances must be ₱0 to ₱100,000 a month.");
        Employment record = new Employment(employeeId, startDate, allowances, payMethod, by);
        for (String task : ONBOARDING_TASKS) {
            record.onboarding.add("Payroll set up".equals(task) ? new Task(task, by, Instant.now().toString()) : new Task(task, "", ""));
        }
        employment.put(employeeId, record);
        return record;
    }

    public synchronized Employment offboard(String employeeId, LocalDate lastDay, String reason, String by) {
        String clean = reason == null ? "" : reason.trim();
        if (clean.isEmpty() || clean.length() > 200) throw new IllegalArgumentException("A separation reason is required (200 characters max).");
        Employment record = employment.computeIfAbsent(employeeId, id -> new Employment(id, null, PayrollProfile.monthlyAllowances(id), PayrollProfile.payMethod(id), ""));
        if (record.lastDay != null) throw new IllegalStateException("This person is already offboarded.");
        if (record.startDate != null && lastDay.isBefore(record.startDate)) throw new IllegalArgumentException("The last day cannot be before the start date.");
        record.lastDay = lastDay;
        record.separationReason = clean;
        record.offboardedBy = by;
        for (String task : OFFBOARDING_TASKS) record.offboarding.add(new Task(task, "", ""));
        return record;
    }

    public synchronized Employment completeTask(String employeeId, String task, String by) {
        Employment record = find(employeeId).orElseThrow(() -> new NoSuchElementException("No onboarding or offboarding record for this person."));
        for (List<Task> list : Arrays.asList(record.onboarding, record.offboarding)) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).name.equals(task)) {
                    if (list.get(i).isDone()) throw new IllegalStateException("\"" + task + "\" is already done.");
                    list.set(i, new Task(task, by, Instant.now().toString()));
                    return record;
                }
            }
        }
        throw new IllegalArgumentException("Unknown checklist item for this person.");
    }

    public synchronized Optional<Employment> find(String employeeId) { return Optional.ofNullable(employment.get(employeeId)); }

    public synchronized List<Employment> all() { return new ArrayList<>(employment.values()); }

    public synchronized void remove(String employeeId) { employment.remove(employeeId); }

    
    public synchronized boolean isSeparated(String employeeId, LocalDate today) {
        Employment record = employment.get(employeeId);
        return record != null && record.lastDay != null && today.isAfter(record.lastDay);
    }

    public synchronized long[] monthlyAllowances(String employeeId) {
        Employment record = employment.get(employeeId);
        return record != null && record.startDate != null ? record.monthlyAllowanceCents.clone() : PayrollProfile.monthlyAllowances(employeeId);
    }

    public synchronized String payMethod(String employeeId) {
        Employment record = employment.get(employeeId);
        return record != null && record.startDate != null ? record.payMethod : PayrollProfile.payMethod(employeeId);
    }
}
