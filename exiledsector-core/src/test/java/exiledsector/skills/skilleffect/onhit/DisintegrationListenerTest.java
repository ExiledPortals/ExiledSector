package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.skills.skilleffect.weapon.BallisticAsEnergy;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisintegrationListenerTest {

    private static ShipAPI shipWithDynamicValue(String key, float value) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(key, 0f)).thenReturn(value);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static DamagingProjectileAPI projectileFrom(WeaponAPI.WeaponType type) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(type);
        DamagingProjectileAPI proj = mock(DamagingProjectileAPI.class);
        when(proj.getWeapon()).thenReturn(weapon);
        return proj;
    }

    private static ShipAPI armoredTarget(ArmorGridAPI grid, Vector2f point) {
        ShipAPI target = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat armorTaken = mock(MutableStat.class);
        when(armorTaken.getModifiedValue()).thenReturn(1f);
        when(stats.getArmorDamageTakenMult()).thenReturn(armorTaken);
        when(target.getMutableStats()).thenReturn(stats);
        when(target.getArmorGrid()).thenReturn(grid);
        when(grid.getGrid()).thenReturn(new float[5][5]);
        when(grid.getCellAtLocation(point)).thenReturn(new int[]{2, 2});
        when(grid.getArmorValue(anyInt(), anyInt())).thenReturn(100f);
        return target;
    }

    @Test
    void disintegrationStripsExtraArmorAroundAnEnergyHit() {
        ShipAPI ship = shipWithDynamicValue(DisintegrationListener.ARMOR_DAMAGE_PERCENT_KEY, 50f);
        Vector2f point = new Vector2f();
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        ShipAPI target = armoredTarget(grid, point);
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(300f);

        String modId = new DisintegrationListener(ship).modifyDamageDealt(
                projectileFrom(WeaponAPI.WeaponType.ENERGY), target, damage, point, false);

        assertEquals(null, modId);
        verify(grid).setArmorValue(2, 2, 90f);
        verify(grid).setArmorValue(0, 1, 95f);
        verify(grid, never()).setArmorValue(eq(0), eq(0), anyFloat());
        verify(target).syncWithArmorGridState();
    }

    @Test
    void disintegrationIgnoresShieldHitsAndNonEnergyWeapons() {
        ShipAPI ship = shipWithDynamicValue(DisintegrationListener.ARMOR_DAMAGE_PERCENT_KEY, 50f);
        Vector2f point = new Vector2f();
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        ShipAPI target = armoredTarget(grid, point);
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(300f);
        DisintegrationListener listener = new DisintegrationListener(ship);

        listener.modifyDamageDealt(projectileFrom(WeaponAPI.WeaponType.ENERGY), target, damage, point, true);
        listener.modifyDamageDealt(projectileFrom(WeaponAPI.WeaponType.BALLISTIC), target, damage, point, false);

        verify(grid, never()).setArmorValue(anyInt(), anyInt(), anyFloat());
    }

    @Test
    void disintegrationStripsArmorAroundBallisticHitsWhileBallisticCountsAsEnergy() {
        ShipAPI ship = shipWithDynamicValue(DisintegrationListener.ARMOR_DAMAGE_PERCENT_KEY, 50f);
        StatBonus flag = new StatBonus();
        flag.modifyFlat("fafnir", 1f);
        when(ship.getMutableStats().getDynamic().getMod(BallisticAsEnergy.KEY)).thenReturn(flag);
        Vector2f point = new Vector2f();
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        ShipAPI target = armoredTarget(grid, point);
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(300f);
        DamagingProjectileAPI ballisticShot = projectileFrom(WeaponAPI.WeaponType.BALLISTIC);
        when(ballisticShot.getWeapon().getShip()).thenReturn(ship);

        new DisintegrationListener(ship).modifyDamageDealt(ballisticShot, target, damage, point, false);

        verify(grid).setArmorValue(2, 2, 90f);
    }
}
