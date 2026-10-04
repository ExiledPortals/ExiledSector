package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefractionDroneTest {

    private static final float FRAME = 0.016f;

    private MockedStatic<Global> global;
    private MockedStatic<WeaponDroneStats> stats;
    private MockedStatic<MagicFakeBeamPlugin> fakeBeams;
    private CombatEngineAPI engine;
    private final List<DamagingProjectileAPI> projectiles = new ArrayList<>();
    private final Vector2f droneLocation = new Vector2f();
    private final StatBonus rangeBonus = new StatBonus();
    private ShipAPI droneShip;
    private ShipAPI firingShip;
    private WeaponAPI weapon;
    private ProjectileSpecAPI projectileSpec;
    private RefractionDrones pool;
    private EnergyChainListener chain;
    private RefractionDrone drone;

    @BeforeEach
    void setUp() {
        fakeBeams = Mockito.mockStatic(MagicFakeBeamPlugin.class);
        engine = mock(CombatEngineAPI.class);
        when(engine.getProjectiles()).thenReturn(projectiles);
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);
        stats = Mockito.mockStatic(WeaponDroneStats.class);

        weapon = mock(WeaponAPI.class);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        projectileSpec = mock(ProjectileSpecAPI.class);
        when(weapon.getSpec()).thenReturn(spec);
        when(spec.getWeaponId()).thenReturn("energy_gun");
        when(spec.getProjectileSpec()).thenReturn(projectileSpec);
        when(projectileSpec.getCoreColor()).thenReturn(Color.WHITE);
        when(projectileSpec.getFringeColor()).thenReturn(Color.CYAN);
        when(projectileSpec.getWidth()).thenReturn(14f);
        when(weapon.getRange()).thenAnswer(invocation -> rangeBonus.computeEffective(600f));
        when(weapon.getProjectileSpeed()).thenReturn(1000f);
        when(weapon.getCooldown()).thenReturn(WeaponDroneFactory.SINGLE_SHOT_REFIRE_DELAY);

        droneShip = mock(ShipAPI.class);
        MutableShipStatsAPI droneStats = mock(MutableShipStatsAPI.class);
        when(droneShip.getMutableStats()).thenReturn(droneStats);
        when(droneStats.getEnergyWeaponRangeBonus()).thenReturn(rangeBonus);
        when(droneShip.getAllWeapons()).thenReturn(List.of(weapon));
        when(droneShip.getLocation()).thenReturn(droneLocation);
        when(droneShip.getVelocity()).thenReturn(new Vector2f(30f, 40f));
        when(droneShip.getMouseTarget()).thenReturn(new Vector2f());
        when(engine.isEntityInPlay(droneShip)).thenReturn(true);

        ShipVariantAPI droneVariant = mock(ShipVariantAPI.class);
        when(droneVariant.getHullMods()).thenReturn(new java.util.LinkedHashSet<>());
        when(droneShip.getVariant()).thenReturn(droneVariant);
        firingShip = mock(ShipAPI.class);
        when(firingShip.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class));
        when(firingShip.getVariant()).thenReturn(mock(ShipVariantAPI.class));
        pool = mock(RefractionDrones.class);
        when(pool.firingShip()).thenReturn(firingShip);
        chain = mock(EnergyChainListener.class);
        drone = new RefractionDrone(droneShip, pool, chain);
    }

    @AfterEach
    void tearDown() {
        stats.close();
        global.close();
        fakeBeams.close();
    }

    private static ShipAPI shieldedShipAtOrigin(float shieldRadius) {
        ShipAPI ship = mock(ShipAPI.class);
        ShieldAPI shield = mock(ShieldAPI.class);
        when(ship.getShieldCenterEvenIfNoShield()).thenReturn(new Vector2f(0f, 0f));
        when(ship.getShield()).thenReturn(shield);
        when(shield.isOn()).thenReturn(true);
        when(shield.getRadius()).thenReturn(shieldRadius);
        return ship;
    }

    private static ShipAPI target(Vector2f location, Vector2f velocity) {
        ShipAPI target = mock(ShipAPI.class);
        when(target.getLocation()).thenReturn(location);
        when(target.getVelocity()).thenReturn(velocity);
        return target;
    }

    private DamagingProjectileAPI projectileFrom(WeaponAPI firedBy) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        Map<String, Object> data = new HashMap<>();
        when(projectile.getWeapon()).thenReturn(firedBy);
        when(projectile.getCustomData()).thenReturn(data);
        doAnswer(invocation -> data.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(projectile).setCustomData(any(), any());
        return projectile;
    }

    private ChainLink link() {
        return new ChainLink(List.of(mock(ShipAPI.class)), 1, 0.8f);
    }

    private void launchBehindTheHitShip() {
        drone.launch(shieldedShipAtOrigin(100f), new Vector2f(100f, 0f), target(new Vector2f(-500f, 0f), new Vector2f()),
                link(), 600f);
    }

    @Test
    void launchPlacesTheDroneBeyondTheFarSideOfTheShieldStoppedAndAimedAtTheTarget() {
        launchBehindTheHitShip();

        assertEquals(-120f, droneLocation.x, 0.01f);
        assertEquals(0f, droneLocation.y, 0.01f);
        assertEquals(new Vector2f(), droneShip.getVelocity());
        verify(droneShip).setFacing(180f);
        verify(weapon).setFacing(180f);
        verify(weapon).setForceFireOneFrame(true);
        stats.verify(() -> WeaponDroneStats.mirror(any(), any()));
        assertFalse(drone.isReady());
    }

    @Test
    void launchLeadsAMovingTarget() {
        drone.launch(shieldedShipAtOrigin(100f), new Vector2f(100f, 0f), target(new Vector2f(500f, 0f), new Vector2f(0f, 200f)),
                link(), 600f);

        Vector2f aim = droneShip.getMouseTarget();
        assertEquals(500f, aim.x, 0.01f);
        assertTrue(aim.y > 0f);
    }

    @Test
    void launchDrawsAStreakInTheProjectilesColoursFromTheImpactPointToTheDrone() {
        launchBehindTheHitShip();

        fakeBeams.verify(() -> MagicFakeBeamPlugin.addBeam(eq(0.05f), eq(0.15f), eq(14f), eq(new Vector2f(100f, 0f)),
                eq(180f), eq(220f), eq(Color.WHITE), eq(Color.CYAN)));
    }

    @Test
    void launchStretchesTheDronesRangeStatSoTheShotTravelsAsFarAsTheFiringWeapons() {
        when(weapon.getRange()).thenAnswer(invocation -> rangeBonus.computeEffective(600f) * 1.5f + 30f);

        drone.launch(shieldedShipAtOrigin(100f), new Vector2f(100f, 0f), target(new Vector2f(-500f, 0f), new Vector2f()),
                link(), 1230f);

        assertEquals(1230f, weapon.getRange(), 0.01f);
        assertEquals(200f, rangeBonus.getFlatBonus(), 0.01f);
    }

    @Test
    void aDroneThatAlreadyReachesTheRangeGetsNoExtraBonus() {
        rangeBonus.modifyFlat(RefractionDrone.RANGE_MATCH_MOD_ID, 50f);

        launchBehindTheHitShip();

        assertEquals(0f, rangeBonus.getFlatBonus(), 0.01f);
    }

    @Test
    void itTagsOnlyItsOwnUntaggedShotsHandsThemBackToTheFiringShipAndRearms() {
        DamagingProjectileAPI own = projectileFrom(weapon);
        DamagingProjectileAPI other = projectileFrom(mock(WeaponAPI.class));
        DamagingProjectileAPI alreadyTagged = projectileFrom(weapon);
        new ChainLink(List.of(), 3, 0.5f).tag(alreadyTagged);
        launchBehindTheHitShip();
        projectiles.addAll(List.of(own, other, alreadyTagged));
        when(weapon.usesAmmo()).thenReturn(true);
        when(weapon.getChargeLevel()).thenReturn(1f);

        drone.advance(FRAME);

        assertEquals(1, own.getCustomData().get(ChainLink.COUNT_KEY));
        assertEquals(0.8f, own.getCustomData().get(ChainLink.DEALT_MULT_KEY));
        verify(own).setSource(firingShip);
        assertFalse(ChainLink.isTagged(other));
        verify(other, never()).setSource(any());
        assertEquals(3, alreadyTagged.getCustomData().get(ChainLink.COUNT_KEY));
        assertTrue(drone.isReady());
        verify(weapon).setRemainingCooldownTo(0f);
        verify(weapon).resetAmmo();
    }

    @Test
    void aStrayShotSeenBeforeTheWeaponFiresDoesNotEndTheLaunch() {
        launchBehindTheHitShip();
        projectiles.add(projectileFrom(weapon));

        drone.advance(FRAME);

        assertFalse(drone.isReady());
        verify(weapon, times(2)).setForceFireOneFrame(true);
    }

    @Test
    void itKeepsPullingTheTriggerUntilTheWeaponFires() {
        launchBehindTheHitShip();

        drone.advance(FRAME);
        drone.advance(FRAME);
        when(weapon.getChargeLevel()).thenReturn(1f);
        drone.advance(FRAME);

        verify(weapon, times(3)).setForceFireOneFrame(true);
    }

    @Test
    void aWeaponThatNeverFiresGivesUpAfterTheTimeout() {
        launchBehindTheHitShip();

        drone.advance(RefractionDrone.FIRE_TIMEOUT_SECONDS + FRAME);

        assertTrue(drone.isReady());
    }

    @Test
    void aShotThatHitsBeforeItIsFoundIsTaggedWithTheDronesLinkAndForwarded() {
        launchBehindTheHitShip();
        DamagingProjectileAPI own = projectileFrom(weapon);
        DamageAPI damage = mock(DamageAPI.class);
        ShipAPI hit = mock(ShipAPI.class);

        drone.modifyDamageDealt(own, hit, damage, new Vector2f(), true);

        assertEquals(0.8f, own.getCustomData().get(ChainLink.DEALT_MULT_KEY));
        verify(own).setSource(firingShip);
        verify(chain).handleHit(own, hit, damage, new Vector2f(), true);
        when(weapon.getChargeLevel()).thenReturn(1f);
        drone.advance(FRAME);
        assertTrue(drone.isReady());
    }

    @Test
    void hitsFromOtherWeaponsAreIgnored() {
        launchBehindTheHitShip();

        drone.modifyDamageDealt(projectileFrom(mock(WeaponAPI.class)), mock(ShipAPI.class), mock(DamageAPI.class), new Vector2f(), true);

        verify(chain, never()).handleHit(any(), any(), any(), any(), Mockito.anyBoolean());
    }

    @Test
    void anIdleDroneRemovesItselfFromTheBattleAndThePool() {
        drone.advance(RefractionDrone.IDLE_REMOVE_SECONDS + FRAME);

        verify(engine).removeEntity(droneShip);
        verify(pool).forget(drone);
        assertFalse(drone.isInPlay());
    }
}
