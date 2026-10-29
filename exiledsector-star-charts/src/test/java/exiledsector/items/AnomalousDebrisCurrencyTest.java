package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnomalousDebrisCurrencyTest {

    private static final String REVENANT = "exiledSector_test_revenant";
    private static final String VIGIL = "exiledSector_test_vigil";
    private static final String GRANTED_PREFIX = "HULL_PERCENT_ANOMALOUS_DEBRIS";
    private static final String GRANTED_SUFFIX = "BEAM_WEAPON_RANGE_PERCENT_ANOMALOUS_DEBRIS";
    private static final String BASE = "test_bridge";
    private static final String RELIC = "test_relic";
    private final ItemStore store = new ItemStore();

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray()
                .put(HullItemRows.modifierRow(GRANTED_PREFIX, "prefix", "HULL_PERCENT:15:15", "Revenant"))
                .put(HullItemRows.modifierRow(GRANTED_SUFFIX, "suffix", "BEAM_WEAPON_RANGE_PERCENT:25:25", "Vigil")));
        ItemDefinitions.register(List.of(
                definition(BASE, false, List.of(prefix("HULL_PERCENT"), prefix("FLUX_CAPACITY_MULT"), prefix("FLUX_DISSIPATION_MULT")),
                        List.of(suffix("BEAM_WEAPON_RANGE_PERCENT"), suffix("ARMOR_PERCENT"), suffix("HULL_MULT"))),
                definition(RELIC, true, List.of(prefix("ARMOR_PERCENT")), List.of())));
        AnomalousDebrisCurrencies.register(new JSONArray()
                .put(currencyRow(REVENANT, GRANTED_PREFIX, "hull_item", 1))
                .put(currencyRow(VIGIL, GRANTED_SUFFIX, "hull_item", 6)));
    }

    @AfterEach
    void tearDown() {
        AnomalousDebrisCurrencies.resetForTests();
        ReservedModifiers.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static PoolEntry prefix(String effectName) {
        return PoolEntry.of(ModifierSlot.PREFIX, effectName, 4f, 6f, 1f);
    }

    private static PoolEntry suffix(String effectName) {
        return PoolEntry.of(ModifierSlot.SUFFIX, effectName, 4f, 6f, 1f);
    }

    private static ItemDefinition definition(String id, boolean unique, List<PoolEntry> prefixes, List<PoolEntry> suffixes) {
        return new ItemDefinition(id, ItemKind.BRIDGE, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "", prefixes, suffixes, "");
    }

    private static JSONObject currencyRow(String id, String modifier, String itemTypes, int minTier) throws JSONException {
        return new JSONObject().put("id", id).put("name", id).put("modifier", modifier).put("item_types", itemTypes)
                .put("min_tier", String.valueOf(minTier)).put("weight", "1");
    }

    private Item common() {
        return store.add(new ItemData(BASE, 7L, List.of(RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("FLUX_CAPACITY_MULT", 5f))),
                null));
    }

    private static AnomalousDebrisCurrency currency(String itemId) {
        return (AnomalousDebrisCurrency) ItemCurrencies.byItemId(itemId);
    }

    @Test
    void rerollingGivesFourModifiersWithTheGrantedOneAndAtMostTwoPerSide() {
        for (long seed = 0; seed < 40; seed++) {
            for (String currencyId : List.of(REVENANT, VIGIL)) {
                Item item = common();
                AnomalousDebrisCurrency currency = currency(currencyId);

                assertSame(item, currency.use(item, new CraftingContext(store, definition -> true, Set.of()), new Random(seed)));

                List<RolledModifier> explicits = item.explicits();
                assertEquals(4, explicits.size());
                assertEquals(ItemRarity.RARE, item.rarity());
                assertTrue(explicits.stream().anyMatch(rolled -> rolled.modifierId().equals(currency.modifierId())));
                assertEquals(2, explicits.stream().filter(rolled -> rolled.slot() == ModifierSlot.PREFIX).count());
                assertEquals(2, explicits.stream().filter(rolled -> rolled.slot() == ModifierSlot.SUFFIX).count());
                Set<String> effects = new HashSet<>();
                explicits.forEach(rolled -> rolled.lines().forEach(line -> assertTrue(effects.add(line.effectName()), line.effectName())));
                assertEquals(ModifierSlot.PREFIX, explicits.get(0).slot());
            }
        }
    }

    @Test
    void theGrantedModifierKeepsItsIdAndFixedValueThoughThePoolRollsTheSameEffect() {
        Item item = common();
        currency(REVENANT).use(item, new CraftingContext(store, definition -> true, Set.of()), new Random(3L));
        Item reloaded = Item.create("reloaded", BASE, 7L, item.explicits());

        RolledModifier granted = reloaded.explicits().stream().filter(rolled -> rolled.lines().get(0).effectName().equals("HULL_PERCENT"))
                .findFirst().orElseThrow();
        assertEquals(GRANTED_PREFIX, granted.modifierId());
        assertEquals(15f, granted.lines().get(0).magnitude());
        assertNotNull(reloaded.explicitModifier(GRANTED_PREFIX));
    }

    private Item lockedWith(String effectName, ModifierSlot slot, float magnitude) {
        return store.add(new ItemData(BASE, 9L, List.of(RolledModifier.single(slot, new RolledEffect(effectName, magnitude)).asLocked()), null));
    }

    @Test
    void anEngramKeepsAFixationLockedModifierAndFillsAroundIt() {
        for (long seed = 0; seed < 40; seed++) {
            for (String currencyId : List.of(REVENANT, VIGIL)) {
                Item item = lockedWith("FLUX_CAPACITY_MULT", ModifierSlot.PREFIX, 5f);
                AnomalousDebrisCurrency currency = currency(currencyId);

                assertSame(item, currency.use(item, new CraftingContext(store, definition -> true, Set.of()), new Random(seed)));

                List<RolledModifier> explicits = item.explicits();
                assertEquals(4, explicits.size());
                assertEquals(2, explicits.stream().filter(rolled -> rolled.slot() == ModifierSlot.PREFIX).count());
                RolledModifier locked = explicits.stream().filter(RolledModifier::locked).findFirst().orElseThrow();
                assertEquals("FLUX_CAPACITY_MULT", locked.lines().get(0).effectName());
                assertEquals(5f, locked.lines().get(0).magnitude());
                assertTrue(explicits.stream().anyMatch(rolled -> rolled.modifierId().equals(currency.modifierId())));
                Set<String> effects = new HashSet<>();
                explicits.forEach(rolled -> rolled.lines().forEach(line -> assertTrue(effects.add(line.effectName()), line.effectName())));
            }
        }
    }

    @Test
    void anEngramRefusesAnItemWhoseLockedModifierRollsItsOwnEffect() {
        Item item = lockedWith("HULL_PERCENT", ModifierSlot.PREFIX, 5f);
        CraftingContext context = new CraftingContext(store, definition -> true, Set.of());

        assertFalse(currency(REVENANT).canUse(item, context));
        assertEquals(AnomalousDebrisCurrency.LOCKED_CLASH_KEY, currency(REVENANT).refusalNoticeKey(item));
        assertTrue(currency(VIGIL).canUse(item, context));
        assertNull(currency(VIGIL).refusalNoticeKey(item));
    }

    @Test
    void corruptingAroundALockedReservedModifierNeverAddsItsOrdinaryTwin() {
        for (long seed = 0; seed < 60; seed++) {
            Item item = common();
            currency(REVENANT).use(item, new CraftingContext(store, definition -> true, Set.of()), new Random(seed));
            List<RolledModifier> locked = new java.util.ArrayList<>(item.explicits());
            locked.replaceAll(rolled -> rolled.modifierId().equals(GRANTED_PREFIX) ? rolled.asLocked() : rolled);
            item.replaceExplicits(locked);

            assertNotNull(ItemCrafting.randomiseModifiers(store, item, new Random(seed + 100L), definition -> true));

            long hullLines = item.explicits().stream().flatMap(rolled -> rolled.lines().stream())
                    .filter(line -> line.effectName().equals("HULL_PERCENT")).count();
            assertEquals(1, hullLines);
            assertTrue(item.explicits().stream().anyMatch(rolled -> rolled.locked() && rolled.modifierId().equals(GRANTED_PREFIX)));
        }
    }

    @Test
    void theStarChartsDomainOffersItsEngramsToTheHullBench() {
        assertEquals(Set.of(REVENANT, VIGIL), new HashSet<>(new exiledsector.charts.StarChartsDomain().hullBenchCurrencies().stream()
                .map(ItemCurrency::itemId).toList()));
    }

    @Test
    void laterCraftingNeverAddsASecondModifierOfTheGrantedEffect() {
        Item item = common();
        currency(REVENANT).use(item, new CraftingContext(store, definition -> true, Set.of()), new Random(5L));
        List<RolledModifier> withoutOtherPrefix = new java.util.ArrayList<>(item.explicits());
        withoutOtherPrefix.removeIf(rolled -> rolled.slot() == ModifierSlot.PREFIX && !rolled.modifierId().equals(GRANTED_PREFIX));

        List<List<PoolEntry>> open = AffixLayout.openSides(item.definition(), withoutOtherPrefix);

        assertTrue(open.stream().flatMap(List::stream).noneMatch(entry -> entry.effectName().equals("HULL_PERCENT")));
        assertTrue(open.stream().flatMap(List::stream).anyMatch(entry -> entry.effectName().equals("FLUX_CAPACITY_MULT")));
    }

    @Test
    void onlyNonUniqueItemsOfATakenTypeCanBeRerolled() {
        Item relic = store.add(new ItemData(RELIC, 1L));
        CraftingContext context = new CraftingContext(store, definition -> true, Set.of());

        assertFalse(currency(REVENANT).canUse(relic, context));
        assertTrue(currency(REVENANT).canUse(common(), context));
        assertFalse(currency(REVENANT).appliesTo(ItemType.STAR_CHART));
        assertFalse(currency(REVENANT).canUse(common(), new CraftingContext(store, definition -> true, Set.of(ModifierSlot.PREFIX))));
    }

    @Test
    void theCurrenciesNeverDropFromShipLootAndReserveTheirModifiers() {
        assertFalse(currency(REVENANT).dropsFromShipLoot());
        assertFalse(currency(REVENANT).synthesisable());
        assertTrue(ReservedModifiers.isReserved(GRANTED_PREFIX));
        assertEquals(VIGIL, ReservedModifiers.owner(GRANTED_SUFFIX));
    }

    @Test
    void aCurrencyDropsOnlyFromItsMinimumTier() {
        for (long seed = 0; seed < 20; seed++) {
            assertEquals(REVENANT, AnomalousDebrisCurrencies.pick(1, new Random(seed)).itemId());
        }
        Set<String> picked = new HashSet<>();
        Random random = new Random(11L);
        for (int draw = 0; draw < 40; draw++) {
            picked.add(AnomalousDebrisCurrencies.pick(6, random).itemId());
        }
        assertEquals(Set.of(REVENANT, VIGIL), picked);
    }

    @Test
    void rowsWithAnUnknownModifierOrNoTakenItemTypeAreSkipped() throws Exception {
        List<String> problems = AnomalousDebrisCurrencies.register(new JSONArray()
                .put(currencyRow("exiledSector_test_missing", "NOT_A_MODIFIER", "hull_item", 1))
                .put(currencyRow("exiledSector_test_chart", GRANTED_PREFIX, "star_chart", 1))
                .put(currencyRow(REVENANT, GRANTED_PREFIX, "hull_item", 1)));

        assertEquals(1, AnomalousDebrisCurrencies.all().size());
        assertNull(ItemCurrencies.byItemId("exiledSector_test_missing"));
        assertFalse(ReservedModifiers.isReserved(GRANTED_SUFFIX));
        assertFalse(problems.isEmpty());
    }

    @Test
    void theIntegrityCheckRejectsAPoolThatListsAReservedModifier() throws Exception {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkModifiers(new JSONArray().put(HullItemRows.modifierRow(GRANTED_PREFIX, "prefix", "HULL_PERCENT:15:15", "Revenant"))
                .put(HullItemRows.modifierRow("ARMOR_PERCENT", "suffix", "ARMOR_PERCENT:6:9", "Plated")), ItemModifiers.dataPath());
        integrity.checkDefinitions(new JSONArray().put(HullItemRows.row("pooled", "bridge", "@" + GRANTED_PREFIX, "@ARMOR_PERCENT")),
                "hull_items.csv", ItemType.HULL_ITEM);
        AnomalousDebrisCurrencies.checkRows(integrity, new JSONArray().put(currencyRow(REVENANT, GRANTED_PREFIX, "hull_item", 1))
                .put(currencyRow(VIGIL, "NOT_A_MODIFIER", "hull_item", 1)), AnomalousDebrisCurrencies.PATH);

        List<String> errors = integrity.finish().stream().filter(violation -> violation.severity() == ItemDataIntegrity.Severity.ERROR)
                .map(ItemDataIntegrity.Violation::message).toList();
        assertEquals(2, errors.size(), errors.toString());
        assertTrue(errors.stream().anyMatch(message -> message.contains("no item pool")));
        assertTrue(errors.stream().anyMatch(message -> message.contains("NOT_A_MODIFIER")));
    }

    @Test
    void theTooltipNamesTheModifierAndListsItsLines() {
        AnomalousDebrisCurrency revenant = currency(REVENANT);

        assertEquals(1, revenant.grantedLines().size());
        assertTrue(revenant.grantedLines().get(0).plain().contains("15"));
        assertTrue(revenant.description().contains("Revenant"));
        assertEquals(AnomalousDebrisCurrency.DEFAULT_COLOR, revenant.color());
    }
}
