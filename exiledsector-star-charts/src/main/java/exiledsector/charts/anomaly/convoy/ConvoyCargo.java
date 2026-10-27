package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.CargoPodsEntityPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.loot.ChartCargoLoot;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.loot.ChartLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDrops;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class ConvoyCargo {

    static final String CATEGORY_KEY_PREFIX = "chart.convoy.cargo.";

    private ConvoyCargo() {
    }

    static List<String> manifestKeys(int tier) {
        List<String> keys = new ArrayList<>();
        for (DropType type : ChartLoot.dropTypesFor(ConvoyRules.LOOT_SCOPE, tier)) {
            keys.add(CATEGORY_KEY_PREFIX + type.name().toLowerCase(Locale.ROOT));
        }
        if (ItemDrops.hasRule(ConvoyRules.CARGO_SITE)) {
            keys.add(CATEGORY_KEY_PREFIX + "items");
        }
        if (ChartDrops.hasRule(ChartDrops.Source.SYSTEM_SALVAGE)) {
            keys.add(CATEGORY_KEY_PREFIX + "currency");
            keys.add(CATEGORY_KEY_PREFIX + "star_chart");
        }
        return keys;
    }

    static StyledText manifest(int tier) {
        return Translation.list(manifestKeys(tier).stream().map(Translation::styled).toList());
    }

    static CargoAPI roll(SectorAPI sector, ChartRun run, ConvoyState convoy, float share, Random random) {
        return ChartCargoLoot.roll(sector, run, ConvoyRules.LOOT_SCOPE, ConvoyRules.CARGO_SITE, convoy.cargoSteps() * share,
                convoy.itemChanceMult() * share, random);
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
