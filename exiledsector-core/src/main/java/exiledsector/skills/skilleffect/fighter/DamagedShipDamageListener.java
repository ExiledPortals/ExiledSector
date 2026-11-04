package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

final class DamagedShipDamageListener implements DamageDealtModifier {

    static final String DAMAGE_PERCENT_KEY = "exiledLib_strikecraftDamageToDamagedShipsPercent";
    static final String DAMAGE_MOD_ID = "exiledLib_strikecraftDamageToDamagedShips";
    static final float DAMAGED_HULL_LEVEL = 0.5f;

    private final ShipAPI fighter;
    private final ShipAPI carrier;

    DamagedShipDamageListener(ShipAPI fighter, ShipAPI carrier) {
        this.fighter = fighter;
        this.carrier = carrier;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || targetShip.isFighter() || targetShip.getHullLevel() >= DAMAGED_HULL_LEVEL
                || !CombatQueries.isHostile(fighter, targetShip)) {
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
