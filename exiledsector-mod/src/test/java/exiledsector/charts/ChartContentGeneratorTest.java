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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
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
        ChartContentDataTest.loadShippedTables();
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
    void fleetParametersFollowThePlanWithoutAnyMarketOrHyperspaceLocation() {
        PlannedFleet planned = new PlannedFleet(FleetRole.BOSS, 0, 250f, 0.9f, 3, 2, 2);

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
}
