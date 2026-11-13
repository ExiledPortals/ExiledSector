package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class LootSources {

    private static final DomainContributions<LootSource> SOURCES = new DomainContributions<>(ExiledSectorDomain::lootSources);
    private static final DomainContributions<FleetDifficulty> DIFFICULTIES = DomainContributions.fromEveryDomain(ExiledSectorDomain::fleetDifficulties);

    private LootSources() {
    }

    public static List<LootSource> all() {
        return SOURCES.all();
    }

    public static ItemChances itemChancesFor(CampaignFleetAPI fleet) {
        return SOURCES.firstOr(source -> source.itemChancesFor(fleet), LootMultipliers.NONE.itemChances());
    }

    public static FleetLootRules fleetLootRules(CampaignFleetAPI fleet) {
        return SOURCES.firstOr(source -> source.fleetLootRules(fleet), FleetLootRules.REGULAR);
    }

    public static LootMultipliers modifiersFor(SectorEntityToken site) {
        return SOURCES.firstOr(source -> source.modifiersFor(site), LootMultipliers.NONE);
    }

    public static ItemDrops.SiteLoot rollSite(SectorAPI sector, String siteId, Random random, LootMultipliers modifiers) {
        ItemDrops.SiteLoot baseLoot = ItemDrops.rollSite(siteId, random, modifiers.itemChances(), modifiers.feedstockMult(), modifiers.craftingItemMult());
        SiteFind find = new SiteFind(sector, siteId, random, modifiers.itemChances(), new ArrayList<>(), new ArrayList<>());
        SOURCES.all().forEach(source -> source.addSiteFinds(find));
        return baseLoot.plus(find.items(), find.upgrades(), Map.of());
    }

    public static List<ItemData> bossDrops(int chartTier, String bossFactionId, float chanceMult, Random random,
                                             Predicate<ItemDefinition> allowed) {
        List<ItemData> drops = new ArrayList<>();
        SOURCES.all().forEach(source -> drops.addAll(source.bossDrops(chartTier, bossFactionId, chanceMult, random, allowed)));
        return drops;
    }

    public static List<ItemData> shapeDrops(List<ItemData> items) {
        return shape(items, false);
    }

    public static List<ItemData> shapeBattleDrops(List<ItemData> items) {
        return shape(items, true);
    }

    public static List<ItemData> shapeSiteDrops(List<ItemData> items, SectorEntityToken site) {
        List<ItemData> shaped = shapeDrops(items);
        if (site == null || shaped.isEmpty()) {
            return shaped;
        }
        List<ItemData> siteShaped = new ArrayList<>(shaped.size());
        for (ItemData item : shaped) {
            ItemData shapedItem = item;
            for (LootSource source : SOURCES.all()) {
                shapedItem = source.shapeSiteDrop(shapedItem, site);
            }
            siteShaped.add(shapedItem);
        }
        return siteShaped;
    }

    private static List<ItemData> shape(List<ItemData> items, boolean fromBattle) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        LocationAPI location = playerFleet == null ? null : playerFleet.getContainingLocation();
        if (location == null || items.isEmpty()) {
            return items;
        }
        List<ItemData> shaped = new ArrayList<>(items.size());
        for (ItemData item : items) {
            ItemData shapedItem = item;
            for (LootSource source : SOURCES.all()) {
                shapedItem = fromBattle ? source.shapeBattleDrop(shapedItem, location) : source.shapeDrop(shapedItem, location);
            }
            shaped.add(shapedItem);
        }
        return shaped;
    }

    public static float difficultyMultiplier(float battleDifficulty) {
        return DIFFICULTIES.firstOr(difficulty -> difficulty.difficultyMultiplier(battleDifficulty), 1f);
    }

    public static boolean shipLevelsRated() {
        return DIFFICULTIES.all().stream().anyMatch(FleetDifficulty::ratesShipLevels);
    }

    public static int shipLevel(FleetMemberAPI member) {
        int level = 0;
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            level = Math.max(level, difficulty.shipLevel(member));
        }
        return level;
    }

    public static FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        return DIFFICULTIES.first(difficulty -> difficulty.levelRange(fleetMemory));
    }

    public static String levelSeed(MemoryAPI fleetMemory) {
        return DIFFICULTIES.first(difficulty -> difficulty.levelSeed(fleetMemory));
    }

    public static String memberLevelSeed(MemoryAPI fleetMemory, FleetMemberAPI member) {
        return DIFFICULTIES.first(difficulty -> difficulty.memberLevelSeed(fleetMemory, member));
    }

    public static int memberExtraLevels(MemoryAPI fleetMemory, FleetMemberAPI member) {
        int levels = 0;
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            levels = Math.max(levels, difficulty.memberExtraLevels(fleetMemory, member));
        }
        return levels;
    }

    public static List<String> grantedNodeIds(MemoryAPI fleetMemory) {
        return DIFFICULTIES.firstOr(difficulty -> difficulty.grantedNodeIds(fleetMemory), List.of());
    }
}
