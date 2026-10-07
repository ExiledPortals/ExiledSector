package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

final class NearbyDestructionHeartless implements AdvanceableListener {

    static final String RANGE_KEY = "exiledSector_heartlessNearbyDestructionRange";
    private static final float CHECK_SECONDS = 0.25f;

    private final ShipAPI ownerShip;
    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks wrecks;

    NearbyDestructionHeartless(ShipAPI ownerShip) {
        this.ownerShip = ownerShip;
        this.wrecks = new NearbyWrecks(ownerShip, other -> !other.isFighter() && other.getParentStation() == null);
    }

    @Override
    public void advance(float amount) {
        if (!ownerShip.isAlive() || ownerShip.isHulk()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        float range = ownerShip.getMutableStats().getDynamic().getValue(RANGE_KEY, 0f);
        if (range <= 0f) {
            return;
        }
        int destroyed = wrecks.newWithin(range).size();
        if (destroyed > 0) {
            HeartlessStacks stacks = HeartlessStacks.of(ownerShip);
            for (int i = 0; i < destroyed; i++) {
                stacks.gain();
            }
        }
    }
}
