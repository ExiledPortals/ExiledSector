package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

final class PhantomWakeListener extends PhaseTransitionListener {

    static final String SECONDS_KEY = "exiledSector_phaseEntryAfterimageSeconds";

    private PhaseAfterimage currentAfterimage;

    PhantomWakeListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    void onPhaseEntry() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || !ownerIsAliveNotHulk()) {
            return;
        }
        float lifetimeSeconds = magnitude(SECONDS_KEY);
        if (currentAfterimage != null && currentAfterimage.restartAt(ownerShip, lifetimeSeconds, engine)) {
            return;
        }
        currentAfterimage = PhaseAfterimage.leave(ownerShip, lifetimeSeconds, engine);
    }
}
