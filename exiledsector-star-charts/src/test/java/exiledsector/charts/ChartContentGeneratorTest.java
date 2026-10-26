package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData;
import exiledsector.charts.ChartContentGenerator.Hazard;
import exiledsector.charts.ChartContentPlan.FleetRole;
import exiledsector.charts.ChartContentPlan.PlannedFleet;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.SpatialBreach;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartContentGeneratorTest {

    private static final String RUN_ID = "run1";
    private static final Vector2f WORMHOLE = new Vector2f(6000f, 0f);

    private SectorAPI sector;
    private StarSystemAPI system;
    private PlanetAPI star;
    private final List<SectorEntityToken> inSystem = new ArrayList<>();
    private FakeChartContentEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadShipped();
        sector = mock(SectorAPI.class);
        when(sector.getFaction(anyString())).thenReturn(mock(FactionAPI.class));
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.isStar()).thenReturn(true);
        when(star.getRadius()).thenReturn(500f);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(system.getPlanets()).thenReturn(List.of(star));
        when(system.getCenter()).thenReturn(star);
        when(system.getStar()).thenReturn(star);
        when(system.createToken(anyFloat(), anyFloat())).thenAnswer(invocation -> {
            SectorEntityToken token = mock(SectorEntityToken.class);
            when(token.getLocation()).thenReturn(new Vector2f(invocation.<Float>getArgument(0), invocation.<Float>getArgument(1)));
            return token;
        });
        doAnswer(invocation -> inSystem.add(invocation.getArgument(0))).when(system).addEntity(any());
        doAnswer(invocation -> inSystem.remove(invocation.<SectorEntityToken>getArgument(0))).when(system).removeEntity(any());
        engine = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f, 0f, -5000f, 6500f, 0f));
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        ChartTiers.resetForTests();
        ChartPois.resetForTests();
        ChartFactions.resetForTests();
    }

    private ChartContent generate(long seed, int tier) {
        return ChartContentGenerator.generate(sector, system, RUN_ID, seed, tier, WORMHOLE);
    }

    @Test
    void placesTheTiersLootAndFleetsAndRecordsTheObjectiveTargets() {
        for (int world = 0; world < 6; world++) {
            PlanetAPI planet = mock(PlanetAPI.class);
            engine.offeringAt(planet, -2500f + world * 1000f, 2500f);
            engine.planetCategories.put(planet, world % 2 == 0 ? "cat_hab3" : "cat_barren");
        }
        ChartContent content = generate(42L, 10);
        ChartContentPlan plan = ChartContentPlanner.plan(42L, ChartTiers.forTier(10), ChartPois.all(), ChartFactions.all(), id -> true);

        assertEquals(10, content.tier());
        assertEquals(plan.pois().size(), content.pois());
        assertEquals(plan.fleets().size(), content.fleets());
        assertEquals(plan.faction().factionId(), content.factionId());
        assertEquals(plan.objective(), content.objective());
        assertFalse(content.targetIds().isEmpty());
        assertEquals(engine.fleets.size(), inSystem.size());
        assertNotNull(content.bossFleetId());
        for (String targetId : content.targetIds()) {
            assertTrue(targetId.startsWith("fleet-"), targetId);
        }
    }

    @Test
    void theSameSeedAndTierGenerateTheSameContent() {
        ChartContent first = generate(7L, 9);
        List<String> firstTypes = List.copyOf(engine.salvageTypes);
        List<Float> firstPower = engine.fleetParams.stream().map(params -> params.combatPts).toList();
        List<String> firstSeeds = engine.fleets.stream().map(fleet -> ChartIds.fleetSeed(fleet.getMemoryWithoutUpdate())).toList();
        engine = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f, 0f, -5000f));

        ChartContent second = generate(7L, 9);

        assertEquals(first, second);
        assertEquals(firstTypes, engine.salvageTypes);
        assertEquals(firstPower, engine.fleetParams.stream().map(params -> params.combatPts).toList());
        assertEquals(firstSeeds, engine.fleets.stream().map(fleet -> ChartIds.fleetSeed(fleet.getMemoryWithoutUpdate())).toList());
    }

    @Test
    void lootGetsItsSalvageSpecOverrideAndRunTags() {
        engine = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f));
        generate(3L, 12);

        assertFalse(engine.salvageEntities.isEmpty());
        for (int i = 0; i < engine.salvageEntities.size(); i++) {
            SectorEntityToken entity = engine.salvageEntities.get(i);
            assertTrue(entity.hasTag(ChartIds.ENTITY_TAG));
            assertTrue(entity.hasTag(ChartIds.runTag(RUN_ID)));
            assertTrue(entity.hasTag(ChartIds.CONTENT_TAG));
            if (!"wreck".equals(engine.salvageTypes.get(i))) {
                assertNotNull(entity.getMemoryWithoutUpdate().getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE));
            }
            verify(entity, never()).setName(anyString());
        }
    }

    @Test
    void fleetsAreHostileStayPutNeverDespawnAndCarryTheirTierAndRun() {
        generate(3L, 6);

        assertFalse(engine.fleets.isEmpty());
        for (CampaignFleetAPI fleet : engine.fleets) {
            for (String flag : ChartContentGenerator.HOSTILITY_FLAGS) {
                assertTrue(fleet.getMemoryWithoutUpdate().getBoolean(flag), flag);
            }
            assertEquals(6, fleet.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY));
            assertNotNull(ChartIds.fleetSeed(fleet.getMemoryWithoutUpdate()));
            assertTrue(fleet.getMemoryWithoutUpdate().getBoolean(ChartIds.fleetRunKey(RUN_ID)));
            assertTrue(fleet.hasTag(ChartIds.runTag(RUN_ID)));
            assertTrue(fleet.hasTag(ChartIds.ENTITY_TAG));
            verify(fleet).setNoAutoDespawn(true);
            verify(fleet).setTransponderOn(true);
            verify(fleet).removeAbility(Abilities.GO_DARK);
            verify(fleet).removeAbility(Abilities.TRANSVERSE_JUMP);
            List<FakeChartContentEngine.Assignment> given = engine.assignments.get(fleet);
            assertFalse(given.isEmpty());
            assertEquals(FleetAssignment.PATROL_SYSTEM, given.get(given.size() - 1).assignment());
            assertTrue(given.stream().noneMatch(assignment -> assignment.target() == null));
        }
    }

    @Test
    void guardsOrbitTheirLootAggressivelyAndTheBossHoldsItsTarget() {
        ChartContent content = generate(11L, 8);

        int guardsChecked = 0;
        for (CampaignFleetAPI fleet : engine.fleets) {
            List<FakeChartContentEngine.Assignment> given = engine.assignments.get(fleet);
            boolean isPatrol = given.size() == 1;
            if (!isPatrol) {
                assertEquals(FleetAssignment.ORBIT_AGGRESSIVE, given.get(0).assignment());
                guardsChecked++;
            }
        }
        assertTrue(guardsChecked >= 1);
        assertTrue(engine.fleets.stream().anyMatch(fleet -> fleet.getId().equals(content.bossFleetId())));
    }

    @Test
    void aSiteThatNeedsAPlanetCategoryPrefersAMatchingWorld() throws Exception {
        ChartPois.register(new JSONArray().put(new JSONObject().put("id", "mine").put("kind", "station").put("entity", "station_mining")
                .put("cost", "1").put("weight", "1").put("planet_categories", "cat_lava")));
        PlanetAPI lava = mock(PlanetAPI.class);
        PlanetAPI frozen = mock(PlanetAPI.class);
        engine = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 5000f, 0f)
                .offeringAt(lava, 3000f, 3000f).offeringAt(frozen, -3000f, -3000f));
        engine.planetCategories.put(lava, "cat_lava");
        engine.planetCategories.put(frozen, "cat_frozen");

        int runs = 0;
        for (long seed = 1; seed <= 10; seed++) {
            engine.salvageEntities.clear();
            generate(seed, 6);
            if (engine.salvageEntities.isEmpty()) {
                continue;
            }
            runs++;
            assertEquals(new Vector2f(3000f, 3000f), engine.salvageEntities.get(0).getLocation());
        }

        assertTrue(runs > 0);
    }

    @Test
    void theBossHoldsAtTheLayoutsFeatureWhenThereIsOne() {
        SectorEntityToken feature = mock(SectorEntityToken.class);
        when(feature.getLocation()).thenReturn(new Vector2f(9000f, 0f));

        ChartContent content = ChartContentGenerator.generate(sector, system, RUN_ID, 11L, 8, WORMHOLE, ChartGenContext.none(), feature);
        CampaignFleetAPI boss = engine.fleets.stream().filter(fleet -> fleet.getId().equals(content.bossFleetId())).findFirst().orElseThrow();

        assertEquals(FleetAssignment.ORBIT_AGGRESSIVE, engine.assignments.get(boss).get(0).assignment());
        assertSame(feature, engine.assignments.get(boss).get(0).target());
    }

    private void useStationBosses() {
        ChartBosses.useForTests(List.of(new ChartBosses.Definition(ChartBossType.STATION, 1f, 1, 15, 0.5f)),
                List.of(new ChartBosses.Station("station2_Standard", Set.of(), 1, 15, 1f)));
    }

    @Test
    void aStationBossIsTheBossAndItsGuardOrbitsItWithoutChasing() {
        useStationBosses();
        try {
            ChartContent content = generate(11L, 8);

            assertEquals(ChartBossType.STATION, content.bossType());
            assertEquals(List.of("station2_Standard"), engine.stationVariants);
            CampaignFleetAPI station = engine.stations.get(0);
            assertEquals(station.getId(), content.bossFleetId());
            assertTrue(inSystem.contains(station));
            verify(station).setAI(null);
            assertTrue(station.getMemoryWithoutUpdate().getBoolean(ChartIds.BOSS_MEMORY_KEY));
            assertTrue(station.getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_FIGHT_TO_THE_LAST));
            assertTrue(station.getMemoryWithoutUpdate().getBoolean(ChartIds.fleetRunKey(RUN_ID)));
            assertTrue(station.hasTag(ChartIds.runTag(RUN_ID)));
            if (content.objective() == ChartObjective.BOSS) {
                assertEquals(List.of(station.getId()), content.targetIds());
            }
            CampaignFleetAPI guard = engine.fleets.stream()
                    .filter(fleet -> engine.assignments.get(fleet).get(0).assignment() == FleetAssignment.ORBIT_PASSIVE).findFirst().orElseThrow();
            assertSame(station, engine.assignments.get(guard).get(0).target());
            assertFalse(guard.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT));
            assertTrue(guard.getMemoryWithoutUpdate().getBoolean(ChartIds.BOSS_MEMORY_KEY));
            assertEquals(ChartContentGenerator.GUARD_OFFSET, Vector2f.sub(guard.getLocation(), station.getLocation(), new Vector2f()).length(), 1f);
        } finally {
            ChartBosses.resetForTests();
        }
    }

    private ChartContent skirmish(SkirmishSide side) {
        ChartGenContext context = ChartGenContext.none().withAtlasNode(new AtlasRunNode(1L, "hegemony", List.of(), List.of(FactionSkirmish.ID), false,
                "tritachyon", side));
        return ChartContentGenerator.generate(sector, system, RUN_ID, 11L, 8, WORMHOLE, context, null, List.of(FactionSkirmish.ID));
    }

    private String factionOf(CampaignFleetAPI fleet) {
        return engine.fleetParams.get(engine.fleets.indexOf(fleet)).factionId;
    }

    @Test
    void aSkirmishSetsTheDefendersStationAndBothSidesAgainstEachOther() {
        useStationBosses();
        try {
            ChartContent content = skirmish(SkirmishSide.NEITHER);

            CampaignFleetAPI station = engine.stations.get(0);
            assertEquals(station.getId(), content.bossFleetId());
            assertTrue(station.getMemoryWithoutUpdate().getBoolean("$cfai_makeHostile_tritachyon"));
            assertEquals(List.of(), engine.dressedFactions);
            List<CampaignFleetAPI> attackers = engine.fleets.stream().filter(fleet -> factionOf(fleet).equals("tritachyon")).toList();
            List<CampaignFleetAPI> defenders = engine.fleets.stream().filter(fleet -> factionOf(fleet).equals("hegemony")).toList();
            assertFalse(attackers.isEmpty());
            for (CampaignFleetAPI attacker : attackers) {
                MemoryAPI memory = attacker.getMemoryWithoutUpdate();
                assertTrue(memory.getBoolean("$cfai_makeHostile_hegemony"));
                assertTrue(memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE));
                assertEquals(SkirmishSide.ATTACKER.key(), memory.getString(ChartIds.SKIRMISH_SIDE_MEMORY_KEY));
                FleetAssignment first = engine.assignments.get(attacker).get(0).assignment();
                if (memory.getBoolean(ChartIds.SKIRMISH_COMMANDER_MEMORY_KEY)) {
                    assertEquals(FleetAssignment.ORBIT_PASSIVE, first);
                } else {
                    assertEquals(FleetAssignment.ORBIT_AGGRESSIVE, first);
                    assertSame(station, engine.assignments.get(attacker).get(0).target());
                    assertEquals(FleetAssignment.PATROL_SYSTEM, engine.assignments.get(attacker).get(1).assignment());
                }
            }
            assertEquals(1, attackers.stream().filter(fleet -> fleet.getMemoryWithoutUpdate().getBoolean(ChartIds.SKIRMISH_COMMANDER_MEMORY_KEY)).count());
            for (CampaignFleetAPI defender : defenders) {
                assertTrue(defender.getMemoryWithoutUpdate().getBoolean("$cfai_makeHostile_tritachyon"));
            }
            assertTrue(defenders.stream().anyMatch(fleet -> engine.assignments.get(fleet).get(0).assignment() == FleetAssignment.ORBIT_AGGRESSIVE
                    && engine.assignments.get(fleet).get(0).target() == station));
            assertTrue(attackers.stream().allMatch(fleet -> Vector2f.sub(fleet.getLocation(), station.getLocation(), new Vector2f()).length()
                    > ChartContentGenerator.GUARD_OFFSET * 2f));
        } finally {
            ChartBosses.resetForTests();
        }
    }

    @Test
    void eachSideGetsItsOwnBossObjective() {
        useStationBosses();
        try {
            for (SkirmishSide side : List.of(SkirmishSide.ATTACKER, SkirmishSide.NEITHER)) {
                ChartContent content = skirmish(side);
                String stationId = engine.stations.get(engine.stations.size() - 1).getId();
                assertEquals(ChartObjective.BOSS, content.objective());
                assertEquals(List.of(stationId), content.targetIds());
                assertEquals(stationId, content.bossFleetId());
                assertNull(content.protectedFleetId());
            }

            ChartContent defending = skirmish(SkirmishSide.DEFENDER);
            CampaignFleetAPI station = engine.stations.get(engine.stations.size() - 1);
            CampaignFleetAPI commander = engine.fleets.stream()
                    .filter(fleet -> fleet.getMemoryWithoutUpdate().getBoolean(ChartIds.SKIRMISH_COMMANDER_MEMORY_KEY)).reduce((first, last) -> last)
                    .orElseThrow();
            assertEquals(ChartObjective.BOSS, defending.objective());
            assertEquals(List.of(commander.getId()), defending.targetIds());
            assertEquals(commander.getId(), defending.bossFleetId());
            assertEquals(ChartBossType.FLEET, defending.bossType());
            assertEquals(station.getId(), defending.protectedFleetId());
        } finally {
            ChartBosses.resetForTests();
        }
    }

    @Test
    void theAlliedSideIsFriendlyToThePlayerAndCarriesNoLoot() {
        useStationBosses();
        try {
            skirmish(SkirmishSide.DEFENDER);

            CampaignFleetAPI station = engine.stations.get(0);
            List<CampaignFleetAPI> allies = new ArrayList<>(engine.fleets.stream().filter(fleet -> factionOf(fleet).equals("hegemony")).toList());
            allies.add(station);
            for (CampaignFleetAPI ally : allies) {
                MemoryAPI memory = ally.getMemoryWithoutUpdate();
                assertFalse(memory.getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE));
                assertTrue(memory.getBoolean(ChartContentGenerator.MAKE_NON_HOSTILE));
                assertTrue(memory.getBoolean(ChartContentGenerator.NON_HOSTILE_TAKES_PRIORITY));
                assertFalse(memory.contains(ChartIds.START_DP_MEMORY_KEY));
                assertTrue(memory.getBoolean("$cfai_makeHostile_tritachyon"));
                verify(ally).setFaction("allies", true);
            }
            assertEquals(List.of("hegemony"), engine.dressedFactions);
            for (CampaignFleetAPI enemy : engine.fleets.stream().filter(fleet -> factionOf(fleet).equals("tritachyon")).toList()) {
                assertTrue(enemy.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_HOSTILE));
                assertFalse(enemy.getMemoryWithoutUpdate().getBoolean(ChartContentGenerator.MAKE_NON_HOSTILE));
                assertTrue(enemy.getMemoryWithoutUpdate().getBoolean("$cfai_makeHostile_hegemony"));
                assertTrue(enemy.getMemoryWithoutUpdate().getBoolean("$cfai_makeHostile_allies"));
                verify(enemy, never()).setFaction(anyString(), Mockito.anyBoolean());
            }
        } finally {
            ChartBosses.resetForTests();
        }
    }

    @Test
    void aStationThatCannotBeBuiltLeavesItsGuardAsTheBossFleet() {
        useStationBosses();
        engine.missingStations = true;
        try {
            ChartContent content = generate(11L, 8);

            assertEquals(ChartBossType.FLEET, content.bossType());
            CampaignFleetAPI boss = engine.fleets.stream().filter(fleet -> fleet.getId().equals(content.bossFleetId())).findFirst().orElseThrow();
            assertEquals(FleetAssignment.ORBIT_AGGRESSIVE, engine.assignments.get(boss).get(0).assignment());
            assertTrue(boss.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT));
            float fullBoss = ChartTier.fleetPoints(Math.min(ChartContentPlanner.BOSS_SIZE_CAP, ChartTiers.forTier(8).bossSize()));
            assertEquals(fullBoss, engine.fleetParams.get(engine.fleets.indexOf(boss)).combatPts, 0.001f);
        } finally {
            ChartBosses.resetForTests();
        }
    }

    @Test
    void fleetParametersFollowThePlanWithoutAnyMarketOrHyperspaceLocation() {
        PlannedFleet planned = new PlannedFleet(FleetRole.BOSS, 0, 250f, 0.9f, 3, 2, 2, 0);

        FleetParamsV3 params = ChartContentGenerator.fleetParams("hegemony", planned, new Random(5));

        assertEquals("hegemony", params.factionId);
        assertNull(params.source);
        assertNull(params.locInHyper);
        assertEquals(0.9f, params.qualityOverride);
        assertEquals(250f, params.combatPts);
        assertEquals(3, params.officerLevelBonus);
        assertEquals(2, params.officerNumberBonus);
        assertNull(params.averageSMods);
        assertEquals(Boolean.TRUE, params.ignoreMarketFleetSizeMult);
        assertNotNull(params.random);
    }

    @Test
    void lootIsKeptAwayFromTheWormholeAndFromStarsAndCoronas() {
        List<Hazard> hazards = List.of(new Hazard(new Vector2f(), 1100f), new Hazard(new Vector2f(3000f, 3000f), 400f));

        assertFalse(ChartContentGenerator.isClear(new ChartSpot(null, new Vector2f(6500f, 0f), null, 1f), hazards, WORMHOLE, 2000f));
        assertFalse(ChartContentGenerator.isClear(new ChartSpot(null, new Vector2f(800f, 0f), null, 1f), hazards, WORMHOLE, 2000f));
        assertFalse(ChartContentGenerator.isClear(new ChartSpot(null, new Vector2f(3100f, 3000f), null, 1f), hazards, WORMHOLE, 2000f));
        assertTrue(ChartContentGenerator.isClear(new ChartSpot(null, new Vector2f(-4000f, 0f), null, 1f), hazards, WORMHOLE, 2000f));
        assertFalse(ChartContentGenerator.isClear(new ChartSpot(null, null, null, 1f), hazards, WORMHOLE, 2000f));
    }

    @Test
    void orbitsThatSweepPastTheWormholeAreRejectedEvenFromTheFarSide() {
        SectorEntityToken centre = mock(SectorEntityToken.class);
        when(centre.getLocation()).thenReturn(new Vector2f());
        ChartSpot sameRing = new ChartSpot(null, new Vector2f(-6000f, 0f), centre, 1f);
        ChartSpot innerRing = new ChartSpot(null, new Vector2f(-2500f, 0f), centre, 1f);

        assertFalse(ChartContentGenerator.isClear(sameRing, List.of(), WORMHOLE, 2000f));
        assertTrue(ChartContentGenerator.isClear(innerRing, List.of(), WORMHOLE, 2000f));
    }

    @Test
    void starsAreHazardsSoNoLootLandsInsideThem() {
        List<Hazard> hazards = ChartContentGenerator.hazards(system);

        assertEquals(1, hazards.size());
        assertEquals(500f + ChartContentGenerator.STAR_CLEARANCE, hazards.get(0).radius());
    }

    @Test
    void withNoClearSpotsNothingIsPlacedAndTheObjectiveFallsBackToFleets() {
        engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing().offering(6000f, 100f, 0f, 0f));

        ChartContent content = generate(42L, 10);

        assertEquals(0, content.pois());
        assertTrue(engine.salvageEntities.isEmpty());
        assertTrue(content.objective() == ChartObjective.CLEAR || content.objective() == ChartObjective.BOSS);
        for (CampaignFleetAPI fleet : engine.fleets) {
            Vector2f at = fleet.getLocation();
            assertTrue(Vector2f.sub(at, WORMHOLE, new Vector2f()).length() >= ChartContentGenerator.FLEET_WORMHOLE_CLEARANCE, at.toString());
        }
    }

    @Test
    void aFailingFleetFactoryLeavesLootButNoFleetsAndNoFleetObjective() {
        engine.failFleets = true;

        ChartContent content = generate(42L, 10);

        assertEquals(0, content.fleets());
        assertTrue(inSystem.isEmpty());
        assertNull(content.objective());
        verify(system, never()).addEntity(any(CampaignFleetAPI.class));
    }

    @Test
    void emptyFleetsAreNeverAddedToTheSystem() {
        engine.emptyFleets = true;

        ChartContent content = generate(42L, 4);

        assertEquals(0, content.fleets());
        assertTrue(inSystem.isEmpty());
    }

    @Test
    void anObjectiveThatCouldNotBePlacedFallsBackToOneThatCan() {
        assertEquals(ChartObjective.BOSS, ChartContentGenerator.settleObjective(ChartObjective.BOSS, true, true));
        assertEquals(ChartObjective.CLEAR, ChartContentGenerator.settleObjective(ChartObjective.BOSS, true, false));
        assertEquals(ChartObjective.BOSS, ChartContentGenerator.settleObjective(ChartObjective.CLEAR, false, true));
        assertNull(ChartContentGenerator.settleObjective(ChartObjective.BOSS, false, false));
        assertNull(ChartContentGenerator.settleObjective(null, false, false));
    }

    @Test
    void theBossIsRenamedAndMadeAggressive() {
        CampaignFleetAPI boss = engine.tagged(mock(CampaignFleetAPI.class));
        MemoryAPI bossMemory = FakeChartContentEngine.memory();
        when(boss.getMemoryWithoutUpdate()).thenReturn(bossMemory);
        when(boss.hasAbility(anyString())).thenReturn(false);

        ChartContentGenerator.prepareFleet(boss, RUN_ID, 9, FleetRole.BOSS, 12, 77L);

        verify(boss).setName(anyString());
        verify(boss, never()).removeAbility(anyString());
        assertTrue(boss.getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_FIGHT_TO_THE_LAST));
        assertEquals(9, boss.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY));
        assertEquals(12, boss.getMemoryWithoutUpdate().get(ChartIds.STRENGTH_BONUS_MEMORY_KEY));
    }

    @Test
    void aFleetWithinItsSizeCapGetsNoStrengthBonus() {
        CampaignFleetAPI patrol = engine.tagged(mock(CampaignFleetAPI.class));
        MemoryAPI patrolMemory = FakeChartContentEngine.memory();
        when(patrol.getMemoryWithoutUpdate()).thenReturn(patrolMemory);

        ChartContentGenerator.prepareFleet(patrol, RUN_ID, 9, FleetRole.PATROL, 0, 78L);

        assertFalse(patrol.getMemoryWithoutUpdate().contains(ChartIds.STRENGTH_BONUS_MEMORY_KEY));
    }

    private ChartGenContext certainColonyItem() throws Exception {
        ChartColonyItems.register(new JSONArray().put(new JSONObject().put("id", "ceiling").put("value", "0.9999999"))
                .put(new JSONObject().put("id", "synchrotron").put("value", "10")).put(new JSONObject().put("id", "mantle_bore").put("value", "10")),
                itemId -> true);
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_COLONY_ITEM_PERCENT, 100000f)));
    }

    @Test
    void aRolledColonyItemGoesToTheBossOnceAndTheSameSeedPicksTheSameItem() throws Exception {
        try {
            ChartGenContext context = certainColonyItem();

            ChartContent content = ChartContentGenerator.generate(sector, system, RUN_ID, 21L, 12, WORMHOLE, context);

            assertNotNull(content.bossFleetId());
            assertEquals(1, engine.colonyItems.size());
            assertEquals(content.bossFleetId(), engine.colonyItems.get(0).holder().getId());
            String firstItem = engine.colonyItems.get(0).itemId();
            engine = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f, 0f, -5000f, 6500f, 0f));
            ChartContentGenerator.generate(sector, system, RUN_ID, 21L, 12, WORMHOLE, context);
            assertEquals(firstItem, engine.colonyItems.get(0).itemId());
        } finally {
            ChartColonyItems.clear();
        }
    }

    @Test
    void noBonusGivesAColonyItemBelowTheFirstTierWithAChance() throws Exception {
        try {
            ChartGenContext context = certainColonyItem();

            ChartContentGenerator.generate(sector, system, RUN_ID, 21L, ChartTiers.firstColonyItemTier() - 1, WORMHOLE, context);

            assertEquals(List.of(), engine.colonyItems);
        } finally {
            ChartColonyItems.clear();
        }
    }

    @Test
    void placedSitesGetTheChartsVanillaLootBonus() throws Exception {
        ChartLoot.register(new JSONArray().put(new JSONObject().put("id", "weapons2").put("drop_type", "weapon").put("group", "weapons2")),
                group -> true);
        try {
            DropData specWeapons = new DropData();
            specWeapons.group = "weapons2";
            specWeapons.chances = 10;
            for (ChartPoi poi : ChartPois.all()) {
                engine.siteDropsBySpec.put(poi.specForSalvage(), new ChartVanillaLoot.SiteDrops(List.of(specWeapons), List.of()));
            }
            ChartGenContext moreWeapons = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_WEAPON_PERCENT, 50f)));

            ChartContentGenerator.generate(sector, system, RUN_ID, 3L, 12, WORMHOLE, moreWeapons);

            int sites = 0;
            for (int i = 0; i < engine.salvageEntities.size(); i++) {
                if (!"wreck".equals(engine.salvageTypes.get(i))) {
                    verify(engine.salvageEntities.get(i)).addDropRandom(Mockito.<DropData>argThat(drop -> "weapons2".equals(drop.group)
                            && drop.chances == 5));
                    sites++;
                }
            }
            assertTrue(sites > 0);
        } finally {
            ChartLoot.clear();
        }
    }

    @Test
    void aPlannedAnomalyIsGeneratedWithThePlacedSitesAndRecorded() {
        ChartAnomaliesTest.PlacesNothing expansion = new ChartAnomaliesTest.PlacesNothing();
        ChartAnomalies.useForTests(List.of(expansion), List.of(ChartAnomaliesTest.definition(SpatialBreach.ID)));
        try {
            ChartGenContext breach = ChartGenContext.none().withAnomalies(List.of(SpatialBreach.ID));

            ChartContent content = ChartContentGenerator.generate(sector, system, RUN_ID, 42L, 10, WORMHOLE, breach);

            assertEquals(List.of(SpatialBreach.ID), content.anomalies());
            assertEquals(1, expansion.sites.size());
            AnomalyContentSite site = expansion.sites.get(0);
            assertEquals(RUN_ID, site.runId());
            assertEquals(10, site.tier());
            assertEquals(content.pois(), site.pois().size());
            assertFalse(site.isClear(new Vector2f()), "the star is a hazard");
            assertFalse(site.isClear(new Vector2f(WORMHOLE)), "the wormhole needs clearance");
            assertTrue(site.isClear(new Vector2f(-4000f, 3000f)));
            assertEquals(site.random(5L).nextLong(), site.random(5L).nextLong());
        } finally {
            ChartAnomalies.resetForTests();
        }
    }

    @Test
    void aFailingAnomalyIsLeftOutAndTheRestOfTheContentStays() {
        ChartAnomaly failing = new ChartAnomaly() {
            @Override
            public String id() {
                return SpatialBreach.ID;
            }

            @Override
            public void generate(AnomalyContentSite site) {
                throw new IllegalStateException("test failure");
            }
        };
        ChartAnomalies.useForTests(List.of(failing), List.of(ChartAnomaliesTest.definition(SpatialBreach.ID)));
        try {
            ChartGenContext breach = ChartGenContext.none().withAnomalies(List.of(SpatialBreach.ID));

            ChartContent content = ChartContentGenerator.generate(sector, system, RUN_ID, 42L, 10, WORMHOLE, breach);

            assertEquals(List.of(), content.anomalies());
            assertTrue(content.fleets() > 0);
            assertTrue(content.pois() > 0);
        } finally {
            ChartAnomalies.resetForTests();
        }
    }


    @Test
    void aNeutronStarAlwaysHasAResearchStationOrbitingItsStar() {
        ChartAnomalies.useForTests(List.of(ChartAnomaliesTest.definition("neutron_star")));
        try {
            ChartGenContext neutronStar = ChartGenContext.none().withAnomalies(List.of("neutron_star"));

            ChartContent content = ChartContentGenerator.generate(sector, system, RUN_ID, 42L, 3, WORMHOLE, neutronStar);

            assertEquals(List.of("neutron_star"), content.anomalies());
            assertEquals(1, engine.salvageTypes.stream().filter("station_research"::equals).count());
            SectorEntityToken station = engine.salvageEntities.get(engine.salvageTypes.indexOf("station_research"));
            verify(station).setCircularOrbit(eq(star), anyFloat(), anyFloat(), anyFloat());
            assertTrue(station.hasTag(ChartIds.runTag(RUN_ID)));
        } finally {
            ChartAnomalies.resetForTests();
        }
    }

    @Test
    void aFailingAnomalyTakesTheSitesItPlacedWithIt() {
        ChartAnomaly placesThenFails = new ChartAnomaly() {
            @Override
            public String id() {
                return SpatialBreach.ID;
            }

            @Override
            public void generate(AnomalyContentSite site) {
                site.addSite(ChartSites.pick(ChartPoi.Kind.CACHE, site.tier(), new Random(1L)), new ChartSites.Orbit(star, 0f, 5000f, 100f),
                        new Random(2L));
                throw new IllegalStateException("test failure");
            }
        };
        ChartAnomalies.useForTests(List.of(placesThenFails), List.of(ChartAnomaliesTest.definition(SpatialBreach.ID)));
        try {
            ChartGenContext breach = ChartGenContext.none().withAnomalies(List.of(SpatialBreach.ID));

            ChartContent content = ChartContentGenerator.generate(sector, system, RUN_ID, 42L, 10, WORMHOLE, breach);

            assertEquals(List.of(), content.anomalies());
            verify(system).removeEntity(engine.salvageEntities.get(engine.salvageEntities.size() - 1));
        } finally {
            ChartAnomalies.resetForTests();
        }
    }
}
