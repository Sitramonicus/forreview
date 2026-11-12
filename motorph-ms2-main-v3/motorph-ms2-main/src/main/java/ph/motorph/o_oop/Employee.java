package ph.motorph.o_oop;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class Employee {
    
    @SerializedName("eid") private final String emp_sEid;
    @SerializedName("firstName") private final String emp_sFirst;
    @SerializedName("lastName") private final String emp_sLast;
    @SerializedName("employeeStatus") private final String emp_sStatus;
    @SerializedName("position") private final String emp_sPosition;
    @SerializedName("department") private final String emp_sDept;
    @SerializedName("birthday") private final LocalDate emp_dBirthday;

    @SerializedName("phone") private final String emp_sPhone;
    @SerializedName("street") private final String emp_sStreet;
    @SerializedName("city") private final String emp_sCity;
    @SerializedName("region") private final String emp_sRegion;
    @SerializedName("postalCode") private final String emp_sPostal;
    @SerializedName("sssNumber") private final String emp_sSss;
    @SerializedName("philhealthNumber") private final String emp_sPhilhealth;
    @SerializedName("tinNumber") private final String emp_sTin;
    @SerializedName("pagibigNumber") private final String emp_sPagibig;

    @SerializedName("monthlyBasic") private final BigDecimal emp_bdBasic;
    @SerializedName("riceSubsidy") private final BigDecimal emp_bdRice;
    @SerializedName("phoneAllowance") private final BigDecimal emp_bdPhoneAllow;
    @SerializedName("clothingAllowance") private final BigDecimal emp_bdClothingAllow;
    @SerializedName("semiMonthlyRate") private final BigDecimal emp_bdSemiRate;
    @SerializedName("hourlyRate") private final BigDecimal emp_bdHourlyRate;

    @SerializedName("supervisorEid") private final String emp_sSupervisor;
    @SerializedName("branchName") private final String emp_sBranch;

    private Employee(Builder b) {
        this.emp_sEid = require(b.eid, "employee number");
        this.emp_sFirst = require(b.firstName, "first name");
        this.emp_sLast = require(b.lastName, "last name");
        this.emp_dBirthday = b.birthday == null ? LocalDate.of(1970, 1, 1) : b.birthday;
        this.emp_sStatus = require(b.employeeStatus, "employment status");
        this.emp_sPosition = require(b.position, "position");
        this.emp_sDept = require(b.department, "department");
        this.emp_sPhone = blankToEmpty(b.phone);
        this.emp_sStreet = blankToEmpty(b.street);
        this.emp_sCity = blankToEmpty(b.city);
        this.emp_sRegion = blankToEmpty(b.region);
        this.emp_sPostal = blankToEmpty(b.postalCode);
        this.emp_sSss = blankToEmpty(b.sssNumber);
        this.emp_sPhilhealth = blankToEmpty(b.philhealthNumber);
        this.emp_sTin = blankToEmpty(b.tinNumber);
        this.emp_sPagibig = blankToEmpty(b.pagibigNumber);
        this.emp_bdBasic = money(b.monthlyBasic, "monthly basic rate");
        this.emp_bdRice = money(b.riceSubsidy, "rice subsidy");
        this.emp_bdPhoneAllow = money(b.phoneAllowance, "phone allowance");
        this.emp_bdClothingAllow = money(b.clothingAllowance, "clothing allowance");
        this.emp_bdSemiRate = money(b.semiMonthlyRate, "semi-monthly rate");
        this.emp_bdHourlyRate = money(b.hourlyRate, "hourly rate");
        this.emp_sSupervisor = blankToEmpty(b.supervisorEid);
        this.emp_sBranch = require(b.branchName, "branch");
    }

    private static String require(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String text = value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return text;
    }
    private static String blankToEmpty(String value) { return value == null ? "" : value.trim(); }
    private static BigDecimal money(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.signum() < 0) throw new IllegalArgumentException(field + " must not be negative");
        return value;
    }

    
    public String getEid() { return emp_sEid; }
    public String getFirstName() { return emp_sFirst; }
    public String getLastName() { return emp_sLast; }
    
    public String getFullName() { return emp_sLast + ", " + emp_sFirst; }
    
    public String getDisplayName() { return emp_sFirst + " " + emp_sLast; }
    public LocalDate getBirthday() { return emp_dBirthday; }
    public String getEmployeeStatus() { return emp_sStatus; }
    public String getPosition() { return emp_sPosition; }
    public String getDepartment() { return emp_sDept; }
    public String getPhone() { return emp_sPhone; }
    public String getStreet() { return emp_sStreet; }
    public String getCity() { return emp_sCity; }
    public String getRegion() { return emp_sRegion; }
    public String getPostalCode() { return emp_sPostal; }
    public String getSssNumber() { return emp_sSss; }
    public String getPhilhealthNumber() { return emp_sPhilhealth; }
    public String getTinNumber() { return emp_sTin; }
    public String getPagibigNumber() { return emp_sPagibig; }
    public BigDecimal getMonthlyBasic() { return emp_bdBasic; }
    public BigDecimal getRiceSubsidy() { return emp_bdRice; }
    public BigDecimal getPhoneAllowance() { return emp_bdPhoneAllow; }
    public BigDecimal getClothingAllowance() { return emp_bdClothingAllow; }
    public BigDecimal getSemiMonthlyRate() { return emp_bdSemiRate; }
    public BigDecimal getHourlyRate() { return emp_bdHourlyRate; }
    public String getSupervisorEid() { return emp_sSupervisor; }
    public boolean hasSupervisor() { return !emp_sSupervisor.isEmpty(); }
    public String getBranchName() { return emp_sBranch; }

    
    public BigDecimal getTotalAllowances() { return emp_bdRice.add(emp_bdPhoneAllow).add(emp_bdClothingAllow); }

    
    
    

    
    public String getId() { return emp_sEid; }
    
    public String getName() { return getDisplayName(); }
    public String getRole() { return emp_sPosition; }
    
    public String getLocation() { return emp_sBranch; }
    
    public String getStatus() { return emp_sStatus; }

    
    public static Employee hired(String id, String name, String department, String role, String location, String status) {
        String[] parts = name.trim().isEmpty() ? new String[]{""} : name.trim().split(" +");
        String lastName = parts.length > 1 ? parts[parts.length - 1] : "";
        String firstName = parts.length > 1 ? name.substring(0, name.lastIndexOf(lastName)).trim() : name;
        return new Builder().eid(id).firstName(firstName == null || firstName.isEmpty() ? name : firstName)
            .lastName(lastName.isEmpty() ? "-" : lastName).employeeStatus(status).position(role)
            .department(department).branchName(location)
            .monthlyBasic(BigDecimal.ZERO).riceSubsidy(BigDecimal.ZERO).phoneAllowance(BigDecimal.ZERO)
            .clothingAllowance(BigDecimal.ZERO).semiMonthlyRate(BigDecimal.ZERO).hourlyRate(BigDecimal.ZERO)
            .build();
    }

    @Override public String toString() { return emp_sEid + " " + getFullName(); }

    
    public static final class Builder {
        private String eid, firstName, lastName, employeeStatus, position, department;
        private LocalDate birthday;
        private String phone, street, city, region, postalCode;
        private String sssNumber, philhealthNumber, tinNumber, pagibigNumber;
        private BigDecimal monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance, semiMonthlyRate, hourlyRate;
        private String supervisorEid, branchName;

        public Builder eid(String v) { eid = v; return this; }
        public Builder firstName(String v) { firstName = v; return this; }
        public Builder lastName(String v) { lastName = v; return this; }
        public Builder birthday(LocalDate v) { birthday = v; return this; }
        public Builder employeeStatus(String v) { employeeStatus = v; return this; }
        public Builder position(String v) { position = v; return this; }
        public Builder department(String v) { department = v; return this; }
        public Builder phone(String v) { phone = v; return this; }
        public Builder street(String v) { street = v; return this; }
        public Builder city(String v) { city = v; return this; }
        public Builder region(String v) { region = v; return this; }
        public Builder postalCode(String v) { postalCode = v; return this; }
        public Builder sssNumber(String v) { sssNumber = v; return this; }
        public Builder philhealthNumber(String v) { philhealthNumber = v; return this; }
        public Builder tinNumber(String v) { tinNumber = v; return this; }
        public Builder pagibigNumber(String v) { pagibigNumber = v; return this; }
        public Builder monthlyBasic(BigDecimal v) { monthlyBasic = v; return this; }
        public Builder riceSubsidy(BigDecimal v) { riceSubsidy = v; return this; }
        public Builder phoneAllowance(BigDecimal v) { phoneAllowance = v; return this; }
        public Builder clothingAllowance(BigDecimal v) { clothingAllowance = v; return this; }
        public Builder semiMonthlyRate(BigDecimal v) { semiMonthlyRate = v; return this; }
        public Builder hourlyRate(BigDecimal v) { hourlyRate = v; return this; }
        public Builder supervisorEid(String v) { supervisorEid = v; return this; }
        public Builder branchName(String v) { branchName = v; return this; }
        public Employee build() { return new Employee(this); }
    }

    public Builder toBuilder() {
        return new Builder().eid(emp_sEid).firstName(emp_sFirst).lastName(emp_sLast).birthday(emp_dBirthday)
            .employeeStatus(emp_sStatus).position(emp_sPosition).department(emp_sDept)
            .phone(emp_sPhone).street(emp_sStreet).city(emp_sCity).region(emp_sRegion).postalCode(emp_sPostal)
            .sssNumber(emp_sSss).philhealthNumber(emp_sPhilhealth).tinNumber(emp_sTin).pagibigNumber(emp_sPagibig)
            .monthlyBasic(emp_bdBasic).riceSubsidy(emp_bdRice).phoneAllowance(emp_bdPhoneAllow)
            .clothingAllowance(emp_bdClothingAllow).semiMonthlyRate(emp_bdSemiRate).hourlyRate(emp_bdHourlyRate)
            .supervisorEid(emp_sSupervisor).branchName(emp_sBranch);
    }
}
