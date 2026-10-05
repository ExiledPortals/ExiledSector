package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcSkillTreeBuilder;
import exiledsector.skills.npc.NpcTreeRecords;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.SocketableDefinitions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NpcUniqueAlertsTest {

    private final Map<String, Object> memoryValues = new HashMap<>();
    private MockedStatic<Global> globalMock;
    private CampaignUIAPI ui;
    private CampaignFleetAPI fleet;

    @BeforeEach
    void setUp() throws Exception {
        RealSkillData.clear();
        register("root", SkillTier.ROOT);
        register("socket_1", SkillTier.SOCKET, "root");
        SocketableDefinitions.register(new JSONArray()
                .put(definition("basic", false))
                .put(definition("relic", true).put("name", "Ancient Relic")));

        globalMock = Mockito.mockStatic(Global.class);
        SectorAPI sector = mock(SectorAPI.class);
        ui = mock(CampaignUIAPI.class);
        when(sector.getCampaignUI()).thenReturn(ui);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        globalMock.when(Global::getSector).thenReturn(sector);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getColor(anyString())).thenReturn(Color.WHITE);
        globalMock.when(Global::getSettings).thenReturn(settings);

        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(call -> Boolean.TRUE.equals(memoryValues.get(call.<String>getArgument(0))));
        when(memory.get(anyString())).thenAnswer(call -> memoryValues.get(call.<String>getArgument(0)));
        when(memory.contains(anyString())).thenAnswer(call -> memoryValues.containsKey(call.<String>getArgument(0)));
        doAnswer(call -> memoryValues.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        fleet = mock(CampaignFleetAPI.class);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        when(fleet.getNameWithFactionKeepCase()).thenReturn("Hegemony Patrol");
        FleetMemberAPI flagship = mock(FleetMemberAPI.class);
        when(flagship.getId()).thenReturn("flagship");
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(flagship));
        when(fleet.getFleetData()).thenReturn(fleetData);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        NpcSocketables.clearCache();
        SocketableDefinitions.clear();
        RealSkillData.clear();
    }

    private static JSONObject definition(String id, boolean unique) throws Exception {
        return new JSONObject().put("id", id).put("kind", "subroutine").put("prefixes", "HULL_MULT:4:6")
                .put("unique", Boolean.toString(unique));
    }

    private static void register(String id, SkillTier tier, String... connectedTo) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", tier).build();
        SkillTree.registerType(type);
        SkillTree.register(new SkillNode(id, type, List.of(connectedTo), 0f, 0f));
    }

    private String recordCarrying(String definitionId) {
        ShipSkillData data = NpcSkillTreeBuilder.rootedTree(SkillTree.get("root"), 1);
        data.allocate(SkillTree.get("socket_1"), 1);
        data.socketItem("socket_1", NpcSocketables.id(definitionId, 7L));
        String record = NpcTreeTag.encode(data);
        NpcTreeRecords.of(fleet.getMemoryWithoutUpdate()).put("flagship", record);
        return record;
    }

    @Test
    void onlyAFleetWhoseFlagshipCarriesAUniqueIsMarked() {
        NpcUniqueAlerts.markIfCarrying(fleet, recordCarrying("basic"));
        assertFalse(memoryValues.containsKey(NpcUniqueAlerts.CARRIER_KEY));

        NpcUniqueAlerts.markIfCarrying(fleet, NpcTreeRecords.NOT_LEVELLED);
        assertFalse(memoryValues.containsKey(NpcUniqueAlerts.CARRIER_KEY));

        NpcUniqueAlerts.markIfCarrying(fleet, recordCarrying("relic"));
        assertTrue(memoryValues.containsKey(NpcUniqueAlerts.CARRIER_KEY));
    }

    @Test
    void thePlayerIsToldOnceWhenTheCarrierComesIntoSensorRange() {
        NpcUniqueAlerts.markIfCarrying(fleet, recordCarrying("relic"));
        when(fleet.getVisibilityLevelToPlayerFleet()).thenReturn(VisibilityLevel.NONE);

        NpcUniqueAlerts.alertIfSensed(fleet);
        verify(ui, never()).addMessage(anyString(), any(Color.class), anyString(), anyString(), any(Color.class), any(Color.class));

        when(fleet.getVisibilityLevelToPlayerFleet()).thenReturn(VisibilityLevel.SENSOR_CONTACT);
        NpcUniqueAlerts.alertIfSensed(fleet);
        NpcUniqueAlerts.alertIfSensed(fleet);

        verify(ui, times(1)).addMessage(contains("Ancient Relic"), any(Color.class), eq("Ancient Relic"), eq("an unidentified fleet"),
                any(Color.class), any(Color.class));
    }

    @Test
    void anIdentifiedCarrierIsNamed() {
        NpcUniqueAlerts.markIfCarrying(fleet, recordCarrying("relic"));
        when(fleet.getVisibilityLevelToPlayerFleet()).thenReturn(VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS);

        NpcUniqueAlerts.alertIfSensed(fleet);

        verify(ui).addMessage(eq("Sensors detect Ancient Relic aboard the Hegemony Patrol."), any(Color.class), eq("Ancient Relic"),
                eq("Hegemony Patrol"), any(Color.class), any(Color.class));
    }

    @Test
    void anUnmarkedFleetIsNeverAnnounced() {
        when(fleet.getVisibilityLevelToPlayerFleet()).thenReturn(VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS);

        NpcUniqueAlerts.alertIfSensed(fleet);

        verify(ui, never()).addMessage(anyString(), any(Color.class), anyString(), anyString(), any(Color.class), any(Color.class));
    }
}
