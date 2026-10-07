package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

abstract class ShipCombatListener {

    protected final ShipAPI ownerShip;
    protected final String modId;

    ShipCombatListener(ShipAPI ownerShip) {
        this.ownerShip = ownerShip;
        this.modId = null;
    }

    ShipCombatListener(ShipAPI ownerShip, String modIdPrefix) {
        this.ownerShip = ownerShip;
        this.modId = modIdPrefix + ownerShip.getId();
    }

    final float magnitude(String key) {
        return ownerShip.getMutableStats().getDynamic().getValue(key, 0f);
    }

    final boolean ownerIsAliveNotHulk() {
        return CombatQueries.isAliveNotHulk(ownerShip);
    }
}
