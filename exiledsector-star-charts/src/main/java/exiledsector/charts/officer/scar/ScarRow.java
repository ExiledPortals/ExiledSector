package exiledsector.charts.officer.scar;

import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.List;

public record ScarRow(String id, ScarDetector detector, float threshold, String themeTag, List<Effect> effects,
                      List<String> dModIds, boolean randomDMod, int extraLevels) {

    public record Effect(HullFeature feature, SkillEffect effect, float magnitudePerStack) {
    }

    public List<Effect> fittingEffects(HullFacts hull) {
        if (hull == null) {
            return List.of();
        }
        List<Effect> fitting = new ArrayList<>();
        for (Effect entry : effects) {
            if (entry.feature().fits(hull)) {
                fitting.add(entry);
            }
        }
        return fitting;
    }

    public boolean fits(HullFacts hull) {
        return effects.isEmpty() || !fittingEffects(hull).isEmpty();
    }

    public boolean themed() {
        return themeTag != null && !themeTag.isEmpty();
    }

    public float score(ScarEvidence evidence) {
        if (detector == null || threshold <= 0f || evidence == null) {
            return 0f;
        }
        return detector.measure(evidence) / threshold;
    }
}
