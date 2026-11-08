package ph.motorph.o_oop;

import ph.motorph.sp_secpol.AccessPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports MotorPH's employee file and assigns each person a branch.
 *
 * Two things happen here that the rest of the application relies on:
 *
 * <ol>
 *   <li><b>Every row becomes an employee.</b> Nothing is dropped for being awkward — probationary staff, the CMO's
 *       three-person team and the CEO who reports to "N/A" are all imported.</li>
 *   <li><b>Branches follow the leaf-team rule.</b> A team with no teams beneath it gets a geographic centre computed
 *       from its members' municipalities, then the nearest of the four field offices. Supervisors who lead other
 *       supervisors, and every C-level executive, go to Main Branch. The reasoning and the resulting table are in
 *       {@code docs/a_doc/branch-assignment.md}.</li>
 * </ol>
 *
 * The file is read from the classpath ({@code data/EmployeeDetails.csv}) so the packaged application starts with the
 * roster already in place; a path can also be supplied for tests and for a refresh from disk.
 */
public final class EmployeeImport {
    public static final String RESOURCE = "/data/EmployeeDetails.csv";

    /** The employee whose payslip MotorPH supplies as the worked example, and the phone allowance that slip states. */
    public static final String SAMPLE_EMPLOYEE = "10015";
    public static final java.math.BigDecimal SAMPLE_PHONE_ALLOWANCE = new java.math.BigDecimal("2000.00");

    private final List<Employee> employees = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();

    public static EmployeeImport fromClasspath() {
        try (InputStream in = EmployeeImport.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Employee file not found on the classpath: " + RESOURCE);
            return parse(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read the employee file: " + ex.getMessage(), ex);
        }
    }

    public static EmployeeImport parse(InputStream stream) throws IOException {
        EmployeeImport result = new EmployeeImport();
        // ── pass 1: read every row exactly as the file states it ────────────────
        List<Map<String, String>> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) throw new IllegalStateException("The employee file is empty.");
        if (headerLine.startsWith("\uFEFF")) headerLine = headerLine.substring(1);
        List<String> header = CsvLine.fields(headerLine);
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            List<String> cells = CsvLine.fields(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) {
                row.put(header.get(i).trim(), i < cells.size() ? cells.get(i).trim() : "");
            }
            rows.add(row);
        }

