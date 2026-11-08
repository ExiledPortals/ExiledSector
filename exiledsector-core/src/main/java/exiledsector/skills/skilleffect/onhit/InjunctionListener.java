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
    static final String VENT_SLOWDOWN_PERCENT_KEY = "exiledSector_injunctionVentSlowdownPercent";

    InjunctionListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ShipAPI targetShip = CombatQueries.hostileLiveShip(ownerShip, target);
        if (targetShip == null || targetShip.isFighter()) {
            return null;
        }
        WeaponAPI weapon = CombatQueries.weaponBehind(param);
        if (weapon == null || weapon.getShip() != ownerShip || !WeaponScope.ENERGY.matches(weapon)) {
            return null;
        }
        Injunction.Slowdown slowdown = new Injunction.Slowdown(magnitude(SLOWDOWN_PERCENT_KEY), magnitude(VENT_SLOWDOWN_PERCENT_KEY));
        if (slowdown.slowsAnything()) {
            Injunction.inflict(targetShip, slowdown);
        }
        return null;
    }
}
