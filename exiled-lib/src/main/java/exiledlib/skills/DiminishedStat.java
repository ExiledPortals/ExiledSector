package exiledlib.skills;

import exiledlib.HostMod;
import exiledlib.skills.skilleffect.DefenseSkillEffect;
import exiledlib.skills.skilleffect.FluxSkillEffect;
import exiledlib.skills.skilleffect.MovementSkillEffect;
import exiledlib.skills.skilleffect.ScopedWeaponEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.StatMode;
import exiledlib.skills.skilleffect.WeaponStatFamily;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public enum DiminishedStat {

    ARMOUR("diminishingArmour", 50, StatBaseline::armor,
            Set.of(DefenseSkillEffect.ARMOR_FLAT, DefenseSkillEffect.ARMOR_PERCENT)),
    HULL("diminishingHull", 50, StatBaseline::hull,
            Set.of(DefenseSkillEffect.HULL_FLAT, DefenseSkillEffect.HULL_PERCENT, DefenseSkillEffect.HULL_MULT)),
    FLUX_CAPACITY("diminishingFluxCapacity", 50, StatBaseline::fluxCapacity,
            Set.of(FluxSkillEffect.FLUX_CAPACITY_FLAT, FluxSkillEffect.FLUX_CAPACITY_PERCENT, FluxSkillEffect.FLUX_CAPACITY_MULT)),
    FLUX_DISSIPATION("diminishingFluxDissipation", 50, StatBaseline::fluxDissipation,
            Set.of(FluxSkillEffect.FLUX_DISSIPATION_FLAT, FluxSkillEffect.FLUX_DISSIPATION_PERCENT, FluxSkillEffect.FLUX_DISSIPATION_MULT)),
    TOP_SPEED("diminishingTopSpeed", 30, StatBaseline::topSpeed,
            Set.of(MovementSkillEffect.TOP_SPEED_FLAT, MovementSkillEffect.TOP_SPEED_PERCENT, MovementSkillEffect.TOP_SPEED_MULT)),
    WEAPON_RANGE("diminishingWeaponRange", 30, baseline -> 0f,
            ScopedWeaponEffect.all().stream()
                    .filter(effect -> effect.family() == WeaponStatFamily.RANGE && effect.mode() != StatMode.FLAT)
                    .collect(Collectors.toUnmodifiableSet()));

    private static final Map<SkillEffect, DiminishedStat> BY_EFFECT = byEffect();

    private final String thresholdSettingName;
    private final int defaultThresholdPercent;
    private final ToDoubleFunction<StatBaseline> baseValue;
    private final Set<SkillEffect> effects;

    DiminishedStat(String thresholdSettingName, int defaultThresholdPercent, ToDoubleFunction<StatBaseline> baseValue,
                   Set<? extends SkillEffect> effects) {
        this.thresholdSettingName = thresholdSettingName;
        this.defaultThresholdPercent = defaultThresholdPercent;
        this.baseValue = baseValue;
        this.effects = Set.copyOf(effects);
    }

    private static Map<SkillEffect, DiminishedStat> byEffect() {
        Map<SkillEffect, DiminishedStat> statsByEffect = new HashMap<>();
        for (DiminishedStat stat : values()) {
            stat.effects.forEach(effect -> statsByEffect.put(effect, stat));
        }
        return Map.copyOf(statsByEffect);
    }

    public static DiminishedStat of(SkillEffect effect) {
        return BY_EFFECT.get(effect);
    }

    public static Map<DiminishedStat, Integer> defaultThresholds() {
        Map<DiminishedStat, Integer> thresholds = new EnumMap<>(DiminishedStat.class);
        for (DiminishedStat stat : values()) {
            thresholds.put(stat, stat.defaultThresholdPercent);
        }
        return thresholds;
    }

    public String thresholdFieldId() {
        return HostMod.settingId(thresholdSettingName);
    }

    public int defaultThresholdPercent() {
        return defaultThresholdPercent;
    }

    public String translationKey() {
        return "diminishing.stat." + name();
    }

    Set<SkillEffect> effects() {
        return effects;
    }

    float baseValue(StatBaseline baseline) {
        return (float) baseValue.applyAsDouble(baseline);
    }
}
