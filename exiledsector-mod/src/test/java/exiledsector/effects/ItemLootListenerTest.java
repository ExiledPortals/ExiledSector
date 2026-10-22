package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
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
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.items.BattleItemDrops;
import exiledsector.items.FactionFleetDrops;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinitions;
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
import java.util.Map;
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
    private static final String TEST_DROPS = "test_drops";
    private static final String SECTION_ITEM_TAG = TAG + "|frameworkTypes:bridge+reactor|frameworkSockets:bridge=npc:bridge_basic/3";

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private CargoAPI playerCargo;
    private final ItemLootListener listener = new ItemLootListener();

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
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
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "volume_subroutine").put("kind", "MODSPEC_CACHE").put("rarity", "10")));
        FactionFleetDrops.register(TEST_DROPS, Map.of("volume_subroutine", FactionFleetDrops.Source.anyFaction(1f)));
    }

    @AfterEach
    void tearDown() {
        FactionFleetDrops.clear(TEST_DROPS);
        ItemDefinitions.clear();
        RealSkillData.clear();
        globalMock.close();
        lunaSettingsMock.close();
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

    private static Random alwaysLucky() {
        return new Random() {
            @Override
            public float nextFloat() {
                return 0f;
            }
        };
    }

    private static FleetMemberAPI factionShip(BattleAPI battle, String id) {
        FleetMemberAPI member = warship(id, 100f);
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn("pirates");
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getFaction()).thenReturn(faction);
        when(battle.getSourceFleet(member)).thenReturn(fleet);
        return member;
    }

    @Test
    void aDestroyedNpcShipDropsNothingItCarried() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(flagship("wrecked"), flagship("framed", SECTION_ITEM_TAG)),
                List.of(flagship("crippled"))), 1f));
        CargoAPI loot = mock(CargoAPI.class);

        luckyListener.reportEncounterLootGenerated(context(battle), loot);

        verify(loot, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aWonBattlesFactionDropGoesIntoThatBattlesLoot() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(factionShip(battle, "wrecked")),
                List.of(factionShip(battle, "crippled"))), 1f));
        CargoAPI loot = mock(CargoAPI.class);

        luckyListener.reportEncounterLootGenerated(context(battle), loot);

        assertEquals(1, lootData(loot, 1).size());
    }

    @Test
    void aRecoveredShipStillCountsTowardsTheBattlesDrops() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        FleetMemberAPI crippled = factionShip(battle, "crippled");
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(), List.of(crippled)), 1f));
        luckyListener.reportShipsRecovered(List.of(crippled), null);
        CargoAPI loot = mock(CargoAPI.class);

        luckyListener.reportEncounterLootGenerated(context(battle), loot);

        assertEquals(1, lootData(loot, 1).size());
    }

    @Test
    void aWonBattleWithNoLootScreenPutsTheItemStraightIntoTheCargo() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(factionShip(battle, "wrecked")), List.of()), 1f));

        luckyListener.reportBattleFinished(null, battle);

        assertEquals(1, lootData(playerCargo, 1).size());
    }

    @Test
    void aLostBattleDropsNothing() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        EngagementResultAPI result = engagement(battle, List.of(factionShip(battle, "wrecked")), List.of());
        when(result.didPlayerWin()).thenReturn(false);
        luckyListener.holdLoot(PlayerEngagement.of(result, 1f));

        luckyListener.reportBattleFinished(null, battle);

        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void leavingABattleWithoutLootForgetsItsItemsSoRejoiningCannotDropThem() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(factionShip(battle, "wrecked")), List.of()), 1f));
        when(battle.isPlayerInvolved()).thenReturn(false);

        luckyListener.reportShownInteractionDialog(null);
        CargoAPI loot = mock(CargoAPI.class);
        luckyListener.reportEncounterLootGenerated(context(battle), loot);
        luckyListener.reportBattleFinished(null, battle);

        verify(loot, never()).addSpecial(any(), anyFloat());
        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void lootedItemsAreNotDeliveredAgainWhenTheBattleFinishes() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(factionShip(battle, "wrecked")), List.of()), 1f));
        CargoAPI loot = mock(CargoAPI.class);

        luckyListener.reportEncounterLootGenerated(context(battle), loot);
        luckyListener.reportBattleFinished(null, battle);

        assertEquals(1, lootData(loot, 1).size());
        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void anotherBattlesLootNeverPicksUpThisBattlesItems() {
        ItemLootListener luckyListener = new ItemLootListener(alwaysLucky());
        BattleAPI battle = mock(BattleAPI.class);
        luckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(factionShip(battle, "wrecked")), List.of()), 1f));
        CargoAPI otherLoot = mock(CargoAPI.class);

        luckyListener.reportEncounterLootGenerated(context(mock(BattleAPI.class)), otherLoot);
        CargoAPI loot = mock(CargoAPI.class);
        luckyListener.reportEncounterLootGenerated(context(battle), loot);

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
        BattleAPI battle = mock(BattleAPI.class);
        listener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(warship("a", 120f)), List.of(warship("b", 80f))), 1f));
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(battle), loot);

        verify(loot).addSpecial(MaterialItems.data(ItemDisassembly.PARTS_ITEM_ID), 10);
    }

    @Test
    void lostBattlesAddNoParts() {
        BattleAPI battle = mock(BattleAPI.class);
        EngagementResultAPI result = engagement(battle, List.of(warship("a", 200f)), List.of());
        when(result.didPlayerWin()).thenReturn(false);
        listener.holdLoot(PlayerEngagement.of(result, 1f));
        CargoAPI loot = mock(CargoAPI.class);

        listener.reportEncounterLootGenerated(context(battle), loot);

        verify(loot, never()).addSpecial(Mockito.any(), anyFloat());
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
                .put(new JSONObject().put("source", "battle").put("chance", "0.25").put("min_strength", "150").put("strength_per_tier", "100")
                        .put("max_tier", "6")));
        try {
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
            chartListener.holdLoot(PlayerEngagement.of(engagement(strongBattle, List.of(warship("c", 200f), warship("d", 200f)), List.of()), 1f));
            chartListener.reportBattleFinished(null, strongBattle);
            List<SpecialItemData> strongCharts = starCharts(playerCargo);
            assertEquals(1, strongCharts.size());
            assertEquals(1, ItemData.ofStarChart(strongCharts.get(0)).preview().tier(), "ships without NPC trees count as level 0");
        } finally {
            ChartDrops.clear();
            StarChartFixtures.clear();
        }
    }
    private static Random neverLucky() {
        return new Random() {
            @Override
            public float nextFloat() {
                return 0.999f;
            }
        };
    }

    private static List<String> hullItemsAdded(CargoAPI cargo) {
        ArgumentCaptor<SpecialItemData> specials = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, Mockito.atLeast(0)).addSpecial(specials.capture(), anyFloat());
        return specials.getAllValues().stream().filter(special -> ItemData.ITEM_ID.equals(special.getId())).map(SpecialItemData::getData)
                .toList();
    }

    private List<String> battleHullItems(int playerLevel, String treeTag) throws Exception {
        MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
        when(playerStats.getLevel()).thenReturn(playerLevel);
        when(Global.getSector().getPlayerStats()).thenReturn(playerStats);
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "test_bridge").put("section", "bridge").put("rarity", "10")
                .put("prefixes", "HULL_PERCENT:4:6")));
        BattleItemDrops.register(new JSONArray().put(new JSONObject().put("drop", "mods_1").put("per_100_power", "1")));
        try {
            ItemLootListener unluckyListener = new ItemLootListener(neverLucky());
            BattleAPI battle = mock(BattleAPI.class);
            FleetMemberAPI wreck = warship("wreck", 100f);
            if (treeTag != null) {
                when(wreck.getVariant().getTags()).thenReturn(List.of(treeTag));
            }
            unluckyListener.holdLoot(PlayerEngagement.of(engagement(battle, List.of(wreck), List.of()), 1f));
            CargoAPI loot = mock(CargoAPI.class);
            unluckyListener.reportEncounterLootGenerated(context(battle), loot);
            return hullItemsAdded(loot);
        } finally {
            BattleItemDrops.clear();
        }
    }

    @Test
    void aWonBattleDropsHullItemsByTheDefeatedShipsPowerFromPlayerLevelFifteen() throws Exception {
        String thirtyNodeTree = "exiledSector_npcTree|generated|30|root" + ",n".repeat(30);

        assertEquals(1, battleHullItems(15, null).size());
        assertEquals(2, battleHullItems(15, thirtyNodeTree).size());
        assertEquals(0, battleHullItems(14, thirtyNodeTree).size());
    }
}
