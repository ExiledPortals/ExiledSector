package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.motes.MoteKind;
import exiledsector.motes.Motes;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StardustMoteListenerTest {

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

    @Test
    void corrosionBeamLaunchesAMoteForEveryTwoSecondsOfArmorOrHullContact() {
        ShipAPI ship = ship("corrosion", 0, Map.of(StardustMoteListener.MOTE_DAMAGE_KEY, 200f));
        ShipAPI enemy = ship("enemy", 1, Map.of());
        StardustMoteListener corrosion = new StardustMoteListener(ship, new Random(1));
        BeamAPI beam = mock(BeamAPI.class);
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getLocation()).thenReturn(new Vector2f());
        when(beam.getWeapon()).thenReturn(weapon);
        DamageAPI tick = mock(DamageAPI.class);
        when(tick.isDps()).thenReturn(true);
        when(tick.getDpsDuration()).thenReturn(0.1f);
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        MissileAPI mote = mock(MissileAPI.class, RETURNS_DEEP_STUBS);
        when(mote.getActiveLayers()).thenReturn(EnumSet.noneOf(CombatEngineLayers.class));
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(mote);
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getCombatEngine).thenReturn(engine);

            for (int ticks = 0; ticks < 19; ticks++) {
                corrosion.modifyDamageDealt(beam, enemy, tick, new Vector2f(), false);
            }
            verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());

            for (int ticks = 0; ticks < 2; ticks++) {
                corrosion.modifyDamageDealt(beam, enemy, tick, new Vector2f(), false);
            }
            verify(engine).spawnProjectile(eq(ship), eq(null), eq(MoteKind.STARDUST.launcherWeaponId()), any(), anyFloat(), any());
            verify(mote.getDamage()).setDamage(200f);
            verify(mote.getDamage()).setFluxComponent(200f);

            float contactBefore = corrosion.contactSeconds(weapon, 0);
            corrosion.modifyDamageDealt(beam, ship("ally", 0, Map.of()), tick, new Vector2f(), false);
            corrosion.modifyDamageDealt(new Object(), enemy, tick, new Vector2f(), false);
            corrosion.modifyDamageDealt(beam, enemy, tick, new Vector2f(), true);
            assertEquals(contactBefore, corrosion.contactSeconds(weapon, 0), 0.0001f);
        }
        assertTrue(corrosion.contactSeconds(weapon, 0) < StardustMoteListener.CONTACT_SECONDS_PER_MOTE);
    }

    @Test
    void extraMotesInFlightRaiseTheCapOnTopOfTheBase() {
        ShipAPI normal = ship("normal", 0, Map.of(StardustMoteListener.MOTE_DAMAGE_KEY, 200f));
        ShipAPI integrated = ship("integrated", 0, Map.of(StardustMoteListener.MOTE_DAMAGE_KEY, 200f,
                StardustMoteListener.EXTRA_MOTES_IN_FLIGHT_KEY, 1f));

        assertEquals(Motes.MAX_LIVE_MOTES_PER_SOURCE, new StardustMoteListener(normal).maxMotesInFlight());
        assertEquals(Motes.MAX_LIVE_MOTES_PER_SOURCE + 1, new StardustMoteListener(integrated).maxMotesInFlight());
    }

    @Test
    void corrosionCountsEachBeamOfAMultiBeamWeaponSeparately() {
        ShipAPI ship = ship("corrosion", 0, Map.of(StardustMoteListener.MOTE_DAMAGE_KEY, 200f));
        ShipAPI enemy = ship("enemy", 1, Map.of());
        StardustMoteListener corrosion = new StardustMoteListener(ship, new Random(1));
        WeaponAPI weapon = mock(WeaponAPI.class);
        BeamAPI left = mock(BeamAPI.class);
        BeamAPI right = mock(BeamAPI.class);
        when(left.getWeapon()).thenReturn(weapon);
        when(right.getWeapon()).thenReturn(weapon);
        when(weapon.getBeams()).thenReturn(List.of(left, right));
        DamageAPI tick = mock(DamageAPI.class);
        when(tick.isDps()).thenReturn(true);
        when(tick.getDpsDuration()).thenReturn(0.1f);

        for (int ticks = 0; ticks < 15; ticks++) {
            corrosion.modifyDamageDealt(left, enemy, tick, new Vector2f(), false);
            corrosion.modifyDamageDealt(right, enemy, tick, new Vector2f(), false);
        }

        assertEquals(1.5f, corrosion.contactSeconds(weapon, 0), 0.0001f);
        assertEquals(1.5f, corrosion.contactSeconds(weapon, 1), 0.0001f);
    }
}
