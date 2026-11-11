package ph.motorph.o_oop;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class Locality {
    private Locality() {}

    
    private static final Map<String, double[]> lc_mPlaces = new LinkedHashMap<>();
    private static void put(String lc_sName, double lc_dblLat, double lc_dblLon) {
        lc_mPlaces.put(lc_sName.toLowerCase(Locale.ROOT), new double[]{lc_dblLat, lc_dblLon});
    }

    static {
        
        put("makati", 14.5547, 121.0244);
        put("quezon city", 14.6760, 121.0437);
        put("taguig", 14.5176, 121.0509);
        put("pasig", 14.5764, 121.0851);
        put("manila", 14.5995, 120.9842);
        put("caloocan", 14.6488, 120.9675);
        put("las piñas", 14.4450, 120.9930);
        put("las pinas", 14.4450, 120.9930);
        put("metro manila", 14.5995, 120.9842);
        
        put("dasmarinas", 14.3294, 120.9367);
        put("dasmariñas", 14.3294, 120.9367);
        put("licab", 15.5400, 120.7200);
        put("malolos", 14.8443, 120.8114);
        put("pililla", 14.5033, 121.3050);
        put("maragondon", 14.2711, 120.7380);
        put("bustos", 14.9580, 120.9180);
        
        put("mabalacat", 15.2200, 120.5600);
        put("palayan", 15.5400, 121.0800);
        
        put("luisiana", 14.1860, 121.5110);
        put("catanauan", 13.5920, 122.3220);
        put("lucena", 13.9370, 121.6170);
        put("camalaniugan", 18.2745, 121.6840);
        
        put("iloilo city", 10.7202, 122.5621);
        put("roxas city", 11.5853, 122.7511);
        put("kabankalan", 9.9890, 122.8140);
        put("cebu", 10.3157, 123.8854);
        put("liloan", 10.3980, 123.9990);
        put("tapaz", 11.2620, 122.5450);
        
        put("gingoog", 8.8450, 125.1370);
        put("catigbian", 9.8030, 124.0200);
        put("digos", 6.7497, 125.3572);
        put("santo tomas", 7.5300, 125.6300);
        put("tabuk", 17.4136, 121.4410);
        put("kiamba", 5.9890, 124.6250);
        put("surallah", 6.3750, 124.7480);
        put("bislig", 8.2070, 126.3180);
        
        put("ubay", 10.0560, 124.4730);
        put("compostela", 7.6670, 126.0860);
        put("bongabon", 15.6320, 121.1450);
        put("davao del norte", 7.4478, 125.8078);
        put("davao del sur", 6.7497, 125.3572);
        put("zamboanga del sur", 7.8250, 123.4370);
        put("nueva ecija", 15.5780, 120.9980);
    }

    
    public static String cityOf(String address) {
        if (address == null) return null;
        String lc_sText = address.toLowerCase(Locale.ROOT);
        String lc_sBest = null;
        for (String lc_sPlace : lc_mPlaces.keySet()) {
            if (lc_sText.contains(lc_sPlace) && (lc_sBest == null || lc_sPlace.length() > lc_sBest.length())) lc_sBest = lc_sPlace;
        }
        return lc_sBest;
    }

    public static double[] coordinates(String place) {
        return place == null ? null : lc_mPlaces.get(place.toLowerCase(Locale.ROOT));
    }

    
    public static double kilometres(double[] lc_apFrom, double[] lc_apTo) {
        if (lc_apFrom == null || lc_apTo == null) return Double.MAX_VALUE;
        double lc_dblDLat = Math.toRadians(lc_apTo[0] - lc_apFrom[0]), lc_dblDLon = Math.toRadians(lc_apTo[1] - lc_apFrom[1]);
        double lc_dblLat1 = Math.toRadians(lc_apFrom[0]), lc_dblLat2 = Math.toRadians(lc_apTo[0]);
        double lc_dblH = Math.sin(lc_dblDLat / 2) * Math.sin(lc_dblDLat / 2)
                 + Math.cos(lc_dblLat1) * Math.cos(lc_dblLat2) * Math.sin(lc_dblDLon / 2) * Math.sin(lc_dblDLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(lc_dblH), Math.sqrt(1 - lc_dblH));
    }
}
