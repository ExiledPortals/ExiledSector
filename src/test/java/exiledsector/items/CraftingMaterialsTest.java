package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CraftingMaterialsTest {

    private static final String PARTS = ItemDisassembly.PARTS_ITEM_ID;
    private static final String AUGMENTATION = CraftingCurrency.AUGMENTATION.itemId();

    private static CargoAPI cargoHolding(String commodityId, float quantity) {
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(commodityId))).thenReturn(quantity);
        return cargo;
    }

    @Test
    void storedAndCarriedMaterialsCountTogetherAndAreSpentFromStorageFirst() {
        CargoAPI cargo = cargoHolding(PARTS, 5f);
        Map<String, Integer> stored = new HashMap<>(Map.of(PARTS, 3));
        CraftingMaterials materials = new CraftingMaterials(cargo, stored, true);

        assertEquals(8, materials.count(PARTS));
        assertFalse(materials.take(PARTS, 9));
        verify(cargo, never()).removeItems(Mockito.any(), Mockito.any(), anyFloat());

        assertTrue(materials.take(PARTS, 4));
        assertEquals(0, materials.stored(PARTS));
        assertFalse(stored.containsKey(PARTS));
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(PARTS), 1f);
    }

    @Test
    void newMaterialsGoToStorageOnlyWhileStoringIsOn() {
        CargoAPI cargo = mock(CargoAPI.class);
        Map<String, Integer> stored = new HashMap<>();

        new CraftingMaterials(cargo, stored, true).add(AUGMENTATION, 2);
        assertEquals(Map.of(AUGMENTATION, 2), stored);
        verify(cargo, never()).addSpecial(Mockito.any(), anyFloat());

        new CraftingMaterials(cargo, stored, false).add(AUGMENTATION, 1);
        assertEquals(Map.of(AUGMENTATION, 2), stored);
        verify(cargo).addSpecial(MaterialItems.data(AUGMENTATION), 1f);
    }

    @Test
    void settlingWhileStoringMovesEveryPartAndKernelOutOfCargo() {
        CargoAPI cargo = cargoHolding(PARTS, 40f);
        when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(AUGMENTATION))).thenReturn(2f);
        Map<String, Integer> stored = new HashMap<>(Map.of(PARTS, 5));

        assertEquals(42, new CraftingMaterials(cargo, stored, true).settle());

        assertEquals(Map.of(PARTS, 45, AUGMENTATION, 2), stored);
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(PARTS), 40f);
        verify(cargo).removeItems(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(AUGMENTATION), 2f);
    }

    @Test
    void settlingWithStoringOffReturnsEverythingToCargo() {
        CargoAPI cargo = mock(CargoAPI.class);
        Map<String, Integer> stored = new HashMap<>(Map.of(PARTS, 12, AUGMENTATION, 1));

        assertEquals(13, new CraftingMaterials(cargo, stored, false).settle());

        assertTrue(stored.isEmpty());
        verify(cargo).addSpecial(MaterialItems.data(PARTS), 12f);
        verify(cargo).addSpecial(MaterialItems.data(AUGMENTATION), 1f);
    }

    @Test
    void cargoOnlyMaterialsNeverTouchTheStore() {
        CargoAPI cargo = cargoHolding(PARTS, 3f);
        CraftingMaterials materials = CraftingMaterials.cargoOnly(cargo);

        assertFalse(materials.isStoring());
        assertEquals(0, materials.settle());
        assertEquals(3, materials.count(PARTS));
    }

    @Test
    void thePartsAndAllThreeKernelsAreManaged() {
        assertEquals(1 + CraftingCurrency.values().length, MaterialItems.ids().size());
        assertTrue(MaterialItems.ids().contains(PARTS));
    }
}
