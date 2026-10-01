package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LostSectorListenersTest {

    private final List<Object> gridShips = new ArrayList<>();
    private MockedStatic<Global> globalMock;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(String id, float x, Map<String, Float> dynamicValues) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        dynamicValues.forEach((key, value) -> when(dynamic.getValue(key, 0f)).thenReturn(value));
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    @Test
    void inertialDamageFollowsSpeedAndOnlyUpdatesWhenTheWholePercentChanges() {
        ShipAPI ship = ship("a", 0f, Map.of(InertialSuperchargerListener.DAMAGE_PERCENT_PER_SPEED_KEY, 0.08f));
        MutableShipStatsAPI stats = ship.getMutableStats();
        MutableStat ballistic = mock(MutableStat.class);
        MutableStat energy = mock(MutableStat.class);
        MutableStat beam = mock(MutableStat.class);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballistic);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energy);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beam);
        InertialSuperchargerListener listener = new InertialSuperchargerListener(ship);

        ship.getVelocity().set(150f, 0f);
        listener.advance(0.016f);
        ship.getVelocity().set(151f, 0f);
        listener.advance(0.016f);

        verify(ballistic, times(1)).modifyPercent("exiledSector_inertialSupercharger_a", 12f);
        verify(energy, times(1)).modifyPercent("exiledSector_inertialSupercharger_a", 12f);
        verify(beam, times(1)).modifyPercent("exiledSector_inertialSupercharger_a", -12f);

        ship.getVelocity().set(0f, 0f);
        listener.advance(0.016f);

        verify(ballistic).unmodify("exiledSector_inertialSupercharger_a");
        verify(beam).unmodify("exiledSector_inertialSupercharger_a");
    }

    private static WeaponAPI weaponIn(WeaponAPI.WeaponType slotType, WeaponAPI.WeaponSize slotSize) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSlotAPI slot = mock(WeaponSlotAPI.class);
        when(slot.getWeaponType()).thenReturn(slotType);
        when(slot.getSlotSize()).thenReturn(slotSize);
        when(weapon.getSlot()).thenReturn(slot);
        return weapon;
    }

    @Test
    void onlyWeaponsInMediumEnergySlotsGainTheFlatRange() {
        ShipAPI ship = ship("a", 0f, Map.of(MediumEnergySlotRangeListener.RANGE_FLAT_KEY, 100f));
        MediumEnergySlotRangeListener listener = new MediumEnergySlotRangeListener();

        assertEquals(100f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.MEDIUM)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponSize.SMALL)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, weaponIn(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponSize.MEDIUM)));
        assertEquals(0f, listener.getWeaponBaseRangeFlatMod(ship, mock(WeaponAPI.class)));
    }

    private static ApplyDamageResultAPI shieldHit(float flux) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getDamageToShields()).thenReturn(flux);
        return result;
    }

    @Test
    void shieldFluxChargesTheReserveThatDrivesRateOfFireAndFluxCost() {
        ShipAPI ship = ship("a", 0f, Map.of(AbsorbReserveListener.RATE_OF_FIRE_PERCENT_KEY, 50f));
        MutableShipStatsAPI stats = ship.getMutableStats();
        MutableStat ballisticRof = mock(MutableStat.class);
        MutableStat energyRof = mock(MutableStat.class);
        StatBonus ballisticFlux = mock(StatBonus.class);
        StatBonus energyFlux = mock(StatBonus.class);
        MutableStat beamFlux = mock(MutableStat.class);
        when(stats.getBallisticRoFMult()).thenReturn(ballisticRof);
        when(stats.getEnergyRoFMult()).thenReturn(energyRof);
        when(stats.getBallisticWeaponFluxCostMod()).thenReturn(ballisticFlux);
        when(stats.getEnergyWeaponFluxCostMod()).thenReturn(energyFlux);
        when(stats.getBeamWeaponFluxCostMult()).thenReturn(beamFlux);
        AbsorbReserveListener listener = new AbsorbReserveListener(ship);

        listener.reportDamageApplied(null, mock(ShipAPI.class), shieldHit(400f));
        listener.reportDamageApplied(null, ship, shieldHit(250f));
        listener.advance(0.5f);

        verify(energyRof).modifyPercent("exiledSector_absorbReserve_a", 20f);
        verify(energyFlux).modifyMult("exiledSector_absorbReserve_a", 100f / 120f);
        verify(beamFlux).modifyMult("exiledSector_absorbReserve_a", 1f / (100f / 120f));

        listener.reportDamageApplied(null, ship, shieldHit(5000f));
        listener.advance(1f);

        verify(ballisticRof).modifyPercent("exiledSector_absorbReserve_a", 50f);

        listener.advance(10f);

        verify(ballisticRof).unmodify("exiledSector_absorbReserve_a");
        verify(energyFlux).unmodify("exiledSector_absorbReserve_a");
        verify(beamFlux).unmodify("exiledSector_absorbReserve_a");
    }

    private record Armored(ShipAPI ship, ArmorGridAPI grid, float[][] cells, MutableStat maxSpeed, MutableStat turnRate,
                           WeaponAPI weapon, ShipEngineAPI engine) {
    }

    private static Armored heartShip() {
        ShipAPI ship = ship("heart", 0f, Map.of(
                AccretionListener.ARMOR_RESTORE_PERCENT_KEY, 10f,
                AccretionListener.ARMOR_PERCENT_PER_STACK_KEY, 5f,
                AccretionListener.MOBILITY_PENALTY_PERCENT_PER_STACK_KEY, 3f,
                AccretionListener.RADIATION_DISABLES_KEY, 1f));
        float[][] cells = {{50f, 100f}, {0f, 100f}};
        ArmorGridAPI grid = mock(ArmorGridAPI.class);
        when(grid.getMaxArmorInCell()).thenReturn(100f);
        when(grid.getGrid()).thenReturn(cells);
        when(grid.getArmorValue(Mockito.anyInt(), Mockito.anyInt())).thenAnswer(call -> cells[(int) call.getArgument(0)][(int) call.getArgument(1)]);
        Mockito.doAnswer(call -> {
            cells[(int) call.getArgument(0)][(int) call.getArgument(1)] = call.getArgument(2);
            return null;
        }).when(grid).setArmorValue(Mockito.anyInt(), Mockito.anyInt(), anyFloat());
        when(ship.getArmorGrid()).thenReturn(grid);

        MutableShipStatsAPI stats = ship.getMutableStats();
        MutableStat maxSpeed = mock(MutableStat.class);
        MutableStat turnRate = mock(MutableStat.class);
        when(stats.getMaxSpeed()).thenReturn(maxSpeed);
        when(stats.getMaxTurnRate()).thenReturn(turnRate);

        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getLocation()).thenReturn(new Vector2f());
        when(ship.getAllWeapons()).thenReturn(List.of(weapon));
        ShipEngineAPI engine = mock(ShipEngineAPI.class);
        when(engine.getLocation()).thenReturn(new Vector2f());
        ShipEngineControllerAPI controller = mock(ShipEngineControllerAPI.class);
        when(controller.getShipEngines()).thenReturn(List.of(engine));
        when(ship.getEngineController()).thenReturn(controller);
        return new Armored(ship, grid, cells, maxSpeed, turnRate, weapon, engine);
    }

    private static ShipAPI other(float x, boolean fighter) {
        ShipAPI other = ship("other" + x, x, Map.of());
        when(other.isFighter()).thenReturn(fighter);
        return other;
    }

    private static void destroy(ShipAPI ship) {
        when(ship.isAlive()).thenReturn(false);
        when(ship.isHulk()).thenReturn(true);
    }

    @Test
    void aShipDestroyedNearbyAddsAStackRestoresArmourAndSetsOffRadiation() {
        Armored heart = heartShip();
        ShipAPI enemy = other(800f, false);
        gridShips.addAll(List.of(heart.ship(), enemy));
        AccretionListener listener = new AccretionListener(heart.ship(), new Random(1));

        listener.advance(0.3f);
        destroy(enemy);
        listener.advance(0.3f);

        assertEquals(65f, heart.cells()[0][0]);
        assertEquals(105f, heart.cells()[0][1]);
        assertEquals(15f, heart.cells()[1][0]);
        verify(heart.maxSpeed()).modifyPercent("exiledSector_accretion_heart", -3f);
        verify(heart.turnRate()).modifyPercent("exiledSector_accretion_heart", -3f);
        verify(heart.weapon()).disable();
        verify(heart.engine()).disable();
        verify(heart.ship()).syncWithArmorGridState();
    }

    @Test
    void stacksStopAtFiveButLaterWrecksStillRestoreArmour() {
        Armored heart = heartShip();
        List<ShipAPI> enemies = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            enemies.add(other(100f + i, false));
        }
        gridShips.add(heart.ship());
        gridShips.addAll(enemies);
        AccretionListener listener = new AccretionListener(heart.ship(), new Random(1));

        listener.advance(0.3f);
        enemies.forEach(LostSectorListenersTest::destroy);
        listener.advance(0.3f);

        verify(heart.maxSpeed()).modifyPercent("exiledSector_accretion_heart", -15f);
        verify(heart.maxSpeed(), never()).modifyPercent("exiledSector_accretion_heart", -18f);
        verify(heart.weapon(), times(5)).disable();
        assertEquals(125f, heart.cells()[0][1], 0.01f);
        assertEquals(125f, heart.cells()[0][0], 0.01f);
    }

    @Test
    void fightersFarWrecksAndShipsNeverSeenAliveDoNotCount() {
        Armored heart = heartShip();
        ShipAPI fighter = other(200f, true);
        ShipAPI far = other(1900f, false);
        ShipAPI alreadyDead = other(300f, false);
        destroy(alreadyDead);
        gridShips.addAll(List.of(heart.ship(), fighter, far, alreadyDead));
        AccretionListener listener = new AccretionListener(heart.ship(), new Random(1));

        listener.advance(0.3f);
        destroy(fighter);
        destroy(far);
        listener.advance(0.3f);

        assertEquals(50f, heart.cells()[0][0]);
        verify(heart.maxSpeed(), never()).modifyPercent(anyString(), anyFloat());
        verify(heart.ship(), never()).syncWithArmorGridState();
    }

    @Test
    void aShipThatRetreatsOrAStationModuleThatDiesIsNotAWreck() {
        Armored heart = heartShip();
        ShipAPI retreating = other(400f, false);
        ShipAPI module = other(500f, false);
        when(module.getParentStation()).thenReturn(mock(ShipAPI.class));
        gridShips.addAll(List.of(heart.ship(), retreating, module));
        AccretionListener listener = new AccretionListener(heart.ship(), new Random(1));

        listener.advance(0.3f);
        when(retreating.isAlive()).thenReturn(false);
        when(retreating.getHitpoints()).thenReturn(800f);
        destroy(module);
        listener.advance(0.3f);

        assertEquals(50f, heart.cells()[0][0]);
        verify(heart.maxSpeed(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void nothingIsAddedOnceTheHeartShipItselfIsDestroyed() {
        Armored heart = heartShip();
        ShipAPI enemy = other(800f, false);
        gridShips.addAll(List.of(heart.ship(), enemy));
        AccretionListener listener = new AccretionListener(heart.ship(), new Random(1));

        listener.advance(0.3f);
        destroy(heart.ship());
        destroy(enemy);
        listener.advance(0.3f);

        verify(heart.maxSpeed(), never()).modifyPercent(eq("exiledSector_accretion_heart"), anyFloat());
    }
}
