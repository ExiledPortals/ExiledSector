package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.persistence.CombatData;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class SwarmLinkCensus extends BaseEveryFrameCombatPlugin {

    static final float REFRESH_INTERVAL_SECONDS = 0.5f;
    private static final String CUSTOM_DATA_KEY = "exiledSector_swarmLinkCensus";

    private final Map<ShipAPI, SwarmLinkListener> listenersByFighter = new IdentityHashMap<>();
    private final Map<Integer, List<ShipAPI>> strikecraftByOwner = new HashMap<>();
    private final List<SwarmLinkListener> listenersInPlay = new ArrayList<>();
    private boolean newListenerWaiting;
    private float secondsSinceRefresh;

    static void register(SwarmLinkListener listener) {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        CombatData.getOrCreatePlugin(engine, CUSTOM_DATA_KEY, SwarmLinkCensus.class, SwarmLinkCensus::new).add(listener);
    }

    void add(SwarmLinkListener listener) {
        listenersByFighter.put(listener.fighter(), listener);
        newListenerWaiting = true;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        secondsSinceRefresh += amount;
        if (secondsSinceRefresh < REFRESH_INTERVAL_SECONDS && !newListenerWaiting) {
            return;
        }
        refresh(engine.getShips());
    }

    void refresh(List<ShipAPI> ships) {
        secondsSinceRefresh = 0f;
        newListenerWaiting = false;
        countStrikecraft(ships);
        for (SwarmLinkListener listener : listenersInPlay) {
            listener.refresh(this);
        }
        listenersInPlay.clear();
        listenersByFighter.values().removeIf(listener -> !CombatQueries.isAliveNotHulk(listener.fighter()));
    }

    private void countStrikecraft(List<ShipAPI> ships) {
        for (List<ShipAPI> strikecraft : strikecraftByOwner.values()) {
            strikecraft.clear();
        }
        for (ShipAPI ship : ships) {
            SwarmLinkListener listener = listenersByFighter.get(ship);
            if (listener != null) {
                listenersInPlay.add(listener);
            }
            if (isStrikecraft(ship)) {
                strikecraftByOwner.computeIfAbsent(ship.getOwner(), owner -> new ArrayList<>()).add(ship);
            }
        }
    }

    int strikecraftNear(ShipAPI fighter, float range, int countLimit) {
        List<ShipAPI> sameSide = strikecraftByOwner.get(fighter.getOwner());
        if (sameSide == null || countLimit <= 0) {
            return 0;
        }
        int count = 0;
        for (ShipAPI strikecraft : sameSide) {
            if (strikecraft != fighter && CombatQueries.withinRadius(strikecraft.getLocation(), fighter.getLocation(), range)
                    && ++count >= countLimit) {
                return count;
            }
        }
        return count;
    }

    private static boolean isStrikecraft(ShipAPI ship) {
        return ship.isFighter() && !ship.isDrone() && CombatQueries.isAliveNotHulk(ship) && !CombatQueries.isAfterimage(ship);
    }
}
