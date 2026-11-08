package ph.motorph.o_oop;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Municipality-level coordinates for the places named in the employee file.
 *
 * Why this exists: the addresses in MotorPH's file are street-level but fictional, so geocoding them exactly is
 * neither possible nor responsible (and the brief forbids shipping staff addresses to an external geocoder).
 * What the file <em>does</em> support reliably is the <b>city or municipality</b> each person lives in — that is
 * the finest grain that is truthful. Team centroids are therefore computed from municipality coordinates, and
 * the branch assignment derived from them is documented as an approximation at city level.
 *
 * Coordinates are approximate municipality centres, good to a few kilometres. That is far more precision than a
 * nearest-branch decision between offices 10–15 km apart needs.
 */
public final class Locality {
    private Locality() {}

    /** Lower-case "city" → {latitude, longitude}. */
    private static final Map<String, double[]> PLACES = new LinkedHashMap<>();
    private static void put(String name, double lat, double lon) { PLACES.put(name.toLowerCase(Locale.ROOT), new double[]{lat, lon}); }

    static {
        // Metro Manila
        put("makati", 14.5547, 121.0244);
        put("quezon city", 14.6760, 121.0437);
        put("taguig", 14.5176, 121.0509);
        put("pasig", 14.5764, 121.0851);
        put("manila", 14.5995, 120.9842);
        put("caloocan", 14.6488, 120.9675);
        put("las piñas", 14.4450, 120.9930);
        put("las pinas", 14.4450, 120.9930);
        put("metro manila", 14.5995, 120.9842);
        // Cavite / Laguna / Bulacan / Rizal
        put("dasmarinas", 14.3294, 120.9367);
        put("dasmariñas", 14.3294, 120.9367);
        put("licab", 15.5400, 120.7200);
        put("malolos", 14.8443, 120.8114);
        put("pililla", 14.5033, 121.3050);
        put("maragondon", 14.2711, 120.7380);
        put("bustos", 14.9580, 120.9180);
        // Central Luzon
        put("mabalacat", 15.2200, 120.5600);
        put("palayan", 15.5400, 121.0800);
        // CALABARZON / Bicol
        put("luisiana", 14.1860, 121.5110);
        put("catanauan", 13.5920, 122.3220);
        put("lucena", 13.9370, 121.6170);
        put("camalaniugan", 18.2745, 121.6840);
        // Visayas
        put("iloilo city", 10.7202, 122.5621);
        put("roxas city", 11.5853, 122.7511);
        put("kabankalan", 9.9890, 122.8140);
        put("cebu", 10.3157, 123.8854);
        put("liloan", 10.3980, 123.9990);
        put("tapaz", 11.2620, 122.5450);
        // Mindanao
        put("gingoog", 8.8450, 125.1370);
        put("catigbian", 9.8030, 124.0200);
        put("digos", 6.7497, 125.3572);
        put("santo tomas", 7.5300, 125.6300);
        put("tabuk", 17.4136, 121.4410);
        put("kiamba", 5.9890, 124.6250);
        put("surallah", 6.3750, 124.7480);
        put("bislig", 8.2070, 126.3180);
        // Palawan / Mindoro / Marinduque / Romblon group
        put("ubay", 10.0560, 124.4730);
        put("compostela", 7.6670, 126.0860);
        put("bongabon", 15.6320, 121.1450);
        put("davao del norte", 7.4478, 125.8078);
        put("davao del sur", 6.7497, 125.3572);
        put("zamboanga del sur", 7.8250, 123.4370);
        put("nueva ecija", 15.5780, 120.9980);
    }

    /**
     * The city or municipality named in an address, or {@code null} when none is recognised.
     *
     * Order matters: the address format is "…, Poblacion, <City> <postal> <Province>", and several provinces
     * share a name with a city elsewhere (Quezon the province vs. Quezon City). The longest known place name that
     * appears in the address wins, which resolves "Quezon City" before the bare province word.
     */
    public static String cityOf(String address) {
        if (address == null) return null;
        String text = address.toLowerCase(Locale.ROOT);
        String best = null;
        for (String place : PLACES.keySet()) {
            if (text.contains(place) && (best == null || place.length() > best.length())) best = place;
        }
        return best;
    }

    public static double[] coordinates(String place) {
        return place == null ? null : PLACES.get(place.toLowerCase(Locale.ROOT));
    }

    /** Distance in kilometres between two coordinate pairs, good enough at this scale. */
    public static double kilometres(double[] a, double[] b) {
        if (a == null || b == null) return Double.MAX_VALUE;
        double dLat = Math.toRadians(b[0] - a[0]), dLon = Math.toRadians(b[1] - a[1]);
        double lat1 = Math.toRadians(a[0]), lat2 = Math.toRadians(b[0]);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
