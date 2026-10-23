package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

public abstract class ShipCombatListener {

    protected final ShipAPI ownerShip;
    protected final String modId;

    protected ShipCombatListener(ShipAPI ownerShip) {
        this.ownerShip = ownerShip;
        this.modId = null;
    }

    protected ShipCombatListener(ShipAPI ownerShip, String modIdPrefix) {
        this.ownerShip = ownerShip;
        this.modId = modIdPrefix + ownerShip.getId();
    }

    protected final float magnitude(String key) {
        return ownerShip.getMutableStats().getDynamic().getValue(key, 0f);
    }

    protected final boolean ownerIsAliveNotHulk() {
        return CombatQueries.isAliveNotHulk(ownerShip);
    }
}
