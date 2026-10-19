package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.i18n.StyledText;
import exiledsector.skills.AnchorLinks;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.unlock.NodeGates;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WormholeAnchorSocketingTest {

    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";
    private static final String ABYSS_ANCHOR = "wormhole_anchor_abyss";
    private static final String KESTEVEN_ANCHOR = "wormhole_anchor_kesteven";
    private static final String GATE_TAG = "gate_test";
    private static final String GATE_FLAG = "$test_abyssOpen";

    private final Map<String, Item> itemsById = new HashMap<>();
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private MemoryAPI sectorMemory;
    private SkillNode root;
    private SkillNode socket;
    private SkillNode secondSocket;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        sectorMemory = mock(MemoryAPI.class);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        NodeGates.register(new JSONObject("{\"" + GATE_TAG + "\": {\"unlockConditions\": [{\"type\": \"memoryFlag\", \"key\": \"" + GATE_FLAG + "\"}]}}"));

        SkillTree.clearNodes();
        SkillType exitType = new SkillType.Builder("wormhole_exit", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT).build();
        SkillType socketType = new SkillType.Builder("socket", "Socket", "", SkillTier.SOCKET).build();
        root = new SkillNode("root", new SkillType.Builder("root_type", "Root", "", SkillTier.ROOT).build(), List.of(), 0f, 0f);
        socket = new SkillNode("socket", socketType, List.of("root"), 0f, 0f, SkillNodeDecoration.NONE, List.of("core"));
        secondSocket = new SkillNode("socket_b", socketType, List.of("root"), 0f, 0f, SkillNodeDecoration.NONE, List.of("core"));
        for (SkillNode node : List.of(root, socket, secondSocket,
                new SkillNode("hegemony_2", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("hegemony")),
                new SkillNode("rat_abyss_wormhole_1", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("abyss", GATE_TAG)))) {
            SkillTree.register(node);
        }
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(WormholeAnchorTest.anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2").put("drop_chance", "0.06"))
                .put(WormholeAnchorTest.anchorRow(ABYSS_ANCHOR, "abyss", "rat_abyss_wormhole_1")
                        .put("drop_factions", "rat_abyssals;rat_abyssals_deep").put("drop_chance", "0.1"))
                .put(WormholeAnchorTest.anchorRow(KESTEVEN_ANCHOR, "kesteven", "kesteven_wormhole1").put("drop_factions", "kesteven")
                        .put("drop_chance", "0.2")));
        NpcFactionVolumes.register(new JSONArray("[{\"faction\": \"hegemony\", \"region\": \"hegemony\"}]"));
        AnchorLinks.install(new AnchorLinks.ExitLookup() {
            @Override
            public String exitNodeId(String socketedItemId) {
                return itemsById.get(socketedItemId) instanceof WormholeAnchor anchor ? anchor.linkedExitNodeId() : null;
            }

            @Override
            public boolean canOpen(String socketedItemId) {
                return itemsById.get(socketedItemId) instanceof WormholeAnchor anchor && anchor.canOpen(Global.getSector());
            }

            @Override
            public int revision() {
                return 0;
            }
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        AnchorLinks.install(null);
        NodeGates.register(new JSONObject("{}"));
        NpcFactionVolumes.clear();
        WormholeAnchorDefinitions.clear();
        SkillTree.clearNodes();
        globalMock.close();
        lunaSettingsMock.close();
    }

    private WormholeAnchor anchor(String definitionId, String itemId) {
        WormholeAnchor anchor = (WormholeAnchor) ItemData.rolled(ItemDefinitions.get(definitionId), 5L).create(itemId);
        itemsById.put(itemId, anchor);
        return anchor;
    }

    private ShipSkillData shipWithSocket() {
        ShipSkillData data = new ShipSkillData();
        data.chooseStartingRoot(root);
        data.allocate(socket, 1);
        return data;
    }

    private static List<String> plain(List<StyledText> lines) {
        return lines.stream().map(StyledText::plain).toList();
    }

    @Test
    void anAnchorLinksWhileItsAreaIsOnButOnlyOpensOnceItsGateIsMet() {
        WormholeAnchor hegemony = anchor(HEGEMONY_ANCHOR, "socketable_1");
        WormholeAnchor abyss = anchor(ABYSS_ANCHOR, "socketable_2");
        WormholeAnchor kesteven = anchor(KESTEVEN_ANCHOR, "socketable_3");

        assertTrue(hegemony.canOpen(Global.getSector()));
        assertEquals("rat_abyss_wormhole_1", abyss.linkedExitNodeId());
        assertEquals(WormholeAnchor.Inertness.GATED, abyss.inertness(Global.getSector()));
        assertFalse(abyss.canOpen(Global.getSector()));
        assertNull(kesteven.linkedExitNodeId());
        assertEquals(WormholeAnchor.Inertness.AREA_OFF, kesteven.inertness(Global.getSector()));

        when(sectorMemory.getBoolean(GATE_FLAG)).thenReturn(true);

        assertTrue(abyss.canOpen(Global.getSector()));
    }

    @Test
    void aGatedAnchorNamesItsGateAndFallsBackToTheVolumeForAnUnknownOne() {
        WormholeAnchor abyss = anchor(ABYSS_ANCHOR, "socketable_2");

        assertEquals(List.of("Leads to the Abyss volume.", "Inert until the way into the Abyss volume is unlocked."), plain(abyss.statusLines()));
    }

    @Test
    void theSocketTooltipSaysWhetherTheWormholeIsOpenWaitingForASlotOrAlreadyAnchored() {
        anchor(HEGEMONY_ANCHOR, "socketable_1");
        anchor(HEGEMONY_ANCHOR, "socketable_4");
        ShipSkillData data = shipWithSocket();
        data.allocate(secondSocket, 1);
        data.socketItem("socket", "socketable_1");
        data.socketItem("socket_b", "socketable_4");
        WormholeAnchor first = (WormholeAnchor) itemsById.get("socketable_1");
        WormholeAnchor second = (WormholeAnchor) itemsById.get("socketable_4");

        assertEquals("Closed: opening the wormhole to the Hegemony volume needs a free node slot.",
                plain(first.socketStatusLines(data, "socket")).get(1));

        data.openAnchorWormholes(100);

        assertEquals("The wormhole to the Hegemony volume is open. Ctrl click the socket to travel through it.",
                plain(first.socketStatusLines(data, "socket")).get(1));
        assertEquals("Inert: another anchor on this ship already opens the Hegemony volume.",
                plain(second.socketStatusLines(data, "socket_b")).get(1));
    }

    @Test
    void aSecondAnchorForAVolumeIsRefusedButSwappingInTheSameSocketIsNot() {
        Map<String, String> volumeByItemId = Map.of("socketable_1", "hegemony", "socketable_2", "abyss");
        Map<String, String> socketed = new LinkedHashMap<>(Map.of("socket", "socketable_1", "socket_b", "socketable_2"));

        assertTrue(WormholeAnchorSocketLimits.volumeTaken("hegemony", "socketable_9", "socket_c", socketed, volumeByItemId::get));
        assertFalse(WormholeAnchorSocketLimits.volumeTaken("hegemony", "socketable_9", "socket", socketed, volumeByItemId::get));
        assertFalse(WormholeAnchorSocketLimits.volumeTaken("hegemony", "socketable_1", "socket_c", socketed, volumeByItemId::get));
        assertFalse(WormholeAnchorSocketLimits.volumeTaken("pirate", "socketable_9", "socket_c", socketed, volumeByItemId::get));
    }

    @Test
    void anAnchorAloneOnItsShipIsNeverRefused() {
        assertNull(WormholeAnchorSocketLimits.refusal(anchor(HEGEMONY_ANCHOR, "socketable_1"), "socket", Map.of()));
    }

    @Test
    void theLoadSweepKeepsTheFirstAnchorPerVolumeBySocketId() {
        ItemStore store = new ItemStore();
        Item first = store.add(ItemDefinitions.get(HEGEMONY_ANCHOR), 1L);
        Item second = store.add(ItemDefinitions.get(HEGEMONY_ANCHOR), 2L);
        store.add(ItemDefinitions.get(ABYSS_ANCHOR), 3L);
        ShipSkillData data = shipWithSocket();
        data.allocate(secondSocket, 1);
        data.socketItem("socket_b", first.id());
        data.socketItem("socket", second.id());

        List<Item> returned = WormholeAnchorSocketLimits.unsocketExtras(Map.of("ship", data), store);

        assertEquals(List.of(first), returned);
        assertEquals(second.id(), data.getSocketedItem("socket"));
        assertNull(data.getSocketedItem("socket_b"));
    }

    @Test
    void anAnchorDropsFromItsVolumesFactionsAndItsExtraFactionsScaledByDefeatedDeploymentPoints() {
        FactionFleetDrops.Source hegemony = FactionFleetDrops.source(HEGEMONY_ANCHOR);
        FactionFleetDrops.Source abyss = FactionFleetDrops.source(ABYSS_ANCHOR);

        assertEquals(0.06f, FactionFleetDrops.chance(hegemony, Map.of("hegemony", 100f)), 1e-6f);
        assertEquals(0f, FactionFleetDrops.chance(hegemony, Map.of("tritachyon", 500f)), 1e-6f);
        assertEquals(0.15f, FactionFleetDrops.chance(abyss, Map.of("rat_abyssals", 100f, "rat_abyssals_deep", 50f)), 1e-6f);
        assertEquals(FactionFleetDrops.MAX_CHANCE_PER_BATTLE, FactionFleetDrops.chance(abyss, Map.of("rat_abyssals", 10000f)), 1e-6f);
    }

    @Test
    void aBadDropChanceKeepsTheAnchorButNeverDropsIt() throws Exception {
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(WormholeAnchorTest.anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2").put("drop_chance", "6%"))
                .put(WormholeAnchorTest.anchorRow(ABYSS_ANCHOR, "abyss", "rat_abyss_wormhole_1").put("drop_chance", "-1")));

        assertEquals("hegemony_2", WormholeAnchorDefinitions.exitNodeId(HEGEMONY_ANCHOR));
        assertEquals(0f, FactionFleetDrops.source(HEGEMONY_ANCHOR).chancePer100DeploymentPoints());
        assertEquals(0f, FactionFleetDrops.source(ABYSS_ANCHOR).chancePer100DeploymentPoints());
        assertFalse(WormholeAnchorDefinitions.isValidDropChance("6%"));
        assertTrue(WormholeAnchorDefinitions.isValidDropChance(""));
    }

    @Test
    void aRolledDropRespectsTheAllowedCheckSoAnAnchorWhoseAreaIsOffNeverDrops() {
        Random random = new Random(7L);
        int kestevenDrops = 0;
        int hegemonyDrops = 0;
        for (int trial = 0; trial < 200; trial++) {
            for (ItemData dropped : FactionFleetDrops.roll(Map.of("kesteven", 10000f, "hegemony", 10000f), random, FactionFleetDrops::available)) {
                if (KESTEVEN_ANCHOR.equals(dropped.definitionId())) {
                    kestevenDrops++;
                } else if (HEGEMONY_ANCHOR.equals(dropped.definitionId())) {
                    hegemonyDrops++;
                }
            }
        }

        assertEquals(0, kestevenDrops);
        assertTrue(hegemonyDrops > 50 && hegemonyDrops < 150, String.valueOf(hegemonyDrops));
        assertEquals(Set.of(), Set.copyOf(FactionFleetDrops.roll(Map.of(), random, definition -> true)));
    }
}
