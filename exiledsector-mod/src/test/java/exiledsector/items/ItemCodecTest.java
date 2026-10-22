package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import exiledlib.skills.skilleffect.EffectAliases;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCodecTest {

    private static final String PREFIXES = "FIGHTER_ARMOR_PERCENT:10:15; FLUX_DISSIPATION_MULT:4:6";
    private static final String SUFFIXES = "ARMOR_PERCENT:-1:1; HULL_MULT:0:6";

    private static final List<RolledModifier> COMMON = List.of(prefix("FIGHTER_ARMOR_PERCENT", 12f), suffix("HULL_MULT", 5f));
    private static final List<RolledModifier> RARE = List.of(prefix("FIGHTER_ARMOR_PERCENT", 12f), prefix("FLUX_DISSIPATION_MULT", 5.5f),
            suffix("ARMOR_PERCENT", -0.25f), suffix("HULL_MULT", 0.0001f));
    private static final List<RolledModifier> HYBRID = List.of(new RolledModifier("plated_frame", ModifierSlot.PREFIX,
            List.of(new RolledEffect("ARMOR_PERCENT", 5f), new RolledEffect("HULL_MULT", 3f))), suffix("HULL_MULT", 5f));

    private static final String COMMON_TEXT = "P=FIGHTER_ARMOR_PERCENT:12;S=HULL_MULT:5";
    private static final String RARE_TEXT = "P=FIGHTER_ARMOR_PERCENT:12;P=FLUX_DISSIPATION_MULT:5.5;S=ARMOR_PERCENT:-0.25;S=HULL_MULT:1.0E-4";
    private static final String HYBRID_TEXT = "P=plated_frame=ARMOR_PERCENT:5+HULL_MULT:3;S=HULL_MULT:5";
    private static final String RELEASED_COMMON_TEXT = "FIGHTER_ARMOR_PERCENT:12;HULL_MULT:5";
    private static final String RELEASED_RARE_TEXT = "FIGHTER_ARMOR_PERCENT:12;FLUX_DISSIPATION_MULT:5.5;ARMOR_PERCENT:-0.25;HULL_MULT:1.0E-4";

    private static final ItemData COMMON_ITEM =
            new ItemData("sub", 42L, COMMON, new FrozenName("FIGHTER_ARMOR_PERCENT", "HULL_MULT", null, null));
    private static final ItemData RARE_ITEM = new ItemData("sub", 42L, RARE, new FrozenName(null, null, "Askonia", "Doctrine"));
    private static final ItemData PRODUCT_ITEM =
            new ItemData("gadget", -9L, RARE, FrozenName.product("Sunny", "Brand", "Pal", "X-7"));
    private static final ItemData UNIQUE_ITEM = new ItemData("relic", 3L, COMMON, FrozenName.NONE);
    private static final ItemData HYBRID_ITEM = new ItemData("sub", 11L, HYBRID, new FrozenName("plated_frame", "HULL_MULT", null, null));
    private static final ItemData ASSEMBLED_ITEM =
            new ItemData("sub", 8L, List.of(), new FrozenName(null, null, "Sunny Pal", null));
    private static final ItemData UNNAMED_ITEM = new ItemData("removed", 9L, COMMON, null);

    private static final String COMMON_CARGO =
            "{\"np\":\"FIGHTER_ARMOR_PERCENT\",\"s\":42,\"d\":\"sub\",\"e\":\"" + COMMON_TEXT + "\",\"ns\":\"HULL_MULT\",\"n\":true}";
    private static final String RARE_CARGO = "{\"r2\":\"Doctrine\",\"s\":42,\"d\":\"sub\",\"e\":\"" + RARE_TEXT + "\",\"n\":true,\"r1\":\"Askonia\"}";
    private static final String PRODUCT_CARGO = "{\"r2\":\"Pal\",\"rb\":\"Brand\",\"s\":-9,\"d\":\"gadget\",\"e\":\"" + RARE_TEXT
            + "\",\"rm\":\"X-7\",\"n\":true,\"rp\":true,\"r1\":\"Sunny\"}";
    private static final String UNIQUE_CARGO = "{\"s\":3,\"d\":\"relic\",\"e\":\"" + COMMON_TEXT + "\",\"n\":true}";
    private static final String ASSEMBLED_CARGO = "{\"s\":8,\"d\":\"sub\",\"e\":\"\",\"n\":true,\"r1\":\"Sunny Pal\"}";
    private static final String UNNAMED_CARGO = "{\"s\":9,\"d\":\"removed\",\"e\":\"" + COMMON_TEXT + "\"}";

    @BeforeEach
    void registerDefinitions() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row("sub", "bridge", PREFIXES, SUFFIXES))
                .put(ItemFixtures.row("gadget", "bridge", PREFIXES, SUFFIXES))
                .put(ItemFixtures.row("relic", "bridge", PREFIXES, SUFFIXES)));
    }

    @AfterEach
    void clearDefinitions() {
        ItemDefinitions.clear();
    }

    private static RolledModifier prefix(String effectName, float magnitude) {
        return RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect(effectName, magnitude));
    }

    private static RolledModifier suffix(String effectName, float magnitude) {
        return RolledModifier.single(ModifierSlot.SUFFIX, new RolledEffect(effectName, magnitude));
    }

    private static ItemData npcItem(ItemData item) {
        return new ItemData(item.definitionId(), item.seed(), item.explicits(), null);
    }

    @Test
    void cargoItemsAreWrittenWithTheSlotAndIdOfEveryModifier() {
        assertEquals(COMMON_CARGO, COMMON_ITEM.toSpecialItem().getData());
        assertEquals(RARE_CARGO, RARE_ITEM.toSpecialItem().getData());
        assertEquals(PRODUCT_CARGO, PRODUCT_ITEM.toSpecialItem().getData());
        assertEquals(UNIQUE_CARGO, UNIQUE_ITEM.toSpecialItem().getData());
        assertEquals(ASSEMBLED_CARGO, ASSEMBLED_ITEM.toSpecialItem().getData());
        assertEquals(UNNAMED_CARGO, UNNAMED_ITEM.toSpecialItem().getData());
        assertEquals("sub|7", new ItemData("sub", 7L).toSpecialItem().getData());
        assertEquals(ItemData.ITEM_ID, COMMON_ITEM.toSpecialItem().getId());
    }

    @Test
    void npcIdsAreWrittenWithTheSlotAndIdOfEveryModifier() {
        assertEquals("npc:sub/42/" + COMMON_TEXT, NpcItems.id(COMMON_ITEM));
        assertEquals("npc:sub/42/" + RARE_TEXT, NpcItems.id(RARE_ITEM));
        assertEquals("npc:sub/11/" + HYBRID_TEXT, NpcItems.id(HYBRID_ITEM));
        assertEquals("npc:sub/8/", NpcItems.id(ASSEMBLED_ITEM));
        assertEquals("npc:sub/7", NpcItems.id(new ItemData("sub", 7L)));
        assertEquals("npc:sub/7", NpcItems.id("sub", 7L));
    }

    @Test
    void writtenCargoStringsDecodeToTheSameItems() {
        assertEquals(COMMON_ITEM, ItemData.parse(COMMON_CARGO));
        assertEquals(RARE_ITEM, ItemData.parse(RARE_CARGO));
        assertEquals(PRODUCT_ITEM, ItemData.parse(PRODUCT_CARGO));
        assertEquals(UNIQUE_ITEM, ItemData.parse(UNIQUE_CARGO));
        assertEquals(ASSEMBLED_ITEM, ItemData.parse(ASSEMBLED_CARGO));
        assertEquals(UNNAMED_ITEM, ItemData.parse(UNNAMED_CARGO));
    }

    @Test
    void releasedEffectOnlyStringsDecodeIntoModifiersWithSlotsFromTheDefinition() {
        assertEquals(COMMON_ITEM, ItemData.parse(COMMON_CARGO.replace(COMMON_TEXT, RELEASED_COMMON_TEXT)));
        assertEquals(RARE_ITEM, ItemData.parse(RARE_CARGO.replace(RARE_TEXT, RELEASED_RARE_TEXT)));
        assertEquals(npcItem(RARE_ITEM), NpcItems.item("npc:sub/42/" + RELEASED_RARE_TEXT));
        assertEquals(new ItemData("removed", 9L, List.of(suffix("FIGHTER_ARMOR_PERCENT", 12f), suffix("HULL_MULT", 5f)), null),
                ItemData.parse(UNNAMED_CARGO.replace(COMMON_TEXT, RELEASED_COMMON_TEXT)), "without a definition the slot falls back to suffix");
        assertEquals(List.of(ModifierSlot.PREFIX, ModifierSlot.PREFIX, ModifierSlot.SUFFIX),
                ItemCodec.decodeModifiers("BEAM_WEAPON_DAMAGE_PERCENT:12;FIGHTER_ARMOR_PERCENT:12;HULL_MULT:5", "sub").stream()
                        .map(RolledModifier::slot).toList(), "a retired effect before a prefix is a prefix, as in old saves");
    }

    @Test
    void frozenCargoStringsKeepTheirLenientReadingRules() {
        assertEquals(new ItemData("sub", 5L), ItemData.parse("{\"d\":\"sub\",\"s\":5}"));
        assertEquals(new ItemData("sub", 5L), ItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"BROKEN\",\"n\":true,\"r1\":\"A\"}"));
        assertEquals(new ItemData("sub", 5L, COMMON, null),
                ItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"" + COMMON_TEXT + "\",\"n\":false,\"r1\":\"A\"}"));
        assertEquals(new ItemData("sub", 5L, COMMON, new FrozenName(null, null, "A", null)),
                ItemData.parse("{\"d\":\"sub\",\"s\":\"5\",\"e\":\"" + COMMON_TEXT + "\",\"n\":true,\"r1\":\"A\",\"r2\":\"\"}"));
        assertEquals(new ItemData("sub", 5L, COMMON, FrozenName.product(null, null, null, null)),
                ItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"" + COMMON_TEXT + "\",\"n\":true,\"rp\":true,\"np\":\"X\"}"));
        assertNull(ItemData.parse("{\"s\":5}"));
        assertNull(ItemData.parse("{\"d\":\"\",\"s\":5}"));
        assertNull(ItemData.parse("{\"d\":\"sub\"}"));
        assertNull(ItemData.parse("{\"d\":\"sub\",\"s\":\"x\"}"));
        assertNull(ItemData.parse("{not json"));
    }

    @Test
    void savedSeedOnlyCargoStringsDecodeToTheSameItems() {
        assertEquals(new ItemData("sub", 7L), ItemData.parse("sub|7"));
        assertEquals(new ItemData("retired_item", -5L), ItemData.parse("retired_item|-5"));
        assertEquals(new ItemData("a|b", 7L), ItemData.parse("a|b|7"));
        assertEquals(new ItemData("a/b", 7L), ItemData.parse("a/b|7"));
        assertNull(ItemData.parse("|7"));
        assertNull(ItemData.parse("sub|"));
        assertNull(ItemData.parse("sub|x"));
        assertNull(ItemData.parse("no_seed"));
        assertNull(ItemData.parse(""));
        assertNull(ItemData.parse(null));
    }

    @Test
    void savedNpcIdsDecodeToTheSameItems() {
        assertEquals(npcItem(COMMON_ITEM), NpcItems.item("npc:sub/42/" + COMMON_TEXT));
        assertEquals(npcItem(RARE_ITEM), NpcItems.item("npc:sub/42/" + RARE_TEXT));
        assertEquals(npcItem(HYBRID_ITEM), NpcItems.item("npc:sub/11/" + HYBRID_TEXT));
        assertEquals(new ItemData("sub", 8L, List.of(), null), NpcItems.item("npc:sub/8/"));
        assertEquals(new ItemData("sub", 7L), NpcItems.item("npc:sub/7"));
        assertEquals(new ItemData("a/b", 7L), NpcItems.item("npc:a/b/7"));
        assertEquals(ItemFixtures.data("sub", 42L, List.of(new RolledEffect("B", 2f)), null), NpcItems.item("npc:sub/42/A:1/B:2"));
        assertEquals(ItemFixtures.data("sub", 42L, List.of(new RolledEffect("A", 1f), new RolledEffect("B", 2f)), null),
                NpcItems.item("npc:sub/42/A:1;;B:2;"));
    }

    @Test
    void brokenOrForeignNpcIdsStayUnresolved() {
        assertNull(NpcItems.item("npc:sub/42/BROKEN"));
        assertNull(NpcItems.item("npc:sub/42/A:x"));
        assertNull(NpcItems.item("npc:sub/42/:1"));
        assertNull(NpcItems.item("npc:sub/42/X=A:1"));
        assertNull(NpcItems.item("npc:sub/42/P==A:1"));
        assertNull(NpcItems.item("npc:sub/42/P=frame=A:1+B"));
        assertNull(NpcItems.item("npc:sub/x"));
        assertNull(NpcItems.item("npc:/7"));
        assertNull(NpcItems.item("npc:/7/A:1"));
        assertNull(NpcItems.item("npc:missing_seed"));
        assertNull(NpcItems.item("npc:"));
        assertNull(NpcItems.item("sub|7"));
        assertNull(NpcItems.item(COMMON_CARGO));
        assertNull(NpcItems.item("socketable_3"));
        assertNull(NpcItems.item(null));
        assertTrue(NpcItems.isNpcId("npc:sub/7"));
        assertFalse(NpcItems.isNpcId("socketable_3"));
        assertFalse(NpcItems.isNpcId(null));
    }

    @Test
    void everyWrittenFormRoundTripsThroughTheSharedDecoder() {
        for (ItemData item : List.of(COMMON_ITEM, RARE_ITEM, PRODUCT_ITEM, UNIQUE_ITEM, HYBRID_ITEM, ASSEMBLED_ITEM, UNNAMED_ITEM)) {
            assertEquals(item, ItemData.of(item.toSpecialItem()));
            assertEquals(npcItem(item), NpcItems.item(NpcItems.id(item)));
            assertEquals(npcItem(item), ItemCodec.decode(ItemCodec.npc(item)));
        }
        assertEquals(new ItemData("sub", 7L), ItemCodec.decode(ItemCodec.npc("sub", 7L)));
    }

    @Test
    void rolledModifierListsKeepTheirTextForm() {
        assertEquals(COMMON_TEXT, ItemCodec.modifiers(COMMON));
        assertEquals(RARE_TEXT, ItemCodec.modifiers(RARE));
        assertEquals(HYBRID_TEXT, ItemCodec.modifiers(HYBRID));
        assertEquals("", ItemCodec.modifiers(List.of()));
        assertEquals(RARE, ItemCodec.decodeModifiers(RARE_TEXT, "sub"));
        assertEquals(HYBRID, ItemCodec.decodeModifiers(HYBRID_TEXT, "sub"));
        assertEquals(List.of(), ItemCodec.decodeModifiers("", "sub"));
        assertNull(ItemCodec.decodeModifiers("A", "sub"));
        assertNull(ItemCodec.decodeModifiers(":1", "sub"));
        assertNull(ItemCodec.decodeModifiers("A:", "sub"));
        assertNull(ItemCodec.decodeModifiers(null, "sub"));
    }

    @Test
    void aSavedItemWithARenamedEffectDecodesToTheCurrentName() throws Exception {
        EffectAliases.register(new JSONArray().put(new JSONObject().put("alias", "OLD_HULL_MULT").put("effect", "HULL_MULT")));
        try {
            assertEquals(COMMON_ITEM, ItemCodec.decode(COMMON_CARGO.replace("HULL_MULT", "OLD_HULL_MULT")));
            assertEquals(COMMON_ITEM, ItemCodec.decode(COMMON_CARGO.replace(COMMON_TEXT, RELEASED_COMMON_TEXT)
                    .replace("HULL_MULT", "OLD_HULL_MULT")));
            assertEquals(List.of(suffix("HULL_MULT", 5f)), ItemCodec.decodeModifiers("S=OLD_HULL_MULT=OLD_HULL_MULT:5", "sub"));
        } finally {
            EffectAliases.clear();
        }
    }
}
