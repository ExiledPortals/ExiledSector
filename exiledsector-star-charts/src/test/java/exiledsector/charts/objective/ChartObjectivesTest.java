package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.MutableValue;
import exiledsector.MockSector;
import exiledsector.charts.content.ChartBossType;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.run.ChartContentListener;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.loot.ChartDropRules;
import exiledsector.items.ItemData;
import exiledsector.items.StarChartFixtures;
import exiledsector.loot.Loot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartObjectivesTest {

    private static final String RUN_ID = "objectiveRun";

    private MockSector mockSector;
    private SectorAPI sector;
    private StarSystemAPI system;
    private MutableValue credits;
    private final List<CampaignFleetAPI> systemFleets = new ArrayList<>();
    private final FakeChartContentEngine tags = new FakeChartContentEngine();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sector = mockSector.sector;
        system = mock(StarSystemAPI.class);
        when(sector.getStarSystem(ChartIds.systemId(RUN_ID))).thenReturn(system);
        when(system.getFleets()).thenAnswer(invocation -> new ArrayList<>(systemFleets));
        credits = mock(MutableValue.class);
        when(mockSector.cargo().getCredits()).thenReturn(credits);
        when(sector.getIntelManager()).thenReturn(mock(IntelManagerAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private ChartRun openRun() {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(7, "hegemony", "boss", 4, 6));
        run.recordArrival();
        ChartRuns.store(run);
        return run;
    }

    private CampaignFleetAPI fleet(String id, boolean ofThisRun) {
        CampaignFleetAPI fleet = tags.tagged(mock(CampaignFleetAPI.class));
        when(fleet.getId()).thenReturn(id);
        if (ofThisRun) {
            fleet.addTag(ChartIds.runTag(RUN_ID));
        }
        return fleet;
    }

    @Test
    void onlyDestroyingTheBossCompletesTheChartOnceAndGrantsNoCredits() {
        ChartRun run = openRun();

        assertFalse(ChartObjectives.fleetGone(sector, fleet("guard", true), FleetDespawnReason.DESTROYED_BY_BATTLE, null));
        assertFalse(run.isObjectiveComplete());
        verify(credits, never()).add(anyFloat());

        assertTrue(ChartObjectives.fleetGone(sector, fleet("boss", true), FleetDespawnReason.NO_MEMBERS, null));
        assertTrue(run.isObjectiveComplete());

        assertFalse(ChartObjectives.fleetGone(sector, fleet("boss", true), FleetDespawnReason.DESTROYED_BY_BATTLE, null));
        verify(credits, never()).add(anyFloat());
    }

    @Test
    void despawnsThatAreNotDefeatsOrBelongToOtherRunsAreIgnored() {
        ChartRun run = openRun();

        assertFalse(ChartObjectives.fleetGone(sector, fleet("boss", true), FleetDespawnReason.PLAYER_FAR_AWAY, null));
        assertFalse(ChartObjectives.fleetGone(sector, fleet("boss", false), FleetDespawnReason.DESTROYED_BY_BATTLE, null));
        assertFalse(ChartObjectives.fleetGone(sector, fleet("patrol", true), FleetDespawnReason.DESTROYED_BY_BATTLE, null));
        assertFalse(run.isObjectiveComplete());
    }

    @Test
    void nothingCountsOnceTheRunIsClosing() {
        ChartRun run = openRun();
        run.beginClosing();

        assertFalse(ChartObjectives.fleetGone(sector, fleet("boss", true), FleetDespawnReason.DESTROYED_BY_BATTLE, null));
        assertFalse(run.isObjectiveComplete());
        verify(credits, never()).add(anyFloat());
    }

    @Test
    void killingTheBossCompletesTheBossObjectiveEvenWhenAnotherFleetDidIt() {
        ChartRun run = openRun();

        assertTrue(ChartObjectives.fleetGone(sector, fleet("boss", true), FleetDespawnReason.DESTROYED_BY_BATTLE, null));

        assertTrue(run.isObjectiveComplete());
    }

    @Test
    void theFleetListenerForwardsDefeatsAndSurvivesErrors() {
        ChartRun run = openRun();
        ChartContentListener listener = new ChartContentListener();
        CampaignFleetAPI broken = mock(CampaignFleetAPI.class);
        when(broken.hasTag(Mockito.anyString())).thenThrow(new IllegalStateException("odd fleet"));

        listener.reportFleetDespawnedToListener(broken, FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        listener.reportFleetDespawnedToListener(fleet("boss", true), FleetDespawnReason.DESTROYED_BY_BATTLE, null);

        assertTrue(run.isObjectiveComplete());
    }

    @Test
    void reconcilingCatchesABossThatVanishedWithoutAnEvent() {
        ChartRun run = openRun();
        CampaignFleetAPI boss = fleet("boss", true);
        systemFleets.add(boss);
        systemFleets.add(fleet("guard", true));

        assertFalse(ChartObjectives.reconcile(sector));
        assertFalse(run.isObjectiveComplete());

        when(boss.isDespawning()).thenReturn(true);
        assertTrue(ChartObjectives.reconcile(sector));
        assertTrue(run.isObjectiveComplete());
        assertFalse(ChartObjectives.reconcile(sector));
    }

    private CampaignFleetAPI bossFleet(String... memberIds) {
        CampaignFleetAPI boss = fleet("boss", true);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        List<FleetMemberAPI> members = new ArrayList<>();
        for (String memberId : memberIds) {
            FleetMemberAPI member = mock(FleetMemberAPI.class);
            when(member.getId()).thenReturn(memberId);
            members.add(member);
        }
        when(fleetData.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(members));
        when(boss.getFleetData()).thenReturn(fleetData);
        when(boss.getFlagship()).thenAnswer(invocation -> members.isEmpty() ? null : members.get(0));
        return boss;
    }

    private ChartRun openBossRun(String flagshipId) {
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(7, "hegemony", "boss", ChartBossType.FLEET, 4, 6,
                List.of(), flagshipId));
        run.recordArrival();
        ChartRuns.store(run);
        return run;
    }

    @Test
    void losingTheFlagshipCompletesTheBossObjectiveWhileTheRestOfItsFleetLivesOn() {
        ChartRun run = openBossRun("flagship");
        systemFleets.add(bossFleet("flagship", "escort"));

        assertFalse(ChartObjectives.reconcile(sector));
        assertFalse(run.isObjectiveComplete());

        systemFleets.set(0, bossFleet("escort"));
        assertTrue(ChartObjectives.reconcile(sector));
        assertTrue(run.isObjectiveComplete());
    }

    @Test
    void aRunWithoutARecordedFlagshipAdoptsTheBossFleetsCurrentOne() {
        ChartRun run = openBossRun(null);
        systemFleets.add(bossFleet("flagship", "escort"));

        assertFalse(ChartObjectives.reconcile(sector));
        assertEquals("flagship", run.bossFlagshipId());

        systemFleets.set(0, bossFleet("escort"));
        assertTrue(ChartObjectives.reconcile(sector));
        assertTrue(run.isObjectiveComplete());
    }

    @Test
    void theBossFlagshipFallingInBattleCompletesTheObjectiveWithoutTouchingTheCargo() {
        ChartRun run = openBossRun("flagship");
        CampaignFleetAPI boss = bossFleet("flagship", "escort");
        List<FleetMemberAPI> members = boss.getFleetData().getMembersListCopy();

        assertTrue(ChartObjectives.bossFellInBattle(sector, boss, List.of(members.get(1))).isEmpty());
        assertTrue(ChartObjectives.bossFellInBattle(sector, fleet("guard", true), List.of(members.get(0))).isEmpty());
        assertFalse(run.isObjectiveComplete());

        ChartObjectives.bossFellInBattle(sector, boss, members);

        assertTrue(run.isObjectiveComplete());
        verify(credits, never()).add(anyFloat());
        verify(mockSector.cargo(), never()).addSpecial(Mockito.any(), anyFloat());
        assertTrue(ChartObjectives.bossFellInBattle(sector, boss, members).isEmpty());
        assertFalse(ChartObjectives.reconcile(sector));
    }

    @Test
    void theBossFallingInBattleDropsItsGuaranteedStarChartOnceAtTheRunTierOrAbove() throws Exception {
        StarChartFixtures.registerShipped();
        ChartDropRules.register(new JSONArray().put(new JSONObject().put("source", "chart_boss").put("guaranteed", "1")
                .put("same_tier", "0").put("plus_1", "1")));
        try {
            openBossRun("flagship");
            CampaignFleetAPI boss = bossFleet("flagship", "escort");
            List<FleetMemberAPI> members = boss.getFleetData().getMembersListCopy();

            Loot loot = ChartObjectives.bossFellInBattle(sector, boss, members);

            List<ItemData> charts = loot.items().stream().filter(item -> item.toSpecialItem().getId().equals(ItemData.STAR_CHART_ITEM_ID)).toList();
            assertEquals(1, charts.size(), loot.toString());
            assertEquals(8, charts.get(0).preview().tier());
            assertTrue(ChartObjectives.bossFellInBattle(sector, boss, members).isEmpty());
        } finally {
            ChartDropRules.clear();
            StarChartFixtures.clear();
        }
    }

    @Test
    void aTwinnedBossDropsItsOwnGuaranteedStarChartOnceWhenItsFlagshipFallsEvenAfterTheObjectiveIsDone() throws Exception {
        StarChartFixtures.registerShipped();
        ChartDropRules.register(new JSONArray().put(new JSONObject().put("source", "chart_boss").put("guaranteed", "1")
                .put("same_tier", "0").put("plus_1", "1")));
        try {
            ChartRun run = openBossRun("flagship");
            CampaignFleetAPI boss = bossFleet("flagship");
            CampaignFleetAPI twin = fleet("twin", true);
            MemoryAPI twinMemory = FakeChartContentEngine.memory();
            when(twin.getMemoryWithoutUpdate()).thenReturn(twinMemory);
            twinMemory.set(ChartIds.fleetRunKey(RUN_ID), true);
            ChartFleetMarks.markTwinBoss(twin, "twinFlagship");
            FleetMemberAPI twinFlagship = mock(FleetMemberAPI.class);
            when(twinFlagship.getId()).thenReturn("twinFlagship");
            FleetMemberAPI twinEscort = mock(FleetMemberAPI.class);
            when(twinEscort.getId()).thenReturn("twinEscort");

            assertEquals(1, chartsIn(ChartObjectives.bossFellInBattle(sector, boss, boss.getFleetData().getMembersListCopy())));
            assertTrue(run.isObjectiveComplete());
            assertTrue(ChartObjectives.bossFellInBattle(sector, twin, List.of(twinEscort)).isEmpty());

            Loot twinLoot = ChartObjectives.bossFellInBattle(sector, twin, List.of(twinEscort, twinFlagship));

            assertEquals(1, chartsIn(twinLoot));
            assertEquals(8, twinLoot.items().get(0).preview().tier());
            assertTrue(ChartObjectives.bossFellInBattle(sector, twin, List.of(twinFlagship)).isEmpty());
        } finally {
            ChartDropRules.clear();
            StarChartFixtures.clear();
        }
    }

    private static int chartsIn(Loot loot) {
        return (int) loot.items().stream().filter(item -> item.toSpecialItem().getId().equals(ItemData.STAR_CHART_ITEM_ID)).count();
    }

    @Test
    void theFlagshipOnlyCountsForTheBossFleet() {
        ChartRun run = openBossRun("flagship");
        CampaignFleetAPI boss = bossFleet("flagship");
        CampaignFleetAPI guard = fleet("guard", true);
        systemFleets.addAll(List.of(boss, guard));

        assertFalse(ChartObjectives.reconcile(sector));
        assertFalse(run.isObjectiveComplete());
    }

    @Test
    void theIntelFollowsItsRunAndAsksToBeRemovedOnceTheRunIsGone() {
        ChartRun run = openRun();
        ChartObjectiveIntel intel = new ChartObjectiveIntel(RUN_ID);

        assertTrue(intel.belongsToRun(RUN_ID));
        assertFalse(intel.belongsToRun("other"));
        assertFalse(intel.belongsToRun(null));
        assertFalse(intel.shouldRemoveIntel());
        assertNull(intel.getMapLocation(null));
        assertNull(ChartObjectiveIntel.progressText(run));

        ChartRuns.delete(RUN_ID);
        assertTrue(intel.shouldRemoveIntel());
    }
}
