package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemUnlock;
import exiledsector.items.MaterialItems;
import exiledsector.ui.CampaignMessages;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ChartRewards {

    static final String REWARD_SITE = "exiledSector_chart_reward";
    static final long REWARD_SALT = 0x43485245574152L;
    static final float CREDITS_PER_TIER_STEP = 10000f;
    static final float CREDIT_TIER_EXPONENT = 1.5f;
    static final float CREDIT_ROUNDING = 1000f;
    static final float ITEM_CHANCE_PER_TIER = 0.1f;
    private static final Color REWARD_COLOUR = new Color(120, 230, 140);
    private static final Logger LOG = Logger.getLogger(ChartRewards.class);

    public record Reward(int credits, int hullItems, int materials) {
    }

    private ChartRewards() {
    }

    static int creditsFor(int tier) {
        float credits = CREDITS_PER_TIER_STEP * (float) Math.pow(ChartTiers.clamp(tier), CREDIT_TIER_EXPONENT);
        return Math.round(credits / CREDIT_ROUNDING) * (int) CREDIT_ROUNDING;
    }

    static float itemChanceMultFor(int tier) {
        return 1f + ITEM_CHANCE_PER_TIER * (ChartTiers.clamp(tier) - 1);
    }

    static Random rewardRandom(ChartRun run) {
        return ChartLayoutGenerator.derivedRandom(run.layoutSeed() ^ run.runId().hashCode(), REWARD_SALT);
    }

    public static Reward grant(SectorAPI sector, ChartRun run) {
        if (!run.claimReward()) {
            return null;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        CargoAPI cargo = playerFleet == null ? null : playerFleet.getCargo();
        if (cargo == null) {
            LOG.warn("[ExiledSector] The Star Chart reward for run " + run.runId() + " had no cargo to go into");
            return new Reward(0, 0, 0);
        }
        Random random = rewardRandom(run);
        int credits = creditsFor(run.tier());
        cargo.getCredits().add(credits);
        float chanceMult = itemChanceMultFor(run.tier());
        int hullItems = 0;
        for (ItemData item : ItemDrops.roll(REWARD_SITE, random, chanceMult)) {
            cargo.addSpecial(item.toSpecialItem(), 1f);
            hullItems++;
        }
        Map<String, Integer> materials = ItemDrops.rollMaterials(REWARD_SITE, random, chanceMult);
        materials.forEach((itemId, quantity) -> MaterialItems.add(cargo, itemId, quantity));
        if (ItemUnlock.sectionsOpen(sector)) {
            ItemDrops.SectionItemLoot sectionLoot = ItemDrops.rollSectionItemLoot(REWARD_SITE, random, chanceMult);
            for (ItemData item : sectionLoot.items()) {
                cargo.addSpecial(item.toSpecialItem(), 1f);
                hullItems++;
            }
            sectionLoot.upgrades().forEach(upgrade -> cargo.addSpecial(upgrade.toSpecialItem(), 1f));
        }
        grantChartDrops(run, cargo, random);
        int materialCount = materials.values().stream().mapToInt(Integer::intValue).sum();
        announce(run, credits, hullItems);
        return new Reward(credits, hullItems, materialCount);
    }

    static void grantChartDrops(ChartRun run, CargoAPI cargo, Random random) {
        // TODO phase 5: roll Star Chart drops here (same tier or one above, per the plan), into the cargo for the chart store to absorb
    }

    private static void announce(ChartRun run, int credits, int hullItems) {
        String objective = ChartObjectiveIntel.objectiveName(run.objective());
        List<String> lines = new ArrayList<>();
        lines.add(I18n.forGameText(() -> Translation.msg("chart.reward.message").arg("objective", objective).arg("credits", credits).text()));
        if (hullItems > 0) {
            lines.add(I18n.forGameText(() -> Translation.msg("chart.reward.items").count(hullItems).text()));
        }
        for (String line : lines) {
            CampaignMessages.plain(line, REWARD_COLOUR);
        }
    }
}
