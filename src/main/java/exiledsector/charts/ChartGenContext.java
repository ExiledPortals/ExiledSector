package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.items.Item;
import exiledsector.items.SlottedRoll;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ScaleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChartGenContext {

    static final float VAULT_BASE_DROP_VALUE = 20000f;
    static final String VAULT_DROP_GROUP = "basic";
    static final float MIN_SENSOR_RANGE_MULT = 0.1f;
    static final float MAX_SENSOR_RANGE_MULT = 2f;
    static final float PERCENT = 100f;

    public record AppliedEffect(ChartEffect effect, float value) {
    }

    private static final ChartGenContext NONE = new ChartGenContext(List.of());

    private final List<AppliedEffect> applied;
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

    private ChartGenContext(List<AppliedEffect> applied) {
        this.applied = List.copyOf(applied);
        for (AppliedEffect effect : this.applied) {
            effect.effect().beforeGeneration(this, effect.value());
        }
    }

    public static ChartGenContext none() {
        return NONE;
    }

    public static ChartGenContext of(List<AppliedEffect> applied) {
        return applied == null || applied.isEmpty() ? NONE : new ChartGenContext(applied);
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
                float magnitude = roll.roll().magnitude();
                applied.add(new AppliedEffect(effect, roll.line() == null ? magnitude : roll.line().valueFor(magnitude, scale)));
            }
        }
        return of(applied);
    }

    public List<AppliedEffect> applied() {
        return applied;
    }

    public boolean isEmpty() {
        return applied.isEmpty();
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
        int fleetsMin = Math.max(1, base.fleetsMin() + extraFleets);
        int fleetsMax = Math.max(fleetsMin, base.fleetsMax() + extraFleets);
        float averageFleets = Math.max(1f, (base.fleetsMin() + base.fleetsMax()) / 2f);
        float fleetCountMult = Math.max(0f, 1f + extraFleets / averageFleets);
        float fleetPower = base.fleetPower() * multiplier(fleetPowerPercent) * fleetCountMult;
        return new ChartTier(base.tier(), fleetPower, fleetsMin, fleetsMax, base.officerLevelBonus() + officerLevelBonus,
                base.officerCountBonus() + extraOfficers, base.quality() + fleetQualityPercent / PERCENT, base.averageSMods() + averageSModsBonus,
                base.treePercentMin(), base.treePercentMax(), base.lootBudget() * lootMult(), base.boss(),
                base.bossPower() * multiplier(bossPowerPercent), base.clearWeight(), base.bossWeight(), base.vaultWeight());
    }

    private static float multiplier(float percent) {
        return Math.max(0f, 1f + percent / PERCENT);
    }

    public int extraPois() {
        return Math.max(0, extraPois);
    }

    public float lootMult() {
        return multiplier(lootBudgetPercent + quantityPercent);
    }

    public float rewardMult() {
        return multiplier(rewardPercent + quantityPercent);
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
        if (generated.pocket() == null) {
            return;
        }
        String vaultId = generated.content() == null ? null : generated.content().vaultEntityId();
        for (SectorEntityToken entity : generated.pocket().getEntitiesWithTag(ChartIds.CONTENT_TAG)) {
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
        if (vaultId == null || generated.pocket() == null || richnessPercent <= 0f) {
            return;
        }
        SectorEntityToken vault = generated.pocket().getEntityById(vaultId);
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
                        + "quality %+.0f%%, loot x%.2f, %+d point(s) of interest, hull item chance x%.2f, vault x%.2f, reward x%.2f, sensor range x%.2f",
                multiplier(fleetPowerPercent), extraFleets, officerLevelBonus, extraOfficers, averageSModsBonus, multiplier(bossPowerPercent),
                fleetQualityPercent, lootMult(), extraPois(), itemChanceMult(), multiplier(vaultRichnessPercent), rewardMult(), sensorRangeMult());
    }
}
