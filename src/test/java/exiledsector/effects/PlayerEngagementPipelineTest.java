package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.effects.PlayerEngagementPipeline.Stage;
import exiledsector.effects.PlayerEngagementPipeline.Step;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.socketables.SocketLossListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerEngagementPipelineTest {

    private MockedStatic<Global> globalMock;
    private FleetDataAPI fleetData;
    private MutableStat salvageMult;

    @BeforeEach
    void setUp() {
        SectorAPI sector = mock(SectorAPI.class);
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        fleetData = mock(FleetDataAPI.class);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        MutableFleetStatsAPI fleetStats = mock(MutableFleetStatsAPI.class);
        DynamicStatsAPI fleetDynamic = mock(DynamicStatsAPI.class);
        when(playerFleet.getStats()).thenReturn(fleetStats);
        when(fleetStats.getDynamic()).thenReturn(fleetDynamic);
        salvageMult = mock(MutableStat.class);
        when(fleetDynamic.getStat(Stats.BATTLE_SALVAGE_MULT_FLEET)).thenReturn(salvageMult);

        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static FleetMemberAPI ship(String id, float deploymentPoints) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getDeploymentPointsCost()).thenReturn(deploymentPoints);
        return member;
    }

    private static EngagementResultAPI wonEngagement(List<FleetMemberAPI> playerLosses, List<FleetMemberAPI> enemyDestroyed,
                                                     List<FleetMemberAPI> enemyDisabled) {
        EngagementResultForFleetAPI player = mock(EngagementResultForFleetAPI.class);
        when(player.getDestroyed()).thenReturn(playerLosses);
        EngagementResultForFleetAPI enemy = mock(EngagementResultForFleetAPI.class);
        when(enemy.getDestroyed()).thenReturn(enemyDestroyed);
        when(enemy.getDisabled()).thenReturn(enemyDisabled);
        EngagementResultAPI result = mock(EngagementResultAPI.class);
        when(result.didPlayerWin()).thenReturn(true);
        when(result.getWinnerResult()).thenReturn(player);
        when(result.getLoserResult()).thenReturn(enemy);
        return result;
    }

    @Test
    void theStagesRunInTheirDeclaredOrder() {
        assertEquals(List.of(Stage.RECORD_LOSSES, Stage.SYNC_TREES, Stage.AWARD_XP, Stage.SETTLE_CREW, Stage.HOLD_LOOT, Stage.SALVAGE_BONUS),
                List.of(Stage.values()));
    }

    @Test
    void stepsRunInStageOrderWhateverOrderTheyWereGivenIn() {
        List<Stage> ranStages = new ArrayList<>();
        Map<Stage, Step> stepsByStage = new LinkedHashMap<>();
        List<Stage> reversed = new ArrayList<>(List.of(Stage.values()));
        Collections.reverse(reversed);
        reversed.forEach(stage -> stepsByStage.put(stage, engagement -> ranStages.add(stage)));

        new PlayerEngagementPipeline(stepsByStage).reportPlayerEngagement(wonEngagement(List.of(), List.of(), List.of()));

        assertEquals(List.of(Stage.values()), ranStages);
    }

    @Test
    void everyStepSharesOneSummaryOfTheEngagement() {
        List<PlayerEngagement> seen = new ArrayList<>();
        Map<Stage, Step> stepsByStage = new EnumMap<>(Stage.class);
        stepsByStage.put(Stage.AWARD_XP, seen::add);
        stepsByStage.put(Stage.HOLD_LOOT, seen::add);

        new PlayerEngagementPipeline(stepsByStage).reportPlayerEngagement(wonEngagement(List.of(), List.of(), List.of()));

        assertEquals(2, seen.size());
        assertSame(seen.get(0), seen.get(1));
    }

    @Test
    void aMissingResultRunsNoStep() {
        List<Stage> ranStages = new ArrayList<>();
        new PlayerEngagementPipeline(Map.of(Stage.AWARD_XP, engagement -> ranStages.add(Stage.AWARD_XP))).reportPlayerEngagement(null);

        assertTrue(ranStages.isEmpty());
    }

    @Test
    void theSummaryCountsEnemyDeploymentPointsDestroyedOrDisabledAndThePlayersLosses() {
        EngagementResultAPI result = wonEngagement(List.of(ship("player-lost", 10f)), List.of(ship("enemy-a", 30f)),
                List.of(ship("enemy-b", 12f)));

        PlayerEngagement engagement = PlayerEngagement.of(result, 1.5f);

        assertTrue(engagement.playerWon());
        assertEquals(42f, engagement.enemyDeploymentPointsDefeated());
        assertEquals(1.5f, engagement.difficulty());
        assertEquals(Set.of("player-lost"), engagement.playerLossIds());
        assertSame(result, engagement.result());
    }

    @Test
    void aLostEngagementReadsTheEnemyFromTheWinningSide() {
        List<FleetMemberAPI> enemyLosses = List.of(ship("enemy", 20f));
        List<FleetMemberAPI> playerLosses = List.of(ship("player-lost", 5f));
        EngagementResultForFleetAPI enemy = mock(EngagementResultForFleetAPI.class);
        when(enemy.getDestroyed()).thenReturn(enemyLosses);
        EngagementResultForFleetAPI player = mock(EngagementResultForFleetAPI.class);
        when(player.getDisabled()).thenReturn(playerLosses);
        EngagementResultAPI result = mock(EngagementResultAPI.class);
        when(result.getWinnerResult()).thenReturn(enemy);
        when(result.getLoserResult()).thenReturn(player);

        PlayerEngagement engagement = PlayerEngagement.of(result, 1f);

        assertFalse(engagement.playerWon());
        assertEquals(20f, engagement.enemyDeploymentPointsDefeated());
        assertEquals(Set.of("player-lost"), engagement.playerLossIds());
    }

    @Test
    void theRealPipelineRunsLossesTreesXpCrewLootThenSalvage() {
        List<String> calls = new ArrayList<>();
        SocketLossListener socketLossListener = mock(SocketLossListener.class);
        doAnswer(invocation -> calls.add("losses")).when(socketLossListener).reportPlayerEngagement(any());
        SocketableLootListener socketableLootListener = mock(SocketableLootListener.class);
        doAnswer(invocation -> calls.add("loot")).when(socketableLootListener).holdLoot(any());
        EngagementResultAPI result = wonEngagement(List.of(), List.of(), List.of());

        try (MockedStatic<ShipTreeSync> treeSync = Mockito.mockStatic(ShipTreeSync.class);
             MockedStatic<CombatXpAward> xpAward = Mockito.mockStatic(CombatXpAward.class);
             MockedStatic<FleetCrewLedgerSettlement> crewSettlement = Mockito.mockStatic(FleetCrewLedgerSettlement.class);
             MockedStatic<FleetWideEffects> fleetWideEffects = Mockito.mockStatic(FleetWideEffects.class)) {
            treeSync.when(() -> ShipTreeSync.afterPlayerEngagement(any())).thenAnswer(invocation -> calls.add("trees"));
            xpAward.when(() -> CombatXpAward.award(any())).thenAnswer(invocation -> calls.add("xp"));
            crewSettlement.when(FleetCrewLedgerSettlement::schedule).thenAnswer(invocation -> calls.add("crew"));
            fleetWideEffects.when(() -> FleetWideEffects.recomputeSalvageBonus(any())).thenAnswer(invocation -> calls.add("salvage"));

            PlayerEngagementPipeline.create(socketLossListener, socketableLootListener).reportPlayerEngagement(result);
        }

        assertEquals(List.of("losses", "trees", "xp", "crew", "loot", "salvage"), calls);
        verify(socketLossListener).reportPlayerEngagement(result);
    }

    private static FleetMemberAPI memberContributing(String id, float percent) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue("exiledSector_postBattleSalvageContribution", 0f)).thenReturn(percent);
        return member;
    }

    @Test
    void theSalvageBonusTotalsTheContributionsOfTheShipsStillInTheFleet() {
        List<FleetMemberAPI> members = List.of(memberContributing("a", 25f), memberContributing("b", 10f));
        when(fleetData.getMembersListCopy()).thenReturn(members);

        FleetWideEffects.recomputeSalvageBonus(Set.of());

        verify(salvageMult).modifyFlat("exiledSector_postBattleSalvage", 0.35f);
    }

    @Test
    void shipsLostInTheEngagementNoLongerCountTowardsItsSalvage() {
        List<FleetMemberAPI> members = List.of(memberContributing("survivor", 25f), memberContributing("lost", 10f));
        when(fleetData.getMembersListCopy()).thenReturn(members);

        FleetWideEffects.recomputeSalvageBonus(Set.of("lost"));

        verify(salvageMult).modifyFlat("exiledSector_postBattleSalvage", 0.25f);
    }

    @Test
    void theSalvageBonusDropsOnceTheContributingShipHasLeftTheFleet() {
        List<FleetMemberAPI> withSalvager = List.of(memberContributing("salvager", 25f));
        List<FleetMemberAPI> withoutSalvager = List.of(memberContributing("other", 0f));
        when(fleetData.getMembersListCopy()).thenReturn(withSalvager);
        FleetWideEffects.recomputeSalvageBonus(Set.of());

        when(fleetData.getMembersListCopy()).thenReturn(withoutSalvager);
        FleetWideEffects.recomputeSalvageBonus(Set.of());

        InOrder order = inOrder(salvageMult);
        order.verify(salvageMult).modifyFlat("exiledSector_postBattleSalvage", 0.25f);
        order.verify(salvageMult).modifyFlat("exiledSector_postBattleSalvage", 0f);
    }
}
