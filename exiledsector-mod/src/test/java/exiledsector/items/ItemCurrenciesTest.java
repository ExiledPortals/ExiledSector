package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemCurrenciesTest {

    private static final String PURGE_ID = "exiledSector_test_purge_kernel";

    private static final class PurgeCurrency implements ItemCurrency {

        @Override
        public String itemId() {
            return PURGE_ID;
        }

        @Override
        public String key() {
            return "purge";
        }

        @Override
        public float dropWeight() {
            return 2f;
        }

        @Override
        public MaterialTier tier() {
            return MaterialTier.MAJOR;
        }

        @Override
        public Color color() {
            return Color.CYAN;
        }

        @Override
        public boolean synthesisable() {
            return true;
        }

        @Override
        public int partsCost() {
            return 30;
        }

        @Override
        public boolean appliesTo(ItemType type) {
            return type == ItemType.HULL_ITEM;
        }

        @Override
        public boolean canUse(Item item, CraftingContext context) {
            return !item.explicits().isEmpty();
        }

        @Override
        public Item use(Item item, CraftingContext context, Random random) {
            List<RolledModifier> remaining = new ArrayList<>(item.explicits());
            remaining.remove(random.nextInt(remaining.size()));
            item.replaceExplicits(remaining);
            item.refreezeName();
            return item;
        }
    }

    private final ItemCurrency purge = new PurgeCurrency();
    private final ItemStore store = new ItemStore();
    private final CargoAPI cargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(HullItemRows.row(HullItemRows.MILITARY, "bridge", HullItemRows.MILITARY_PREFIXES, HullItemRows.MILITARY_SUFFIXES))
                .put(ItemFixtures.subroutineRow("volume")));
    }

    @AfterEach
    void tearDown() {
        ItemCurrencies.unregister(purge);
        ItemDefinitions.clear();
    }

    private void owns(String itemId, float quantity) {
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(itemId))).thenReturn(quantity);
    }

    private record Expected(String itemId, String key, int partsCost, float dropWeight, MaterialTier tier, boolean synthesisable) {
    }

    @Test
    void materialsGetRarerAndCostlierToGrowInTheIntendedOrder() {
        List<String> commonestFirst = List.of(
                CraftingCurrency.AUGMENTATION.itemId(), CraftingCurrency.ATTUNEMENT.itemId(), CraftingCurrency.SUBSTITUTION.itemId(),
                CraftingCurrency.CORRUPTION.itemId(), CraftingWard.PREFIX.itemId(), CraftingWard.SUFFIX.itemId(),
                CraftingCurrency.TRANSPOSITION.itemId(), CraftingCurrency.ANNULMENT.itemId(), CraftingCurrency.RECALIBRATION.itemId(), CraftingCurrency.FIXATION.itemId(),
                CraftingCurrency.REPLICATION.itemId());
        float previousWeight = Float.MAX_VALUE;
        int previousCost = 0;
        for (String itemId : commonestFirst) {
            CraftingCurrency currency = CraftingCurrency.byItemId(itemId);
            float weight = currency != null ? currency.dropWeight() : CraftingWard.byItemId(itemId).dropWeight();
            assertTrue(weight <= previousWeight, itemId + " drops more often than the material before it");
            previousWeight = weight;
            if (currency != null && currency.synthesisable()) {
                assertTrue(currency.partsCost() > previousCost, itemId + " is not dearer to grow than a commoner material");
                previousCost = currency.partsCost();
            }
        }
        assertTrue(CraftingWard.PREFIX.dropWeight() < CraftingCurrency.CORRUPTION.dropWeight());
        assertTrue(CraftingCurrency.TRANSPOSITION.dropWeight() < CraftingWard.SUFFIX.dropWeight());
    }

    @Test
    void everyBuiltInCurrencyIsRegisteredCommonestFirstWithItsValues() {
        List<Expected> expected = List.of(
                new Expected("exiledSector_augmentation_kernel", "augmentation", 10, 6f, MaterialTier.MINOR, true),
                new Expected("exiledSector_attunement_kernel", "attunement", 15, 4f, MaterialTier.MINOR, true),
                new Expected("exiledSector_substitution_kernel", "substitution", 20, 3f, MaterialTier.MAJOR, true),
                new Expected("exiledSector_corruption_kernel", "corruption", 50, 1.5f, MaterialTier.EXOTIC, true),
                new Expected("exiledSector_transposition_kernel", "transposition", 0, 0.4f, MaterialTier.MYTHIC, false),
                new Expected("exiledSector_annulment_kernel", "annulment", 0, 0.3f, MaterialTier.MYTHIC, false),
                new Expected("exiledSector_recalibration_kernel", "recalibration", 0, 0.3f, MaterialTier.MYTHIC, false),
                new Expected("exiledSector_fixation_kernel", "fixation", 0, 0.2f, MaterialTier.MYTHIC, false),
                new Expected("exiledSector_replication_kernel", "replication", 0, 0.035f, MaterialTier.MYTHIC, false));

        assertEquals(Arrays.asList(CraftingCurrency.values()), ItemCurrencies.all());
        assertEquals(expected, ItemCurrencies.all().stream().map(currency -> new Expected(currency.itemId(), currency.key(),
                currency.partsCost(), currency.dropWeight(), currency.tier(), currency.synthesisable())).toList());
        for (CraftingCurrency currency : CraftingCurrency.values()) {
            assertSame(currency, ItemCurrencies.byItemId(currency.itemId()));
            assertEquals(currency.name().toLowerCase(java.util.Locale.ROOT), currency.key());
        }
        assertEquals(ItemCurrencies.all().stream().filter(currency -> currency != CraftingCurrency.ANNULMENT).toList(),
                ItemCurrencies.forType(ItemType.SUBROUTINE));
        assertEquals(ItemCurrencies.all(), ItemCurrencies.forType(ItemType.HULL_ITEM));
        assertEquals(List.of(), ItemCurrencies.forType(null));
        assertNull(ItemCurrencies.byItemId(ItemDisassembly.PARTS_ITEM_ID));
        assertNull(ItemCurrencies.byItemId(null));
    }

    @Test
    void theBuiltInsKeepTheirWardAndCopyRules() {
        Set<CraftingCurrency> respectsWards = EnumSet.noneOf(CraftingCurrency.class);
        Set<CraftingCurrency> createsCopy = EnumSet.noneOf(CraftingCurrency.class);
        Set<CraftingCurrency> mayChangeDefinition = EnumSet.noneOf(CraftingCurrency.class);
        for (CraftingCurrency currency : CraftingCurrency.values()) {
            if (currency.respectsWards()) {
                respectsWards.add(currency);
            }
            if (currency.createsCopy()) {
                createsCopy.add(currency);
            }
            if (currency.mayChangeDefinition()) {
                mayChangeDefinition.add(currency);
            }
        }

        assertEquals(EnumSet.of(CraftingCurrency.RECALIBRATION, CraftingCurrency.SUBSTITUTION, CraftingCurrency.ANNULMENT,
                CraftingCurrency.FIXATION), respectsWards);
        assertEquals(EnumSet.of(CraftingCurrency.REPLICATION), createsCopy);
        assertEquals(EnumSet.of(CraftingCurrency.TRANSPOSITION, CraftingCurrency.CORRUPTION), mayChangeDefinition);
    }

    @Test
    void kernelDropsPickExactlyAsTheOldFixedListDid() {
        record Drop(String itemId, float weight) {
        }
        List<Drop> legacyDrops = new ArrayList<>();
        for (CraftingCurrency currency : CraftingCurrency.values()) {
            legacyDrops.add(new Drop(currency.itemId(), currency.dropWeight()));
        }
        for (CraftingWard ward : CraftingWard.values()) {
            legacyDrops.add(new Drop(ward.itemId(), ward.dropWeight()));
        }
        Random legacyRandom = new Random(17L);
        Random registryRandom = new Random(17L);
        for (int pick = 0; pick < 5000; pick++) {
            assertEquals(WeightedPick.pick(legacyDrops, Drop::weight, legacyRandom).itemId(), ItemDrops.pickKernel(registryRandom), "pick " + pick);
        }
    }

    @Test
    void anAddedCurrencyJoinsTheMaterialsDropsAndCraftingForItsTypesOnly() {
        ItemCurrencies.register(purge);
        Item hullItem = store.add(ItemDefinitions.get(HullItemRows.MILITARY), 4L);
        Item subroutine = store.add(ItemDefinitions.get("volume"), 4L);
        int explicitsBefore = hullItem.explicits().size();
        owns(PURGE_ID, 2f);

        assertSame(purge, ItemCurrencies.byItemId(PURGE_ID));
        assertEquals(purge, ItemCurrencies.all().get(CraftingCurrency.values().length));
        assertTrue(ItemCurrencies.forType(ItemType.HULL_ITEM).contains(purge));
        assertFalse(ItemCurrencies.forType(ItemType.SUBROUTINE).contains(purge));
        assertTrue(MaterialItems.isMaterial(PURGE_ID));
        assertEquals(MaterialTier.MAJOR, MaterialItems.tier(PURGE_ID));
        assertEquals(Color.CYAN, MaterialItems.color(PURGE_ID));
        assertFalse(ItemCrafting.canUse(purge, subroutine, definition -> true));
        assertTrue(ItemCrafting.canUse(purge, hullItem, definition -> true));
        assertSame(hullItem, ItemCrafting.use(purge, hullItem, CraftingMaterials.cargoOnly(cargo), store, new Random(2L), definition -> true));
        assertEquals(explicitsBefore - 1, hullItem.explicits().size());
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(PURGE_ID), 1);
        assertTrue(ItemCrafting.wardsSpentBy(purge, hullItem, List.of(CraftingWard.PREFIX, CraftingWard.SUFFIX)).isEmpty());
        Map<String, Integer> drops = new LinkedHashMap<>();
        Random random = new Random(5L);
        for (int pick = 0; pick < 4000; pick++) {
            drops.merge(ItemDrops.pickKernel(random), 1, Integer::sum);
        }
        assertTrue(drops.getOrDefault(PURGE_ID, 0) > 0);
    }

    @Test
    void currenciesCannotReuseAMaterialIdOrKey() {
        ItemCurrencies.register(purge);

        assertThrows(IllegalArgumentException.class, () -> ItemCurrencies.register(purge));
        assertThrows(IllegalArgumentException.class, () -> ItemCurrencies.register(CraftingCurrency.AUGMENTATION));
        assertThrows(IllegalArgumentException.class, () -> ItemCurrencies.register(null));
        assertFalse(ItemCurrencies.unregister(CraftingCurrency.AUGMENTATION));
        assertFalse(ItemCurrencies.unregister(null));
        assertTrue(ItemCurrencies.unregister(purge));
        assertFalse(ItemCurrencies.unregister(purge));
        assertEquals(Arrays.asList(CraftingCurrency.values()), ItemCurrencies.all());
        assertFalse(MaterialItems.isMaterial(PURGE_ID));
    }
}
