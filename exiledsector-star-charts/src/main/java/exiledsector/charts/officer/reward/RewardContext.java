package exiledsector.charts.officer.reward;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemUnlock;

import java.util.function.Predicate;

public record RewardContext(int tier, String factionId, String commandCentreBiomeId, Predicate<String> biomeOpen,
                            Predicate<ItemDefinition> itemAllowed, Predicate<String> craftingItemAllowed, float craftingBandShift) {

    public RewardContext(int tier, String factionId, String commandCentreBiomeId, Predicate<String> biomeOpen,
                         Predicate<ItemDefinition> itemAllowed, Predicate<String> craftingItemAllowed) {
        this(tier, factionId, commandCentreBiomeId, biomeOpen, itemAllowed, craftingItemAllowed, ChartTiers.bandShift(tier));
    }

    public static RewardContext forSector(SectorAPI sector, int tier, String factionId, String commandCentreBiomeId) {
        return forSector(sector, tier, factionId, commandCentreBiomeId, ChartTiers.bandShift(tier));
    }

    private static RewardContext forSector(SectorAPI sector, int tier, String factionId, String commandCentreBiomeId, float craftingBandShift) {
        AtlasUnlocks unlocks = AtlasData.unlocksFor(sector);
        return new RewardContext(tier, factionId, commandCentreBiomeId, biomeId -> AtlasData.regionOpen(biomeId, unlocks), itemAllowedIn(sector),
                craftingItemAllowedIn(sector), craftingBandShift);
    }

    public static Predicate<ItemDefinition> itemAllowedIn(SectorAPI sector) {
        return definition -> ItemUnlock.available(definition.type(), sector) && ItemUnlock.canDrop(definition, sector);
    }

    public static Predicate<String> craftingItemAllowedIn(SectorAPI sector) {
        return itemId -> ItemUnlock.craftingItemUsable(itemId, sector);
    }

    public static RewardContext forRun(SectorAPI sector, ChartRun run, String officerFactionId) {
        return forSector(sector, run.tier(), officerFactionId, AtlasRuntime.regionOf(sector, run.atlasNode()),
                ChartRunEffects.contextOf(run).craftingBandShift(run.tier()));
    }
}
