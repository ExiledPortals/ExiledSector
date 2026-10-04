package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EnergyChainListenerTest {

    private MockedStatic<Global> global;
    private MockedStatic<WeaponDroneFactory> factory;
    private MockedStatic<LunaSettings> luna;
    private CombatEngineAPI engine;
    private RefractionDrones drones;
    private ShipAPI ship;
    private EnergyChainListener listener;

    @BeforeEach
    void setUp() {
        luna = Mockito.mockStatic(LunaSettings.class);
        luna.when(() -> LunaSettings.getInt(anyString(), anyString())).thenReturn(MaxChainCountConfig.DEFAULT);
        engine = mock(CombatEngineAPI.class);
        when(engine.getListenerManager()).thenReturn(mock(CombatListenerManagerAPI.class));
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getCombatEngine).thenReturn(engine);
        factory = Mockito.mockStatic(WeaponDroneFactory.class);
        factory.when(() -> WeaponDroneFactory.supportsProjectile(any())).thenReturn(true);
        ship = firingShip(20f);
        drones = mock(RefractionDrones.class);
        listener = new EnergyChainListener(ship, drones);
    }

    @AfterEach
    void tearDown() {
        factory.close();
        global.close();
        luna.close();
    }

    private static ShipAPI firingShip(float falloffPercent) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getOwner()).thenReturn(0);
        when(ship.isAlive()).thenReturn(true);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(EnergyChainListener.CHANCE_KEY, 0f)).thenReturn(100f);
        when(dynamic.getValue(EnergyChainListener.FALLOFF_KEY, 0f)).thenReturn(falloffPercent);
        return ship;
    }

    private static ShipAPI enemy(Vector2f location) {
        ShipAPI enemy = mock(ShipAPI.class);
        when(enemy.getOwner()).thenReturn(1);
        when(enemy.isAlive()).thenReturn(true);
        when(enemy.getCollisionClass()).thenReturn(CollisionClass.SHIP);
        when(enemy.getLocation()).thenReturn(location);
        return enemy;
    }

    private static WeaponAPI weapon(String id, boolean isBeam, WeaponAPI.WeaponType type) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getId()).thenReturn(id);
        when(weapon.isBeam()).thenReturn(isBeam);
        when(weapon.getType()).thenReturn(type);
        when(weapon.getRange()).thenReturn(1000f);
        return weapon;
    }

    private static WeaponAPI energyGun() {
        return weapon("energy_gun", false, WeaponAPI.WeaponType.ENERGY);
    }

    private static DamagingProjectileAPI shot(WeaponAPI weapon, ChainLink tag) {
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        Map<String, Object> data = new HashMap<>();
        when(projectile.getWeapon()).thenReturn(weapon);
        when(projectile.getCustomData()).thenReturn(data);
        if (tag != null) {
            data.put(ChainLink.HIT_LIST_KEY, tag.hitSoFar());
            data.put(ChainLink.COUNT_KEY, tag.count());
            data.put(ChainLink.DEALT_MULT_KEY, tag.dealtMult());
        }
        return projectile;
    }

    private void stubShips(ShipAPI... ships) {
        CollisionGridAPI grid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(grid);
        when(grid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> List.<Object>of(ships).iterator());
    }

    private static DamageAPI damage(float base) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(base);
        when(damage.getBaseDamage()).thenReturn(base);
        return damage;
    }

    private ChainLink launchedLink(WeaponAPI weapon, ShipAPI hitShip, ShipAPI expectedTarget) {
        ArgumentCaptor<ChainLink> link = ArgumentCaptor.forClass(ChainLink.class);
        verify(drones).launch(eq(listener), eq(weapon), eq(hitShip), any(Vector2f.class), eq(expectedTarget), link.capture());
        return link.getValue();
    }

    @Test
    void aShieldHitRefractsTowardsTheNearestEnemyWithTheFirstHopsFalloff() {
        WeaponAPI weapon = energyGun();
        ShipAPI hit = enemy(new Vector2f(0f, 0f));
        ShipAPI near = enemy(new Vector2f(100f, 0f));
        ShipAPI far = enemy(new Vector2f(400f, 0f));
        stubShips(ship, hit, far, near);

        listener.modifyDamageDealt(shot(weapon, null), hit, damage(100f), new Vector2f(), true);

        ChainLink link = launchedLink(weapon, hit, near);
        assertEquals(1, link.count());
        assertEquals(0.8f, link.dealtMult(), 0.0001f);
        assertEquals(List.of(ship, hit), link.hitSoFar());
    }

    @Test
    void aRefractedShotDealsItsReducedShareAndRefractsOnToTheNearestShipNotAlreadyHit() {
        WeaponAPI weapon = energyGun();
        ShipAPI firstTarget = enemy(new Vector2f(100f, 150f));
        ShipAPI current = enemy(new Vector2f(100f, 0f));
        ShipAPI farther = enemy(new Vector2f(400f, 0f));
        stubShips(ship, firstTarget, current, farther);
        DamageAPI damage = damage(100f);

        listener.modifyDamageDealt(shot(weapon, new ChainLink(List.of(ship, firstTarget), 1, 0.8f)), current, damage,
                new Vector2f(100f, 0f), true);

        verify(damage).setDamage(80f);
        ChainLink link = launchedLink(weapon, current, farther);
        assertEquals(2, link.count());
        assertEquals(0.64f, link.dealtMult(), 0.0001f);
        assertEquals(List.of(ship, firstTarget, current), link.hitSoFar());
    }

    @Test
    void aRefractedShotThatHitsHullDealsItsReducedShareWithoutRefracting() {
        ShipAPI current = enemy(new Vector2f(100f, 0f));
        stubShips(ship, current, enemy(new Vector2f(200f, 0f)));
        DamageAPI damage = damage(100f);

        listener.modifyDamageDealt(shot(energyGun(), new ChainLink(List.of(ship), 1, 0.5f)), current, damage, new Vector2f(), false);

        verify(damage).setDamage(50f);
        verify(drones, never()).launch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aShotAtTheMaxChainCountNeverRefractsAgain() {
        ShipAPI current = enemy(new Vector2f(100f, 0f));
        stubShips(ship, current, enemy(new Vector2f(200f, 0f)));

        listener.modifyDamageDealt(shot(energyGun(), new ChainLink(List.of(ship), MaxChainCountConfig.DEFAULT, 1f)), current,
                damage(100f), new Vector2f(), true);

        verify(drones, never()).launch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aShotOneRefractionBelowTheCapStillRefracts() {
        WeaponAPI weapon = energyGun();
        ShipAPI current = enemy(new Vector2f(100f, 0f));
        ShipAPI next = enemy(new Vector2f(200f, 0f));
        stubShips(ship, current, next);

        listener.modifyDamageDealt(shot(weapon, new ChainLink(List.of(ship), MaxChainCountConfig.DEFAULT - 1, 1f)), current,
                damage(100f), new Vector2f(100f, 0f), true);

        assertEquals(MaxChainCountConfig.DEFAULT, launchedLink(weapon, current, next).count());
    }

    @Test
    void aFriendlyShieldNeverStartsARefractionButARefractedShotStillDealsItsReducedShare() {
        ShipAPI friendly = enemy(new Vector2f(0f, 0f));
        when(friendly.getOwner()).thenReturn(0);
        stubShips(ship, friendly, enemy(new Vector2f(100f, 0f)));
        DamageAPI damage = damage(100f);

        listener.modifyDamageDealt(shot(energyGun(), new ChainLink(List.of(ship), 1, 0.8f)), friendly, damage, new Vector2f(), true);

        verify(damage).setDamage(80f);
        verify(drones, never()).launch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void beamsAndNonEnergyWeaponsNeverRefractOrLoseDamage() {
        ShipAPI hit = enemy(new Vector2f(0f, 0f));
        stubShips(ship, hit, enemy(new Vector2f(100f, 0f)));
        DamageAPI damage = damage(100f);
        ChainLink tag = new ChainLink(List.of(ship), 1, 0.5f);

        listener.modifyDamageDealt(shot(weapon("beam", true, WeaponAPI.WeaponType.ENERGY), tag), hit, damage, new Vector2f(), true);
        listener.modifyDamageDealt(shot(weapon("gun", false, WeaponAPI.WeaponType.BALLISTIC), tag), hit, damage, new Vector2f(), true);

        verify(damage, never()).setDamage(anyFloat());
        verify(drones, never()).launch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void blocklistedWeaponsNeverRefract() throws Exception {
        SettingsAPI settings = mock(SettingsAPI.class);
        org.json.JSONArray rows = new org.json.JSONArray().put(new org.json.JSONObject().put("weapon", "blocklisted_gun"));
        when(settings.getMergedSpreadsheetDataForMod("weapon", "data/config/exiledSector/energy_chain_blocklist.csv", "exiledSector"))
                .thenReturn(rows);
        when(settings.getMergedSpreadsheetDataForMod("plugin", "data/config/exiledSector/split_beam_effect_blocklist.csv", "exiledSector"))
                .thenReturn(new org.json.JSONArray());
        global.when(Global::getSettings).thenReturn(settings);
        CsvIdBlocklist.loadAll();
        ShipAPI hit = enemy(new Vector2f(0f, 0f));
        stubShips(ship, hit, enemy(new Vector2f(100f, 0f)));

        listener.modifyDamageDealt(shot(weapon("blocklisted_gun", false, WeaponAPI.WeaponType.ENERGY), null), hit, damage(100f),
                new Vector2f(), true);

        verify(drones, never()).launch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void alliesHulksDronesAndPhasedShipsAreNeverChosen() {
        WeaponAPI weapon = energyGun();
        ShipAPI hit = enemy(new Vector2f(0f, 0f));
        ShipAPI ally = enemy(new Vector2f(10f, 0f));
        when(ally.getOwner()).thenReturn(0);
        ShipAPI hulk = enemy(new Vector2f(20f, 0f));
        when(hulk.isHulk()).thenReturn(true);
        ShipAPI drone = enemy(new Vector2f(30f, 0f));
        when(drone.getCollisionClass()).thenReturn(CollisionClass.NONE);
        ShipAPI valid = enemy(new Vector2f(500f, 0f));
        stubShips(ship, hit, ally, hulk, drone, valid);

        listener.modifyDamageDealt(shot(weapon, null), hit, damage(100f), new Vector2f(), true);

        launchedLink(weapon, hit, valid);
    }

    @Test
    void nothingRefractsOnceTheFiringShipIsGoneButFalloffStillApplies() {
        when(ship.isAlive()).thenReturn(false);
        ShipAPI hit = enemy(new Vector2f(0f, 0f));
        stubShips(ship, hit, enemy(new Vector2f(100f, 0f)));
        DamageAPI damage = damage(100f);

        listener.modifyDamageDealt(shot(energyGun(), new ChainLink(List.of(ship), 1, 0.8f)), hit, damage, new Vector2f(), true);

        verify(damage).setDamage(80f);
        verify(drones, never()).launch(any(), any(), any(), any(), any(), any());
    }

    @Test
    void weaponsThatCannotGoOnADroneNeverRefract() {
        factory.when(() -> WeaponDroneFactory.supportsProjectile(any())).thenReturn(false);
        ShipAPI hit = enemy(new Vector2f(0f, 0f));
        stubShips(ship, hit, enemy(new Vector2f(100f, 0f)));

        listener.modifyDamageDealt(shot(energyGun(), null), hit, damage(100f), new Vector2f(), true);

        verify(drones, never()).launch(any(), any(), any(), any(), any(), any());
    }
}
