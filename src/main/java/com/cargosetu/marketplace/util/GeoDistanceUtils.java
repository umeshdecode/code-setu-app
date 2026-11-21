package com.cargosetu.marketplace.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Geographic coordinate validation, Haversine distance, point-to-corridor segment distance,
 * bounding-box calculation, and fallback city coordinate resolution for Indian logistics hubs.
 */
public final class GeoDistanceUtils {

    public static final double EARTH_RADIUS_KM = 6371.0;
    public static final double DEFAULT_CORRIDOR_RADIUS_KM = 200.0;
    public static final double MIN_RADIUS_KM = 1.0;
    public static final double MAX_RADIUS_KM = 2000.0;

    private static final Map<String, double[]> CITY_COORDINATES = new HashMap<>();

    static {
        // Maharashtra & Goa
        registerCity("mumbai", 19.0760, 72.8777, "maharashtra");
        registerCity("navi mumbai", 19.0330, 73.0297, "maharashtra");
        registerCity("thane", 19.2183, 72.9781, "maharashtra");
        registerCity("bhiwandi", 19.2813, 73.0483, "maharashtra");
        registerCity("pune", 18.5204, 73.8567, "maharashtra");
        registerCity("pimpri", 18.6298, 73.7997, "maharashtra");
        registerCity("nashik", 19.9975, 73.7898, "maharashtra");
        registerCity("nagpur", 21.1458, 79.0882, "maharashtra");
        registerCity("aurangabad", 19.8762, 75.3433, "maharashtra");
        registerCity("chhatrapati sambhajinagar", 19.8762, 75.3433, "maharashtra");
        registerCity("solapur", 17.6599, 75.9064, "maharashtra");
        registerCity("kolhapur", 16.7050, 74.2433, "maharashtra");
        registerCity("amravati", 20.9320, 77.7523, "maharashtra");
        registerCity("jalgaon", 21.0077, 75.5626, "maharashtra");
        registerCity("akola", 20.7002, 77.0082, "maharashtra");
        registerCity("latur", 18.4088, 76.5604, "maharashtra");
        registerCity("dhule", 20.9042, 74.7749, "maharashtra");
        registerCity("ahmednagar", 19.0948, 74.7480, "maharashtra");
        registerCity("satara", 17.6805, 74.0183, "maharashtra");
        registerCity("sangli", 16.8524, 74.5815, "maharashtra");
        registerCity("panaji", 15.4909, 73.8278, "goa");
        registerCity("vasco", 15.3982, 73.8113, "goa");

        // Gujarat
        registerCity("ahmedabad", 23.0225, 72.5714, "gujarat");
        registerCity("surat", 21.1702, 72.8311, "gujarat");
        registerCity("vadodara", 22.3072, 73.1812, "gujarat");
        registerCity("baroda", 22.3072, 73.1812, "gujarat");
        registerCity("rajkot", 22.3039, 70.8022, "gujarat");
        registerCity("bharuch", 21.7051, 72.9959, "gujarat");
        registerCity("ankleshwar", 21.6264, 73.0152, "gujarat");
        registerCity("vapi", 20.3893, 72.9106, "gujarat");
        registerCity("valsad", 20.5992, 72.9342, "gujarat");
        registerCity("gandhidham", 23.0753, 70.1337, "gujarat");
        registerCity("mundra", 22.8394, 69.7213, "gujarat");
        registerCity("kandla", 23.0333, 70.2167, "gujarat");
        registerCity("bhavnagar", 21.7645, 72.1519, "gujarat");
        registerCity("jamnagar", 22.4707, 70.0577, "gujarat");
        registerCity("gandhinagar", 23.2156, 72.6369, "gujarat");
        registerCity("morbi", 22.8173, 70.8377, "gujarat");
        registerCity("mehsana", 23.5880, 72.3693, "gujarat");

        // Delhi NCR, Haryana, Punjab, Himachal, J&K, Uttarakhand
        registerCity("delhi", 28.6139, 77.2090, "delhi");
        registerCity("new delhi", 28.6139, 77.2090, "delhi");
        registerCity("gurugram", 28.4595, 77.0266, "haryana");
        registerCity("gurgaon", 28.4595, 77.0266, "haryana");
        registerCity("faridabad", 28.4089, 77.3178, "haryana");
        registerCity("noida", 28.5355, 77.3910, "uttar pradesh");
        registerCity("greater noida", 28.4744, 77.5040, "uttar pradesh");
        registerCity("ghaziabad", 28.6692, 77.4538, "uttar pradesh");
        registerCity("sonipat", 28.9931, 77.0151, "haryana");
        registerCity("panipat", 29.3909, 76.9635, "haryana");
        registerCity("karnal", 29.6857, 76.9905, "haryana");
        registerCity("ambala", 30.3782, 76.7767, "haryana");
        registerCity("rohtak", 28.8955, 76.6066, "haryana");
        registerCity("hisar", 29.1492, 75.7217, "haryana");
        registerCity("rewari", 28.1970, 76.6191, "haryana");
        registerCity("chandigarh", 30.7333, 76.7794, "chandigarh");
        registerCity("ludhiana", 30.9010, 75.8573, "punjab");
        registerCity("amritsar", 31.6340, 74.8723, "punjab");
        registerCity("jalandhar", 31.3260, 75.5762, "punjab");
        registerCity("patiala", 30.3398, 76.3869, "punjab");
        registerCity("bathinda", 30.2110, 74.9455, "punjab");
        registerCity("baddi", 30.9578, 76.7914, "himachal pradesh");
        registerCity("shimla", 31.1048, 77.1734, "himachal pradesh");
        registerCity("dehradun", 30.3165, 78.0322, "uttarakhand");
        registerCity("haridwar", 29.9457, 78.1642, "uttarakhand");
        registerCity("rudrapur", 28.9845, 79.4004, "uttarakhand");
        registerCity("haldwani", 29.2183, 79.5130, "uttarakhand");
        registerCity("jammu", 32.7266, 74.8570, "jammu and kashmir");
        registerCity("srinagar", 34.0837, 74.7973, "jammu and kashmir");

        // Rajasthan
        registerCity("jaipur", 26.9124, 75.7873, "rajasthan");
        registerCity("udaipur", 24.5854, 73.7125, "rajasthan");
        registerCity("jodhpur", 26.2389, 73.0243, "rajasthan");
        registerCity("kota", 25.2138, 75.8648, "rajasthan");
        registerCity("ajmer", 26.4499, 74.6399, "rajasthan");
        registerCity("bhilwara", 25.3407, 74.6313, "rajasthan");
        registerCity("alwar", 27.5530, 76.6346, "rajasthan");
        registerCity("bhiwadi", 28.2088, 76.8446, "rajasthan");
        registerCity("bikaner", 28.0229, 73.3119, "rajasthan");
        registerCity("chittorgarh", 24.8887, 74.6269, "rajasthan");

        // Madhya Pradesh & Chhattisgarh
        registerCity("indore", 22.7196, 75.8577, "madhya pradesh");
        registerCity("bhopal", 23.2599, 77.4126, "madhya pradesh");
        registerCity("gwalior", 26.2183, 78.1828, "madhya pradesh");
        registerCity("jabalpur", 23.1815, 79.9864, "madhya pradesh");
        registerCity("ujjain", 23.1765, 75.7885, "madhya pradesh");
        registerCity("dewas", 22.9676, 76.0534, "madhya pradesh");
        registerCity("satna", 24.6005, 80.8322, "madhya pradesh");
        registerCity("sagar", 23.8388, 78.7378, "madhya pradesh");
        registerCity("ratlam", 23.3315, 75.0367, "madhya pradesh");
        registerCity("raipur", 21.2514, 81.6296, "chhattisgarh");
        registerCity("bhilai", 21.1938, 81.3509, "chhattisgarh");
        registerCity("bilaspur", 22.0797, 82.1409, "chhattisgarh");

        // Uttar Pradesh, Bihar, Jharkhand
        registerCity("lucknow", 26.8467, 80.9462, "uttar pradesh");
        registerCity("kanpur", 26.4499, 80.3319, "uttar pradesh");
        registerCity("agra", 27.1767, 78.0081, "uttar pradesh");
        registerCity("varanasi", 25.3176, 82.9739, "uttar pradesh");
        registerCity("prayagraj", 25.4358, 81.8463, "uttar pradesh");
        registerCity("allahabad", 25.4358, 81.8463, "uttar pradesh");
        registerCity("meerut", 28.9845, 77.7064, "uttar pradesh");
        registerCity("bareilly", 28.3670, 79.4304, "uttar pradesh");
        registerCity("aligarh", 27.8974, 78.0880, "uttar pradesh");
        registerCity("moradabad", 28.8386, 78.7733, "uttar pradesh");
        registerCity("gorakhpur", 26.7606, 83.3732, "uttar pradesh");
        registerCity("jhansi", 25.4484, 78.5685, "uttar pradesh");
        registerCity("mathura", 27.4924, 77.6737, "uttar pradesh");
        registerCity("patna", 25.5941, 85.1376, "bihar");
        registerCity("muzaffarpur", 26.1197, 85.3910, "bihar");
        registerCity("gaya", 24.7914, 85.0002, "bihar");
        registerCity("bhagalpur", 25.2425, 86.9842, "bihar");
        registerCity("ranchi", 23.3441, 85.3096, "jharkhand");
        registerCity("jamshedpur", 22.8046, 86.2029, "jharkhand");
        registerCity("dhanbad", 23.7957, 86.4304, "jharkhand");
        registerCity("bokaro", 23.6693, 86.1511, "jharkhand");

        // Karnataka, Tamil Nadu, Telangana, Andhra Pradesh, Kerala
        registerCity("bengaluru", 12.9716, 77.5946, "karnataka");
        registerCity("bangalore", 12.9716, 77.5946, "karnataka");
        registerCity("mysuru", 12.2958, 76.6394, "karnataka");
        registerCity("mysore", 12.2958, 76.6394, "karnataka");
        registerCity("hubballi", 15.3647, 75.1240, "karnataka");
        registerCity("hubli", 15.3647, 75.1240, "karnataka");
        registerCity("belagavi", 15.8497, 74.4977, "karnataka");
        registerCity("belgaum", 15.8497, 74.4977, "karnataka");
        registerCity("mangaluru", 12.9141, 74.8560, "karnataka");
        registerCity("mangalore", 12.9141, 74.8560, "karnataka");
        registerCity("tumakuru", 13.3379, 77.1173, "karnataka");
        registerCity("bellary", 15.1394, 76.9214, "karnataka");
        registerCity("chennai", 13.0827, 80.2707, "tamil nadu");
        registerCity("coimbatore", 11.0168, 76.9558, "tamil nadu");
        registerCity("madurai", 9.9252, 78.1198, "tamil nadu");
        registerCity("tiruchirappalli", 10.7905, 78.7047, "tamil nadu");
        registerCity("trichy", 10.7905, 78.7047, "tamil nadu");
        registerCity("salem", 11.6643, 78.1460, "tamil nadu");
        registerCity("tiruppur", 11.1085, 77.3411, "tamil nadu");
        registerCity("erode", 11.3410, 77.7172, "tamil nadu");
        registerCity("hosur", 12.7409, 77.8253, "tamil nadu");
        registerCity("vellore", 12.9165, 79.1325, "tamil nadu");
        registerCity("thoothukudi", 8.7642, 78.1348, "tamil nadu");
        registerCity("tuticorin", 8.7642, 78.1348, "tamil nadu");
        registerCity("hyderabad", 17.3850, 78.4867, "telangana");
        registerCity("secunderabad", 17.4399, 78.4983, "telangana");
        registerCity("warangal", 17.9689, 79.5941, "telangana");
        registerCity("karimnagar", 18.4386, 79.1288, "telangana");
        registerCity("nizamabad", 18.6725, 78.0941, "telangana");
        registerCity("visakhapatnam", 17.6868, 83.2185, "andhra pradesh");
        registerCity("vizag", 17.6868, 83.2185, "andhra pradesh");
        registerCity("vijayawada", 16.5062, 80.6480, "andhra pradesh");
        registerCity("guntur", 16.3067, 80.4365, "andhra pradesh");
        registerCity("nellore", 14.4426, 79.9865, "andhra pradesh");
        registerCity("tirupati", 13.6288, 79.4192, "andhra pradesh");
        registerCity("kurnool", 15.8281, 78.0373, "andhra pradesh");
        registerCity("kakinada", 16.9891, 82.2475, "andhra pradesh");
        registerCity("anantapur", 14.6819, 77.6006, "andhra pradesh");
        registerCity("kochi", 9.9312, 76.2673, "kerala");
        registerCity("cochin", 9.9312, 76.2673, "kerala");
        registerCity("thiruvananthapuram", 8.5241, 76.9366, "kerala");
        registerCity("trivandrum", 8.5241, 76.9366, "kerala");
        registerCity("kozhikode", 11.2588, 75.7804, "kerala");
        registerCity("calicut", 11.2588, 75.7804, "kerala");
        registerCity("thrissur", 10.5276, 76.2144, "kerala");

        // West Bengal, Odisha, Assam & North East
        registerCity("kolkata", 22.5726, 88.3639, "west bengal");
        registerCity("calcutta", 22.5726, 88.3639, "west bengal");
        registerCity("howrah", 22.5958, 88.2636, "west bengal");
        registerCity("durgapur", 23.5204, 87.3119, "west bengal");
        registerCity("asansol", 23.6739, 86.9524, "west bengal");
        registerCity("siliguri", 26.7271, 88.3953, "west bengal");
        registerCity("haldia", 22.0667, 88.0698, "west bengal");
        registerCity("kharagpur", 22.3460, 87.2320, "west bengal");
        registerCity("bhubaneswar", 20.2961, 85.8245, "odisha");
        registerCity("cuttack", 20.4625, 85.8830, "odisha");
        registerCity("rourkela", 22.2604, 84.8536, "odisha");
        registerCity("sambalpur", 21.4669, 83.9812, "odisha");
        registerCity("paradip", 20.3166, 86.6114, "odisha");
        registerCity("guwahati", 26.1445, 91.7362, "assam");
        registerCity("silchar", 24.8333, 92.7789, "assam");
        registerCity("dibrugarh", 27.4728, 94.9120, "assam");
    }

