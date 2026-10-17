package exiledsector.skills.unlock;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodeGatesTest {

    private static final String GATE_TAG = "gate_test";
    private static final String FLAG = "$test_gateOpen";
    private static final SkillType SMALL = new SkillType.Builder("small", "Small", "a.png", SkillTier.SMALL).build();

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private MemoryAPI sectorMemory;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        sectorMemory = mock(MemoryAPI.class);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        NodeGates.register(new JSONObject("{\"" + GATE_TAG + "\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + FLAG + "\"}]}}"));
    }

    @AfterEach
    void tearDown() throws Exception {
        NodeGates.register(new JSONObject("{}"));
        globalMock.close();
        lunaSettingsMock.close();
    }

    private static SkillNode node(String... tags) {
        return new SkillNode("node", SMALL, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of(tags));
    }

    @Test
    void aGatedNodeIsALockedSensorGhostUntilItsConditionIsMet() {
        SkillNode gated = node("abyss", GATE_TAG);

        assertTrue(NodeGates.isLocked(gated, null));
        assertTrue(NodeGates.isHidden(gated, null));

        when(sectorMemory.getBoolean(FLAG)).thenReturn(true);

        assertFalse(NodeGates.isLocked(gated, null));
        assertFalse(NodeGates.isHidden(gated, null));
    }

    @Test
    void nodesWithoutAGateTagAreNeverGated() {
        assertFalse(NodeGates.isLocked(node("abyss"), null));
        assertFalse(NodeGates.isLocked(node("abyss", "gate_without_config"), null));
    }

    @Test
    void theShowHiddenNodesSettingRevealsAGatedNodeButKeepsItLocked() {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", "exiledSector_showHiddenNodesByDefault")).thenReturn(true);
        SkillNode gated = node("abyss", GATE_TAG);

        assertTrue(NodeGates.isLocked(gated, null));
        assertFalse(NodeGates.isHidden(gated, null));
    }

    @Test
    void aGatedNodeCannotBeAllocatedEvenWhenItsTypeIsUnlocked() {
        SkillNode gated = node("abyss", GATE_TAG);
        NodeEligibility.Context context = new NodeEligibility.Context(List.of(), null, null, type -> false, null);

        assertEquals(NodeEligibility.Kind.LOCKED, NodeEligibility.check(gated, null, context).kind());
    }

    @Test
    void registeringReplacesEveryPreviousGate() throws Exception {
        NodeGates.register(new JSONObject("{\"gate_other\": {\"unlockConditions\": []}}"));

        assertEquals(Map.of("gate_other", List.of()), NodeGates.all());
        assertFalse(NodeGates.isLocked(node(GATE_TAG), new ShipSkillData()));
    }
}
