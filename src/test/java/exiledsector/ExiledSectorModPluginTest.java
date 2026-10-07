package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledsector.effects.NpcFleetDialogListener;
import exiledsector.effects.NpcFleetInflationListener;
import exiledsector.effects.NpcFleetSweepScript;
import exiledsector.effects.PlayerEngagementPipeline;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.socketables.SocketLossListener;
import exiledsector.socketables.SocketableDefinitions;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.SocketableNames;
import exiledsector.ui.SkillTreeRefitButton;
import exiledsector.ui.inspect.NpcTreeInspectInput;
import exiledsector.ui.inspect.SkillTreeCodexListener;
import lunalib.lunaRefit.BaseRefitButton;
import lunalib.lunaRefit.LunaRefitManager;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExiledSectorModPluginTest {

    private static final String TYPES = "{ \"skillTypes\": [ { \"id\": \"root_type\", \"name\": \"Root\", \"icon\": \"a.png\", \"tier\": \"ROOT\" },"
            + " { \"id\": \"armor\", \"name\": \"Armor\", \"icon\": \"a.png\" } ] }";
    private static final String NODES = "{ \"id\": \"root\", \"type\": \"root_type\" }, { \"id\": \"armor\", \"type\": \"armor\", \"connectedTo\": [\"root\"] }";

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<LunaSettings.SettingsCreator> settingsCreatorMock;
    private SectorAPI sector;
    private ListenerManagerAPI listenerManager;
    private SettingsAPI settings;
    private Map<String, Object> persistentData;

    @BeforeEach
    void setUp() throws Exception {
        persistentData = new HashMap<>();
        sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        listenerManager = mock(ListenerManagerAPI.class);
        when(sector.getListenerManager()).thenReturn(listenerManager);

        settings = mock(SettingsAPI.class);
        when(settings.loadJSON("data/skilltrees/skill_types.json")).thenReturn(new JSONObject("{ \"skillTypes\": [] }"));
        when(settings.loadJSON("data/skilltrees/ship_skill_tree.json")).thenReturn(new JSONObject("{ \"nodes\": [] }"));
        when(settings.getMergedSpreadsheetDataForMod("plugin", "data/config/exiledSector/split_beam_effect_blocklist.csv", "exiledSector"))
                .thenReturn(new JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("weapon", "data/config/exiledSector/energy_chain_blocklist.csv", "exiledSector"))
                .thenReturn(new JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("weapon", "data/config/exiledSector/ballistic_pierce_blocklist.csv", "exiledSector"))
                .thenReturn(new JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("hullmod", "data/config/exiledSector/drone_marker_hullmods.csv", "exiledSector"))
                .thenReturn(new JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("faction", "data/config/exiledSector/npc_faction_volumes.csv", "exiledSector"))
                .thenReturn(new JSONArray("[{\"faction\": \"hegemony\", \"region\": \"hegemony\"}]"));
        when(settings.getMergedSpreadsheetDataForMod("id", "data/config/exiledSector/socketables.csv", "exiledSector"))
                .thenReturn(new JSONArray("[{\"id\": \"chip\", \"kind\": \"subroutine\", \"prefixes\": \"HULL_MULT:4:6\"}]"));
        when(settings.getMergedJSONForMod("data/config/exiledSector/socketable_names.json", "exiledSector")).thenReturn(new JSONObject("{}"));
        when(settings.getMergedSpreadsheetDataForMod("effect", "data/config/exiledSector/socketable_affixes.csv", "exiledSector"))
                .thenReturn(new JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("old", "data/config/exiledSector/node_replacements.csv", "exiledSector"))
                .thenReturn(new JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("site", "data/config/exiledSector/socketable_salvage.csv", "exiledSector"))
                .thenReturn(new JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("item", "data/config/exiledSector/socketable_crafting.csv", "exiledSector"))
                .thenReturn(new JSONArray());

        Logger logger = mock(Logger.class);

        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(() -> Global.getLogger(any())).thenReturn(logger);
        settingsCreatorMock = Mockito.mockStatic(LunaSettings.SettingsCreator.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        loadTree("");
        SkillTree.clearTypes();
        NpcFactionVolumes.clear();
        SocketableDefinitions.clear();
        SocketableNames.clear();
        settingsCreatorMock.close();
        globalMock.close();
        lunaSettingsMock.close();
        BaseRefitButton registered = LunaRefitManager.getFirstButtonOfClass(SkillTreeRefitButton.class);
        if (registered != null) {
            LunaRefitManager.INSTANCE.removeButton(registered);
        }
    }

    private void loadTree(String nodes) throws Exception {
        loadTree(TYPES, nodes);
    }

    private void loadTree(String types, String nodes) throws Exception {
        when(settings.loadJSON("data/skilltrees/skill_types.json")).thenReturn(new JSONObject(types));
        when(settings.loadJSON("data/skilltrees/ship_skill_tree.json")).thenReturn(new JSONObject("{ \"nodes\": [ " + nodes + " ] }"));
        SkillTree.load();
    }

    private static ShipSkillData shipWithARemovedNode() {
        ShipSkillData data = ShipSkillDataManager.get("ship");
        data.chooseStartingRoot(SkillTree.get("root"));
        data.allocate(SkillTree.get("armor"), 3);
        data.getAllocatedNodeIds().add("removed_by_update");
        return data;
    }

    @Test
    void onGameLoadForgetsSavedNodesTheSkillTreeNoLongerHas() throws Exception {
        loadTree(NODES);
        ShipSkillData data = shipWithARemovedNode();

        new ExiledSectorModPlugin().onGameLoad(false);

        assertEquals(List.of("root", "armor"), List.copyOf(data.getAllocatedNodeIds()));
    }

    @Test
    void aShipResetBecauseItsRootWasRemovedKeepsItsReserveSlotUntilTheNextLoad() throws Exception {
        loadTree(NODES);
        ShipSkillData data = ShipSkillDataManager.get("rootless");
        data.chooseStartingRoot(new SkillNode("removed_root", SkillTree.getType("root_type"), List.of(), 0f, 0f));
        data.allocate(SkillTree.get("armor"), 3);
        persistentData.put("exiledSector_opSpentSlots", new HashMap<>(Map.of("rootless", 0)));

        new ExiledSectorModPlugin().onGameLoad(false);

        assertTrue(data.getAllocatedNodeIds().isEmpty());
        assertEquals(Map.of("rootless", 0), persistentData.get("exiledSector_opSpentSlots"));

        new ExiledSectorModPlugin().onGameLoad(false);

        assertEquals(Map.of(), persistentData.get("exiledSector_opSpentSlots"));
        assertNull(ShipSkillDataManager.find("rootless"));
    }

    @Test
    void onGameLoadKeepsAnOptionalNodeWhoseChosenOptionIsStillOffered() throws Exception {
        loadTree(TYPES.replace(" ] }", ", { \"id\": \"slot\", \"name\": \"Slot\", \"icon\": \"a.png\", \"optionalOptions\": [\"armor\"] } ] }"),
                NODES + ", { \"id\": \"slot_1\", \"type\": \"slot\", \"connectedTo\": [\"root\"] }");
        ShipSkillData data = ShipSkillDataManager.get("ship");
        data.chooseStartingRoot(SkillTree.get("root"));
        data.selectOption(SkillTree.get("slot_1"), SkillTree.getType("armor"), 3);

        new ExiledSectorModPlugin().onGameLoad(false);

        assertTrue(data.isAllocated("slot_1"));
        assertEquals("armor", data.getOptionalSelection("slot_1"));
    }

    @Test
    void onGameLoadLeavesSavedNodesAloneWhenASkillTypeFailedToLoad() throws Exception {
        loadTree(TYPES.replace(" ] }", ", { \"id\": \"broken\", \"name\": \"Broken\", \"icon\": \"a.png\", \"tier\": \"BOGUS\" } ] }"), NODES);
        ShipSkillData data = shipWithARemovedNode();

        new ExiledSectorModPlugin().onGameLoad(false);

        assertTrue(data.isAllocated("removed_by_update"));
    }

    @Test
    void onGameLoadLeavesSavedNodesAloneWhenTheSkillTreeDidNotLoadCompletely() throws Exception {
        loadTree(NODES + ", { \"id\": \"broken\", \"type\": \"missing_type\" }");
        ShipSkillData data = shipWithARemovedNode();

        new ExiledSectorModPlugin().onGameLoad(false);

        assertTrue(data.isAllocated("removed_by_update"));
    }

    @Test
    void onApplicationLoadRegistersTheSkillTreeRefitButton() throws Exception {
        new ExiledSectorModPlugin().onApplicationLoad();

        assertTrue(LunaRefitManager.hasButtonOfClass(SkillTreeRefitButton.class));
    }

    @Test
    void onApplicationLoadRefreshesLunaSettingsSoNewSettingsAreWrittenWithTheirDefaults() throws Exception {
        new ExiledSectorModPlugin().onApplicationLoad();

        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.refresh("exiledSector"), Mockito.times(2));
    }

    @Test
    void onApplicationLoadLoadsEveryCombatListMergedAcrossMods() throws Exception {
        new ExiledSectorModPlugin().onApplicationLoad();

        verify(Global.getSettings()).getMergedSpreadsheetDataForMod("plugin", "data/config/exiledSector/split_beam_effect_blocklist.csv", "exiledSector");
        verify(Global.getSettings()).getMergedSpreadsheetDataForMod("weapon", "data/config/exiledSector/energy_chain_blocklist.csv", "exiledSector");
        verify(Global.getSettings()).getMergedSpreadsheetDataForMod("weapon", "data/config/exiledSector/ballistic_pierce_blocklist.csv", "exiledSector");
        verify(Global.getSettings()).getMergedSpreadsheetDataForMod("hullmod", "data/config/exiledSector/drone_marker_hullmods.csv", "exiledSector");
    }

    @Test
    void onGameLoadRegistersTheInstallerScriptAsTransientSoItIsNeverSaved() {
        new ExiledSectorModPlugin().onGameLoad(true);

        verify(sector).addTransientScript(any(SkillTreeInstaller.class));
        verify(sector, never()).addScript(any(SkillTreeInstaller.class));
    }

    @Test
    void onGameLoadRemovesInstallerCopiesPersistedByOlderSavesBeforeAddingTheTransientOne() {
        new ExiledSectorModPlugin().onGameLoad(false);

        InOrder order = inOrder(sector);
        order.verify(sector).removeScriptsOfClass(SkillTreeInstaller.class);
        order.verify(sector).addTransientScript(any(SkillTreeInstaller.class));
    }

    @Test
    void onGameLoadDropsTemplateAssignmentsForShipsWithNoProgressOrDeletedTemplates() {
        String templateId = SkillTreeTemplateStore.save("Brawler", "root_low_tech_1", HullSize.CRUISER, List.of()).id();
        ShipSkillDataManager.get("kept").addXp(5f);
        SkillTreeTemplateStore.assign("kept", templateId);
        SkillTreeTemplateStore.assign("no-record", templateId);
        ShipSkillDataManager.get("levelled-but-dangling").addXp(5f);
        SkillTreeTemplateStore.assign("levelled-but-dangling", "deleted-template");

        new ExiledSectorModPlugin().onGameLoad(false);

        assertEquals(Map.of("kept", templateId), persistentData.get("exiledSector_skillTreeTemplateAssignments"));
    }

    @Test
    void onGameLoadReleasesReserveSlotsAndDropsBlankRecordsLeftByShipsWithNoProgress() {
        ShipSkillDataManager.get("temporary-copy");
        ShipSkillDataManager.get("levelled").addXp(5f);
        persistentData.put("exiledSector_opSpentSlots", new HashMap<>(Map.of("temporary-copy", 0, "levelled", 1, "sold", 2)));
        persistentData.put("exiledSector_opSpentNextSlot", 3);

        new ExiledSectorModPlugin().onGameLoad(false);

        assertEquals(Map.of("levelled", 1), persistentData.get("exiledSector_opSpentSlots"));
        assertNull(ShipSkillDataManager.find("temporary-copy"));
        assertNotNull(ShipSkillDataManager.find("levelled"));
        assertFalse(persistentData.containsKey("exiledSector_opSpentNextSlot"));
    }

    @Test
    void onGameLoadRegistersOnePlayerEngagementPipelineAsTransient() {
        new ExiledSectorModPlugin().onGameLoad(true);

        verify(sector).addTransientListener(any(PlayerEngagementPipeline.class));
        verify(sector, never()).addTransientListener(any(SocketLossListener.class));
    }

    @Test
    void onGameLoadClearsTheNpcTreeCache() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of("exiledSector_npcTree|bulwark|0|missing_root"));
        ShipSkillData before = SkillDataResolver.resolve(null, variant);

        new ExiledSectorModPlugin().onGameLoad(false);

        assertNotSame(before, SkillDataResolver.resolve(null, variant));
    }

    @Test
    void onApplicationLoadLoadsTheSocketableDefinitionsMergedAcrossMods() throws Exception {
        new ExiledSectorModPlugin().onApplicationLoad();

        assertEquals(SocketType.SUBROUTINE, SocketableDefinitions.get("chip").kind());
    }

    @Test
    void onApplicationLoadLoadsTheNpcFactionVolumesMergedAcrossMods() throws Exception {
        new ExiledSectorModPlugin().onApplicationLoad();

        assertEquals("hegemony", NpcFactionVolumes.regionFor("hegemony"));
    }

    @Test
    void onGameLoadRegistersTheNpcFleetHooksAndInspectionListenersAsTransient() {
        new ExiledSectorModPlugin().onGameLoad(false);

        verify(sector).addTransientScript(any(NpcFleetSweepScript.class));
        verify(sector, never()).addScript(any(NpcFleetSweepScript.class));
        verify(sector).addTransientListener(any(NpcFleetDialogListener.class));
        verify(listenerManager).addListener(any(NpcFleetInflationListener.class), eq(true));
        verify(listenerManager).addListener(any(NpcTreeInspectInput.class), eq(true));
        verify(listenerManager).addListener(any(SkillTreeCodexListener.class), eq(true));
    }
}
