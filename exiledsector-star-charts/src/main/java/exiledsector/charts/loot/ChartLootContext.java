package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.drop.CraftingDrops;
import exiledsector.items.drop.ItemChances;
import exiledsector.items.modifier.ItemRoll;
import exiledsector.loot.DropType;
import exiledsector.loot.LootMultipliers;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public record ChartLootContext(ChartGenContext effects, int tier) {

    private static final ChartLootContext OUTSIDE_RUNS = new ChartLootContext(ChartGenContext.none(), ChartDrops.NO_TIER);

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

    public ChartLootContext plusQuantityPercent(float increasedPercent) {
        return withContext(effects.plusQuantityPercent(increasedPercent));
    }

    public ChartLootContext forFleetLoot(boolean boss) {
        return withContext(effects.forFleetLoot(boss));
    }

    public ChartLootContext withEffects(List<ChartGenContext.AppliedEffect> extraEffects) {
        return withContext(effects.withEffects(extraEffects));
    }

    private ChartLootContext withContext(ChartGenContext derived) {
        return derived == effects ? this : new ChartLootContext(derived, tier);
    }

    public float bandShift() {
        return isOutsideRuns() ? CraftingDrops.NO_BAND_SHIFT : ChartTiers.bandShift(tier);
    }

    public float rarity(DropType type) {
        return itemChances().rarity(type);
    }

    public float craftingBandShift(DropType craftingSubcategory) {
        return multipliers(1f).craftingBandShift(craftingSubcategory);
    }

    public ItemChances itemChances() {
        return effects.itemChances();
    }

    public float extraDropShare(DropType type) {
        return effects.extraDropShare(type);
    }

    public LootMultipliers multipliers(float scale) {
        return new LootMultipliers(itemChances().scaled(scale), bandShift());
    }

    public float quantityMult(DropType type) {
        return itemChances().quantity(type);
    }

    public float quantityMult(DropType type, float extraIncreasedPercent) {
        return itemChances().plusPercent(type, extraIncreasedPercent).quantity(type);
    }

    public ChartDrops.Mults chartDropMults(float scale) {
        return new ChartDrops.Mults(quantityMult(DropType.STAR_CHART) * scale, quantityMult(DropType.BASIC_CRAFTING_ITEM) * scale, quantityMult(DropType.CATALYST) * scale);
    }

    public Map<String, Integer> craftingItems(float baseCount, Predicate<String> pool, Random random) {
        return CraftingDrops.roll(baseCount, pool, multipliers(1f), random);
    }

    public CraftingDrops.Split craftingSplit(Predicate<String> pool) {
        return CraftingDrops.split(pool, multipliers(1f));
    }

    public ItemData rolledWithDropLimits(ItemDefinition definition, long seed) {
        return ItemData.rolled(definition, seed, ItemRoll.dropAtRarity(rarity(DropType.forDefinition(definition))));
    }
}
