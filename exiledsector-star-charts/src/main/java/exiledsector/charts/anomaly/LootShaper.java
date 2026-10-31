package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemDrops;

import java.util.List;
import java.util.Random;

public interface LootShaper extends ChartAnomaly {

    default List<String> lootScopes() {
        return List.of();
    }

    default ItemDrops.SiteLoot fleetItems(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float share, Random random) {
        return null;
    }

    default ItemDrops.SiteLoot extraFleetLoot(CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, Random random) {
        return ItemDrops.SiteLoot.NONE;
    }

    default boolean holdsBackBossUnique(ChartRun run) {
        return false;
    }

    default boolean guaranteesBossTierUp() {
        return false;
    }

    default boolean holdsPlayerBattle() {
        return false;
    }

    default float battleLootMult(ChartGenContext context) {
        return 1f;
    }

    default ItemDrops.SiteLoot touchBattleLoot(ItemDrops.SiteLoot loot, Random random) {
        return loot;
    }

    default ItemChances siteChances(MemoryAPI siteMemory, ItemChances chances) {
        return chances;
    }
}
