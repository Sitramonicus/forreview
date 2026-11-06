package ph.motorph.web;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Address, contact number and birthdate (design doc: the RBAC matrix's Address entity, and "name, birthdate, branch..."
 * at onboarding; database design: Address = building, street, city, region, postal code; Employee = contact number,
 * birthdate). HR and the System Admin create and update them; everyone else reads only their own (checked in AppServer).
 * Synthetic demo data only; never enter a real address or phone number.
 */
public final class PersonalDetails {
    public static final class Record {
        /** One-line address for display (older data files have only this). */
        public final String address, building, street, city, region, postalCode, contactNumber, birthdate, updatedBy, updatedAt;
        /** First name and surname kept apart (the directory shows them joined); gender; personal email. */
        public final String firstName, surname, gender, email;

        Record(Names names, String gender, String email, String building, String street, String city, String region, String postalCode,
                String contactNumber, String birthdate, String updatedBy, String updatedAt) {
            this.firstName = names.firstName;
            this.surname = names.surname;
            this.gender = gender;
            this.email = email;
            this.building = building;
            this.street = street;
            this.city = city;
            this.region = region;
            this.postalCode = postalCode;
            this.contactNumber = contactNumber;
            this.birthdate = birthdate;
            this.updatedBy = updatedBy;
            this.updatedAt = updatedAt;
            StringBuilder line = new StringBuilder();
            for (String part : new String[] {building, street, city}) {
                if (part.isEmpty()) continue;
                if (line.length() > 0) line.append(", ");
                line.append(part);
            }
            this.address = line + ", " + region + " " + postalCode;
        }
    }

    public static final List<String> GENDERS = java.util.Arrays.asList("Female", "Male", "Prefer not to say");

    /** First name and surname as typed; checked by set(). */
    public static final class Names {
        final String firstName, surname;

        public Names(String firstName, String surname) {
            this.firstName = trim(firstName).replaceAll("\\s+", " ");
            this.surname = trim(surname).replaceAll("\\s+", " ");
        }

        public String full() { return firstName + " " + surname; }
    }

    /** The parts of an address as typed; checked by set(). */
    public static final class Address {
        final String building, street, city, region, postalCode;

        public Address(String building, String street, String city, String region, String postalCode) {
            this.building = trim(building);
            this.street = trim(street);
            this.city = trim(city);
            this.region = trim(region);
            this.postalCode = trim(postalCode);
        }
    }

    private final Map<String, Record> records = new HashMap<>();

    public synchronized Optional<Record> find(String employeeId) { return Optional.ofNullable(records.get(employeeId)); }

    /**
     * Street, city and region are required (building optional, up to 100 characters); a 4-digit postal code; a Philippine
     * mobile number (09XXXXXXXXX or +639XXXXXXXXX); a birthdate that makes the person 15 to 100 years old today.
     */
    public synchronized Record set(String employeeId, Names names, String gender, String email, Address address, String contactNumber, String birthdate,
            LocalDate today, String by, String at) {
        for (String part : new String[] {names.firstName, names.surname}) {
            if (part.isEmpty() || part.length() > 60 || !part.matches("[\\p{L} .'-]+")) {
                throw new IllegalArgumentException("First name and surname are required (letters, spaces, . ' - ; up to 60 characters each).");
            }
        }
        if (!GENDERS.contains(trim(gender))) throw new IllegalArgumentException("Gender must be Female, Male or Prefer not to say.");
        String mail = trim(email).toLowerCase(java.util.Locale.ROOT);
        if (!mail.isEmpty() && (mail.length() > 120 || !mail.matches("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}"))) {
            throw new IllegalArgumentException("Personal email must look like name@example.com (or be left empty).");
        }
        if (address.building.length() > 100) throw new IllegalArgumentException("Building must be at most 100 characters.");
        if (address.street.isEmpty() || address.street.length() > 150) throw new IllegalArgumentException("Street is required (up to 150 characters).");
        if (address.city.isEmpty() || address.city.length() > 80) throw new IllegalArgumentException("City is required (up to 80 characters).");
        if (address.region.isEmpty() || address.region.length() > 80) throw new IllegalArgumentException("Region is required (up to 80 characters).");
        if (!address.postalCode.matches("[0-9]{4}")) throw new IllegalArgumentException("Postal code must be 4 digits.");
        String phone = trim(contactNumber).replaceAll("[\\s-]", "");
        if (!phone.matches("(09|\\+639)[0-9]{9}")) throw new IllegalArgumentException("Contact number must be a mobile number like 09171234567 or +639171234567.");
        LocalDate born;
        try {
            born = LocalDate.parse(trim(birthdate));
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Birthdate must be a date (YYYY-MM-DD).");
        }
        int age = Period.between(born, today).getYears();
        if (born.isAfter(today) || age < 15 || age > 100) throw new IllegalArgumentException("Birthdate must make the employee 15 to 100 years old.");
        Record record = new Record(names, trim(gender), mail, address.building, address.street, address.city, address.region, address.postalCode, phone,
            born.toString(), by, at);
        records.put(employeeId, record);
        return record;
    }

    public synchronized void remove(String employeeId) { records.remove(employeeId); }

    private static String trim(String value) { return value == null ? "" : value.trim(); }
}
