package exiledsector.charts.anomaly.station;

final class StationRules {

    static final String NEXUS_DAMAGED_VARIANT = "remnant_station2_Damaged";
    static final String NEXUS_VARIANT = "remnant_station2_Standard";
    static final String MOTHERSHIP_VARIANT = "station_derelict_survey_mothership_Standard";
    static final int LAST_DAMAGED_NEXUS_TIER = 7;
    static final float THRESHOLD_LY = 1f;
    static final int MIN_FLEETS = 0;
    static final int DAMAGED_NEXUS_MAX_FLEETS = 2;
    static final int NEXUS_MAX_FLEETS = 3;
    static final int MOTHERSHIP_MAX_FLEETS = 2;
    static final float DAMAGED_NEXUS_RESPAWN_DAYS = 25f;
    static final float NEXUS_RESPAWN_DAYS = 15f;
    static final float MOTHERSHIP_RESPAWN_DAYS = 20f;
    static final int VANILLA_MIN_POINTS = 6;
    static final int VANILLA_MAX_POINTS = 12;
    static final float BASE_FLEET_SHARE = 0.35f;
    static final float FLEET_SHARE_PER_LOSS = 0.05f;
    static final float MAX_FLEET_SHARE = 0.6f;
    static final int NO_SHIP_SIZE_CAP = 0;
    static final int DRONE_MAX_SHIP_SIZE = 3;
    static final float ORBIT_MIN = 3000f;
    static final float ORBIT_BAND_WIDTH = 2000f;
    static final int ORBIT_BANDS = 4;
    static final float MIN_ENTRY_GAP = 4000f;
    static final float STATION_SPIN = 5f;

    private StationRules() {
    }

    static float fleetShare(int fleetsLost) {
        return Math.min(MAX_FLEET_SHARE, BASE_FLEET_SHARE + FLEET_SHARE_PER_LOSS * Math.max(0, fleetsLost));
    }

    static boolean damagedNexus(int tier) {
        return tier <= LAST_DAMAGED_NEXUS_TIER;
    }
}
