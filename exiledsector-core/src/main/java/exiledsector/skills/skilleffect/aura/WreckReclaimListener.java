package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.NearbyWrecks;
import exiledsector.skills.skilleffect.ShipCombatListener;

import java.util.List;

final class WreckReclaimListener extends ShipCombatListener implements AdvanceableListener {

    static final String RESTORE_PERCENT_KEY = "exiledLib_nearbyDestructionReplacementRatePercent";
    static final float RANGE = 1500f;
    private static final float CHECK_SECONDS = 0.25f;

    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks wrecks;

    WreckReclaimListener(ShipAPI ownerShip) {
        super(ownerShip);
        this.wrecks = new NearbyWrecks(ownerShip, other -> isEnemyShip(ownerShip, other));
    }

    static boolean isEnemyShip(ShipAPI ownerShip, ShipAPI other) {
        return !other.isFighter() && other.getParentStation() == null && CombatQueries.isHostile(ownerShip, other);
    }

    @Override
    public void advance(float amount) {
        if (!ownerShip.hasLaunchBays() || !ownerIsAliveNotHulk()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        float percent = magnitude(RESTORE_PERCENT_KEY);
        if (percent <= 0f) {
            return;
        }
        int destroyed = wrecks.newWithin(RANGE).size();
        if (destroyed > 0) {
            restore(ownerShip.getLaunchBaysCopy(), destroyed * percent / 100f);
        }
    }

    static void restore(List<FighterLaunchBayAPI> bays, float rateGain) {
        for (FighterLaunchBayAPI bay : bays) {
            bay.setCurrRate(Math.min(1f, bay.getCurrRate() + rateGain));
        }
    }
}
