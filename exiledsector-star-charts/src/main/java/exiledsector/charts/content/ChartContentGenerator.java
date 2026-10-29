package exiledsector.charts.content;

import com.fs.starfarer.api.EveryFrameScript;
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
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.wormhole.ChartWormholePlacement;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.content.ChartContentPlan.PlannedPoi;
import exiledsector.charts.content.ChartContentPlan.PlannedStation;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.i18n.Translation;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.charts.loot.ChartFleetLoot;
import exiledsector.charts.loot.ChartVanillaLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.run.ChartRunBound;
import exiledsector.items.WeightedPick;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class ChartContentGenerator {

    static final float POI_GAP = 100f;
    public static final float POI_WORMHOLE_CLEARANCE = 2000f;
    static final float FLEET_WORMHOLE_CLEARANCE = 3000f;
    static final float GUARD_OFFSET = 250f;
    static final float STATION_ORBIT_GAP = 300f;
    static final float STATION_ORBIT_DAYS = 60f;
    static final float STATION_SPIN = 5f;
    static final float STAR_CLEARANCE = 600f;
    static final float CORONA_CLEARANCE = 300f;
    static final float DERELICT_RECOVERY_CHANCE = 0.5f;
    public static final float ASSIGNMENT_DAYS = 1_000_000f;
    static final float MIN_SPOT_WEIGHT = 0.0001f;
    static final long PLACEMENT_SALT = 0x43485053504f54L;
    static final long SALVAGE_SALT = 0x434853414c5647L;
    static final long FLEET_BUILD_SALT = 0x43484255494c44L;
    static final long LOOT_SALT = 0x43484c4f4f5453L;
    static final long COLONY_ITEM_SALT = 0x4348434f4c4f4eL;
    static final long STATION_SALT = 0x43485354415449L;
    static final long STAGING_SALT = 0x4348535441474eL;
    static final String HOSTILE_TO_FACTION_PREFIX = "$cfai_makeHostile_";
    static final String MAKE_NON_HOSTILE = "$cfai_makeNonHostile";
    static final String NON_HOSTILE_TAKES_PRIORITY = "$makeNonHostileTakesPriority";
    static final List<String> HOSTILITY_FLAGS = List.of(MemFlags.MEMORY_KEY_MAKE_HOSTILE, MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,
            MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, MemFlags.MEMORY_KEY_NO_JUMP, MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON,
            MemFlags.MEMORY_KEY_NO_REP_IMPACT, MemFlags.MEMORY_KEY_LOW_REP_IMPACT);
    static final List<String> REMOVED_ABILITIES = List.of(Abilities.GO_DARK, Abilities.TRANSVERSE_JUMP);
    static final Set<String> MEEK_PERSONALITIES = Set.of(Personalities.TIMID, Personalities.CAUTIOUS, Personalities.STEADY);
    private static final Logger LOG = Logger.getLogger(ChartContentGenerator.class);
    private static final ChartContentEngine VANILLA_ENGINE = new VanillaChartContentEngine();
    private static final Set<String> REPORTED_FAILURES = new HashSet<>();
    private static ChartContentEngine engine = VANILLA_ENGINE;

    public record Hazard(Vector2f centre, float radius) {
    }

    private ChartContentGenerator() {
    }

    public static ChartContent generate(SectorAPI sector, StarSystemAPI system, String runId, long seed, int tier, Vector2f innerWormhole) {
        return generate(sector, system, runId, seed, tier, innerWormhole, ChartGenContext.none());
    }

    public static ChartContent generate(SectorAPI sector, StarSystemAPI system, String runId, long seed, int tier, Vector2f innerWormhole,
                                        ChartGenContext chartContext) {
        return generate(sector, system, runId, seed, tier, innerWormhole, chartContext, null);
    }

    public static ChartContent generate(SectorAPI sector, StarSystemAPI system, String runId, long seed, int tier, Vector2f innerWormhole,
                                        ChartGenContext chartContext, SectorEntityToken featureAnchor) {
        return generate(sector, system, runId, seed, tier, innerWormhole, chartContext, featureAnchor,
                ChartAnomalies.planned(chartContext));
    }

    public static ChartContent generate(SectorAPI sector, StarSystemAPI system, String runId, long seed, int tier, Vector2f innerWormhole,
                                        ChartGenContext chartContext, SectorEntityToken featureAnchor, List<String> anomalyIds) {
        int clampedTier = ChartTiers.clamp(tier);
        ChartContentPlan plan;
        try {
            plan = ChartContentPlanner.plan(seed, ChartTiers.forTier(clampedTier), ChartPois.all(), ChartFactions.all(),
                    factionId -> isLoaded(sector, factionId), chartContext, StarChartSettings.fleetLimit(), anomalyIds);
        } catch (RuntimeException e) {
            report("plan the content", seed, e);
            return ChartContent.none(clampedTier);
        }
        Build build = new Build(system, runId, clampedTier, seed, innerWormhole,
                (chartContext == null ? ChartGenContext.none() : chartContext).withAnomalyEffects(plan.anomalies()), featureAnchor);
        try {
            build.placePois(plan);
            build.spawnFleets(plan);
            build.placeColonyItem(plan);
            build.generateAnomalies(plan);
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

    static ChartObjective settleObjective(ChartObjective planned, boolean hasGuards, boolean hasBoss) {
        List<ChartObjective> order = new ArrayList<>();
        if (planned != null) {
            order.add(planned);
        }
        order.addAll(List.of(ChartObjective.CLEAR, ChartObjective.BOSS));
        for (ChartObjective candidate : order) {
            boolean achievable = switch (candidate) {
                case CLEAR -> hasGuards;
                case BOSS -> hasBoss;
            };
            if (achievable) {
                return candidate;
            }
        }
        return null;
    }

    static List<Hazard> hazards(StarSystemAPI system) {
        List<Hazard> hazards = new ArrayList<>();
        for (PlanetAPI body : system.getPlanets()) {
            if (body.isStar() && body.getLocation() != null) {
                hazards.add(new Hazard(new Vector2f(body.getLocation()), body.getRadius() + STAR_CLEARANCE));
            }
        }
        for (CampaignTerrainAPI terrain : system.getTerrainCopy()) {
            CampaignTerrainPlugin plugin = terrain.getPlugin();
            if (plugin instanceof StarCoronaTerrainPlugin && plugin instanceof BaseRingTerrain ring && ring.params != null && terrain.getLocation() != null) {
                float outerEdge = ring.params.middleRadius + ring.params.bandWidthInEngine * 0.5f;
                float flareReach = plugin instanceof FlareManagerDelegate flares ? ChartWormholePlacement.flareReach(flares, outerEdge) : 0f;
                hazards.add(new Hazard(new Vector2f(terrain.getLocation()), outerEdge + flareReach + CORONA_CLEARANCE));
            }
        }
        return hazards;
    }

    public static boolean isClear(ChartSpot spot, List<Hazard> hazards, Vector2f innerWormhole, float wormholeClearance) {
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
        if (planned.role().bossFleet()) {
            params.forceAllowPhaseShipsEtc = true;
        }
        return params;
    }

    static String fleetType(FleetRole role) {
        return switch (role) {
            case GUARD -> FleetTypes.PATROL_MEDIUM;
            case PATROL -> FleetTypes.PATROL_LARGE;
            case BOSS, DEFENDER -> FleetTypes.TASK_FORCE;
        };
    }

    public static void applyEnemySensorRange(CampaignFleetAPI fleet, float sensorRangeBonus) {
        if (sensorRangeBonus > 0f && fleet.getStats() != null) {
            fleet.getStats().getSensorRangeMod().modifyFlat(ChartIds.ENEMY_SENSOR_MOD_ID, sensorRangeBonus);
        }
    }

    static void prepareFleet(CampaignFleetAPI fleet, String runId, int tier, FleetRole role, int strengthBonus, long fleetSeed) {
        markContent(fleet, runId, tier, strengthBonus, fleetSeed, role.bossFleet());
        if (role == FleetRole.DEFENDER) {
            fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT);
        }
        fleet.setTransponderOn(true);
        for (String abilityId : REMOVED_ABILITIES) {
            if (fleet.hasAbility(abilityId)) {
                fleet.removeAbility(abilityId);
            }
        }
        if (role.bossFleet()) {
            fleet.setName(Translation.gameText(role == FleetRole.DEFENDER ? "chart.fleet.bossDefenders.name" : "chart.fleet.boss.name"));
            PersonAPI commander = fleet.getCommander();
            if (commander != null && commander.getPersonalityAPI() != null && MEEK_PERSONALITIES.contains(commander.getPersonalityAPI().getId())) {
                commander.setPersonality(Personalities.AGGRESSIVE);
            }
        }
    }

    static void joinSkirmish(CampaignFleetAPI fleet, boolean defending, ChartContentPlan plan, String allyFactionId) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        String enemyFactionId = defending ? plan.skirmish().attackerFactionId() : plan.faction().factionId();
        memory.set(HOSTILE_TO_FACTION_PREFIX + enemyFactionId, true);
        memory.set(ChartIds.SKIRMISH_SIDE_MEMORY_KEY, (defending ? SkirmishSide.DEFENDER : SkirmishSide.ATTACKER).key());
        if (plan.skirmish().alliedWith(defending)) {
            memory.unset(MemFlags.MEMORY_KEY_MAKE_HOSTILE);
            memory.unset(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE);
            memory.set(MAKE_NON_HOSTILE, true);
            memory.set(NON_HOSTILE_TAKES_PRIORITY, true);
            memory.unset(ChartIds.START_DP_MEMORY_KEY);
            if (allyFactionId != null) {
                fleet.setFaction(allyFactionId, true);
            }
        } else if (allyFactionId != null) {
            memory.set(HOSTILE_TO_FACTION_PREFIX + allyFactionId, true);
        }
    }

    static void prepareStation(CampaignFleetAPI station, String runId, int tier, long stationSeed) {
        markContent(station, runId, tier, 0, stationSeed, true);
        station.setName(Translation.gameText("chart.fleet.bossStation.name"));
    }

    private static void markContent(CampaignFleetAPI fleet, String runId, int tier, int strengthBonus, long fleetSeed, boolean boss) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(ChartIds.TIER_MEMORY_KEY, tier);
        if (strengthBonus > 0) {
            memory.set(ChartIds.STRENGTH_BONUS_MEMORY_KEY, strengthBonus);
        }
        memory.set(ChartIds.SEED_MEMORY_KEY, Long.toString(fleetSeed, Character.MAX_RADIX));
        memory.set(ChartIds.fleetRunKey(runId), true);
        ChartFleetLoot.recordStart(fleet);
        for (String flag : HOSTILITY_FLAGS) {
            memory.set(flag, true);
        }
        if (boss) {
            memory.set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
            memory.set(ChartIds.BOSS_MEMORY_KEY, true);
        }
        fleet.setNoAutoDespawn(true);
        tagContent(fleet, runId);
    }

    public static void tagContent(SectorEntityToken entity, String runId) {
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
            LOG.error("[ExiledSector] A Star Chart system (seed " + seed + ") could not " + what
                    + "; the system gets less content. Later failures of this kind are logged at debug level", cause);
        } else {
            LOG.debug("[ExiledSector] A Star Chart system (seed " + seed + ") could not " + what + " again", cause);
        }
    }

    static ChartContentEngine engine() {
        return engine;
    }

    static void useEngineForTests(ChartContentEngine testEngine) {
        engine = testEngine;
        REPORTED_FAILURES.clear();
    }

    public static void resetForTests() {
        engine = VANILLA_ENGINE;
        REPORTED_FAILURES.clear();
    }

    private static final class Build {

        private final StarSystemAPI system;
        private final String runId;
        private final int tier;
        private final long seed;
        private final Vector2f innerWormhole;
        private final ChartGenContext chartContext;
        private final Random placementRandom;
        private final Random salvageRandom;
        private final Random fleetRandom;
        private final Random lootRandom;
        private final List<SectorEntityToken> poiEntities = new ArrayList<>();
        private final List<CampaignFleetAPI> fleets = new ArrayList<>();
        private final List<String> guardFleetIds = new ArrayList<>();
        private final List<SectorEntityToken> guardedPois = new ArrayList<>();
        private final Set<SectorEntityToken> usedFoci = new LinkedHashSet<>();
        private final List<String> anomalies = new ArrayList<>();
        private final List<SectorEntityToken> anomalySites = new ArrayList<>();
        private List<Hazard> hazards = List.of();
        private String bossFleetId;
        private ChartBossType bossType = ChartBossType.FLEET;
        private CampaignFleetAPI skirmishStation;
        private String allyFactionId;
        private String defenderBossId;
        private String commanderId;
        private final List<String> attackerFleetIds = new ArrayList<>();
        private Vector2f stagingPoint;
        private final SectorEntityToken featureAnchor;

        Build(StarSystemAPI system, String runId, int tier, long seed, Vector2f innerWormhole, ChartGenContext chartContext,
              SectorEntityToken featureAnchor) {
            this.featureAnchor = featureAnchor;
            this.system = system;
            this.runId = runId;
            this.tier = tier;
            this.seed = seed;
            this.innerWormhole = innerWormhole == null ? null : new Vector2f(innerWormhole);
            this.chartContext = chartContext;
            this.placementRandom = ChartLayoutGenerator.derivedRandom(seed, PLACEMENT_SALT);
            this.salvageRandom = ChartLayoutGenerator.derivedRandom(seed, SALVAGE_SALT);
            this.fleetRandom = ChartLayoutGenerator.derivedRandom(seed, FLEET_BUILD_SALT);
            this.lootRandom = ChartLayoutGenerator.derivedRandom(seed, LOOT_SALT);
        }

        void placePois(ChartContentPlan plan) {
            hazards = hazards(system);
            for (PlannedPoi planned : plan.pois()) {
                SectorEntityToken placed = null;
                try {
                    placed = placePoi(planned.poi(), plan.faction().factionId());
                } catch (RuntimeException | LinkageError e) {
                    report("place a " + planned.poi().id() + " point of interest", seed, e);
                }
                poiEntities.add(placed);
            }
        }

        private SectorEntityToken placePoi(ChartPoi poi, String factionId) {
            List<ChartSpot> clear = clearSpots(engine.spots(system, placementRandom, usedFoci, POI_GAP), hazards, innerWormhole,
                    POI_WORMHOLE_CLEARANCE);
            if (!poi.planetCategories().isEmpty()) {
                List<ChartSpot> matching = clear.stream().filter(candidate -> candidate.focus() instanceof PlanetAPI planet && !planet.isStar()
                        && poi.planetCategories().contains(engine.planetCategory(planet))).toList();
                clear = matching.isEmpty() ? clear : matching;
            }
            ChartSpot spot = WeightedPick.pick(clear, candidate -> Math.max(candidate.weight(), MIN_SPOT_WEIGHT), placementRandom);
            boolean recoverable = salvageRandom.nextFloat() < DERELICT_RECOVERY_CHANCE;
            Random entityRandom = new Random(salvageRandom.nextLong());
            if (spot == null) {
                return null;
            }
            SectorEntityToken entity = poi.kind() == ChartPoi.Kind.DERELICT
                    ? engine.addDerelict(system, factionId, poi.derelictType(), recoverable, entityRandom)
                    : engine.addSalvageEntity(system, poi.entityType(), poi.specForSalvage(), entityRandom);
            if (entity == null) {
                return null;
            }
            try {
                engine.place(entity, spot);
                if (poi.kind() != ChartPoi.Kind.DERELICT) {
                    entity.getMemoryWithoutUpdate().set(MemFlags.SALVAGE_SPEC_ID_OVERRIDE, poi.specForSalvage());
                }
                tagContent(entity, runId);
                if (poi.kind() != ChartPoi.Kind.CACHE && spot.focus() != null) {
                    usedFoci.add(spot.focus());
                }
            } catch (RuntimeException | LinkageError e) {
                system.removeEntity(entity);
                throw e;
            }
            addVanillaLoot(entity, poi);
            return entity;
        }

        private void addVanillaLoot(SectorEntityToken site, ChartPoi poi) {
            try {
                ChartVanillaLoot.SiteDrops specDrops = poi.kind() == ChartPoi.Kind.DERELICT ? ChartVanillaLoot.SiteDrops.NONE
                        : engine.siteDrops(poi.specForSalvage());
                ChartVanillaLoot.addToSite(site, poi.kind(), specDrops, chartContext, tier, lootRandom);
            } catch (RuntimeException | LinkageError e) {
                report("add loot to a " + poi.id() + " point of interest", seed, e);
            }
        }

        void placeColonyItem(ChartContentPlan plan) {
            try {
                Random colonyItemRandom = ChartLayoutGenerator.derivedRandom(seed, COLONY_ITEM_SALT);
                float chance = ChartColonyItems.chance(ChartTiers.colonyItemChance(tier), chartContext.dropMult(DropType.COLONY_ITEM));
                if (!(colonyItemRandom.nextFloat() < chance)) {
                    return;
                }
                String itemId = ChartColonyItems.pick(colonyItemRandom);
                SectorEntityToken holder = colonyItemHolder(plan, colonyItemRandom);
                if (itemId != null && holder != null) {
                    engine.addColonyItem(holder, itemId);
                }
            } catch (RuntimeException | LinkageError e) {
                report("place a colony item", seed, e);
            }
        }

        private SectorEntityToken colonyItemHolder(ChartContentPlan plan, Random colonyItemRandom) {
            for (CampaignFleetAPI fleet : fleets) {
                if (fleet.getId() != null && fleet.getId().equals(bossFleetId) && !fleet.getMemoryWithoutUpdate().getBoolean(MAKE_NON_HOSTILE)) {
                    return fleet;
                }
            }
            List<SectorEntityToken> stationsAndDomainSites = new ArrayList<>();
            List<SectorEntityToken> anySites = new ArrayList<>();
            for (int i = 0; i < poiEntities.size() && i < plan.pois().size(); i++) {
                SectorEntityToken site = poiEntities.get(i);
                if (site == null) {
                    continue;
                }
                anySites.add(site);
                ChartPoi.Kind kind = plan.pois().get(i).poi().kind();
                if (kind == ChartPoi.Kind.STATION || kind == ChartPoi.Kind.DOMAIN) {
                    stationsAndDomainSites.add(site);
                }
            }
            List<SectorEntityToken> candidates = stationsAndDomainSites.isEmpty() ? anySites : stationsAndDomainSites;
            return candidates.isEmpty() ? null : candidates.get(colonyItemRandom.nextInt(candidates.size()));
        }

        void spawnFleets(ChartContentPlan plan) {
            List<PlannedFleet> defenders = new ArrayList<>(plan.fleets());
            if (plan.skirmish() != null) {
                allyFactionId = dressAllies(plan);
                defenders.sort(Comparator.comparing(planned -> planned.role() != FleetRole.DEFENDER));
            }
            for (PlannedFleet planned : defenders) {
                long fleetSeed = fleetRandom.nextLong();
                try {
                    spawnFleet(plan, planned, fleetSeed);
                } catch (RuntimeException | LinkageError e) {
                    report("spawn a " + planned.role().name().toLowerCase(Locale.ROOT) + " fleet", seed, e);
                }
            }
            if (plan.skirmish() == null) {
                return;
            }
            defenderBossId = bossFleetId;
            for (PlannedFleet planned : plan.skirmish().attackerFleets()) {
                long fleetSeed = fleetRandom.nextLong();
                try {
                    spawnAttackerFleet(plan, planned, fleetSeed);
                } catch (RuntimeException | LinkageError e) {
                    report("spawn a skirmish attacker fleet", seed, e);
                }
            }
        }

        private void spawnAttackerFleet(ChartContentPlan plan, PlannedFleet planned, long fleetSeed) {
            Random oneFleetRandom = new Random(fleetSeed);
            boolean ally = plan.skirmish().alliedWith(false);
            FleetRole role = planned.role();
            CampaignFleetAPI fleet = engine.createFleet(fleetParams(plan.skirmish().attackerFactionId(), planned, oneFleetRandom));
            if (fleet == null || fleet.isEmpty()) {
                return;
            }
            prepareFleet(fleet, runId, tier, role, planned.strengthBonus(), fleetSeed);
            joinSkirmish(fleet, false, plan, allyFactionId);
            if (role == FleetRole.BOSS) {
                fleet.getMemoryWithoutUpdate().set(ChartIds.SKIRMISH_COMMANDER_MEMORY_KEY, true);
            }
            if (!ally) {
                applyEnemySensorRange(fleet, chartContext.enemySensorRangeBonus());
                try {
                    ChartVanillaLoot.addToFleet(fleet, role == FleetRole.BOSS, chartContext, tier);
                } catch (RuntimeException | LinkageError e) {
                    report("add loot to a skirmish attacker fleet", seed, e);
                }
            }
            Vector2f staging = stagingPoint();
            double angle = oneFleetRandom.nextDouble() * Math.PI * 2d;
            system.addEntity(fleet);
            try {
                fleet.setLocation(staging.x + (float) Math.cos(angle) * GUARD_OFFSET, staging.y + (float) Math.sin(angle) * GUARD_OFFSET);
                assignAttacker(fleet, role, staging);
            } catch (RuntimeException | LinkageError e) {
                system.removeEntity(fleet);
                throw e;
            }
            fleets.add(fleet);
            attackerFleetIds.add(fleet.getId());
            if (role == FleetRole.BOSS) {
                commanderId = fleet.getId();
                if (plan.skirmish().side() == SkirmishSide.DEFENDER) {
                    bossFleetId = commanderId;
                    bossType = ChartBossType.FLEET;
                }
            }
        }

        private String dressAllies(ChartContentPlan plan) {
            SkirmishSide side = plan.skirmish().side();
            if (side == SkirmishSide.NEITHER) {
                return null;
            }
            try {
                return engine.dressAllies(side == SkirmishSide.DEFENDER ? plan.faction().factionId() : plan.skirmish().attackerFactionId());
            } catch (RuntimeException | LinkageError e) {
                report("dress the skirmish allies", seed, e);
                return null;
            }
        }

        private Vector2f stagingPoint() {
            if (stagingPoint != null) {
                return stagingPoint;
            }
            Vector2f defended = skirmishStation != null && skirmishStation.getLocation() != null ? new Vector2f(skirmishStation.getLocation())
                    : farSideOf(innerWormhole);
            List<ChartSpot> clear = clearSpots(engine.spots(system, ChartLayoutGenerator.derivedRandom(seed, STAGING_SALT), null, POI_GAP), hazards,
                    innerWormhole, FLEET_WORMHOLE_CLEARANCE);
            ChartSpot farthest = null;
            float farthestDistance = -1f;
            for (ChartSpot spot : clear) {
                float distance = Vector2f.sub(spot.position(), defended, new Vector2f()).length();
                if (distance > farthestDistance) {
                    farthest = spot;
                    farthestDistance = distance;
                }
            }
            stagingPoint = farthest != null ? new Vector2f(farthest.position()) : new Vector2f(-defended.y, defended.x);
            return stagingPoint;
        }

        private void assignAttacker(CampaignFleetAPI fleet, FleetRole role, Vector2f staging) {
            if (role == FleetRole.BOSS) {
                fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, system.createToken(staging.x, staging.y), ASSIGNMENT_DAYS);
                return;
            }
            if (skirmishStation != null) {
                fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, skirmishStation, ASSIGNMENT_DAYS);
            }
            SectorEntityToken patrolCentre = system.getCenter() != null ? system.getCenter() : system.getStar();
            if (patrolCentre != null) {
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, patrolCentre, ASSIGNMENT_DAYS);
            }
        }

        private void spawnFleet(ChartContentPlan plan, PlannedFleet planned, long fleetSeed) {
            Random oneFleetRandom = new Random(fleetSeed);
            SectorEntityToken guarded = planned.guardsPoi() && planned.poiIndex() < poiEntities.size() ? poiEntities.get(planned.poiIndex()) : null;
            if (featureAnchor != null && planned.role().bossFleet()) {
                guarded = featureAnchor;
            }
            PlannedFleet built = planned;
            if (planned.role() == FleetRole.DEFENDER) {
                CampaignFleetAPI station = spawnStation(plan, guarded, fleetSeed);
                if (station != null) {
                    guarded = station;
                } else {
                    built = fallbackBoss(plan.bossStation(), planned);
                }
            }
            FleetRole role = built.role();
            boolean ally = plan.skirmish() != null && plan.skirmish().alliedWith(true);
            CampaignFleetAPI fleet = engine.createFleet(fleetParams(plan.faction().factionId(), built, oneFleetRandom));
            if (fleet == null || fleet.isEmpty()) {
                return;
            }
            prepareFleet(fleet, runId, tier, role, built.strengthBonus(), fleetSeed);
            if (plan.skirmish() != null) {
                joinSkirmish(fleet, true, plan, allyFactionId);
            }
            if (!ally) {
                applyEnemySensorRange(fleet, chartContext.enemySensorRangeBonus());
                try {
                    ChartVanillaLoot.addToFleet(fleet, role == FleetRole.BOSS, chartContext, tier);
                } catch (RuntimeException | LinkageError e) {
                    report("add loot to a " + role.name().toLowerCase(Locale.ROOT) + " fleet", seed, e);
                }
            }
            Vector2f spawnAt = spawnLocation(role, guarded, oneFleetRandom);
            system.addEntity(fleet);
            try {
                fleet.setLocation(spawnAt.x, spawnAt.y);
                assign(fleet, role, guarded);
            } catch (RuntimeException | LinkageError e) {
                system.removeEntity(fleet);
                throw e;
            }
            fleets.add(fleet);
            if (role == FleetRole.GUARD) {
                guardFleetIds.add(fleet.getId());
                if (guarded != null && !guardedPois.contains(guarded)) {
                    guardedPois.add(guarded);
                }
            } else if (role == FleetRole.BOSS) {
                bossFleetId = fleet.getId();
                bossType = ChartBossType.FLEET;
            }
        }

        private static PlannedFleet fallbackBoss(PlannedStation station, PlannedFleet guard) {
            if (station != null && station.fallbackBoss() != null) {
                return station.fallbackBoss();
            }
            return new PlannedFleet(FleetRole.BOSS, guard.poiIndex(), guard.combatPoints(), guard.quality(), guard.officerLevelBonus(),
                    guard.officerCountBonus(), guard.strengthBonus(), guard.averageSMods());
        }

        private CampaignFleetAPI spawnStation(ChartContentPlan plan, SectorEntityToken anchor, long fleetSeed) {
            PlannedStation planned = plan.bossStation();
            if (planned == null) {
                return null;
            }
            long stationSeed = fleetSeed ^ STATION_SALT;
            Random stationRandom = new Random(stationSeed);
            try {
                CampaignFleetAPI station = engine.createStation(plan.faction().factionId(), planned.variantId(), planned.officerLevel(), stationRandom);
                if (station == null || station.isEmpty()) {
                    report("build a boss station", seed, new IllegalStateException("Station variant " + planned.variantId() + " is not loaded"));
                    return null;
                }
                prepareStation(station, runId, tier, stationSeed);
                if (plan.skirmish() != null) {
                    joinSkirmish(station, true, plan, allyFactionId);
                }
                if (plan.skirmish() == null || !plan.skirmish().alliedWith(true)) {
                    try {
                        ChartVanillaLoot.addToFleet(station, true, chartContext, tier);
                    } catch (RuntimeException | LinkageError e) {
                        report("add loot to a boss station", seed, e);
                    }
                }
                system.addEntity(station);
                try {
                    station.setAI(null);
                    anchorStation(station, anchor, stationRandom);
                } catch (RuntimeException | LinkageError e) {
                    system.removeEntity(station);
                    throw e;
                }
                fleets.add(station);
                bossFleetId = station.getId();
                bossType = ChartBossType.STATION;
                if (plan.skirmish() != null) {
                    skirmishStation = station;
                }
                return station;
            } catch (RuntimeException | LinkageError e) {
                report("spawn a boss station", seed, e);
                return null;
            }
        }

        private void anchorStation(CampaignFleetAPI station, SectorEntityToken anchor, Random stationRandom) {
            if (anchor == null || anchor.getLocation() == null) {
                Vector2f farSide = farSideOf(innerWormhole);
                station.setFixedLocation(farSide.x, farSide.y);
                return;
            }
            float angle = stationRandom.nextFloat() * 360f;
            float orbitRadius = anchor.getRadius() + STATION_ORBIT_GAP;
            Vector2f at = anchor.getLocation();
            double radians = Math.toRadians(angle);
            station.setLocation(at.x + (float) Math.cos(radians) * orbitRadius, at.y + (float) Math.sin(radians) * orbitRadius);
            station.setCircularOrbitWithSpin(anchor, angle, orbitRadius, STATION_ORBIT_DAYS, STATION_SPIN, STATION_SPIN);
        }

        private Vector2f spawnLocation(FleetRole role, SectorEntityToken guarded, Random oneFleetRandom) {
            if (guarded != null && guarded.getLocation() != null) {
                double angle = oneFleetRandom.nextDouble() * Math.PI * 2d;
                Vector2f at = guarded.getLocation();
                return new Vector2f(at.x + (float) Math.cos(angle) * GUARD_OFFSET, at.y + (float) Math.sin(angle) * GUARD_OFFSET);
            }
            if (role == FleetRole.PATROL) {
                List<ChartSpot> clear = clearSpots(engine.spots(system, oneFleetRandom, null, POI_GAP), hazards, innerWormhole,
                        FLEET_WORMHOLE_CLEARANCE);
                ChartSpot spot = WeightedPick.pick(clear, candidate -> Math.max(candidate.weight(), MIN_SPOT_WEIGHT), oneFleetRandom);
                if (spot != null) {
                    return new Vector2f(spot.position());
                }
            }
            return farSideOf(innerWormhole);
        }

        private void assign(CampaignFleetAPI fleet, FleetRole role, SectorEntityToken guarded) {
            SectorEntityToken patrolCentre = system.getCenter() != null ? system.getCenter() : system.getStar();
            if (role == FleetRole.PATROL && skirmishStation != null) {
                fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, skirmishStation, ASSIGNMENT_DAYS);
            } else if (role != FleetRole.PATROL) {
                SectorEntityToken holdPoint = guarded;
                if (holdPoint == null) {
                    Vector2f farSide = farSideOf(innerWormhole);
                    holdPoint = system.createToken(farSide.x, farSide.y);
                }
                FleetAssignment hold = role == FleetRole.DEFENDER ? FleetAssignment.ORBIT_PASSIVE : FleetAssignment.ORBIT_AGGRESSIVE;
                fleet.addAssignment(hold, holdPoint, ASSIGNMENT_DAYS);
            }
            if (patrolCentre != null) {
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, patrolCentre, ASSIGNMENT_DAYS);
            }
        }

        void generateAnomalies(ChartContentPlan plan) {
            for (String anomalyId : plan.anomalies()) {
                ChartAnomaly anomaly = ChartAnomalies.byId(anomalyId);
                if (anomaly == null) {
                    continue;
                }
                AnomalyContentSite site = new AnomalyContentSite(system, runId, tier, plan.faction().factionId(), chartContext, poiEntities,
                        guardedPois, innerWormhole, spot -> isClear(spot, hazards, innerWormhole, POI_WORMHOLE_CLEARANCE),
                        salt -> ChartLayoutGenerator.derivedRandom(seed ^ ((long) anomalyId.hashCode() << 32), salt));
                try {
                    anomaly.generate(site);
                    anomalies.add(anomalyId);
                } catch (RuntimeException | LinkageError e) {
                    site.placed().forEach(system::removeEntity);
                    report("generate the " + anomalyId + " anomaly", seed, e);
                    continue;
                }
                anomalySites.addAll(site.placed());
            }
        }

        ChartContent settle(ChartContentPlan plan) {
            if (plan.skirmish() != null) {
                return settleSkirmish(plan);
            }
            ChartObjective objective = settleObjective(plan.objective(), !guardFleetIds.isEmpty(), bossFleetId != null);
            List<String> targets = objective == null ? List.of() : switch (objective) {
                case CLEAR -> guardFleetIds;
                case BOSS -> List.of(bossFleetId);
            };
            return new ChartContent(tier, plan.faction().factionId(), objective, targets, bossFleetId, bossType, fleets.size(), placedPois(), anomalies);
        }

        private ChartContent settleSkirmish(ChartContentPlan plan) {
            if (plan.skirmish().side() != SkirmishSide.DEFENDER) {
                ChartObjective objective = settleObjective(ChartObjective.BOSS, !guardFleetIds.isEmpty(), bossFleetId != null);
                List<String> targets = objective == null ? List.of() : switch (objective) {
                    case CLEAR -> guardFleetIds;
                    case BOSS -> List.of(bossFleetId);
                };
                return new ChartContent(tier, plan.faction().factionId(), objective, targets, bossFleetId, bossType, fleets.size(), placedPois(), anomalies);
            }
            String protectedId = skirmishStation != null ? skirmishStation.getId() : defenderBossId;
            ChartObjective objective = commanderId != null ? ChartObjective.BOSS : attackerFleetIds.isEmpty() ? null : ChartObjective.CLEAR;
            List<String> targets = commanderId != null ? List.of(commanderId) : attackerFleetIds;
            return new ChartContent(tier, plan.faction().factionId(), objective, objective == null ? List.of() : targets, commanderId,
                    ChartBossType.FLEET, fleets.size(), placedPois(), anomalies, protectedId);
        }

        private int placedPois() {
            int placedPois = 0;
            for (SectorEntityToken entity : poiEntities) {
                if (entity != null) {
                    placedPois++;
                }
            }
            return placedPois;
        }

        void removeEverything() {
            for (CampaignFleetAPI fleet : fleets) {
                try {
                    fleet.despawn(FleetDespawnReason.PLAYER_FAR_AWAY, null);
                    system.removeEntity(fleet);
                } catch (RuntimeException e) {
                    LOG.debug("Removing a half-generated Star Chart fleet failed", e);
                }
            }
            List<SectorEntityToken> sites = new ArrayList<>(poiEntities);
            sites.addAll(anomalySites);
            for (SectorEntityToken entity : sites) {
                if (entity != null) {
                    try {
                        system.removeEntity(entity);
                    } catch (RuntimeException e) {
                        LOG.debug("Removing a half-generated Star Chart point of interest failed", e);
                    }
                }
            }
            for (EveryFrameScript script : new ArrayList<>(system.getScripts())) {
                if (script instanceof ChartRunBound bound && bound.belongsToRun(runId)) {
                    system.removeScript(script);
                }
            }
            fleets.clear();
            poiEntities.clear();
            anomalySites.clear();
        }
    }
}