    private GeoDistanceUtils() {
    }

    private static void registerCity(String name, double lat, double lng, String state) {
        CITY_COORDINATES.put(name.toLowerCase(Locale.ROOT), new double[]{lat, lng});
    }

    public static boolean isValidCoordinate(Double lat, Double lng) {
        if (lat == null || lng == null) return false;
        if (Double.isNaN(lat) || Double.isInfinite(lat) || Double.isNaN(lng) || Double.isInfinite(lng)) {
            return false;
        }
        return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0
                && !(Math.abs(lat) < 0.0001 && Math.abs(lng) < 0.0001);
    }

    /**
     * Resolves coordinates using explicit (lat, lng) if valid, or falls back to the built-in city lookup.
     *
     * @return double[]{lat, lng} or null if neither is available.
     */
    public static double[] resolveCoordinates(Double lat, Double lng, String cityText) {
        if (isValidCoordinate(lat, lng)) {
            return new double[]{lat, lng};
        }
        if (cityText == null || cityText.isBlank()) {
            return null;
        }
        String normalized = cityText.trim().toLowerCase(Locale.ROOT);
        // Strip state or country suffix if comma-separated (e.g. "Mumbai, Maharashtra")
        int commaIdx = normalized.indexOf(',');
        String primaryCity = commaIdx > 0 ? normalized.substring(0, commaIdx).trim() : normalized;

        double[] exact = CITY_COORDINATES.get(primaryCity);
        if (exact != null) {
            return new double[]{exact[0], exact[1]};
        }
        exact = CITY_COORDINATES.get(normalized);
        if (exact != null) {
            return new double[]{exact[0], exact[1]};
        }
        for (Map.Entry<String, double[]> entry : CITY_COORDINATES.entrySet()) {
            if (primaryCity.contains(entry.getKey()) || entry.getKey().contains(primaryCity)) {
                return new double[]{entry.getValue()[0], entry.getValue()[1]};
            }
        }
        return null;
    }

