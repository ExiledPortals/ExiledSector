package exiledsector.items;

import exiledsector.ProjectFiles;
import exiledsector.i18n.EnglishCatalogueExtension;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.ThemeTagsExtension;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({ThemeTagsExtension.class, EnglishCatalogueExtension.class})
class InfusedCraftTest {

    private static final String PLATE = "infused_plate";
    private static final String RELIC = "infused_relic";
    private static final String FLUX = "T_FLUX";
    private static final String HULL = "T_HULL";
    private static final String WEAPON = "T_WEAPON";
    private static final String KINETIC = "T_KINETIC";
    private static final String HIGH_EXPLOSIVE = "T_HIGH_EXPLOSIVE";
    private static final String SHIELD = "T_SHIELD";
    private static final String ARMOR = "T_ARMOR";
    private static final String BIOME = "T_BIOME";
    private static final int SEEDS = 60;
    private static final String FEEDSTOCK = "test_feedstock";
    private static final int COST = 10;
    private static final String ALL_CRAFTS = FEEDSTOCK + ",10,10,10,10,10,10";
    private static final String ALL_BUT_SWAP = FEEDSTOCK + ",10,10,10,10,,10";
    private static final String COSTS = String.join("\n",
            "tag,feedstock,reforge_with_tag,reforge_more_alike,reforge_less_alike,reroll_tag_values,swap_damage_type,add_and_remove",
            "weapon," + ALL_BUT_SWAP, "flux," + ALL_BUT_SWAP, "armour," + ALL_BUT_SWAP, "front_shield," + ALL_BUT_SWAP,
            "kinetic," + ALL_CRAFTS, "high_explosive," + ALL_CRAFTS, "fragmentation," + ALL_CRAFTS);

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(modifierRow(FLUX, "prefix", "FLUX_CAPACITY_MULT:4:6", "flux"))
                .put(modifierRow(HULL, "prefix", "HULL_MULT:4:6", "hull").put(ItemModifiers.EXCLUSIVE_GROUPS_COLUMN, "plating"))
                .put(modifierRow(WEAPON, "prefix", "BALLISTIC_WEAPON_DAMAGE_PERCENT:5:10", "ballistic;damage"))
                .put(modifierRow(KINETIC, "suffix", "KINETIC_DAMAGE_TAKEN_PERCENT:-6:-4", "kinetic;damage_taken"))
                .put(modifierRow(HIGH_EXPLOSIVE, "suffix", "HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT:-12:-8", "high_explosive;damage_taken"))
                .put(modifierRow(SHIELD, "suffix", "SHIELD_DAMAGE_TAKEN_MULT:-15:-10", "front_shield"))
                .put(modifierRow(ARMOR, "suffix", "ARMOR_PERCENT:6:9", "armour").put(ItemModifiers.EXCLUSIVE_GROUPS_COLUMN, "plating"))
                .put(modifierRow(BIOME, "suffix", "FRAGMENTATION_DAMAGE_TAKEN_PERCENT:-6:-4", "fragmentation;damage_taken")));
        ItemDefinitions.register(new JSONArray()
                .put(definitionRow(PLATE, "@T_FLUX; @T_HULL; @T_WEAPON", "@T_KINETIC; @T_HIGH_EXPLOSIVE; @T_SHIELD; @T_ARMOR"))
                .put(definitionRow(RELIC, "@T_FLUX", "@T_KINETIC").put("unique", "TRUE")));
        ModifierSources.reserve(BIOME, "biome:test", Set.of(ItemKind.ARMOR_PLATING));
        InfusedCraftCosts.register(CDL.toJSONArray(COSTS));
    }

    @AfterEach
    void tearDown() {
        InfusedCraftCosts.clear();
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static JSONObject modifierRow(String id, String affix, String lines, String tags) throws Exception {
        return new JSONObject().put("id", id).put("affix", affix).put("scopes", "hull_item").put("lines", lines).put("name", id).put("tags", tags);
    }

    private static JSONObject definitionRow(String id, String prefixes, String suffixes) throws Exception {
        return new JSONObject().put("id", id).put("name", "Infused Plate").put("icon", "graphics/icons/cargo/chip1.png").put("rarity", "20")
                .put("prefixes", prefixes).put("suffixes", suffixes).put("section", "armor_plating");
    }

    private static RolledModifier rolled(String modifierId, float magnitude) {
        Modifier modifier = ItemModifiers.get(modifierId);
        return new RolledModifier(modifierId, modifier.slot(), List.of(new RolledLine(modifier.line().effectName(), magnitude)));
    }

    private static Item plate(RolledModifier... modifiers) {
        return Item.create("plate", PLATE, 7L, List.of(modifiers));
    }

    private static List<String> ids(Item item) {
        return item.explicits().stream().map(RolledModifier::modifierId).toList();
    }

    private static String refusal(InfusedCraft craft, Item item, String tag) {
        return refusal(craft, item, tag, Set.of());
    }

    private static String refusal(InfusedCraft craft, Item item, String tag, Set<CraftingInhibitor> inhibitors) {
        return craft.verdict(item, tag, inhibitors).refusalKey();
    }

    private static boolean craft(InfusedCraft craft, Item item, String tag, long seed) {
        return craft(craft, item, tag, Set.of(), seed);
    }

    private static boolean craft(InfusedCraft craft, Item item, String tag, Set<CraftingInhibitor> inhibitors, long seed) {
        return craft.apply(item, tag, ForgeChoice.WHOLE_ITEM, inhibitors, new Random(seed));
    }

    private static List<RolledModifier> slot(Item item, ModifierSlot side) {
        return item.explicits().stream().filter(rolled -> rolled.slot() == side).toList();
    }

    @Test
    void theTaggedReforgeAlwaysRollsAModifierWithItsTag() {
        for (long seed = 0; seed < SEEDS; seed++) {
            Item narrow = plate(rolled(FLUX, 5f), rolled(KINETIC, -5f));
            assertTrue(craft(InfusedCraft.REFORGE_WITH_TAG, narrow, "front_shield", seed));
            assertTrue(ids(narrow).contains(SHIELD), "seed " + seed);

            Item broad = plate(rolled(FLUX, 5f));
            assertTrue(craft(InfusedCraft.REFORGE_WITH_TAG, broad, "weapon", seed));
            assertTrue(ids(broad).contains(WEAPON), "seed " + seed);
            assertFalse(ids(broad).contains(HULL) && ids(broad).contains(ARMOR), "seed " + seed);
        }
    }

    @Test
    void reforgesKeepImmutableAndReservedModifiers() {
        for (InfusedCraft craft : List.of(InfusedCraft.REFORGE_WITH_TAG, InfusedCraft.REFORGE_MORE_ALIKE, InfusedCraft.REFORGE_LESS_ALIKE)) {
            for (long seed = 0; seed < SEEDS; seed++) {
                Item item = plate(rolled(FLUX, 5f).asImmutable(), rolled(HULL, 5f), rolled(BIOME, -5f));
                assertTrue(craft(craft, item, "armour", seed), craft + " seed " + seed);
                assertTrue(item.explicits().contains(rolled(FLUX, 5f).asImmutable()), craft + " seed " + seed);
                assertTrue(item.explicits().contains(rolled(BIOME, -5f)), craft + " seed " + seed);
                assertTrue(item.explicits().size() >= 3, craft + " seed " + seed);
            }
        }
    }

    @Test
    void theAlikeReforgesShiftTheChanceOfRollingThePreviousModifiers() {
        int moreAlikeKept = 0;
        int lessAlikeKept = 0;
        for (long seed = 0; seed < 200; seed++) {
            moreAlikeKept += keptAfter(InfusedCraft.REFORGE_MORE_ALIKE, seed);
            lessAlikeKept += keptAfter(InfusedCraft.REFORGE_LESS_ALIKE, seed);
        }
        assertTrue(moreAlikeKept > lessAlikeKept * 2, moreAlikeKept + " against " + lessAlikeKept);
    }

    private static int keptAfter(InfusedCraft craft, long seed) {
        Item item = plate(rolled(FLUX, 5f), rolled(KINETIC, -5f));
        assertTrue(craft(craft, item, "flux", seed));
        return (int) ids(item).stream().filter(id -> id.equals(FLUX) || id.equals(KINETIC)).count();
    }

    @Test
    void theTagRerollChangesOnlyModifiersWithItsTag() {
        Item item = plate(rolled(FLUX, 5f), rolled(KINETIC, -5f), rolled(HIGH_EXPLOSIVE, -10f));

        assertTrue(craft(InfusedCraft.REROLL_TAG_VALUES, item, "kinetic", 3L));
        assertEquals(rolled(FLUX, 5f), item.explicits().get(0));
        assertNotEquals(rolled(KINETIC, -5f), item.explicits().get(1));
        assertEquals(rolled(HIGH_EXPLOSIVE, -10f), item.explicits().get(2));

        assertEquals(InfusedCraft.REFUSAL_PREFIX + "reroll_tag_values", refusal(InfusedCraft.REROLL_TAG_VALUES, item, "armour"));
        assertFalse(craft(InfusedCraft.REROLL_TAG_VALUES, item, "armour", 3L));
    }

    @Test
    void uniquesTakeOnlyTheTagReroll() {
        Item relic = Item.create("relic", RELIC, 1L, List.of());

        assertTrue(InfusedCraft.REROLL_TAG_VALUES.verdict(relic, "flux", Set.of()).allowed());
        for (InfusedCraft craft : List.of(InfusedCraft.REFORGE_WITH_TAG, InfusedCraft.REFORGE_MORE_ALIKE, InfusedCraft.ADD_AND_REMOVE)) {
            assertEquals(InfusedCraft.REFUSAL_PREFIX + "unique", refusal(craft, relic, "flux"), craft.id());
        }
        assertEquals(InfusedCraft.REFUSAL_PREFIX + "unique", refusal(InfusedCraft.SWAP_DAMAGE_TYPE, relic, "high_explosive"));
    }

    @Test
    void theDamageSwapTurnsAModifierIntoItsSiblingAtTheSameRollPosition() {
        RolledModifier kinetic = rolled(KINETIC, -5f);
        Item item = plate(rolled(FLUX, 5f), kinetic, rolled(BIOME, -5f));

        assertTrue(InfusedCraft.SWAP_DAMAGE_TYPE.picksAChoice());
        assertEquals(List.of(ForgeChoice.modifier(kinetic)), InfusedCraft.SWAP_DAMAGE_TYPE.choices(item, "high_explosive", Set.of()));
        assertTrue(InfusedCraft.SWAP_DAMAGE_TYPE.apply(item, "high_explosive", ForgeChoice.modifier(kinetic), Set.of(), new Random(1L)));

        assertEquals(List.of(FLUX, BIOME, HIGH_EXPLOSIVE), ids(item));
        RolledLine swapped = item.explicits().get(2).lines().get(0);
        assertEquals("HIGH_EXPLOSIVE_DAMAGE_TAKEN_PERCENT", swapped.effectName());
        assertEquals(-10f, swapped.magnitude(), 1e-4f);
    }

    @Test
    void theDamageSwapRefusesWithoutASiblingAndLeavesReservedModifiersAlone() {
        Item item = plate(rolled(HIGH_EXPLOSIVE, -10f), rolled(BIOME, -5f));

        assertEquals(InfusedCraft.REFUSAL_PREFIX + "swap_damage_type", refusal(InfusedCraft.SWAP_DAMAGE_TYPE, item, "fragmentation"));
        assertEquals(List.of(), InfusedCraft.SWAP_DAMAGE_TYPE.choices(item, "kinetic", Set.of()).stream()
                .filter(choice -> choice.modifier().modifierId().equals(BIOME)).toList());
        assertEquals(InfusedCraft.REFUSAL_PREFIX + "tag", refusal(InfusedCraft.SWAP_DAMAGE_TYPE, item, "flux"));
    }

    @Test
    void addAndRemoveTradesAModifierWithoutTheTagAndRespectsExclusiveGroups() {
        for (long seed = 0; seed < SEEDS; seed++) {
            Item item = plate(rolled(FLUX, 5f).asImmutable(), rolled(HULL, 5f), rolled(BIOME, -5f));

            assertTrue(craft(InfusedCraft.ADD_AND_REMOVE, item, "armour", seed));
            assertEquals(List.of(FLUX, BIOME, ARMOR), ids(item), "seed " + seed);
        }
    }

    @Test
    void addAndRemoveRefusesWhenEveryRemovableModifierHasTheTag() {
        Item item = plate(rolled(ARMOR, 7f));

        assertEquals(InfusedCraft.REFUSAL_PREFIX + "add_and_remove", refusal(InfusedCraft.ADD_AND_REMOVE, item, "armour"));
        assertFalse(craft(InfusedCraft.ADD_AND_REMOVE, item, "armour", 1L));
        assertEquals(List.of(ARMOR), ids(item));
    }

    @Test
    void emptyMirroredCorruptedAndUnofferedCraftsAreRefused() {
        Item corrupted = plate(rolled(FLUX, 5f));
        corrupted.corrupt(CorruptionOutcome.NOTHING);
        Item mirrored = plate(rolled(FLUX, 5f)).mirrorAs("copy");

        for (InfusedCraft craft : InfusedCraft.values()) {
            assertEquals(InfusedCraft.REFUSAL_PREFIX + "loadFirst", refusal(craft, null, "kinetic"), craft.id());
            assertEquals(InfusedCraft.REFUSAL_PREFIX + "corrupted", refusal(craft, corrupted, "kinetic"), craft.id());
            assertEquals(InfusedCraft.REFUSAL_PREFIX + "mirrored", refusal(craft, mirrored, "kinetic"), craft.id());
            assertEquals(InfusedCraft.REFUSAL_PREFIX + "tag", refusal(craft, plate(rolled(FLUX, 5f)), "low_tech"), craft.id());
        }
        assertFalse(craft(InfusedCraft.REFORGE_WITH_TAG, corrupted, "flux", 1L));
        assertEquals(List.of(rolled(FLUX, 5f)), corrupted.explicits());
    }

    @Test
    void usingACraftTakesItsFeedstockOnlyWhenItRuns() {
        CraftingMaterials materials = CraftingMaterials.storedForTests(new HashMap<>(Map.of(FEEDSTOCK, COST + 5)));

        assertEquals(Optional.empty(), use(InfusedCraft.REFORGE_WITH_TAG, null, "weapon", materials, Set.of()));
        assertEquals(COST + 5, materials.count(FEEDSTOCK));

        assertEquals(Optional.of(Set.of()), use(InfusedCraft.REFORGE_WITH_TAG, plate(rolled(FLUX, 5f)), "weapon", materials, Set.of()));
        assertEquals(5, materials.count(FEEDSTOCK));
        assertFalse(InfusedCraft.REFORGE_WITH_TAG.canAfford("weapon", materials));
        assertEquals(Optional.empty(), use(InfusedCraft.REFORGE_WITH_TAG, plate(rolled(FLUX, 5f)), "weapon", materials, Set.of()));
    }

    private static Optional<Set<CraftingInhibitor>> use(InfusedCraft craft, Item item, String tag, CraftingMaterials materials,
                                                        Set<CraftingInhibitor> inhibitors) {
        return craft.use(item, tag, ForgeChoice.WHOLE_ITEM, materials, inhibitors, new Random(1L));
    }

    @Test
    void aPrefixInhibitorKeepsEveryPrefixThroughTheTaggedReforge() {
        for (long seed = 0; seed < SEEDS; seed++) {
            Item item = plate(rolled(FLUX, 5f), rolled(KINETIC, -5f));

            assertEquals(Set.of(CraftingInhibitor.PREFIX),
                    InfusedCraft.REFORGE_WITH_TAG.inhibitorsSpent(item, "front_shield", Set.of(CraftingInhibitor.PREFIX)));
            assertTrue(craft(InfusedCraft.REFORGE_WITH_TAG, item, "front_shield", Set.of(CraftingInhibitor.PREFIX), seed));
            assertTrue(slot(item, ModifierSlot.PREFIX).contains(rolled(FLUX, 5f)), "seed " + seed);
            assertTrue(ids(item).contains(SHIELD), "seed " + seed);
        }
    }

    @Test
    void anInhibitorIsSpentOnlyWhenItsSideHoldsSomethingTheCraftCouldChange() {
        Item suffixOnly = plate(rolled(KINETIC, -5f));
        Item reservedSuffix = plate(rolled(FLUX, 5f), rolled(BIOME, -5f));

        assertEquals(Set.of(), InfusedCraft.REFORGE_WITH_TAG.inhibitorsSpent(suffixOnly, "weapon", Set.of(CraftingInhibitor.PREFIX)));
        assertEquals(Set.of(), InfusedCraft.REFORGE_WITH_TAG.inhibitorsSpent(reservedSuffix, "weapon", Set.of(CraftingInhibitor.SUFFIX)));
        assertEquals(Set.of(), InfusedCraft.ADD_AND_REMOVE.inhibitorsSpent(plate(rolled(ARMOR, 7f), rolled(FLUX, 5f)), "armour",
                Set.of(CraftingInhibitor.SUFFIX)));
    }

    @Test
    void aSuffixInhibitorLimitsTheExchangeToPrefixes() {
        for (long seed = 0; seed < SEEDS; seed++) {
            Item item = plate(rolled(HULL, 5f), rolled(KINETIC, -5f));

            assertEquals(Set.of(CraftingInhibitor.SUFFIX),
                    InfusedCraft.ADD_AND_REMOVE.inhibitorsSpent(item, "weapon", Set.of(CraftingInhibitor.SUFFIX)));
            assertTrue(craft(InfusedCraft.ADD_AND_REMOVE, item, "weapon", Set.of(CraftingInhibitor.SUFFIX), seed));
            assertEquals(Set.of(KINETIC, WEAPON), Set.copyOf(ids(item)), "seed " + seed);
        }
    }

    @Test
    void bothInhibitorsBlockTheExchangeWithoutSpendingAnything() {
        Set<CraftingInhibitor> both = Set.of(CraftingInhibitor.PREFIX, CraftingInhibitor.SUFFIX);
        Item item = plate(rolled(HULL, 5f), rolled(KINETIC, -5f));
        CraftingMaterials materials = CraftingMaterials.storedForTests(new HashMap<>(Map.of(FEEDSTOCK, COST,
                CraftingInhibitor.PREFIX.itemId(), 1, CraftingInhibitor.SUFFIX.itemId(), 1)));

        assertEquals(InfusedCraft.REFUSAL_PREFIX + "inhibited", refusal(InfusedCraft.ADD_AND_REMOVE, item, "weapon", both));
        assertEquals(Optional.empty(), use(InfusedCraft.ADD_AND_REMOVE, item, "weapon", materials, both));
        assertEquals(List.of(HULL, KINETIC), ids(item));
        assertEquals(COST, materials.count(FEEDSTOCK));
        assertEquals(1, materials.count(CraftingInhibitor.PREFIX.itemId()));
    }

    @Test
    void usingACraftConsumesOneOfEachSpentInhibitor() {
        CraftingMaterials materials = CraftingMaterials.storedForTests(new HashMap<>(Map.of(FEEDSTOCK, COST,
                CraftingInhibitor.PREFIX.itemId(), 2, CraftingInhibitor.SUFFIX.itemId(), 1)));
        Set<CraftingInhibitor> both = Set.of(CraftingInhibitor.PREFIX, CraftingInhibitor.SUFFIX);

        assertEquals(Optional.of(Set.of(CraftingInhibitor.PREFIX)),
                use(InfusedCraft.REFORGE_WITH_TAG, plate(rolled(FLUX, 5f), rolled(BIOME, -5f)), "front_shield", materials, both));
        assertEquals(0, materials.count(FEEDSTOCK));
        assertEquals(1, materials.count(CraftingInhibitor.PREFIX.itemId()));
        assertEquals(1, materials.count(CraftingInhibitor.SUFFIX.itemId()));
    }

    @Test
    void anInhibitorThatIsNoLongerHeldStopsTheCraft() {
        CraftingMaterials materials = CraftingMaterials.storedForTests(new HashMap<>(Map.of(FEEDSTOCK, COST)));
        Item item = plate(rolled(FLUX, 5f), rolled(KINETIC, -5f));

        assertEquals(Optional.empty(), use(InfusedCraft.REFORGE_WITH_TAG, item, "front_shield", materials, Set.of(CraftingInhibitor.PREFIX)));
        assertEquals(COST, materials.count(FEEDSTOCK));
        assertEquals(List.of(FLUX, KINETIC), ids(item));
    }

    @Test
    void refusalsNameTheTagWithItsArticle() {
        Map<String, String> args = InfusedCraft.ADD_AND_REMOVE.verdict(plate(rolled(ARMOR, 7f)), "armour", Set.of()).args();

        assertEquals("an", args.get("article"));
        assertEquals("Armor", args.get("tag"));
        assertEquals("a", InfusedCraft.article("weapon"));
    }

    @Test
    void everyRefusalAndCraftHasText() throws Exception {
        String catalogue = Files.readString(ProjectFiles.gameFile("data/strings/exiledSector/core/en.json"), StandardCharsets.UTF_8);
        List<String> keys = new ArrayList<>(List.of("loadFirst", "itemType", "mirrored", "corrupted", "locked", "tag", "unique", "inhibited"));
        for (InfusedCraft craft : InfusedCraft.values()) {
            keys.add(craft.id());
            assertTrue(catalogue.contains("\"ui.infusedCraft." + craft.id() + ".name\""), craft.id());
            assertTrue(catalogue.contains("\"ui.infusedCraft." + craft.id() + ".description\""), craft.id());
        }
        keys.forEach(key -> assertTrue(catalogue.contains("\"" + InfusedCraft.REFUSAL_PREFIX + key + "\""), key));
    }
}
