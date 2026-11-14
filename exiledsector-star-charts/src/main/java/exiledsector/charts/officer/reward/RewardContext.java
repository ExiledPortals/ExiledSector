package exiledsector.charts.officer.reward;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemUnlock;

import java.util.function.Predicate;

public record RewardContext(int tier, String factionId, String commandCentreBiomeId, Predicate<String> biomeOpen,
                            Predicate<ItemDefinition> itemAllowed, Predicate<String> craftingItemAllowed) {

    public static RewardContext forSector(SectorAPI sector, int tier, String factionId, String commandCentreBiomeId) {
        AtlasUnlocks unlocks = AtlasData.unlocksFor(sector);
        return new RewardContext(tier, factionId, commandCentreBiomeId, biomeId -> AtlasData.regionOpen(biomeId, unlocks), itemAllowedIn(sector),
                craftingItemAllowedIn(sector));
    }

    public static Predicate<ItemDefinition> itemAllowedIn(SectorAPI sector) {
        return definition -> ItemUnlock.available(definition.type(), sector) && ItemUnlock.canDrop(definition, sector);
    }

    public static Predicate<String> craftingItemAllowedIn(SectorAPI sector) {
        return itemId -> ItemUnlock.craftingItemUsable(itemId, sector);
    }

    public static RewardContext forRun(SectorAPI sector, ChartRun run, String officerFactionId) {
        return forSector(sector, run.tier(), officerFactionId, AtlasRuntime.regionOf(sector, run.atlasNode()));
    }
}
