package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledlib.MockSector;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodeGatesTest {

    private static final String GATE_TAG = "gate_test";
    private static final String FLAG = "$test_gateOpen";
    private static final SkillType SMALL = new SkillType.Builder("small", "Small", "a.png", SkillTier.SMALL).build();

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockSector mockSector;
    private MemoryAPI sectorMemory;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        sectorMemory = mock(MemoryAPI.class);
        mockSector = new MockSector();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        NodeGates.register(new JSONObject("{\"" + GATE_TAG + "\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + FLAG + "\"}]}}"));
    }

    @AfterEach
    void tearDown() throws Exception {
        NodeGates.register(new JSONObject("{}"));
        mockSector.close();
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
    void aNodeWithTwoGatesStaysHiddenUntilBothAreOpen() throws Exception {
        String secondFlag = "$test_secondGateOpen";
        NodeGates.register(new JSONObject("{\"" + GATE_TAG + "\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + FLAG
                + "\"}]}, \"gate_second\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + secondFlag + "\"}]}}"));
        SkillNode twiceGated = node("stargazer", GATE_TAG, "gate_second");

        when(sectorMemory.getBoolean(FLAG)).thenReturn(true);
        assertTrue(NodeGates.isHidden(twiceGated, null));

        when(sectorMemory.getBoolean(FLAG)).thenReturn(false);
        when(sectorMemory.getBoolean(secondFlag)).thenReturn(true);
        assertTrue(NodeGates.isHidden(twiceGated, null));

        when(sectorMemory.getBoolean(FLAG)).thenReturn(true);
        assertFalse(NodeGates.isHidden(twiceGated, null));
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
        ShipSkillData shipData = new ShipSkillData();
        NodeEligibility.Context context = new NodeEligibility.Context(List.of(), null, null, type -> false, null, null,
                node -> NodeGates.isLocked(node, shipData));

        assertEquals(NodeEligibility.Kind.LOCKED, NodeEligibility.check(gated, null, context).kind());
    }

    @Test
    void npcTreesBuiltWithoutAGateCheckIgnoreThePlayersGates() {
        SkillNode gated = node("abyss", GATE_TAG);
        NodeEligibility.Context npcContext = new NodeEligibility.Context(List.of(), null, null, type -> false, null);

        assertTrue(NodeGates.isLocked(gated, null));
        assertNull(npcContext.gated());
    }

    @Test
    void aGateWithholdsItsAnchorsByDefaultAndCanHandThemOutFreely() throws Exception {
        NodeGates.register(new JSONObject("{\"" + GATE_TAG + "\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + FLAG
                + "\"}]}, \"gate_free\": {\"anchors\": \"free\", \"rewardAnchor\": true, \"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \""
                + FLAG + "\"}]}, \"gate_typo\": {\"anchors\": \"sometimes\", \"unlockConditions\": []}}"));

        assertEquals(NodeGates.AnchorRule.WITHHELD, NodeGates.gate(GATE_TAG).anchorRule());
        assertFalse(NodeGates.gate(GATE_TAG).rewardAnchor());
        assertEquals(NodeGates.AnchorRule.FREE, NodeGates.gate("gate_free").anchorRule());
        assertTrue(NodeGates.gate("gate_free").rewardAnchor());
        assertEquals(NodeGates.AnchorRule.WITHHELD, NodeGates.gate("gate_typo").anchorRule());
        assertNull(NodeGates.gate("gate_missing"));
    }

    @Test
    void onlyAClosedWithheldGateOnTheExitHoldsItsAnchorBack() throws Exception {
        NodeGates.register(new JSONObject("{\"" + GATE_TAG + "\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + FLAG
                + "\"}]}, \"gate_free\": {\"anchors\": \"free\", \"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + FLAG + "\"}]}}"));

        assertTrue(NodeGates.withholdsAnchor(node("abyss", GATE_TAG)));
        assertFalse(NodeGates.withholdsAnchor(node("nightcross", "gate_free")));
        assertFalse(NodeGates.withholdsAnchor(node("hegemony")));
        assertFalse(NodeGates.withholdsAnchor(null));

        when(sectorMemory.getBoolean(FLAG)).thenReturn(true);

        assertFalse(NodeGates.withholdsAnchor(node("abyss", GATE_TAG)));
    }

    @Test
    void registeringReplacesEveryPreviousGate() throws Exception {
        NodeGates.register(new JSONObject("{\"gate_other\": {\"unlockConditions\": []}}"));

        assertEquals(Map.of("gate_other", List.of()), NodeGates.all());
        assertFalse(NodeGates.isLocked(node(GATE_TAG), new ShipSkillData()));
    }

    @Test
    void aMalformedGateIsSkippedWithoutOpeningTheOthers() throws Exception {
        NodeGates.register(new JSONObject("{\"gate_broken\": {\"unlockConditions\": [{\"type\": \"noSuchCondition\"}]}, \"" + GATE_TAG
                + "\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + FLAG + "\"}]}}"));

        assertNull(NodeGates.gate("gate_broken"));
        assertTrue(NodeGates.isLocked(node(GATE_TAG), null));
    }
}
