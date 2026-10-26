package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DamageConversionTest {

    private static final DamageConversion HARDPOINT_KINETIC_TO_HE = new DamageConversion("a", WeaponType.BALLISTIC, null,
            DamageConversion.Mounting.HARDPOINT, false, DamageType.KINETIC, DamageType.HIGH_EXPLOSIVE);
    private static final DamageConversion TURRET_BEAM_TO_HE = new DamageConversion("b", WeaponType.ENERGY, null,
            DamageConversion.Mounting.TURRET, true, DamageType.ENERGY, DamageType.HIGH_EXPLOSIVE);
    private static final DamageConversion SMALL_ENERGY_TO_EMP = new DamageConversion("c", WeaponType.ENERGY, WeaponSize.SMALL,
            DamageConversion.Mounting.ANY, false, null, null);
    private static final List<DamageConversion> CONVERSIONS = List.of(HARDPOINT_KINETIC_TO_HE, TURRET_BEAM_TO_HE, SMALL_ENERGY_TO_EMP);

    private static WeaponSlotAPI mount(WeaponType type, WeaponSize size, boolean hardpoint) {
        WeaponSlotAPI mount = mock(WeaponSlotAPI.class);
        when(mount.getWeaponType()).thenReturn(type);
        when(mount.getSlotSize()).thenReturn(size);
        when(mount.isHardpoint()).thenReturn(hardpoint);
        when(mount.isTurret()).thenReturn(!hardpoint);
        return mount;
    }

    private static WeaponAPI weapon(boolean beam) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.isBeam()).thenReturn(beam);
        return weapon;
    }

    private static DamageConversion first(WeaponAPI weapon, WeaponSlotAPI mount, DamageType sourceType, Set<DamageConversion> owned) {
        return DamageConversion.first(weapon, mount, sourceType, CONVERSIONS,
                key -> owned.stream().anyMatch(conversion -> conversion.flagKey().equals(key)) ? 1 : 0);
    }

    @Test
    void aConversionAppliesOnlyToWeaponsInItsMountTypeSizeAndMounting() {
        Set<DamageConversion> owned = Set.of(HARDPOINT_KINETIC_TO_HE, TURRET_BEAM_TO_HE, SMALL_ENERGY_TO_EMP);

        assertSame(HARDPOINT_KINETIC_TO_HE, first(weapon(false), mount(WeaponType.BALLISTIC, WeaponSize.LARGE, true), DamageType.KINETIC, owned));
        assertNull(first(weapon(false), mount(WeaponType.BALLISTIC, WeaponSize.LARGE, false), DamageType.KINETIC, owned));
        assertNull(first(weapon(false), mount(WeaponType.HYBRID, WeaponSize.LARGE, true), DamageType.KINETIC, owned));
        assertNull(first(weapon(false), mount(WeaponType.BALLISTIC, WeaponSize.LARGE, true), DamageType.HIGH_EXPLOSIVE, owned));
        assertSame(TURRET_BEAM_TO_HE, first(weapon(true), mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), DamageType.ENERGY, owned));
        assertNull(first(weapon(false), mount(WeaponType.ENERGY, WeaponSize.MEDIUM, false), DamageType.ENERGY, owned));
        assertSame(SMALL_ENERGY_TO_EMP, first(weapon(false), mount(WeaponType.ENERGY, WeaponSize.SMALL, true), DamageType.FRAGMENTATION, owned));
        assertNull(first(weapon(false), null, DamageType.KINETIC, owned));
    }

    @Test
    void onlyOwnedConversionsApplyAndTheFirstOneMatchingAMountWins() {
        WeaponSlotAPI smallEnergyTurret = mount(WeaponType.ENERGY, WeaponSize.SMALL, false);

        assertSame(TURRET_BEAM_TO_HE, first(weapon(true), smallEnergyTurret, DamageType.ENERGY, Set.of(TURRET_BEAM_TO_HE, SMALL_ENERGY_TO_EMP)));
        assertSame(SMALL_ENERGY_TO_EMP, first(weapon(true), smallEnergyTurret, DamageType.ENERGY, Set.of(SMALL_ENERGY_TO_EMP)));
        assertNull(first(weapon(true), smallEnergyTurret, DamageType.ENERGY, Set.of()));
    }
}
