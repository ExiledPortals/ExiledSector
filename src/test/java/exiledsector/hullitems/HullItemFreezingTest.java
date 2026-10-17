package exiledsector.hullitems;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static exiledsector.hullitems.HullItemFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemFreezingTest {

    private static final String BEFORE_PREFIXES = "FIGHTER_ARMOR_PERCENT:10:15; FLUX_DISSIPATION_MULT:4:6";
    private static final String BEFORE_SUFFIXES = "HULL_MULT:4:6; ARMOR_PERCENT:6:9";
    private static final List<RolledEffect> COMMON = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f), new RolledEffect("HULL_MULT", 5f));
    private static final List<RolledEffect> RARE = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f),
            new RolledEffect("FLUX_DISSIPATION_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f));

    @BeforeEach
    void setUp() throws Exception {
        registerSubroutine(BEFORE_PREFIXES, BEFORE_SUFFIXES);
        registerWords("Askonia", "Corvus");
        HullItemFixtures.registerModifierNames("FIGHTER_ARMOR_PERCENT,prefix,FIGHTER_ARMOR_PERCENT:10:15,Plated",
                "HULL_MULT,suffix,HULL_MULT:4:6,Fortitude");
    }

    @AfterEach
    void tearDown() {
        NpcHullItems.clearCache();
        HullItemNames.clear();
        HullItemModifiers.clear();
        HullItemDefinitions.clear();
    }

    private static void registerSubroutine(String prefixes, String suffixes) throws Exception {
        HullItemDefinitions.register(new JSONArray().put(row("sub", "subroutine", prefixes, suffixes)
                .put("name", "Military-grade Domain Subroutine")));
    }

    private static void registerWords(String... first) throws Exception {
        HullItemNames.registerWords(new JSONObject().put("sub", new JSONObject().put("style", "codename")
                .put("first", new JSONArray(List.of(first))).put("second", new JSONArray(List.of("Doctrine")))));
    }

    private static HullItemData frozen(List<RolledEffect> effects) {
        HullItemDefinition definition = HullItemDefinitions.get("sub");
        List<RolledModifier> modifiers = RolledModifier.fromLegacyEffects(definition, effects);
        return new HullItemData("sub", 42L, modifiers, HullItemNames.freeze(definition, 42L, modifiers));
    }

    private static void rebalance() throws Exception {
        registerSubroutine("FLUX_DISSIPATION_MULT:8:10", "ARMOR_PERCENT:10:12");
        registerWords("Zagan", "Thule", "Eos", "Magec", "Duzahk");
    }

    private static void registerGadget() throws Exception {
        HullItemDefinitions.register(new JSONArray().put(row("gadget", "subroutine", BEFORE_PREFIXES, BEFORE_SUFFIXES)
                .put("name", "Consumer-grade Domain Subroutine")));
        HullItemNames.registerWords(new JSONObject().put("gadget", new JSONObject().put("style", "product")
                .put("first", new JSONArray(List.of("Sunny"))).put("second", new JSONArray(List.of("Pal")))));
    }

    @Test
    void consumerProductNamesFreezeTheirWordsSoTheyCanBeTranslated() throws Exception {
        registerGadget();
        List<RolledModifier> rare = RolledModifier.fromLegacyEffects(HullItemDefinitions.get("gadget"), RARE);
        HullItemData item = new HullItemData("gadget", 42L, rare, HullItemNames.freeze(HullItemDefinitions.get("gadget"), 42L, rare));

        assertEquals(FrozenName.product("Sunny", null, "Pal", null), item.frozenName());
        assertEquals(item, HullItemData.of(item.toSpecialItem()));
        assertEquals("Sunny Pal", item.preview().name());
    }

    @Test
    void anAssembledProductNameFromAnOlderSaveIsRefrozenFromItsSeed() throws Exception {
        registerGadget();
        HullItem owned = HullItem.withEffects("socketable_1", "gadget", 42L, RARE);
        owned.freezeName(new FrozenName(null, null, "Sunny Pal", null));

        assertEquals("Sunny Pal", owned.name());
        assertEquals(FrozenName.product("Sunny", null, "Pal", null), owned.frozenName());
    }

    @Test
    void aFrozenCargoItemSurvivesTheSaveFormatWithItsRollsAndName() {
        HullItemData item = frozen(RARE);

        HullItemData restored = HullItemData.of(item.toSpecialItem());

        assertEquals(item, restored);
        assertEquals(RARE, RolledModifier.lines(restored.explicits()));
    }

    @Test
    void droppedItemsAreFrozenWhileOldCargoDataStillLoadsAsLegacy() {
        HullItemData dropped = HullItemData.rolled(HullItemDefinitions.get("sub"), 7L);
        HullItemData legacy = HullItemData.parse("sub|7");

        assertNotNull(dropped.explicits());
        assertNotNull(dropped.frozenName());
        assertEquals(new HullItemData("sub", 7L), legacy);
        assertNull(legacy.explicits());
        assertEquals(RolledModifier.lines(dropped.explicits()), legacy.preview().effects());
    }

    @Test
    void aRebalanceLeavesFrozenCargoItemsAndTheirNamesAlone() throws Exception {
        HullItemData common = frozen(COMMON);
        HullItemData rare = frozen(RARE);
        String commonName = common.preview().name();
        String rareName = rare.preview().name();

        rebalance();
        HullItemStore store = new HullItemStore();
        HullItem claimedCommon = store.add(HullItemData.of(common.toSpecialItem()));
        HullItem claimedRare = store.add(HullItemData.of(rare.toSpecialItem()));

        assertEquals("Plated subroutine of Fortitude", commonName);
        assertEquals(COMMON, claimedCommon.effects());
        assertEquals(commonName, claimedCommon.name());
        assertEquals(RARE, claimedRare.effects());
        assertEquals(rareName, claimedRare.name());
    }

    @Test
    void anOwnedHullItemFromAnOlderSaveFreezesItsNameOnceAndKeepsIt() throws Exception {
        HullItem owned = HullItem.withEffects("socketable_1", "sub", 42L, RARE);
        String before = owned.name();

        rebalance();

        assertEquals(before, owned.name());
        assertNotEquals(before, HullItem.withEffects("socketable_2", "sub", 42L, RARE).name());
    }

    @Test
    void frozenNamesAreSavedAndOlderSavesWithoutThemFreezeOnLoad() throws Exception {
        HullItemStore store = new HullItemStore();
        HullItem saved = store.add(frozen(RARE));
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        HullItemSaveAliases.register(xstream);

        String xml = xstream.toXML(store);
        String olderXml = xml.replaceAll("(?s)\\s*<frozenName>.*?</frozenName>", "");
        rebalance();
        HullItemStore loaded = (HullItemStore) xstream.fromXML(xml);

        assertTrue(xml.contains("<exiledSector.FrozenName") || xml.contains("<frozenName>"), xml);
        assertEquals(saved.name(), loaded.owned().get(0).name());
        HullItemStore older = (HullItemStore) xstream.fromXML(olderXml);
        older.freezeNames();
        String frozenOnLoad = older.owned().get(0).name();
        registerWords("Valhalla");
        assertEquals(frozenOnLoad, older.owned().get(0).name());
    }

    @Test
    void missingNameWordsAreNotFrozenSoTheNameAppearsOnceTheWordsLoad() throws Exception {
        HullItemNames.registerWords(new JSONObject());
        HullItem owned = HullItem.withEffects("socketable_1", "sub", 42L, RARE);
        HullItemData dropped = HullItemData.of(frozen(RARE).toSpecialItem());
        assertEquals("Military-grade Domain Subroutine", owned.name());

        registerWords("Askonia", "Corvus");

        assertNull(dropped.frozenName());
        assertNotEquals("Military-grade Domain Subroutine", owned.name());
        assertEquals(frozen(RARE).preview().name(), owned.name());
    }

    @Test
    void anNpcItemWithNoEffectsStaysFrozenAsNoEffects() throws Exception {
        String id = NpcHullItems.id(new HullItemData("sub", 42L, List.of(), null));

        assertEquals("npc:sub/42/", id);
        assertEquals(List.of(), RolledModifier.lines(NpcHullItems.item(id).explicits()));
    }

    @Test
    void npcItemsCarryTheirRollsInTheirIdAndOldIdsStillResolve() throws Exception {
        HullItemData item = frozen(RARE);
        String id = NpcHullItems.id(item);

        rebalance();
        HullItemData parsed = NpcHullItems.item(id);

        assertTrue(id.startsWith("npc:sub/42/"), id);
        assertEquals(RARE, RolledModifier.lines(parsed.explicits()));
        assertEquals(RARE, HullItemStore.lookup(id).effects());
        HullItemData legacy = NpcHullItems.item("npc:sub/42");
        assertEquals(new HullItemData("sub", 42L), legacy);
        assertNull(NpcHullItems.item("npc:sub/42/BROKEN"));
    }
}
