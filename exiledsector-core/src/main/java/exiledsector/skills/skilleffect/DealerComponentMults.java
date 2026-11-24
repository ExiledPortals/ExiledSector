package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public final class DealerComponentMults {

    static final String MOD_ID = "exiledSector_dealerComponentMults";

    private DealerComponentMults() {
    }

    public static void applyDuring(ShipAPI dealer, ShipAPI target, Runnable applyHit) {
        MutableShipStatsAPI dealt = dealer.getMutableStats();
        MutableShipStatsAPI taken = target.getMutableStats();
        taken.getWeaponDamageTakenMult().modifyMult(MOD_ID, dealt.getDamageToTargetWeaponsMult().getModifiedValue());
        taken.getEngineDamageTakenMult().modifyMult(MOD_ID, dealt.getDamageToTargetEnginesMult().getModifiedValue());
        try {
            applyHit.run();
        } finally {
            taken.getWeaponDamageTakenMult().unmodify(MOD_ID);
            taken.getEngineDamageTakenMult().unmodify(MOD_ID);
        }
    }
}
