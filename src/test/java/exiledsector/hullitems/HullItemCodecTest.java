package exiledsector.hullitems;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemCodecTest {

    private static final List<RolledEffect> COMMON = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f), new RolledEffect("HULL_MULT", 5f));
    private static final List<RolledEffect> RARE = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f),
            new RolledEffect("FLUX_DISSIPATION_MULT", 5.5f), new RolledEffect("ARMOR_PERCENT", -0.25f), new RolledEffect("HULL_MULT", 0.0001f));
    private static final String COMMON_TEXT = "FIGHTER_ARMOR_PERCENT:12;HULL_MULT:5";
    private static final String RARE_TEXT = "FIGHTER_ARMOR_PERCENT:12;FLUX_DISSIPATION_MULT:5.5;ARMOR_PERCENT:-0.25;HULL_MULT:1.0E-4";

    private static final HullItemData COMMON_ITEM =
            new HullItemData("sub", 42L, COMMON, new FrozenName("FIGHTER_ARMOR_PERCENT", "HULL_MULT", null, null));
    private static final HullItemData RARE_ITEM = new HullItemData("sub", 42L, RARE, new FrozenName(null, null, "Askonia", "Doctrine"));
    private static final HullItemData PRODUCT_ITEM =
            new HullItemData("gadget", -9L, RARE, FrozenName.product("Sunny", "Brand", "Pal", "X-7"));
    private static final HullItemData UNIQUE_ITEM = new HullItemData("relic", 3L, COMMON, FrozenName.NONE);
    private static final HullItemData ASSEMBLED_ITEM =
            new HullItemData("sub", 8L, List.of(), new FrozenName(null, null, "Sunny Pal", null));
    private static final HullItemData UNNAMED_ITEM = new HullItemData("removed", 9L, COMMON, null);

    private static final String COMMON_CARGO =
            "{\"np\":\"FIGHTER_ARMOR_PERCENT\",\"s\":42,\"d\":\"sub\",\"e\":\"" + COMMON_TEXT + "\",\"ns\":\"HULL_MULT\",\"n\":true}";
    private static final String RARE_CARGO = "{\"r2\":\"Doctrine\",\"s\":42,\"d\":\"sub\",\"e\":\"" + RARE_TEXT + "\",\"n\":true,\"r1\":\"Askonia\"}";
    private static final String PRODUCT_CARGO = "{\"r2\":\"Pal\",\"rb\":\"Brand\",\"s\":-9,\"d\":\"gadget\",\"e\":\"" + RARE_TEXT
            + "\",\"rm\":\"X-7\",\"n\":true,\"rp\":true,\"r1\":\"Sunny\"}";
    private static final String UNIQUE_CARGO = "{\"s\":3,\"d\":\"relic\",\"e\":\"" + COMMON_TEXT + "\",\"n\":true}";
    private static final String ASSEMBLED_CARGO = "{\"s\":8,\"d\":\"sub\",\"e\":\"\",\"n\":true,\"r1\":\"Sunny Pal\"}";
    private static final String UNNAMED_CARGO = "{\"s\":9,\"d\":\"removed\",\"e\":\"" + COMMON_TEXT + "\"}";

    private static HullItemData npcItem(HullItemData item) {
        return new HullItemData(item.definitionId(), item.seed(), item.effects(), null);
    }

    @Test
    void cargoItemsAreWrittenExactlyAsTheReleasedBuildWroteThem() {
        assertEquals(COMMON_CARGO, COMMON_ITEM.toSpecialItem().getData());
        assertEquals(RARE_CARGO, RARE_ITEM.toSpecialItem().getData());
        assertEquals(PRODUCT_CARGO, PRODUCT_ITEM.toSpecialItem().getData());
        assertEquals(UNIQUE_CARGO, UNIQUE_ITEM.toSpecialItem().getData());
        assertEquals(ASSEMBLED_CARGO, ASSEMBLED_ITEM.toSpecialItem().getData());
        assertEquals(UNNAMED_CARGO, UNNAMED_ITEM.toSpecialItem().getData());
        assertEquals("sub|7", new HullItemData("sub", 7L).toSpecialItem().getData());
        assertEquals(HullItemData.ITEM_ID, COMMON_ITEM.toSpecialItem().getId());
    }

    @Test
    void npcIdsAreWrittenExactlyAsTheReleasedBuildWroteThem() {
        assertEquals("npc:sub/42/" + COMMON_TEXT, NpcHullItems.id(COMMON_ITEM));
        assertEquals("npc:sub/42/" + RARE_TEXT, NpcHullItems.id(RARE_ITEM));
        assertEquals("npc:gadget/-9/" + RARE_TEXT, NpcHullItems.id(PRODUCT_ITEM));
        assertEquals("npc:relic/3/" + COMMON_TEXT, NpcHullItems.id(UNIQUE_ITEM));
        assertEquals("npc:sub/8/", NpcHullItems.id(ASSEMBLED_ITEM));
        assertEquals("npc:sub/7", NpcHullItems.id(new HullItemData("sub", 7L)));
        assertEquals("npc:sub/7", NpcHullItems.id("sub", 7L));
    }

    @Test
    void savedFrozenCargoStringsDecodeToTheSameItems() {
        assertEquals(COMMON_ITEM, HullItemData.parse(COMMON_CARGO));
        assertEquals(RARE_ITEM, HullItemData.parse(RARE_CARGO));
        assertEquals(PRODUCT_ITEM, HullItemData.parse(PRODUCT_CARGO));
        assertEquals(UNIQUE_ITEM, HullItemData.parse(UNIQUE_CARGO));
        assertEquals(ASSEMBLED_ITEM, HullItemData.parse(ASSEMBLED_CARGO));
        assertEquals(UNNAMED_ITEM, HullItemData.parse(UNNAMED_CARGO));
    }

    @Test
    void frozenCargoStringsKeepTheirLenientReadingRules() {
        assertEquals(new HullItemData("sub", 5L), HullItemData.parse("{\"d\":\"sub\",\"s\":5}"));
        assertEquals(new HullItemData("sub", 5L), HullItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"BROKEN\",\"n\":true,\"r1\":\"A\"}"));
        assertEquals(new HullItemData("sub", 5L, COMMON, null),
                HullItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"" + COMMON_TEXT + "\",\"n\":false,\"r1\":\"A\"}"));
        assertEquals(new HullItemData("sub", 5L, COMMON, new FrozenName(null, null, "A", null)),
                HullItemData.parse("{\"d\":\"sub\",\"s\":\"5\",\"e\":\"" + COMMON_TEXT + "\",\"n\":true,\"r1\":\"A\",\"r2\":\"\"}"));
        assertEquals(new HullItemData("sub", 5L, COMMON, FrozenName.product(null, null, null, null)),
                HullItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"" + COMMON_TEXT + "\",\"n\":true,\"rp\":true,\"np\":\"X\"}"));
        assertNull(HullItemData.parse("{\"s\":5}"));
        assertNull(HullItemData.parse("{\"d\":\"\",\"s\":5}"));
        assertNull(HullItemData.parse("{\"d\":\"sub\"}"));
        assertNull(HullItemData.parse("{\"d\":\"sub\",\"s\":\"x\"}"));
        assertNull(HullItemData.parse("{not json"));
    }

    @Test
    void savedSeedOnlyCargoStringsDecodeToTheSameItems() {
        assertEquals(new HullItemData("sub", 7L), HullItemData.parse("sub|7"));
        assertEquals(new HullItemData("domain_subroutine_military", -5L), HullItemData.parse("domain_subroutine_military|-5"));
        assertEquals(new HullItemData("a|b", 7L), HullItemData.parse("a|b|7"));
        assertEquals(new HullItemData("a/b", 7L), HullItemData.parse("a/b|7"));
        assertNull(HullItemData.parse("|7"));
        assertNull(HullItemData.parse("sub|"));
        assertNull(HullItemData.parse("sub|x"));
        assertNull(HullItemData.parse("no_seed"));
        assertNull(HullItemData.parse(""));
        assertNull(HullItemData.parse(null));
    }

    @Test
    void savedNpcIdsDecodeToTheSameItems() {
        assertEquals(npcItem(COMMON_ITEM), NpcHullItems.item("npc:sub/42/" + COMMON_TEXT));
        assertEquals(npcItem(RARE_ITEM), NpcHullItems.item("npc:sub/42/" + RARE_TEXT));
        assertEquals(npcItem(PRODUCT_ITEM), NpcHullItems.item("npc:gadget/-9/" + RARE_TEXT));
        assertEquals(new HullItemData("sub", 8L, List.of(), null), NpcHullItems.item("npc:sub/8/"));
        assertEquals(new HullItemData("sub", 7L), NpcHullItems.item("npc:sub/7"));
        assertEquals(new HullItemData("a/b", 7L), NpcHullItems.item("npc:a/b/7"));
        assertEquals(new HullItemData("sub", 42L, List.of(new RolledEffect("B", 2f)), null), NpcHullItems.item("npc:sub/42/A:1/B:2"));
        assertEquals(new HullItemData("sub", 42L, List.of(new RolledEffect("A", 1f), new RolledEffect("B", 2f)), null),
                NpcHullItems.item("npc:sub/42/A:1;;B:2;"));
    }

    @Test
    void brokenOrForeignNpcIdsStayUnresolved() {
        assertNull(NpcHullItems.item("npc:sub/42/BROKEN"));
        assertNull(NpcHullItems.item("npc:sub/42/A:x"));
        assertNull(NpcHullItems.item("npc:sub/42/:1"));
        assertNull(NpcHullItems.item("npc:sub/x"));
        assertNull(NpcHullItems.item("npc:/7"));
        assertNull(NpcHullItems.item("npc:/7/A:1"));
        assertNull(NpcHullItems.item("npc:missing_seed"));
        assertNull(NpcHullItems.item("npc:"));
        assertNull(NpcHullItems.item("sub|7"));
        assertNull(NpcHullItems.item(COMMON_CARGO));
        assertNull(NpcHullItems.item("socketable_3"));
        assertNull(NpcHullItems.item(null));
        assertTrue(NpcHullItems.isNpcId("npc:sub/7"));
        assertFalse(NpcHullItems.isNpcId("socketable_3"));
        assertFalse(NpcHullItems.isNpcId(null));
    }

    @Test
    void everyWrittenFormRoundTripsThroughTheSharedDecoder() {
        for (HullItemData item : List.of(COMMON_ITEM, RARE_ITEM, PRODUCT_ITEM, UNIQUE_ITEM, ASSEMBLED_ITEM, UNNAMED_ITEM)) {
            assertEquals(item, HullItemData.of(item.toSpecialItem()));
            assertEquals(npcItem(item), NpcHullItems.item(NpcHullItems.id(item)));
            assertEquals(npcItem(item), HullItemCodec.decode(HullItemCodec.npc(item)));
        }
        assertEquals(new HullItemData("sub", 7L), HullItemCodec.decode(HullItemCodec.npc("sub", 7L)));
    }

    @Test
    void rolledEffectListsKeepTheirTextForm() {
        assertEquals(COMMON_TEXT, HullItemCodec.effects(COMMON));
        assertEquals(RARE_TEXT, HullItemCodec.effects(RARE));
        assertEquals("", HullItemCodec.effects(List.of()));
        assertEquals(RARE, HullItemCodec.decodeEffects(RARE_TEXT));
        assertEquals(List.of(), HullItemCodec.decodeEffects(""));
        assertEquals(List.of(new RolledEffect("A:B", 1f)), HullItemCodec.decodeEffects("A:B:1"));
        assertNull(HullItemCodec.decodeEffects("A"));
        assertNull(HullItemCodec.decodeEffects(":1"));
        assertNull(HullItemCodec.decodeEffects("A:"));
        assertNull(HullItemCodec.decodeEffects(null));
    }

    @Test
    void aSavedItemWithARenamedEffectDecodesToTheCurrentName() throws Exception {
        exiledsector.skills.skilleffect.EffectAliases.register(new org.json.JSONArray()
                .put(new org.json.JSONObject().put("alias", "OLD_HULL_MULT").put("effect", "HULL_MULT")));
        try {
            HullItemData decoded = HullItemCodec.decode(COMMON_CARGO.replace("HULL_MULT", "OLD_HULL_MULT"));

            assertEquals(COMMON_ITEM, decoded);
        } finally {
            exiledsector.skills.skilleffect.EffectAliases.clear();
        }
    }
}
