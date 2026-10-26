package exiledsector.effects;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnfitWeaponsTest {

    private final Map<String, String> fittedWeapons = new LinkedHashMap<>();
    private final WeaponGroupSpec group = new WeaponGroupSpec();
    private ShipVariantAPI variant;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() {
        variant = mock(ShipVariantAPI.class);
        when(variant.getHullSpec()).thenReturn(mock(ShipHullSpecAPI.class));
        when(variant.getNonBuiltInWeaponSlots()).thenAnswer(call -> new ArrayList<>(fittedWeapons.keySet()));
        when(variant.getWeaponId(anyString())).thenAnswer(call -> fittedWeapons.get(call.<String>getArgument(0)));
        doAnswer(call -> fittedWeapons.remove(call.<String>getArgument(0))).when(variant).clearSlot(anyString());
        when(variant.getWeaponGroups()).thenReturn(List.of(group));
        cargo = mock(CargoAPI.class);
    }

    private void fitted(String slotId, WeaponType slotType, boolean builtIn, String weaponId, boolean fits) {
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.getWeaponType()).thenReturn(slotType);
        when(slot.isBuiltIn()).thenReturn(builtIn);
        WeaponSpecAPI weapon = mock(WeaponSpecAPI.class);
        when(slot.weaponFits(weapon)).thenReturn(fits);
        when(variant.getSlot(slotId)).thenReturn(slot);
        when(variant.getWeaponSpec(slotId)).thenReturn(weapon);
        fittedWeapons.put(slotId, weaponId);
        group.addSlot(slotId);
    }

    @Test
    void anEnergyWeaponLeftInASlotThatIsBallisticAgainGoesBackToCargo() {
        fitted("WS0001", WeaponType.BALLISTIC, false, "pulselaser", false);
        fitted("WS0002", WeaponType.BALLISTIC, false, "heavymauler", true);

        List<String> returned = UnfitWeapons.returnUnfitWeapons(variant, cargo);

        assertEquals(List.of("pulselaser"), returned);
        assertEquals(Map.of("WS0002", "heavymauler"), fittedWeapons);
        assertEquals(List.of("WS0002"), group.getSlots());
        verify(cargo).addWeapons("pulselaser", 1);
    }

    @Test
    void aBallisticWeaponLeftInASlotThatIsEnergyAgainGoesBackToCargo() {
        fitted("WS0001", WeaponType.ENERGY, false, "heavymauler", false);
        fitted("WS0002", WeaponType.MISSILE, false, "harpoon", true);

        assertEquals(List.of("heavymauler"), UnfitWeapons.returnUnfitWeapons(variant, cargo));
        assertEquals(Map.of("WS0002", "harpoon"), fittedWeapons);
    }

    @Test
    void builtInSlotsAreLeftAlone() {
        fitted("WS0001", WeaponType.BALLISTIC, true, "pulselaser", false);

        assertTrue(UnfitWeapons.returnUnfitWeapons(variant, cargo).isEmpty());
        verify(cargo, never()).addWeapons(anyString(), anyInt());
    }

    @Test
    void withNoCargoToReturnItToNothingIsRemoved() {
        fitted("WS0001", WeaponType.BALLISTIC, false, "pulselaser", false);

        assertTrue(UnfitWeapons.returnUnfitWeapons(variant, null).isEmpty());
        assertEquals(Map.of("WS0001", "pulselaser"), fittedWeapons);
    }
}
