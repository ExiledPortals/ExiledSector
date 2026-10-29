package exiledsector.ui.stats;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

record StatReading(float base, float value, float flat, float factor, List<Modifier> modifiers) {

    private static final float EPSILON = 0.0005f;

    enum Kind {
        FLAT,
        PERCENT,
        MULT
    }

    record Modifier(String modId, Kind kind, float value, String desc) {
    }

    static StatReading of(MutableStat stat) {
        return new StatReading(stat.getBaseValue(), stat.getModifiedValue(), stat.getFlatMod(), (1f + stat.getPercentMod() / 100f) * stat.getMult(),
                modifiers(stat.getFlatMods(), stat.getPercentMods(), stat.getMultMods()));
    }

    static StatReading of(StatBonus bonus, float base) {
        return new StatReading(base, bonus.computeEffective(base), bonus.getFlatBonus(), bonus.getBonusMult(),
                modifiers(bonus.getFlatBonuses(), bonus.getPercentBonuses(), bonus.getMultBonuses()));
    }

    static StatReading scaled(float base, List<MutableStat> factors) {
        float factor = 1f;
        List<Modifier> modifiers = new ArrayList<>();
        for (MutableStat stat : factors) {
            factor *= stat.getBaseValue() == 0f ? 1f : stat.getModifiedValue() / stat.getBaseValue();
            modifiers.addAll(of(stat).modifiers());
        }
        return new StatReading(base, base * factor, 0f, factor, List.copyOf(modifiers));
    }

    boolean changed() {
        return Math.abs(flat) > EPSILON || Math.abs(factor - 1f) > EPSILON;
    }

    private static List<Modifier> modifiers(Map<String, MutableStat.StatMod> flatMods, Map<String, MutableStat.StatMod> percentMods,
                                            Map<String, MutableStat.StatMod> multMods) {
        List<Modifier> modifiers = new ArrayList<>();
        add(modifiers, Kind.FLAT, flatMods, 0f);
        add(modifiers, Kind.PERCENT, percentMods, 0f);
        add(modifiers, Kind.MULT, multMods, 1f);
        return List.copyOf(modifiers);
    }

    private static void add(List<Modifier> modifiers, Kind kind, Map<String, MutableStat.StatMod> mods, float neutralValue) {
        if (mods == null) {
            return;
        }
        List<Modifier> added = new ArrayList<>();
        mods.forEach((modId, mod) -> {
            if (mod != null && Math.abs(mod.getValue() - neutralValue) > EPSILON) {
                added.add(new Modifier(modId, kind, mod.getValue(), mod.getDesc()));
            }
        });
        added.sort(Comparator.comparing(Modifier::modId));
        modifiers.addAll(added);
    }
}
