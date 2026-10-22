package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaterialMigrationTest {

    @Test
    void legacyCommodityStacksBecomeSpecialItemStacks() {
        CargoAPI cargo = mock(CargoAPI.class);
        String fixation = CraftingCurrency.FIXATION.itemId();
        when(cargo.getCommodityQuantity(anyString())).thenReturn(0f);
        when(cargo.getCommodityQuantity(ItemDisassembly.PARTS_ITEM_ID)).thenReturn(37f);
        when(cargo.getCommodityQuantity(fixation)).thenReturn(2f);

        assertEquals(39, MaterialMigration.convert(cargo));

        verify(cargo).removeCommodity(ItemDisassembly.PARTS_ITEM_ID, 37);
        verify(cargo).addSpecial(MaterialItems.data(ItemDisassembly.PARTS_ITEM_ID), 37);
        verify(cargo).removeCommodity(fixation, 2);
        verify(cargo).addSpecial(MaterialItems.data(fixation), 2);
    }

    @Test
    void cargoWithoutLegacyMaterialsIsLeftAlone() {
        CargoAPI cargo = mock(CargoAPI.class);

        assertEquals(0, MaterialMigration.convert(cargo));
        assertEquals(0, MaterialMigration.convert(null));

        verify(cargo, never()).removeCommodity(anyString(), anyFloat());
        verify(cargo, never()).addSpecial(any(), anyFloat());
    }
}
