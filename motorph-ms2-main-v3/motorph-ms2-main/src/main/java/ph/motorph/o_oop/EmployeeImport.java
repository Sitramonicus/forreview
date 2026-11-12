package ph.motorph.o_oop;


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

public final class EmployeeImport {
    public static final String RESOURCE = "/data/EmployeeDetails.csv";

    
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

        
        
        
        
        Map<String, String> nameToEid = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String eid = blank(row.get("Employee #"));
            nameToEid.put(normaliseName(blank(row.get("Last Name")) + " " + blank(row.get("First Name"))), eid);
            nameToEid.put(normaliseName(blank(row.get("First Name")) + " " + blank(row.get("Last Name"))), eid);
        }
        Map<String, String> supervisorEidOf = new LinkedHashMap<>();   
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

        
        java.util.Set<String> leadsOtherSupervisors = new java.util.HashSet<>();
        for (Map.Entry<String, String> pair : supervisorEidOf.entrySet()) {
            if (!directReportCount.containsKey(pair.getKey())) continue;
            leadsOtherSupervisors.add(pair.getValue());
        }

        
        
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

        
        for (Map<String, String> row : rows) {
            String eid = blank(row.get("Employee #"));
            Branch branch = branchFor(eid, supervisorEidOf, directReportCount, leadsOtherSupervisors, teamBranch, row);
            Employee employee = new Employee.Builder()
                .eid(eid)
                .firstName(blank(row.get("First Name")))
                .lastName(blank(row.get("Last Name")))
                .birthday(CsvLine.date(row.get("Birthday")))
                .position(blank(row.get("Position")))
                .department(Policies.departmentOf(row.get("Position")))
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

    
    private static Branch branchFor(String eid, Map<String, String> supervisorEidOf,
            Map<String, Integer> directReportCount, java.util.Set<String> leadsOtherSupervisors,
            Map<String, Branch> teamBranch, Map<String, String> row) {
        
        
        if (Policies.forPosition(blank(row.get("Position"))).isExecutive()) return Branch.MAIN;
        boolean isSupervisor = directReportCount.containsKey(eid);
        if (isSupervisor && leadsOtherSupervisors.contains(eid)) return Branch.MAIN;
        Branch own = teamBranch.get(eid);
        if (own != null) return own;                      
        
        String cursor = supervisorEidOf.get(eid);
        for (int guard = 0; guard < 12 && cursor != null && !cursor.isEmpty(); guard++) {
            Branch inherited = teamBranch.get(cursor);
            if (inherited != null) return inherited;
            cursor = supervisorEidOf.get(cursor);
        }
        return Branch.MAIN;
    }
    private static String blank(String value) { return value == null ? "" : value.replaceAll("\\s+", " ").trim(); }

    
    private static String normaliseName(String name) {
        return name == null ? "" : name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]", "");
    }

    
    private static String nameOfRow(List<Map<String, String>> rows, String eid) {
        for (Map<String, String> row : rows) {
            if (eid.equals(blank(row.get("Employee #")))) {
                return blank(blank(row.get("First Name")) + " " + blank(row.get("Last Name")));
            }
        }
        return eid;
    }

    
    private static String assignBranch(List<String> cities, List<String> notes, String who) {
        double lat = 0, lng = 0;
        int seen = 0;
        for (String city : cities) {
            double[] point = Locality.coordinates(city);
            if (point == null) continue;
            lat += point[0];
            lng += point[1];
            seen++;
        }
        if (seen == 0) {
            notes.add(who + "'s team has no recognisable city, so the branch falls back to Main.");
            return Branch.MAIN.label();
        }
        Branch nearest = Branch.MAIN;
        double best = Double.MAX_VALUE;
        for (Branch branch : Branch.fieldOffices()) {
            double distance = Locality.kilometres(new double[]{ lat / seen, lng / seen },
                new double[]{ branch.latitude(), branch.longitude() });
            if (distance < best) {
                best = distance;
                nearest = branch;
            }
        }
        return nearest.label();
    }

    public List<Employee> employees() { return new ArrayList<>(employees); }
    public List<String> notes() { return new ArrayList<>(notes); }

    
    public BigDecimal totalMonthlyBasic() {
        BigDecimal total = BigDecimal.ZERO;
        for (Employee employee : employees) total = total.add(employee.getMonthlyBasic());
        return total;
    }
    public LocalDate anyBirthday() { return employees.isEmpty() ? null : employees.get(0).getBirthday(); }
}
