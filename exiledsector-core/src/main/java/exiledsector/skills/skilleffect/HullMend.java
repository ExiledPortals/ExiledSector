package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

public final class HullMend {

    private HullMend() {
    }

    public static float heal(ShipAPI ship, float percentPerSecond, float amount, float budget) {
        float maxHitpoints = ship.getMaxHitpoints();
        float hitpoints = ship.getHitpoints();
        float healed = Math.min(maxHitpoints * percentPerSecond / 100f * amount, Math.min(maxHitpoints - hitpoints, budget));
        ship.setHitpoints(hitpoints + healed);
        return healed;
    }
}
