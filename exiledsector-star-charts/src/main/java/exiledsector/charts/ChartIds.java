package exiledsector.charts;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public final class ChartIds {

    public static final String PREFIX = "exiledSector_chart_";
    public static final String SYSTEM_TAG = PREFIX + "system";
    public static final String ENTITY_TAG = PREFIX + "entity";
    public static final String OUTER_WORMHOLE_TAG = PREFIX + "outer";
    public static final String INNER_WORMHOLE_TAG = PREFIX + "inner";
    public static final String RUN_TAG_PREFIX = PREFIX + "run_";
    public static final String WORMHOLE_TYPE = PREFIX + "wormhole";
    public static final String MEMORY_PREFIX = "$" + PREFIX;
    public static final String NEX_DO_NOT_COLONIZE = "$nex_do_not_colonize";
    public static final String HYPERSPACE_LOCATION_ID = "hyperspace";
    public static final String SYSTEM_BASE_NAME = "Charted Space";
    public static final String TIER_MEMORY_KEY = "$exiledSector_chartTier";
    public static final String SEED_MEMORY_KEY = "$exiledSector_chartSeed";
    public static final String STRENGTH_BONUS_MEMORY_KEY = "$exiledSector_chartStrengthBonus";
    public static final String BOSS_MEMORY_KEY = "$exiledSector_chartBoss";
    public static final String START_DP_MEMORY_KEY = "$exiledSector_chartStartDp";
    public static final String LOOT_DP_PAID_MEMORY_KEY = "$exiledSector_chartLootDpPaid";
    public static final String DEFENDED_SITE_MEMORY_KEY = "$exiledSector_chartDefendedSite";
    public static final String DEFENDER_LOOT_ADDED_MEMORY_KEY = "$exiledSector_chartDefenderLootAdded";
    public static final String VANILLA_DEFENDER_FLEET_MEMORY_KEY = "$defenderFleet";
    public static final String FLEET_LOOT_SITE = PREFIX + "fleet";
    public static final String BOSS_LOOT_SITE = PREFIX + "boss";
    public static final String CONTENT_TAG = PREFIX + "content";
    public static final String SENSOR_MOD_ID = PREFIX + "sensor_range";
    public static final String ENEMY_SENSOR_MOD_ID = PREFIX + "enemy_sensor_range";

    private ChartIds() {
    }

    public static String fleetSeed(MemoryAPI fleetMemory) {
        Object stored = fleetMemory == null ? null : fleetMemory.get(SEED_MEMORY_KEY);
        return stored instanceof String seed && !seed.isEmpty() ? seed : null;
    }

    public static String fleetRunKey(String runId) {
        return MEMORY_PREFIX + "run_" + runId;
    }

    public static String systemId(String runId) {
        return PREFIX + runId;
    }

    public static String runTag(String runId) {
        return RUN_TAG_PREFIX + runId;
    }

    public static String outerWormholeId(String runId) {
        return PREFIX + "outer_" + runId;
    }

    public static String innerWormholeId(String runId) {
        return PREFIX + "inner_" + runId;
    }

    public static String starId(String runId) {
        return PREFIX + "star_" + runId;
    }

    public static String runIdOf(StarSystemAPI system) {
        for (String tag : system.getTags()) {
            if (tag.startsWith(RUN_TAG_PREFIX)) {
                return tag.substring(RUN_TAG_PREFIX.length());
            }
        }
        String uniqueId = system.getOptionalUniqueId();
        return uniqueId != null && uniqueId.startsWith(PREFIX) ? uniqueId.substring(PREFIX.length()) : null;
    }

    public static boolean belongsToRun(SectorEntityToken entity, String runId) {
        return runId != null && entity.hasTag(runTag(runId));
    }

    public static String locationIdOf(LocationAPI location) {
        if (location == null || location.isHyperspace()) {
            return HYPERSPACE_LOCATION_ID;
        }
        if (location instanceof StarSystemAPI system && system.getOptionalUniqueId() != null) {
            return system.getOptionalUniqueId();
        }
        return location.getId();
    }

    public static LocationAPI resolveLocation(SectorAPI sector, String locationId) {
        if (locationId == null || HYPERSPACE_LOCATION_ID.equals(locationId)) {
            return sector.getHyperspace();
        }
        return sector.getStarSystem(locationId);
    }
}
