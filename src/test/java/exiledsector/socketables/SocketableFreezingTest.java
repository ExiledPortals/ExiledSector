package exiledsector.socketables;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static exiledsector.socketables.SocketableFixtures.row;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableFreezingTest {

    private static final String BEFORE_PREFIXES = "FIGHTER_ARMOR_PERCENT:10:15; FLUX_DISSIPATION_MULT:4:6";
    private static final String BEFORE_SUFFIXES = "HULL_MULT:4:6; ARMOR_PERCENT:6:9";
    private static final List<RolledEffect> COMMON = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f), new RolledEffect("HULL_MULT", 5f));
    private static final List<RolledEffect> RARE = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f),
            new RolledEffect("FLUX_DISSIPATION_MULT", 5f), new RolledEffect("ARMOR_PERCENT", 7f));

    @BeforeEach
    void setUp() throws Exception {
        registerSubroutine(BEFORE_PREFIXES, BEFORE_SUFFIXES);
        registerWords("Askonia", "Corvus");
        SocketableNames.registerAffixes(new JSONArray()
                .put(new JSONObject().put("effect", "FIGHTER_ARMOR_PERCENT").put("prefix", "Plated").put("suffix", "Wings"))
                .put(new JSONObject().put("effect", "HULL_MULT").put("prefix", "Sturdy").put("suffix", "Fortitude")));
    }

    @AfterEach
    void tearDown() {
        NpcSocketables.clearCache();
        SocketableNames.clear();
        SocketableDefinitions.clear();
    }

    private static void registerSubroutine(String prefixes, String suffixes) throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("sub", "subroutine", prefixes, suffixes).put("grade", "military")
                .put("name", "Military-grade Domain Subroutine")));
    }

    private static void registerWords(String... first) throws Exception {
        SocketableNames.registerWords(new JSONObject().put("military", new JSONObject().put("style", "codename")
                .put("first", new JSONArray(List.of(first))).put("second", new JSONArray(List.of("Doctrine")))));
    }

    private static SocketableItemData frozen(List<RolledEffect> effects) {
        SocketableDefinition definition = SocketableDefinitions.get("sub");
        return new SocketableItemData("sub", 42L, effects, SocketableNames.freeze(definition, 42L, effects));
    }

    private static void rebalance() throws Exception {
        registerSubroutine("FLUX_DISSIPATION_MULT:8:10", "ARMOR_PERCENT:10:12");
        registerWords("Zagan", "Thule", "Eos", "Magec", "Duzahk");
    }

    private static void registerGadget() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(row("gadget", "subroutine", BEFORE_PREFIXES, BEFORE_SUFFIXES).put("grade", "consumer")
                .put("name", "Consumer-grade Domain Subroutine")));
        SocketableNames.registerWords(new JSONObject().put("consumer", new JSONObject().put("style", "product")
                .put("first", new JSONArray(List.of("Sunny"))).put("second", new JSONArray(List.of("Pal")))));
    }

    @Test
    void consumerProductNamesFreezeTheirWordsSoTheyCanBeTranslated() throws Exception {
        registerGadget();
        SocketableItemData item = new SocketableItemData("gadget", 42L, RARE, SocketableNames.freeze(SocketableDefinitions.get("gadget"), 42L, RARE));

        assertEquals(FrozenName.product("Sunny", null, "Pal", null), item.name());
        assertEquals(item, SocketableItemData.of(item.toSpecialItem()));
        assertEquals("Sunny Pal", item.preview().name());
    }

    @Test
    void anAssembledProductNameFromAnOlderSaveIsRefrozenFromItsSeed() throws Exception {
        registerGadget();
        Socketable owned = SocketableKind.SUBROUTINE.create("socketable_1", "gadget", 42L, RARE);
        owned.freezeName(new FrozenName(null, null, "Sunny Pal", null));

        assertEquals("Sunny Pal", owned.name());
        assertEquals(FrozenName.product("Sunny", null, "Pal", null), owned.frozenName());
    }

    @Test
    void aFrozenCargoItemSurvivesTheSaveFormatWithItsRollsAndName() {
        SocketableItemData item = frozen(RARE);

        SocketableItemData restored = SocketableItemData.of(item.toSpecialItem());

        assertEquals(item, restored);
        assertEquals(RARE, restored.effects());
    }

    @Test
    void droppedItemsAreFrozenWhileOldCargoDataStillLoadsAsLegacy() {
        SocketableItemData dropped = SocketableItemData.rolled(SocketableDefinitions.get("sub"), 7L);
        SocketableItemData legacy = SocketableItemData.parse("sub|7");

        assertNotNull(dropped.effects());
        assertNotNull(dropped.name());
        assertEquals(new SocketableItemData("sub", 7L), legacy);
        assertNull(legacy.effects());
        assertEquals(dropped.effects(), legacy.preview().effects());
    }

    @Test
    void aRebalanceLeavesFrozenCargoItemsAndTheirNamesAlone() throws Exception {
        SocketableItemData common = frozen(COMMON);
        SocketableItemData rare = frozen(RARE);
        String commonName = common.preview().name();
        String rareName = rare.preview().name();

        rebalance();
        SocketableStore store = new SocketableStore();
        Socketable claimedCommon = store.add(SocketableItemData.of(common.toSpecialItem()));
        Socketable claimedRare = store.add(SocketableItemData.of(rare.toSpecialItem()));

        assertEquals("Plated military-grade subroutine of Fortitude", commonName);
        assertEquals(COMMON, claimedCommon.effects());
        assertEquals(commonName, claimedCommon.name());
        assertEquals(RARE, claimedRare.effects());
        assertEquals(rareName, claimedRare.name());
    }

    @Test
    void anOwnedSocketableFromAnOlderSaveFreezesItsNameOnceAndKeepsIt() throws Exception {
        Socketable owned = SocketableKind.SUBROUTINE.create("socketable_1", "sub", 42L, RARE);
        String before = owned.name();

        rebalance();

        assertEquals(before, owned.name());
        assertNotEquals(before, SocketableKind.SUBROUTINE.create("socketable_2", "sub", 42L, RARE).name());
    }

    @Test
    void frozenNamesAreSavedAndOlderSavesWithoutThemFreezeOnLoad() throws Exception {
        SocketableStore store = new SocketableStore();
        Socketable saved = store.add(frozen(RARE));
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        SocketableSaveAliases.register(xstream);

        String xml = xstream.toXML(store);
        String olderXml = xml.replaceAll("(?s)\\s*<frozenName>.*?</frozenName>", "");
        rebalance();
        SocketableStore loaded = (SocketableStore) xstream.fromXML(xml);

        assertTrue(xml.contains("<exiledSector.FrozenName") || xml.contains("<frozenName>"), xml);
        assertEquals(saved.name(), loaded.owned().get(0).name());
        SocketableStore older = (SocketableStore) xstream.fromXML(olderXml);
        older.freezeNames();
        String frozenOnLoad = older.owned().get(0).name();
        registerWords("Valhalla");
        assertEquals(frozenOnLoad, older.owned().get(0).name());
    }

    @Test
    void missingNameWordsAreNotFrozenSoTheNameAppearsOnceTheWordsLoad() throws Exception {
        SocketableNames.registerWords(new JSONObject());
        Socketable owned = SocketableKind.SUBROUTINE.create("socketable_1", "sub", 42L, RARE);
        SocketableItemData dropped = SocketableItemData.of(frozen(RARE).toSpecialItem());
        assertEquals("Military-grade Domain Subroutine", owned.name());

        registerWords("Askonia", "Corvus");

        assertNull(dropped.name());
        assertNotEquals("Military-grade Domain Subroutine", owned.name());
        assertEquals(frozen(RARE).preview().name(), owned.name());
    }

    @Test
    void anNpcItemWithNoEffectsStaysFrozenAsNoEffects() throws Exception {
        String id = NpcSocketables.id(new SocketableItemData("sub", 42L, List.of(), null));

        assertEquals("npc:sub/42/", id);
        assertEquals(List.of(), NpcSocketables.item(id).effects());
    }

    @Test
    void npcItemsCarryTheirRollsInTheirIdAndOldIdsStillResolve() throws Exception {
        SocketableItemData item = frozen(RARE);
        String id = NpcSocketables.id(item);

        rebalance();
        SocketableItemData parsed = NpcSocketables.item(id);

        assertTrue(id.startsWith("npc:sub/42/"), id);
        assertEquals(RARE, parsed.effects());
        assertEquals(RARE, SocketableStore.lookup(id).effects());
        SocketableItemData legacy = NpcSocketables.item("npc:sub/42");
        assertEquals(new SocketableItemData("sub", 42L), legacy);
        assertNull(NpcSocketables.item("npc:sub/42/BROKEN"));
    }
}
