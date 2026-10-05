package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.compat.LostSectorCompat;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
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
    private CombatEngineAPI combatEngine;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        combatEngine = engine;
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

    private void enableLostSector(boolean enabled) {
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI mods = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(mods);
        when(mods.isModEnabled("lost_sector")).thenReturn(enabled);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    private static ShipAPI augmentedInertialShip() {
        ShipAPI ship = ship("a", 0f, Map.of(InertialSuperchargerListener.DAMAGE_PERCENT_PER_SPEED_KEY, 0.08f,
                InertialSuperchargerListener.AUGMENTED_PROJECTILE_SPEED_KEY, 100f));
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod(LostSectorCompat.AUGMENTED_SYSTEMS_HULLMOD_ID)).thenReturn(true);
        when(ship.getVariant()).thenReturn(variant);
        MutableShipStatsAPI stats = ship.getMutableStats();
        when(stats.getBallisticWeaponDamageMult()).thenReturn(mock(MutableStat.class));
        when(stats.getEnergyWeaponDamageMult()).thenReturn(mock(MutableStat.class));
        when(stats.getBeamWeaponDamageMult()).thenReturn(mock(MutableStat.class));
        when(stats.getProjectileSpeedMult()).thenReturn(mock(MutableStat.class));
        return ship;
    }

    @Test
    void withLostSectorAugmentedHullsAlsoGainProjectileSpeedFromTheDamageBonus() {
        enableLostSector(true);
        ShipAPI ship = augmentedInertialShip();
        InertialSuperchargerListener listener = new InertialSuperchargerListener(ship);

        ship.getVelocity().set(150f, 0f);
        listener.advance(0.016f);
        ship.getVelocity().set(0f, 0f);
        listener.advance(0.016f);

        verify(ship.getMutableStats().getProjectileSpeedMult()).modifyPercent("exiledSector_inertialSupercharger_a", 12f);
        verify(ship.getMutableStats().getProjectileSpeedMult()).unmodify("exiledSector_inertialSupercharger_a");
    }

    @Test
    void withoutLostSectorTheAugmentedProjectileSpeedNeverApplies() {
        enableLostSector(false);
        ShipAPI ship = augmentedInertialShip();
        InertialSuperchargerListener listener = new InertialSuperchargerListener(ship);

        ship.getVelocity().set(150f, 0f);
        listener.advance(0.016f);

        verify(ship.getMutableStats().getProjectileSpeedMult(), never()).modifyPercent(anyString(), anyFloat());
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

    private static final Map<String, Float> FROZEN_HEART = Map.of(
            NearbyDestructionHeartless.RANGE_KEY, 1500f,
            HeartlessStacks.ARMOR_RESTORE_PERCENT_KEY, 10f,
            HeartlessStacks.ARMOR_PERCENT_PER_STACK_KEY, 5f,
            HeartlessStacks.MOBILITY_PENALTY_PERCENT_PER_STACK_KEY, 3f,
            HeartlessStacks.RADIATION_EMP_KEY, 400f);
    private static final String HEARTLESS_MOD_ID = "exiledSector_heartless_heart";

    private record Armored(ShipAPI ship, float[][] cells, MutableStat maxSpeed, MutableStat turnRate,
                           WeaponAPI weapon, ShipEngineAPI engine, HeartlessStacks stacks) {
    }

    private static Armored heartShip(Map<String, Float> magnitudes) {
        ShipAPI ship = ship("heart", 0f, magnitudes);
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
        when(stats.getAcceleration()).thenReturn(mock(MutableStat.class));
        when(stats.getDeceleration()).thenReturn(mock(MutableStat.class));
        when(stats.getTurnAcceleration()).thenReturn(mock(MutableStat.class));

        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getLocation()).thenReturn(new Vector2f(10f, 0f));
        when(ship.getAllWeapons()).thenReturn(List.of(weapon));
        ShipEngineAPI engine = mock(ShipEngineAPI.class);
        when(engine.getLocation()).thenReturn(new Vector2f(-20f, 0f));
        ShipEngineControllerAPI controller = mock(ShipEngineControllerAPI.class);
        when(controller.getShipEngines()).thenReturn(List.of(engine));
        when(ship.getEngineController()).thenReturn(controller);

        HeartlessStacks stacks = new HeartlessStacks(ship, new Random(1));
        when(ship.getListeners(HeartlessStacks.class)).thenReturn(List.of(stacks));
        return new Armored(ship, cells, maxSpeed, turnRate, weapon, engine, stacks);
    }

    private void verifyEmpAt(ShipAPI ship, float x, int times) {
        verify(combatEngine, times(times)).applyDamage(eq(ship), Mockito.argThat(point -> point.x == x && point.y == 0f), eq(0f),
                eq(DamageType.ENERGY), eq(400f), eq(true), eq(false), eq(ship));
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
    void gainingAStackRaisesMaxArmourRestoresArmourSlowsTheShipAndSetsOffRadiation() {
        Armored heart = heartShip(FROZEN_HEART);

        heart.stacks().gain();

        assertEquals(1, heart.stacks().stacks());
        assertEquals(65f, heart.cells()[0][0]);
        assertEquals(105f, heart.cells()[0][1]);
        assertEquals(15f, heart.cells()[1][0]);
        verify(heart.maxSpeed()).modifyPercent(HEARTLESS_MOD_ID, -3f);
        verify(heart.turnRate()).modifyPercent(HEARTLESS_MOD_ID, -3f);
        verify(heart.ship().getMutableStats().getTurnAcceleration()).modifyPercent(HEARTLESS_MOD_ID, -6f);
        verify(heart.ship().getMutableStats().getAcceleration()).modifyPercent(HEARTLESS_MOD_ID, -3f);
        verifyEmpAt(heart.ship(), 10f, 1);
        verifyEmpAt(heart.ship(), -20f, 1);
        verify(heart.ship()).syncWithArmorGridState();
    }

    @Test
    void stacksStopAtFiveAndNothingHappensForGainsBeyondThat() {
        Armored heart = heartShip(FROZEN_HEART);

        for (int i = 0; i < 7; i++) {
            heart.stacks().gain();
        }

        assertEquals(HeartlessStacks.DEFAULT_MAX_STACKS, heart.stacks().stacks());
        verify(heart.maxSpeed()).modifyPercent(HEARTLESS_MOD_ID, -15f);
        verify(heart.maxSpeed(), never()).modifyPercent(HEARTLESS_MOD_ID, -18f);
        verifyEmpAt(heart.ship(), 10f, 5);
        assertEquals(125f, heart.cells()[0][0], 0.01f);
        assertEquals(125f, heart.cells()[0][1], 0.01f);
    }

    @Test
    void eachConsequenceAppliesOnlyWhenItsOwnEffectIsPresent() {
        Armored heart = heartShip(Map.of(HeartlessStacks.ARMOR_RESTORE_PERCENT_KEY, 10f));

        heart.stacks().gain();

        assertEquals(60f, heart.cells()[0][0]);
        assertEquals(100f, heart.cells()[0][1]);
        verify(heart.maxSpeed(), never()).modifyPercent(anyString(), anyFloat());
        verify(combatEngine, never()).applyDamage(any(CombatEntityAPI.class), any(Vector2f.class), anyFloat(), any(DamageType.class),
                anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void otherEffectsCanRaiseOrLowerTheStackCap() {
        Armored raised = heartShip(Map.of(HeartlessStacks.MAX_STACKS_KEY, 2f));
        Armored lowered = heartShip(Map.of(HeartlessStacks.MAX_STACKS_KEY, -3f));
        Armored removed = heartShip(Map.of(HeartlessStacks.MAX_STACKS_KEY, -10f));

        for (int i = 0; i < 10; i++) {
            raised.stacks().gain();
            lowered.stacks().gain();
            removed.stacks().gain();
        }

        assertEquals(7, raised.stacks().stacks());
        assertEquals(2, lowered.stacks().stacks());
        assertEquals(0, removed.stacks().stacks());
    }

    @Test
    void stacksStillCountWithNoConsequences() {
        Armored heart = heartShip(Map.of());

        heart.stacks().gain();

        assertEquals(1, heart.stacks().stacks());
        assertEquals(50f, heart.cells()[0][0]);
        verify(heart.ship(), never()).syncWithArmorGridState();
    }

    @Test
    void aShipWithoutAStackCounterGetsOneTheFirstTimeItGainsAStack() {
        ShipAPI ship = ship("fresh", 0f, Map.of());
        when(ship.getListeners(HeartlessStacks.class)).thenReturn(List.of());

        HeartlessStacks stacks = HeartlessStacks.of(ship);

        verify(ship).addListener(stacks);
    }

    @Test
    void aNonFighterShipDestroyedInRangeAddsAStack() {
        Armored heart = heartShip(FROZEN_HEART);
        ShipAPI enemy = other(800f, false);
        gridShips.addAll(List.of(heart.ship(), enemy));
        NearbyDestructionHeartless source = new NearbyDestructionHeartless(heart.ship());

        source.advance(0.3f);
        destroy(enemy);
        source.advance(0.3f);

        assertEquals(1, heart.stacks().stacks());
    }

    @Test
    void fightersFarWrecksAndShipsNeverSeenAliveDoNotAddStacks() {
        Armored heart = heartShip(FROZEN_HEART);
        ShipAPI fighter = other(200f, true);
        ShipAPI far = other(1900f, false);
        ShipAPI alreadyDead = other(300f, false);
        destroy(alreadyDead);
        gridShips.addAll(List.of(heart.ship(), fighter, far, alreadyDead));
        NearbyDestructionHeartless source = new NearbyDestructionHeartless(heart.ship());

        source.advance(0.3f);
        destroy(fighter);
        destroy(far);
        source.advance(0.3f);

        assertEquals(0, heart.stacks().stacks());
    }

    @Test
    void aShipThatRetreatsOrAStationModuleThatDiesAddsNoStack() {
        Armored heart = heartShip(FROZEN_HEART);
        ShipAPI retreating = other(400f, false);
        ShipAPI module = other(500f, false);
        when(module.getParentStation()).thenReturn(mock(ShipAPI.class));
        gridShips.addAll(List.of(heart.ship(), retreating, module));
        NearbyDestructionHeartless source = new NearbyDestructionHeartless(heart.ship());

        source.advance(0.3f);
        when(retreating.isAlive()).thenReturn(false);
        when(retreating.getHitpoints()).thenReturn(800f);
        destroy(module);
        source.advance(0.3f);

        assertEquals(0, heart.stacks().stacks());
    }

    @Test
    void nothingIsAddedOnceTheShipItselfIsDestroyed() {
        Armored heart = heartShip(FROZEN_HEART);
        ShipAPI enemy = other(800f, false);
        gridShips.addAll(List.of(heart.ship(), enemy));
        NearbyDestructionHeartless source = new NearbyDestructionHeartless(heart.ship());

        source.advance(0.3f);
        destroy(heart.ship());
        destroy(enemy);
        source.advance(0.3f);
        heart.stacks().gain();

        assertEquals(0, heart.stacks().stacks());
        verify(heart.maxSpeed(), never()).modifyPercent(eq(HEARTLESS_MOD_ID), anyFloat());
    }
}
