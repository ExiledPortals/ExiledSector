package exiledsector.charts.generation;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.effect.ChartBattleRules;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.effect.ChartFactionEffect;
import exiledsector.charts.effect.ChartFleetStat;
import exiledsector.charts.effect.ChartPrefixRewards;
import exiledsector.compat.FirstFailureLog;
import exiledsector.items.Item;
import exiledsector.items.drop.ItemChances;
import exiledsector.items.modifier.SlottedRoll;
import exiledsector.items.starchart.StarChart;
import exiledsector.loot.DropType;
import exiledsector.modifiers.ModifierEffect;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.ScaleContext;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ChartGenContext {

    static final float MIN_SENSOR_RANGE_MULT = 0.1f;
    static final float MAX_SENSOR_RANGE_MULT = 2f;
    public static final float PERCENT = 100f;
    static final float QUANTITY_PERCENT_PER_QUALITY = 1f;
    private static final FirstFailureLog UNRESOLVED_ROLLS = FirstFailureLog.warnings(Logger.getLogger(ChartGenContext.class));

    public record AppliedEffect(ChartModifier effect, float value) {
    }

    private static final ChartGenContext NONE = new ChartGenContext(List.of(), List.of(), 0, null);

    private final List<AppliedEffect> chartLines;
    private final List<AppliedEffect> addedLines;
    private final float modifierEffectPercent;
    private final List<AppliedEffect> applied;
    private final int quantity;
    private final String factionId;
    private float fleetSizePercent;
    private int baseFleets;
    private int extraFleets;
    private int officerLevelBonus;
    private int extraOfficers;
    private float bossSizePercent;
    private float fleetQualityPercent;
    private float sensorRangePercent;
    private float enemySensorRangeBonus;
    private boolean twinnedBoss;
    private final Set<HullSize> bannedCombatHullSizes = EnumSet.noneOf(HullSize.class);
    private boolean shipSystemsDisabled;
    private boolean autopilotForced;
    private float arcStrikeEmp;
    private float weaponMisfireRate;
    private float damagePercentPerSMod;
    private final Set<String> enemyGrantedNodeIds = new LinkedHashSet<>();
    private float battleSizeBonus;
    private final Map<ChartPoi.Kind, Float> siteBudgetPercents = new EnumMap<>(ChartPoi.Kind.class);
    private int extraPois;
    private final Map<DropType, Float> dropPercents = new EnumMap<>(DropType.class);
    private final Map<DropType, Float> rarityPercents = new EnumMap<>(DropType.class);
    private float bossSpoilsPercent;
    private float bossStarChartPercent;
    private final Map<ChartFleetStat, Float> fleetStatPercents = new EnumMap<>(ChartFleetStat.class);
    private Map<ChartFleetStat, Float> fleetStatMults;
    private final Map<String, Float> anomalyPercents = new LinkedHashMap<>();
    private final List<String> forcedAnomalies = new ArrayList<>();
    private boolean stationBossBarred;
    private String biomeId;
    private final Map<String, String> anomalyData = new LinkedHashMap<>();

    private ChartGenContext(List<AppliedEffect> chartLines, List<AppliedEffect> addedLines, int quantity, String factionId) {
        this.chartLines = List.copyOf(chartLines);
        this.addedLines = List.copyOf(addedLines);
        this.modifierEffectPercent = totalOf(ChartModifier.CHART_MODIFIER_EFFECT_PERCENT, this.addedLines);
        this.applied = scaledPlusAdded(this.chartLines, multiplier(modifierEffectPercent), this.addedLines);
        this.quantity = Math.max(0, quantity);
        this.factionId = factionId;
        for (AppliedEffect effect : this.applied) {
            effect.effect().beforeGeneration(this, effect.value());
        }
    }

    public static ChartGenContext none() {
        return NONE;
    }

    public static ChartGenContext orNone(ChartGenContext context) {
        return context == null ? NONE : context;
    }

    public static ChartGenContext of(List<AppliedEffect> applied) {
        return of(applied, 0);
    }

    public static ChartGenContext of(List<AppliedEffect> applied, int quantity) {
        return of(applied, quantity, null);
    }

    public static ChartGenContext of(List<AppliedEffect> applied, int quantity, String factionId) {
        boolean noEffects = applied == null || applied.isEmpty();
        if (noEffects && quantity <= 0 && factionId == null) {
            return NONE;
        }
        return new ChartGenContext(List.of(), noEffects ? List.of() : applied, quantity, factionId);
    }

    private static float totalOf(ChartModifier effect, List<AppliedEffect> lines) {
        float total = 0f;
        for (AppliedEffect line : lines) {
            if (line.effect() == effect) {
                total += line.value();
            }
        }
        return total;
    }

    private static List<AppliedEffect> scaledPlusAdded(List<AppliedEffect> chartLines, float chartLineMult, List<AppliedEffect> addedLines) {
        List<AppliedEffect> lines = new ArrayList<>(chartLines.size() + addedLines.size());
        for (AppliedEffect line : chartLines) {
            lines.add(chartLineMult == 1f ? line : new AppliedEffect(line.effect(), line.effect().withModifierEffect(line.value(), chartLineMult)));
        }
        lines.addAll(addedLines);
        return List.copyOf(lines);
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
            ModifierEffect rolledLine = roll.roll().effect(effects);
            if (rolledLine instanceof ChartFactionEffect faction) {
                factionId = factionId == null ? faction.factionId() : factionId;
            } else if (rolledLine instanceof ChartModifier effect) {
                if (roll.line() == null) {
                    reportUnresolved(chart, effect);
                } else {
                    applied.add(new AppliedEffect(effect, roll.line().valueFor(roll.roll().magnitude(), scale)));
                }
            }
        }
        chart.explicits().stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX)
                .forEach(prefix -> applied.addAll(ChartPrefixRewards.rewards(prefix.modifierId(), chart.tier())));
        int chartQuantity = chart instanceof StarChart starChart ? starChart.quantity() : 0;
        if (applied.isEmpty() && chartQuantity <= 0 && factionId == null) {
            return NONE;
        }
        return new ChartGenContext(applied, List.of(), chartQuantity, factionId);
    }

    private static void reportUnresolved(Item chart, ChartModifier effect) {
        UNRESOLVED_ROLLS.report(chart.definitionId() + "/" + effect.name(), "[ExiledSector] Star Chart " + chart.definitionId() + " has a "
                + effect.name() + " roll whose modifier is no longer defined; the roll is ignored", null);
    }

    public List<AppliedEffect> applied() {
        return applied;
    }

    public float modifierEffectPercent() {
        return modifierEffectPercent;
    }

    public int quantity() {
        return quantity;
    }

    public String factionId() {
        return factionId;
    }

    public boolean isEmpty() {
        return applied.isEmpty() && quantity == 0;
    }

    public void addFleetSizePercent(float value) {
        fleetSizePercent += value;
    }

    public void addBaseFleets(int value) {
        baseFleets += value;
    }

    public void addExtraFleets(int value) {
        extraFleets += value;
    }

    public void addOfficerLevelBonus(int value) {
        officerLevelBonus += value;
    }

    public void addExtraOfficers(int value) {
        extraOfficers += value;
    }

    public void addBossSizePercent(float value) {
        bossSizePercent += value;
    }

    public void addFleetQualityPercent(float value) {
        fleetQualityPercent += value;
    }

    public void addSensorRangePercent(float value) {
        sensorRangePercent += value;
    }

    public void addEnemySensorRangeBonus(float value) {
        enemySensorRangeBonus += value;
    }

    public float enemySensorRangeBonus() {
        return enemySensorRangeBonus;
    }

    public void twinBoss() {
        twinnedBoss = true;
    }

    public boolean twinnedBoss() {
        return twinnedBoss;
    }

    public void banCombatHullSize(HullSize hullSize) {
        bannedCombatHullSizes.add(hullSize);
    }

    public Set<HullSize> bannedCombatHullSizes() {
        return Collections.unmodifiableSet(bannedCombatHullSizes);
    }

    public void disableShipSystems() {
        shipSystemsDisabled = true;
    }

    public void forceAutopilot() {
        autopilotForced = true;
    }

    public void addArcStrikeEmp(float emp) {
        arcStrikeEmp += emp;
    }

    public void addWeaponMisfireSeconds(float seconds) {
        if (seconds > 0f) {
            weaponMisfireRate += 1f / seconds;
        }
    }

    public void addDamagePercentPerSMod(float percent) {
        damagePercentPerSMod += percent;
    }

    public void grantEnemyNode(String nodeId) {
        enemyGrantedNodeIds.add(nodeId);
    }

    public Set<String> enemyGrantedNodeIds() {
        return Collections.unmodifiableSet(enemyGrantedNodeIds);
    }

    public void addBattleSizeBonus(float value) {
        battleSizeBonus += value;
    }

    public ChartBattleRules battleRules() {
        return new ChartBattleRules(Math.round(battleSizeBonus), shipSystemsDisabled, autopilotForced, Math.max(0f, arcStrikeEmp), weaponMisfireRate,
                damagePercentPerSMod);
    }

    public void addSiteBudgetPercent(ChartPoi.Kind kind, float value) {
        siteBudgetPercents.merge(kind, value, Float::sum);
    }

    public void addAllSiteBudgetsPercent(float value) {
        for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
            addSiteBudgetPercent(kind, value);
        }
    }

    public void addExtraPois(int value) {
        extraPois += value;
    }

    public void addRarityPercent(DropType type, float value) {
        rarityPercents.merge(type, value, Float::sum);
    }

    public void addDropPercent(DropType type, float value) {
        dropPercents.merge(type, value, Float::sum);
    }

    public void addBossSpoilsPercent(float value) {
        bossSpoilsPercent += value;
    }

    public void addBossStarChartPercent(float value) {
        bossStarChartPercent += value;
    }

    public void addFleetStatPercent(ChartFleetStat stat, float value) {
        fleetStatPercents.merge(stat, value, Float::sum);
    }

    public void addAnomalyPercent(String tuningKey, float value) {
        anomalyPercents.merge(tuningKey, value, Float::sum);
    }

    public ChartGenContext withAtlasNode(AtlasRunNode node) {
        if (node == null) {
            return this;
        }
        ChartGenContext context = derive(addedPlus(node.effects()), node.factionId().isEmpty() ? factionId : node.factionId());
        context.forceAnomalies(node.anomalyIds());
        context.anomalyData.putAll(node.anomalyData());
        return context;
    }

    public ChartGenContext withAnomalies(List<String> anomalyIds) {
        if (anomalyIds == null || anomalyIds.isEmpty()) {
            return this;
        }
        ChartGenContext context = derive(addedLines, factionId);
        context.forceAnomalies(anomalyIds);
        return context;
    }

    public ChartGenContext withAnomalyEffects(List<String> anomalyIds) {
        if (anomalyIds == null || anomalyIds.isEmpty()) {
            return this;
        }
        List<AppliedEffect> effects = new ArrayList<>(ChartAnomalies.effectsOf(anomalyIds));
        effects.addAll(ChartAnomalies.effectsInRun(anomalyIds, this));
        return effects.isEmpty() ? this : derive(addedPlus(effects), factionId);
    }

    public ChartGenContext withEffects(List<AppliedEffect> effects) {
        return effects == null || effects.isEmpty() ? this : derive(addedPlus(effects), factionId);
    }

    public ChartGenContext plusQuantityPercent(float increasedPercent) {
        return increasedPercent == 0f ? this : withEffects(List.of(new AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, increasedPercent)));
    }

    public ChartGenContext forFleetLoot(boolean boss) {
        return boss ? plusQuantityPercent(bossSpoilsPercent) : this;
    }

    private List<AppliedEffect> addedPlus(List<AppliedEffect> effects) {
        List<AppliedEffect> combined = new ArrayList<>(addedLines);
        combined.addAll(effects);
        return combined;
    }

    private ChartGenContext derive(List<AppliedEffect> derivedAddedLines, String derivedFactionId) {
        ChartGenContext context = new ChartGenContext(chartLines, derivedAddedLines, quantity, derivedFactionId);
        context.forceAnomalies(forcedAnomalies);
        context.stationBossBarred = stationBossBarred;
        context.biomeId = biomeId;
        context.anomalyData.putAll(anomalyData);
        return context;
    }

    private void forceAnomalies(List<String> anomalyIds) {
        for (String anomalyId : anomalyIds) {
            if (!forcedAnomalies.contains(anomalyId)) {
                forcedAnomalies.add(anomalyId);
            }
        }
    }

    public List<String> forcedAnomalies() {
        return List.copyOf(forcedAnomalies);
    }

    public ChartGenContext withStationBossBarred(boolean barred) {
        if (barred == stationBossBarred) {
            return this;
        }
        ChartGenContext context = derive(addedLines, factionId);
        context.stationBossBarred = barred;
        return context;
    }

    public boolean stationBossBarred() {
        return stationBossBarred;
    }

    public ChartGenContext withBiome(String regionId) {
        if (Objects.equals(regionId, biomeId)) {
            return this;
        }
        ChartGenContext context = derive(addedLines, factionId);
        context.biomeId = regionId;
        return context;
    }

    public String biomeId() {
        return biomeId;
    }

    public ChartGenContext withAnomalyData(String anomalyId, String data) {
        if (data == null || data.equals(anomalyData.get(anomalyId))) {
            return this;
        }
        ChartGenContext context = derive(addedLines, factionId);
        context.anomalyData.put(anomalyId, data);
        return context;
    }

    public String anomalyData(String anomalyId) {
        return anomalyData.get(anomalyId);
    }

    public float anomalyPercent(String tuningKey) {
        return anomalyPercents.getOrDefault(tuningKey, 0f);
    }

    public float anomalyMult(String tuningKey) {
        return multiplier(anomalyPercent(tuningKey));
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
                base.officerCountBonus() + extraOfficers, base.fleetQuality() + fleetQualityPercent, base.levelMin(), base.levelMax(),
                base.cacheBudget() * siteBudgetMult(ChartPoi.Kind.CACHE), base.domainBudget() * siteBudgetMult(ChartPoi.Kind.DOMAIN),
                base.stationBudget() * siteBudgetMult(ChartPoi.Kind.STATION), base.derelictBudget() * siteBudgetMult(ChartPoi.Kind.DERELICT),
                base.boss(), base.boss() ? Math.max(0f, base.bossSize() + bossSizePercent) : base.bossSize());
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

    public float bossSizePercent() {
        return bossSizePercent;
    }

    private float qualityQuantityPercent() {
        return quantity * QUANTITY_PERCENT_PER_QUALITY;
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

    public float quantityPercent() {
        return dropPercent(DropType.ITEM);
    }

    public float itemRarityPercent() {
        return rarityPercent(DropType.ITEM);
    }

    public float rarityPercent(DropType type) {
        return rarityPercents.getOrDefault(type, 0f);
    }

    public float dropPercent(DropType type) {
        float percent = dropPercents.getOrDefault(type, 0f);
        return type == DropType.ITEM ? percent + qualityQuantityPercent() : percent;
    }

    public float dropMult(DropType type) {
        return itemChances().quantity(type);
    }

    public float extraDropShare(DropType type) {
        return Math.max(0f, dropMult(type) - 1f);
    }

    public float bossSpoilsPercent() {
        return bossSpoilsPercent;
    }

    public float bossStarChartPercent() {
        return bossStarChartPercent;
    }

    public float bossStarChartMult(float extraPercent) {
        return itemChances().plusPercent(DropType.STAR_CHART, bossStarChartPercent + extraPercent).quantity(DropType.STAR_CHART);
    }

    public ItemChances itemChances() {
        Map<DropType, Float> nodePercents = new EnumMap<>(DropType.class);
        for (DropType node : DropType.values()) {
            float percent = dropPercent(node);
            if (percent != 0f) {
                nodePercents.put(node, percent);
            }
        }
        return new ItemChances(nodePercents, rarityPercents, 1f);
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
        return factionText + String.format(Locale.ROOT, "fleet size %+.0f%%, %+d rolled fleet(s), %+d added fleet(s), %+d officer level(s), %+d officer(s), boss size %+.0f%%, "
                        + "fleet quality %+.0f%%, site budgets %s, %+d point(s) of interest, item rarity %+.0f%%, drop types %s, "
                        + "boss spoils %+.0f%%, sensor range x%.2f, chart quality %d, anomalies %s, twinned boss %b, "
                        + "barred combat hulls %s, ship systems disabled %b, autopilot forced %b, arc strike EMP %.0f, weapon misfires %.3f/s, damage per S-mod %+.0f%%, enemy node grants %s",
                fleetSizePercent, baseFleets, extraFleets, officerLevelBonus, extraOfficers, bossSizePercent,
                fleetQualityPercent, siteBudgetText(), extraPois(), itemRarityPercent(), describeDropPercents(), bossSpoilsPercent,
                sensorRangeMult(), quantity, forcedAnomalies.isEmpty() ? "none" : forcedAnomalies.toString(), twinnedBoss,
                bannedCombatHullSizes.isEmpty() ? "none" : bannedCombatHullSizes.toString(), shipSystemsDisabled, autopilotForced, battleRules().arcStrikeEmp(), weaponMisfireRate,
                damagePercentPerSMod, enemyGrantedNodeIds.isEmpty() ? "none" : enemyGrantedNodeIds.toString());
    }

    private String describeDropPercents() {
        List<String> parts = new ArrayList<>();
        for (DropType type : DropType.values()) {
            float percent = dropPercent(type);
            if (percent != 0f) {
                parts.add(String.format(Locale.ROOT, "%s %+.0f%%", type.name().toLowerCase(Locale.ROOT), percent));
            }
        }
        if (parts.isEmpty()) {
            return "none";
        }
        return String.join(" ", parts);
    }
}
