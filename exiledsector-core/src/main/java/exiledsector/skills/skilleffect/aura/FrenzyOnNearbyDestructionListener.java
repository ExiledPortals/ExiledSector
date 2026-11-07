package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.NearbyDestructionListener;
import exiledsector.skills.skilleffect.weapon.PirateFrenzy;

import java.util.List;

final class FrenzyOnNearbyDestructionListener extends NearbyDestructionListener {

    static final String RANGE_KEY = "exiledSector_pirateFrenzyNearbyDestructionRange";

    private final PirateFrenzy frenzy;

    FrenzyOnNearbyDestructionListener(ShipAPI ownerShip) {
        super(ownerShip, other -> WreckReclaimListener.isEnemyShip(ownerShip, other));
        this.frenzy = PirateFrenzy.of(ownerShip);
    }

    @Override
    protected float activeRange() {
        return magnitude(RANGE_KEY);
    }

    @Override
    protected void onDestroyed(List<ShipAPI> wrecks) {
        for (int i = 0; i < wrecks.size(); i++) {
            frenzy.gain();
        }
    }
}
