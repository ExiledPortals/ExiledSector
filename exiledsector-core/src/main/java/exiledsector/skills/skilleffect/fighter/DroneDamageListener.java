package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

final class DroneDamageListener implements DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledLib_strikecraftDamageToDronesPercent";
    static final String DAMAGE_MOD_ID = "exiledLib_strikecraftDamageToDrones";

    private final ShipAPI fighter;
    private final ShipAPI carrier;

    DroneDamageListener(ShipAPI fighter, ShipAPI carrier) {
        this.fighter = fighter;
        this.carrier = carrier;
    }

    static boolean isDrone(ShipAPI target) {
        if (!target.isFighter()) {
            return false;
        }
        ShipHullSpecAPI hullSpec = target.getHullSpec();
        return hullSpec != null && hullSpec.getMinCrew() <= 0f;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || !isDrone(targetShip) || !CombatQueries.isHostile(fighter, targetShip)) {
            return null;
        }
        float percent = FighterListenerEffect.carrierValue(carrier, DAMAGE_PERCENT_KEY);
        if (percent <= 0f) {
            return null;
        }
        damage.getModifier().modifyPercent(DAMAGE_MOD_ID, percent);
        return DAMAGE_MOD_ID;
    }
}
