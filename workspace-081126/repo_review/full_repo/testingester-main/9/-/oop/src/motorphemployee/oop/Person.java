package motorphemployee.oop;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Person - root of the employee hierarchy (MS1 Week 4 CRC row: "Person (Base Class)").
 *
 * Holds only the personal data: firstName, lastName, birthday, address, phoneNumber.
 * Employee IS-A Person. Everything below inherits these fields and methods once,
 * so the 19-field Employee does not duplicate what a person already owns.
 */
public class Person {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter US = DateTimeFormatter.ofPattern("M/d/yyyy");

    private String firstName;
    private String lastName;
    private String birthday;
    private String address;
    private String phoneNumber;

    public Person(String firstName, String lastName, String birthday, String address, String phoneNumber) {
        this.firstName = requireText(firstName, "firstName");
        this.lastName = requireText(lastName, "lastName");
        this.birthday = requireBirthday(birthday);
        this.address = requireText(address, "address");
        this.phoneNumber = requirePhone(phoneNumber);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = requireText(firstName, "firstName");
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = requireText(lastName, "lastName");
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = requireBirthday(birthday);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = requireText(address, "address");
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = requirePhone(phoneNumber);
    }

    /** Returns "lastName, firstName" - the single format used across the whole system. */
    public String getFullName() {
        return lastName + ", " + firstName;
    }

    public int getAge() {
        LocalDate born = parseDate(birthday);
        return Period.between(born, LocalDate.now()).getYears();
    }

    public void updateAddress(String address) {
        setAddress(address);
    }

    public void updatePhoneNumber(String phoneNumber) {
        setPhoneNumber(phoneNumber);
    }

    @Override
    public String toString() {
        return getFullName() + " (" + birthday + ")";
    }

    // ---- validation helpers: one rule, used by both the constructor and the setters ----

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be null or empty");
        }
        return value.trim();
    }

    private static String requireBirthday(String value) {
        String text = requireText(value, "birthday");
        if (parseDate(text).isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("birthday cannot be in the future: " + value);
        }
        return text;
    }

    private static String requirePhone(String value) {
        String text = requireText(value, "phoneNumber");
        if (!text.matches("[0-9+()\\-\\s]{7,20}")) {
            throw new IllegalArgumentException("phoneNumber has an invalid format: " + value);
        }
        return text;
    }

    private static LocalDate parseDate(String value) {
        String text = requireText(value, "birthday");
        try {
            return LocalDate.parse(text, ISO);
        } catch (DateTimeParseException first) {
            try {
                return LocalDate.parse(text, US);
            } catch (DateTimeParseException second) {
                throw new IllegalArgumentException("birthday must be yyyy-MM-dd or M/d/yyyy: " + value);
            }
        }
    }
}
