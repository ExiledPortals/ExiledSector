package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.NearbyDestructionListener;

import java.util.List;

final class WreckReclaimListener extends NearbyDestructionListener {

    static final String RESTORE_PERCENT_KEY = "exiledSector_nearbyDestructionReplacementRatePercent";
    static final float RANGE = 1500f;

    WreckReclaimListener(ShipAPI ownerShip) {
        super(ownerShip, other -> isEnemyShip(ownerShip, other));
    }

    static boolean isEnemyShip(ShipAPI ownerShip, ShipAPI other) {
        return !other.isFighter() && other.getParentStation() == null && CombatQueries.isHostile(ownerShip, other);
    }

    @Override
    protected boolean canReact() {
        return ownerShip.hasLaunchBays() && super.canReact();
    }

    @Override
    protected float activeRange() {
        return magnitude(RESTORE_PERCENT_KEY) > 0f ? RANGE : 0f;
    }

    @Override
    protected void onDestroyed(List<ShipAPI> wrecks) {
        restore(ownerShip.getLaunchBaysCopy(), wrecks.size() * magnitude(RESTORE_PERCENT_KEY) / 100f);
    }

    static void restore(List<FighterLaunchBayAPI> bays, float rateGain) {
        for (FighterLaunchBayAPI bay : bays) {
            bay.setCurrRate(Math.min(1f, bay.getCurrRate() + rateGain));
        }
    }
}
