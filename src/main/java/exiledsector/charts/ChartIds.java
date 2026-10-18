package exiledsector.charts;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public final class ChartIds {

    public static final String PREFIX = "exiledSector_chart_";
    public static final String POCKET_TAG = PREFIX + "pocket";
    public static final String ENTITY_TAG = PREFIX + "entity";
    public static final String OUTER_WORMHOLE_TAG = PREFIX + "outer";
    public static final String INNER_WORMHOLE_TAG = PREFIX + "inner";
    public static final String RUN_TAG_PREFIX = PREFIX + "run_";
    public static final String WORMHOLE_TYPE = PREFIX + "wormhole";
    public static final String MEMORY_PREFIX = "$" + PREFIX;
    public static final String NEX_DO_NOT_COLONIZE = "$nex_do_not_colonize";
    public static final String HYPERSPACE_LOCATION_ID = "hyperspace";
    public static final String POCKET_BASE_NAME = "Charted Space";
    public static final String TIER_MEMORY_KEY = "$exiledSector_chartTier";
    public static final String SEED_MEMORY_KEY = "$exiledSector_chartSeed";
    public static final String VAULT_TAG = PREFIX + "vault";
    public static final String CONTENT_TAG = PREFIX + "content";
    public static final String ITEM_CHANCE_MEMORY_KEY = MEMORY_PREFIX + "itemChanceMult";
    public static final String SENSOR_MOD_ID = PREFIX + "sensor_range";

    private ChartIds() {
    }

    public static String fleetSeed(MemoryAPI fleetMemory) {
        Object stored = fleetMemory == null ? null : fleetMemory.get(SEED_MEMORY_KEY);
        return stored instanceof String seed && !seed.isEmpty() ? seed : null;
    }

    public static String fleetRunKey(String runId) {
        return MEMORY_PREFIX + "run_" + runId;
    }

    public static String pocketSystemId(String runId) {
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

    public static String runIdOf(StarSystemAPI pocket) {
        for (String tag : pocket.getTags()) {
            if (tag.startsWith(RUN_TAG_PREFIX)) {
                return tag.substring(RUN_TAG_PREFIX.length());
            }
        }
        String uniqueId = pocket.getOptionalUniqueId();
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
