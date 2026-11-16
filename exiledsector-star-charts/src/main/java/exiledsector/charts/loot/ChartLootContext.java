package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.CraftingDrops;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemRoll;
import exiledsector.loot.LootMultipliers;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public record ChartLootContext(ChartGenContext effects, int tier) {

    private static final ChartLootContext OUTSIDE_RUNS = new ChartLootContext(ChartGenContext.none(), ChartDrops.NO_TIER);

    public enum PlayerFindBonus {
        APPLIES,
        SKIPPED
    }

    public ChartLootContext {
        effects = ChartGenContext.orNone(effects);
    }

    public static ChartLootContext of(ChartRun run) {
        return run == null ? OUTSIDE_RUNS : new ChartLootContext(ChartRunEffects.contextOf(run), run.tier());
    }

    public static ChartLootContext atTier(int tier) {
        return new ChartLootContext(ChartGenContext.none(), tier);
    }

    public static ChartLootContext forFleet(CampaignFleetAPI fleet) {
        return of(fleet == null || !ChartFleetLedger.hasLootLedger(fleet) ? null : ChartFleetLedger.liveRunOf(fleet));
    }

    public static ChartLootContext at(LocationAPI location) {
        return of(ChartRuns.liveIn(location));
    }

    public static ChartLootContext outsideRuns() {
        return OUTSIDE_RUNS;
    }

    public boolean isOutsideRuns() {
        return tier == ChartDrops.NO_TIER;
    }

    public ChartLootContext withEffects(List<ChartGenContext.AppliedEffect> extraEffects) {
        ChartGenContext withExtras = effects.withEffects(extraEffects);
        return withExtras == effects ? this : new ChartLootContext(withExtras, tier);
    }

    public float bandShift() {
        return isOutsideRuns() ? CraftingDrops.NO_BAND_SHIFT : ChartTiers.bandShift(tier);
    }

    public float rarity() {
        return effects.itemRarityMult();
    }

    public float craftingBandShift() {
        return bandShift() * rarity();
    }

    public ItemChances itemChances() {
        return effects.itemChances();
    }

    public float dropMult(DropType type) {
        return effects.dropMult(type);
    }

    public float extraDropShare(DropType type) {
        return effects.extraDropShare(type);
    }

    public float carriedDropMult(DropType type) {
        return effects.carriedDropMult(type);
    }

    public float rewardMult() {
        return effects.rewardMult();
    }

    public LootMultipliers multipliers(float scale) {
        if (isOutsideRuns()) {
            return LootMultipliers.of(scale);
        }
        return new LootMultipliers(itemChances().scaled(scale), dropMult(DropType.FEEDSTOCK) * scale, dropMult(DropType.CRAFTING_ITEM) * scale,
                bandShift());
    }

    public LootMultipliers rewardMultipliers(float tierRamp) {
        float chanceMult = tierRamp * rewardMult();
        return new LootMultipliers(rewardChancesWithoutItemPercent(chanceMult), chanceMult, chanceMult, bandShift());
    }

    private ItemChances rewardChancesWithoutItemPercent(float chanceMult) {
        return ItemChances.of(chanceMult).withRarity(rarity());
    }

    public ChartDrops.Mults chartDropMults(float scale) {
        return new ChartDrops.Mults(dropMult(DropType.STAR_CHART) * scale, dropMult(DropType.CRAFTING_ITEM) * scale,
                dropMult(DropType.CATALYST) * scale);
    }

    public ChartDrops.Mults rewardChartDropMults() {
        return new ChartDrops.Mults(effects.rewardDropMult(DropType.STAR_CHART), effects.rewardDropMult(DropType.CRAFTING_ITEM),
                effects.rewardDropMult(DropType.CATALYST));
    }

    public Map<String, Integer> craftingItems(float expectedCount, Predicate<String> pool, PlayerFindBonus playerFindBonus, Random random) {
        return playerFindBonus == PlayerFindBonus.APPLIES
                ? CraftingDrops.rollFoundByPlayer(expectedCount, pool, craftingBandShift(), random)
                : CraftingDrops.roll(expectedCount, pool, craftingBandShift(), random);
    }

    public ItemData rolledWithDropLimits(ItemDefinition definition, long seed) {
        return ItemData.rolled(definition, seed, ItemRoll.dropAtRarity(rarity()));
    }

    public ItemData rolledWithAffixLimitsAtRunTier(ItemDefinition definition, long seed) {
        return ItemData.rolled(definition, seed, ItemRoll.atTier(tier).withRarity(rarity()));
    }
}
