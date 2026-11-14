package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ShipDestructionWatch extends BaseEveryFrameCombatPlugin {

    static final float CHECK_INTERVAL_SECONDS = 0.25f;
    static final String CUSTOM_DATA_KEY = "exiledSector_shipDestructionWatch";

    public interface Subscriber {
        ShipAPI watchingShip();

        void onShipsDestroyed(List<DestroyedShip> destroyedShips);
    }

    public record DestroyedShip(ShipAPI wreck, int ownerWhileAlive) {

        public boolean wasHostileTo(ShipAPI ship) {
            return CombatQueries.isHostileOwner(ship, ownerWhileAlive);
        }
    }

    private final List<Subscriber> subscribers = new ArrayList<>();
    private Map<ShipAPI, Integer> ownersOfLiveShips = new IdentityHashMap<>();
    private Map<ShipAPI, Integer> ownersSeenAliveThisCheck = new IdentityHashMap<>();
    private float secondsSinceCheck;

    public static void subscribe(Subscriber subscriber) {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        ShipDestructionWatch watch;
        if (engine.getCustomData().get(CUSTOM_DATA_KEY) instanceof ShipDestructionWatch existing) {
            watch = existing;
        } else {
            watch = new ShipDestructionWatch();
            engine.getCustomData().put(CUSTOM_DATA_KEY, watch);
            engine.addPlugin(watch);
        }
        watch.subscribers.add(subscriber);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        secondsSinceCheck += amount;
        if (secondsSinceCheck < CHECK_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceCheck = 0f;
        List<DestroyedShip> destroyedShips = destroyedSinceLastCheck(engine.getShips());
        if (!destroyedShips.isEmpty()) {
            notifySubscribers(destroyedShips);
        }
    }

    private List<DestroyedShip> destroyedSinceLastCheck(List<ShipAPI> ships) {
        List<DestroyedShip> destroyedShips = new ArrayList<>();
        for (ShipAPI ship : ships) {
            if (CombatQueries.isAfterimage(ship)) {
                continue;
            }
            if (CombatQueries.isAliveNotHulk(ship)) {
                ownersSeenAliveThisCheck.put(ship, ship.getOwner());
                continue;
            }
            Integer ownerWhileAlive = ownersOfLiveShips.get(ship);
            if (ownerWhileAlive != null && isWreck(ship)) {
                destroyedShips.add(new DestroyedShip(ship, ownerWhileAlive));
            }
        }
        Map<ShipAPI, Integer> previousOwners = ownersOfLiveShips;
        ownersOfLiveShips = ownersSeenAliveThisCheck;
        ownersSeenAliveThisCheck = previousOwners;
        ownersSeenAliveThisCheck.clear();
        return destroyedShips;
    }

    private void notifySubscribers(List<DestroyedShip> destroyedShips) {
        int subscriberCount = subscribers.size();
        for (int index = 0; index < subscriberCount; index++) {
            Subscriber subscriber = subscribers.get(index);
            if (ownersOfLiveShips.containsKey(subscriber.watchingShip())) {
                subscriber.onShipsDestroyed(destroyedShips);
            }
        }
    }

    private static boolean isWreck(ShipAPI ship) {
        return ship.isHulk() || ship.getHitpoints() <= 0f;
    }
}
