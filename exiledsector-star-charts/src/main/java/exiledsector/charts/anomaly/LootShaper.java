package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.loot.FleetClaim;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.loot.Loot;

import java.util.List;
import java.util.Random;

public interface LootShaper extends ChartAnomaly {

    default List<String> lootScopes() {
        return List.of();
    }

    default Loot fleetLoot(FleetClaim claim, ChartLootContext loot, Random random) {
        return Loot.NONE;
    }

    default boolean holdsBackBossUnique(ChartRun run) {
        return false;
    }

    default Loot bossDefeatedLoot(SectorAPI sector, ChartRun run) {
        return Loot.NONE;
    }

    default boolean holdsPlayerBattle() {
        return false;
    }

    default float battleLootMult(ChartGenContext context) {
        return 1f;
    }

    default ItemData shapeDrop(SectorAPI sector, ItemData item, LocationAPI location) {
        return item;
    }

    default ItemData shapeBattleDrop(SectorAPI sector, ItemData item, LocationAPI location) {
        return item;
    }

    default boolean overchargesHullItems(CampaignFleetAPI fleet) {
        return false;
    }

    default void battleWon(AnomalyRun run) {
    }

    default float bossStarChartPercent(ChartRun run) {
        return 0f;
    }

    default ItemChances siteChances(MemoryAPI siteMemory, ItemChances chances) {
        return chances;
    }
}
