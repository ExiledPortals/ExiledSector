package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.List;

public record IntegratedEffects(List<SkillTypeEffect> effects, List<HullSizeSkillEffect> hullSizeEffects, List<String> effectText) {

    public IntegratedEffects {
        effects = List.copyOf(effects);
        hullSizeEffects = List.copyOf(hullSizeEffects);
        effectText = effectText == null ? List.of() : List.copyOf(effectText);
    }

    public IntegratedEffects(List<SkillTypeEffect> effects, List<HullSizeSkillEffect> hullSizeEffects) {
        this(effects, hullSizeEffects, List.of());
    }

    public boolean replacesEffectText() {
        return !effectText.isEmpty();
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
