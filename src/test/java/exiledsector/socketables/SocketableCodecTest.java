package exiledsector.socketables;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableCodecTest {

    private static final List<RolledEffect> COMMON = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f), new RolledEffect("HULL_MULT", 5f));
    private static final List<RolledEffect> RARE = List.of(new RolledEffect("FIGHTER_ARMOR_PERCENT", 12f),
            new RolledEffect("FLUX_DISSIPATION_MULT", 5.5f), new RolledEffect("ARMOR_PERCENT", -0.25f), new RolledEffect("HULL_MULT", 0.0001f));
    private static final String COMMON_TEXT = "FIGHTER_ARMOR_PERCENT:12;HULL_MULT:5";
    private static final String RARE_TEXT = "FIGHTER_ARMOR_PERCENT:12;FLUX_DISSIPATION_MULT:5.5;ARMOR_PERCENT:-0.25;HULL_MULT:1.0E-4";

    private static final SocketableItemData COMMON_ITEM =
            new SocketableItemData("sub", 42L, COMMON, new FrozenName("FIGHTER_ARMOR_PERCENT", "HULL_MULT", null, null));
    private static final SocketableItemData RARE_ITEM = new SocketableItemData("sub", 42L, RARE, new FrozenName(null, null, "Askonia", "Doctrine"));
    private static final SocketableItemData PRODUCT_ITEM =
            new SocketableItemData("gadget", -9L, RARE, FrozenName.product("Sunny", "Brand", "Pal", "X-7"));
    private static final SocketableItemData UNIQUE_ITEM = new SocketableItemData("relic", 3L, COMMON, FrozenName.NONE);
    private static final SocketableItemData ASSEMBLED_ITEM =
            new SocketableItemData("sub", 8L, List.of(), new FrozenName(null, null, "Sunny Pal", null));
    private static final SocketableItemData UNNAMED_ITEM = new SocketableItemData("removed", 9L, COMMON, null);

    private static final String COMMON_CARGO =
            "{\"np\":\"FIGHTER_ARMOR_PERCENT\",\"s\":42,\"d\":\"sub\",\"e\":\"" + COMMON_TEXT + "\",\"ns\":\"HULL_MULT\",\"n\":true}";
    private static final String RARE_CARGO = "{\"r2\":\"Doctrine\",\"s\":42,\"d\":\"sub\",\"e\":\"" + RARE_TEXT + "\",\"n\":true,\"r1\":\"Askonia\"}";
    private static final String PRODUCT_CARGO = "{\"r2\":\"Pal\",\"rb\":\"Brand\",\"s\":-9,\"d\":\"gadget\",\"e\":\"" + RARE_TEXT
            + "\",\"rm\":\"X-7\",\"n\":true,\"rp\":true,\"r1\":\"Sunny\"}";
    private static final String UNIQUE_CARGO = "{\"s\":3,\"d\":\"relic\",\"e\":\"" + COMMON_TEXT + "\",\"n\":true}";
    private static final String ASSEMBLED_CARGO = "{\"s\":8,\"d\":\"sub\",\"e\":\"\",\"n\":true,\"r1\":\"Sunny Pal\"}";
    private static final String UNNAMED_CARGO = "{\"s\":9,\"d\":\"removed\",\"e\":\"" + COMMON_TEXT + "\"}";

    private static SocketableItemData npcItem(SocketableItemData item) {
        return new SocketableItemData(item.definitionId(), item.seed(), item.effects(), null);
    }

    @Test
    void cargoItemsAreWrittenExactlyAsTheReleasedBuildWroteThem() {
        assertEquals(COMMON_CARGO, COMMON_ITEM.toSpecialItem().getData());
        assertEquals(RARE_CARGO, RARE_ITEM.toSpecialItem().getData());
        assertEquals(PRODUCT_CARGO, PRODUCT_ITEM.toSpecialItem().getData());
        assertEquals(UNIQUE_CARGO, UNIQUE_ITEM.toSpecialItem().getData());
        assertEquals(ASSEMBLED_CARGO, ASSEMBLED_ITEM.toSpecialItem().getData());
        assertEquals(UNNAMED_CARGO, UNNAMED_ITEM.toSpecialItem().getData());
        assertEquals("sub|7", new SocketableItemData("sub", 7L).toSpecialItem().getData());
        assertEquals(SocketableItemData.ITEM_ID, COMMON_ITEM.toSpecialItem().getId());
    }

    @Test
    void npcIdsAreWrittenExactlyAsTheReleasedBuildWroteThem() {
        assertEquals("npc:sub/42/" + COMMON_TEXT, NpcSocketables.id(COMMON_ITEM));
        assertEquals("npc:sub/42/" + RARE_TEXT, NpcSocketables.id(RARE_ITEM));
        assertEquals("npc:gadget/-9/" + RARE_TEXT, NpcSocketables.id(PRODUCT_ITEM));
        assertEquals("npc:relic/3/" + COMMON_TEXT, NpcSocketables.id(UNIQUE_ITEM));
        assertEquals("npc:sub/8/", NpcSocketables.id(ASSEMBLED_ITEM));
        assertEquals("npc:sub/7", NpcSocketables.id(new SocketableItemData("sub", 7L)));
        assertEquals("npc:sub/7", NpcSocketables.id("sub", 7L));
    }

    @Test
    void savedFrozenCargoStringsDecodeToTheSameItems() {
        assertEquals(COMMON_ITEM, SocketableItemData.parse(COMMON_CARGO));
        assertEquals(RARE_ITEM, SocketableItemData.parse(RARE_CARGO));
        assertEquals(PRODUCT_ITEM, SocketableItemData.parse(PRODUCT_CARGO));
        assertEquals(UNIQUE_ITEM, SocketableItemData.parse(UNIQUE_CARGO));
        assertEquals(ASSEMBLED_ITEM, SocketableItemData.parse(ASSEMBLED_CARGO));
        assertEquals(UNNAMED_ITEM, SocketableItemData.parse(UNNAMED_CARGO));
    }

    @Test
    void frozenCargoStringsKeepTheirLenientReadingRules() {
        assertEquals(new SocketableItemData("sub", 5L), SocketableItemData.parse("{\"d\":\"sub\",\"s\":5}"));
        assertEquals(new SocketableItemData("sub", 5L), SocketableItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"BROKEN\",\"n\":true,\"r1\":\"A\"}"));
        assertEquals(new SocketableItemData("sub", 5L, COMMON, null),
                SocketableItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"" + COMMON_TEXT + "\",\"n\":false,\"r1\":\"A\"}"));
        assertEquals(new SocketableItemData("sub", 5L, COMMON, new FrozenName(null, null, "A", null)),
                SocketableItemData.parse("{\"d\":\"sub\",\"s\":\"5\",\"e\":\"" + COMMON_TEXT + "\",\"n\":true,\"r1\":\"A\",\"r2\":\"\"}"));
        assertEquals(new SocketableItemData("sub", 5L, COMMON, FrozenName.product(null, null, null, null)),
                SocketableItemData.parse("{\"d\":\"sub\",\"s\":5,\"e\":\"" + COMMON_TEXT + "\",\"n\":true,\"rp\":true,\"np\":\"X\"}"));
        assertNull(SocketableItemData.parse("{\"s\":5}"));
        assertNull(SocketableItemData.parse("{\"d\":\"\",\"s\":5}"));
        assertNull(SocketableItemData.parse("{\"d\":\"sub\"}"));
        assertNull(SocketableItemData.parse("{\"d\":\"sub\",\"s\":\"x\"}"));
        assertNull(SocketableItemData.parse("{not json"));
    }

    @Test
    void savedSeedOnlyCargoStringsDecodeToTheSameItems() {
        assertEquals(new SocketableItemData("sub", 7L), SocketableItemData.parse("sub|7"));
        assertEquals(new SocketableItemData("domain_subroutine_military", -5L), SocketableItemData.parse("domain_subroutine_military|-5"));
        assertEquals(new SocketableItemData("a|b", 7L), SocketableItemData.parse("a|b|7"));
        assertEquals(new SocketableItemData("a/b", 7L), SocketableItemData.parse("a/b|7"));
        assertNull(SocketableItemData.parse("|7"));
        assertNull(SocketableItemData.parse("sub|"));
        assertNull(SocketableItemData.parse("sub|x"));
        assertNull(SocketableItemData.parse("no_seed"));
        assertNull(SocketableItemData.parse(""));
        assertNull(SocketableItemData.parse(null));
    }

    @Test
    void savedNpcIdsDecodeToTheSameItems() {
        assertEquals(npcItem(COMMON_ITEM), NpcSocketables.item("npc:sub/42/" + COMMON_TEXT));
        assertEquals(npcItem(RARE_ITEM), NpcSocketables.item("npc:sub/42/" + RARE_TEXT));
        assertEquals(npcItem(PRODUCT_ITEM), NpcSocketables.item("npc:gadget/-9/" + RARE_TEXT));
        assertEquals(new SocketableItemData("sub", 8L, List.of(), null), NpcSocketables.item("npc:sub/8/"));
        assertEquals(new SocketableItemData("sub", 7L), NpcSocketables.item("npc:sub/7"));
        assertEquals(new SocketableItemData("a/b", 7L), NpcSocketables.item("npc:a/b/7"));
        assertEquals(new SocketableItemData("sub", 42L, List.of(new RolledEffect("B", 2f)), null), NpcSocketables.item("npc:sub/42/A:1/B:2"));
        assertEquals(new SocketableItemData("sub", 42L, List.of(new RolledEffect("A", 1f), new RolledEffect("B", 2f)), null),
                NpcSocketables.item("npc:sub/42/A:1;;B:2;"));
    }

    @Test
    void brokenOrForeignNpcIdsStayUnresolved() {
        assertNull(NpcSocketables.item("npc:sub/42/BROKEN"));
        assertNull(NpcSocketables.item("npc:sub/42/A:x"));
        assertNull(NpcSocketables.item("npc:sub/42/:1"));
        assertNull(NpcSocketables.item("npc:sub/x"));
        assertNull(NpcSocketables.item("npc:/7"));
        assertNull(NpcSocketables.item("npc:/7/A:1"));
        assertNull(NpcSocketables.item("npc:missing_seed"));
        assertNull(NpcSocketables.item("npc:"));
        assertNull(NpcSocketables.item("sub|7"));
        assertNull(NpcSocketables.item(COMMON_CARGO));
        assertNull(NpcSocketables.item("socketable_3"));
        assertNull(NpcSocketables.item(null));
        assertTrue(NpcSocketables.isNpcId("npc:sub/7"));
        assertFalse(NpcSocketables.isNpcId("socketable_3"));
        assertFalse(NpcSocketables.isNpcId(null));
    }

    @Test
    void everyWrittenFormRoundTripsThroughTheSharedDecoder() {
        for (SocketableItemData item : List.of(COMMON_ITEM, RARE_ITEM, PRODUCT_ITEM, UNIQUE_ITEM, ASSEMBLED_ITEM, UNNAMED_ITEM)) {
            assertEquals(item, SocketableItemData.of(item.toSpecialItem()));
            assertEquals(npcItem(item), NpcSocketables.item(NpcSocketables.id(item)));
            assertEquals(npcItem(item), SocketableCodec.decode(SocketableCodec.npc(item)));
        }
        assertEquals(new SocketableItemData("sub", 7L), SocketableCodec.decode(SocketableCodec.npc("sub", 7L)));
    }

    @Test
    void rolledEffectListsKeepTheirTextForm() {
        assertEquals(COMMON_TEXT, SocketableCodec.effects(COMMON));
        assertEquals(RARE_TEXT, SocketableCodec.effects(RARE));
        assertEquals("", SocketableCodec.effects(List.of()));
        assertEquals(RARE, SocketableCodec.decodeEffects(RARE_TEXT));
        assertEquals(List.of(), SocketableCodec.decodeEffects(""));
        assertEquals(List.of(new RolledEffect("A:B", 1f)), SocketableCodec.decodeEffects("A:B:1"));
        assertNull(SocketableCodec.decodeEffects("A"));
        assertNull(SocketableCodec.decodeEffects(":1"));
        assertNull(SocketableCodec.decodeEffects("A:"));
        assertNull(SocketableCodec.decodeEffects(null));
    }
}
