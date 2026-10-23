package exiledlib.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.ScaledBonus;
import exiledlib.skills.skilleffect.ShipCombatListener;
import exiledlib.skills.skilleffect.StatTarget;
import exiledlib.skills.skilleffect.movement.Maneuverability;
import org.lwjgl.util.vector.Vector2f;

final class EscortListener extends ShipCombatListener implements AdvanceableListener {

    static final String MANEUVER_BONUS_KEY = "exiledSector_escortManeuverBonusPercent";
    static final String SPEED_BONUS_KEY = "exiledSector_escortSpeedBonusPercent";
    static final String WEAPON_RANGE_BONUS_KEY = "exiledSector_escortWeaponRangeBonusPercent";
    private static final ScaledBonus ESCORT_BONUS = new ScaledBonus(
            ScaledBonus.percent(Maneuverability.target(), MANEUVER_BONUS_KEY),
            ScaledBonus.percent(StatTarget.liveStat(MutableShipStatsAPI::getMaxSpeed), SPEED_BONUS_KEY),
            ScaledBonus.percent(StatTarget.all(StatTarget.liveBonus(MutableShipStatsAPI::getBallisticWeaponRangeBonus),
                    StatTarget.liveBonus(MutableShipStatsAPI::getEnergyWeaponRangeBonus)), WEAPON_RANGE_BONUS_KEY));

    static final float ESCORT_RANGE = 700f;
    static final float PROXIMITY_FADE_DISTANCE = 500f;
    private static final float SHIELD_RADIUS_OVERLAP_MULT = 0.75f;
    private static final float DESTROYER_ESCORTING_CAPITAL_MULT = 2f;
    private static final float SEARCH_MARGIN = 300f;
    private static final float EASE_SECONDS = 0.5f;
    private static final float MAG_SNAP = 0.001f;
    private static final String ESCORT_BONUS_MOD_ID = "exiledSector_escortBonus";

    private final IntervalUtil retargetInterval = new IntervalUtil(0.9f, 1.1f);
    private float targetMag;
    private float appliedMag;
    private float easeRate;

    EscortListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        retargetInterval.advance(amount);
        boolean retargeted = retargetInterval.intervalElapsed();
        if (retargeted) {
            targetMag = proximityMagnitude();
            easeRate = Math.abs(targetMag - appliedMag) / EASE_SECONDS;
        }
        if (retargeted || appliedMag != targetMag) {
            appliedMag = approach(appliedMag, targetMag, easeRate * amount);
            ESCORT_BONUS.apply(ownerShip.getMutableStats(), ESCORT_BONUS_MOD_ID, appliedMag);
        }
    }

    private static float approach(float current, float target, float maxStep) {
        if (Math.abs(target - current) <= maxStep + MAG_SNAP) {
            return target;
        }
        return current + Math.signum(target - current) * maxStep;
    }

    private float proximityMagnitude() {
        float searchRadius = ESCORT_RANGE + PROXIMITY_FADE_DISTANCE + ownerShip.getCollisionRadius() + SEARCH_MARGIN;
        float best = 0f;
        for (ShipAPI escorted : CombatQueries.shipsNear(ownerShip.getLocation(), searchRadius, this::isLargerFriendly)) {
            best = Math.max(best, magnitudeFor(escorted));
        }
        return best;
    }

    private boolean isLargerFriendly(ShipAPI candidate) {
        return candidate != ownerShip && candidate.getOwner() == ownerShip.getOwner() && CombatQueries.isAliveNotHulk(candidate)
                && candidate.getHullSize().ordinal() > ownerShip.getHullSize().ordinal();
    }

    private float magnitudeFor(ShipAPI escorted) {
        float radiusOverlap = (ownerShip.getShieldRadiusEvenIfNoShield() + escorted.getShieldRadiusEvenIfNoShield())
                * SHIELD_RADIUS_OVERLAP_MULT;
        float distance = Vector2f.sub(ownerShip.getShieldCenterEvenIfNoShield(),
                escorted.getShieldCenterEvenIfNoShield(), null).length() - radiusOverlap;

        float mag;
        if (distance < ESCORT_RANGE) {
            mag = 1f;
        } else if (distance < ESCORT_RANGE + PROXIMITY_FADE_DISTANCE) {
            mag = 1f - (distance - ESCORT_RANGE) / PROXIMITY_FADE_DISTANCE;
        } else {
            mag = 0f;
        }

        if (ownerShip.isDestroyer() && escorted.isCapital()) {
            mag *= DESTROYER_ESCORTING_CAPITAL_MULT;
        }
        return mag;
    }
}
