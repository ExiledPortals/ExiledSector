package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.ArcDischarge;
import exiledsector.skills.skilleffect.ShipCombatListener;

final class ArmorArcListener extends ShipCombatListener implements DamageListener {

    static final String ARC_COUNT_KEY = "exiledSector_armorDamageEmpArcs";
    static final float ARMOR_DAMAGE_PER_DISCHARGE = 500f;

    private float armorDamageTowardDischarge;

    ArmorArcListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    float armorDamageTowardDischarge() {
        return armorDamageTowardDischarge;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip || !ownerIsAliveNotHulk() || result.getTotalDamageToArmor() <= 0f) {
            return;
        }
        armorDamageTowardDischarge += result.getTotalDamageToArmor();
        int arcCount = Math.round(magnitude(ARC_COUNT_KEY));
        while (armorDamageTowardDischarge >= ARMOR_DAMAGE_PER_DISCHARGE && ArcDischarge.discharge(ownerShip, arcCount)) {
            armorDamageTowardDischarge -= ARMOR_DAMAGE_PER_DISCHARGE;
        }
        armorDamageTowardDischarge = Math.min(armorDamageTowardDischarge, ARMOR_DAMAGE_PER_DISCHARGE);
    }
}
