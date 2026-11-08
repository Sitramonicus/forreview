package ph.motorph.o_oop;

/**
 * A MotorPH operating location. Each branch carries the coordinates its team-centroid assignment is measured
 * against (see {@code docs/a_doc/branch-assignment.md}).
 *
 * The four locations are the ones the rest of the application and its tests already use — Makati, Quezon City,
 * Taguig and Pasig — plus {@link #MAIN}, which is the head office for the executives and for supervisors whose
 * roster spans more than one team.
 */
public enum Branch {
    MAIN("Main Branch", 14.5995, 120.9842),   // Manila head office
    MAKATI("Makati", 14.5547, 121.0244),
    QUEZON_CITY("Quezon City", 14.6760, 121.0437),
    TAGUIG("Taguig", 14.5176, 121.0509),
    PASIG("Pasig", 14.5764, 121.0851);

    private final String label;
    private final double latitude, longitude;

    Branch(String label, double latitude, double longitude) {
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String label() { return label; }
    public double latitude() { return latitude; }
    public double longitude() { return longitude; }

    public static Branch fromLabel(String value) {
        for (Branch branch : values()) if (branch.label.equalsIgnoreCase(value == null ? "" : value.trim())) return branch;
        throw new IllegalArgumentException("Unknown branch: " + value);
    }

    /** Every branch except Main, for the teams that get a geographic assignment. */
    public static Branch[] fieldOffices() {
        return new Branch[]{ MAKATI, QUEZON_CITY, TAGUIG, PASIG };
    }
}
