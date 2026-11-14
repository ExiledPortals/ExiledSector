package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ShipDestructionWatch.DestroyedShip;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public abstract class NearbyDestructionListener extends ShipCombatListener implements ShipDestructionWatch.Subscriber {

    private final Predicate<DestroyedShip> wreckFilter;

    protected NearbyDestructionListener(ShipAPI ownerShip, Predicate<DestroyedShip> wreckFilter) {
        super(ownerShip);
        this.wreckFilter = wreckFilter;
        ShipDestructionWatch.subscribe(this);
    }

    @Override
    public final ShipAPI watchingShip() {
        return ownerShip;
    }

    @Override
    public final void onShipsDestroyed(List<DestroyedShip> destroyedShips) {
        if (!canReact()) {
            return;
        }
        float range = activeRange();
        if (range <= 0f) {
            return;
        }
        List<ShipAPI> wrecksInRange = new ArrayList<>();
        for (DestroyedShip destroyed : destroyedShips) {
            ShipAPI wreck = destroyed.wreck();
            if (wreck != ownerShip && wreckFilter.test(destroyed)
                    && CombatQueries.withinRadius(wreck.getLocation(), ownerShip.getLocation(), range)) {
                wrecksInRange.add(wreck);
            }
        }
        if (!wrecksInRange.isEmpty()) {
            onDestroyed(wrecksInRange);
        }
    }

    protected boolean canReact() {
        return ownerIsAliveNotHulk();
    }

    protected abstract float activeRange();

    protected abstract void onDestroyed(List<ShipAPI> wrecks);
}
