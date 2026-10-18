package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.items.Item;
import exiledsector.items.SlottedRoll;
import exiledsector.items.StarChart;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ScaleContext;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ChartGenContext {

    static final float VAULT_BASE_DROP_VALUE = 20000f;
    static final String VAULT_DROP_GROUP = "basic";
    static final float MIN_SENSOR_RANGE_MULT = 0.1f;
    static final float MAX_SENSOR_RANGE_MULT = 2f;
    static final float PERCENT = 100f;
    static final float QUANTITY_PERCENT_PER_QUALITY = 1f;
    private static final Logger LOG = Logger.getLogger(ChartGenContext.class);
    private static final Set<String> REPORTED_UNRESOLVED_ROLLS = ConcurrentHashMap.newKeySet();

    public record AppliedEffect(ChartEffect effect, float value) {
    }

    private static final ChartGenContext NONE = new ChartGenContext(List.of(), 0);

    private final List<AppliedEffect> applied;
    private final int quality;
    private float fleetPowerPercent;
    private int extraFleets;
    private int officerLevelBonus;
    private int extraOfficers;
    private int averageSModsBonus;
    private float bossPowerPercent;
    private float fleetQualityPercent;
    private float sensorRangePercent;
    private float quantityPercent;
    private float lootBudgetPercent;
    private int extraPois;
    private float itemChancePercent;
    private float vaultRichnessPercent;
    private float rewardPercent;

    private ChartGenContext(List<AppliedEffect> applied, int quality) {
        this.applied = List.copyOf(applied);
        this.quality = Math.max(0, quality);
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
        boolean noEffects = applied == null || applied.isEmpty();
        if (noEffects && quality <= 0) {
            return NONE;
        }
        return new ChartGenContext(noEffects ? List.of() : applied, quality);
    }

    public static ChartGenContext from(Item chart) {
        if (chart == null) {
            return none();
        }
        ModifierEffects effects = chart.modifierEffects();
        ScaleContext scale = chart.scaleContext(null);
        List<AppliedEffect> applied = new ArrayList<>();
        for (SlottedRoll roll : chart.rolls()) {
            if (roll.roll().effect(effects) instanceof ChartEffect effect) {
                if (roll.line() == null) {
                    reportUnresolved(chart, effect);
                } else {
                    applied.add(new AppliedEffect(effect, roll.line().valueFor(roll.roll().magnitude(), scale)));
                }
            }
        }
        return of(applied, chart instanceof StarChart starChart ? starChart.quality() : 0);
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

    public boolean isEmpty() {
        return applied.isEmpty() && quality == 0;
    }

    void addFleetPowerPercent(float value) {
        fleetPowerPercent += value;
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

    void addAverageSModsBonus(int value) {
        averageSModsBonus += value;
    }

    void addBossPowerPercent(float value) {
        bossPowerPercent += value;
    }

    void addFleetQualityPercent(float value) {
        fleetQualityPercent += value;
    }

    void addSensorRangePercent(float value) {
        sensorRangePercent += value;
    }

    void addQuantityPercent(float value) {
        quantityPercent += value;
    }

    void addLootBudgetPercent(float value) {
        lootBudgetPercent += value;
    }

    void addExtraPois(int value) {
        extraPois += value;
    }

    void addItemChancePercent(float value) {
        itemChancePercent += value;
    }

    void addVaultRichnessPercent(float value) {
        vaultRichnessPercent += value;
    }

    void addRewardPercent(float value) {
        rewardPercent += value;
    }

    public ChartTier adjust(ChartTier base) {
        if (isEmpty()) {
            return base;
        }
        int fleetsMin = Math.min(ChartContentPlanner.MAX_FLEETS, Math.max(1, base.fleetsMin() + extraFleets));
        int fleetsMax = Math.min(ChartContentPlanner.MAX_FLEETS, Math.max(fleetsMin, base.fleetsMax() + extraFleets));
        float averageFleets = Math.max(1f, (base.fleetsMin() + base.fleetsMax()) / 2f);
        float fleetCountMult = Math.max(0f, 1f + extraFleets / averageFleets);
        float fleetPower = base.fleetPower() * multiplier(fleetPowerPercent) * fleetCountMult;
        return new ChartTier(base.tier(), fleetPower, fleetsMin, fleetsMax, base.officerLevelBonus() + officerLevelBonus,
                base.officerCountBonus() + extraOfficers, base.quality() + fleetQualityPercent / PERCENT, base.averageSMods() + averageSModsBonus,
                base.treePercentMin(), base.treePercentMax(), base.lootBudget() * lootMult(), base.boss(), base.bossPower(), base.clearWeight(),
                base.bossWeight(), base.vaultWeight());
    }

    private static float multiplier(float percent) {
        return Math.max(0f, 1f + percent / PERCENT);
    }

    public int extraPois() {
        return Math.max(0, extraPois);
    }

    public float fleetPowerMult() {
        return multiplier(fleetPowerPercent);
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

    public int averageSModsBonus() {
        return averageSModsBonus;
    }

    public float fleetQualityPercent() {
        return fleetQualityPercent;
    }

    public float bossPowerMult() {
        return multiplier(bossPowerPercent);
    }

    private float qualityQuantityPercent() {
        return quality * QUANTITY_PERCENT_PER_QUALITY;
    }

    public float lootMult() {
        return multiplier(lootBudgetPercent + quantityPercent + qualityQuantityPercent());
    }

    public float chartDropMult() {
        return multiplier(quantityPercent + qualityQuantityPercent());
    }

    public float lootMultWithoutVault() {
        return multiplier(lootBudgetPercent + quantityPercent + qualityQuantityPercent() + vaultRichnessPercent);
    }

    public float lootBudgetWithoutVault(ChartTier base) {
        return isEmpty() ? base.lootBudget() : base.lootBudget() * lootMultWithoutVault();
    }

    public float rewardMult() {
        return multiplier(rewardPercent + quantityPercent + qualityQuantityPercent());
    }

    public float itemChanceMult() {
        return multiplier(itemChancePercent);
    }

    public float vaultItemChanceMult() {
        return itemChanceMult() * multiplier(vaultRichnessPercent);
    }

    public float vaultRichnessPercent() {
        return vaultRichnessPercent;
    }

    public float sensorRangeMult() {
        return Math.max(MIN_SENSOR_RANGE_MULT, Math.min(MAX_SENSOR_RANGE_MULT, 1f + sensorRangePercent / PERCENT));
    }

    void afterGeneration(ChartGenerated generated) {
        for (AppliedEffect effect : applied) {
            effect.effect().afterGeneration(generated, effect.value());
        }
    }

    static void markSalvageChances(ChartGenerated generated) {
        if (generated.system() == null) {
            return;
        }
        String vaultId = generated.content() == null ? null : generated.content().vaultEntityId();
        for (SectorEntityToken entity : generated.system().getEntitiesWithTag(ChartIds.CONTENT_TAG)) {
            if (entity instanceof CampaignFleetAPI || !ChartIds.belongsToRun(entity, generated.runId())) {
                continue;
            }
            float chanceMult = entity.getId() != null && entity.getId().equals(vaultId)
                    ? generated.context().vaultItemChanceMult() : generated.context().itemChanceMult();
            MemoryAPI memory = entity.getMemoryWithoutUpdate();
            if (memory != null && chanceMult != 1f) {
                memory.set(ChartIds.ITEM_CHANCE_MEMORY_KEY, chanceMult);
            }
        }
    }

    static void enrichVault(ChartGenerated generated, float richnessPercent) {
        String vaultId = generated.content() == null ? null : generated.content().vaultEntityId();
        if (vaultId == null || generated.system() == null || richnessPercent <= 0f) {
            return;
        }
        SectorEntityToken vault = generated.system().getEntityById(vaultId);
        int extraValue = Math.round(VAULT_BASE_DROP_VALUE * richnessPercent / PERCENT);
        if (vault != null && extraValue > 0) {
            vault.addDropValue(VAULT_DROP_GROUP, extraValue);
        }
    }

    public String describe() {
        if (isEmpty()) {
            return "no chart modifiers";
        }
        return String.format(Locale.ROOT, "fleet power x%.2f, %+d fleet(s), %+d officer level(s), %+d officer(s), %+d S-mod(s), boss power x%.2f, "
                        + "quality %+.0f%%, loot x%.2f (x%.2f without a vault), %+d point(s) of interest, hull item chance x%.2f, vault x%.2f, "
                        + "reward x%.2f, sensor range x%.2f, chart quality %d",
                multiplier(fleetPowerPercent), extraFleets, officerLevelBonus, extraOfficers, averageSModsBonus, bossPowerMult(),
                fleetQualityPercent, lootMult(), lootMultWithoutVault(), extraPois(), itemChanceMult(), multiplier(vaultRichnessPercent), rewardMult(),
                sensorRangeMult(), quality);
    }
}
