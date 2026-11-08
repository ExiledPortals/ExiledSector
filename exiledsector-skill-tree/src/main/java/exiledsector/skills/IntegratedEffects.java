package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.List;

public record IntegratedEffects(List<SkillTypeEffect> effects, List<HullSizeSkillEffect> hullSizeEffects) {

    public IntegratedEffects {
        effects = List.copyOf(effects);
        hullSizeEffects = List.copyOf(hullSizeEffects);
    }

    public List<SkillTypeEffect> effectsFor(HullSize hullSize) {
        if (hullSizeEffects.isEmpty()) {
            return effects;
        }
        List<SkillTypeEffect> combined = new ArrayList<>(effects);
        HullSizeSkillEffect.addNonZero(combined, hullSizeEffects, hullSize);
        return combined;
    }
}
