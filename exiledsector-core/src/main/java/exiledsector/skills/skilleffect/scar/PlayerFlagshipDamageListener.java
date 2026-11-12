package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.weapon.TargetDamageBonusListener;

final class PlayerFlagshipDamageListener extends TargetDamageBonusListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_scarDamageToPlayerFlagshipPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_scarPlayerFlagshipDamage_";

    PlayerFlagshipDamageListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX, DAMAGE_PERCENT_KEY);
    }

    @Override
    protected boolean qualifies(ShipAPI target) {
        PersonAPI captain = target.getCaptain();
        return !target.isFighter() && captain != null && captain.isPlayer();
    }
}
