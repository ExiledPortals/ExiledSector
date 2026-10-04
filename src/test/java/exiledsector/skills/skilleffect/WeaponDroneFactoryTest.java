package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeaponDroneFactoryTest {

    private MockedStatic<Global> global;
    private MockedStatic<WeaponDroneStats> stats;
    private SettingsAPI settings;
    private CombatEngineAPI engine;
    private ProjectileWeaponSpecAPI shared;
    private final float[] chargeTime = {0.5f};
    private final int[] burstSize = {3};
    private final float[] refireDelay = {0f};
    private final List<String> seenDuringConstruction = new ArrayList<>();

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        engine = mock(CombatEngineAPI.class);
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getSettings).thenReturn(settings);
        global.when(Global::getCombatEngine).thenReturn(engine);
        FactoryAPI factory = mock(FactoryAPI.class);
        when(factory.createPerson()).thenReturn(mock(PersonAPI.class, Answers.RETURNS_DEEP_STUBS));
        global.when(Global::getFactory).thenReturn(factory);
        stats = Mockito.mockStatic(WeaponDroneStats.class);

        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(settings.getHullSpec(WeaponDroneFactory.HULL_ID)).thenReturn(hull);
        when(settings.createEmptyVariant(WeaponDroneFactory.HULL_ID, hull)).thenReturn(mock(ShipVariantAPI.class));
        shared = mock(ProjectileWeaponSpecAPI.class);
        when(shared.getChargeTime()).thenAnswer(invocation -> chargeTime[0]);
        when(shared.getBurstSize()).thenAnswer(invocation -> burstSize[0]);
        when(shared.getRefireDelay()).thenAnswer(invocation -> refireDelay[0]);
        doAnswer(invocation -> chargeTime[0] = invocation.getArgument(0)).when(shared).setChargeTime(anyFloat());
        doAnswer(invocation -> burstSize[0] = invocation.getArgument(0)).when(shared).setBurstSize(anyInt());
        doAnswer(invocation -> refireDelay[0] = invocation.getArgument(0)).when(shared).setRefireDelay(anyFloat());
        when(shared.getWeaponId()).thenReturn("pulselaser");
        when(settings.getWeaponSpec("pulselaser")).thenReturn(shared);
    }

    @AfterEach
    void tearDown() {
        stats.close();
        global.close();
    }

    private static WeaponAPI weaponWith(Object spec, WeaponAPI.WeaponSize size) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getSpec()).thenReturn((WeaponSpecAPI) spec);
        when(weapon.getSize()).thenReturn(size);
        return weapon;
    }

    private String sharedState() {
        return chargeTime[0] + "/" + burstSize[0] + "/" + refireDelay[0];
    }

    private ShipAPI droneThatRecordsTheSharedSpec() {
        ShipAPI drone = mock(ShipAPI.class, Answers.RETURNS_DEEP_STUBS);
        WeaponAPI droneWeapon = mock(WeaponAPI.class);
        doAnswer(invocation -> seenDuringConstruction.add("clone " + sharedState())).when(droneWeapon).ensureClonedSpec();
        when(drone.getAllWeapons()).thenReturn(List.of(droneWeapon));
        when(engine.createFXDrone(any())).thenAnswer(invocation -> {
            seenDuringConstruction.add("build " + sharedState());
            return drone;
        });
        return drone;
    }

    @Test
    void theDronesWeaponIsBuiltAndClonedAsAnInstantSingleShotThenTheSharedSpecIsRestored() {
        ShipAPI drone = droneThatRecordsTheSharedSpec();
        Object controller = new Object();

        Object result = WeaponDroneFactory.createSingleShot(mock(ShipAPI.class), weaponWith(shared, WeaponAPI.WeaponSize.SMALL),
                built -> controller);

        assertEquals(List.of("build 0.0/1/60.0", "clone 0.0/1/60.0"), seenDuringConstruction);
        assertEquals("0.5/3/0.0", sharedState());
        assertSame(controller, result);
        InOrder order = Mockito.inOrder(drone, engine);
        order.verify(drone).addListener(controller);
        order.verify(engine).addEntity(drone);
    }

    @Test
    void theSharedSpecIsRestoredEvenWhenBuildingTheDroneFails() {
        when(engine.createFXDrone(any())).thenThrow(new IllegalStateException("bad variant"));

        assertThrows(IllegalStateException.class, () -> WeaponDroneFactory.createSingleShot(mock(ShipAPI.class),
                weaponWith(shared, WeaponAPI.WeaponSize.SMALL), built -> new Object()));

        assertEquals("0.5/3/0.0", sharedState());
    }

    @Test
    void onlyProjectileWeaponsThatFireRealProjectilesCanRefract() {
        ProjectileWeaponSpecAPI projectileGun = mock(ProjectileWeaponSpecAPI.class);
        when(projectileGun.getWeaponId()).thenReturn("support_test_projectile");
        when(projectileGun.getProjectileSpec()).thenReturn(mock(ProjectileSpecAPI.class));
        ProjectileWeaponSpecAPI missileLauncher = mock(ProjectileWeaponSpecAPI.class);
        when(missileLauncher.getWeaponId()).thenReturn("support_test_missile");
        when(missileLauncher.getProjectileSpec()).thenReturn(mock(MissileSpecAPI.class));
        BeamWeaponSpecAPI beam = mock(BeamWeaponSpecAPI.class);
        when(beam.getWeaponId()).thenReturn("support_test_beam");

        assertTrue(WeaponDroneFactory.supportsProjectile(weaponWith(projectileGun, WeaponAPI.WeaponSize.MEDIUM)));
        assertFalse(WeaponDroneFactory.supportsProjectile(weaponWith(missileLauncher, WeaponAPI.WeaponSize.MEDIUM)));
        assertFalse(WeaponDroneFactory.supportsProjectile(weaponWith(beam, WeaponAPI.WeaponSize.MEDIUM)));
    }

    @Test
    void aWeaponMarkedUnsupportedStaysUnsupported() {
        ProjectileWeaponSpecAPI gun = mock(ProjectileWeaponSpecAPI.class);
        when(gun.getWeaponId()).thenReturn("support_test_marked");
        when(gun.getProjectileSpec()).thenReturn(mock(ProjectileSpecAPI.class));
        WeaponAPI weapon = weaponWith(gun, WeaponAPI.WeaponSize.SMALL);

        WeaponDroneFactory.markProjectileUnsupported(weapon);

        assertFalse(WeaponDroneFactory.supportsProjectile(weapon));
    }

    @Test
    void dronesCopyOnlyTheListedMarkerHullModsTheFiringShipHasWithoutDuplicates() throws Exception {
        when(settings.getMergedSpreadsheetDataForMod("hullmod", "data/config/exiledSector/drone_marker_hullmods.csv", "exiledSector"))
                .thenReturn(new org.json.JSONArray().put(new org.json.JSONObject().put("hullmod", "ix_dawnstar_neutron"))
                        .put(new org.json.JSONObject().put("hullmod", "ix_feedback_error")));
        when(settings.getMergedSpreadsheetDataForMod("weapon", "data/config/exiledSector/energy_chain_blocklist.csv", "exiledSector"))
                .thenReturn(new org.json.JSONArray());
        when(settings.getMergedSpreadsheetDataForMod("plugin", "data/config/exiledSector/split_beam_effect_blocklist.csv", "exiledSector"))
                .thenReturn(new org.json.JSONArray());
        CsvIdList.loadAll();
        ShipAPI firingShip = mock(ShipAPI.class);
        ShipVariantAPI firingVariant = mock(ShipVariantAPI.class);
        when(firingShip.getVariant()).thenReturn(firingVariant);
        when(firingVariant.getHullMods()).thenReturn(List.of("ix_dawnstar_neutron", "exiledSector_core", "safetyoverrides"));
        ShipAPI drone = mock(ShipAPI.class);
        ShipVariantAPI droneVariant = mock(ShipVariantAPI.class);
        java.util.Set<String> droneMods = new java.util.LinkedHashSet<>();
        when(drone.getVariant()).thenReturn(droneVariant);
        when(droneVariant.getHullMods()).thenReturn(droneMods);

        WeaponDroneFactory.mirrorMarkerHullMods(firingShip, drone);
        WeaponDroneFactory.mirrorMarkerHullMods(firingShip, drone);

        assertEquals(java.util.Set.of("ix_dawnstar_neutron"), droneMods);
    }

    @Test
    void aSplitBeamDronesShareListenerRunsBeforeTheSharedListenersSoTheySeeTheSharedDamage() {
        ShipAPI drone = droneThatRecordsTheSharedSpec();
        ShipAPI firingShip = mock(ShipAPI.class);
        DamageDealtModifier sharedListener = mock(DamageDealtModifier.class);
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(sharedListener));
        DamageDealtModifier share = mock(DamageDealtModifier.class);

        WeaponDroneFactory.create(firingShip, weaponWith(shared, WeaponAPI.WeaponSize.SMALL), share);

        InOrder order = Mockito.inOrder(drone, engine);
        order.verify(drone).addListener(share);
        order.verify(drone).addListener(sharedListener);
        order.verify(engine).addEntity(drone);
    }

    @Test
    void theRefractionControllerIsAddedBeforeTheFiringShipsSharedListeners() {
        ShipAPI drone = droneThatRecordsTheSharedSpec();
        ShipAPI firingShip = mock(ShipAPI.class);
        DamageDealtModifier sharedListener = mock(DamageDealtModifier.class);
        when(firingShip.getListeners(DamageDealtModifier.class)).thenReturn(List.of(sharedListener));
        Object controller = new Object();

        WeaponDroneFactory.createSingleShot(firingShip, weaponWith(shared, WeaponAPI.WeaponSize.SMALL), built -> controller);

        InOrder order = Mockito.inOrder(drone);
        order.verify(drone).addListener(controller);
        order.verify(drone).addListener(sharedListener);
        verify(engine).addEntity(drone);
    }
}
