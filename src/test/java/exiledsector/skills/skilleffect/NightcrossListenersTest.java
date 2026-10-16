package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class NightcrossListenersTest {

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
    void dynamicGridWaitsForShieldsToStayDownThenDropsTheBonusWhenTheyRise() {
        ShipAPI ship = ship("grid", 0, Map.of(DynamicGridListener.ENERGY_BONUS_PERCENT_KEY, 15f));
        ShieldAPI shield = mock(ShieldAPI.class);
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(ship.getShield()).thenReturn(shield);
        when(ship.getFluxTracker()).thenReturn(flux);
        when(ship.getHullSize()).thenReturn(HullSize.CRUISER);
        DynamicGridListener grid = new DynamicGridListener(ship);
        String modId = "exiledSector_dynamicGrid_grid";

        grid.advance(1.5f);
        assertFalse(grid.bonusActive());
        grid.advance(0.6f);
        assertTrue(grid.bonusActive());
        verify(ship.getMutableStats().getEnergyRoFMult()).modifyPercent(modId, 15f);
        verify(ship.getMutableStats().getEnergyWeaponFluxCostMod()).modifyPercent(modId, -15f);

        when(shield.isOn()).thenReturn(true);
        grid.advance(0.1f);
        assertFalse(grid.bonusActive());
        verify(ship.getMutableStats().getEnergyRoFMult()).unmodify(modId);

        when(shield.isOn()).thenReturn(false);
        when(flux.isVenting()).thenReturn(true);
        grid.advance(5f);
        assertFalse(grid.bonusActive());
    }

    @Test
    void dynamicGridNeverTurnsOnForAShipWithoutShields() {
        ShipAPI ship = ship("bare", 0, Map.of(DynamicGridListener.ENERGY_BONUS_PERCENT_KEY, 15f));
        when(ship.getFluxTracker()).thenReturn(mock(FluxTrackerAPI.class));
        when(ship.getHullSize()).thenReturn(HullSize.FRIGATE);
        DynamicGridListener grid = new DynamicGridListener(ship);

        grid.advance(10f);

        assertFalse(grid.bonusActive());
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

    @Test
    void hardlightMarksAreSharedByEveryFighterOfOneCarrierAndCapped() {
        ShipAPI carrier = ship("carrier", 0, Map.of(FighterFocusFireMarks.PERCENT_PER_HIT_KEY, 2f, FighterFocusFireMarks.MAX_PERCENT_KEY, 30f));
        ShipAPI enemy = ship("enemy", 1, Map.of());
        FighterFocusFireMarks marks = new FighterFocusFireMarks(carrier);
        FighterFocusFireMarks.FighterHits firstFighter = new FighterFocusFireMarks.FighterHits(marks);
        FighterFocusFireMarks.FighterHits secondFighter = new FighterFocusFireMarks.FighterHits(marks);
        DamageAPI damage = mock(DamageAPI.class);
        MutableStat modifier = mock(MutableStat.class);
        when(damage.getModifier()).thenReturn(modifier);

        assertNull(firstFighter.modifyDamageDealt(null, enemy, damage, new Vector2f(), false));
        assertEquals("exiledSector_fighterFocusFire_carrier", secondFighter.modifyDamageDealt(null, enemy, damage, new Vector2f(), false));
        verify(modifier).modifyPercent("exiledSector_fighterFocusFire_carrier", 2f);

        for (int hit = 0; hit < 20; hit++) {
            firstFighter.modifyDamageDealt(null, enemy, damage, new Vector2f(), false);
        }
        assertEquals(30f, marks.markPercent(enemy), 0.0001f);
        assertEquals(0f, marks.markPercent(ship("other", 1, Map.of())), 0.0001f);
    }

    @Test
    void hardlightCountsASecondOfBeamContactAsOneHit() {
        ShipAPI carrier = ship("carrier", 0, Map.of(FighterFocusFireMarks.PERCENT_PER_HIT_KEY, 2f, FighterFocusFireMarks.MAX_PERCENT_KEY, 30f));
        ShipAPI enemy = ship("enemy", 1, Map.of());
        FighterFocusFireMarks marks = new FighterFocusFireMarks(carrier);
        FighterFocusFireMarks.FighterHits beamFighter = new FighterFocusFireMarks.FighterHits(marks);
        DamageAPI beamTick = mock(DamageAPI.class);
        when(beamTick.isDps()).thenReturn(true);
        when(beamTick.getDpsDuration()).thenReturn(0.1f);
        when(beamTick.getModifier()).thenReturn(mock(MutableStat.class));

        for (int tick = 0; tick < 10; tick++) {
            beamFighter.modifyDamageDealt(null, enemy, beamTick, new Vector2f(), false);
        }

        assertEquals(2f, marks.markPercent(enemy), 0.001f);
    }

    @Test
    void gravityDragMeasuresRangeToTheTargetsEdge() {
        ShipAPI bigEnemy = enemyAt(GravityDragField.RANGE);
        when(bigEnemy.getCollisionRadius()).thenReturn(300f);
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(bigEnemy).stream().filter(filter).toList();

        float reduction = GravityDragField.reductionsFrom(List.of(dragSource("source", 5f, false)), everyone).get(bigEnemy);

        assertEquals(5f * 300f / GravityDragField.RANGE, reduction, 0.001f);
    }

    @Test
    void hardlightMarksIgnoreFriendlyShips() {
        ShipAPI carrier = ship("carrier", 0, Map.of(FighterFocusFireMarks.PERCENT_PER_HIT_KEY, 2f, FighterFocusFireMarks.MAX_PERCENT_KEY, 30f));
        FighterFocusFireMarks marks = new FighterFocusFireMarks(carrier);
        ShipAPI ally = ship("ally", 0, Map.of());

        new FighterFocusFireMarks.FighterHits(marks).modifyDamageDealt(null, ally, mock(DamageAPI.class), new Vector2f(), false);

        assertEquals(0f, marks.markPercent(ally), 0.0001f);
    }

    private static GravityDragListener dragSource(String id, float percent, boolean phased) {
        ShipAPI source = ship(id, 0, Map.of(GravityDragListener.TIMEFLOW_PERCENT_KEY, percent));
        when(source.isPhased()).thenReturn(phased);
        return new GravityDragListener(source);
    }

    private static ShipAPI enemyAt(float x) {
        ShipAPI enemy = ship("enemy", 1, Map.of());
        when(enemy.getLocation()).thenReturn(new Vector2f(x, 0f));
        return enemy;
    }

    @Test
    void gravityDragFallsWithDistanceAndDoublesWhilePhased() {
        ShipAPI near = enemyAt(0f);
        ShipAPI halfway = enemyAt(GravityDragField.RANGE / 2f);
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(near, halfway).stream().filter(filter).toList();

        Map<ShipAPI, Float> calm = GravityDragField.reductionsFrom(List.of(dragSource("calm", 5f, false)), everyone);
        Map<ShipAPI, Float> phased = GravityDragField.reductionsFrom(List.of(dragSource("phased", 5f, true)), everyone);

        assertEquals(5f, calm.get(near), 0.001f);
        assertEquals(2.5f, calm.get(halfway), 0.001f);
        assertEquals(10f, phased.get(near), 0.001f);
    }

    @Test
    void gravityDragFromManySourcesIsCappedPerShip() {
        ShipAPI near = enemyAt(0f);
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(near).stream().filter(filter).toList();
        List<GravityDragListener> sources = List.of(dragSource("a", 5f, true), dragSource("b", 5f, true), dragSource("c", 5f, true));

        assertEquals(GravityDragField.MAX_TOTAL_REDUCTION_PERCENT, GravityDragField.reductionsFrom(sources, everyone).get(near), 0.001f);
    }

    @Test
    void gravityDragLeavesAlliesFightersAndHulksAlone() {
        ShipAPI ally = ship("ally", 0, Map.of());
        ShipAPI fighter = enemyAt(0f);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI hulk = enemyAt(0f);
        when(hulk.isHulk()).thenReturn(true);
        GravityDragField.ShipsNear everyone = (source, radius, filter) -> List.of(ally, fighter, hulk).stream().filter(filter).toList();

        assertTrue(GravityDragField.reductionsFrom(List.of(dragSource("source", 5f, false)), everyone).isEmpty());
    }
}
