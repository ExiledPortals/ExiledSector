package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ShotBehaviorSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.loading.WingRole;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

class BomberPayloadDetonationTest {

    private static final float FRAME = 0.1f;
    private static final float DETONATION_SECONDS = BomberPayloadDetonation.CHECK_INTERVAL_SECONDS + FRAME;

    private MockedStatic<Global> global;
    private CombatEngineAPI engine;
    private ShipAPI carrier;

    private static final class AlwaysRolls extends Random {
        @Override
        public float nextFloat() {
            return 0f;
        }
    }

    private static final class NeverRolls extends Random {
        @Override
        public float nextFloat() {
            return 0.999f;
        }
    }

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.isEntityInPlay(any())).thenReturn(true);
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);
        carrier = ship(new Vector2f(0f, 0f));
        when(carrier.getCollisionRadius()).thenReturn(200f);
    }

    @AfterEach
    void tearDown() {
        global.close();
    }

    private static ShipAPI ship(Vector2f location) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getLocation()).thenReturn(location);
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.getHitpoints()).thenReturn(100f);
        when(ship.getMaxHitpoints()).thenReturn(100f);
        return ship;
    }

    private static WeaponAPI weapon(WeaponAPI.WeaponType type, boolean usesAmmo, int ammo) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(type);
        when(weapon.usesAmmo()).thenReturn(usesAmmo);
        when(weapon.getAmmo()).thenReturn(ammo);
        when(weapon.getId()).thenReturn(type.name().toLowerCase() + "_" + ammo);
        return weapon;
    }

    private static DamageAPI damage(float amount, DamageType type, float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getBaseDamage()).thenReturn(amount);
        when(damage.getDamage()).thenReturn(amount);
        when(damage.getType()).thenReturn(type);
        when(damage.getFluxComponent()).thenReturn(emp);
        return damage;
    }

    private static MissileSpecAPI missileSpec(float damage, DamageType type, float visualRadius, DamagingExplosionSpec explosion,
                                              ShotBehaviorSpecAPI behavior) {
        MissileSpecAPI spec = mock(MissileSpecAPI.class);
        DamageAPI specDamage = damage(damage, type, 0f);
        when(spec.getDamage()).thenReturn(specDamage);
        when(spec.getExplosionRadius()).thenReturn(visualRadius);
        when(spec.getExplosionSpec()).thenReturn(explosion);
        when(spec.getBehaviorSpec()).thenReturn(behavior);
        return spec;
    }

    private static ShotBehaviorSpecAPI behavior(String name, JSONObject params) {
        ShotBehaviorSpecAPI behavior = mock(ShotBehaviorSpecAPI.class);
        when(behavior.getBehavorString()).thenReturn(name);
        when(behavior.getParams()).thenReturn(params);
        return behavior;
    }

    private static WeaponAPI armedWith(WeaponAPI.WeaponType type, int ammo, int burstSize, Object projectileSpec) {
        WeaponAPI weapon = weapon(type, true, ammo);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getBurstSize()).thenReturn(burstSize);
        when(spec.getProjectileSpec()).thenReturn(projectileSpec);
        when(weapon.getSpec()).thenReturn(spec);
        return weapon;
    }

    private static WeaponAPI beam(int ammo, float damagePerSecond, float burstSeconds, float empPerSecond) {
        WeaponAPI weapon = weapon(WeaponAPI.WeaponType.ENERGY, true, ammo);
        BeamWeaponSpecAPI spec = mock(BeamWeaponSpecAPI.class);
        when(spec.getDamagePerSecond()).thenReturn(damagePerSecond);
        when(spec.getBurstDuration()).thenReturn(burstSeconds);
        when(spec.getEMPPerSecond()).thenReturn(empPerSecond);
        when(spec.getDamageType()).thenReturn(DamageType.ENERGY);
        when(weapon.isBeam()).thenReturn(true);
        when(weapon.getSpec()).thenReturn(spec);
        return weapon;
    }

    private ShipAPI bomber(float distanceFromCarrier, WeaponAPI... weapons) {
        ShipAPI bomber = ship(new Vector2f(distanceFromCarrier, 0f));
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(wing.getRole()).thenReturn(WingRole.BOMBER);
        when(bomber.getWing()).thenReturn(wing);
        when(bomber.getAllWeapons()).thenReturn(List.of(weapons));
        return bomber;
    }

    private static void fly(BomberPayloadDetonation detonation, float seconds) {
        for (float elapsed = 0f; elapsed < seconds - 1e-4f; elapsed += FRAME) {
            detonation.advance(FRAME);
        }
    }

    private void detonate(ShipAPI bomber) {
        fly(new BomberPayloadDetonation(bomber, carrier, 100f, new AlwaysRolls()), DETONATION_SECONDS);
    }

    private void verifyKilled(ShipAPI bomber, int times) {
        verify(engine, times(times)).applyDamage(eq(bomber), any(), eq(BomberPayloadDetonation.SELF_DESTRUCT_DAMAGE), eq(DamageType.ENERGY),
                eq(0f), eq(true), eq(false), eq(bomber), eq(false));
    }

    private void verifyNoDetonation(ShipAPI bomber) {
        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        verify(engine, never()).spawnDamagingExplosion(any(), any(), any());
        verify(engine, never()).applyDamage(eq(bomber), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());
    }

    private Map<DamageType, DamagingExplosionSpec> foldedBlasts(ShipAPI bomber, int count) {
        ArgumentCaptor<DamagingExplosionSpec> blasts = ArgumentCaptor.forClass(DamagingExplosionSpec.class);
        verify(engine, times(count)).spawnDamagingExplosion(blasts.capture(), eq(bomber), any());
        return blasts.getAllValues().stream().collect(Collectors.toMap(DamagingExplosionSpec::getDamageType, Function.identity()));
    }

    private MissileAPI spawnsMissile(ShipAPI bomber, WeaponAPI weapon, MissileSpecAPI spec) {
        MissileAPI shot = mock(MissileAPI.class);
        when(shot.getSpec()).thenReturn(spec);
        when(engine.spawnProjectile(eq(bomber), eq(weapon), anyString(), any(), anyFloat(), any())).thenReturn(shot);
        return shot;
    }

    @Test
    void onlyBombersAreArmed() {
        ShipAPI fighter = ship(new Vector2f(2000f, 0f));
        FighterWingAPI wing = mock(FighterWingAPI.class);
        when(wing.getRole()).thenReturn(WingRole.FIGHTER);
        when(fighter.getWing()).thenReturn(wing);
        BomberPayloadDetonation.arm(fighter, carrier, 5f);
        verify(fighter, never()).addListener(any());

        ShipAPI bomber = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        BomberPayloadDetonation.arm(bomber, carrier, 5f);
        verify(bomber, times(1)).addListener(any(BomberPayloadDetonation.class));
    }

    @Test
    void theDiceAreOnlyRolledEveryThreeSecondsAndOnlyFailingRollsSpareTheBomber() {
        ShipAPI bomber = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        fly(new BomberPayloadDetonation(bomber, carrier, 5f, new NeverRolls()), 30f);
        verifyNoDetonation(bomber);

        BomberPayloadDetonation unlucky = new BomberPayloadDetonation(bomber, carrier, 5f, new AlwaysRolls());
        fly(unlucky, BomberPayloadDetonation.CHECK_INTERVAL_SECONDS - 0.2f);
        verifyNoDetonation(bomber);
        fly(unlucky, 0.3f);
        verifyKilled(bomber, 1);
    }

    @Test
    void bombersCloseToTheirCarrierNeverDetonate() {
        float justInside = 200f + BomberPayloadDetonation.SAFE_DISTANCE_FROM_CARRIER - 10f;
        ShipAPI bomber = bomber(justInside, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        fly(new BomberPayloadDetonation(bomber, carrier, 100f, new AlwaysRolls()), 30f);
        verifyNoDetonation(bomber);
    }

    @Test
    void landingBombersAndBombersThatDroppedTheirPayloadNeverDetonate() {
        ShipAPI landing = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 1));
        when(landing.isLanding()).thenReturn(true);
        fly(new BomberPayloadDetonation(landing, carrier, 100f, new AlwaysRolls()), 10f);
        verifyNoDetonation(landing);

        ShipAPI spent = bomber(2000f, weapon(WeaponAPI.WeaponType.MISSILE, true, 0));
        fly(new BomberPayloadDetonation(spent, carrier, 100f, new AlwaysRolls()), 10f);
        verifyNoDetonation(spent);
    }

    @Test
    void weaponsWithoutAmmunitionNeverCountAsPayload() {
        WeaponAPI gun = weapon(WeaponAPI.WeaponType.BALLISTIC, false, 0);
        WeaponAPI laser = beam(0, 500f, 1f, 0f);
        when(laser.usesAmmo()).thenReturn(false);
        when(gun.getAmmoPerSecond()).thenReturn(2f);
        ShipAPI bomber = bomber(2000f, gun, laser);

        assertFalse(BomberPayloadDetonation.carriesPayload(bomber));
        fly(new BomberPayloadDetonation(bomber, carrier, 100f, new AlwaysRolls()), 30f);
        verifyNoDetonation(bomber);
    }

    @Test
    void regeneratingOrdnanceCountsWithAtLeastOneShotWhenItIsAllTheBomberCarries() {
        WeaponAPI pointDefence = beam(0, 350f, 0.3f, 0f);
        when(pointDefence.getSpec().getAmmoPerSecond()).thenReturn(0.5f);
        WeaponAPI rockets = armedWith(WeaponAPI.WeaponType.MISSILE, 0, 4, missileSpec(200f, DamageType.HIGH_EXPLOSIVE, 50f, null, null));
        AmmoTrackerAPI tracker = mock(AmmoTrackerAPI.class);
        when(tracker.getAmmoPerSecond()).thenReturn(0.1f);
        when(rockets.getAmmoTracker()).thenReturn(tracker);

        assertTrue(BomberPayloadDetonation.isLoaded(pointDefence, true));
        assertTrue(BomberPayloadDetonation.isLoaded(rockets, true));
        assertEquals(1, BomberPayloadDetonation.shotsCarried(pointDefence));
        assertEquals(1, BomberPayloadDetonation.shotsCarried(rockets));

        ShipAPI bomber = bomber(2000f, pointDefence, rockets);
        detonate(bomber);
        Map<DamageType, DamagingExplosionSpec> blasts = foldedBlasts(bomber, 2);
        assertEquals(350f * 0.3f, blasts.get(DamageType.ENERGY).getMaxDamage(), 1e-3f);
        assertEquals(200f, blasts.get(DamageType.HIGH_EXPLOSIVE).getMaxDamage(), 1e-3f);
        verifyKilled(bomber, 1);
    }

    @Test
    void regeneratingOrdnanceOnlyCountsWhileTheBomberStillCarriesItsFixedPayload() {
        WeaponAPI pointDefence = beam(0, 350f, 0.3f, 0f);
        when(pointDefence.getSpec().getAmmoPerSecond()).thenReturn(0.5f);
        WeaponAPI spentSabot = armedWith(WeaponAPI.WeaponType.MISSILE, 0, 1, missileSpec(100f, DamageType.KINETIC, 125f, null, null));
        ShipAPI spentLongbow = bomber(2000f, pointDefence, spentSabot);

        assertFalse(BomberPayloadDetonation.regeneratingOrdnanceCounts(spentLongbow));
        assertFalse(BomberPayloadDetonation.carriesPayload(spentLongbow));
        fly(new BomberPayloadDetonation(spentLongbow, carrier, 100f, new AlwaysRolls()), 30f);
        verifyNoDetonation(spentLongbow);

        WeaponAPI loadedSabot = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, missileSpec(100f, DamageType.KINETIC, 125f, null, null));
        ShipAPI armedLongbow = bomber(2000f, pointDefence, loadedSabot);

        assertTrue(BomberPayloadDetonation.regeneratingOrdnanceCounts(armedLongbow));
        assertEquals(2, BomberPayloadDetonation.payloads(armedLongbow).size());
    }

    @Test
    void aDisabledFixedPayloadStillKeepsRegeneratingOrdnanceFromCounting() {
        WeaponAPI pointDefence = beam(0, 350f, 0.3f, 0f);
        when(pointDefence.getSpec().getAmmoPerSecond()).thenReturn(0.5f);
        WeaponAPI jammedTorpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, missileSpec(1000f, DamageType.HIGH_EXPLOSIVE, 125f, null, null));
        when(jammedTorpedo.isDisabled()).thenReturn(true);

        assertFalse(BomberPayloadDetonation.carriesPayload(bomber(2000f, pointDefence, jammedTorpedo)));
    }

    @Test
    void everyLoadedWeaponAddsItsDamageForAsManyShotsAsOneBurstCarries() {
        WeaponAPI breachSalvo = armedWith(WeaponAPI.WeaponType.MISSILE, 10, 3, missileSpec(150f, DamageType.HIGH_EXPLOSIVE, 80f, null, null));
        WeaponAPI harpoon = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, missileSpec(1500f, DamageType.HIGH_EXPLOSIVE, 150f, null, null));
        ProjectileSpecAPI slug = mock(ProjectileSpecAPI.class);
        DamageAPI slugDamage = damage(100f, DamageType.KINETIC, 0f);
        when(slug.getDamage()).thenReturn(slugDamage);
        WeaponAPI cannon = armedWith(WeaponAPI.WeaponType.BALLISTIC, 5, 2, slug);
        WeaponAPI spentTorpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 0, 1, missileSpec(4000f, DamageType.HIGH_EXPLOSIVE, 350f, null, null));
        ShipAPI bomber = bomber(2000f, breachSalvo, harpoon, cannon, spentTorpedo);

        assertEquals(3, BomberPayloadDetonation.payloads(bomber).size());
        detonate(bomber);

        Map<DamageType, DamagingExplosionSpec> blasts = foldedBlasts(bomber, 2);
        DamagingExplosionSpec explosive = blasts.get(DamageType.HIGH_EXPLOSIVE);
        assertEquals(150f * 3 + 1500f, explosive.getMaxDamage(), 1e-3f);
        assertEquals(150f, explosive.getRadius(), 1e-3f);
        assertEquals(CollisionClass.HITS_SHIPS_AND_ASTEROIDS, explosive.getCollisionClass());
        assertEquals(CollisionClass.HITS_SHIPS_AND_ASTEROIDS, explosive.getCollisionClassIfByFighter());
        DamagingExplosionSpec kinetic = blasts.get(DamageType.KINETIC);
        assertEquals(200f, kinetic.getMaxDamage(), 1e-3f);
        assertEquals(BomberPayloadDetonation.MIN_BLAST_RADIUS, kinetic.getRadius(), 1e-3f);
        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }

    @Test
    void splittingMissilesDetonateWithEveryWarheadTheyCarry() throws JSONException {
        JSONObject params = new JSONObject("{\"numShots\":5,\"damage\":200,\"emp\":200,\"damageType\":\"KINETIC\"}");
        MissileSpecAPI sabot = missileSpec(100f, DamageType.KINETIC, 125f, null, behavior("MIRV", params));
        ShipAPI bomber = bomber(2000f, armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, sabot));

        detonate(bomber);

        DamagingExplosionSpec blast = foldedBlasts(bomber, 1).get(DamageType.KINETIC);
        assertEquals(1000f, blast.getMaxDamage(), 1e-3f);
        assertEquals(1000f, blast.getMaxEMPDamage(), 1e-3f);
        assertEquals(125f, blast.getRadius(), 1e-3f);
        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }

    @Test
    void ammoLimitedBeamsCountAsPayloadAndAreNeverSpawned() {
        WeaponAPI balefire = beam(1, 1500f, 0.9f, 1200f);
        ShipAPI bomber = bomber(2000f, balefire);

        assertTrue(BomberPayloadDetonation.carriesPayload(bomber));
        detonate(bomber);

        DamagingExplosionSpec blast = foldedBlasts(bomber, 1).get(DamageType.ENERGY);
        assertEquals(1350f, blast.getMaxDamage(), 1e-2f);
        assertEquals(1080f, blast.getMaxEMPDamage(), 1e-2f);
        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        verifyKilled(bomber, 1);
    }

    @Test
    void aTinyAddOnExplosionIsFoldedIntoTheBlastInsteadOfBeingUsed() {
        MissileSpecAPI sower = missileSpec(4000f, DamageType.KINETIC, 350f, BomberPayloadDetonation.blast(4000f, 0f, DamageType.KINETIC, 2f, null), null);
        ShipAPI bomber = bomber(2000f, armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, sower));

        detonate(bomber);

        DamagingExplosionSpec blast = foldedBlasts(bomber, 1).get(DamageType.KINETIC);
        assertEquals(4000f, blast.getMaxDamage(), 1e-3f);
        assertEquals(350f, blast.getRadius(), 1e-3f);
        verify(engine, never()).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
    }

    @Test
    void aPayloadWithARealExplosionUsesItsOwn() {
        MissileSpecAPI atropos = missileSpec(1000f, DamageType.HIGH_EXPLOSIVE, 250f, BomberPayloadDetonation.blast(1000f, 0f, DamageType.HIGH_EXPLOSIVE, 250f, null), null);
        WeaponAPI torpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, atropos);
        ShipAPI bomber = bomber(2000f, torpedo);
        MissileAPI shot = spawnsMissile(bomber, torpedo, atropos);
        when(shot.explode()).thenReturn(mock(DamagingProjectileAPI.class));

        detonate(bomber);

        verify(shot).explode();
        verify(engine).removeEntity(shot);
        verify(engine, never()).spawnDamagingExplosion(any(), any(), any());
        verify(engine).spawnExplosion(any(), any(), any(), eq(250f * 2f), anyFloat());
        verifyKilled(bomber, 1);
    }

    @Test
    void anUnsupportedSpawnFallsBackToTheBlast() {
        MissileSpecAPI atropos = missileSpec(1000f, DamageType.HIGH_EXPLOSIVE, 250f, BomberPayloadDetonation.blast(1000f, 0f, DamageType.HIGH_EXPLOSIVE, 250f, null), null);
        WeaponAPI torpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, atropos);
        ShipAPI bomber = bomber(2000f, torpedo);
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(null);

        detonate(bomber);

        assertEquals(1000f, foldedBlasts(bomber, 1).get(DamageType.HIGH_EXPLOSIVE).getMaxDamage(), 1e-3f);
        verifyKilled(bomber, 1);
    }

    @Test
    void aSpawnThatThrowsNeverCrashesAndStillBlowsUp() {
        MissileSpecAPI atropos = missileSpec(1000f, DamageType.HIGH_EXPLOSIVE, 250f, BomberPayloadDetonation.blast(1000f, 0f, DamageType.HIGH_EXPLOSIVE, 250f, null), null);
        WeaponAPI torpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, atropos);
        ShipAPI bomber = bomber(2000f, torpedo);
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenThrow(new ClassCastException("beam spec"));

        detonate(bomber);

        assertEquals(1000f, foldedBlasts(bomber, 1).get(DamageType.HIGH_EXPLOSIVE).getMaxDamage(), 1e-3f);
        verifyKilled(bomber, 1);
    }

    @Test
    void aShotWhoseExplosionThrowsIsRemovedAndFolded() {
        MissileSpecAPI atropos = missileSpec(1000f, DamageType.HIGH_EXPLOSIVE, 250f, BomberPayloadDetonation.blast(1000f, 0f, DamageType.HIGH_EXPLOSIVE, 250f, null), null);
        WeaponAPI torpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, atropos);
        ShipAPI bomber = bomber(2000f, torpedo);
        MissileAPI shot = spawnsMissile(bomber, torpedo, atropos);
        when(shot.explode()).thenThrow(new IllegalStateException("modded explosion"));

        detonate(bomber);

        verify(engine).removeEntity(shot);
        assertEquals(1000f, foldedBlasts(bomber, 1).get(DamageType.HIGH_EXPLOSIVE).getMaxDamage(), 1e-3f);
    }

    @Test
    void proximityFusedPayloadsExplodeThroughTheirOwnFuse() throws JSONException {
        JSONObject params = new JSONObject("{\"range\":50,\"explosionSpec\":{\"duration\":0.1,\"radius\":150,\"coreRadius\":75,"
                + "\"collisionClass\":\"PROJECTILE_FF\",\"collisionClassByFighter\":\"PROJECTILE_FIGHTER\",\"particleColor\":[255,255,255,255]}}");
        ShotBehaviorSpecAPI fuse = behavior("PROXIMITY_FUSE", params);
        ProximityExplosionEffect afterEffect = mock(ProximityExplosionEffect.class);
        when(fuse.getOnProximityExplosionEffect()).thenReturn(afterEffect);
        MissileSpecAPI plasma = missileSpec(300f, DamageType.ENERGY, 60f, null, fuse);
        WeaponAPI caster = armedWith(WeaponAPI.WeaponType.MISSILE, 5, 1, plasma);
        ShipAPI bomber = bomber(2000f, caster);
        MissileAPI shot = spawnsMissile(bomber, caster, plasma);
        when(shot.getDamageAmount()).thenReturn(330f);
        when(shot.getDamageType()).thenReturn(DamageType.ENERGY);
        DamagingProjectileAPI fuseExplosion = mock(DamagingProjectileAPI.class);
        when(engine.spawnDamagingExplosion(any(), eq(bomber), any())).thenReturn(fuseExplosion);

        global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));

        detonate(bomber);

        DamagingExplosionSpec explosion = foldedBlasts(bomber, 1).get(DamageType.ENERGY);
        assertEquals(150f, explosion.getRadius(), 1e-3f);
        assertEquals(330f, explosion.getMaxDamage(), 1e-3f);
        assertEquals(165f, explosion.getMinDamage(), 1e-3f);
        assertEquals(CollisionClass.PROJECTILE_FF, explosion.getCollisionClass());
        verify(afterEffect).onExplosion(fuseExplosion, shot);
        verify(engine).removeEntity(shot);
    }

    @Test
    void minesThatExplodeThemselvesAreNotExplodedTwice() throws JSONException {
        JSONObject params = new JSONObject("{\"range\":50,\"explosionSpec\":{\"duration\":0.1,\"radius\":200,\"coreRadius\":100}}");
        MissileSpecAPI mine = missileSpec(1000f, DamageType.HIGH_EXPLOSIVE, 60f, null, behavior("PROXIMITY_FUSE", params));
        WeaponAPI layer = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, mine);
        ShipAPI bomber = bomber(2000f, layer);
        MissileAPI shot = spawnsMissile(bomber, layer, mine);
        when(shot.explode()).thenReturn(mock(DamagingProjectileAPI.class));
        when(engine.isEntityInPlay(shot)).thenReturn(false);

        detonate(bomber);

        verify(shot).explode();
        verify(engine, never()).spawnDamagingExplosion(any(), any(), any());
        verify(engine, never()).removeEntity(shot);
    }

    @Test
    void onlyAFewPayloadsAreSpawnedAndTheRestIsFoldedIntoTheBlast() {
        MissileSpecAPI bomb = missileSpec(100f, DamageType.HIGH_EXPLOSIVE, 100f, BomberPayloadDetonation.blast(100f, 0f, DamageType.HIGH_EXPLOSIVE, 100f, null), null);
        WeaponAPI rack = armedWith(WeaponAPI.WeaponType.MISSILE, 20, 20, bomb);
        ShipAPI bomber = bomber(2000f, rack);
        MissileAPI shot = spawnsMissile(bomber, rack, bomb);
        when(shot.explode()).thenReturn(mock(DamagingProjectileAPI.class));

        detonate(bomber);

        verify(engine, times(BomberPayloadDetonation.MAX_SPAWNED_PAYLOADS)).spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any());
        float folded = (20 - BomberPayloadDetonation.MAX_SPAWNED_PAYLOADS) * 100f;
        assertEquals(folded, foldedBlasts(bomber, 1).get(DamageType.HIGH_EXPLOSIVE).getMaxDamage(), 1e-3f);
    }

    @Test
    void theBiggestPayloadsGetTheSpawnsFirst() {
        MissileSpecAPI small = missileSpec(10f, DamageType.HIGH_EXPLOSIVE, 100f, BomberPayloadDetonation.blast(10f, 0f, DamageType.HIGH_EXPLOSIVE, 100f, null), null);
        MissileSpecAPI reaper = missileSpec(4000f, DamageType.HIGH_EXPLOSIVE, 350f, BomberPayloadDetonation.blast(4000f, 0f, DamageType.HIGH_EXPLOSIVE, 350f, null), null);
        WeaponAPI pods = armedWith(WeaponAPI.WeaponType.MISSILE, 20, 20, small);
        WeaponAPI torpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, reaper);
        ShipAPI bomber = bomber(2000f, pods, torpedo);
        MissileAPI podShot = spawnsMissile(bomber, pods, small);
        when(podShot.explode()).thenReturn(mock(DamagingProjectileAPI.class));
        MissileAPI reaperShot = spawnsMissile(bomber, torpedo, reaper);
        when(reaperShot.explode()).thenReturn(mock(DamagingProjectileAPI.class));

        detonate(bomber);

        verify(reaperShot).explode();
        verify(engine, times(BomberPayloadDetonation.MAX_SPAWNED_PAYLOADS - 1)).spawnProjectile(eq(bomber), eq(pods), anyString(), any(), anyFloat(), any());
        float folded = (20 - (BomberPayloadDetonation.MAX_SPAWNED_PAYLOADS - 1)) * 10f;
        assertEquals(folded, foldedBlasts(bomber, 1).get(DamageType.HIGH_EXPLOSIVE).getMaxDamage(), 1e-3f);
    }

    @Test
    void theBombersOwnDamageBonusesScaleFoldedDamageAndEmpLikeTheEngine() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat missileDamage = new MutableStat(1f);
        missileDamage.modifyPercent("test", 50f);
        missileDamage.modifyFlat("test", 0.1f);
        when(stats.getMissileWeaponDamageMult()).thenReturn(missileDamage);
        MutableStat beamDamage = new MutableStat(1f);
        beamDamage.modifyMult("test", 2f);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beamDamage);
        MissileSpecAPI cauldron = missileSpec(750f, DamageType.HIGH_EXPLOSIVE, 150f, null, null);
        DamageAPI withEmp = damage(750f, DamageType.HIGH_EXPLOSIVE, 100f);
        when(cauldron.getDamage()).thenReturn(withEmp);
        WeaponAPI harpoon = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, cauldron);
        WeaponAPI lance = beam(1, 1000f, 1f, 0f);
        ShipAPI bomber = bomber(2000f, harpoon, lance);
        when(bomber.getMutableStats()).thenReturn(stats);

        detonate(bomber);

        Map<DamageType, DamagingExplosionSpec> blasts = foldedBlasts(bomber, 2);
        assertEquals(750f * 1.6f, blasts.get(DamageType.HIGH_EXPLOSIVE).getMaxDamage(), 1e-2f);
        assertEquals(100f * 1.6f, blasts.get(DamageType.HIGH_EXPLOSIVE).getMaxEMPDamage(), 1e-2f);
        assertEquals(2000f, blasts.get(DamageType.ENERGY).getMaxDamage(), 1e-2f);
    }

    @Test
    void aFoldedPayloadWithARealExplosionKeepsThatExplosionsRadius() {
        MissileSpecAPI reaper = missileSpec(4000f, DamageType.HIGH_EXPLOSIVE, 350f, BomberPayloadDetonation.blast(4000f, 0f, DamageType.HIGH_EXPLOSIVE, 175f, null), null);
        ShipAPI bomber = bomber(2000f, armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, reaper));
        when(engine.spawnProjectile(any(), any(), anyString(), any(), anyFloat(), any())).thenReturn(null);

        detonate(bomber);

        assertEquals(175f, foldedBlasts(bomber, 1).get(DamageType.HIGH_EXPLOSIVE).getRadius(), 1e-3f);
        verify(engine).spawnExplosion(any(), any(), any(), eq(350f * 2f), anyFloat());
    }

    @Test
    void aPayloadThatExplodesOnItsOwnStillDeliversItsEmp() {
        MissileSpecAPI cauldron = missileSpec(800f, DamageType.ENERGY, 275f, BomberPayloadDetonation.blast(800f, 0f, DamageType.ENERGY, 210f, null), null);
        DamageAPI withEmp = damage(800f, DamageType.ENERGY, 1600f);
        when(cauldron.getDamage()).thenReturn(withEmp);
        WeaponAPI torpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, cauldron);
        ShipAPI bomber = bomber(2000f, torpedo);
        MissileAPI shot = spawnsMissile(bomber, torpedo, cauldron);
        when(shot.explode()).thenReturn(mock(DamagingProjectileAPI.class));

        detonate(bomber);

        DamagingExplosionSpec empOnly = foldedBlasts(bomber, 1).get(DamageType.ENERGY);
        assertEquals(0f, empOnly.getMaxDamage(), 1e-3f);
        assertEquals(1600f, empOnly.getMaxEMPDamage(), 1e-3f);
        assertEquals(210f, empOnly.getRadius(), 1e-3f);
    }

    @Test
    void disabledWeaponsDoNotCountAsPayload() {
        WeaponAPI jammed = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, missileSpec(750f, DamageType.HIGH_EXPLOSIVE, 150f, null, null));
        when(jammed.isDisabled()).thenReturn(true);
        WeaponAPI wrecked = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, missileSpec(750f, DamageType.HIGH_EXPLOSIVE, 150f, null, null));
        when(wrecked.isPermanentlyDisabled()).thenReturn(true);

        assertFalse(BomberPayloadDetonation.carriesPayload(bomber(2000f, jammed, wrecked)));
    }

    @Test
    void theSelfDestructIsTheVanillaForcedKill() {
        ShipAPI bomber = bomber(2000f, armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, missileSpec(750f, DamageType.HIGH_EXPLOSIVE, 150f, null, null)));

        detonate(bomber);

        verifyKilled(bomber, 1);
        assertEquals(1000000f, BomberPayloadDetonation.SELF_DESTRUCT_DAMAGE);
    }

    @Test
    void theFallbackBlastHitsEveryShipNearbyLikeAVanillaTorpedo() {
        DamagingExplosionSpec blast = BomberPayloadDetonation.blast(800f, 100f, DamageType.FRAGMENTATION, 100f, null);

        assertEquals(CollisionClass.HITS_SHIPS_AND_ASTEROIDS, blast.getCollisionClass());
        assertEquals(CollisionClass.HITS_SHIPS_AND_ASTEROIDS, blast.getCollisionClassIfByFighter());
        assertEquals(DamageType.FRAGMENTATION, blast.getDamageType());
        assertEquals(100f, blast.getMaxEMPDamage());
    }

    @Test
    void aBomberOnlyEverDetonatesOnce() {
        MissileSpecAPI atropos = missileSpec(1000f, DamageType.HIGH_EXPLOSIVE, 250f, BomberPayloadDetonation.blast(1000f, 0f, DamageType.HIGH_EXPLOSIVE, 250f, null), null);
        WeaponAPI torpedo = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, atropos);
        WeaponAPI harpoon = armedWith(WeaponAPI.WeaponType.MISSILE, 1, 1, missileSpec(750f, DamageType.KINETIC, 150f, null, null));
        ShipAPI bomber = bomber(2000f, torpedo, harpoon);
        MissileAPI shot = spawnsMissile(bomber, torpedo, atropos);
        when(shot.explode()).thenReturn(mock(DamagingProjectileAPI.class));

        fly(new BomberPayloadDetonation(bomber, carrier, 100f, new AlwaysRolls()), 30f);

        verifyKilled(bomber, 1);
        verify(shot, times(1)).explode();
        verify(engine, times(1)).spawnDamagingExplosion(any(), eq(bomber), any());
    }
}
