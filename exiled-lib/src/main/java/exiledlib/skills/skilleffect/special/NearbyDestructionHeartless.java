package exiledlib.skills.skilleffect.special;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledlib.skills.skilleffect.NearbyWrecks;
import exiledlib.skills.skilleffect.ShipCombatListener;

final class NearbyDestructionHeartless extends ShipCombatListener implements AdvanceableListener {

    static final String RANGE_KEY = "exiledLib_heartlessNearbyDestructionRange";
    private static final float CHECK_SECONDS = 0.25f;

    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks wrecks;

    NearbyDestructionHeartless(ShipAPI ownerShip) {
        super(ownerShip);
        this.wrecks = new NearbyWrecks(ownerShip, other -> !other.isFighter() && other.getParentStation() == null);
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        float range = magnitude(RANGE_KEY);
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
