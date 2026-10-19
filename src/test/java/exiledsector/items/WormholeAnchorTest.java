package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.i18n.StyledText;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WormholeAnchorTest {

    private static final String HEGEMONY_ANCHOR = "wormhole_anchor_hegemony";
    private static final String ABYSS_ANCHOR = "wormhole_anchor_abyss";
    private static final String BEACON = "wormhole_anchor_exotech";

    private final CargoAPI cargo = mock(CargoAPI.class);
    private SkillNode socket;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        SkillType socketType = new SkillType.Builder("socket_type", "Socket", "", SkillTier.SOCKET).build();
        SkillType wormholeType = new SkillType.Builder("wormhole_exit_type", "Wormhole Exit", "", SkillTier.WORMHOLE_EXIT).build();
        socket = new SkillNode("socket", socketType, List.of(), 0f, 0f);
        SkillTree.register(socket);
        SkillTree.register(new SkillNode("hegemony_2", wormholeType, List.of(), 100f, 0f, SkillNodeDecoration.NONE, List.of("hegemony")));
        SkillTree.register(new SkillNode("exoship", wormholeType, List.of(), 200f, 0f, SkillNodeDecoration.NONE, List.of("exotech")));
        ItemDefinitions.register(new JSONArray().put(ItemFixtures.subroutineRow(ItemFixtures.SUBROUTINE)));
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2"))
                .put(anchorRow(ABYSS_ANCHOR, "abyss", "rat_abyss_wormhole_1"))
                .put(anchorRow(BEACON, "exotech", "exoship").put("unlock", ItemUnlock.RAT_PLAYER_EXOSHIP.id())));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        WormholeAnchorDefinitions.clear();
        ItemDefinitions.clear();
        ItemDrops.clear();
    }

    static JSONObject anchorRow(String id, String volume, String exitNode) throws Exception {
        return new JSONObject().put("id", id).put("volume", volume).put("exit_node", exitNode).put("name", id + " name")
                .put("icon", "graphics/icons/nav_relay.png").put("rarity", "10").put("description", "A navigation lock.");
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);
        return xstream;
    }

    private static Item anchor(String definitionId) {
        return ItemData.rolled(ItemDefinitions.get(definitionId), 5L).create("socketable_9");
    }

    @Test
    void anchorDefinitionsLoadAsTheirOwnTypeOutsideTheShipItemList() {
        ItemDefinition hegemony = ItemDefinitions.get(HEGEMONY_ANCHOR);

        assertEquals(ItemType.WORMHOLE_ANCHOR, hegemony.type());
        assertNull(hegemony.kind());
        assertFalse(hegemony.unique());
        assertTrue(hegemony.pool().isEmpty());
        assertEquals(3, ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR).size());
        assertTrue(ItemDefinitions.all().stream().noneMatch(definition -> definition.type() == ItemType.WORMHOLE_ANCHOR));
        assertEquals("hegemony", WormholeAnchorDefinitions.volume(HEGEMONY_ANCHOR));
        assertEquals("hegemony_2", WormholeAnchorDefinitions.exitNodeId(HEGEMONY_ANCHOR));
        assertEquals(ItemUnlock.RAT_PLAYER_EXOSHIP.id(), ItemDefinitions.get(BEACON).unlock());
    }

    @Test
    void anAnchorCarriesItsFactionColourAndCrestAndFramesInThatColour() throws Exception {
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2").put("color", "#F5961E").put("crest_faction", "hegemony"))
                .put(anchorRow(BEACON, "exotech", "exoship").put("color", "orange")));

        WormholeAnchor hegemony = assertInstanceOf(WormholeAnchor.class, anchor(HEGEMONY_ANCHOR));
        WormholeAnchor beacon = assertInstanceOf(WormholeAnchor.class, anchor(BEACON));

        assertEquals(new Color(245, 150, 30), hegemony.color());
        assertEquals(hegemony.color(), hegemony.frameColor());
        assertEquals("hegemony", hegemony.crestFactionId());
        assertEquals(WormholeAnchorDefinitions.DEFAULT_COLOR, beacon.color());
        assertNull(beacon.crestFactionId());
        assertEquals(WormholeAnchorDefinitions.DEFAULT_COLOR, WormholeAnchorDefinitions.style("missing_anchor").color());
        CargoIconLook look = CargoIconLooks.forItem(hegemony);
        assertEquals(hegemony.color(), look.color());
        assertEquals("hegemony", look.anchor().crestFactionId());
        assertTrue(look.framed());
        assertEquals(look.anchor(), look.unframed().anchor());
        assertNull(CargoIconLooks.forItem(anchor(ItemFixtures.SUBROUTINE)).anchor());
    }

    @Test
    void theLoaderSkipsRowsWithABadVolumeASecondAnchorForAVolumeOrATakenId() throws Exception {
        WormholeAnchorDefinitions.register(new JSONArray()
                .put(anchorRow("core_anchor", "core", "core_exit"))
                .put(anchorRow("made_up_anchor", "nowhere", "exit"))
                .put(anchorRow("first_hegemony", "hegemony", "hegemony_2"))
                .put(anchorRow("second_hegemony", "hegemony", "hegemony_2"))
                .put(anchorRow("no_exit", "pirate", ""))
                .put(anchorRow(ItemFixtures.SUBROUTINE, "luddic", "luddic_2")));

        assertEquals(List.of("first_hegemony"), ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR).stream().map(ItemDefinition::id).toList());
        assertEquals(ItemType.SUBROUTINE, ItemDefinitions.get(ItemFixtures.SUBROUTINE).type());
    }

    @Test
    void anAnchorRollsNothingAndIgnoresStoredTraits() {
        ItemData rolled = ItemData.rolled(ItemDefinitions.get(HEGEMONY_ANCHOR), 5L);
        Item anchor = rolled.withTraits(new ItemTraits(1, 0, CorruptionOutcome.NOTHING, null, true)).create("socketable_3");

        assertInstanceOf(WormholeAnchor.class, anchor);
        assertEquals(List.of(), rolled.explicits());
        assertEquals(List.of(), anchor.explicits());
        assertEquals(List.of(), anchor.implicits());
        assertEquals(List.of(), anchor.skillEffects());
        assertFalse(anchor.corrupted());
        assertFalse(anchor.mirrored());
        assertEquals(ItemRarity.COMMON, anchor.rarity());
        assertEquals(HEGEMONY_ANCHOR + " name", anchor.name());
    }

    @Test
    void anAnchorSurvivesTheCargoCodec() {
        SpecialItemData special = ItemData.rolled(ItemDefinitions.get(HEGEMONY_ANCHOR), 5L).toSpecialItem();

        assertEquals(ItemData.ITEM_ID, special.getId());
        ItemData decoded = ItemData.of(special);
        assertEquals(HEGEMONY_ANCHOR, decoded.definitionId());
        assertEquals(5L, decoded.seed());
        assertInstanceOf(WormholeAnchor.class, decoded.preview());
    }

    @Test
    void aSavedAnchorLoadsUnderItsOwnAliasAndComesBackAfterItsDefinitionWasMissing() throws Exception {
        ItemStore store = new ItemStore();
        Item stored = store.add(ItemDefinitions.get(HEGEMONY_ANCHOR), 5L);
        assertInstanceOf(WormholeAnchor.class, stored);

        String xml = xstream().toXML(store);
        assertTrue(xml.contains("<exiledSector.WormholeAnchor>"), xml);
        Item restored = ((ItemStore) xstream().fromXML(xml)).owned().get(0);
        assertInstanceOf(WormholeAnchor.class, restored);
        assertEquals(stored.id(), restored.id());

        WormholeAnchorDefinitions.clear();
        Item orphaned = ((ItemStore) xstream().fromXML(xml)).owned().get(0);
        assertInstanceOf(UnresolvedItem.class, orphaned);
        String orphanXml = xstream().toXML(orphaned);
        WormholeAnchorDefinitions.register(new JSONArray().put(anchorRow(HEGEMONY_ANCHOR, "hegemony", "hegemony_2")));
        assertInstanceOf(WormholeAnchor.class, xstream().fromXML(orphanXml));
    }

    @Test
    void noCurrencyCanBeUsedOnAnAnchorEvenOneExtendedToAnchors() {
        Item anchor = anchor(HEGEMONY_ANCHOR);
        ItemStore store = new ItemStore();
        for (ItemCurrency currency : ItemCurrencies.all()) {
            when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(currency.itemId()))).thenReturn(5f);
            assertFalse(ItemCrafting.canUse(currency, anchor, definition -> true), currency.key());
            assertNull(ItemCrafting.use(currency, anchor, CraftingMaterials.cargoOnly(cargo), store, new Random(1L), definition -> true),
                    currency.key());
        }
        assertTrue(ItemCurrencies.extend(CraftingCurrency.REPLICATION, ItemType.WORMHOLE_ANCHOR));
        try {
            assertFalse(ItemCrafting.canUse(CraftingCurrency.REPLICATION, anchor, definition -> true));
        } finally {
            ItemCurrencies.retract(CraftingCurrency.REPLICATION, ItemType.WORMHOLE_ANCHOR);
        }
        verify(cargo, never()).removeItems(Mockito.any(), Mockito.any(), anyFloat());
        assertTrue(ItemCurrencies.forType(ItemType.WORMHOLE_ANCHOR).isEmpty());
        assertFalse(ItemCrafting.takesCurrency(ItemType.WORMHOLE_ANCHOR));
    }

    @Test
    void anAnchorCannotBeDisassembled() {
        ItemStore store = new ItemStore();
        Item stored = store.add(ItemDefinitions.get(HEGEMONY_ANCHOR), 5L);
        Map<String, ShipSkillData> ships = new HashMap<>();

        assertFalse(ItemDisassembly.canDisassemble(stored));
        assertEquals(0, ItemDisassembly.disassemble(stored, CraftingMaterials.cargoOnly(cargo), store, ships));
        assertSame(stored, store.find(stored.id()));
        verify(cargo, never()).addSpecial(Mockito.any(), anyFloat());
    }

    @Test
    void anAnchorFitsAStandardTreeSocketOnlyWhileItsExitIsInTheActiveTree() {
        Item hegemony = anchor(HEGEMONY_ANCHOR);
        Item abyss = anchor(ABYSS_ANCHOR);
        SkillNode exit = SkillTree.get("hegemony_2");

        assertTrue(hegemony.canSocketInto(socket));
        assertFalse(hegemony.canSocketInto(exit));
        assertFalse(hegemony.canSocketInto((SkillNode) null));
        assertFalse(hegemony.canSocketInto(ShipSection.BRIDGE));
        assertFalse(abyss.canSocketInto(socket), "the abyss exit is not in the active tree, as when its area is off");
        assertTrue(SocketCustody.fitsTreeSocket(hegemony));
        assertTrue(SocketCustody.fitsTreeSocket(abyss));
    }

    @Test
    void anAnchorWhoseAreaIsOffExplainsWhyItCannotBeInstalled() {
        WormholeAnchor hegemony = (WormholeAnchor) anchor(HEGEMONY_ANCHOR);
        WormholeAnchor abyss = (WormholeAnchor) anchor(ABYSS_ANCHOR);

        assertNull(hegemony.socketRefusal());
        assertEquals(ABYSS_ANCHOR + " name can't be installed: it is inert while the Abyss volume is turned off.", abyss.socketRefusal());
        assertEquals(List.of("Leads to the Hegemony volume."), hegemony.statusLines().stream().map(StyledText::plain).toList());
        assertEquals(2, abyss.statusLines().size());
        assertEquals("wormhole anchor", abyss.kindName());
    }

    @Test
    void theTooltipNamesTheVolumeAndWhyAnAnchorIsInert() {
        List<String> hegemonyLines = anchor(HEGEMONY_ANCHOR).tooltipLines().stream().map(StyledText::plain).toList();
        List<String> abyssLines = anchor(ABYSS_ANCHOR).tooltipLines().stream().map(StyledText::plain).toList();
        List<String> beaconLines = anchor(BEACON).tooltipLines().stream().map(StyledText::plain).toList();

        assertEquals(List.of("A navigation lock.", "Leads to the Hegemony volume."), hegemonyLines);
        assertEquals("Leads to the Abyss volume.", abyssLines.get(1));
        assertTrue(abyssLines.get(2).startsWith("Inert while the Abyss volume is turned off."), abyssLines.get(2));
        assertEquals("Inert until you recover or own the Exoship.", beaconLines.get(beaconLines.size() - 1));
    }

    @Test
    void dropAndNpcPickersNeverPickAnAnchor() throws Exception {
        ItemDefinitions.register(List.of(ItemDefinitions.get(HEGEMONY_ANCHOR), ItemDefinitions.get(ItemFixtures.SUBROUTINE)));
        ItemDrops.register(new JSONArray().put(new JSONObject().put("site", "station").put("chances", "1;1;1").put("uniqueChance", "1")));
        Random random = new Random(3L);
        Set<String> anchorIds = Set.of(HEGEMONY_ANCHOR, ABYSS_ANCHOR, BEACON);

        for (int trial = 0; trial < 200; trial++) {
            assertNotAnchor(ItemDrops.pickBasic(random), anchorIds);
            assertNotAnchor(ItemDrops.pickUnique(random, definition -> true), anchorIds);
            assertNotAnchor(ItemDrops.pickSectionItemBasic(random), anchorIds);
            assertNotAnchor(ItemDrops.pickChart(random, definition -> true), anchorIds);
            assertNotAnchor(NpcItems.pickDefinition(random, definition -> true), anchorIds);
            for (ItemData dropped : ItemDrops.rollSite("station", random, ItemChances.of(1f), 1f, 1f, true).items()) {
                assertFalse(anchorIds.contains(dropped.definitionId()), dropped.definitionId());
            }
        }
        assertFalse(ItemDrops.canBeSynthesised(ItemDefinitions.get(HEGEMONY_ANCHOR)));
    }

    private static void assertNotAnchor(ItemDefinition picked, Set<String> anchorIds) {
        assertTrue(picked == null || !anchorIds.contains(picked.id()) && picked.type() != ItemType.WORMHOLE_ANCHOR,
                picked == null ? "" : picked.id());
    }

    @Test
    void anAnchorIsAWormholeAnchorNoun() {
        assertEquals("wormhole anchor", ItemDefinitions.get(HEGEMONY_ANCHOR).noun());
        assertNotNull(ItemDefinitions.get(HEGEMONY_ANCHOR).displayName());
    }
}
