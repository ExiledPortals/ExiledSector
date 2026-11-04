package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.BattleRepairRecord;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class HighFluxMendingListener extends ShipCombatListener implements AdvanceableListener {

    static final String REGEN_PERCENT_KEY = "exiledSector_highFluxHullRegenPercent";
    static final float FLUX_LEVEL_THRESHOLD_PERCENT = 50f;
    static final float MAX_TOTAL_REGEN_PERCENT_OF_HULL = 20f;
    static final String MENDED_KEY_PREFIX = "exiledSector_highFluxHullMended_";

    private BattleRepairRecord mendRecord;

    HighFluxMendingListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || !ownerIsAliveNotHulk()) {
            return;
        }
        float maxHitpoints = ownerShip.getMaxHitpoints();
        float hitpoints = ownerShip.getHitpoints();
        if (hitpoints >= maxHitpoints || ownerShip.getFluxTracker().getFluxLevel() * 100f <= FLUX_LEVEL_THRESHOLD_PERCENT) {
            return;
        }
        float regenPercent = magnitude(REGEN_PERCENT_KEY);
        BattleRepairRecord mended = regenPercent > 0f ? mendRecordForThisBattle() : null;
        if (mended == null) {
            return;
        }
        float remainingRegen = mended.remainingBelow(maxHitpoints * MAX_TOTAL_REGEN_PERCENT_OF_HULL / 100f);
        if (remainingRegen <= 0f) {
            return;
        }
        float healed = Math.min(maxHitpoints * regenPercent / 100f * amount, Math.min(maxHitpoints - hitpoints, remainingRegen));
        mended.record(healed);
        ownerShip.setHitpoints(hitpoints + healed);
    }

    private BattleRepairRecord mendRecordForThisBattle() {
        if (mendRecord == null) {
            CombatEngineAPI engine = Global.getCombatEngine();
            mendRecord = engine == null ? null : BattleRepairRecord.of(engine, ownerShip, MENDED_KEY_PREFIX);
        }
        return mendRecord;
    }
}
