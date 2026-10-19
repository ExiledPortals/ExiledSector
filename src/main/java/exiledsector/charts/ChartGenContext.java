package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.items.Item;
import exiledsector.items.SlottedRoll;
import exiledsector.items.StarChart;
import exiledsector.modifiers.ModifierEffect;
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
    private float quantityPercent;
    private float lootBudgetPercent;
    private int extraPois;
    private float itemChancePercent;
    private float vaultRichnessPercent;
    private float rewardPercent;

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
        int fleetsMin = Math.max(1, base.fleetsMin() + baseFleets);
        int fleetsMax = Math.max(fleetsMin, base.fleetsMax() + baseFleets);
        return new ChartTier(base.tier(), base.fleetSize() + fleetSizePercent, fleetsMin, fleetsMax, base.officerLevelBonus() + officerLevelBonus,
                base.officerCountBonus() + extraOfficers, base.fleetQuality() + fleetQualityPercent, base.fleetStrengthMin(), base.fleetStrengthMax(),
                base.lootBudget() * lootMult(), base.boss(), base.boss() ? Math.max(0f, base.wardenSize() + wardenSizePercent) : base.wardenSize(),
                base.clearWeight(), base.bossWeight(), base.vaultWeight());
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
        String factionText = factionId == null ? "" : "faction " + factionId + ", ";
        if (isEmpty()) {
            return factionText + "no chart modifiers";
        }
        return factionText + String.format(Locale.ROOT, "fleet size %+.0f%%, %+d rolled fleet(s), %+d added fleet(s), %+d officer level(s), %+d officer(s), warden size %+.0f%%, "
                        + "fleet quality %+.0f%%, loot x%.2f (x%.2f without a vault), %+d point(s) of interest, hull item chance x%.2f, vault x%.2f, "
                        + "reward x%.2f, sensor range x%.2f, chart quality %d",
                fleetSizePercent, baseFleets, extraFleets, officerLevelBonus, extraOfficers, wardenSizePercent,
                fleetQualityPercent, lootMult(), lootMultWithoutVault(), extraPois(), itemChanceMult(), multiplier(vaultRichnessPercent), rewardMult(),
                sensorRangeMult(), quality);
    }
}
