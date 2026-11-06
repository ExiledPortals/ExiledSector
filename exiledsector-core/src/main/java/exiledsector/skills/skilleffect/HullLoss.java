package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public final class HullLoss {

    public static final float KILLING_BLOW_MULTIPLE = 3f;
    public static final float KILLING_BLOW_HULL_LEFT = 1f;

    private HullLoss() {
    }

    public static void take(ShipAPI ship, float hullDamage, ShipAPI attacker, CombatEngineAPI engine) {
        if (ship.getHitpoints() > hullDamage) {
            ship.setHitpoints(ship.getHitpoints() - hullDamage);
            return;
        }
        if (engine == null) {
            return;
        }
        ship.setHitpoints(Math.min(ship.getHitpoints(), KILLING_BLOW_HULL_LEFT));
        engine.applyDamage(ship, new Vector2f(ship.getLocation()), killingBlowDamage(ship), DamageType.ENERGY, 0f, true, false, attacker);
    }

    public static float killingBlowDamage(ShipAPI ship) {
        ArmorGridAPI armor = ship.getArmorGrid();
        MutableShipStatsAPI stats = ship.getMutableStats();
        float armorRating = armor == null ? 0f : armor.getArmorRating() / resistanceOnly(stats.getArmorDamageTakenMult().getModifiedValue());
        float damageTakenMult = resistanceOnly(stats.getHullDamageTakenMult().getModifiedValue())
                * resistanceOnly(stats.getEnergyDamageTakenMult().getModifiedValue())
                * resistanceOnly(stats.getProjectileDamageTakenMult().getModifiedValue());
        return KILLING_BLOW_MULTIPLE * (Math.max(0f, ship.getHitpoints()) + armorRating) / damageTakenMult;
    }

    private static float resistanceOnly(float damageTakenMult) {
        return damageTakenMult > 0f && damageTakenMult < 1f ? damageTakenMult : 1f;
    }
}
