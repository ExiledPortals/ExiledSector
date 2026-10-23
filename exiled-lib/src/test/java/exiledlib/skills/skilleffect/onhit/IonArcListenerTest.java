package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IonArcListenerTest {

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

    private static ShipAPI fluxedTarget(float hardFlux, float maxFlux) {
        ShipAPI target = ship("target", 1, Map.of());
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(flux.getHardFlux()).thenReturn(hardFlux);
        when(flux.getMaxFlux()).thenReturn(maxFlux);
        when(target.getFluxTracker()).thenReturn(flux);
        return target;
    }

    @Test
    void ionArcChanceScalesWithTheTargetsHardFluxLevel() {
        assertEquals(0.075f, IonArcListener.arcChance(15f, fluxedTarget(500f, 1000f)), 0.0001f);
        assertEquals(0.15f, IonArcListener.arcChance(15f, fluxedTarget(1000f, 1000f)), 0.0001f);
        assertEquals(0f, IonArcListener.arcChance(15f, fluxedTarget(0f, 1000f)), 0.0001f);
    }

    @Test
    void ionArcFiresAShieldPiercingArcOnlyForEnergyShotsThatHitAShield() {
        ShipAPI ship = ship("ion", 0, Map.of(IonArcListener.CHANCE_AT_FULL_HARD_FLUX_KEY, 100f));
        ShipAPI target = fluxedTarget(1000f, 1000f);
        Random alwaysArcs = mock(Random.class);
        when(alwaysArcs.nextFloat()).thenReturn(0f);
        IonArcListener ion = new IonArcListener(ship, alwaysArcs);
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(WeaponAPI.WeaponType.ENERGY);
        when(shot.getWeapon()).thenReturn(weapon);
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(200f);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            assertNull(ion.modifyDamageDealt(shot, target, damage, new Vector2f(), false));
            verify(engine, never()).spawnEmpArcPierceShields(any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat(),
                    anyString(), anyFloat(), any(), any());

            ion.modifyDamageDealt(shot, target, damage, new Vector2f(), true);
            verify(engine).spawnEmpArcPierceShields(eq(ship), any(), eq(target), eq(target), eq(DamageType.ENERGY), eq(50f), eq(100f),
                    anyFloat(), anyString(), anyFloat(), any(), any());
        }
    }
}
