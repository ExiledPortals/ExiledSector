package exiledsector.skills.skilleffect.phase;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.Random;

final class PhaseExitLensStrikeListener extends PhaseTransitionListener {

    static final String STRIKE_COUNT_KEY = "exiledSector_phaseExitLensStrikes";

    private final Random random = new Random();

    PhaseExitLensStrikeListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    void onPhaseExit() {
        int strikes = Math.round(magnitude(STRIKE_COUNT_KEY));
        if (strikes <= 0 || !ownerIsAliveNotHulk()) {
            return;
        }
        WeightedRandomPicker<CombatEntityAPI> targets = LensStrike.targetsNear(ownerShip, random);
        for (int strike = 0; strike < strikes && !targets.isEmpty(); strike++) {
            LensStrike.strike(ownerShip, targets.pick(), random);
        }
    }
}
