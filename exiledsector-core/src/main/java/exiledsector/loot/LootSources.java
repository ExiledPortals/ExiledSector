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
import exiledsector.items.SalvageSiteDrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class LootSources {

    private static final DomainContributions<LootSource> SOURCES = new DomainContributions<>(ExiledSectorDomain::lootSources);
    private static final List<LootSource> CORE_SOURCES_AFTER_DOMAINS = List.of(CraftingLootSource.INSTANCE);

    private LootSources() {
    }

    public static List<LootSource> all() {
        List<LootSource> sources = new ArrayList<>(SOURCES.all());
        sources.addAll(CORE_SOURCES_AFTER_DOMAINS);
        return sources;
    }

    public static FleetLootRules fleetLootRules(CampaignFleetAPI fleet) {
        return SOURCES.firstOr(source -> source.fleetLootRules(fleet), FleetLootRules.LEVEL_SCALED);
    }

    public static LootMultipliers lootMultipliersFor(SectorEntityToken site) {
        return SOURCES.firstOr(source -> source.lootMultipliersFor(site), LootMultipliers.NONE);
    }

    public static Loot rollSite(SectorAPI sector, String siteId, Random random, LootMultipliers multipliers) {
        Loot baseLoot = SalvageSiteDrops.rollSite(siteId, random, multipliers);
        SiteFind find = new SiteFind(sector, siteId, random, multipliers.chances(), new ArrayList<>(), new ArrayList<>());
        SOURCES.all().forEach(source -> source.addSiteFinds(find));
        return baseLoot.plus(find.items(), find.schematics(), Map.of());
    }

    public static List<ItemData> bossDrops(int chartTier, String bossFactionId, float chanceMult, Random random,
                                             Predicate<ItemDefinition> allowed) {
        List<ItemData> drops = new ArrayList<>();
        SOURCES.all().forEach(source -> drops.addAll(source.bossDrops(chartTier, bossFactionId, chanceMult, random, allowed)));
        return drops;
    }

    public static List<ItemData> shape(List<ItemData> items, DropOrigin origin) {
        if (items.isEmpty()) {
            return items;
        }
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        LocationAPI playerLocation = playerFleet == null ? null : playerFleet.getContainingLocation();
        List<ItemData> shapedWhereThePlayerIs = playerLocation == null ? items : shapeEach(items, origin.whereThePlayerIs(), playerLocation);
        return origin.kind() == DropOrigin.Kind.SITE ? shapeEach(shapedWhereThePlayerIs, origin, playerLocation) : shapedWhereThePlayerIs;
    }

    private static List<ItemData> shapeEach(List<ItemData> items, DropOrigin origin, LocationAPI playerLocation) {
        List<ItemData> shaped = new ArrayList<>(items.size());
        for (ItemData item : items) {
            ItemData shapedItem = item;
            for (LootSource source : SOURCES.all()) {
                shapedItem = source.shape(shapedItem, origin, playerLocation);
            }
            shaped.add(shapedItem);
        }
        return shaped;
    }
}
