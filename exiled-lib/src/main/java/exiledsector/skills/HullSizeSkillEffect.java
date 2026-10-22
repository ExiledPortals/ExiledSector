package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.SkillEffect;

public record HullSizeSkillEffect(SkillEffect effect, float frigate, float destroyer, float cruiser, float capitalShip) {

    public float valueFor(HullSize hullSize) {
        if (hullSize == null) return capitalShip;
        return switch (hullSize) {
            case FRIGATE -> frigate;
            case DESTROYER -> destroyer;
            case CRUISER -> cruiser;
            default -> capitalShip;
        };
    }
}
