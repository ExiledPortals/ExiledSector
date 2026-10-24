package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class SplitBeamDrones {

    private static final Logger LOG = Logger.getLogger(SplitBeamDrones.class);
    private static final float SPLIT_TIMEOUT_SECONDS = 0.3f;
    private static final float MIN_FIRING_SECONDS = 1f;
    private static final float CONNECTOR_OVERLAP = 20f;
    private static final float STAT_MIRROR_INTERVAL_SECONDS = 0.25f;
    private static final String SHARE_MOD_ID = "exiledSector_splitBeamDroneShare";

    private final ShipAPI firingShip;
    private final Map<SplitKey, SplitDrone> drones = new HashMap<>();
    private final Map<BeamAPI, BeamClaim> claimsByBeam = new HashMap<>();

    SplitBeamDrones(ShipAPI firingShip) {
        this.firingShip = firingShip;
    }

    void refresh(WeaponAPI weapon, ShipAPI primaryTarget, ShipAPI splitTarget, Vector2f impactPoint, float share) {
        SplitKey key = new SplitKey(weapon, splitTarget);
        SplitDrone split = drones.get(key);
        if (split == null || !split.isInPlay()) {
            split = start(key, weapon);
            if (split == null) {
                return;
            }
            drones.put(key, split);
        }
        split.retarget(impactPoint, Refraction.origin(primaryTarget, impactPoint, splitTarget.getLocation()), share);
    }

    void claim(BeamAPI beam, List<ShipAPI> splitTargets) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || splitTargets.isEmpty() && drones.isEmpty()) {
            claimsByBeam.remove(beam);
            return;
        }
        float now = engine.getTotalElapsedTime(false);
        WeaponAPI weapon = beam.getWeapon();
        BeamClaim claim = claimsByBeam.computeIfAbsent(beam, key -> new BeamClaim());
        claim.weapon = weapon;
        claim.splitTargets = List.copyOf(splitTargets);
        claim.claimedAt = now;
        Iterator<BeamClaim> claims = claimsByBeam.values().iterator();
        while (claims.hasNext()) {
            if (now - claims.next().claimedAt > SPLIT_TIMEOUT_SECONDS) {
                claims.remove();
            }
        }
        for (SplitDrone split : drones.values()) {
            if (split.splitKey.weapon() == weapon && !isClaimed(weapon, split.splitTarget)) {
                split.stopSharing();
            }
        }
    }

    private boolean isClaimed(WeaponAPI weapon, ShipAPI splitTarget) {
        for (BeamClaim claim : claimsByBeam.values()) {
            if (claim.weapon == weapon && claim.splitTargets.contains(splitTarget)) {
                return true;
            }
        }
        return false;
    }

    private SplitDrone start(SplitKey key, WeaponAPI weapon) {
        ShareListener shareListener = new ShareListener();
        ShipAPI droneShip = WeaponDroneShell.buildOrMarkUnsupported(() -> WeaponDroneFactory.create(firingShip, weapon, shareListener),
                weapon, weapon.getId(), WeaponDroneFactory::markBeamUnsupported, LOG, "Beam splitting", "splits without a drone");
        if (droneShip == null) {
            return null;
        }
        SplitDrone split = new SplitDrone(key, droneShip, shareListener);
        WeaponDroneStats.mirrorTimeMult(firingShip.getMutableStats(), droneShip.getMutableStats());
        split.droneShip.addListener(split);
        return split;
    }

    private record SplitKey(WeaponAPI weapon, ShipAPI splitTarget) {
    }

    private static final class BeamClaim {
        private WeaponAPI weapon;
        private List<ShipAPI> splitTargets = List.of();
        private float claimedAt;
    }

    private final class SplitDrone extends WeaponDroneShell {

        private final SplitKey splitKey;
        private final WeaponAPI droneWeapon;
        private final ShipAPI splitTarget;
        private final ShareListener shareListener;
        private final Vector2f impactPoint = new Vector2f();
        private final Vector2f origin = new Vector2f();
        private float secondsSinceRefresh;
        private float secondsSinceStart;
        private float secondsSinceMirror;

        private SplitDrone(SplitKey splitKey, ShipAPI droneShip, ShareListener shareListener) {
            super(droneShip);
            this.splitKey = splitKey;
            this.droneWeapon = droneShip.getAllWeapons().get(0);
            this.splitTarget = splitKey.splitTarget();
            this.shareListener = shareListener;
        }

        @Override
        ShipAPI firingShip() {
            return firingShip;
        }

        private void retarget(Vector2f newImpactPoint, Vector2f newOrigin, float share) {
            impactPoint.set(newImpactPoint);
            origin.set(newOrigin);
            shareListener.damageShare = share;
            secondsSinceRefresh = 0f;
        }

        private void stopSharing() {
            secondsSinceRefresh = Float.POSITIVE_INFINITY;
            shareListener.damageShare = 0f;
        }

        @Override
        void advanceState(float amount) {
            boolean firingShipGone = !firingShip.isAlive();
            if (!firingShipGone) {
                WeaponDroneStats.mirrorTimeMult(firingShip.getMutableStats(), droneShip.getMutableStats());
            }
            secondsSinceRefresh += amount;
            secondsSinceStart += amount;
            boolean sharing = secondsSinceRefresh <= SPLIT_TIMEOUT_SECONDS;
            if (!sharing) {
                shareListener.damageShare = 0f;
            }
            boolean withinSplitWindow = secondsSinceStart <= MIN_FIRING_SECONDS || sharing;
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

        @Override
        void forget() {
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
