package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemnantNotableEffectsTest {

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

    @Test
    void autofireAimAccuracyAddsTheMagnitudeAsAFraction() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat accuracy = mock(MutableStat.class);
        when(stats.getAutofireAimAccuracy()).thenReturn(accuracy);

        SkillEffect.byName("WEAPON_AUTOFIRE_ACCURACY_PERCENT").apply(stats, "mod_id", 50f);

        verify(accuracy).modifyFlat("mod_id", 0.5f);
    }

    @Test
    void missileGuidanceDescribesTheImprovedAlgorithmInsteadOfARawNumber() {
        assertEquals("Significantly improved missile guidance algorithm.",
                SkillEffect.byName("MISSILE_WEAPON_GUIDANCE_FLAT").description(1f).plain());
    }

    @Test
    void weaponTurnRateDescriptionsCoverAllWeapons() {
        assertEquals("50% increased weapon turn rate.", SkillEffect.byName("WEAPON_TURN_RATE_PERCENT").description(50f).plain());
        assertEquals("25% less weapon turn rate.", SkillEffect.byName("WEAPON_TURN_RATE_MULT").description(-25f).plain());
    }

    @Test
    void maxCombatReadinessAddsTheMagnitudeAsAFlatFraction() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat maxCr = mock(MutableStat.class);
        when(stats.getMaxCombatReadiness()).thenReturn(maxCr);

        LogisticsSkillEffect.MAX_COMBAT_READINESS_PERCENT.apply(stats, "mod_id", 5f);

        verify(maxCr).modifyFlat("mod_id", 0.05f, "Ship skill tree");
        assertEquals("5% increased maximum combat readiness.",
                LogisticsSkillEffect.MAX_COMBAT_READINESS_PERCENT.description(5f).plain());
    }

    @Test
    void nanoforgeMendsHullOnlyAfterGoingUndamagedForTheDelay() {
        ShipAPI ship = shipWithDynamicValue(NanoforgeMendingListener.REGEN_PERCENT_KEY, 1f);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenReturn(500f);
        NanoforgeMendingListener listener = new NanoforgeMendingListener(ship);

        listener.advance(4f);
        verify(ship, never()).setHitpoints(anyFloat());

        listener.advance(1f);
        verify(ship).setHitpoints(510f);
    }

    @Test
    void nanoforgeRestartsTheDelayWhenHullDamageIsTaken() {
        ShipAPI ship = shipWithDynamicValue(NanoforgeMendingListener.REGEN_PERCENT_KEY, 1f);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenReturn(500f);
        NanoforgeMendingListener listener = new NanoforgeMendingListener(ship);
        listener.advance(4f);

        when(ship.getHitpoints()).thenReturn(400f);
        listener.advance(1f);
        listener.advance(3f);

        verify(ship, never()).setHitpoints(anyFloat());
    }

    @Test
    void nanoforgeNeverMendsPastMaximumHull() {
        ShipAPI ship = shipWithDynamicValue(NanoforgeMendingListener.REGEN_PERCENT_KEY, 10f);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenReturn(990f);
        NanoforgeMendingListener listener = new NanoforgeMendingListener(ship);

        listener.advance(6f);

        verify(ship).setHitpoints(1000f);
    }

    @Test
    void nanoforgeRepairsAtMostTheShipsMaximumHullInTotalPerDeployment() {
        ShipAPI ship = shipWithDynamicValue(NanoforgeMendingListener.REGEN_PERCENT_KEY, 100f);
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenReturn(100f);
        NanoforgeMendingListener listener = new NanoforgeMendingListener(ship);
        listener.advance(5f);
        verify(ship).setHitpoints(1000f);

        when(ship.getHitpoints()).thenReturn(200f);
        listener.advance(1f);
        listener.advance(5f);
        verify(ship).setHitpoints(300f);

        when(ship.getHitpoints()).thenReturn(300f);
        listener.advance(10f);
        verify(ship, times(2)).setHitpoints(anyFloat());
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

    private static ShipAPI enemyAt(Vector2f location, MutableStat accuracy) {
        ShipAPI enemy = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(enemy.getMutableStats()).thenReturn(stats);
        when(stats.getAutofireAimAccuracy()).thenReturn(accuracy);
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.getLocation()).thenReturn(location);
        return enemy;
    }

    @Test
    void terrifyingPresenceLowersNearbyEnemyAccuracyAndRestoresItWhenTheyLeave() {
        ShipAPI ship = shipWithDynamicValue(TerrifyingPresenceListener.ACCURACY_PENALTY_PERCENT_KEY, 25f);
        when(ship.getId()).thenReturn("source");
        when(ship.getLocation()).thenReturn(new Vector2f());
        Vector2f enemyLocation = new Vector2f(500f, 0f);
        MutableStat accuracy = mock(MutableStat.class);
        ShipAPI enemy = enemyAt(enemyLocation, accuracy);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        List<ShipAPI> ships = new ArrayList<>(List.of(ship, enemy));
        when(engine.getShips()).thenReturn(ships);

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            TerrifyingPresenceListener listener = new TerrifyingPresenceListener(ship);

            listener.advance(0.3f);
            verify(accuracy).modifyFlat("exiledSector_terrifyingPresence_source", -0.25f);

            enemyLocation.set(TerrifyingPresenceListener.RANGE + 100f, 0f);
            listener.advance(0.3f);
            verify(accuracy).unmodify("exiledSector_terrifyingPresence_source");
        }
    }

    @Test
    void terrifyingPresenceLiftsThePenaltyWhenTheSourceDies() {
        ShipAPI ship = shipWithDynamicValue(TerrifyingPresenceListener.ACCURACY_PENALTY_PERCENT_KEY, 25f);
        when(ship.getId()).thenReturn("source");
        when(ship.getLocation()).thenReturn(new Vector2f());
        MutableStat accuracy = mock(MutableStat.class);
        ShipAPI enemy = enemyAt(new Vector2f(100f, 0f), accuracy);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(List.of(ship, enemy));

        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);
            TerrifyingPresenceListener listener = new TerrifyingPresenceListener(ship);
            listener.advance(0.3f);

            when(ship.isAlive()).thenReturn(false);
            listener.advance(0.3f);

            verify(accuracy).unmodify("exiledSector_terrifyingPresence_source");
        }
    }
}