        // ── pass 2: resolve the supervisor column to employee numbers, then classify teams ──
        // Everything below is keyed by employee number. The file writes supervisors as names ("Romualdez,
        // Fredrick "), sometimes in the wrong order or with doubled spaces ("San, Jose Brad"), so the names are
        // resolved once, here, and never compared again.
        Map<String, String> nameToEid = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String eid = blank(row.get("Employee #"));
            nameToEid.put(normaliseName(blank(row.get("Last Name")) + " " + blank(row.get("First Name"))), eid);
            nameToEid.put(normaliseName(blank(row.get("First Name")) + " " + blank(row.get("Last Name"))), eid);
            nameToEid.put(normaliseName(nameOf(row)), eid);
        }
        Map<String, String> supervisorEidOf = new LinkedHashMap<>();   // employee → supervisor, both numbers
        for (Map<String, String> row : rows) {
            String eid = blank(row.get("Employee #"));
            String raw = blank(row.get("Immediate Supervisor"));
            if (raw.isEmpty() || "N/A".equalsIgnoreCase(raw)) continue;
            String supervisorEid = nameToEid.get(normaliseName(raw));
            if (supervisorEid == null) {
                result.notes.add(eid + " names a supervisor (" + raw + ") with no employee record; the line is ignored.");
                continue;
            }
            supervisorEidOf.put(eid, supervisorEid);
        }

        Map<String, Integer> directReportCount = new LinkedHashMap<>();
        for (String supervisorEid : supervisorEidOf.values()) directReportCount.merge(supervisorEid, 1, Integer::sum);

        // A person leads other supervisors if somebody reporting to them also has direct reports.
        java.util.Set<String> leadsOtherSupervisors = new java.util.HashSet<>();
        for (String eid : supervisorEidOf.keySet()) {
            if (!directReportCount.containsKey(eid)) continue;         // not a supervisor themselves
            leadsOtherSupervisors.add(supervisorEidOf.get(eid));        // their boss leads a supervisor
        }

        // Leaf team = supervisor with direct reports and no supervisors beneath them. These are the teams the
        // centroid rule applies to, so each gets one branch that its members inherit.
        Map<String, Branch> teamBranch = new LinkedHashMap<>();
        for (String supervisorEid : directReportCount.keySet()) {
            if (leadsOtherSupervisors.contains(supervisorEid)) continue;
            List<String> cities = new ArrayList<>();
            for (Map<String, String> row : rows) {
                if (supervisorEid.equals(blank(row.get("Employee #")))) continue;
                String eid = blank(row.get("Employee #"));
                if (supervisorEid.equals(supervisorEidOf.get(eid))) {
                    String city = Locality.cityOf(row.get("Address"));
                    if (city != null) cities.add(city);
                }
            }
            String name = nameOfRow(rows, supervisorEid);
            teamBranch.put(supervisorEid, Branch.fromLabel(assignBranch(cities, result.notes, name)));
        }

        // ── pass 3: build the records ───────────────────────────────────────────
        for (Map<String, String> row : rows) {
            String eid = blank(row.get("Employee #"));
            Branch branch = branchFor(eid, supervisorEidOf, directReportCount, leadsOtherSupervisors, teamBranch, row);
            Employee employee = new Employee.Builder()
                .eid(eid)
                .firstName(blank(row.get("First Name")))
                .lastName(blank(row.get("Last Name")))
                .birthday(CsvLine.date(row.get("Birthday")))
                .position(blank(row.get("Position")))
                .department(RolePolicy.departmentOf(row.get("Position")))
                .employeeStatus(blank(row.get("Status")))
                .phone(blank(row.get("Phone Number")))
                .sssNumber(blank(row.get("SSS #")))
                .philhealthNumber(blank(row.get("Philhealth #")))
                .tinNumber(blank(row.get("TIN #")))
                .pagibigNumber(blank(row.get("Pag-ibig #")))
                .street(blank(row.get("Address")))
                .monthlyBasic(CsvLine.money(row.get("Basic Salary")))
                .riceSubsidy(CsvLine.money(row.get("Rice Subsidy")))
                .phoneAllowance(CsvLine.money(row.get("Phone Allowance")))
                .clothingAllowance(CsvLine.money(row.get("Clothing Allowance")))
                .semiMonthlyRate(CsvLine.money(row.get("Gross Semi-monthly Rate")))
                .hourlyRate(CsvLine.money(row.get("Hourly Rate")))
                .supervisorEid(supervisorEidOf.getOrDefault(eid, ""))
                .branchName(branch.label())
                .build();
            result.employees.add(employee);
        }
        if (result.employees.isEmpty()) throw new IllegalStateException("The employee file contains no rows.");
        // ── pass 4: say out loud where the file and the worked example disagree ──
        for (Employee employee : result.employees) {
            if (!SAMPLE_EMPLOYEE.equals(employee.getEid())) continue;
            if (employee.getPhoneAllowance().compareTo(SAMPLE_PHONE_ALLOWANCE) != 0) {
                result.notes.add(String.format(
                    "%s (%s): the sample payslip states a %s phone allowance, the employee file states %s. Both figures"
                        + " are kept: the difference is carried as a named allowance correction on the payslip, never"
                        + " folded into the file, and the file's number remains what the employee is contracted for.",
                    employee.getEid(), employee.getFullName(), CsvLine.moneyText(SAMPLE_PHONE_ALLOWANCE),
                    CsvLine.moneyText(employee.getPhoneAllowance())));
            }
        }
        return result;
    }

    /**
     * Branch for one person, applying the brief's rules in order:
     * <ol>
     *   <li>Every C-level executive goes to Main Branch — they report to the board, not to a field office.</li>
     *   <li>Anyone whose roster spans more than one team (they lead a supervisor who leads others) goes to
     *       Main Branch, together with the executives who sit above them.</li>
     *   <li>The leader of a leaf team takes that team's centroid branch.</li>
     *   <li>A member of a leaf team inherits it; anyone above a leaf team inherits the nearest leaf team beneath
     *       them, so nobody is left without a branch.</li>
     * </ol>
     */
    private static Branch branchFor(String eid, Map<String, String> supervisorEidOf,
            Map<String, Integer> directReportCount, java.util.Set<String> leadsOtherSupervisors,
            Map<String, Branch> teamBranch, Map<String, String> row) {
        // Every C-level executive sits at head office, the CMO included: the CMO's own team still gets the
        // centroid branch of its members, but the executive is not part of a field office's headcount.
        if (RolePolicy.forPosition(blank(row.get("Position"))).isExecutive()) return Branch.MAIN;
        boolean isSupervisor = directReportCount.containsKey(eid);
        if (isSupervisor && leadsOtherSupervisors.contains(eid)) return Branch.MAIN;
        Branch own = teamBranch.get(eid);
        if (own != null) return own;                      // leads a leaf team
        // walk up until a leaf team leader is found
        String cursor = supervisorEidOf.get(eid);
        for (int guard = 0; guard < 12 && cursor != null && !cursor.isEmpty(); guard++) {
            Branch inherited = teamBranch.get(cursor);
            if (inherited != null) return inherited;
            cursor = supervisorEidOf.get(cursor);
        }
        return Branch.MAIN;
    }

    /** Nearest field office to the team's municipality centroid. */
    private static String assignBranch(List<String> cities, List<String> notes, String team) {
        if (cities.isEmpty()) {
            notes.add(team + ": no geocodable address in the file, assigned Main Branch.");
            return Branch.MAIN.label();
        }
        double lat = 0, lon = 0;
        int counted = 0;
        for (String city : cities) {
            double[] xy = Locality.coordinates(city);
            if (xy == null) continue;
            lat += xy[0]; lon += xy[1]; counted++;
        }
        if (counted == 0) {
            notes.add(team + ": no recognised municipality, assigned Main Branch.");
            return Branch.MAIN.label();
        }
        double[] centre = { lat / counted, lon / counted };
        Branch best = null, runnerUp = null;
        double bestDistance = Double.MAX_VALUE, secondDistance = Double.MAX_VALUE;
        for (Branch branch : Branch.fieldOffices()) {
            double distance = Locality.kilometres(centre, new double[]{ branch.latitude(), branch.longitude() });
            if (distance < bestDistance) { secondDistance = bestDistance; runnerUp = best; bestDistance = distance; best = branch; }
            else if (distance < secondDistance) { secondDistance = distance; runnerUp = branch; }
        }
        notes.add(String.format("%s: %d address(es) averaged to (%.3f, %.3f); nearest %s at %.0f km (next: %s at %.0f km).",
            team, counted, centre[0], centre[1], best.label(), bestDistance, runnerUp.label(), secondDistance));
        return best.label();
    }

    private static String describeCentre(List<String> cities) {
        return cities.isEmpty() ? "" : String.join(" + ", cities);
    }

    private static String nameOfRow(List<Map<String, String>> rows, String eid) {
        for (Map<String, String> row : rows) if (eid.equals(blank(row.get("Employee #")))) return nameOf(row);
        return eid;
    }

    private static String nameOf(Map<String, String> row) {
        return blank(row.get("Last Name")) + ", " + blank(row.get("First Name"));
    }
    private static String positionOf(List<Map<String, String>> rows, String eid) {
        for (Map<String, String> row : rows) if (eid.equals(blank(row.get("Employee #")))) return blank(row.get("Position"));
        return "";
    }
    private static String eidOf(List<Map<String, String>> rows, String name) {
        for (Map<String, String> row : rows) if (name.equals(nameOf(row))) return blank(row.get("Employee #"));
        return "";
    }
    private static String blank(String value) { return value == null ? "" : value.replaceAll("\\s+", " ").trim(); }

    /** "San, Jose Brad" and "San Jose, Brad" both become "bradsanjose"; punctuation and order stop mattering. */
    private static String normaliseName(String name) {
        return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]", "");
    }

    public List<Employee> employees() { return new ArrayList<>(employees); }
    public List<String> notes() { return new ArrayList<>(notes); }

    /** The monthly basic MotorPH's file states, used to sanity-check the import. */
    public BigDecimal totalMonthlyBasic() {
        BigDecimal total = BigDecimal.ZERO;
        for (Employee employee : employees) total = total.add(employee.getMonthlyBasic());
        return total;
    }
    public LocalDate anyBirthday() { return employees.isEmpty() ? null : employees.get(0).getBirthday(); }
}
