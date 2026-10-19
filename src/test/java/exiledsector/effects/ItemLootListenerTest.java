package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.items.MaterialItems;
import exiledsector.charts.ChartContent;
import exiledsector.charts.ChartDrops;
import exiledsector.charts.ChartFleetFixtures;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartObjective;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRuns;
import exiledsector.items.StarChartFixtures;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.items.HullUpgradeData;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import exiledsector.items.ItemDisassembly;
import lunalib.lunaSettings.LunaSettings;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemLootListenerTest {

    private static final String TAG = "exiledSector_npcTree|generated|1|root,socket_1|sockets:socket_1=npc:volume_subroutine/12";
    private static final String SECTION_ITEM_TAG = TAG + "|frameworkTypes:bridge+reactor|frameworkSockets:bridge=npc:bridge_basic/3";

    private MockedStatic<Global> globalMock;
    private CargoAPI playerCargo;
    private final ItemLootListener listener = new ItemLootListener();

    @BeforeEach
    void setUp() throws Exception {
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
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "volume_subroutine").put("kind", "VOLUME")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        RealSkillData.clear();
        globalMock.close();
    }

    private static FleetMemberAPI flagship(String id) {
        return flagship(id, TAG);
    }

    private static FleetMemberAPI flagship(String id, String tag) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of(tag));
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
            assertEquals(ItemData.ITEM_ID, item.getId());
            return item.getData();
        }).toList();
    }

    @Test
    void aDestroyedOrDisabledEnemyShipsItemDropsIntoThatBattlesLoot() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(flagship("wrecked")), List.of(flagship("crippled"))), 1f));
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(battle), loot);

        assertEquals(List.of("volume_subroutine|12", "volume_subroutine|12"), lootData(loot, 2));
    }

    private static FleetMemberAPI cruiserFlagship(String id) {
        FleetMemberAPI member = flagship(id, SECTION_ITEM_TAG);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(HullSize.CRUISER);
        when(member.getHullSpec()).thenReturn(hullSpec);
        return member;
    }

    private static Random alwaysLucky() {
        return new Random() {
            @Override
            public float nextFloat() {
                return 0f;
            }
        };
    }

    @Test
    void aDestroyedFlagshipDropsTheItemsInItsSocketsAndSometimesAHullUpgrade() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(cruiserFlagship("wrecked")), List.of()), 1f));
        CargoAPI loot = mock(CargoAPI.class);

        luckyListener.reportEncounterLootGenerated(context(battle), loot);

        ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(loot, Mockito.times(3)).addSpecial(items.capture(), Mockito.eq(1f));
        assertEquals(List.of(ItemData.ITEM_ID + ":volume_subroutine|12", ItemData.ITEM_ID + ":bridge_basic|3",
                        HullUpgradeData.ITEM_ID + ":CRUISER"),
                items.getAllValues().stream().map(item -> item.getId() + ":" + item.getData()).toList());
    }

    @Test
    void withHullUpgradesSwitchedOffAFlagshipDropsOnlyItsSubroutines() {
        HullUpgradesSetting.setEnabledForTests(false);
        try {
            ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
            BattleAPI battle = mock(BattleAPI.class);
            luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(cruiserFlagship("wrecked")), List.of()), 1f));
            CargoAPI loot = mock(CargoAPI.class);

            luckyListener.reportEncounterLootGenerated(context(battle), loot);

            ArgumentCaptor<SpecialItemData> items = ArgumentCaptor.forClass(SpecialItemData.class);
            verify(loot, Mockito.times(1)).addSpecial(items.capture(), Mockito.eq(1f));
            assertEquals(List.of(ItemData.ITEM_ID + ":volume_subroutine|12"),
                    items.getAllValues().stream().map(item -> item.getId() + ":" + item.getData()).toList());
        } finally {
            HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
        }
    }

    @Test
    void aRecoveredFlagshipKeepsItsSectionOutOfTheLoot() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        FleetMemberAPI crippled = cruiserFlagship("crippled");
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(), List.of(crippled)), 1f));
        luckyListener.reportShipsRecovered(List.of(crippled), null);
        CargoAPI loot = mock(CargoAPI.class);

        luckyListener.reportEncounterLootGenerated(context(battle), loot);

        verify(loot, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aRecoveredShipKeepsItsItemOutOfTheLoot() {
        BattleAPI battle = mock(BattleAPI.class);
        FleetMemberAPI crippled = flagship("crippled");
        listener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(), List.of(crippled)), 1f));
        listener.reportShipsRecovered(List.of(crippled), null);
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(battle), loot);

        verify(loot, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aWonBattleWithNoLootScreenPutsTheItemStraightIntoTheCargo() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(flagship("wrecked")), List.of()), 1f));

        listener.reportBattleFinished(null, battle);

        assertEquals(List.of("volume_subroutine|12"), lootData(playerCargo, 1));
    }

    @Test
    void aLostBattleDropsNothing() {
        BattleAPI battle = mock(BattleAPI.class);
        EngagementResultAPI result = engagement(battle, List.of(flagship("wrecked")), List.of());
        when(result.didPlayerWin()).thenReturn(false);
        listener.holdLoot(PlayerEngagement.of(result, 1f));

        listener.reportBattleFinished(null, battle);

        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void leavingABattleWithoutLootForgetsItsItemsSoRejoiningCannotDropThem() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(flagship("wrecked")), List.of()), 1f));
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
        listener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(flagship("wrecked")), List.of()), 1f));
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(battle), loot);
        listener.reportBattleFinished(null, battle);

        assertEquals(1, lootData(loot, 1).size());
        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void anotherBattlesLootNeverPicksUpThisBattlesItems() {
        BattleAPI battle = mock(BattleAPI.class);
        listener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(flagship("wrecked")), List.of()), 1f));
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
            luna.when(() -> LunaSettings.getInt(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            BattleAPI battle = mock(BattleAPI.class);
            listener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(warship("a", 120f)), List.of(warship("b", 80f))), 1f));
            CargoAPI loot = mock(CargoAPI.class);

            listener.reportEncounterLootGenerated(context(battle), loot);

            verify(loot).addSpecial(MaterialItems.data(ItemDisassembly.PARTS_ITEM_ID), 10);
        }
    }

    @Test
    void lostBattlesAddNoParts() {
        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.getFloat(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            luna.when(() -> LunaSettings.getInt(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            BattleAPI battle = mock(BattleAPI.class);
            EngagementResultAPI result = engagement(battle, List.of(warship("a", 200f)), List.of());
            when(result.didPlayerWin()).thenReturn(false);
            listener.holdLoot(PlayerEngagement.of(result, 1f));
            CargoAPI loot = mock(CargoAPI.class);

            listener.reportEncounterLootGenerated(context(battle), loot);

            verify(loot, never()).addSpecial(Mockito.any(), anyFloat());
        }
    }
private static List<SpecialItemData> starCharts(CargoAPI cargo) {
        ArgumentCaptor<SpecialItemData> specials = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, Mockito.atLeast(0)).addSpecial(specials.capture(), anyFloat());
        return specials.getAllValues().stream().filter(special -> ItemData.STAR_CHART_ITEM_ID.equals(special.getId())).toList();
    }

    @Test
    void wonBattlesRollStarChartsFromChartFleetsAtTheRunTierAndOutsideOnlyFromStrongEnemies() throws Exception {
        StarChartFixtures.registerShipped();
        ChartDrops.register(new JSONArray()
                .put(new JSONObject().put("source", "chart_fleet").put("chance", "1"))
                .put(new JSONObject().put("source", "battle").put("chance", "1").put("min_strength", "150").put("strength_per_tier", "100")
                        .put("max_tier", "6")));
        try (MockedStatic<LunaSettings> luna = Mockito.mockStatic(LunaSettings.class)) {
            luna.when(() -> LunaSettings.getFloat(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            luna.when(() -> LunaSettings.getInt(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
            SectorAPI sector = Global.getSector();
            when(sector.getPersistentData()).thenReturn(new HashMap<>());
            ChartRun run = new ChartRun("lootRun", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
            run.recordContent(new ChartContent(8, "hegemony", ChartObjective.CLEAR, List.of("g1"), "boss", 4, 6));
            run.recordArrival();
            ChartRuns.store(run);
            StarSystemAPI system = mock(StarSystemAPI.class);
            when(system.hasTag(ChartIds.SYSTEM_TAG)).thenReturn(true);
            when(system.getTags()).thenReturn(List.of(ChartIds.SYSTEM_TAG, ChartIds.runTag("lootRun")));
            CampaignFleetAPI playerFleet = sector.getPlayerFleet();
            when(playerFleet.getContainingLocation()).thenReturn(system);
            ItemLootListener chartListener = new ItemLootListener(new Random(1L), new Random(2L));

            BattleAPI strayBattle = mock(BattleAPI.class);
            chartListener.holdLoot(PlayerEngagement.of(engagement(strayBattle, List.of(warship("stray", 400f)), List.of()), 1f));
            CargoAPI strayLoot = mock(CargoAPI.class);
            chartListener.reportEncounterLootGenerated(context(strayBattle), strayLoot);
            assertEquals(List.of(), starCharts(strayLoot), "ships that are not from a chart fleet drop no charts inside the system");

            BattleAPI systemBattle = mock(BattleAPI.class);
            FleetMemberAPI guardShip = warship("a", 20f);
            CampaignFleetAPI guardFleet = ChartFleetFixtures.chartFleet("g1", "lootRun", 20f);
            when(systemBattle.getSourceFleet(guardShip)).thenReturn(guardFleet);
            chartListener.holdLoot(PlayerEngagement.of(engagement(systemBattle, List.of(guardShip), List.of()), 1f));
            CargoAPI systemLoot = mock(CargoAPI.class);
            chartListener.reportEncounterLootGenerated(context(systemBattle), systemLoot);
            List<SpecialItemData> systemCharts = starCharts(systemLoot);
            assertEquals(1, systemCharts.size());
            assertEquals(8, ItemData.ofStarChart(systemCharts.get(0)).preview().tier());

            when(playerFleet.getContainingLocation()).thenReturn(mock(LocationAPI.class));
            BattleAPI weakBattle = mock(BattleAPI.class);
            chartListener.holdLoot(PlayerEngagement.of(engagement(weakBattle, List.of(warship("b", 100f)), List.of()), 1f));
            CargoAPI weakLoot = mock(CargoAPI.class);
            chartListener.reportEncounterLootGenerated(context(weakBattle), weakLoot);
            assertEquals(List.of(), starCharts(weakLoot));

            BattleAPI strongBattle = mock(BattleAPI.class);
            chartListener.holdLoot(PlayerEngagement.of(engagement(strongBattle, List.of(warship("c", 200f), warship("d", 160f)), List.of()), 1f));
            chartListener.reportBattleFinished(null, strongBattle);
            List<SpecialItemData> strongCharts = starCharts(playerCargo);
            assertEquals(1, strongCharts.size());
            assertEquals(3, ItemData.ofStarChart(strongCharts.get(0)).preview().tier());
        } finally {
            ChartDrops.clear();
            StarChartFixtures.clear();
        }
    }
}

