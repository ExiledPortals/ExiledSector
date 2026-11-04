package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.StyledText;
import exiledsector.modifiers.ModifierCategory;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ScaleContext;
import exiledsector.skills.ShipHullSizes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemStatTotals {

    public static final ItemStatTotals NONE = new ItemStatTotals(Map.of());
    private static final Pattern SHOWN_NUMBER = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");
    private static final int HULL_SIZE_COUNT = ShipHullSizes.ORDER.size();

    private static final class StatSum {

        private double fixedValue;
        private double[] hullSizeValues;

        void addFixed(double shownValue) {
            fixedValue += shownValue;
        }

        void addPerHullSize(double[] shownValues) {
            if (hullSizeValues == null) {
                hullSizeValues = new double[HULL_SIZE_COUNT];
            }
            for (int index = 0; index < HULL_SIZE_COUNT; index++) {
                hullSizeValues[index] += shownValues[index];
            }
        }
    }

    private final Map<String, Map<ModifierCategory, StatSum>> sumsByStat;

    private ItemStatTotals(Map<String, Map<ModifierCategory, StatSum>> sumsByStat) {
        this.sumsByStat = sumsByStat;
    }

    public static ItemStatTotals of(Item item) {
        return of(item.rolls(), item.modifierEffects(), item::scaleContext);
    }

    public static ItemStatTotals of(List<SlottedRoll> rolls, ModifierEffects effects, Function<HullSize, ScaleContext> contexts) {
        Map<String, Map<ModifierCategory, StatSum>> sumsByStat = new LinkedHashMap<>();
        ScaleContext itemContext = contexts.apply(null);
        for (SlottedRoll slottedRoll : rolls) {
            RolledLine roll = slottedRoll.roll();
            ModifierEffect effect = roll.effect(effects);
            ModifierLine line = slottedRoll.line();
            StatSum sum = sumsByStat.computeIfAbsent(roll.effectName(), stat -> new EnumMap<>(ModifierCategory.class))
                    .computeIfAbsent(slottedRoll.category(), category -> new StatSum());
            if (line != null && line.scalesWithHullSize() && line.listsEveryValueFor(itemContext)) {
                double[] shownValues = new double[HULL_SIZE_COUNT];
                for (int index = 0; index < HULL_SIZE_COUNT; index++) {
                    float hullValue = line.valueFor(roll.magnitude(), contexts.apply(ShipHullSizes.ORDER.get(index)));
                    shownValues[index] = shownValue(effect, hullValue);
                }
                sum.addPerHullSize(shownValues);
            } else {
                float value = line == null ? roll.magnitude() : line.valueFor(roll.magnitude(), itemContext);
                sum.addFixed(shownValue(effect, value));
            }
        }
        return sumsByStat.isEmpty() ? NONE : new ItemStatTotals(Collections.unmodifiableMap(sumsByStat));
    }

    static double shownValue(ModifierEffect effect, float value) {
        StyledText description = effect == null ? null : effect.description(value);
        if (description == null || description.spans().isEmpty()) {
            return value;
        }
        StyledText.Span valueSpan = description.spans().get(0);
        Matcher numberMatcher = SHOWN_NUMBER.matcher(description.plain().substring(valueSpan.start(), valueSpan.end()));
        if (!numberMatcher.find()) {
            return value;
        }
        return Math.copySign(Double.parseDouble(numberMatcher.group().replace(",", "")), value);
    }

    public Set<String> stats() {
        return sumsByStat.keySet();
    }

    public List<Double> totals(String statName, Set<ModifierCategory> categories) {
        Map<ModifierCategory, StatSum> sums = sumsByStat.get(statName);
        if (sums == null) {
            return List.of();
        }
        boolean present = false;
        double fixedTotal = 0d;
        double[] hullSizeTotals = null;
        for (ModifierCategory category : categories) {
            StatSum sum = sums.get(category);
            if (sum == null) {
                continue;
            }
            present = true;
            fixedTotal += sum.fixedValue;
            if (sum.hullSizeValues != null) {
                hullSizeTotals = hullSizeTotals == null ? new double[HULL_SIZE_COUNT] : hullSizeTotals;
                for (int index = 0; index < HULL_SIZE_COUNT; index++) {
                    hullSizeTotals[index] += sum.hullSizeValues[index];
                }
            }
        }
        if (!present) {
            return List.of();
        }
        if (hullSizeTotals == null) {
            return List.of(fixedTotal);
        }
        List<Double> perHullSize = new ArrayList<>(HULL_SIZE_COUNT);
        for (double hullSizeTotal : hullSizeTotals) {
            perHullSize.add(fixedTotal + hullSizeTotal);
        }
        return perHullSize;
    }
}
