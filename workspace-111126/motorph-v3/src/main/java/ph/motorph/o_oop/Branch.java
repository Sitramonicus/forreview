package ph.motorph.o_oop;

public enum Branch {
    MAIN("Main Branch", 14.5995, 120.9842),
    MAKATI("Makati", 14.5547, 121.0244),
    QUEZON_CITY("Quezon City", 14.6760, 121.0437),
    TAGUIG("Taguig", 14.5176, 121.0509),
    PASIG("Pasig", 14.5764, 121.0851);

    private final String br_sName;
    private final double br_dblLat, br_dblLng;

    Branch(String br_sName, double br_dblLat, double br_dblLng) {
        this.br_sName = br_sName;
        this.br_dblLat = br_dblLat;
        this.br_dblLng = br_dblLng;
    }

    public String label() { return br_sName; }
    public double latitude() { return br_dblLat; }
    public double longitude() { return br_dblLng; }

    public static Branch fromLabel(String value) {
        for (Branch br_iter : values()) if (br_iter.br_sName.equalsIgnoreCase(value == null ? "" : value.trim())) return br_iter;
        throw new IllegalArgumentException("Unknown branch: " + value);
    }

    public static Branch[] fieldOffices() {
        return new Branch[]{ MAKATI, QUEZON_CITY, TAGUIG, PASIG };
    }
}
