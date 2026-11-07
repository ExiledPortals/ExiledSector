package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import org.lwjgl.util.vector.Vector2f;

final class InjunctionListener extends ShipCombatListener implements DamageDealtModifier {

    static final String SLOWDOWN_PERCENT_KEY = "exiledSector_injunctionSlowdownPercent";

    InjunctionListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(target instanceof ShipAPI targetShip) || targetShip.isFighter() || !CombatQueries.isAliveNotHulk(targetShip)
                || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return null;
        }
        WeaponAPI weapon = CombatQueries.weaponBehind(param);
        if (weapon == null || weapon.getShip() != ownerShip || !WeaponScope.ENERGY.matches(weapon)) {
            return null;
        }
        float slowdownPercent = magnitude(SLOWDOWN_PERCENT_KEY);
        if (slowdownPercent > 0f) {
            Injunction.inflict(targetShip, slowdownPercent);
        }
        return null;
    }
}
