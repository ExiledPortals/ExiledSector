package exiledlib.skills;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;

public enum CombatGate {

    LOW_HULL {
        @Override
        public boolean holds(ShipAPI ship) {
            return ship.getHullLevel() < LOW_HULL_LEVEL;
        }
    },
    SYSTEM_ACTIVE {
        @Override
        public boolean holds(ShipAPI ship) {
            ShipSystemAPI system = ship.getSystem();
            return system != null && system.isActive();
        }
    },
    PHASED {
        @Override
        public boolean holds(ShipAPI ship) {
            return ship.isPhased();
        }
    };

    public static final float LOW_HULL_LEVEL = 0.5f;

    public abstract boolean holds(ShipAPI ship);
}
