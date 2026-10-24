package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.CraftingContext;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingWard;
import exiledsector.items.Item;
import exiledsector.items.ItemCurrencies;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemType;
import exiledsector.items.MaterialItems;
import exiledsector.items.MaterialTier;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartFixtures;
import exiledsector.ui.chart.StarChartCrafting.Refusal;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartCraftingTest {

    private static final String SURVEY_ID = "exiledSector_test_chart_survey";
    private static final String COPY_ID = "exiledSector_test_chart_copy";

    private record TestChartCurrency(String itemId, String key, boolean copies) implements ItemCurrency {

        @Override
        public float dropWeight() {
            return 0f;
        }

        @Override
        public MaterialTier tier() {
            return MaterialTier.MAJOR;
        }

        @Override
        public Color color() {
            return Color.ORANGE;
        }

        @Override
        public boolean synthesisable() {
            return false;
        }

        @Override
        public int partsCost() {
            return 0;
        }

        @Override
        public boolean appliesTo(ItemType type) {
            return type == ItemType.STAR_CHART;
        }

        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return true;
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            return copies ? context.store().add(ItemData.of(item)) : item;
        }

        @Override
        public boolean createsCopy() {
            return copies;
        }
    }

    private final ItemCurrency survey = new TestChartCurrency(SURVEY_ID, "testChartSurvey", false);
    private final ItemCurrency copier = new TestChartCurrency(COPY_ID, "testChartCopy", true);
    private final CargoAPI cargo = mock(CargoAPI.class);
    private final ItemStore store = ItemStore.forStarCharts();
    private Item chart;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        chart = store.add(ItemData.rolledAtTier(ItemDefinitions.get("star_chart_binary_far"), 8L, 6));
    }

    @AfterEach
    void tearDown() {
        StarChartCurrency.unregisterAll();
        ItemCurrencies.unregister(survey);
        ItemCurrencies.unregister(copier);
        StarChartFixtures.clear();
    }

    private void owns(String itemId, float quantity) {
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(itemId))).thenReturn(quantity);
    }

    @Test
    void theChartCurrenciesAndTheExtendedKernelsJoinTheRowOnlyOnceRegistered() {
        assertEquals(List.of(), StarChartCrafting.currencies());
        for (CraftingCurrency currency : CraftingCurrency.values()) {
            assertEquals(Refusal.CANNOT_USE, refusalWithOne(currency));
        }

        StarChartCurrency.registerAll();

        assertEquals(List.of(CraftingCurrency.AUGMENTATION, CraftingCurrency.ATTUNEMENT, CraftingCurrency.CORRUPTION, CraftingCurrency.TRANSPOSITION,
                CraftingCurrency.RECALIBRATION, StarChartCurrency.REFORGE, StarChartCurrency.ANNULMENT, StarChartCurrency.CARTOGRAPHY),
                StarChartCrafting.currencies());
        for (ItemCurrency currency : List.of(CraftingCurrency.AUGMENTATION, CraftingCurrency.ATTUNEMENT, CraftingCurrency.RECALIBRATION, CraftingCurrency.CORRUPTION,
                StarChartCurrency.REFORGE, StarChartCurrency.ANNULMENT, StarChartCurrency.CARTOGRAPHY)) {
            assertEquals(Refusal.NONE, refusalWithOne(currency), currency.key());
        }
        for (CraftingCurrency currency : List.of(CraftingCurrency.SUBSTITUTION, CraftingCurrency.FIXATION,
                CraftingCurrency.REPLICATION)) {
            assertEquals(Refusal.CANNOT_USE, refusalWithOne(currency), currency.key());
        }
    }

    @Test
    void eachChartCurrencyExplainsWhyItCannotBeUsed() {
        StarChartCurrency.registerAll();
        CraftingMaterials materials = CraftingMaterials.cargoOnly(cargo);
        when(cargo.getQuantity(eq(CargoAPI.CargoItemType.SPECIAL), any())).thenReturn(100f);
        Item topTier = store.add(ItemData.rolledAtTier(ItemDefinitions.get("star_chart_single"), 3L, 15));
        Item unique = store.add(ItemData.rolledAtTier(ItemDefinitions.get(StarChartFixtures.TEST_UNIQUE), 3L, 2));

        assertEquals(Refusal.MAX_TIER, StarChartCrafting.refusal(StarChartCurrency.CARTOGRAPHY, topTier, store, materials, definition -> true));
        assertEquals(Refusal.UNIQUE, StarChartCrafting.refusal(StarChartCurrency.REFORGE, unique, store, materials, definition -> true));
        assertEquals(Refusal.UNIQUE, StarChartCrafting.refusal(StarChartCurrency.ANNULMENT, unique, store, materials, definition -> true));
        assertEquals(Refusal.UNIQUE, StarChartCrafting.refusal(CraftingCurrency.AUGMENTATION, unique, store, materials, definition -> true));
        assertEquals(Refusal.NOT_UNIQUE, StarChartCrafting.refusal(CraftingCurrency.TRANSPOSITION, chart, store, materials, definition -> true));
        assertEquals(Refusal.CANNOT_USE, StarChartCrafting.refusal(CraftingCurrency.TRANSPOSITION, unique, store, materials, definition -> true));
        assertEquals(Refusal.WARD_BLOCKS, StarChartCrafting.refusal(StarChartCurrency.ANNULMENT, chart, store, materials, definition -> true,
                List.of(CraftingWard.PREFIX, CraftingWard.SUFFIX)));
        Random random = new Random(4L);
        while (StarChartCrafting.use(StarChartCurrency.ANNULMENT, chart, store, materials, random, definition -> true) != null) {
            assertTrue(chart.explicits().size() < 6);
        }
        assertEquals(Refusal.NO_MODIFIERS, StarChartCrafting.refusal(StarChartCurrency.ANNULMENT, chart, store, materials, definition -> true));
        while (StarChartCrafting.use(CraftingCurrency.AUGMENTATION, chart, store, materials, random, definition -> true) != null) {
            assertTrue(chart.explicits().size() <= 6);
        }
        assertEquals(6, chart.explicits().size());
        assertEquals(Refusal.FULL, StarChartCrafting.refusal(CraftingCurrency.AUGMENTATION, chart, store, materials, definition -> true));
    }

    @Test
    void aChartCurrencyKeepsTheChartSelectedAndATranspositionSelectsTheReplacement() {
        StarChartCurrency.registerAll();
        CraftingMaterials materials = CraftingMaterials.cargoOnly(cargo);
        owns(StarChartCurrency.CARTOGRAPHY.itemId(), 1f);
        owns(CraftingCurrency.TRANSPOSITION.itemId(), 1f);

        StarChartCrafting.Used raised = StarChartCrafting.use(StarChartCurrency.CARTOGRAPHY, chart, store, materials, new Random(1L),
                definition -> true);

        assertSame(chart, raised.selectedAfter());
        assertEquals(7, chart.tier());
        Item unique = store.add(ItemData.rolledAtTier(ItemDefinitions.get(StarChartFixtures.TEST_UNIQUE), 3L, 8));
        StarChartCrafting.Used transposed = StarChartCrafting.use(CraftingCurrency.TRANSPOSITION, unique, store, materials, new Random(1L),
                definition -> true);
        assertEquals(StarChartFixtures.TEST_GATED_UNIQUE, transposed.selectedAfter().definitionId());
        assertSame(transposed.selectedAfter(), store.find(unique.id()));
        assertEquals(8, transposed.selectedAfter().tier());
    }

    private Refusal refusalWithOne(ItemCurrency currency) {
        owns(currency.itemId(), 1f);
        return StarChartCrafting.refusal(currency, chart, store, CraftingMaterials.cargoOnly(cargo), definition -> true);
    }

    @Test
    void aRegisteredChartCurrencyJoinsTheRowAndApplies() {
        ItemCurrencies.register(survey);
        owns(SURVEY_ID, 2f);
        CraftingMaterials materials = CraftingMaterials.cargoOnly(cargo);

        assertEquals(List.of(survey), StarChartCrafting.currencies());
        assertEquals(Refusal.NONE, StarChartCrafting.refusal(survey, chart, store, materials, definition -> true));
        StarChartCrafting.Used used = StarChartCrafting.use(survey, chart, store, materials, new Random(1L), definition -> true);

        assertSame(chart, used.result());
        assertSame(chart, used.selectedAfter());
        assertEquals(1, store.owned().size());
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(SURVEY_ID), 1);
    }

    @Test
    void aCopyingCurrencyKeepsTheOriginalSelectedAndStoresTheCopy() {
        ItemCurrencies.register(copier);
        owns(COPY_ID, 1f);

        StarChartCrafting.Used used = StarChartCrafting.use(copier, chart, store, CraftingMaterials.cargoOnly(cargo), new Random(1L),
                definition -> true);

        assertSame(chart, used.selectedAfter());
        assertNotSame(chart, used.result());
        assertEquals(2, store.owned().size());
        assertSame(used.result(), store.find(used.result().id()));
    }

    @Test
    void theNoticeForACopySaysWhereTheCopyWentWhileOtherCurrenciesNameTheChange() {
        ItemCurrencies.register(copier);
        ItemCurrencies.register(survey);
        owns(COPY_ID, 1f);
        owns(SURVEY_ID, 1f);
        CraftingMaterials materials = CraftingMaterials.cargoOnly(cargo);
        String nameBefore = chart.name();

        StarChartCrafting.Used copied = StarChartCrafting.use(copier, chart, store, materials, new Random(1L), definition -> true);
        StarChartCrafting.Used surveyed = StarChartCrafting.use(survey, chart, store, materials, new Random(1L), definition -> true);

        assertEquals("chart.ui.currency.copied", StarChartCrafting.usedNoticeKey(copier));
        assertEquals("chart.ui.currency.used", StarChartCrafting.usedNoticeKey(survey));
        assertEquals(COPY_ID + " used: a copy of " + nameBefore + " went into storage as " + copied.result().name() + ".",
                StarChartCrafting.usedNotice(copier, nameBefore, copied));
        assertEquals(SURVEY_ID + " used: " + nameBefore + " is now " + surveyed.result().name() + ".",
                StarChartCrafting.usedNotice(survey, nameBefore, surveyed));
    }

    @Test
    void refusalsExplainWhyACurrencyCannotBeUsed() throws Exception {
        ItemCurrencies.register(survey);
        CraftingMaterials materials = CraftingMaterials.cargoOnly(cargo);

        assertEquals(Refusal.NONE_OWNED, StarChartCrafting.refusal(survey, chart, store, materials, definition -> true));
        assertNull(StarChartCrafting.use(survey, chart, store, materials, new Random(1L), definition -> true));
        verify(cargo, never()).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(SURVEY_ID), 1);

        owns(SURVEY_ID, 1f);
        assertEquals(Refusal.NO_CHART, StarChartCrafting.refusal(survey, null, store, materials, definition -> true));
        Item loose = ItemData.rolledAtTier(ItemDefinitions.get("star_chart_single"), 9L, 2).preview();
        assertEquals(Refusal.NO_CHART, StarChartCrafting.refusal(survey, loose, store, materials, definition -> true));
        assertEquals(Refusal.NO_CHART, StarChartCrafting.refusal(survey, chart, new ItemStore(), materials, definition -> true));
        assertEquals(Refusal.CORRUPTED, StarChartCrafting.refusal(survey, withFlag("c", "NOTHING"), store, materials, definition -> true));
        assertEquals(Refusal.MIRRORED, StarChartCrafting.refusal(survey, withFlag("m", Boolean.TRUE), store, materials, definition -> true));
    }

    private Item withFlag(String key, Object value) throws Exception {
        JSONObject json = new JSONObject(ItemData.of(chart).encode());
        json.put(key, value);
        Item flagged = store.add(ItemData.ofStarChart(new SpecialItemData(ItemData.STAR_CHART_ITEM_ID, json.toString())));
        assertTrue(flagged.corrupted() || flagged.mirrored());
        return flagged;
    }
}
