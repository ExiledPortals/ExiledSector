package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.NearbyDestructionListener;

import java.util.List;

final class NearbyDestructionHeartless extends NearbyDestructionListener {

    static final String RANGE_KEY = "exiledSector_heartlessNearbyDestructionRange";

    NearbyDestructionHeartless(ShipAPI ownerShip) {
        super(ownerShip, other -> !other.isFighter() && other.getParentStation() == null);
    }

    @Override
    protected float activeRange() {
        return magnitude(RANGE_KEY);
    }

    @Override
    protected void onDestroyed(List<ShipAPI> wrecks) {
        HeartlessStacks stacks = HeartlessStacks.of(ownerShip);
        for (int i = 0; i < wrecks.size(); i++) {
            stacks.gain();
        }
    }
}
