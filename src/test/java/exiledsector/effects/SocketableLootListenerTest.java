package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.socketables.SocketableItemData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exiledsector.socketables.SocketableDisassembly;
import lunalib.lunaSettings.LunaSettings;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocketableLootListenerTest {

    private static final String TAG = "exiledSector_npcTree|generated|1|root,socket_1|sockets:socket_1=npc:domain_subroutine_military/12";

    private MockedStatic<Global> globalMock;
    private CargoAPI playerCargo;
    private final SocketableLootListener listener = new SocketableLootListener();

    @BeforeEach
    void setUp() {
        globalMock = Mockito.mockStatic(Global.class);
        playerCargo = mock(CargoAPI.class);
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        when(playerFleet.getCargo()).thenReturn(playerCargo);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of());
        when(playerFleet.getFleetData()).thenReturn(fleetData);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        globalMock.when(Global::getSector).thenReturn(sector);
        SkillType rootType = new SkillType.Builder("root_type", "root", "a.png", SkillTier.ROOT).build();
        SkillType socketType = new SkillType.Builder("socket", "socket", "a.png", SkillTier.SOCKET).build();
        SkillTree.registerType(rootType);
        SkillTree.registerType(socketType);
        SkillTree.register(new SkillNode("root", rootType, List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("socket_1", socketType, List.of("root"), 0f, 0f));
    }

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
        globalMock.close();
    }

    private static FleetMemberAPI flagship(String id) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of(TAG));
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getVariant()).thenReturn(variant);
        return member;
    }

    private static EngagementResultAPI engagement(BattleAPI battle, List<FleetMemberAPI> destroyed, List<FleetMemberAPI> disabled) {
        FleetMemberAPI playerShip = flagship("player_ship");
        EngagementResultForFleetAPI player = mock(EngagementResultForFleetAPI.class);
        when(player.isPlayer()).thenReturn(true);
        when(player.getDestroyed()).thenReturn(List.of(playerShip));
        EngagementResultForFleetAPI enemy = mock(EngagementResultForFleetAPI.class);
        when(enemy.getDestroyed()).thenReturn(destroyed);
        when(enemy.getDisabled()).thenReturn(disabled);
        EngagementResultAPI result = mock(EngagementResultAPI.class);
        when(result.getBattle()).thenReturn(battle);
        when(result.didPlayerWin()).thenReturn(true);
        when(result.getWinnerResult()).thenReturn(player);
        when(result.getLoserResult()).thenReturn(enemy);
        return result;
    }

    private static FleetEncounterContextPlugin context(BattleAPI battle) {
        FleetEncounterContextPlugin context = mock(FleetEncounterContextPlugin.class);
        when(context.getBattle()).thenReturn(battle);
        return context;
    }

    private static List<String> lootData(CargoAPI loot, int expected) {
        ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(loot, Mockito.times(expected)).addSpecial(items.capture(), Mockito.eq(1f));
        return items.getAllValues().stream().map(item -> {
            assertEquals(SocketableItemData.ITEM_ID, item.getId());
            return item.getData();
        }).toList();
    }

    @Test
    void aDestroyedOrDisabledEnemyShipsItemDropsIntoThatBattlesLoot() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.reportPlayerEngagement(engagement(battle, List.of(flagship("wrecked")), List.of(flagship("crippled"))));
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(battle), loot);

        assertEquals(List.of("domain_subroutine_military|12", "domain_subroutine_military|12"), lootData(loot, 2));
    }

    @Test
    void aRecoveredShipKeepsItsItemOutOfTheLoot() {
        BattleAPI battle = mock(BattleAPI.class);
        FleetMemberAPI crippled = flagship("crippled");
        listener.reportPlayerEngagement(engagement(battle, List.of(), List.of(crippled)));
        listener.reportShipsRecovered(List.of(crippled), null);
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(battle), loot);

        verify(loot, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aWonBattleWithNoLootScreenPutsTheItemStraightIntoTheCargo() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.reportPlayerEngagement(engagement(battle, List.of(flagship("wrecked")), List.of()));

        listener.reportBattleFinished(null, battle);

        assertEquals(List.of("domain_subroutine_military|12"), lootData(playerCargo, 1));
    }

    @Test
    void aLostBattleDropsNothing() {
        BattleAPI battle = mock(BattleAPI.class);
        EngagementResultAPI result = engagement(battle, List.of(flagship("wrecked")), List.of());
        when(result.didPlayerWin()).thenReturn(false);
        listener.reportPlayerEngagement(result);

        listener.reportBattleFinished(null, battle);

        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void leavingABattleWithoutLootForgetsItsItemsSoRejoiningCannotDropThem() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.reportPlayerEngagement(engagement(battle, List.of(flagship("wrecked")), List.of()));
        when(battle.isPlayerInvolved()).thenReturn(false);

        listener.reportShownInteractionDialog(null);
        CargoAPI loot = mock(CargoAPI.class);
        listener.reportEncounterLootGenerated(context(battle), loot);
        listener.reportBattleFinished(null, battle);

        verify(loot, never()).addSpecial(any(), anyFloat());
        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void lootedItemsAreNotDeliveredAgainWhenTheBattleFinishes() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.reportPlayerEngagement(engagement(battle, List.of(flagship("wrecked")), List.of()));
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(battle), loot);
        listener.reportBattleFinished(null, battle);

        assertEquals(1, lootData(loot, 1).size());
        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void anotherBattlesLootNeverPicksUpThisBattlesItems() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.reportPlayerEngagement(engagement(battle, List.of(flagship("wrecked")), List.of()));
        CargoAPI otherLoot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(mock(BattleAPI.class)), otherLoot);
        CargoAPI loot = mock(CargoAPI.class);
        listener.reportEncounterLootGenerated(context(battle), loot);

        verify(otherLoot, never()).addSpecial(any(), anyFloat());
        assertEquals(1, lootData(loot, 1).size());
    }

    private static FleetMemberAPI warship(String id, float deploymentPoints) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getVariant()).thenReturn(mock(ShipVariantAPI.class));
        when(member.getDeploymentPointsCost()).thenReturn(deploymentPoints);
        return member;
    }

    @Test
    void wonBattlesAddPartsForTheEnemyDeploymentPointsDefeated() {
        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.getFloat(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            BattleAPI battle = mock(BattleAPI.class);
            listener.reportPlayerEngagement(engagement(battle, List.of(warship("a", 120f)), List.of(warship("b", 80f))));
            CargoAPI loot = mock(CargoAPI.class);

            listener.reportEncounterLootGenerated(context(battle), loot);

            verify(loot).addCommodity(SocketableDisassembly.PARTS_COMMODITY_ID, 10);
        }
    }

    @Test
    void lostBattlesAddNoParts() {
        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.getFloat(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            BattleAPI battle = mock(BattleAPI.class);
            EngagementResultAPI result = engagement(battle, List.of(warship("a", 200f)), List.of());
            when(result.didPlayerWin()).thenReturn(false);
            listener.reportPlayerEngagement(result);
            CargoAPI loot = mock(CargoAPI.class);

            listener.reportEncounterLootGenerated(context(battle), loot);

            verify(loot, never()).addCommodity(Mockito.anyString(), anyFloat());
        }
    }
}
