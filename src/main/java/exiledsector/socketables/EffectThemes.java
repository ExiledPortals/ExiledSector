package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.SkillType;
import exiledsector.skills.tags.SkillTags;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EffectThemes {

    private static final double TIE_EPSILON = 1e-9;

    private final Map<String, Set<String>> themesByEffect;

    private EffectThemes(Map<String, Set<String>> themesByEffect) {
        this.themesByEffect = themesByEffect;
    }

    public static EffectThemes from(Collection<SkillType> types) {
        Map<String, Map<String, Double>> scores = new HashMap<>();
        for (SkillType type : types) {
            List<String> themes = type.getTags().stream().filter(SkillTags.THEME::contains).toList();
            Set<String> effects = new LinkedHashSet<>();
            type.effectsFor(HullSize.CAPITAL_SHIP).forEach(effect -> effects.add(effect.effect().name()));
            if (themes.isEmpty() || effects.isEmpty()) {
                continue;
            }
            double share = 1.0 / effects.size();
            for (String effect : effects) {
                Map<String, Double> effectScores = scores.computeIfAbsent(effect, key -> new HashMap<>());
                themes.forEach(theme -> effectScores.merge(theme, share, Double::sum));
            }
        }
        Map<String, Set<String>> themesByEffect = new HashMap<>();
        scores.forEach((effect, effectScores) -> {
            double best = effectScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
            Set<String> themes = new LinkedHashSet<>();
            for (String theme : SkillTags.THEME) {
                Double score = effectScores.get(theme);
                if (score != null && score >= best - TIE_EPSILON) {
                    themes.add(theme);
                }
            }
            themesByEffect.put(effect, Set.copyOf(themes));
        });
        return new EffectThemes(themesByEffect);
    }

    public Set<String> of(String effectName) {
        return themesByEffect.getOrDefault(effectName, Set.of());
    }

    public Set<String> of(List<RolledEffect> effects) {
        Set<String> themes = new LinkedHashSet<>();
        for (RolledEffect effect : effects) {
            themes.addAll(of(effect.effectName()));
        }
        return themes;
    }
}
