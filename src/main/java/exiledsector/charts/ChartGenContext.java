package exiledsector.charts;

import exiledsector.items.Item;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemKind;
import exiledsector.items.SlottedRoll;
import exiledsector.items.StarChart;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ScaleContext;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ChartGenContext {

    static final float MIN_SENSOR_RANGE_MULT = 0.1f;
    static final float MAX_SENSOR_RANGE_MULT = 2f;
    static final float PERCENT = 100f;
    static final float QUANTITY_PERCENT_PER_QUALITY = 1f;
    private static final Logger LOG = Logger.getLogger(ChartGenContext.class);
    private static final Set<String> REPORTED_UNRESOLVED_ROLLS = ConcurrentHashMap.newKeySet();

    public record AppliedEffect(ChartEffect effect, float value) {
    }

    private static final ChartGenContext NONE = new ChartGenContext(List.of(), 0, null);

    private final List<AppliedEffect> applied;
    private final int quality;
    private final String factionId;
    private float fleetSizePercent;
    private int baseFleets;
    private int extraFleets;
    private int officerLevelBonus;
    private int extraOfficers;
    private float wardenSizePercent;
    private float fleetQualityPercent;
    private float sensorRangePercent;
    private float enemySensorRangeBonus;
    private float quantityPercent;
    private final Map<ChartPoi.Kind, Float> siteBudgetPercents = new EnumMap<>(ChartPoi.Kind.class);
    private int extraPois;
    private float itemPercent;
    private final Map<DropType, Float> dropPercents = new EnumMap<>(DropType.class);
    private float wardenSpoilsPercent;
    private float rewardPercent;
    private final Map<ChartFleetStat, Float> fleetStatPercents = new EnumMap<>(ChartFleetStat.class);
    private Map<ChartFleetStat, Float> fleetStatMults;

    private ChartGenContext(List<AppliedEffect> applied, int quality, String factionId) {
        this.applied = List.copyOf(applied);
        this.quality = Math.max(0, quality);
        this.factionId = factionId;
        for (AppliedEffect effect : this.applied) {
            effect.effect().beforeGeneration(this, effect.value());
        }
    }

    public static ChartGenContext none() {
        return NONE;
    }

    public static ChartGenContext of(List<AppliedEffect> applied) {
        return of(applied, 0);
    }

    public static ChartGenContext of(List<AppliedEffect> applied, int quality) {
        return of(applied, quality, null);
    }

    public static ChartGenContext of(List<AppliedEffect> applied, int quality, String factionId) {
        boolean noEffects = applied == null || applied.isEmpty();
        if (noEffects && quality <= 0 && factionId == null) {
            return NONE;
        }
        return new ChartGenContext(noEffects ? List.of() : applied, quality, factionId);
    }

    public static ChartGenContext from(Item chart) {
        if (chart == null) {
            return none();
        }
        ModifierEffects effects = chart.modifierEffects();
        ScaleContext scale = chart.scaleContext(null);
        List<AppliedEffect> applied = new ArrayList<>();
        String factionId = null;
        for (SlottedRoll roll : chart.rolls()) {
            ModifierEffect rolledEffect = roll.roll().effect(effects);
            if (rolledEffect instanceof ChartFactionEffect faction) {
                factionId = factionId == null ? faction.factionId() : factionId;
            } else if (rolledEffect instanceof ChartEffect effect) {
                if (roll.line() == null) {
                    reportUnresolved(chart, effect);
                } else {
                    applied.add(new AppliedEffect(effect, roll.line().valueFor(roll.roll().magnitude(), scale)));
                }
            }
        }
        return of(applied, chart instanceof StarChart starChart ? starChart.quality() : 0, factionId);
    }

    private static void reportUnresolved(Item chart, ChartEffect effect) {
        if (REPORTED_UNRESOLVED_ROLLS.add(chart.definitionId() + "/" + effect.name())) {
            LOG.warn("[ExiledSector] Star Chart " + chart.definitionId() + " has a " + effect.name()
                    + " roll whose modifier is no longer defined; the roll is ignored");
        }
    }

    public List<AppliedEffect> applied() {
        return applied;
    }

    public int quality() {
        return quality;
    }

    public String factionId() {
        return factionId;
    }

    public boolean isEmpty() {
        return applied.isEmpty() && quality == 0;
    }

    void addFleetSizePercent(float value) {
        fleetSizePercent += value;
    }

    void addBaseFleets(int value) {
        baseFleets += value;
    }

    void addExtraFleets(int value) {
        extraFleets += value;
    }

    void addOfficerLevelBonus(int value) {
        officerLevelBonus += value;
    }

    void addExtraOfficers(int value) {
        extraOfficers += value;
    }

    void addWardenSizePercent(float value) {
        wardenSizePercent += value;
    }

    void addFleetQualityPercent(float value) {
        fleetQualityPercent += value;
    }

    void addSensorRangePercent(float value) {
        sensorRangePercent += value;
    }

    void addEnemySensorRangeBonus(float value) {
        enemySensorRangeBonus += value;
    }

    public float enemySensorRangeBonus() {
        return enemySensorRangeBonus;
    }

    void addQuantityPercent(float value) {
        quantityPercent += value;
    }

    void addSiteBudgetPercent(ChartPoi.Kind kind, float value) {
        siteBudgetPercents.merge(kind, value, Float::sum);
    }

    void addAllSiteBudgetsPercent(float value) {
        for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
            addSiteBudgetPercent(kind, value);
        }
    }

    void addExtraPois(int value) {
        extraPois += value;
    }

    void addItemPercent(float value) {
        itemPercent += value;
    }

    void addDropPercent(DropType type, float value) {
        dropPercents.merge(type, value, Float::sum);
    }

    void addWardenSpoilsPercent(float value) {
        wardenSpoilsPercent += value;
    }

    void addRewardPercent(float value) {
        rewardPercent += value;
    }

    void addFleetStatPercent(ChartFleetStat stat, float value) {
        fleetStatPercents.merge(stat, value, Float::sum);
    }

    public Map<ChartFleetStat, Float> fleetStatMults() {
        if (fleetStatMults == null) {
            Map<ChartFleetStat, Float> mults = new EnumMap<>(ChartFleetStat.class);
            fleetStatPercents.forEach((stat, percent) -> {
                float mult = ChartFleetStat.multFor(percent);
                if (mult != 1f) {
                    mults.put(stat, mult);
                }
            });
            fleetStatMults = mults.isEmpty() ? Map.of() : Collections.unmodifiableMap(mults);
        }
        return fleetStatMults;
    }

    public ChartTier adjust(ChartTier base) {
        if (isEmpty()) {
            return base;
        }
        int fleetsMin = Math.max(1, base.fleetsMin() + baseFleets);
        int fleetsMax = Math.max(fleetsMin, base.fleetsMax() + baseFleets);
        return new ChartTier(base.tier(), base.fleetSize() + fleetSizePercent, fleetsMin, fleetsMax, base.officerLevelBonus() + officerLevelBonus,
                base.officerCountBonus() + extraOfficers, base.fleetQuality() + fleetQualityPercent, base.fleetStrengthMin(), base.fleetStrengthMax(),
                base.cacheBudget() * siteBudgetMult(ChartPoi.Kind.CACHE), base.domainBudget() * siteBudgetMult(ChartPoi.Kind.DOMAIN),
                base.stationBudget() * siteBudgetMult(ChartPoi.Kind.STATION), base.derelictBudget() * siteBudgetMult(ChartPoi.Kind.DERELICT),
                base.boss(), base.boss() ? Math.max(0f, base.wardenSize() + wardenSizePercent) : base.wardenSize(),
                base.clearWeight(), base.bossWeight());
    }

    private static float multiplier(float percent) {
        return Math.max(0f, 1f + percent / PERCENT);
    }

    public int extraPois() {
        return Math.max(0, extraPois);
    }

    public float fleetSizePercent() {
        return fleetSizePercent;
    }

    public int extraFleets() {
        return extraFleets;
    }

    public int officerLevelBonus() {
        return officerLevelBonus;
    }

    public int extraOfficers() {
        return extraOfficers;
    }

    public float fleetQualityPercent() {
        return fleetQualityPercent;
    }

    public float wardenSizePercent() {
        return wardenSizePercent;
    }

    private float qualityQuantityPercent() {
        return quality * QUANTITY_PERCENT_PER_QUALITY;
    }

    public float siteBudgetPercent(ChartPoi.Kind kind) {
        return siteBudgetPercents.getOrDefault(kind, 0f);
    }

    public float siteBudgetMult(ChartPoi.Kind kind) {
        return multiplier(siteBudgetPercent(kind));
    }

    private String siteBudgetText() {
        StringBuilder text = new StringBuilder();
        for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
            text.append(text.isEmpty() ? "" : " ").append(kind.name().toLowerCase(Locale.ROOT))
                    .append(String.format(Locale.ROOT, " x%.2f", siteBudgetMult(kind)));
        }
        return text.toString();
    }

    public float rewardMult() {
        return multiplier(rewardPercent + quantityPercent + qualityQuantityPercent());
    }

    public float quantityPercent() {
        return quantityPercent + qualityQuantityPercent();
    }

    public float itemPercent() {
        return itemPercent;
    }

    public float dropPercent(DropType type) {
        return dropPercents.getOrDefault(type, 0f);
    }

    public float dropMult(DropType type) {
        float percent = dropPercent(type);
        if (type.isItem()) {
            percent += itemPercent;
        }
        if (type.quantityApplies()) {
            percent += quantityPercent();
        }
        return multiplier(percent);
    }

    public float wardenSpoilsPercent() {
        return wardenSpoilsPercent;
    }

    public float wardenSpoilsMult() {
        return multiplier(wardenSpoilsPercent);
    }

    public ItemChances itemChances() {
        Map<ItemKind, Float> targetedShares = new EnumMap<>(ItemKind.class);
        dropPercents.forEach((type, percent) -> {
            if (type.itemKind() != null && percent != 0f) {
                targetedShares.put(type.itemKind(), percent / PERCENT);
            }
        });
        float generalMult = multiplier(itemPercent + quantityPercent());
        return targetedShares.isEmpty() ? ItemChances.of(generalMult) : new ItemChances(generalMult, targetedShares);
    }

    public float sensorRangeMult() {
        return Math.max(MIN_SENSOR_RANGE_MULT, Math.min(MAX_SENSOR_RANGE_MULT, 1f + sensorRangePercent / PERCENT));
    }

    void afterGeneration(ChartGenerated generated) {
        for (AppliedEffect effect : applied) {
            effect.effect().afterGeneration(generated, effect.value());
        }
    }

    public String describe() {
        String factionText = factionId == null ? "" : "faction " + factionId + ", ";
        if (isEmpty()) {
            return factionText + "no chart modifiers";
        }
        return factionText + String.format(Locale.ROOT, "fleet size %+.0f%%, %+d rolled fleet(s), %+d added fleet(s), %+d officer level(s), %+d officer(s), warden size %+.0f%%, "
                        + "fleet quality %+.0f%%, site budgets %s, %+d point(s) of interest, item drops %+.0f%%, drop types %s, "
                        + "warden spoils x%.2f, reward x%.2f, sensor range x%.2f, chart quality %d",
                fleetSizePercent, baseFleets, extraFleets, officerLevelBonus, extraOfficers, wardenSizePercent,
                fleetQualityPercent, siteBudgetText(), extraPois(), itemPercent, describeDropPercents(), wardenSpoilsMult(), rewardMult(),
                sensorRangeMult(), quality);
    }

    private String describeDropPercents() {
        if (dropPercents.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        dropPercents.forEach((type, percent) -> parts.add(String.format(Locale.ROOT, "%s %+.0f%%", type.name().toLowerCase(Locale.ROOT), percent)));
        return String.join(" ", parts);
    }
}
