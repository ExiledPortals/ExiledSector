package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.NearbyWrecks;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.weapon.PirateFrenzy;

final class FrenzyOnNearbyDestructionListener extends ShipCombatListener implements AdvanceableListener {

    static final String RANGE_KEY = "exiledSector_pirateFrenzyNearbyDestructionRange";
    private static final float CHECK_SECONDS = 0.25f;

    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final NearbyWrecks wrecks;
    private final PirateFrenzy frenzy;

    FrenzyOnNearbyDestructionListener(ShipAPI ownerShip) {
        super(ownerShip);
        this.wrecks = new NearbyWrecks(ownerShip, other -> WreckReclaimListener.isEnemyShip(ownerShip, other));
        this.frenzy = PirateFrenzy.of(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        float range = magnitude(RANGE_KEY);
        if (range <= 0f) {
            return;
        }
        int destroyed = wrecks.newWithin(range).size();
        for (int i = 0; i < destroyed; i++) {
            frenzy.gain();
        }
    }
}
