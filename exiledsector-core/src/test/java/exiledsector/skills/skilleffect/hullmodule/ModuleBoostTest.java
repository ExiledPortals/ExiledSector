package exiledsector.skills.skilleffect.hullmodule;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShieldSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.floatThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleBoostTest {

    private static final String PREFIX = "exiledLib_moduleShare_parent_";

    private static ShipAPI parent(Map<String, Float> moduleBonuses) {
        ShipAPI parent = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(parent.getId()).thenReturn("parent");
        when(parent.getMutableStats()).thenReturn(stats);
        moduleBonuses.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        return parent;
    }

    private static ShipAPI plate(ArmorGridAPI grid, ShieldAPI shield) {
        ShipAPI plate = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(plate.getMutableStats()).thenReturn(stats);
        when(stats.getHullBonus()).thenReturn(new StatBonus());
        when(stats.getArmorBonus()).thenReturn(new StatBonus());
        when(stats.getShieldArcBonus()).thenReturn(new StatBonus());
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHitpoints()).thenReturn(1000f);
        when(hullSpec.getArmorRating()).thenReturn(400f);
        ShieldSpecAPI shieldSpec = mock(ShieldSpecAPI.class);
        when(shieldSpec.getArc()).thenReturn(90f);
        when(hullSpec.getShieldSpec()).thenReturn(shieldSpec);
        when(plate.getHullSpec()).thenReturn(hullSpec);
        when(plate.getMaxHitpoints()).thenReturn(1000f);
        when(plate.getHitpoints()).thenReturn(1000f);
        when(plate.getArmorGrid()).thenReturn(grid);
        when(plate.getShield()).thenReturn(shield);
        return plate;
    }

    private static ArmorGridAPI grid(float... cells) {
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getGrid()).thenReturn(new float[cells.length][1]);
        when(grid.getMaxArmorInCell()).thenReturn(400f);
        for (int x = 0; x < cells.length; x++) {
            when(grid.getArmorValue(x, 0)).thenReturn(cells[x]);
        }
        return grid;
    }

    private static Map<SkillEffect, Float> shared(Object... nameMagnitudePairs) {
        Map<SkillEffect, Float> totals = new LinkedHashMap<>();
        for (int i = 0; i < nameMagnitudePairs.length; i += 2) {
            totals.put(SkillEffect.byName((String) nameMagnitudePairs[i]), (Float) nameMagnitudePairs[i + 1]);
        }
        return totals;
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) < 0.01f;
    }

    @Test
    void theShareAndTheTaniaModuleNodesRescaleHullAndArmorOnce() {
        ArmorGridAPI grid = grid(400f, 200f);
        ShipAPI plate = plate(grid, null);
        ShipAPI parent = parent(Map.of(ModuleBoost.HULL_PERCENT_KEY, 20f, ModuleBoost.ARMOR_PERCENT_KEY, 10f));

        ModuleBoost.boost(parent, plate, shared("HULL_PERCENT", 10f, "ARMOR_PERCENT", 10f), PREFIX);

        verify(plate).setMaxHitpoints(floatThat(hull -> near(hull, 1300f)));
        verify(plate).setHitpoints(floatThat(hull -> near(hull, 1300f)));
        verify(grid).setArmorValue(eq(0), eq(0), floatThat(armor -> near(armor, 480f)));
        verify(grid).setArmorValue(eq(1), eq(0), floatThat(armor -> near(armor, 240f)));
        verify(plate).syncWithArmorGridState();
    }

    @Test
    void armorAlreadyRaisedByAnEarlierEngagementIsNotRaisedAgain() {
        ArmorGridAPI grid = grid(440f);
        ShipAPI plate = plate(grid, null);

        ModuleBoost.boost(parent(Map.of()), plate, shared("ARMOR_PERCENT", 10f), PREFIX);

        verify(grid).setArmorValue(eq(0), eq(0), floatThat(armor -> near(armor, 440f)));
    }

    @Test
    void shieldArcIsRescaledOnlyOnModulesWithAShield() {
        ShieldAPI shield = mock(ShieldAPI.class);
        when(shield.getArc()).thenReturn(90f);
        ShipAPI shielded = plate(grid(400f), shield);
        ShipAPI shieldless = plate(grid(400f), null);

        ModuleBoost.boost(parent(Map.of()), shielded, shared("SHIELD_ARC_PERCENT", 20f), PREFIX);
        ModuleBoost.boost(parent(Map.of()), shieldless, shared("SHIELD_ARC_PERCENT", 20f), PREFIX);

        verify(shield).setArc(floatThat(arc -> near(arc, 108f)));
        verify(shieldless, never()).setHitpoints(anyFloat());
    }

    @Test
    void shieldArcNeverGoesPast360Degrees() {
        ShieldAPI shield = mock(ShieldAPI.class);
        when(shield.getArc()).thenReturn(340f);
        ShipAPI plate = plate(grid(400f), shield);

        ModuleBoost.boost(parent(Map.of()), plate, shared("SHIELD_ARC_PERCENT", 20f), PREFIX);

        verify(shield).setArc(360f);
    }

    @Test
    void sharedAmmoRaisesMaxAmmoOfMatchingWeaponsOnly() {
        ShipAPI plate = plate(grid(400f), null);
        when(plate.getMutableStats().getBallisticAmmoBonus()).thenReturn(new StatBonus());
        when(plate.getMutableStats().getMissileAmmoBonus()).thenReturn(new StatBonus());
        WeaponAPI ballistic = ammoWeapon(WeaponAPI.WeaponType.BALLISTIC, 20, 20);
        WeaponAPI missile = ammoWeapon(WeaponAPI.WeaponType.MISSILE, 4, 6);
        when(plate.getAllWeapons()).thenReturn(List.of(ballistic, missile));

        ModuleBoost.boost(parent(Map.of()), plate, shared("BALLISTIC_WEAPON_AMMO_PERCENT", 25f), PREFIX);

        verify(ballistic).setMaxAmmo(25);
        verify(ballistic).resetAmmo();
        verify(missile, never()).setMaxAmmo(org.mockito.ArgumentMatchers.anyInt());
    }

    private static WeaponAPI ammoWeapon(WeaponAPI.WeaponType type, int specMaxAmmo, int maxAmmo) {
        WeaponAPI weapon = mock(WeaponAPI.class, RETURNS_DEEP_STUBS);
        when(weapon.usesAmmo()).thenReturn(true);
        when(weapon.getType()).thenReturn(type);
        when(weapon.getSpec().getMaxAmmo()).thenReturn(specMaxAmmo);
        when(weapon.getMaxAmmo()).thenReturn(maxAmmo);
        return weapon;
    }

    @Test
    void nothingIsRescaledWhenNoHullArmorOrArcValueChanges() {
        ArmorGridAPI grid = grid(400f);
        ShipAPI plate = plate(grid, null);

        ModuleBoost.boost(parent(Map.of()), plate, shared("FLUX_CAPACITY_PERCENT", 10f), PREFIX);

        verify(plate, never()).setMaxHitpoints(anyFloat());
        verify(grid, never()).setArmorValue(eq(0), eq(0), anyFloat());
        verify(plate.getMutableStats().getFluxCapacity()).modifyPercent(PREFIX + "FLUX_CAPACITY_PERCENT", 10f);
        assertFalse(ModuleBoost.hasModuleBonus(parent(Map.of()).getMutableStats()));
    }

    @Test
    void rebuildEffectsRunTheirAfterCreationHookOnTheModule() {
        ShipAPI plate = plate(grid(400f), null);
        WeaponAPI smallGun = mock(WeaponAPI.class);
        when(smallGun.getSize()).thenReturn(WeaponAPI.WeaponSize.SMALL);
        when(smallGun.getType()).thenReturn(WeaponAPI.WeaponType.ENERGY);
        WeaponAPI largeGun = mock(WeaponAPI.class);
        when(largeGun.getSize()).thenReturn(WeaponAPI.WeaponSize.LARGE);
        when(plate.getAllWeapons()).thenReturn(List.of(smallGun, largeGun));

        ModuleBoost.boost(parent(Map.of()), plate, shared("PD_RECLASSIFY_SMALL_WEAPONS", 0f), PREFIX);

        verify(smallGun).setPD(true);
        verify(largeGun, never()).setPD(true);
        verify(plate.getMutableStats().getFluxCapacity(), never()).modifyPercent(anyString(), anyFloat());
    }
}
