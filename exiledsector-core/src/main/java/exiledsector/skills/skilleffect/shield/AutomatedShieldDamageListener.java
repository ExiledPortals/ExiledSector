package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class AutomatedShieldDamageListener extends ShipCombatListener implements DamageTakenModifier {

    static final String REDUCTION_PERCENT_KEY = "exiledLib_shieldDamageTakenFromAutomatedPercent";
    private static final String MOD_ID_PREFIX = "exiledLib_automatedShieldDamage_";

    AutomatedShieldDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!shieldHit) {
            return null;
        }
        ShipAPI attacker = CombatQueries.attackingShip(null, param);
        if (attacker == null || !Misc.isAutomated(attacker) || !CombatQueries.isHostile(ownerShip, attacker)) {
            return null;
        }
        float reductionPercent = Math.min(100f, magnitude(REDUCTION_PERCENT_KEY));
        if (reductionPercent <= 0f) {
            return null;
        }
        damage.getModifier().modifyMult(modId, 1f - reductionPercent / 100f);
        return modId;
    }
}
