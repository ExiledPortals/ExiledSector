package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.effects.SkillTreeLootSource;
import exiledsector.skills.AnchorLinks;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.tags.SkillTags;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnchorJanusGateTest {

    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";
    private static final String BEACON = "wormhole_anchor_exotech";

    private final Map<String, Item> itemsById = new HashMap<>();
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockedStatic<Global> globalMock;
    private MemoryAPI sectorMemory;
    private Boolean allowBeforeJanus;
    private SkillNode root;
    private SkillNode socket;

    @BeforeEach
    void setUp() throws Exception {
        allowBeforeJanus = null;
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), anyString())).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getBoolean(anyString(), Mockito.eq(AnchorJanusGate.ALLOW_BEFORE_JANUS_FIELD_ID)))
                .thenAnswer(invocation -> allowBeforeJanus);
        sectorMemory = mock(MemoryAPI.class);
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);

        SkillTree.clearNodes();
        SkillType exitType = new SkillType.Builder("wormhole_exit", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT).build();
        SkillType socketType = new SkillType.Builder("socket", "Socket", "", SkillTier.SOCKET).build();
        root = new SkillNode("root", new SkillType.Builder("root_type", "Root", "", SkillTier.ROOT).build(), List.of(), 0f, 0f);
        socket = new SkillNode("socket", socketType, List.of("root"), 0f, 0f, SkillNodeDecoration.NONE, List.of("core"));
        for (SkillNode node : List.of(root, socket,
                new SkillNode("hegemony_2", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of("hegemony")),
                new SkillNode("exoship", exitType, List.of(), 0f, 0f, SkillNodeDecoration.NONE, List.of(SkillTags.EXOTECH_REGION)))) {
            SkillTree.register(node);
        }
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2").put("drop_chance", "1").put("market_chance", "1"))
                .put(anchorRow(BEACON, SkillTags.EXOTECH_REGION, "exoship").put("drop_factions", "rat_exotech")
                        .put("drop_chance", "1").put("market_chance", "1")));
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
    void tearDown() {
        AnchorLinks.install(null);
        NpcFactionVolumes.clear();
        WormholeAnchorDefinitions.clear();
        SkillTree.clearNodes();
        globalMock.close();
        lunaSettingsMock.close();
    }

    private static JSONObject anchorRow(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id + " name")
                .put("icon", "graphics/icons/nav_relay.png").put("rarity", "10").put("description", "Opens a wormhole.");
    }

    private void janusObtained(boolean obtained) {
        when(sectorMemory.getBoolean(JanusDevice.FLAG)).thenReturn(obtained);
    }

    private WormholeAnchor anchor(String definitionId, String itemId) {
        WormholeAnchor anchor = (WormholeAnchor) ItemData.rolled(ItemDefinitions.get(definitionId), 5L).create(itemId);
        itemsById.put(itemId, anchor);
        return anchor;
    }

    private ShipSkillData shipWithSocketed(WormholeAnchor anchor) {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, socket, 1);
        data.socketItem(socket.getId(), anchor.id());
        return data;
    }

    private static Set<String> droppedIds(Map<String, Float> defeated) {
        Random random = new Random(3L);
        Set<String> droppedIds = new HashSet<>();
        for (int battle = 0; battle < 50; battle++) {
            FactionFleetDrops.roll(defeated, random, FactionFleetDrops::available).forEach(item -> droppedIds.add(item.definition().id()));
        }
        return droppedIds;
    }

    @Test
    void theGateOpensWithTheJanusDeviceOrTheToggle() {
        SectorAPI sector = Global.getSector();
        assertFalse(AnchorJanusGate.open(sector));

        janusObtained(true);
        assertTrue(AnchorJanusGate.open(sector));

        janusObtained(false);
        allowBeforeJanus = true;
        assertTrue(AnchorJanusGate.open(sector));
    }

    @Test
    void theBeaconIsExemptAndEveryOtherAnchorIsGated() {
        assertFalse(AnchorJanusGate.applies(BEACON));
        assertTrue(AnchorJanusGate.applies(HEGEMONY_ANCHOR));
        assertFalse(AnchorJanusGate.withholds(BEACON, Global.getSector()));
        assertTrue(AnchorJanusGate.withholds(HEGEMONY_ANCHOR, Global.getSector()));
    }

    @Test
    void anchorsNeitherDropNorStockBeforeJanusButTheBeaconStillDoes() {
        Map<String, Float> defeated = Map.of("hegemony", 10000f, "rat_exotech", 10000f);

        assertFalse(FactionFleetDrops.available(ItemDefinitions.get(HEGEMONY_ANCHOR)));
        assertTrue(FactionFleetDrops.available(ItemDefinitions.get(BEACON)));
        assertEquals(Set.of(BEACON), droppedIds(defeated));
        assertTrue(WormholeAnchorDefinitions.rollMarketStock("hegemony", new Random(3L)).isEmpty());
        assertEquals(List.of(ItemDefinitions.get(BEACON)), WormholeAnchorDefinitions.rollMarketStock("rat_exotech", new Random(3L)));

        janusObtained(true);

        assertTrue(FactionFleetDrops.available(ItemDefinitions.get(HEGEMONY_ANCHOR)));
        assertEquals(Set.of(BEACON, HEGEMONY_ANCHOR), droppedIds(defeated));
        assertEquals(List.of(ItemDefinitions.get(HEGEMONY_ANCHOR)), WormholeAnchorDefinitions.rollMarketStock("hegemony", new Random(3L)));
    }

    @Test
    void showcaseSalvageOffersOnlyTheBeaconBeforeJanus() {
        CargoAPI loot = mock(CargoAPI.class);
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);

        SkillTreeLootSource.INSTANCE.addShowcaseLoot(loot, new Random(3L));

        verify(loot, atLeastOnce()).addSpecial(added.capture(), anyFloat());
        List<String> anchorDefinitionIds = added.getAllValues().stream()
                .map(ItemData::of)
                .filter(item -> item != null && item.definition() != null && item.definition().type() == ItemType.WORMHOLE_ANCHOR)
                .map(item -> item.definition().id()).toList();
        assertEquals(List.of(BEACON), anchorDefinitionIds);
    }

    @Test
    void aHeldAnchorIsInertUntilJanusAndTheBeaconNeverIs() {
        WormholeAnchor hegemony = anchor(HEGEMONY_ANCHOR, "socketable_1");
        WormholeAnchor beacon = anchor(BEACON, "socketable_2");
        SectorAPI sector = Global.getSector();

        assertEquals(WormholeAnchor.Inertness.JANUS, hegemony.inertness(sector));
        assertFalse(hegemony.canOpen(sector));
        assertNull(beacon.inertness(sector));

        janusObtained(true);

        assertNull(hegemony.inertness(sector));
        assertTrue(hegemony.canOpen(sector));
    }

    @Test
    void noWormholeOpensBeforeJanusAndAnOpenOneStaysOpen() {
        ShipSkillData data = shipWithSocketed(anchor(HEGEMONY_ANCHOR, "socketable_1"));

        assertEquals(List.of(), ShipTrees.openAnchorWormholes(data, 100));
        assertFalse(data.isAllocated("hegemony_2"));

        janusObtained(true);
        assertEquals(List.of("hegemony_2"), ShipTrees.openAnchorWormholes(data, 100));

        janusObtained(false);
        assertEquals(List.of(), ShipTrees.openAnchorWormholes(data, 100));
        assertTrue(data.isAllocated("hegemony_2"));
    }

    @Test
    void npcTreesStillTakeTheirVolumesAnchorBeforeJanus() {
        assertNotNull(NpcItems.anchorFor("hegemony"));
    }

    @Test
    void theConsoleAndOtherItemTypesAreUnaffected() {
        ItemDefinition subroutine = mock(ItemDefinition.class);
        when(subroutine.rarity()).thenReturn(10f);
        when(subroutine.type()).thenReturn(ItemType.SUBROUTINE);
        when(subroutine.id()).thenReturn("some_subroutine");

        assertTrue(FactionFleetDrops.available(subroutine));
        verify(sectorMemory, Mockito.never()).getBoolean(any());
    }
}
