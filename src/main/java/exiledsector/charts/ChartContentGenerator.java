package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager.FlareManagerDelegate;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import exiledsector.charts.ChartContentPlan.FleetRole;
import exiledsector.charts.ChartContentPlan.PlannedFleet;
import exiledsector.charts.ChartContentPlan.PlannedPoi;
import exiledsector.i18n.Translation;
import exiledsector.items.WeightedPick;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class ChartContentGenerator {

    static final float POI_GAP = 100f;
    static final float POI_WORMHOLE_CLEARANCE = 2000f;
    static final float FLEET_WORMHOLE_CLEARANCE = 3000f;
    static final float GUARD_OFFSET = 250f;
    static final float STAR_CLEARANCE = 600f;
    static final float CORONA_CLEARANCE = 300f;
    static final float DERELICT_RECOVERY_CHANCE = 0.5f;
    static final float ASSIGNMENT_DAYS = 1_000_000f;
    static final float MIN_SPOT_WEIGHT = 0.0001f;
    static final long PLACEMENT_SALT = 0x43485053504f54L;
    static final long SALVAGE_SALT = 0x434853414c5647L;
    static final long FLEET_BUILD_SALT = 0x43484255494c44L;
    static final List<String> HOSTILITY_FLAGS = List.of(MemFlags.MEMORY_KEY_MAKE_HOSTILE, MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,
            MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, MemFlags.MEMORY_KEY_NO_JUMP, MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON,
            MemFlags.MEMORY_KEY_NO_REP_IMPACT, MemFlags.MEMORY_KEY_LOW_REP_IMPACT);
    static final List<String> REMOVED_ABILITIES = List.of(Abilities.GO_DARK, Abilities.TRANSVERSE_JUMP);
    static final Set<String> MEEK_PERSONALITIES = Set.of(Personalities.TIMID, Personalities.CAUTIOUS, Personalities.STEADY);
    private static final Logger LOG = Logger.getLogger(ChartContentGenerator.class);
    private static final ChartContentEngine VANILLA_ENGINE = new VanillaChartContentEngine();
    private static final Set<String> REPORTED_FAILURES = new HashSet<>();
    private static ChartContentEngine engine = VANILLA_ENGINE;

    record Hazard(Vector2f centre, float radius) {
    }

    private ChartContentGenerator() {
    }

    public static ChartContent generate(SectorAPI sector, StarSystemAPI pocket, String runId, long seed, int tier, Vector2f innerWormhole) {
        return generate(sector, pocket, runId, seed, tier, innerWormhole, ChartGenContext.none());
    }

    public static ChartContent generate(SectorAPI sector, StarSystemAPI pocket, String runId, long seed, int tier, Vector2f innerWormhole,
                                        ChartGenContext chartContext) {
        int clampedTier = ChartTiers.clamp(tier);
        ChartContentPlan plan;
        try {
            plan = ChartContentPlanner.plan(seed, ChartTiers.forTier(clampedTier), ChartPois.all(), ChartFactions.all(),
                    factionId -> isLoaded(sector, factionId), chartContext);
        } catch (RuntimeException e) {
            report("plan the content", seed, e);
            return ChartContent.none(clampedTier);
        }
        Build build = new Build(pocket, runId, clampedTier, seed, innerWormhole);
        try {
            build.placePois(plan);
            build.spawnFleets(plan);
            return build.settle(plan);
        } catch (RuntimeException | LinkageError e) {
            report("generate the content", seed, e);
            build.removeEverything();
            return ChartContent.none(clampedTier);
        }
    }

    static boolean isLoaded(SectorAPI sector, String factionId) {
        try {
            return sector.getFaction(factionId) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    static ChartObjective settleObjective(ChartObjective planned, boolean hasGuards, boolean hasBoss, boolean hasVault) {
        List<ChartObjective> order = new ArrayList<>();
        if (planned != null) {
            order.add(planned);
        }
        order.addAll(List.of(ChartObjective.CLEAR, ChartObjective.BOSS, ChartObjective.VAULT));
        for (ChartObjective candidate : order) {
            boolean achievable = switch (candidate) {
                case CLEAR -> hasGuards;
                case BOSS -> hasBoss;
                case VAULT -> hasVault;
            };
            if (achievable) {
                return candidate;
            }
        }
        return null;
    }

    static List<Hazard> hazards(StarSystemAPI pocket) {
        List<Hazard> hazards = new ArrayList<>();
        for (PlanetAPI body : pocket.getPlanets()) {
            if (body.isStar() && body.getLocation() != null) {
                hazards.add(new Hazard(new Vector2f(body.getLocation()), body.getRadius() + STAR_CLEARANCE));
            }
        }
        for (CampaignTerrainAPI terrain : pocket.getTerrainCopy()) {
            CampaignTerrainPlugin plugin = terrain.getPlugin();
            if (plugin instanceof StarCoronaTerrainPlugin && plugin instanceof BaseRingTerrain ring && ring.params != null && terrain.getLocation() != null) {
                float outerEdge = ring.params.middleRadius + ring.params.bandWidthInEngine * 0.5f;
                float flareReach = plugin instanceof FlareManagerDelegate flares ? ChartWormholePlacement.flareReach(flares, outerEdge) : 0f;
                hazards.add(new Hazard(new Vector2f(terrain.getLocation()), outerEdge + flareReach + CORONA_CLEARANCE));
            }
        }
        return hazards;
    }

    static boolean isClear(ChartSpot spot, List<Hazard> hazards, Vector2f innerWormhole, float wormholeClearance) {
        Vector2f position = spot.position();
        if (position == null) {
            return false;
        }
        if (innerWormhole != null) {
            if (Vector2f.sub(position, innerWormhole, new Vector2f()).length() < wormholeClearance) {
                return false;
            }
            if (spot.focus() != null) {
                float orbitRadius = spot.orbitRadius();
                ChartWormholePlacement.Band path = ChartWormholePlacement.centreBand(spot.focus(), 0).sweptAt(orbitRadius, orbitRadius);
                if (path.widenedBy(wormholeClearance).contains(innerWormhole.length())) {
                    return false;
                }
            }
        }
        for (Hazard hazard : hazards) {
            if (Vector2f.sub(position, hazard.centre(), new Vector2f()).length() < hazard.radius()) {
                return false;
            }
        }
        return true;
    }

    static List<ChartSpot> clearSpots(List<ChartSpot> spots, List<Hazard> hazards, Vector2f innerWormhole, float wormholeClearance) {
        List<ChartSpot> clear = new ArrayList<>();
        for (ChartSpot spot : spots) {
            if (isClear(spot, hazards, innerWormhole, wormholeClearance)) {
                clear.add(spot);
            }
        }
        return clear;
    }

    static Vector2f farSideOf(Vector2f innerWormhole) {
        if (innerWormhole == null || innerWormhole.length() < 1f) {
            return new Vector2f(-FLEET_WORMHOLE_CLEARANCE, 0f);
        }
        return new Vector2f(-innerWormhole.x, -innerWormhole.y);
    }

    static FleetParamsV3 fleetParams(String factionId, PlannedFleet planned, Random fleetRandom) {
        FleetParamsV3 params = new FleetParamsV3(null, factionId, planned.quality(), fleetType(planned.role()), planned.combatPoints(),
                0f, 0f, 0f, 0f, 0f, 0f);
        params.random = fleetRandom;
        params.ignoreMarketFleetSizeMult = true;
        params.withOfficers = true;
        params.officerLevelBonus = planned.officerLevelBonus();
        params.officerNumberBonus = planned.officerCountBonus();
        params.averageSMods = planned.averageSMods() > 0 ? planned.averageSMods() : null;
        if (planned.role() == FleetRole.BOSS) {
            params.forceAllowPhaseShipsEtc = true;
        }
        return params;
    }

    static String fleetType(FleetRole role) {
        return switch (role) {
            case GUARD -> FleetTypes.PATROL_MEDIUM;
            case PATROL -> FleetTypes.PATROL_LARGE;
            case BOSS -> FleetTypes.TASK_FORCE;
        };
    }

    static void prepareFleet(CampaignFleetAPI fleet, String runId, int tier, FleetRole role, long fleetSeed) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(ChartIds.TIER_MEMORY_KEY, tier);
        memory.set(ChartIds.SEED_MEMORY_KEY, Long.toString(fleetSeed, Character.MAX_RADIX));
        memory.set(ChartIds.fleetRunKey(runId), true);
        for (String flag : HOSTILITY_FLAGS) {
            memory.set(flag, true);
        }
        if (role == FleetRole.BOSS) {
            memory.set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        }
        fleet.setNoAutoDespawn(true);
        fleet.setTransponderOn(true);
        for (String abilityId : REMOVED_ABILITIES) {
            if (fleet.hasAbility(abilityId)) {
                fleet.removeAbility(abilityId);
            }
        }
        tagContent(fleet, runId);
        if (role == FleetRole.BOSS) {
            fleet.setName(Translation.gameText("chart.fleet.boss.name"));
            PersonAPI commander = fleet.getCommander();
            if (commander != null && commander.getPersonalityAPI() != null && MEEK_PERSONALITIES.contains(commander.getPersonalityAPI().getId())) {
                commander.setPersonality(Personalities.AGGRESSIVE);
            }
        }
    }

    static void tagContent(SectorEntityToken entity, String runId) {
        entity.addTag(ChartIds.ENTITY_TAG);
        entity.addTag(ChartIds.runTag(runId));
        entity.addTag(ChartIds.CONTENT_TAG);
    }

    private static void report(String what, long seed, Throwable cause) {
        boolean first;
        synchronized (REPORTED_FAILURES) {
            first = REPORTED_FAILURES.add(what);
        }
        if (first) {
            LOG.error("[ExiledSector] A Star Chart pocket (seed " + seed + ") could not " + what
                    + "; the pocket gets less content. Later failures of this kind are logged at debug level", cause);
        } else {
            LOG.debug("[ExiledSector] A Star Chart pocket (seed " + seed + ") could not " + what + " again", cause);
        }
    }

    static void useEngineForTests(ChartContentEngine testEngine) {
        engine = testEngine;
        REPORTED_FAILURES.clear();
    }

    static void resetForTests() {
        engine = VANILLA_ENGINE;
        REPORTED_FAILURES.clear();
    }

    private static final class Build {

        private final StarSystemAPI pocket;
        private final String runId;
        private final int tier;
        private final long seed;
        private final Vector2f innerWormhole;
        private final Random placementRandom;
        private final Random salvageRandom;
        private final Random fleetRandom;
        private final List<SectorEntityToken> poiEntities = new ArrayList<>();
        private final List<CampaignFleetAPI> fleets = new ArrayList<>();
        private final List<String> guardFleetIds = new ArrayList<>();
        private final Set<SectorEntityToken> usedFoci = new LinkedHashSet<>();
        private List<Hazard> hazards = List.of();
        private String bossFleetId;
        private String vaultEntityId;

        Build(StarSystemAPI pocket, String runId, int tier, long seed, Vector2f innerWormhole) {
            this.pocket = pocket;
            this.runId = runId;
            this.tier = tier;
            this.seed = seed;
            this.innerWormhole = innerWormhole == null ? null : new Vector2f(innerWormhole);
            this.placementRandom = ChartLayoutGenerator.derivedRandom(seed, PLACEMENT_SALT);
            this.salvageRandom = ChartLayoutGenerator.derivedRandom(seed, SALVAGE_SALT);
            this.fleetRandom = ChartLayoutGenerator.derivedRandom(seed, FLEET_BUILD_SALT);
        }

        void placePois(ChartContentPlan plan) {
            hazards = hazards(pocket);
            for (PlannedPoi planned : plan.pois()) {
                SectorEntityToken placed = null;
                try {
                    placed = placePoi(planned.poi(), plan.faction().factionId());
                } catch (RuntimeException | LinkageError e) {
                    report("place a " + planned.poi().id() + " point of interest", seed, e);
                }
                poiEntities.add(placed);
                if (placed != null && planned.poi().isVault()) {
                    vaultEntityId = placed.getId();
                }
            }
        }

        private SectorEntityToken placePoi(ChartPoi poi, String factionId) {
            List<ChartSpot> clear = clearSpots(engine.spots(pocket, placementRandom, usedFoci, POI_GAP), hazards, innerWormhole,
                    POI_WORMHOLE_CLEARANCE);
            ChartSpot spot = WeightedPick.pick(clear, candidate -> Math.max(candidate.weight(), MIN_SPOT_WEIGHT), placementRandom);
            boolean recoverable = salvageRandom.nextFloat() < DERELICT_RECOVERY_CHANCE;
            Random entityRandom = new Random(salvageRandom.nextLong());
            if (spot == null) {
                return null;
            }
            SectorEntityToken entity = poi.kind() == ChartPoi.Kind.DERELICT
                    ? engine.addDerelict(pocket, factionId, poi.derelictType(), recoverable, entityRandom)
                    : engine.addSalvageEntity(pocket, poi.entityType(), poi.specForSalvage(), entityRandom);
            if (entity == null) {
                return null;
            }
            try {
                engine.place(entity, spot);
                if (poi.kind() != ChartPoi.Kind.DERELICT) {
                    entity.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, poi.specForSalvage());
                }
                tagContent(entity, runId);
                if (poi.isVault()) {
                    entity.addTag(ChartIds.VAULT_TAG);
                    entity.setName(Translation.gameText("chart.poi.vault.name"));
                }
                if (poi.kind() != ChartPoi.Kind.CACHE && spot.focus() != null) {
                    usedFoci.add(spot.focus());
                }
                return entity;
            } catch (RuntimeException | LinkageError e) {
                pocket.removeEntity(entity);
                throw e;
            }
        }

        void spawnFleets(ChartContentPlan plan) {
            for (PlannedFleet planned : plan.fleets()) {
                long fleetSeed = fleetRandom.nextLong();
                try {
                    spawnFleet(plan, planned, fleetSeed);
                } catch (RuntimeException | LinkageError e) {
                    report("spawn a " + planned.role().name().toLowerCase(Locale.ROOT) + " fleet", seed, e);
                }
            }
        }

        private void spawnFleet(ChartContentPlan plan, PlannedFleet planned, long fleetSeed) {
            Random oneFleetRandom = new Random(fleetSeed);
            CampaignFleetAPI fleet = engine.createFleet(fleetParams(plan.faction().factionId(), planned, oneFleetRandom));
            if (fleet == null || fleet.isEmpty()) {
                return;
            }
            prepareFleet(fleet, runId, tier, planned.role(), fleetSeed);
            SectorEntityToken guarded = planned.guardsPoi() && planned.poiIndex() < poiEntities.size() ? poiEntities.get(planned.poiIndex()) : null;
            Vector2f spawnAt = spawnLocation(planned.role(), guarded, oneFleetRandom);
            pocket.addEntity(fleet);
            try {
                fleet.setLocation(spawnAt.x, spawnAt.y);
                assign(fleet, planned.role(), guarded);
            } catch (RuntimeException | LinkageError e) {
                pocket.removeEntity(fleet);
                throw e;
            }
            fleets.add(fleet);
            if (planned.role() == FleetRole.GUARD) {
                guardFleetIds.add(fleet.getId());
            } else if (planned.role() == FleetRole.BOSS) {
                bossFleetId = fleet.getId();
            }
        }

        private Vector2f spawnLocation(FleetRole role, SectorEntityToken guarded, Random oneFleetRandom) {
            if (guarded != null && guarded.getLocation() != null) {
                double angle = oneFleetRandom.nextDouble() * Math.PI * 2d;
                Vector2f at = guarded.getLocation();
                return new Vector2f(at.x + (float) Math.cos(angle) * GUARD_OFFSET, at.y + (float) Math.sin(angle) * GUARD_OFFSET);
            }
            if (role == FleetRole.PATROL) {
                List<ChartSpot> clear = clearSpots(engine.spots(pocket, oneFleetRandom, null, POI_GAP), hazards, innerWormhole,
                        FLEET_WORMHOLE_CLEARANCE);
                ChartSpot spot = WeightedPick.pick(clear, candidate -> Math.max(candidate.weight(), MIN_SPOT_WEIGHT), oneFleetRandom);
                if (spot != null) {
                    return new Vector2f(spot.position());
                }
            }
            return farSideOf(innerWormhole);
        }

        private void assign(CampaignFleetAPI fleet, FleetRole role, SectorEntityToken guarded) {
            SectorEntityToken patrolCentre = pocket.getCenter() != null ? pocket.getCenter() : pocket.getStar();
            if (role != FleetRole.PATROL) {
                SectorEntityToken holdPoint = guarded;
                if (holdPoint == null) {
                    Vector2f farSide = farSideOf(innerWormhole);
                    holdPoint = pocket.createToken(farSide.x, farSide.y);
                }
                fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, holdPoint, ASSIGNMENT_DAYS);
            }
            if (patrolCentre != null) {
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, patrolCentre, ASSIGNMENT_DAYS);
            }
        }

        ChartContent settle(ChartContentPlan plan) {
            ChartObjective objective = settleObjective(plan.objective(), !guardFleetIds.isEmpty(), bossFleetId != null, vaultEntityId != null);
            List<String> targets = objective == null ? List.of() : switch (objective) {
                case CLEAR -> guardFleetIds;
                case BOSS -> List.of(bossFleetId);
                case VAULT -> List.of(vaultEntityId);
            };
            int placedPois = 0;
            for (SectorEntityToken entity : poiEntities) {
                if (entity != null) {
                    placedPois++;
                }
            }
            return new ChartContent(tier, plan.faction().factionId(), objective, targets, bossFleetId, vaultEntityId, fleets.size(), placedPois);
        }

        void removeEverything() {
            for (CampaignFleetAPI fleet : fleets) {
                try {
                    fleet.despawn(FleetDespawnReason.PLAYER_FAR_AWAY, null);
                    pocket.removeEntity(fleet);
                } catch (RuntimeException e) {
                    LOG.debug("Removing a half-generated Star Chart fleet failed", e);
                }
            }
            for (SectorEntityToken entity : poiEntities) {
                if (entity != null) {
                    try {
                        pocket.removeEntity(entity);
                    } catch (RuntimeException e) {
                        LOG.debug("Removing a half-generated Star Chart point of interest failed", e);
                    }
                }
            }
            fleets.clear();
            poiEntities.clear();
        }
    }
}
