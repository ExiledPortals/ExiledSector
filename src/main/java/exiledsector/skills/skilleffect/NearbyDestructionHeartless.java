package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

final class NearbyDestructionHeartless implements AdvanceableListener {

    static final String RANGE_KEY = "exiledSector_heartlessNearbyDestructionRange";
    private static final float CHECK_SECONDS = 0.25f;

    private final ShipAPI ship;
    private final IntervalUtil interval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks wrecks;

    NearbyDestructionHeartless(ShipAPI ship) {
        this.ship = ship;
        this.wrecks = new NearbyWrecks(ship, other -> !other.isFighter() && other.getParentStation() == null);
    }

    @Override
    public void advance(float amount) {
        if (!ship.isAlive() || ship.isHulk()) {
            return;
        }
        interval.advance(amount);
        if (!interval.intervalElapsed()) {
            return;
        }
        float range = ship.getMutableStats().getDynamic().getValue(RANGE_KEY, 0f);
        if (range <= 0f) {
            return;
        }
        int destroyed = wrecks.newWithin(range).size();
        if (destroyed > 0) {
            HeartlessStacks stacks = HeartlessStacks.of(ship);
            for (int i = 0; i < destroyed; i++) {
                stacks.gain();
            }
        }
    }
}
