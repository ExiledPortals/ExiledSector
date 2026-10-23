package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.compat.NightcrossCompat;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.JanusDevice;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.unlock.NodeGates;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GateRewardAnchorsTest {

    private static final String ABYSS_GATE = "gate_singularity_defeated";
    private static final String LUNAR_GATE = "gate_lunar_fleet_defeated";
    private static final String ABYSS_ANCHOR = "wormhole_anchor_abyss";
    private static final String STARGAZER_ANCHOR = "wormhole_anchor_stargazer";

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private final Map<String, Object> memoryValues = new HashMap<>();
    private final Map<String, Object> persistentData = new HashMap<>();
    private final ModManagerAPI modManager = mock(ModManagerAPI.class);
    private SectorAPI sector;
    private CargoAPI playerCargo;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        memoryValues.put(JanusDevice.FLAG, true);
        MemoryAPI sectorMemory = memory(memoryValues);
        MemoryAPI playerMemory = memory(new HashMap<>());
        sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
        when(sector.getPersistentData()).thenReturn(persistentData);
        when(sector.getSeedString()).thenReturn("seed");
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        playerCargo = mock(CargoAPI.class);
        when(playerFleet.getCargo()).thenReturn(playerCargo);
        when(sector.getPlayerFleet()).thenReturn(playerFleet);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(NightcrossCompat.MOD_ID)).thenReturn(true);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        globalMock.when(Global::getSettings).thenReturn(settings);
        NodeGates.register(new JSONObject("{\"" + ABYSS_GATE + "\": {\"rewardAnchor\": true, \"unlockConditions\": [{\"type\": \"ratSingularityDefeated\"}]},"
                + " \"" + LUNAR_GATE + "\": {\"rewardAnchor\": true, \"unlockConditions\": [{\"type\": \"nightcrossLunarFleetDefeated\"}]}}"));

        SkillTree.clearNodes();
        SkillType exitType = new SkillType.Builder("wormhole_exit", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT).build();
        SkillTree.register(new SkillNode("rat_abyss_wormhole_1", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("abyss", ABYSS_GATE)));
        SkillTree.register(new SkillNode("stargazer_wormhole_1", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("stargazer", LUNAR_GATE)));
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchorRow(ABYSS_ANCHOR, "abyss", "rat_abyss_wormhole_1"))
                .put(anchorRow(STARGAZER_ANCHOR, "stargazer", "stargazer_wormhole_1")));
    }

    @AfterEach
    void tearDown() throws Exception {
        WormholeAnchorDefinitions.clear();
        SkillTree.clearNodes();
        NodeGates.register(new JSONObject("{}"));
        globalMock.close();
        lunaSettingsMock.close();
    }

    private static MemoryAPI memory(Map<String, Object> values) {
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(values.get(call.<String>getArgument(0))));
        doAnswer(call -> values.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        return memory;
    }

    private static JSONObject anchorRow(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id + " name")
                .put("icon", "graphics/icons/nav_relay.png").put("rarity", "10").put("description", "Opens a wormhole.");
    }

    private static FleetMemberAPI member(String hullId) {
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullId()).thenReturn(hullId);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getHullSpec()).thenReturn(hullSpec);
        return member;
    }

    private static BattleLoot wonBattle() {
        BattleLoot battle = new BattleLoot(new Random(3L), new Random(4L));
        battle.setPlayerWon(true);
        return battle;
    }

    private static List<String> ids(List<ItemData> items) {
        return items.stream().map(ItemData::definitionId).toList();
    }

    private BattleEngagement engagementLosing(List<FleetMemberAPI> lost, CampaignFleetAPI sourceFleet) {
        BattleAPI battle = mock(BattleAPI.class);
        lost.forEach(member -> when(battle.getSourceFleet(member)).thenReturn(sourceFleet));
        return new BattleEngagement(battle, 1f, 100f, lost);
    }

    private static CampaignFleetAPI fleet(boolean lunarFleet, List<FleetMemberAPI> members) {
        MemoryAPI fleetMemory = memory(new HashMap<>(lunarFleet ? Map.of("$SDFStargazerLunarFleet", true) : Map.of()));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(fleetMemory);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(new ArrayList<>(members));
        when(fleet.getFleetData()).thenReturn(fleetData);
        return fleet;
    }

    @Test
    void theBattleThatKillsTheGenesisHandsOutExactlyOneAbyssAnchorOncePerSave() {
        FleetMemberAPI genesis = member("rat_genesis");
        BattleLoot battle = wonBattle();
        GateRewardAnchors.recordEngagement(battle, engagementLosing(List.of(genesis), fleet(false, List.of(genesis))));

        assertEquals(List.of(ABYSS_ANCHOR), ids(GateRewardAnchors.claim(battle, List.of(), sector)));
        assertEquals(List.of(), ids(GateRewardAnchors.claim(battle, List.of(), sector)));

        BattleLoot rematch = wonBattle();
        GateRewardAnchors.recordEngagement(rematch, engagementLosing(List.of(genesis), fleet(false, List.of(genesis))));
        assertEquals(List.of(), ids(GateRewardAnchors.claim(rematch, List.of(), sector)));
    }

    @Test
    void noRewardIsAddedWhenTheBattlesOwnRollAlreadyDroppedOne() {
        FleetMemberAPI genesis = member("rat_genesis");
        BattleLoot battle = wonBattle();
        GateRewardAnchors.recordEngagement(battle, engagementLosing(List.of(genesis), fleet(false, List.of(genesis))));
        ItemData rolled = ItemData.rolled(ItemDefinitions.get(ABYSS_ANCHOR), 9L);

        assertEquals(List.of(), ids(GateRewardAnchors.claim(battle, List.of(rolled), sector)));
        assertTrue(GateRewardAnchors.granted(sector, ABYSS_GATE));
    }

    @Test
    void onlyBeatingTheWholeLunarFleetEarnsTheStargazerAnchor() {
        FleetMemberAPI flagship = member("na_macula");
        FleetMemberAPI escort = member("na_escort");
        CampaignFleetAPI lunarFleet = fleet(true, List.of(flagship, escort));

        BattleLoot partial = wonBattle();
        GateRewardAnchors.recordEngagement(partial, engagementLosing(List.of(flagship), lunarFleet));
        assertEquals(List.of(), ids(GateRewardAnchors.claim(partial, List.of(), sector)));

        BattleLoot whole = wonBattle();
        GateRewardAnchors.recordEngagement(whole, engagementLosing(List.of(flagship, escort), lunarFleet));
        assertEquals(List.of(STARGAZER_ANCHOR), ids(GateRewardAnchors.claim(whole, List.of(), sector)));
    }

    @Test
    void aBossBeatenWhileItsAreaIsOffKeepsItsRewardForWhenTheAreaComesBack() {
        SkillTree.clearNodes();
        FleetMemberAPI genesis = member("rat_genesis");
        BattleLoot battle = wonBattle();
        GateRewardAnchors.recordEngagement(battle, engagementLosing(List.of(genesis), fleet(false, List.of(genesis))));

        assertEquals(List.of(), ids(GateRewardAnchors.claim(battle, List.of(), sector)));
        assertFalse(GateRewardAnchors.granted(sector, ABYSS_GATE));
    }

    @Test
    void aLostBattleEarnsNothing() {
        FleetMemberAPI genesis = member("rat_genesis");
        BattleLoot lost = new BattleLoot(new Random(3L), new Random(4L));
        GateRewardAnchors.recordEngagement(lost, engagementLosing(List.of(genesis), fleet(false, List.of(genesis))));

        assertEquals(List.of(), ids(GateRewardAnchors.claim(lost, List.of(), sector)));
    }

    @Test
    void aGateThatOpensWithoutAMarkedBattleGrantsItsAnchorToCargoOnce() {
        GateRewardAnchors.grantMissed(sector);
        verify(playerCargo, never()).addSpecial(any(), anyFloat());

        memoryValues.put("$na_defeated_macula", true);
        GateRewardAnchors.grantMissed(sector);
        GateRewardAnchors.grantMissed(sector);

        ArgumentCaptor<SpecialItemData> granted = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(playerCargo).addSpecial(granted.capture(), anyFloat());
        assertEquals(STARGAZER_ANCHOR, ItemData.of(granted.getValue()).definitionId());
    }

    @Test
    void aGateWhoseModIsMissingNeverGrantsAFallbackAnchor() {
        when(modManager.isModEnabled(NightcrossCompat.MOD_ID)).thenReturn(false);

        GateRewardAnchors.grantMissed(sector);

        verify(playerCargo, never()).addSpecial(any(), anyFloat());
    }

    @Test
    void savesThatAlreadyBeatTheBossGetNoRewardAfterTheUpdate() {
        memoryValues.put("$na_defeated_macula", true);

        GateRewardAnchors.markOpenGatesGrantedOnce(sector);
        GateRewardAnchors.grantMissed(sector);

        verify(playerCargo, never()).addSpecial(any(), anyFloat());
        assertTrue(GateRewardAnchors.granted(sector, LUNAR_GATE));
    }
}
