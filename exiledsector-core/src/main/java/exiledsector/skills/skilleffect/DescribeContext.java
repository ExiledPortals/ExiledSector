package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.SkillTypeEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DescribeContext {

    public static final DescribeContext NONE = new DescribeContext(null, Map.of());

    private final HullSize hullSize;
    private final Map<SkillEffect, Float> magnitudes;

    private DescribeContext(HullSize hullSize, Map<SkillEffect, Float> magnitudes) {
        this.hullSize = hullSize;
        this.magnitudes = magnitudes;
    }

    public static DescribeContext of(List<SkillTypeEffect> effects, HullSize hullSize) {
        Map<SkillEffect, Float> magnitudes = new HashMap<>();
        for (SkillTypeEffect effect : effects) {
            magnitudes.merge(effect.effect(), effect.magnitude(), Float::sum);
        }
        return new DescribeContext(hullSize, magnitudes);
    }

    public HullSize hullSize() {
        return hullSize;
    }

    public float magnitudeOf(SkillEffect effect) {
        return magnitudes.getOrDefault(effect, 0f);
    }

    public boolean showsInParentLine(SkillEffect effect) {
        for (SkillEffect parent : effect.refines()) {
            if (magnitudes.containsKey(parent)) {
                return true;
            }
        }
        return false;
    }
}
