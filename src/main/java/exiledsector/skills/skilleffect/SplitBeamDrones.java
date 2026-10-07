package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

final class SplitBeamDrones {

    private static final float SPLIT_TIMEOUT_SECONDS = 0.3f;
    private static final float MIN_FIRING_SECONDS = 1f;
    private static final float CONNECTOR_OVERLAP = 20f;
    private static final float STAT_MIRROR_INTERVAL_SECONDS = 0.25f;
    private static final String SHARE_MOD_ID = "exiledSector_splitBeamDroneShare";

    private final ShipAPI firingShip;
    private final Map<SplitKey, SplitDrone> drones = new HashMap<>();

    SplitBeamDrones(ShipAPI firingShip) {
        this.firingShip = firingShip;
    }

    void refresh(WeaponAPI weapon, ShipAPI primaryTarget, ShipAPI splitTarget, Vector2f impactPoint, float share) {
        SplitKey key = new SplitKey(weapon, splitTarget);
        SplitDrone split = drones.get(key);
        if (split == null || !split.isInPlay()) {
            split = start(key, weapon);
            drones.put(key, split);
        }
        split.retarget(impactPoint, Refraction.origin(primaryTarget, impactPoint, splitTarget.getLocation()), share);
    }

    private SplitDrone start(SplitKey key, WeaponAPI weapon) {
        ShareListener shareListener = new ShareListener();
        SplitDrone split = new SplitDrone(key, WeaponDroneFactory.create(firingShip, weapon, shareListener), shareListener);
        split.droneShip.addListener(split);
        return split;
    }

    private record SplitKey(WeaponAPI weapon, ShipAPI splitTarget) {
    }

    private final class SplitDrone implements AdvanceableListener {

        private final SplitKey splitKey;
        private final ShipAPI droneShip;
        private final WeaponAPI droneWeapon;
        private final ShipAPI splitTarget;
        private final ShareListener shareListener;
        private final Vector2f impactPoint = new Vector2f();
        private final Vector2f origin = new Vector2f();
        private float secondsSinceRefresh;
        private float secondsSinceStart;
        private float secondsSinceMirror;
        private boolean removed;
        private boolean markersMirrored;

        private SplitDrone(SplitKey splitKey, ShipAPI droneShip, ShareListener shareListener) {
            this.splitKey = splitKey;
            this.droneShip = droneShip;
            this.droneWeapon = droneShip.getAllWeapons().get(0);
            this.splitTarget = splitKey.splitTarget();
            this.shareListener = shareListener;
        }

        private boolean isInPlay() {
            return !removed && Global.getCombatEngine().isEntityInPlay(droneShip);
        }

        private void retarget(Vector2f newImpactPoint, Vector2f newOrigin, float share) {
            impactPoint.set(newImpactPoint);
            origin.set(newOrigin);
            shareListener.damageShare = share;
            secondsSinceRefresh = 0f;
        }

        @Override
        public void advance(float amount) {
            if (removed) {
                return;
            }
            if (!markersMirrored) {
                WeaponDroneFactory.mirrorMarkerHullMods(firingShip, droneShip);
                markersMirrored = true;
            }
            boolean firingShipGone = !firingShip.isAlive();
            float firingShipAmount = firingShipGone ? amount : amount * firingShip.getMutableStats().getTimeMult().getModifiedValue();
            secondsSinceRefresh += firingShipAmount;
            secondsSinceStart += firingShipAmount;
            boolean withinSplitWindow = secondsSinceStart <= MIN_FIRING_SECONDS || secondsSinceRefresh <= SPLIT_TIMEOUT_SECONDS;
            boolean firing = !firingShipGone && withinSplitWindow && splitTarget.isAlive();
            if (!firing && !droneWeapon.isFiring()) {
                remove();
                return;
            }
            secondsSinceMirror += amount;
            if (!firingShipGone && secondsSinceMirror >= STAT_MIRROR_INTERVAL_SECONDS) {
                WeaponDroneStats.mirror(firingShip.getMutableStats(), droneShip.getMutableStats());
                secondsSinceMirror = 0f;
            }
            float angle = VectorUtils.getAngle(origin, splitTarget.getLocation());
            droneShip.getLocation().set(origin);
            droneShip.setFacing(angle);
            droneWeapon.setForceFireOneFrame(firing);
            droneWeapon.setFacing(angle);
            droneWeapon.updateBeamFromPoints();
            drawRefractionConnector(amount, angle);
        }

        private void remove() {
            removed = true;
            Global.getCombatEngine().removeEntity(droneShip);
            drones.remove(splitKey, this);
        }

        private void drawRefractionConnector(float amount, float angle) {
            float gap = MathUtils.getDistance(impactPoint, origin);
            if (amount <= 0f || gap < 1f || droneWeapon.getBeams().isEmpty()) {
                return;
            }
            BeamAPI beam = droneWeapon.getBeams().get(0);
            float brightness = beam.getBrightness();
            if (brightness <= 0f) {
                return;
            }
            MagicFakeBeamPlugin.addBeam(0f, 0f, beam.getWidth(), new Vector2f(impactPoint), angle,
                    gap + CONNECTOR_OVERLAP, dimmed(beam.getCoreColor(), brightness), dimmed(beam.getFringeColor(), brightness));
        }

        private static Color dimmed(Color color, float brightness) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(color.getAlpha() * brightness));
        }
    }

    static final class ShareListener implements DamageDealtModifier {

        private float damageShare = 1f;

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!(param instanceof BeamAPI)) {
                return null;
            }
            damage.getModifier().modifyMult(SHARE_MOD_ID, damageShare);
            return SHARE_MOD_ID;
        }
    }
}
