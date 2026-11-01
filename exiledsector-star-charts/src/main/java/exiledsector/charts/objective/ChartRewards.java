package exiledsector.charts.objective;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.effects.ItemLootListener;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemUnlock;
import exiledsector.items.MaterialItems;
import exiledsector.loot.LootModifiers;
import exiledsector.loot.LootSources;
import exiledsector.ui.CampaignMessages;
import exiledsector.ui.cargo.MaterialDropAnnouncer;
import exiledsector.Seeds;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ChartRewards {

    public static final String REWARD_SITE = "exiledSector_chart_reward";
    static final long REWARD_SALT = 0x43485245574152L;
    static final float CREDITS_PER_TIER_STEP = 10000f;
    static final float CREDIT_TIER_EXPONENT = 1.5f;
    static final float CREDIT_ROUNDING = 1000f;
    static final float ITEM_CHANCE_PER_TIER = 0.1f;
    private static final Color REWARD_COLOUR = new Color(120, 230, 140);
    private static final Logger LOG = Logger.getLogger(ChartRewards.class);

    public record Reward(int credits, int hullItems, int materials, int starCharts) {
    }

    private ChartRewards() {
    }

    public static int creditsFor(int tier) {
        return creditsFor(tier, 1f);
    }

    public static int creditsFor(int tier, float rewardMult) {
        float credits = CREDITS_PER_TIER_STEP * (float) Math.pow(ChartTiers.clamp(tier), CREDIT_TIER_EXPONENT) * Math.max(0f, rewardMult);
        return Math.round(credits / CREDIT_ROUNDING) * (int) CREDIT_ROUNDING;
    }

    static float itemChanceMultFor(int tier) {
        return 1f + ITEM_CHANCE_PER_TIER * (ChartTiers.clamp(tier) - 1);
    }

    static Random rewardRandom(ChartRun run) {
        return Seeds.derived(run.layoutSeed() ^ run.runId().hashCode(), REWARD_SALT);
    }

    public static Reward grant(SectorAPI sector, ChartRun run) {
        if (!run.claimReward()) {
            return null;
        }
        ItemUnlock.recordChartCleared(sector, run.tier());
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        CargoAPI cargo = playerFleet == null ? null : playerFleet.getCargo();
        if (cargo == null) {
            LOG.warn("[ExiledSector] The Star Chart reward for run " + run.runId() + " had no cargo to go into");
            return new Reward(0, 0, 0, 0);
        }
        Random random = rewardRandom(run);
        float rewardMult = ChartRunEffects.contextOf(run).rewardMult();
        int credits = creditsFor(run.tier(), rewardMult);
        cargo.getCredits().add(credits);
        float chanceMult = itemChanceMultFor(run.tier()) * rewardMult;
        ItemDrops.SiteLoot siteLoot = LootSources.rollSite(sector, REWARD_SITE, random, LootModifiers.of(chanceMult));
        ItemLootListener.addSiteLoot(cargo, siteLoot);
        int hullItems = siteLoot.items().size();
        ChartDrops.Loot chartLoot = grantChartDrops(run, cargo, random);
        int materialCount = siteLoot.materials().values().stream().mapToInt(Integer::intValue).sum();
        int chartMaterialCount = chartLoot.materials().values().stream().mapToInt(Integer::intValue).sum();
        announce(run, credits, hullItems, chartLoot.charts().size());
        MaterialDropAnnouncer.announce(chartLoot.currencies());
        return new Reward(credits, hullItems, materialCount + chartMaterialCount, chartLoot.charts().size());
    }

    static ChartDrops.Loot grantChartDrops(ChartRun run, CargoAPI cargo, Random random) {
        ChartDrops.Loot chartLoot = ChartDrops.forRunReward(run, random);
        chartLoot.charts().forEach(chart -> cargo.addSpecial(chart.toSpecialItem(), 1f));
        chartLoot.materials().forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        return chartLoot;
    }

    private static void announce(ChartRun run, int credits, int hullItems, int charts) {
        String objective = ChartObjectiveIntel.objectiveName(run.objective());
        List<String> lines = new ArrayList<>();
        lines.add(I18n.forGameText(() -> Translation.msg("chart.reward.message").arg("objective", objective).arg("credits", credits).text()));
        if (hullItems > 0) {
            lines.add(I18n.forGameText(() -> Translation.msg("chart.reward.items").count(hullItems).text()));
        }
        if (charts > 0) {
            lines.add(I18n.forGameText(() -> Translation.msg("chart.drop.reward").count(charts).text()));
        }
        for (String line : lines) {
            CampaignMessages.plain(line, REWARD_COLOUR);
        }
    }
}
