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
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemUnlock;
import exiledsector.loot.LootMultipliers;
import exiledsector.loot.LootSources;
import exiledsector.ui.CampaignMessages;
import exiledsector.Seeds;
import org.apache.log4j.Logger;

import java.awt.Color;
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

    public record Reward(int credits, ItemDrops.SiteLoot loot) {
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
            return new Reward(0, ItemDrops.SiteLoot.NONE);
        }
        Random random = rewardRandom(run);
        float rewardMult = ChartRunEffects.contextOf(run).rewardMult();
        int credits = creditsFor(run.tier(), rewardMult);
        cargo.getCredits().add(credits);
        float chanceMult = itemChanceMultFor(run.tier()) * rewardMult;
        ItemDrops.SiteLoot siteLoot = LootSources.rollSite(sector, REWARD_SITE, random, LootMultipliers.of(chanceMult));
        ChartDrops.Loot chartLoot = ChartDrops.forRunReward(run, random);
        announce(run, credits);
        return new Reward(credits, siteLoot.plus(chartLoot.charts(), List.of(), chartLoot.materials()));
    }

    private static void announce(ChartRun run, int credits) {
        String objective = ChartObjectiveIntel.objectiveName(run);
        CampaignMessages.plain(I18n.forGameText(() -> Translation.msg("chart.reward.message").arg("objective", objective).arg("credits", credits).text()),
                REWARD_COLOUR);
    }
}
