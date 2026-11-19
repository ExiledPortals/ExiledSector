package exiledsector.charts.anomaly;

import exiledsector.charts.anomaly.magneticstorm.MagneticStormTuning;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.items.ItemData;
import exiledsector.items.modifier.Luck;
import exiledsector.items.modifier.LuckyRolls;
import exiledsector.items.modifier.RolledModifier;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class FlareLuck {

    public static final String LUCKY_MODIFIERS_KEY = ChartModifier.CHART_STELLAR_FLARES_LUCKY_MODIFIERS.anomalyTuningKey();
    public static final String LUCKY_IMPLICITS_KEY = ChartModifier.CHART_STELLAR_FLARES_LUCKY_IMPLICITS.anomalyTuningKey();
    public static final String LUCKY_ROLLS_KEY = ChartModifier.CHART_STELLAR_FLARES_LUCKY_ROLLS.anomalyTuningKey();
    public static final String ALL_LUCKY_KEY = ChartModifier.CHART_STELLAR_FLARES_ALL_LUCKY.anomalyTuningKey();
    static final int REWARD_LUCKY_MODIFIERS = 1;

    private FlareLuck() {
    }

    public static Luck dropLuck(ChartRun run) {
        return run == null ? Luck.NONE : dropLuck(ChartRunEffects.contextOf(run), run.anomalyIds());
    }

    public static Luck dropLuck(ChartGenContext context, Collection<String> anomalyIds) {
        if (!hasFlares(anomalyIds)) {
            return Luck.NONE;
        }
        ChartGenContext tuning = ChartGenContext.orNone(context);
        int luckyModifiers = REWARD_LUCKY_MODIFIERS + count(tuning, LUCKY_MODIFIERS_KEY) + geomagneticSurgeModifiers(tuning, anomalyIds);
        return new Luck(luckyModifiers, isOn(tuning, ALL_LUCKY_KEY), isOn(tuning, LUCKY_IMPLICITS_KEY), rolls(tuning, anomalyIds));
    }

    private static int geomagneticSurgeModifiers(ChartGenContext tuning, Collection<String> anomalyIds) {
        return anomalyIds.contains(MagneticStorm.ANOMALY_ID) ? MagneticStormTuning.of(tuning).extraLuckyModifiers() : 0;
    }

    public static int rolls(ChartRun run) {
        return run == null ? Luck.BASE_ROLLS : rolls(ChartRunEffects.contextOf(run), run.anomalyIds());
    }

    public static int rolls(ChartGenContext context, Collection<String> anomalyIds) {
        return hasFlares(anomalyIds) ? atLeastBaseRolls(ChartGenContext.orNone(context).anomalyPercent(LUCKY_ROLLS_KEY)) : Luck.BASE_ROLLS;
    }

    public static ItemData withLuckyModifiers(ItemData item, ChartRun run, Predicate<RolledModifier> lucky) {
        return LuckyRolls.withLuckyModifiers(item, lucky, rolls(run));
    }

    public static int treeRolls(List<ChartGenContext.AppliedEffect> treeLines) {
        float rolls = 0f;
        for (ChartGenContext.AppliedEffect line : treeLines) {
            if (line.effect() == ChartModifier.CHART_STELLAR_FLARES_LUCKY_ROLLS) {
                rolls += line.value();
            }
        }
        return atLeastBaseRolls(rolls);
    }

    private static int atLeastBaseRolls(float rolls) {
        return Math.max(Luck.BASE_ROLLS, Math.round(rolls));
    }

    private static boolean hasFlares(Collection<String> anomalyIds) {
        return anomalyIds != null && anomalyIds.contains(StellarFlares.ANOMALY_ID);
    }

    private static int count(ChartGenContext context, String key) {
        return Math.max(0, Math.round(context.anomalyPercent(key)));
    }

    private static boolean isOn(ChartGenContext context, String key) {
        return context.anomalyPercent(key) > 0f;
    }
}
