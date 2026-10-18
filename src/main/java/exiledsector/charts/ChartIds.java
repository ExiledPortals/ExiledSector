package exiledsector.charts;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;

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

    private ChartIds() {
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
