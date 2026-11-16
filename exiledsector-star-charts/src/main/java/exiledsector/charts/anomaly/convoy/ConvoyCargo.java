package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.CargoPodsEntityPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.data.RandomRounding;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.loot.ChartCargoLoot;
import exiledsector.charts.loot.ChartDropRules;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.loot.ChartLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.effects.ItemLootListener;
import exiledsector.items.InfusedFeedstock;
import exiledsector.items.SalvageSiteDrops;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

final class ConvoyCargo {

    static final String CATEGORY_KEY_PREFIX = "chart.anomaly.supply_convoy.cargo.";

    private ConvoyCargo() {
    }

    static List<String> manifestKeys(int tier) {
        return manifestKeys(tier, Set.of());
    }

    static List<String> manifestKeys(int tier, Collection<String> feedstockIds) {
        List<String> keys = new ArrayList<>();
        for (DropType type : ChartLoot.dropTypesFor(ConvoyRules.LOOT_SCOPE, tier)) {
            keys.add(CATEGORY_KEY_PREFIX + type.name().toLowerCase(Locale.ROOT));
        }
        if (SalvageSiteDrops.hasRule(ConvoyRules.CARGO_SITE)) {
            keys.add(CATEGORY_KEY_PREFIX + "items");
        }
        if (ConvoyFeedstock.hasRows()) {
            keys.add(feedstockKey(feedstockIds));
        }
        if (ChartDropRules.hasRule(ChartDrops.Source.SYSTEM_SALVAGE)) {
            keys.add(CATEGORY_KEY_PREFIX + "craftingItem");
            keys.add(CATEGORY_KEY_PREFIX + "star_chart");
        }
        return keys;
    }

    static String feedstockKey(Collection<String> feedstockIds) {
        Set<String> kinds = new HashSet<>(feedstockIds);
        InfusedFeedstock only = kinds.size() == 1 ? InfusedFeedstock.byItemId(kinds.iterator().next()) : null;
        return only == null ? CATEGORY_KEY_PREFIX + "feedstock" : CATEGORY_KEY_PREFIX + "feedstock." + only.name().toLowerCase(Locale.ROOT);
    }

    static StyledText manifest(int tier, Collection<String> feedstockIds) {
        return Translation.list(manifestKeys(tier, feedstockIds).stream().map(Translation::styled).toList());
    }

    static StyledText manifest(int tier, ConvoyState convoy) {
        return manifest(tier, convoy == null ? Set.of() : convoy.feedstock().keySet());
    }

    static float freighterPoints(CampaignFleetAPI fleet, boolean civilianOnly) {
        if (fleet == null || fleet.isEmpty()) {
            return 0f;
        }
        if (!civilianOnly || fleet.getFleetData() == null) {
            return fleet.getFleetPoints();
        }
        float points = 0f;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isCivilian()) {
                points += member.getFleetPointCost();
            }
        }
        return points;
    }

    static CargoAPI roll(SectorAPI sector, ChartRun run, ConvoyState convoy, float share, float feedstockScale, Random random) {
        CargoAPI cargo = ChartCargoLoot.roll(sector, run, ConvoyRules.LOOT_SCOPE, ConvoyRules.CARGO_SITE, convoy.cargoSteps() * share,
                convoy.itemChanceMult() * share, random);
        return withFeedstock(cargo, convoy, share * feedstockScale, random);
    }

    static CargoAPI lostCargo(SectorAPI sector, ChartRun run, ConvoyState convoy, float share, float feedstockScale, Random random) {
        CargoAPI cargo = ChartCargoLoot.rollFromBattle(sector, run, ConvoyRules.LOOT_SCOPE, ConvoyRules.CARGO_SITE, convoy.cargoSteps() * share,
                convoy.itemChanceMult() * share, random);
        return withFeedstock(cargo, convoy, share * feedstockScale, random);
    }

    static CargoAPI withFeedstock(CargoAPI cargo, ConvoyState convoy, float share, Random random) {
        ConvoyFeedstock.add(cargo, convoy.feedstock(), share, random);
        cargo.sort();
        return cargo;
    }

    static CargoAPI deliveredCache(SectorAPI sector, ChartRun run, ConvoyState convoy, CampaignFleetAPI fleet, float feedstockScale, Random random) {
        CargoAPI cache = Global.getFactory().createCargo(true);
        if (fleet != null && !fleet.isEmpty()) {
            ItemLootListener.addUnfoughtBattleLoot(cache, fleet, random);
        }
        cache.addAll(roll(sector, run, convoy, convoy.deliveredShare(), feedstockScale, random));
        keepShare(cache, ConvoyRules.DELIVERED_SHARE, random);
        cache.sort();
        return cache;
    }

    static void keepShare(CargoAPI cargo, float share, Random random) {
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (stack.isNull()) {
                continue;
            }
            float size = stack.getSize();
            int kept = RandomRounding.round(size * share, random);
            if (kept < size) {
                cargo.removeItems(stack.getType(), stack.getData(), size - kept);
            }
        }
        cargo.removeEmptyStacks();
    }

    static SectorEntityToken dropPods(LocationAPI location, String runId, CargoAPI cargo, Vector2f at) {
        if (cargo == null || cargo.isEmpty()) {
            return null;
        }
        CustomCampaignEntityAPI pods = Misc.addCargoPods(location, at);
        pods.getCargo().addAll(cargo);
        if (pods.getCustomPlugin() instanceof CargoPodsEntityPlugin plugin) {
            plugin.setNeverExpire(true);
        }
        ChartAnomalyFleets.tag(pods, runId);
        return pods;
    }
}
