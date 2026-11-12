package exiledsector.items;

import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetiredItemsTest {

    private static final String RETIRED_SPECIAL = "exiledSector_flagship_orders";
    private static final String RETIRED_COMMODITY = "exiledSector_old_commodity";
    private static final String RETIRED_WEAPON = "exiledSector_old_weapon";
    private static final RetiredItems.RetiredIds RETIRED = new RetiredItems.RetiredIds(Set.of(RETIRED_SPECIAL),
            Set.of(RETIRED_COMMODITY), Set.of(RETIRED_WEAPON));

    private static CargoStackAPI specialStack(String id, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(new SpecialItemData(id, null));
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    private static CargoStackAPI commodityStack(String id, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.isCommodityStack()).thenReturn(true);
        when(stack.getCommodityId()).thenReturn(id);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    private static CargoStackAPI weaponStack(String id, float size) {
        WeaponSpecAPI weapon = mock(WeaponSpecAPI.class);
        when(weapon.getWeaponId()).thenReturn(id);
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.isWeaponStack()).thenReturn(true);
        when(stack.getWeaponSpecIfWeapon()).thenReturn(weapon);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void retiredStacksLeaveTheCargoAndOthersStay() {
        CargoStackAPI retiredSpecial = specialStack(RETIRED_SPECIAL, 2);
        CargoStackAPI retiredCommodity = commodityStack(RETIRED_COMMODITY, 5);
        CargoStackAPI retiredWeapon = weaponStack(RETIRED_WEAPON, 1);
        CargoStackAPI currentSpecial = specialStack("exiledSector_current", 3);
        CargoStackAPI currentCommodity = commodityStack("supplies", 40);
        CargoAPI cargo = mock(CargoAPI.class);
        when(cargo.getStacksCopy()).thenReturn(List.of(retiredSpecial, retiredCommodity, retiredWeapon,
                currentSpecial, currentCommodity));

        assertEquals(8, RetiredItems.removeFrom(cargo, RETIRED));

        verify(cargo).removeStack(retiredSpecial);
        verify(cargo).removeStack(retiredCommodity);
        verify(cargo).removeStack(retiredWeapon);
        verify(cargo, never()).removeStack(currentSpecial);
        verify(cargo, never()).removeStack(currentCommodity);
        assertEquals(0, RetiredItems.removeFrom(null, RETIRED));
    }

    @Test
    void onlySpecsTaggedRetiredCount() {
        SpecialItemSpecAPI retiredSpecial = mock(SpecialItemSpecAPI.class);
        when(retiredSpecial.getId()).thenReturn(RETIRED_SPECIAL);
        when(retiredSpecial.getTags()).thenReturn(Set.of("hide_in_codex", RetiredItems.RETIRED_ID_TAG));
        SpecialItemSpecAPI currentSpecial = mock(SpecialItemSpecAPI.class);
        when(currentSpecial.getId()).thenReturn("exiledSector_current");
        when(currentSpecial.getTags()).thenReturn(Set.of("hide_in_codex"));
        CommoditySpecAPI untaggedCommodity = mock(CommoditySpecAPI.class);
        when(untaggedCommodity.getId()).thenReturn("supplies");
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getAllSpecialItemSpecs()).thenReturn(List.of(retiredSpecial, currentSpecial));
        when(settings.getAllCommoditySpecs()).thenReturn(List.of(untaggedCommodity));

        RetiredItems.RetiredIds retired = RetiredItems.retiredIds(settings);

        assertEquals(Set.of(RETIRED_SPECIAL), retired.specialItemIds());
        assertTrue(retired.commodityIds().isEmpty());
        assertTrue(retired.weaponIds().isEmpty());
    }
}
