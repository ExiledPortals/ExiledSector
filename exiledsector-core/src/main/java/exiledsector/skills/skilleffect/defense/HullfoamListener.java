package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.DeploymentRepairTally;
import exiledsector.skills.skilleffect.HullMend;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class HullfoamListener extends ShipCombatListener implements AdvanceableListener {

    static final String REGEN_PERCENT_KEY = "exiledSector_hullfoamRegenPercent";
    static final String POOL_PERCENT_KEY = "exiledSector_hullfoamPoolPercent";
    static final float EMPTY_POOL_HULL_LEVEL_PERCENT = 50f;
    private static final String MOD_ID_PREFIX = "exiledSector_hullfoam_";
    private static final String STATUS_ICON = "graphics/icons/hullsys/damper_field.png";

    private final DeploymentRepairTally foamUsed = new DeploymentRepairTally();
    private final PlayerStatus status;
    private int shownPercentLeft = -1;

    HullfoamListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        status = new PlayerStatus(modId, STATUS_ICON, "combat.hullfoam");
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || !ownerIsAliveNotHulk()) {
            return;
        }
        float pool = ownerShip.getHullSpec().getHitpoints() * magnitude(POOL_PERCENT_KEY) / 100f;
        float regenPercent = magnitude(REGEN_PERCENT_KEY);
        if (pool <= 0f || regenPercent <= 0f) {
            return;
        }
        float foamLeft = foamUsed.remainingBelow(pool);
        if (foamLeft > 0f) {
            float usedShare = foamUsed.repaired() / pool;
            float hullCap = ownerShip.getMaxHitpoints() * repairCapLevel(usedShare);
            float missing = hullCap - ownerShip.getHitpoints();
            if (missing > 0f) {
                foamUsed.record(HullMend.heal(ownerShip, regenPercent, amount, Math.min(missing, foamLeft)));
                foamLeft = foamUsed.remainingBelow(pool);
            }
        }
        showStatus(Math.max(0f, foamLeft) / pool);
    }

    static float repairCapLevel(float usedShare) {
        return 1f - EMPTY_POOL_HULL_LEVEL_PERCENT / 100f * Math.min(1f, Math.max(0f, usedShare));
    }

    private void showStatus(float shareLeft) {
        int percentLeft = (int) Math.ceil(shareLeft * 100f);
        if (percentLeft != shownPercentLeft) {
            shownPercentLeft = percentLeft;
            status.invalidate();
        }
        status.showIfPlayer(ownerShip, percentLeft <= 0, () -> percentLeft <= 0 ? Translation.text("combat.hullfoam.empty")
                : Translation.msg("combat.hullfoam.status").arg("value", percentLeft).text());
    }
}
