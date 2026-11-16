package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.loot.ChartFleetLedger;
import exiledsector.i18n.Translation;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

final class BreachIncursion {

    static final String STATION_TAG = ChartIds.PREFIX + "breach_incursion_station";
    static final String GUARD_TAG = ChartIds.PREFIX + "breach_incursion_guard";
    static final long STATION_SALT = 0x4252494e43535441L;
    static final long GUARD_SALT = 0x4252494e43475244L;

    static final int NONE = 0;
    static final int WAITING = 1;
    static final int GUARDED = 2;
    static final int CLEARED = 3;
    static final int GONE = 4;

    record Spawn(String runId, int tier, ChartGenContext context, long seed, Vector2f stationAt) {
    }

    private BreachIncursion() {
    }

    static int update(int stage, StarSystemAPI system, Spawn spawn, Vector2f centre, float radius, boolean collapsing) {
        if (stage == WAITING) {
            return spawnAt(system, spawn);
        }
        return track(stage, system, centre, radius, collapsing);
    }

    static int track(int stage, LocationAPI location, Vector2f centre, float radius, boolean collapsing) {
        if (stage != GUARDED) {
            return stage;
        }
        CampaignFleetAPI guard = guardIn(location);
        if (guard == null || guard.isEmpty()) {
            unlockStation(location);
            return CLEARED;
        }
        if (collapsing && guard.getLocation() != null && !BreachRules.inside(Misc.getDistance(centre, guard.getLocation()), radius)) {
            vanish(location);
            return GONE;
        }
        return stage;
    }

    static int collapse(int stage, LocationAPI location) {
        if (stage == WAITING) {
            return GONE;
        }
        if (stage != GUARDED) {
            return stage;
        }
        vanish(location);
        return GONE;
    }

    private static int spawnAt(StarSystemAPI system, Spawn spawn) {
        float stationX = spawn.stationAt().x;
        float stationY = spawn.stationAt().y;
        Random random = new Random(spawn.seed() ^ STATION_SALT);
        SectorEntityToken anchor = system.createToken(stationX, stationY);
        SectorEntityToken station = ChartSites.addSite(system, spawn.runId(), spawn.tier(), null, spawn.context(),
                ChartPois.byId(StarAnomalies.RESEARCH_STATION), new ChartSites.Orbit(anchor, 0f, 0f, 1f), random);
        if (station == null) {
            return GONE;
        }
        station.setFixedLocation(stationX, stationY);
        station.addTag(STATION_TAG);
        station.addTag(Tags.NON_CLICKABLE);
        CampaignFleetAPI guard = createGuard(spawn.runId(), spawn.tier(), spawn.context(), new Random(spawn.seed() ^ GUARD_SALT));
        if (guard == null) {
            station.removeTag(Tags.NON_CLICKABLE);
            return CLEARED;
        }
        system.addEntity(guard);
        guard.setLocation(stationX, stationY);
        guard.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, ChartContentGenerator.ASSIGNMENT_DAYS);
        return GUARDED;
    }

    static float guardSize(ChartTier adjustedTier) {
        return Math.max(adjustedTier.bossSize(), adjustedTier.fleetSize()) * BreachRules.INCURSION_GUARD_BOSS_SHARE;
    }

    static CampaignFleetAPI createGuard(String runId, int tier, ChartGenContext context, Random random) {
        ChartTier adjusted = ChartAnomalyFleets.tier(tier, context);
        float size = guardSize(adjusted);
        CampaignFleetAPI guard = ChartAnomalyFleets.create(ChartAnomalyFleets.bossCombat(Factions.DERELICT, FleetTypes.TASK_FORCE, adjusted, size), random);
        if (guard == null) {
            return null;
        }
        guard.setName(Translation.gameText("chart.anomaly.spatial_breach.incursion.guard.name"));
        guard.setNoFactionInName(true);
        ChartAnomalyFleets.prepare(guard, runId, tier, random.nextLong(), true, context);
        ChartFleetMarks.setStrengthBonus(guard.getMemoryWithoutUpdate(), ChartContentPlanner.overflowStrength(size, ChartContentPlanner.BOSS_SIZE_CAP));
        ChartFleetLedger.tagAnomaly(guard, SpatialBreach.ANOMALY_ID);
        guard.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        guard.addTag(GUARD_TAG);
        return guard;
    }

    static boolean isGuard(CampaignFleetAPI fleet) {
        return fleet != null && fleet.hasTag(GUARD_TAG);
    }

    private static CampaignFleetAPI guardIn(LocationAPI location) {
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (isGuard(fleet) && fleet.isAlive()) {
                return fleet;
            }
        }
        return null;
    }

    static SectorEntityToken stationIn(LocationAPI location) {
        List<SectorEntityToken> stations = location == null ? List.of() : location.getEntitiesWithTag(STATION_TAG);
        return stations.isEmpty() ? null : stations.get(0);
    }

    private static void unlockStation(LocationAPI location) {
        for (SectorEntityToken station : List.copyOf(location.getEntitiesWithTag(STATION_TAG))) {
            station.removeTag(Tags.NON_CLICKABLE);
        }
    }

    private static void vanish(LocationAPI location) {
        CampaignFleetAPI guard = guardIn(location);
        if (guard != null) {
            guard.despawn();
        }
        for (SectorEntityToken station : List.copyOf(location.getEntitiesWithTag(STATION_TAG))) {
            location.removeEntity(station);
        }
    }
}
