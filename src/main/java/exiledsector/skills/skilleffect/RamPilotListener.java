package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

final class RamPilotListener extends ShipCombatListener implements AdvanceableListener {

    static final float DEFENSE_DROP_MARGIN = 300f;
    static final float THRUST_ARC_DEGREES = 90f;
    static final float AIM_DEAD_BAND_DEGREES = 1f;
    static final float MAX_LEAD_SECONDS = 3f;
    private static final float MIN_CLOSING_SPEED_FRACTION = 0.5f;
    private static final float MIN_TURN_ACCELERATION = 1f;
    private static final float FLAG_SECONDS = 0.5f;
    private static final ShipCommand[] ALWAYS_BLOCKED = {
            ShipCommand.ACCELERATE_BACKWARDS, ShipCommand.STRAFE_LEFT, ShipCommand.STRAFE_RIGHT};

    private final IntervalUtil retargetInterval = new IntervalUtil(0.9f, 1.1f);
    private ShipAPI rammedShip;
    private boolean hasSearched;

    RamPilotListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (!isFlownByAi()) {
            return;
        }
        retargetInterval.advance(amount);
        boolean targetLost = rammedShip != null && !CombatQueries.isAliveNotHulk(rammedShip);
        if (!hasSearched || targetLost || retargetInterval.intervalElapsed()) {
            hasSearched = true;
            rammedShip = CombatQueries.biggestEnemyShip(ownerShip);
        }
        if (rammedShip == null) {
            return;
        }
        focusAiOn(rammedShip);
        steerInto(rammedShip);
        dropDefensesNear(rammedShip);
    }

    private boolean isFlownByAi() {
        if (!ownerIsAliveNotHulk() || ownerShip.isRetreating() || ownerShip.isStation() || ownerShip.isStationModule()) {
            return false;
        }
        ShipSystemAPI travelDrive = ownerShip.getTravelDrive();
        if (travelDrive != null && travelDrive.isOn()) {
            return false;
        }
        return ownerShip != Global.getCombatEngine().getPlayerShip() || ownerShip.getShipAI() != null;
    }

    private void focusAiOn(ShipAPI target) {
        ownerShip.setShipTarget(target);
        ShipwideAIFlags flags = ownerShip.getAIFlags();
        if (flags != null) {
            flags.setFlag(AIFlags.MANEUVER_TARGET, FLAG_SECONDS, target);
            flags.setFlag(AIFlags.DO_NOT_BACK_OFF, FLAG_SECONDS);
        }
    }

    private void steerInto(ShipAPI target) {
        Vector2f aimPoint = interceptPoint(ownerShip.getLocation(), ownerShip.getVelocity(), ownerShip.getMaxSpeed(),
                target.getLocation(), target.getVelocity());
        float angleToAim = angleToAim(ownerShip.getLocation(), ownerShip.getFacing(), aimPoint);
        ShipCommand turn = turnCommand(angleToAim, ownerShip.getAngularVelocity(), ownerShip.getTurnAcceleration());
        ShipCommand throttle = Math.abs(angleToAim) <= THRUST_ARC_DEGREES ? ShipCommand.ACCELERATE : ShipCommand.DECELERATE;

        for (ShipCommand blocked : ALWAYS_BLOCKED) {
            ownerShip.blockCommandForOneFrame(blocked);
        }
        blockUnless(ShipCommand.TURN_LEFT, turn);
        blockUnless(ShipCommand.TURN_RIGHT, turn);
        blockUnless(ShipCommand.ACCELERATE, throttle);
        blockUnless(ShipCommand.DECELERATE, throttle);
        ownerShip.giveCommand(throttle, null, 0);
        if (turn != null) {
            ownerShip.giveCommand(turn, null, 0);
        }
    }

    private void blockUnless(ShipCommand command, ShipCommand given) {
        if (command != given) {
            ownerShip.blockCommandForOneFrame(command);
        }
    }

    private void dropDefensesNear(ShipAPI target) {
        float defenseDropRange = ownerShip.getCollisionRadius() + target.getCollisionRadius() + DEFENSE_DROP_MARGIN;
        if (!CombatQueries.withinRadius(ownerShip.getLocation(), target.getLocation(), defenseDropRange)) {
            return;
        }
        ShipSystemAPI phaseCloak = ownerShip.getPhaseCloak();
        if (phaseCloak != null) {
            holdPhaseUntilInside(phaseCloak, target);
            return;
        }
        ShieldAPI shield = ownerShip.getShield();
        if (shield == null) {
            return;
        }
        if (shield.isOn()) {
            ownerShip.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ownerShip.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
        }
    }

    private void holdPhaseUntilInside(ShipSystemAPI phaseCloak, ShipAPI target) {
        if (!ownerShip.isPhased() || phaseCloak.isChargedown()) {
            return;
        }
        float touchingRange = ownerShip.getCollisionRadius() + target.getCollisionRadius();
        if (CombatQueries.withinRadius(ownerShip.getLocation(), target.getLocation(), touchingRange)
                && CombatQueries.boundsOverlap(ownerShip, target)) {
            ownerShip.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
        } else {
            ownerShip.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
        }
    }

    static Vector2f interceptPoint(Vector2f ownLocation, Vector2f ownVelocity, float ownMaxSpeed,
                                   Vector2f targetLocation, Vector2f targetVelocity) {
        float offsetX = targetLocation.x - ownLocation.x;
        float offsetY = targetLocation.y - ownLocation.y;
        float distance = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY);
        if (distance < 1f) {
            return new Vector2f(targetLocation);
        }
        float closingSpeed = ((ownVelocity.x - targetVelocity.x) * offsetX + (ownVelocity.y - targetVelocity.y) * offsetY) / distance;
        float assumedClosingSpeed = Math.max(closingSpeed, Math.max(ownMaxSpeed * MIN_CLOSING_SPEED_FRACTION, 1f));
        float leadSeconds = Math.min(MAX_LEAD_SECONDS, distance / assumedClosingSpeed);
        return new Vector2f(targetLocation.x + targetVelocity.x * leadSeconds, targetLocation.y + targetVelocity.y * leadSeconds);
    }

    static float angleToAim(Vector2f ownLocation, float facing, Vector2f aimPoint) {
        float desiredFacing = (float) Math.toDegrees(Math.atan2(aimPoint.y - ownLocation.y, aimPoint.x - ownLocation.x));
        float rotation = (desiredFacing - facing) % 360f;
        if (rotation > 180f) {
            rotation -= 360f;
        } else if (rotation < -180f) {
            rotation += 360f;
        }
        return rotation;
    }

    static ShipCommand turnCommand(float angleToAim, float angularVelocity, float turnAcceleration) {
        float stoppingAngle = angularVelocity * Math.abs(angularVelocity) / (2f * Math.max(turnAcceleration, MIN_TURN_ACCELERATION));
        float remainingAngle = angleToAim - stoppingAngle;
        if (Math.abs(remainingAngle) < AIM_DEAD_BAND_DEGREES) {
            return null;
        }
        return remainingAngle > 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
    }
}
