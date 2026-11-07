package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.List;
import java.util.function.Predicate;

public abstract class NearbyDestructionListener extends ShipCombatListener implements AdvanceableListener {

    private static final float CHECK_SECONDS = 0.25f;

    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks nearbyWrecks;

    protected NearbyDestructionListener(ShipAPI ownerShip, Predicate<ShipAPI> wreckFilter) {
        super(ownerShip);
        this.nearbyWrecks = new NearbyWrecks(ownerShip, wreckFilter);
    }

    @Override
    public final void advance(float amount) {
        if (!canReact()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        float range = activeRange();
        if (range <= 0f) {
            return;
        }
        List<ShipAPI> destroyed = nearbyWrecks.newWithin(range);
        if (!destroyed.isEmpty()) {
            onDestroyed(destroyed);
        }
    }

    protected boolean canReact() {
        return ownerIsAliveNotHulk();
    }

    protected abstract float activeRange();

    protected abstract void onDestroyed(List<ShipAPI> wrecks);
}
