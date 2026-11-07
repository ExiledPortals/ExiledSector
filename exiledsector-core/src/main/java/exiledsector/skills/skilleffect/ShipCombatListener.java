package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI;

public abstract class ShipCombatListener {

    protected final ShipAPI ownerShip;
    protected final String modId;
    private final ShipAPI magnitudeHolder;

    protected ShipCombatListener(ShipAPI ownerShip) {
        this(ownerShip, ownerShip, null);
    }

    protected ShipCombatListener(ShipAPI ownerShip, String modIdPrefix) {
        this(ownerShip, ownerShip, modIdPrefix);
    }

    protected ShipCombatListener(ShipAPI ownerShip, ShipAPI magnitudeHolder, String modIdPrefix) {
        this.ownerShip = ownerShip;
        this.magnitudeHolder = magnitudeHolder;
        this.modId = modIdPrefix == null ? null : modIdPrefix + ownerShip.getId();
    }

    protected final float magnitude(String key) {
        return magnitudeHolder.getMutableStats().getDynamic().getValue(key, 0f);
    }

    protected final boolean ownerIsAliveNotHulk() {
        return CombatQueries.isAliveNotHulk(ownerShip);
    }
}
