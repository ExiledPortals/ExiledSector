package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArcWeaponsTest {

    @RegisterExtension
    final CombatEngineExtension combat = new CombatEngineExtension();
    private ShipAPI ownerShip;
    private WeaponAPI arcEmitter;

    @BeforeEach
    void setUp() {
        ownerShip = CombatTestShips.ship().owner(0).at(new Vector2f(500f, 500f)).build();
        when(ownerShip.getFacing()).thenReturn(90f);
        arcEmitter = EngineArcs.scriptedWeapon(mock(WeaponSlotAPI.class), new Vector2f(40f, 10f), new Vector2f(15f, 0f));
        WeaponAPI plainGun = mock(WeaponAPI.class);
        WeaponSlotAPI plainMount = mock(WeaponSlotAPI.class);
        when(plainMount.getLocation()).thenReturn(new Vector2f(40f, 10f));
        when(plainGun.getSlot()).thenReturn(plainMount);
        when(ownerShip.getAllWeapons()).thenReturn(List.of(plainGun, arcEmitter));
    }

    private static CombatEntityAPI entityAt(Vector2f location, float facing) {
        CombatEntityAPI entity = mock(CombatEntityAPI.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getFacing()).thenReturn(facing);
        return entity;
    }

    private static Vector2f placedFrom(CombatEntityAPI anchor, Vector2f offset) {
        double radians = Math.toRadians(anchor.getFacing());
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        return new Vector2f(anchor.getLocation().x + offset.x * cos - offset.y * sin, anchor.getLocation().y + offset.x * sin + offset.y * cos);
    }

    private void elapsed(float seconds) {
        when(combat.engine().getTotalElapsedTime(false)).thenReturn(seconds);
    }

    @Test
    void anArcOffsetIsInTheAnchorsRotatedFrameAsTheEngineStoresIt() {
        CombatEntityAPI anchor = entityAt(new Vector2f(100f, 200f), 90f);
        Vector2f worldStart = new Vector2f(97f, 230f);

        Vector2f offset = ArcWeapons.inFrameOf(anchor, worldStart);
        Vector2f placed = placedFrom(anchor, offset);

        assertEquals(30f, offset.x, 0.001f);
        assertEquals(3f, offset.y, 0.001f);
        assertEquals(worldStart.x, placed.x, 0.001f);
        assertEquals(worldStart.y, placed.y, 0.001f);
    }

    @Test
    void anArcFromThisShipBelongsToTheScriptedWeaponWhoseFirePointItStartsFrom() {
        ArcWeapons arcWeapons = new ArcWeapons(ownerShip);

        assertSame(arcEmitter, arcWeapons.weaponBehind(EngineArcs.startingAt(ownerShip, new Vector2f(50f, 20f)), mock(ShipAPI.class)));
        assertNull(arcWeapons.weaponBehind(EngineArcs.startingAt(ownerShip, new Vector2f(-60f, 0f)), mock(ShipAPI.class)));
    }

    @Test
    void anArcFromAnotherSidesShipIsNotOurs() {
        ShipAPI enemy = CombatTestShips.ship().owner(1).at(new Vector2f()).build();
        when(enemy.getAllWeapons()).thenReturn(List.of(arcEmitter));

        assertNull(new ArcWeapons(ownerShip).weaponBehind(EngineArcs.startingAt(enemy, new Vector2f(40f, 10f)), mock(ShipAPI.class)));
    }

    @Test
    void aChainedArcFromTheCentreOfARecentlyArcedTargetBelongsToTheSameWeapon() {
        ArcWeapons arcWeapons = new ArcWeapons(ownerShip);
        MissileAPI firstTarget = mock(MissileAPI.class);
        when(firstTarget.getLocation()).thenReturn(new Vector2f(900f, 900f));
        elapsed(10f);
        arcWeapons.weaponBehind(EngineArcs.startingAt(ownerShip, new Vector2f(40f, 10f)), firstTarget);

        elapsed(10.5f);
        assertSame(arcEmitter, arcWeapons.weaponBehind(EngineArcs.startingAt(firstTarget, new Vector2f()), mock(ShipAPI.class)));
        elapsed(10f + ArcWeapons.CHAIN_MEMORY_SECONDS + 0.1f);
        assertNull(arcWeapons.weaponBehind(EngineArcs.startingAt(firstTarget, new Vector2f()), mock(ShipAPI.class)));
    }

    @Test
    void aChainedArcMayAlsoStartWhereThePreviousArcLanded() {
        ArcWeapons arcWeapons = new ArcWeapons(ownerShip);
        CombatEntityAPI firstTarget = entityAt(new Vector2f(1000f, 0f), 180f);
        elapsed(3f);
        arcWeapons.weaponBehind(EngineArcs.landingAt(ownerShip, new Vector2f(40f, 10f), new Vector2f(950f, 0f)), firstTarget);

        assertSame(arcEmitter, arcWeapons.weaponBehind(EngineArcs.startingAt(firstTarget, new Vector2f(50f, 0f)), mock(ShipAPI.class)));
        assertNull(arcWeapons.weaponBehind(EngineArcs.startingAt(firstTarget, new Vector2f(0f, 60f)), mock(ShipAPI.class)));
    }

    @Test
    void anArcAnchoredOnATargetNoArcOfOursHitIsLeftAlone() {
        CombatEntityAPI struck = entityAt(new Vector2f(), 0f);

        assertNull(new ArcWeapons(ownerShip).weaponBehind(EngineArcs.startingAt(struck, new Vector2f()), mock(ShipAPI.class)));
    }

    @Test
    void projectilesKeepTheirOwnWeaponAndArcsTheEngineDidNotBuildAreLeftAlone() {
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        WeaponAPI gun = mock(WeaponAPI.class);
        when(shot.getWeapon()).thenReturn(gun);
        ArcWeapons arcWeapons = new ArcWeapons(ownerShip);

        assertSame(gun, arcWeapons.weaponBehind(shot, mock(ShipAPI.class)));
        assertNull(arcWeapons.weaponBehind(mock(EmpArcEntityAPI.class), mock(ShipAPI.class)));
    }

    @Test
    void onlyWeaponsWithAFireOrEveryFrameScriptCanSpawnArcs() {
        WeaponAPI plainGun = mock(WeaponAPI.class);

        assertTrue(ArcWeapons.isScripted(arcEmitter));
        assertFalse(ArcWeapons.isScripted(plainGun));
    }

    @Test
    void theEngineArcFieldsAreReachable() {
        assertTrue(ArcWeapons.ARC_ANCHOR.reachable());
        assertTrue(ArcWeapons.ARC_OFFSET.reachable());
    }
}
