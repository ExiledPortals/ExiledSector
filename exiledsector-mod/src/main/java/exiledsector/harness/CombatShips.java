package exiledsector.harness;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

final class CombatShips {

    static final String PLAYER = "player";
    static final String ENEMY = "enemy";
    static final int PLAYER_SIDE = 0;
    static final int ENEMY_SIDE = 1;

    private CombatShips() {
    }

    static ShipAPI find(CombatEngineAPI engine, String selector) {
        if (PLAYER.equalsIgnoreCase(selector)) {
            return engine.getPlayerShip();
        }
        boolean firstEnemy = ENEMY.equalsIgnoreCase(selector);
        for (ShipAPI ship : engine.getShips()) {
            if (!fighting(ship)) {
                continue;
            }
            if (firstEnemy ? ship.getOwner() == ENEMY_SIDE : matches(ship, selector)) {
                return ship;
            }
        }
        return null;
    }

    static boolean anyEnemyFighting(CombatEngineAPI engine) {
        for (ShipAPI ship : engine.getShips()) {
            if (fighting(ship) && ship.getOwner() == ENEMY_SIDE) {
                return true;
            }
        }
        return false;
    }

    static String describe(ShipAPI ship) {
        return ship.getName() + " (" + ship.getHullSpec().getHullId() + ")";
    }

    private static boolean fighting(ShipAPI ship) {
        return ship.isAlive() && !ship.isHulk() && !ship.isFighter();
    }

    private static boolean matches(ShipAPI ship, String selector) {
        return selector.equals(ship.getHullSpec().getHullId()) || selector.equals(ship.getHullSpec().getBaseHullId())
                || selector.equalsIgnoreCase(ship.getName())
                || (ship.getVariant() != null && selector.equals(ship.getVariant().getHullVariantId()));
    }
}
