package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.combat.systems.EmpArcEntity;
import org.lwjgl.util.vector.Vector2f;
import org.objenesis.ObjenesisStd;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class EngineArcs {

    static final class LandingArc extends EmpArcEntity {

        private Vector2f landing;
        private boolean shieldHit;

        LandingArc() {
            super(null, null, null, null, null, 0f, 0f, 0f, null, 0f, null, null, false, null);
        }

        @Override
        public Vector2f getTargetLocation() {
            return landing;
        }

        @Override
        public boolean isShieldHit() {
            return shieldHit;
        }
    }

    private EngineArcs() {
    }

    public static EmpArcEntityAPI startingAt(CombatEntityAPI anchor, Vector2f offset) {
        return landingAt(anchor, offset, null);
    }

    public static EmpArcEntityAPI landingAt(CombatEntityAPI anchor, Vector2f offset, Vector2f landing) {
        LandingArc arc = new ObjenesisStd().newInstance(LandingArc.class);
        arc.landing = landing;
        ArcWeapons.ARC_ANCHOR.write(arc, anchor);
        ArcWeapons.ARC_OFFSET.write(arc, offset);
        return arc;
    }

    public static EmpArcEntityAPI landingOn(Vector2f landing, boolean shieldHit) {
        LandingArc arc = new ObjenesisStd().newInstance(LandingArc.class);
        arc.landing = landing;
        arc.shieldHit = shieldHit;
        return arc;
    }

    public static WeaponAPI scriptedWeapon(WeaponSlotAPI mount, Vector2f mountLocation, Vector2f fireOffset) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(spec.getTurretFireOffsets()).thenReturn(List.of(fireOffset));
        when(mount.getLocation()).thenReturn(mountLocation);
        when(weapon.getSlot()).thenReturn(mount);
        when(weapon.getSpec()).thenReturn(spec);
        when(weapon.getEffectPlugin()).thenReturn(mock(EveryFrameWeaponEffectPlugin.class));
        return weapon;
    }
}
