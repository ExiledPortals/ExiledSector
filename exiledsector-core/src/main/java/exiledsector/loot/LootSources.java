package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.PlayerFleet;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
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

    private LootSources() {
    }

    public static List<LootSource> all() {
        return SOURCES.all();
    }

    public static FleetLootRules fleetLootRules(CampaignFleetAPI fleet) {
        return SOURCES.firstOr(source -> source.fleetLootRules(fleet), FleetLootRules.LEVEL_SCALED);
    }

    public static LootMultipliers modifiersFor(SectorEntityToken site) {
        return SOURCES.firstOr(source -> source.modifiersFor(site), LootMultipliers.NONE);
    }

    public static ItemDrops.SiteLoot rollSite(SectorAPI sector, String siteId, Random random, LootMultipliers modifiers) {
        ItemDrops.SiteLoot baseLoot = ItemDrops.rollSite(siteId, random, modifiers.itemChances(), modifiers.feedstockMult(), modifiers.craftingItemMult(),
                modifiers.craftingBandShift());
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
}
