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
import java.util.List;
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

class CraftingMaterialFamiliesTest {

    private static final String PURGE_ID = "exiledSector_test_purge_kernel";

    private static final class PurgeCraftingItem implements CraftingMaterialFamily {

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
        public boolean growable() {
            return true;
        }

        @Override
        public int feedstockCost() {
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

    private final CraftingMaterialFamily purge = new PurgeCraftingItem();
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
        CraftingMaterialFamilies.unregister(purge);
        ItemDefinitions.clear();
    }

    private void owns(String itemId, float quantity) {
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(itemId))).thenReturn(quantity);
    }

    private record Expected(String itemId, String key, int feedstockCost, float dropWeight, MaterialTier tier, boolean growable) {
    }

    @Test
    void materialsGetRarerAndCostlierToGrowInTheIntendedOrder() {
        List<String> commonestFirst = List.of(
                Nanoswarm.AUGMENTATION.itemId(), Nanoswarm.FUNDAMENTAL.itemId(), Nanoswarm.RESHAPING.itemId(),
                Nanoswarm.CORRUPTION.itemId(), CraftingInhibitor.PREFIX.itemId(), CraftingInhibitor.SUFFIX.itemId(),
                Nanoswarm.METAMORPH.itemId(), Nanoswarm.ANNULMENT.itemId(), Nanoswarm.TUNING.itemId(), Nanoswarm.IMMUTABLE.itemId(),
                Nanoswarm.REPLICATION.itemId());
        float previousWeight = Float.MAX_VALUE;
        int previousCost = 0;
        for (String itemId : commonestFirst) {
            Nanoswarm craftingItem = Nanoswarm.byItemId(itemId);
            GrowableMaterial material = craftingItem != null ? craftingItem : CraftingInhibitor.byItemId(itemId);
            float weight = craftingItem != null ? craftingItem.dropWeight() : CraftingInhibitor.byItemId(itemId).dropWeight();
            assertTrue(weight <= previousWeight, itemId + " drops more often than the material before it");
            previousWeight = weight;
            if (material.growable()) {
                assertTrue(material.feedstockCost() >= previousCost, itemId + " is cheaper to grow than a commoner material");
                previousCost = material.feedstockCost();
            }
        }
        assertTrue(CraftingInhibitor.PREFIX.dropWeight() < Nanoswarm.CORRUPTION.dropWeight());
        assertTrue(Nanoswarm.METAMORPH.dropWeight() < CraftingInhibitor.SUFFIX.dropWeight());
    }

    @Test
    void everyBuiltInCraftingItemIsRegisteredCommonestFirstWithItsValues() {
        List<Expected> expected = List.of(
                new Expected("exiledSector_augmentation_nanoswarm", "augmentation", 10, 6f, MaterialTier.MINOR, true),
                new Expected("exiledSector_fundamental_nanoswarm", "fundamental", 15, 4f, MaterialTier.MINOR, true),
                new Expected("exiledSector_reshaping_nanoswarm", "reshaping", 20, 3f, MaterialTier.MAJOR, true),
                new Expected("exiledSector_corrupted_nanoswarm", "corruption", 50, 1.5f, MaterialTier.EXOTIC, true),
                new Expected("exiledSector_metamorph_nanoswarm", "metamorph", 150, 0.4f, MaterialTier.MYTHIC, true),
                new Expected("exiledSector_annulment_nanoswarm", "annulment", 200, 0.3f, MaterialTier.MYTHIC, true),
                new Expected("exiledSector_tuning_nanoswarm", "tuning", 200, 0.3f, MaterialTier.MYTHIC, true),
                new Expected("exiledSector_immutable_nanoswarm", "immutable", 300, 0.2f, MaterialTier.MYTHIC, true),
                new Expected("exiledSector_replicator_nanoseed", "replication", 0, 0.035f, MaterialTier.MYTHIC, false));

        assertEquals(Arrays.asList(Nanoswarm.values()), CraftingMaterialFamilies.all());
        assertEquals(expected, CraftingMaterialFamilies.all().stream().map(craftingItem -> new Expected(craftingItem.itemId(), craftingItem.key(),
                craftingItem.feedstockCost(), craftingItem.dropWeight(), craftingItem.tier(), craftingItem.growable())).toList());
        for (Nanoswarm craftingItem : Nanoswarm.values()) {
            assertSame(craftingItem, CraftingMaterialFamilies.byItemId(craftingItem.itemId()));
            assertEquals(craftingItem.name().toLowerCase(java.util.Locale.ROOT), craftingItem.key());
        }
        assertEquals(CraftingMaterialFamilies.all().stream().filter(craftingItem -> craftingItem != Nanoswarm.ANNULMENT).toList(),
                CraftingMaterialFamilies.forType(ItemType.SUBROUTINE));
        assertEquals(CraftingMaterialFamilies.all(), CraftingMaterialFamilies.forType(ItemType.HULL_ITEM));
        assertEquals(List.of(), CraftingMaterialFamilies.forType(null));
        assertNull(CraftingMaterialFamilies.byItemId(ItemDisassembly.FEEDSTOCK_ITEM_ID));
        assertNull(CraftingMaterialFamilies.byItemId(null));
    }

    @Test
    void theBuiltInsKeepTheirWardAndCopyRules() {
        Set<Nanoswarm> respectsInhibitors = EnumSet.noneOf(Nanoswarm.class);
        Set<Nanoswarm> createsCopy = EnumSet.noneOf(Nanoswarm.class);
        Set<Nanoswarm> mayChangeDefinition = EnumSet.noneOf(Nanoswarm.class);
        for (Nanoswarm craftingItem : Nanoswarm.values()) {
            if (craftingItem.respectsInhibitors()) {
                respectsInhibitors.add(craftingItem);
            }
            if (craftingItem.createsCopy()) {
                createsCopy.add(craftingItem);
            }
            if (craftingItem.mayChangeDefinition()) {
                mayChangeDefinition.add(craftingItem);
            }
        }

        assertEquals(EnumSet.of(Nanoswarm.TUNING, Nanoswarm.RESHAPING, Nanoswarm.ANNULMENT,
                Nanoswarm.IMMUTABLE), respectsInhibitors);
        assertEquals(EnumSet.of(Nanoswarm.REPLICATION), createsCopy);
        assertEquals(EnumSet.of(Nanoswarm.METAMORPH, Nanoswarm.CORRUPTION), mayChangeDefinition);
    }

    @Test
    void kernelDropsListEveryCraftingItemAndInhibitorByWeight() {
        List<ItemDrops.MaterialDrop> builtInDrops = new ArrayList<>();
        for (Nanoswarm craftingItem : Nanoswarm.values()) {
            builtInDrops.add(new ItemDrops.MaterialDrop(craftingItem.itemId(), craftingItem.dropWeight()));
        }
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            builtInDrops.add(new ItemDrops.MaterialDrop(inhibitor.itemId(), inhibitor.dropWeight()));
        }
        assertEquals(builtInDrops, ItemDrops.materialDrops());
    }

    @Test
    void anAddedCraftingItemJoinsTheMaterialsDropsAndCraftingForItsTypesOnly() {
        CraftingMaterialFamilies.register(purge);
        Item hullItem = store.add(ItemDefinitions.get(HullItemRows.MILITARY), 4L);
        Item subroutine = store.add(ItemDefinitions.get("volume"), 4L);
        int explicitsBefore = hullItem.explicits().size();
        owns(PURGE_ID, 2f);

        assertSame(purge, CraftingMaterialFamilies.byItemId(PURGE_ID));
        assertEquals(purge, CraftingMaterialFamilies.all().get(Nanoswarm.values().length));
        assertTrue(CraftingMaterialFamilies.forType(ItemType.HULL_ITEM).contains(purge));
        assertFalse(CraftingMaterialFamilies.forType(ItemType.SUBROUTINE).contains(purge));
        assertTrue(MaterialItems.isMaterial(PURGE_ID));
        assertEquals(MaterialTier.MAJOR, MaterialItems.tier(PURGE_ID));
        assertEquals(Color.CYAN, MaterialItems.color(PURGE_ID));
        assertFalse(ItemCrafting.canUse(purge, subroutine, definition -> true));
        assertTrue(ItemCrafting.canUse(purge, hullItem, definition -> true));
        assertSame(hullItem, ItemCrafting.use(purge, hullItem, CraftingMaterials.cargoOnly(cargo), store, new Random(2L), definition -> true));
        assertEquals(explicitsBefore - 1, hullItem.explicits().size());
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(PURGE_ID), 1);
        assertTrue(ItemCrafting.inhibitorsSpentBy(purge, hullItem, List.of(CraftingInhibitor.PREFIX, CraftingInhibitor.SUFFIX)).isEmpty());
        assertTrue(ItemDrops.materialDrops().stream().anyMatch(drop -> drop.itemId().equals(PURGE_ID)));
    }

    @Test
    void craftingItemsCannotReuseAMaterialIdOrKey() {
        CraftingMaterialFamilies.register(purge);

        assertThrows(IllegalArgumentException.class, () -> CraftingMaterialFamilies.register(purge));
        assertThrows(IllegalArgumentException.class, () -> CraftingMaterialFamilies.register(Nanoswarm.AUGMENTATION));
        assertThrows(IllegalArgumentException.class, () -> CraftingMaterialFamilies.register(null));
        assertFalse(CraftingMaterialFamilies.unregister(Nanoswarm.AUGMENTATION));
        assertFalse(CraftingMaterialFamilies.unregister(null));
        assertTrue(CraftingMaterialFamilies.unregister(purge));
        assertFalse(CraftingMaterialFamilies.unregister(purge));
        assertEquals(Arrays.asList(Nanoswarm.values()), CraftingMaterialFamilies.all());
        assertFalse(MaterialItems.isMaterial(PURGE_ID));
    }
}
