package exiledsector.skills;

import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;
import exiledsector.skills.skilleffect.ScopedWeaponEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.WeaponScope;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class DiminishingReturns {

    public static final String EFFICIENCY_FIELD_ID = "exiledSector_diminishingEfficiency";
    public static final int DEFAULT_EFFICIENCY_PERCENT = 50;
    public static final int MAX_THRESHOLD_PERCENT = 500;
    public static final Config OFF = new Config(100, DiminishedStat.defaultThresholds());

    private static final List<WeaponScope> WEAPON_LEAF_SCOPES = Arrays.stream(WeaponScope.values())
            .filter(scope -> scope.children().isEmpty()).toList();
    private static final List<WeaponScope> WEAPON_SCOPES_TOP_DOWN = Arrays.stream(WeaponScope.values())
            .sorted(Comparator.comparingInt(DiminishingReturns::depth)).toList();
    private static final AtomicReference<Config> CURRENT = new AtomicReference<>();

    public record Config(int efficiencyPercent, Map<DiminishedStat, Integer> thresholdPercents) {

        public Config {
            efficiencyPercent = Math.max(0, Math.min(100, efficiencyPercent));
            thresholdPercents = Map.copyOf(thresholdPercents);
        }

        public static Config defaults() {
            return new Config(DEFAULT_EFFICIENCY_PERCENT, DiminishedStat.defaultThresholds());
        }

        static Config fromSettings() {
            Map<DiminishedStat, Integer> thresholds = new EnumMap<>(DiminishedStat.class);
            for (DiminishedStat stat : DiminishedStat.values()) {
                thresholds.put(stat, ModSettings.intOr(stat.thresholdFieldId(), stat.defaultThresholdPercent()));
            }
            return new Config(ModSettings.intOr(EFFICIENCY_FIELD_ID, DEFAULT_EFFICIENCY_PERCENT), thresholds);
        }

        boolean isOff() {
            return efficiencyPercent >= 100;
        }

        float efficiency() {
            return efficiencyPercent / 100f;
        }

        float thresholdPercent(DiminishedStat stat) {
            return Math.max(0, thresholdPercents.getOrDefault(stat, stat.defaultThresholdPercent()));
        }
    }

    public record Sums(Map<SkillEffect, Float> bonuses, Map<SkillEffect, Float> drawbacks) {

        float bonus(SkillEffect effect) {
            return bonuses.getOrDefault(effect, 0f);
        }

        float drawback(SkillEffect effect) {
            return drawbacks.getOrDefault(effect, 0f);
        }
    }

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            CURRENT.set(null);
        }
    }

    private DiminishingReturns() {
    }

    public static Config current() {
        Config config = CURRENT.get();
        if (config == null) {
            config = Config.fromSettings();
            CURRENT.set(config);
        }
        return config;
    }

    static Map<SkillEffect, Float> ratios(Config config, StatBaseline baseline, Sums sums) {
        Map<SkillEffect, Float> ratios = new HashMap<>();
        if (config.isOff()) {
            return ratios;
        }
        for (DiminishedStat stat : DiminishedStat.values()) {
            if (stat == DiminishedStat.WEAPON_RANGE) {
                addRangeRatios(ratios, config, sums);
                continue;
            }
            float baseValue = stat.baseValue(baseline);
            float bonus = 0f;
            float drawback = 0f;
            for (SkillEffect effect : stat.effects()) {
                float toPercent = percentFactor(effect, baseValue);
                bonus += sums.bonus(effect) * toPercent;
                drawback += sums.drawback(effect) * toPercent;
            }
            float ratio = keptShare(bonus, drawback, config.thresholdPercent(stat), config.efficiency());
            if (ratio < 1f) {
                stat.effects().forEach(effect -> ratios.put(effect, ratio));
            }
        }
        return ratios;
    }

    static float diminished(float rawPercent, float thresholdPercent, float efficiency) {
        return rawPercent <= thresholdPercent ? rawPercent : thresholdPercent + (rawPercent - thresholdPercent) * efficiency;
    }

    private static float keptShare(float bonus, float drawback, float thresholdPercent, float efficiency) {
        if (bonus <= 0f) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, bonusBudget(bonus, drawback, thresholdPercent, efficiency) / bonus));
    }

    private static float bonusBudget(float bonus, float drawback, float thresholdPercent, float efficiency) {
        return diminished(bonus + drawback, thresholdPercent, efficiency) - drawback;
    }

    private static float percentFactor(SkillEffect effect, float baseValue) {
        if (effect.statMode() != StatMode.FLAT) {
            return 1f;
        }
        return baseValue > 0f ? 100f / baseValue : 0f;
    }

    private static void addRangeRatios(Map<SkillEffect, Float> ratios, Config config, Sums sums) {
        float thresholdPercent = config.thresholdPercent(DiminishedStat.WEAPON_RANGE);
        Map<WeaponScope, Float> bonusByScope = new EnumMap<>(WeaponScope.class);
        Map<WeaponScope, Float> drawbackByScope = new EnumMap<>(WeaponScope.class);
        for (SkillEffect effect : DiminishedStat.WEAPON_RANGE.effects()) {
            if (effect instanceof ScopedWeaponEffect weaponEffect) {
                bonusByScope.merge(weaponEffect.scope(), sums.bonus(effect), Float::sum);
                drawbackByScope.merge(weaponEffect.scope(), sums.drawback(effect), Float::sum);
            }
        }
        Map<WeaponScope, Float> budgetLeft = new EnumMap<>(WeaponScope.class);
        for (WeaponScope leafScope : WEAPON_LEAF_SCOPES) {
            float bonus = chainTotal(bonusByScope, leafScope);
            budgetLeft.put(leafScope, Math.min(bonus, bonusBudget(bonus, chainTotal(drawbackByScope, leafScope), thresholdPercent,
                    config.efficiency())));
        }
        for (WeaponScope scope : WEAPON_SCOPES_TOP_DOWN) {
            float scopeBonus = bonusByScope.getOrDefault(scope, 0f);
            if (scopeBonus <= 0f) {
                continue;
            }
            float ratio = 0f;
            for (WeaponScope leafScope : WEAPON_LEAF_SCOPES) {
                if (reaches(scope, leafScope)) {
                    float unassignedBonus = chainTotalFrom(bonusByScope, leafScope, depth(scope));
                    ratio = Math.max(ratio, Math.max(0f, Math.min(1f, budgetLeft.get(leafScope) / unassignedBonus)));
                }
            }
            for (WeaponScope leafScope : WEAPON_LEAF_SCOPES) {
                if (reaches(scope, leafScope)) {
                    budgetLeft.merge(leafScope, -scopeBonus * ratio, Float::sum);
                }
            }
            if (ratio < 1f) {
                float scopeRatio = ratio;
                for (SkillEffect effect : DiminishedStat.WEAPON_RANGE.effects()) {
                    if (effect instanceof ScopedWeaponEffect weaponEffect && weaponEffect.scope() == scope) {
                        ratios.put(effect, scopeRatio);
                    }
                }
            }
        }
    }

    private static float chainTotal(Map<WeaponScope, Float> totalsByScope, WeaponScope leafScope) {
        return chainTotalFrom(totalsByScope, leafScope, 0);
    }

    private static float chainTotalFrom(Map<WeaponScope, Float> totalsByScope, WeaponScope leafScope, int minDepth) {
        float total = 0f;
        for (WeaponScope scope = leafScope; scope != null; scope = scope.parent()) {
            if (depth(scope) >= minDepth) {
                total += totalsByScope.getOrDefault(scope, 0f);
            }
        }
        return total;
    }

    private static boolean reaches(WeaponScope effectScope, WeaponScope leafScope) {
        for (WeaponScope scope = leafScope; scope != null; scope = scope.parent()) {
            if (scope == effectScope) {
                return true;
            }
        }
        return false;
    }

    private static int depth(WeaponScope scope) {
        int depth = 0;
        for (WeaponScope parent = scope.parent(); parent != null; parent = parent.parent()) {
            depth++;
        }
        return depth;
    }
}
