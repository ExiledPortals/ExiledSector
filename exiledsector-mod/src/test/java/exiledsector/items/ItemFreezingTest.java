package exiledsector.items;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledlib.items.FrozenName;
import exiledlib.items.Item;
import exiledlib.items.ItemData;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemModifiers;
import exiledlib.items.ItemNames;
import exiledlib.items.ItemStore;
import exiledlib.items.RolledEffect;
import exiledlib.items.RolledModifier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static exiledsector.items.ItemFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFreezingTest {

    private static final String BEFORE_PREFIXES = "FIGHTER_ARMOR_PERCENT:10:15; FLUX_DISSIPATION_MULT:4:6";
    private static final String BEFORE_SUFFIXES = "HULL_MULT:4:6; ARMOR_PERCENT:6:9";
    private static final List<RolledEffect> COMMON = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f), new RolledEffect("HULL_MULT", 5f));
    private static final List<RolledEffect> RARE = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f),
            new RolledEffect("FLUX_DISSIPATION_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f));

    @BeforeEach
    void setUp() throws Exception {
        registerBridge(BEFORE_PREFIXES, BEFORE_SUFFIXES);
        registerWords("Askonia", "Corvus");
        ItemFixtures.registerModifierNames("FIGHTER_ARMOR_PERCENT,prefix,FIGHTER_ARMOR_PERCENT:10:15,Plated",
                "HULL_MULT,suffix,HULL_MULT:4:6,Fortitude");
    }

    @AfterEach
    void tearDown() {
        NpcItems.clearCache();
        ItemNames.clear();
        ItemModifiers.clear();
        ItemDefinitions.clear();
    }

    private static void registerBridge(String prefixes, String suffixes) throws Exception {
        ItemDefinitions.register(new JSONArray().put(row("sub", "bridge", prefixes, suffixes)
                .put("name", "Military-grade Bridge")));
    }

    private static void registerWords(String... first) throws Exception {
        ItemNames.registerWords(new JSONObject().put("sub", new JSONObject().put("style", "codename")
                .put("first", new JSONArray(List.of(first))).put("second", new JSONArray(List.of("Doctrine")))));
    }

    private static ItemData frozen(List<RolledEffect> effects) {
        ItemDefinition definition = ItemDefinitions.get("sub");
        List<RolledModifier> modifiers = RolledModifier.fromLegacyEffects(definition, effects);
        return new ItemData("sub", 42L, modifiers, ItemNames.freeze(definition, 42L, modifiers));
    }

    private static void rebalance() throws Exception {
        registerBridge("FLUX_DISSIPATION_MULT:8:10", "ARMOR_PERCENT:10:12");
        registerWords("Zagan", "Thule", "Eos", "Magec", "Duzahk");
    }

    private static void registerGadget() throws Exception {
        ItemDefinitions.register(new JSONArray().put(row("gadget", "bridge", BEFORE_PREFIXES, BEFORE_SUFFIXES)
                .put("name", "Consumer-grade Bridge")));
        ItemNames.registerWords(new JSONObject().put("gadget", new JSONObject().put("style", "product")
                .put("first", new JSONArray(List.of("Sunny"))).put("second", new JSONArray(List.of("Pal")))));
    }

    private static String withoutElement(String xml, String element) {
        String opening = "<" + element + ">";
        String closing = "</" + element + ">";
        StringBuilder remaining = new StringBuilder(xml);
        int start = remaining.indexOf(opening);
        while (start >= 0) {
            remaining.delete(start, remaining.indexOf(closing, start) + closing.length());
            start = remaining.indexOf(opening, start);
        }
        return remaining.toString();
    }

    @Test
    void consumerProductNamesFreezeTheirWordsSoTheyCanBeTranslated() throws Exception {
        registerGadget();
        List<RolledModifier> rare = RolledModifier.fromLegacyEffects(ItemDefinitions.get("gadget"), RARE);
        ItemData item = new ItemData("gadget", 42L, rare, ItemNames.freeze(ItemDefinitions.get("gadget"), 42L, rare));

        assertEquals(FrozenName.product("Sunny", null, "Pal", null), item.frozenName());
        assertEquals(item, ItemData.of(item.toSpecialItem()));
        assertEquals("Sunny Pal", item.preview().name());
    }

    @Test
    void anAssembledProductNameFromAnOlderSaveIsRefrozenFromItsSeed() throws Exception {
        registerGadget();
        Item owned = Item.withEffects("socketable_1", "gadget", 42L, RARE);
        owned.freezeName(new FrozenName(null, null, "Sunny Pal", null));

        assertEquals("Sunny Pal", owned.name());
        assertEquals(FrozenName.product("Sunny", null, "Pal", null), owned.frozenName());
    }

    @Test
    void aFrozenCargoItemSurvivesTheSaveFormatWithItsRollsAndName() {
        ItemData item = frozen(RARE);

        ItemData restored = ItemData.of(item.toSpecialItem());

        assertEquals(item, restored);
        assertEquals(RARE, RolledModifier.lines(restored.explicits()));
    }

    @Test
    void droppedItemsAreFrozenWhileOldCargoDataStillLoadsAsLegacy() {
        ItemData dropped = ItemData.rolled(ItemDefinitions.get("sub"), 7L);
        ItemData legacy = ItemData.parse("sub|7");

        assertNotNull(dropped.explicits());
        assertNotNull(dropped.frozenName());
        assertEquals(new ItemData("sub", 7L), legacy);
        assertNull(legacy.explicits());
        assertEquals(RolledModifier.lines(dropped.explicits()), legacy.preview().effects());
    }

    @Test
    void aRebalanceLeavesFrozenCargoItemsAndTheirNamesAlone() throws Exception {
        ItemData common = frozen(COMMON);
        ItemData rare = frozen(RARE);
        String commonName = common.preview().name();
        String rareName = rare.preview().name();

        rebalance();
        ItemStore store = new ItemStore();
        Item claimedCommon = store.add(ItemData.of(common.toSpecialItem()));
        Item claimedRare = store.add(ItemData.of(rare.toSpecialItem()));

        assertEquals("Plated officer of Fortitude", commonName);
        assertEquals(COMMON, claimedCommon.effects());
        assertEquals(commonName, claimedCommon.name());
        assertEquals(RARE, claimedRare.effects());
        assertEquals(rareName, claimedRare.name());
    }

    @Test
    void anOwnedHullItemFromAnOlderSaveFreezesItsNameOnceAndKeepsIt() throws Exception {
        Item owned = Item.withEffects("socketable_1", "sub", 42L, RARE);
        String before = owned.name();

        rebalance();

        assertEquals(before, owned.name());
        assertNotEquals(before, Item.withEffects("socketable_2", "sub", 42L, RARE).name());
    }

    @Test
    void frozenNamesAreSavedAndOlderSavesWithoutThemFreezeOnLoad() throws Exception {
        ItemStore store = new ItemStore();
        Item saved = store.add(frozen(RARE));
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);

        String xml = xstream.toXML(store);
        String olderXml = withoutElement(xml, "frozenName");
        rebalance();
        ItemStore loaded = (ItemStore) xstream.fromXML(xml);

        assertTrue(xml.contains("<exiledSector.FrozenName") || xml.contains("<frozenName>"), xml);
        assertEquals(saved.name(), loaded.owned().get(0).name());
        ItemStore older = (ItemStore) xstream.fromXML(olderXml);
        older.freezeNames();
        String frozenOnLoad = older.owned().get(0).name();
        registerWords("Valhalla");
        assertEquals(frozenOnLoad, older.owned().get(0).name());
    }

    @Test
    void missingNameWordsAreNotFrozenSoTheNameAppearsOnceTheWordsLoad() throws Exception {
        ItemNames.registerWords(new JSONObject());
        Item owned = Item.withEffects("socketable_1", "sub", 42L, RARE);
        ItemData dropped = ItemData.of(frozen(RARE).toSpecialItem());
        assertEquals("Military-grade Bridge", owned.name());

        registerWords("Askonia", "Corvus");

        assertNull(dropped.frozenName());
        assertNotEquals("Military-grade Bridge", owned.name());
        assertEquals(frozen(RARE).preview().name(), owned.name());
    }

    @Test
    void anNpcItemWithNoEffectsStaysFrozenAsNoEffects() throws Exception {
        String id = NpcItems.id(new ItemData("sub", 42L, List.of(), null));

        assertEquals("npc:sub/42/", id);
        assertEquals(List.of(), RolledModifier.lines(NpcItems.item(id).explicits()));
    }

    @Test
    void npcItemsCarryTheirRollsInTheirIdAndOldIdsStillResolve() throws Exception {
        ItemData item = frozen(RARE);
        String id = NpcItems.id(item);

        rebalance();
        ItemData parsed = NpcItems.item(id);

        assertTrue(id.startsWith("npc:sub/42/"), id);
        assertEquals(RARE, RolledModifier.lines(parsed.explicits()));
        assertEquals(RARE, ItemStore.lookup(id).effects());
        ItemData legacy = NpcItems.item("npc:sub/42");
        assertEquals(new ItemData("sub", 42L), legacy);
        assertNull(NpcItems.item("npc:sub/42/BROKEN"));
    }
}
