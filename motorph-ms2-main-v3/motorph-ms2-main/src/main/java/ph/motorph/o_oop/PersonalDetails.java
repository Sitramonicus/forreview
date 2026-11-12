package ph.motorph.o_oop;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PersonalDetails {
    public static final class Record {
        
        @SerializedName("address") public final String pd_sAddress;
        @SerializedName("building") public final String pd_sBuilding;
        @SerializedName("street") public final String pd_sStreet;
        @SerializedName("city") public final String pd_sCity;
        @SerializedName("region") public final String pd_sRegion;
        @SerializedName("postalCode") public final String pd_sPostal;
        @SerializedName("contactNumber") public final String pd_sContact;
        @SerializedName("birthdate") public final String pd_sBirthdate;
        @SerializedName("updatedBy") public final String pd_sUpdatedBy;
        @SerializedName("updatedAt") public final String pd_sUpdatedAt;

        @SerializedName("firstName") public final String pd_sFirst;
        @SerializedName("surname") public final String pd_sSurname;
        @SerializedName("gender") public final String pd_sGender;
        @SerializedName("email") public final String pd_sEmail;

        Record(Names names, String gender, String email, String building, String street, String city, String region, String postalCode,
                String contactNumber, String birthdate, String updatedBy, String updatedAt) {
            this.pd_sFirst = names.firstName;
            this.pd_sSurname = names.surname;
            this.pd_sGender = gender;
            this.pd_sEmail = email;
            this.pd_sBuilding = building;
            this.pd_sStreet = street;
            this.pd_sCity = city;
            this.pd_sRegion = region;
            this.pd_sPostal = postalCode;
            this.pd_sContact = contactNumber;
            this.pd_sBirthdate = birthdate;
            this.pd_sUpdatedBy = updatedBy;
            this.pd_sUpdatedAt = updatedAt;
            StringBuilder line = new StringBuilder();
            for (String part : new String[] {building, street, city}) {
                if (part.isEmpty()) continue;
                if (line.length() > 0) line.append(", ");
                line.append(part);
            }
            this.pd_sAddress = line + ", " + region + " " + postalCode;
        }
    }

    public static final List<String> GENDERS = java.util.Arrays.asList("Female", "Male", "Prefer not to say");

    
    public static final class Names {
        final String firstName, surname;

        public Names(String firstName, String surname) {
            this.firstName = trim(firstName).replaceAll("\\s+", " ");
            this.surname = trim(surname).replaceAll("\\s+", " ");
        }

        public String full() { return firstName + " " + surname; }
    }

    
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
