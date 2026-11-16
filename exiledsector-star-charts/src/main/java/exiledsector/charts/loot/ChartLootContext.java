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
import exiledsector.items.ItemQuantity;
import exiledsector.items.ItemRoll;
import exiledsector.loot.LootMultipliers;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;

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

    public LootMultipliers multipliers(float scale) {
        if (isOutsideRuns()) {
            return LootMultipliers.of(scale);
        }
        ItemQuantity chartCraftingItems = ItemQuantity.increasedBy(effects.totalDropPercent(DropType.CRAFTING_ITEM)).scaled(scale);
        return new LootMultipliers(itemChances().scaled(scale), dropMult(DropType.FEEDSTOCK) * scale, chartCraftingItems, bandShift());
    }

    public float craftingItemMult(float extraIncreasedPercent) {
        return ItemQuantity.increasedBy(effects.totalDropPercent(DropType.CRAFTING_ITEM) + extraIncreasedPercent + FoundItemQuantity.craftingItemPercent())
                .mult();
    }

    public ChartDrops.Mults chartDropMults(float scale) {
        return new ChartDrops.Mults(dropMult(DropType.STAR_CHART) * scale, craftingItemMult(0f) * scale, dropMult(DropType.CATALYST) * scale);
    }

    public Map<String, Integer> craftingItems(float expectedCount, Predicate<String> pool, Random random) {
        return CraftingDrops.roll(expectedCount, pool, craftingBandShift(), random);
    }

    public ItemData rolledWithDropLimits(ItemDefinition definition, long seed) {
        return ItemData.rolled(definition, seed, ItemRoll.dropAtRarity(rarity()));
    }
}
