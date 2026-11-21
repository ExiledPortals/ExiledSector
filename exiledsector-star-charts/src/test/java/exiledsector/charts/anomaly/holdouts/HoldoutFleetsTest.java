package exiledsector.charts.anomaly.holdouts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import exiledsector.MockSector;
import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ContentForces;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoldoutFleetsTest {

    private static final String RUN_ID = "run1";
    private static final int TIER = 6;

    private MockSector mockSector;
    private FakeChartContentEngine engine;
    private StarSystemAPI system;
    private CampaignFleetAPI occupier;
    private CampaignFleetAPI boss;
    private CampaignFleetAIAPI bossAi;

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadShipped();
        mockSector = new MockSector();
        engine = FakeChartContentEngine.install(new FakeChartContentEngine().offering(-4000f, 0f, 5000f, 0f, 0f, -5000f));
        system = mock(StarSystemAPI.class);
        PlanetAPI star = mock(PlanetAPI.class);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(system.getCenter()).thenReturn(star);
        MemoryAPI occupierMemory = FakeChartContentEngine.memory();
        occupier = mock(CampaignFleetAPI.class);
        when(occupier.getMemoryWithoutUpdate()).thenReturn(occupierMemory);
        MemoryAPI bossMemory = FakeChartContentEngine.memory();
        boss = mock(CampaignFleetAPI.class);
        bossAi = mock(CampaignFleetAIAPI.class);
        when(boss.getMemoryWithoutUpdate()).thenReturn(bossMemory);
        when(boss.getAI()).thenReturn(bossAi);
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    private AnomalyContentSite site(String holdoutFactionId) {
        AtlasRunNode node = new AtlasRunNode(1L, "hegemony", List.of(), List.of(Holdouts.ANOMALY_ID));
        ChartGenContext context = ChartGenContext.none().withAtlasNode(holdoutFactionId == null ? node : node.withAnomalyData(Holdouts.ANOMALY_ID, holdoutFactionId));
        ContentForces forces = new ContentForces(Map.of(occupier, FleetRole.PATROL, boss, FleetRole.BOSS), boss, ChartBossType.FLEET);
        return new AnomalyContentSite(system, RUN_ID, TIER, "hegemony", context, List.of(), List.of(), new Vector2f(), spot -> true, Random::new,
                new HashSet<>(), forces, null);
    }

    private long fleetsOfType(String fleetType) {
        return engine.fleetParams.stream().filter(params -> fleetType.equals(params.fleetType)).count();
    }

    @Test
    void everyPlanHasThreeToFiveSmallFleetsAndOneOrTwoLargeOnes() {
        ChartTier tier = ChartAnomalyFleets.tier(TIER, ChartGenContext.none());
        Set<Integer> smallCounts = new HashSet<>();
        Set<Integer> largeCounts = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            List<HoldoutFleets.Planned> plan = HoldoutFleets.plan(tier, new Random(seed));
            smallCounts.add((int) plan.stream().filter(planned -> FleetTypes.PATROL_SMALL.equals(planned.fleetType())).count());
            largeCounts.add((int) plan.stream().filter(planned -> FleetTypes.PATROL_LARGE.equals(planned.fleetType())).count());
        }

        assertEquals(Set.of(3, 4, 5), smallCounts);
        assertEquals(Set.of(1, 2), largeCounts);
    }

    @Test
    void theHoldoutsSpawnAsTheirOwnFactionWithLargeFleetsBiggerThanSmallOnes() {
        Holdouts.INSTANCE.generate(site("pirates"));

        long small = fleetsOfType(FleetTypes.PATROL_SMALL);
        long large = fleetsOfType(FleetTypes.PATROL_LARGE);
        assertTrue(small >= 3 && small <= 5, "small fleets: " + small);
        assertTrue(large >= 1 && large <= 2, "large fleets: " + large);
        assertEquals(small + large, engine.fleets.size());
        assertTrue(engine.fleetParams.stream().allMatch(params -> "pirates".equals(params.factionId)));
        float smallPoints = engine.fleetParams.stream().filter(params -> FleetTypes.PATROL_SMALL.equals(params.fleetType)).findFirst().orElseThrow().combatPts;
        float largePoints = engine.fleetParams.stream().filter(params -> FleetTypes.PATROL_LARGE.equals(params.fleetType)).findFirst().orElseThrow().combatPts;
        assertTrue(largePoints > smallPoints, largePoints + " vs " + smallPoints);
    }

    @Test
    void theHoldoutsAndTheOccupiersAreSetAgainstEachOther() {
        Holdouts.INSTANCE.generate(site("pirates"));

        assertTrue(occupier.getMemoryWithoutUpdate().getBoolean(ChartAnomalyFleets.HOSTILE_TO_FACTION_PREFIX + "pirates"));
        for (CampaignFleetAPI holdout : engine.fleets) {
            assertTrue(holdout.getMemoryWithoutUpdate().getBoolean(ChartAnomalyFleets.HOSTILE_TO_FACTION_PREFIX + "hegemony"));
            assertEquals(FleetAssignment.PATROL_SYSTEM, engine.assignments.get(holdout).get(0).assignment());
        }
    }

    @Test
    void theBossStaysOutOfTheFightSoOnlyThePlayerCanFinishTheRun() {
        Holdouts.INSTANCE.generate(site("pirates"));

        assertEquals("pirates", boss.getMemoryWithoutUpdate().getString(MemFlags.FLEET_IGNORED_BY_FACTION));
        assertFalse(boss.getMemoryWithoutUpdate().getBoolean(ChartAnomalyFleets.HOSTILE_TO_FACTION_PREFIX + "pirates"));
        verify(bossAi, times(engine.fleets.size())).doNotAttack(any(), eq(HoldoutFleets.BOSS_TRUCE_DAYS));
        engine.fleets.forEach(holdout -> verify(bossAi).doNotAttack(holdout, HoldoutFleets.BOSS_TRUCE_DAYS));
    }

    @Test
    void noHoldoutsSpawnWithoutAHoldoutFactionOrWhenTheyAlreadyOwnTheSystem() {
        Holdouts.INSTANCE.generate(site(null));
        Holdouts.INSTANCE.generate(site("hegemony"));

        assertTrue(engine.fleetParams.isEmpty());
        assertFalse(occupier.getMemoryWithoutUpdate().getBoolean(ChartAnomalyFleets.HOSTILE_TO_FACTION_PREFIX + "pirates"));
    }
}