    public static double clampRadiusKm(Double radiusKm) {
        if (radiusKm == null || Double.isNaN(radiusKm) || radiusKm <= 0) {
            return DEFAULT_CORRIDOR_RADIUS_KM;
        }
        return Math.min(Math.max(radiusKm, MIN_RADIUS_KM), MAX_RADIUS_KM);
    }

    /**
     * Calculates Haversine great-circle distance between two points in kilometres, rounded to 1 decimal place.
     */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));
        double rawKm = EARTH_RADIUS_KM * c;
        return Math.round(rawKm * 10.0) / 10.0;
    }

    /**
     * Calculates minimum distance in kilometres from point P(pLat, pLng) to the route segment A(aLat, aLng) -> B(bLat, bLng).
     */
    public static double distanceToSegmentKm(
            double pLat, double pLng,
            double aLat, double aLng,
            double bLat, double bLng) {
        double meanLatRad = Math.toRadians((aLat + bLat + pLat) / 3.0);
        double cosLat = Math.max(0.1, Math.cos(meanLatRad));

        double ax = 0.0;
        double ay = 0.0;
        double bx = (bLng - aLng) * cosLat;
        double by = bLat - aLat;
        double px = (pLng - aLng) * cosLat;
        double py = pLat - aLat;

        double dx = bx - ax;
        double dy = by - ay;
        double lenSq = dx * dx + dy * dy;

        double t;
        if (lenSq < 1e-10) {
            t = 0.0;
        } else {
            t = ((px - ax) * dx + (py - ay) * dy) / lenSq;
            t = Math.max(0.0, Math.min(1.0, t));
        }

        double closestLat = aLat + t * (bLat - aLat);
        double closestLng = aLng + t * (bLng - aLng);
        return haversineKm(pLat, pLng, closestLat, closestLng);
    }

    /**
     * Checks whether a truck's planned route is directionally compatible with the search route A -> B.
     * If the truck has no fixed destination (rtCoords == null), it is compatible with any corridor it is near.
     * If the truck has a destination (rtCoords), it is compatible if:
     * 1) Its movement vector has a non-opposing dot product with the search vector A -> B (cos(theta) >= -0.20), OR
     * 2) Its destination is within radiusKm of the search origin A (meaning the truck finishes its trip near origin A and can pick up the load), OR
     * 3) Its destination is within radiusKm of the search destination B.
     */
    public static boolean isDirectionallyCompatible(
            double[] truckStartCoords,
            double[] truckDestCoords,
            double aLat, double aLng,
            double bLat, double bLng,
            double radiusKm) {
        if (truckDestCoords == null) {
            return true;
        }
        double distDestToSearchDest = haversineKm(truckDestCoords[0], truckDestCoords[1], bLat, bLng);
        if (distDestToSearchDest <= radiusKm) {
            return true;
        }
        double distDestToSearchOrigin = haversineKm(truckDestCoords[0], truckDestCoords[1], aLat, aLng);
        if (distDestToSearchOrigin <= radiusKm) {
            return true;
        }
        if (truckStartCoords == null) {
            double distDestToCorridor = distanceToSegmentKm(truckDestCoords[0], truckDestCoords[1], aLat, aLng, bLat, bLng);
            return distDestToCorridor <= radiusKm;
        }

        double meanLatRad = Math.toRadians((aLat + bLat) / 2.0);
        double cosLat = Math.max(0.1, Math.cos(meanLatRad));

        double rx = (bLng - aLng) * cosLat;
        double ry = bLat - aLat;
        double tx = (truckDestCoords[1] - truckStartCoords[1]) * cosLat;
        double ty = truckDestCoords[0] - truckStartCoords[0];

        double rNorm = Math.hypot(rx, ry);
        double tNorm = Math.hypot(tx, ty);
        if (rNorm < 1e-6 || tNorm < 1e-6) {
            return true;
        }
        double cosTheta = (rx * tx + ry * ty) / (rNorm * tNorm);
        // Must be generally aligned with route direction (>= ~75 degrees) AND destination within corridor
        double distDestToCorridor = distanceToSegmentKm(truckDestCoords[0], truckDestCoords[1], aLat, aLng, bLat, bLng);
        return cosTheta >= 0.25 && distDestToCorridor <= radiusKm;
    }

    public static BoundingBox computeBoundingBox(double lat1, double lng1, double lat2, double lng2, double radiusKm) {
        double safeRadius = clampRadiusKm(radiusKm);
        double deltaLat = safeRadius / 110.574;
        double meanLatRad = Math.toRadians((lat1 + lat2) / 2.0);
        double deltaLng = safeRadius / (111.320 * Math.max(0.1, Math.cos(meanLatRad)));

        double minLat = Math.max(-90.0, Math.min(lat1, lat2) - deltaLat);
        double maxLat = Math.min(90.0, Math.max(lat1, lat2) + deltaLat);
        double minLng = Math.max(-180.0, Math.min(lng1, lng2) - deltaLng);
        double maxLng = Math.min(180.0, Math.max(lng1, lng2) + deltaLng);

        return new BoundingBox(minLat, maxLat, minLng, maxLng);
    }

    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
        public boolean contains(double lat, double lng) {
            return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
        }
    }
}
