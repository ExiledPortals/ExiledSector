package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FurorListenerTest {

    private static ShipAPI ship(String id, int owner, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f());
        return ship;
    }

    private static DamageAPI damage(float amount, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(amount);
        when(damage.getFluxComponent()).thenReturn(emp);
        when(damage.computeFluxDealt(0f)).thenReturn(emp);
        when(damage.getMultiplier()).thenReturn(1f);
        return damage;
    }

    @Test
    void furorCountsShieldHitsAtHalfWithoutEmpAndBeamTicksBySeconds() {
        DamageAPI hullHit = damage(100f, 40f);
        assertEquals(110f, FurorListener.countedDamage(hullHit, false), 0.001f);
        assertEquals(50f, FurorListener.countedDamage(hullHit, true), 0.001f);

        DamageAPI beamTick = damage(500f, 0f);
        when(beamTick.isDps()).thenReturn(true);
        when(beamTick.getDpsDuration()).thenReturn(0.1f);
        assertEquals(50f, FurorListener.countedDamage(beamTick, false), 0.001f);
        when(beamTick.getMultiplier()).thenReturn(0.5f);
        when(beamTick.getDamage()).thenReturn(250f);
        assertEquals(25f, FurorListener.countedDamage(beamTick, false), 0.001f, "a half-bright beam counts its multiplier once, not squared");
    }

    @Test
    void furorCountsEmpAfterDamageModifiersAndAcrossTheBeamTick() {
        DamageAPI beamTick = damage(500f, 0f);
        when(beamTick.isDps()).thenReturn(true);
        when(beamTick.getDpsDuration()).thenReturn(0.1f);
        when(beamTick.computeFluxDealt(0f)).thenReturn(40f);

        assertEquals(50f + 40f * FurorListener.EMP_WEIGHT, FurorListener.countedDamage(beamTick, false), 0.001f);
    }

    @Test
    void furorBuildsToItsCapFromDamageAndFadesOverTime() {
        ShipAPI ship = ship("furor", 0, Map.of(FurorListener.RATE_OF_FIRE_PERCENT_KEY, 15f));
        when(ship.getHullSize()).thenReturn(HullSize.DESTROYER);
        FurorListener furor = new FurorListener(ship);
        ShipAPI enemy = ship("enemy", 1, Map.of());
        DamageAPI hit = damage(2000f, 0f);
        ShipAPI enemyFighter = ship("fighter", 1, Map.of());
        when(enemyFighter.isFighter()).thenReturn(true);
        furor.modifyDamageDealt(null, enemyFighter, hit, new Vector2f(), false);
        assertEquals(0f, furor.furorPercent(), 0.001f);

        furor.modifyDamageDealt(null, enemy, hit, new Vector2f(), false);
        assertEquals(7.5f, furor.furorPercent(), 0.001f);
        furor.modifyDamageDealt(null, enemy, hit, new Vector2f(), false);
        furor.modifyDamageDealt(null, enemy, hit, new Vector2f(), false);
        assertEquals(15f, furor.furorPercent(), 0.001f);
        verify(ship.getMutableStats().getEnergyRoFMult()).modifyPercent("exiledLib_furor_furor", 15f);

        furor.advance(5f);
        assertEquals(10f, furor.furorPercent(), 0.001f);

        furor.modifyDamageDealt(null, ship("ally", 0, Map.of()), hit, new Vector2f(), false);
        assertEquals(10f, furor.furorPercent(), 0.001f);
    }

    @Test
    void furorKeepsItsPerHullSizeValuesAndFallbacks() {
        assertEquals(2000f, FurorListener.DAMAGE_FOR_FULL_FUROR.of(HullSize.FIGHTER));
        assertEquals(2000f, FurorListener.DAMAGE_FOR_FULL_FUROR.of(HullSize.FRIGATE));
        assertEquals(4000f, FurorListener.DAMAGE_FOR_FULL_FUROR.of(HullSize.DESTROYER));
        assertEquals(8000f, FurorListener.DAMAGE_FOR_FULL_FUROR.of(HullSize.CRUISER));
        assertEquals(20000f, FurorListener.DAMAGE_FOR_FULL_FUROR.of(HullSize.CAPITAL_SHIP));
        assertEquals(20000f, FurorListener.DAMAGE_FOR_FULL_FUROR.of(HullSize.DEFAULT));
    }
}
