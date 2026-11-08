package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.List;

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

    public static void addNonZero(List<SkillTypeEffect> target, List<HullSizeSkillEffect> hullSizeEffects, HullSize hullSize) {
        for (HullSizeSkillEffect effect : hullSizeEffects) {
            float value = effect.valueFor(hullSize);
            if (value != 0f) {
                target.add(new SkillTypeEffect(effect.effect(), value));
            }
        }
    }
}
