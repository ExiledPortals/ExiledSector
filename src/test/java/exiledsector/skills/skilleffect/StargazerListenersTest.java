package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import exiledsector.motes.MoteKind;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StargazerListenersTest {

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
    void terminalDescentEntersBelowAQuarterHullAndLeavesOnlyAboveFortyPercent() {
        assertFalse(TerminalDescentListener.nextDescentState(false, 0.3f));
        assertTrue(TerminalDescentListener.nextDescentState(false, 0.2f));
        assertTrue(TerminalDescentListener.nextDescentState(true, 0.35f));
        assertTrue(TerminalDescentListener.nextDescentState(true, 0.4f));
        assertFalse(TerminalDescentListener.nextDescentState(true, 0.41f));
    }

    @Test
    void terminalDescentAppliesItsPackageAndBlocksVentingWhileActive() {
        ShipAPI ship = ship("nightmare", 0, Map.of(TerminalDescentListener.HULL_DAMAGE_TAKEN_PERCENT_KEY, -25f,
                TerminalDescentListener.FLUX_DISSIPATION_PERCENT_KEY, 17f, TerminalDescentListener.SYSTEM_REGEN_PERCENT_KEY, 17f,
                TerminalDescentListener.SHIELD_DAMAGE_TAKEN_PERCENT_KEY, 10f));
        float[] hull = {1000f};
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> hull[0]);
        TerminalDescentListener descent = new TerminalDescentListener(ship);
        MutableShipStatsAPI stats = ship.getMutableStats();
        String modId = "exiledSector_terminalDescent_nightmare";

        descent.advance(0.1f);
        assertFalse(descent.inDescent());

        hull[0] = 200f;
        descent.advance(0.1f);
        descent.advance(0.1f);
        assertTrue(descent.inDescent());
        verify(stats.getHullDamageTakenMult()).modifyPercent(modId, -25f);
        verify(stats.getFluxDissipation()).modifyPercent(modId, 17f);
        verify(stats.getSystemRegenBonus()).modifyPercent(modId, 17f);
        verify(stats.getShieldDamageTakenMult()).modifyPercent(modId, 10f);
        verify(stats.getPeakCRDuration()).modifyMult(modId, 0f);
        verify(ship, times(2)).blockCommandForOneFrame(ShipCommand.VENT_FLUX);

        hull[0] = 450f;
        descent.advance(0.1f);
        assertFalse(descent.inDescent());
        verify(stats.getPeakCRDuration()).unmodify(modId);
    }

    @Test
    void terminalDescentWaitsForTheHullToDropDuringTheBattle() {
        ShipAPI ship = ship("battered", 0, Map.of(TerminalDescentListener.HULL_DAMAGE_TAKEN_PERCENT_KEY, -25f));
        float[] hull = {200f};
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> hull[0]);
        TerminalDescentListener descent = new TerminalDescentListener(ship);

        descent.advance(0.1f);
        assertFalse(descent.inDescent());
        verify(ship, never()).blockCommandForOneFrame(ShipCommand.VENT_FLUX);

        hull[0] = 300f;
        descent.advance(0.1f);
        hull[0] = 200f;
        descent.advance(0.1f);
        assertTrue(descent.inDescent());
    }

    @Test
    void terminalDescentStopsAVentAlreadyRunning() {
        ShipAPI ship = ship("venting", 0, Map.of(TerminalDescentListener.HULL_DAMAGE_TAKEN_PERCENT_KEY, -25f));
        float[] hull = {1000f};
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> hull[0]);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(flux.isVenting()).thenReturn(true);
        when(ship.getFluxTracker()).thenReturn(flux);
        TerminalDescentListener descent = new TerminalDescentListener(ship);

        descent.advance(0.1f);
        hull[0] = 100f;
        descent.advance(0.1f);

        verify(flux).stopVenting();
    }

    private static DamageAPI damage(float amount, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(amount);
        when(damage.getFluxComponent()).thenReturn(emp);
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
        assertEquals(25f, FurorListener.countedDamage(beamTick, false), 0.001f);
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
        verify(ship.getMutableStats().getEnergyRoFMult()).modifyPercent("exiledSector_furor_furor", 15f);

        furor.advance(5f);
        assertEquals(10f, furor.furorPercent(), 0.001f);

        furor.modifyDamageDealt(null, ship("ally", 0, Map.of()), hit, new Vector2f(), false);
        assertEquals(10f, furor.furorPercent(), 0.001f);
    }

    private static MalfunctionAuraListener auraSource(String id, float percent) {
        return new MalfunctionAuraListener(ship(id, 0, Map.of(MalfunctionAuraListener.MALFUNCTION_PERCENT_KEY, percent)));
    }

    @Test
    void malfunctionAuraTakesTheStrongestSourceRatherThanStacking() {
        ShipAPI enemy = ship("enemy", 1, Map.of());
        ShipAPI ally = ship("ally", 0, Map.of());
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(enemy, ally).stream().filter(filter).toList();

        Map<ShipAPI, Float> malfunctions = MalfunctionAuraField.strongestPerTarget(List.of(auraSource("a", 5f), auraSource("b", 3f), auraSource("c", 5f)),
                everyone);

        assertEquals(5f, malfunctions.get(enemy), 0.001f);
        assertFalse(malfunctions.containsKey(ally));
    }


    @Test
    void corrosionBeamLaunchesAMoteForEveryTwoSecondsOfArmorOrHullContact() {
        ShipAPI ship = ship("corrosion", 0, Map.of(CorrosionMoteListener.MOTE_DAMAGE_KEY, 200f));
        ShipAPI enemy = ship("enemy", 1, Map.of());
        CorrosionMoteListener corrosion = new CorrosionMoteListener(ship, new Random(1));
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

            float contactBefore = corrosion.contactSeconds(weapon);
            corrosion.modifyDamageDealt(beam, ship("ally", 0, Map.of()), tick, new Vector2f(), false);
            corrosion.modifyDamageDealt(new Object(), enemy, tick, new Vector2f(), false);
            corrosion.modifyDamageDealt(beam, enemy, tick, new Vector2f(), true);
            assertEquals(contactBefore, corrosion.contactSeconds(weapon), 0.0001f);
        }
        assertTrue(corrosion.contactSeconds(weapon) < CorrosionMoteListener.CONTACT_SECONDS_PER_MOTE);
    }
}
