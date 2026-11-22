package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.awt.Color;

final class HyperEngineListener extends ShipCombatListener implements AdvanceableListener {

    static final String SPEED_FLAT_KEY = "exiledSector_hyperEngineZeroFluxSpeedFlat";
    static final float FLUX_LEVEL_PERCENT = 5f;
    static final float TOP_SPEED_SHARE = 1f / 3f;
    private static final String MOD_ID_PREFIX = "exiledSector_hyperEngine_";
    private static final Color ENGINE_COLOR = new Color(27, 238, 178, 255);

    private float speedBonus = Float.NaN;
    private boolean boosted;

    HyperEngineListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        if (Float.isNaN(speedBonus)) {
            if (travelDriveIsOn()) {
                return;
            }
            speedBonus = speedBonus(magnitude(SPEED_FLAT_KEY), stats.getZeroFluxSpeedBoost().getModifiedValue(),
                    stats.getMaxSpeed().getModifiedValue());
        }
        boolean boost = speedBonus > 0f && ownerIsAliveNotHulk() && ownerShip.getFluxTracker().isEngineBoostActive()
                && ownerShip.getFluxTracker().getFluxLevel() * 100f <= FLUX_LEVEL_PERCENT;
        if (boost != boosted) {
            boosted = boost;
            if (boost) {
                stats.getZeroFluxSpeedBoost().modifyFlat(modId, speedBonus);
            } else {
                stats.getZeroFluxSpeedBoost().unmodify(modId);
            }
        }
        if (boost) {
            ownerShip.getEngineController().fadeToOtherColor(this, ENGINE_COLOR, null, 1f, 0.6f);
            ownerShip.getEngineController().extendFlame(this, 0.4f, 0.4f, 0.4f);
        }
    }

    private boolean travelDriveIsOn() {
        ShipSystemAPI travelDrive = ownerShip.getTravelDrive();
        return travelDrive != null && travelDrive.isOn();
    }

    static float speedBonus(float baseBonus, float zeroFluxSpeedBoost, float topSpeed) {
        return Math.max(0f, baseBonus + zeroFluxSpeedBoost - topSpeed * TOP_SPEED_SHARE);
    }
}
