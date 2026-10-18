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
import exiledsector.charts.ChartContentGenerator.Hazard;
import exiledsector.charts.ChartContentPlan.FleetRole;
import exiledsector.charts.ChartContentPlan.PlannedFleet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

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
    private StarSystemAPI pocket;
    private PlanetAPI star;
    private final List<SectorEntityToken> inPocket = new ArrayList<>();
    private FakeChartContentEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        ChartContentDataTest.loadShippedTables();
        sector = mock(SectorAPI.class);
        when(sector.getFaction(anyString())).thenReturn(mock(FactionAPI.class));
        pocket = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.isStar()).thenReturn(true);
        when(star.getRadius()).thenReturn(500f);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(pocket.getPlanets()).thenReturn(List.of(star));
        when(pocket.getCenter()).thenReturn(star);
        when(pocket.getStar()).thenReturn(star);
        when(pocket.createToken(anyFloat(), anyFloat())).thenAnswer(invocation -> {
            SectorEntityToken token = mock(SectorEntityToken.class);
            when(token.getLocation()).thenReturn(new Vector2f(invocation.<Float>getArgument(0), invocation.<Float>getArgument(1)));
            return token;
        });
        doAnswer(invocation -> inPocket.add(invocation.getArgument(0))).when(pocket).addEntity(any());
        doAnswer(invocation -> inPocket.remove(invocation.<SectorEntityToken>getArgument(0))).when(pocket).removeEntity(any());
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
        return ChartContentGenerator.generate(sector, pocket, RUN_ID, seed, tier, WORMHOLE);
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
        assertEquals(engine.fleets.size(), inPocket.size());
        assertNotNull(content.bossFleetId());
        for (String targetId : content.targetIds()) {
            assertTrue(targetId.startsWith(content.objective() == ChartObjective.VAULT ? "poi-" : "fleet-"), targetId);
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
    void lootGetsItsSalvageSpecOverrideAndRunTagsAndTheVaultItsName() {
        boolean sawVault = false;
        for (long seed = 0; seed < 40 && !sawVault; seed++) {
            engine = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 0f, 5000f));
            ChartContent content = generate(seed, 12);
            for (int i = 0; i < engine.salvageEntities.size(); i++) {
                SectorEntityToken entity = engine.salvageEntities.get(i);
                assertTrue(entity.hasTag(ChartIds.ENTITY_TAG));
                assertTrue(entity.hasTag(ChartIds.runTag(RUN_ID)));
                assertTrue(entity.hasTag(ChartIds.CONTENT_TAG));
                if (!"wreck".equals(engine.salvageTypes.get(i))) {
                    assertNotNull(entity.getMemoryWithoutUpdate().getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE));
                }
                if (entity.hasTag(ChartIds.VAULT_TAG)) {
                    sawVault = true;
                    assertEquals("exiledSector_chart_vault", entity.getMemoryWithoutUpdate().getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE));
                    assertEquals(entity.getId(), content.vaultEntityId());
                    verify(entity).setName(anyString());
                }
            }
        }
        assertTrue(sawVault);
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
        assertEquals(2, params.averageSMods);
        assertEquals(Boolean.TRUE, params.ignoreMarketFleetSizeMult);
        assertNotNull(params.random);
        assertNull(ChartContentGenerator.fleetParams("pirates", new PlannedFleet(FleetRole.PATROL, -1, 50f, 0.3f, 0, 0, 0),
                new Random(5)).averageSMods);
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
        List<Hazard> hazards = ChartContentGenerator.hazards(pocket);

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
        assertNull(content.vaultEntityId());
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
        assertTrue(inPocket.isEmpty());
        assertTrue(content.objective() == null || content.objective() == ChartObjective.VAULT);
        verify(pocket, never()).addEntity(any(CampaignFleetAPI.class));
    }

    @Test
    void emptyFleetsAreNeverAddedToThePocket() {
        engine.emptyFleets = true;

        ChartContent content = generate(42L, 4);

        assertEquals(0, content.fleets());
        assertTrue(inPocket.isEmpty());
    }

    @Test
    void anObjectiveThatCouldNotBePlacedFallsBackToOneThatCan() {
        assertEquals(ChartObjective.VAULT, ChartContentGenerator.settleObjective(ChartObjective.VAULT, true, true, true));
        assertEquals(ChartObjective.CLEAR, ChartContentGenerator.settleObjective(ChartObjective.VAULT, true, true, false));
        assertEquals(ChartObjective.BOSS, ChartContentGenerator.settleObjective(ChartObjective.CLEAR, false, true, false));
        assertEquals(ChartObjective.VAULT, ChartContentGenerator.settleObjective(ChartObjective.BOSS, false, false, true));
        assertNull(ChartContentGenerator.settleObjective(ChartObjective.BOSS, false, false, false));
        assertNull(ChartContentGenerator.settleObjective(null, false, false, false));
    }

    @Test
    void theBossIsRenamedAndMadeAggressive() {
        CampaignFleetAPI boss = engine.tagged(mock(CampaignFleetAPI.class));
        MemoryAPI bossMemory = FakeChartContentEngine.memory();
        when(boss.getMemoryWithoutUpdate()).thenReturn(bossMemory);
        when(boss.hasAbility(anyString())).thenReturn(false);

        ChartContentGenerator.prepareFleet(boss, RUN_ID, 9, FleetRole.BOSS, 77L);

        verify(boss).setName(anyString());
        verify(boss, never()).removeAbility(anyString());
        assertTrue(boss.getMemoryWithoutUpdate().getBoolean(MemFlags.FLEET_FIGHT_TO_THE_LAST));
        assertEquals(9, boss.getMemoryWithoutUpdate().get(ChartIds.TIER_MEMORY_KEY));
    }
}
