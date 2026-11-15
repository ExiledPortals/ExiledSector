package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatTestShips;

final class FighterCarriers {

    private FighterCarriers() {
    }

    static ShipAPI with(String magnitudeKey, float magnitude) {
        return CombatTestShips.withDynamic(magnitudeKey, magnitude);
    }
}
