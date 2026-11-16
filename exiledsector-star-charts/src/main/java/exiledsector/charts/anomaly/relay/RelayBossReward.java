package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.ItemUnlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class RelayBossReward {

    static final String REWARD_SEED_PREFIX = "relayReward|";
    static final String ECHO_SEED_PREFIX = "relayEcho|";
    static final Set<ItemType> ECHO_UNIQUE_TYPES = Set.of(ItemType.HULL_ITEM, ItemType.SUBROUTINE);

    private RelayBossReward() {
    }

    static ItemDrops.SiteLoot bossReward(SectorAPI sector, ChartRun run) {
        List<RelayChoice> picks = RelayChoices.picks(run);
        ChartGenContext runContext = RelayTuning.of(run);
        ItemDrops.SiteLoot chosen = picks.isEmpty() ? ItemDrops.SiteLoot.NONE
                : bossReward(picks, RewardContext.forRun(sector, run, run.contentFaction()), run.tier(), runContext,
                Seeds.random(REWARD_SEED_PREFIX + run.runId()));
        ItemDrops.SiteLoot echo = echoUnique(sector, run, runContext, Seeds.random(ECHO_SEED_PREFIX + run.runId()));
        return echo.isEmpty() ? chosen : chosen.plus(echo);
    }

    static ItemDrops.SiteLoot bossReward(List<RelayChoice> picks, RewardContext context, int runTier, ChartGenContext runContext, Random random) {
        List<RewardGroup> groups = rewardGroups(picks, runContext, random);
        return OfficerRewardDrop.roll(OfficerRewardPool.drawn(groups, context, random), runTier, withRewardRarity(runContext), random);
    }

    static List<RewardGroup> rewardGroups(List<RelayChoice> picks, ChartGenContext runContext, Random random) {
        int copies = RelayTuning.rewardCopies(runContext);
        List<RewardGroup> groups = new ArrayList<>();
        for (RelayChoice pick : picks) {
            for (int copy = 0; copy < copies; copy++) {
                groups.add(pick.group());
            }
        }
        int scarRewards = picks.isEmpty() ? 0 : RelayTuning.scarRewards(runContext, RelayChoices.scarCount(picks));
        for (int extra = 0; extra < scarRewards; extra++) {
            groups.add(picks.get(random.nextInt(picks.size())).group());
        }
        return groups;
    }

    static ChartGenContext withRewardRarity(ChartGenContext runContext) {
        float rarityPercent = RelayTuning.rewardRarityPercent(runContext);
        if (rarityPercent == 0f) {
            return runContext;
        }
        return runContext.withEffects(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_ITEM_RARITY_PERCENT, rarityPercent)));
    }

    static ItemDrops.SiteLoot echoUnique(SectorAPI sector, ChartRun run, ChartGenContext runContext, Random random) {
        float chance = RelayTuning.stageUniqueChance(runContext);
        if (chance <= 0f || runContext.guaranteedBossUnique() || !RelayStates.allWoken(run) || random.nextFloat() >= chance) {
            return ItemDrops.SiteLoot.NONE;
        }
        ItemDefinition unique = ItemDrops.pickUniqueFromAnySource(random, definition -> ECHO_UNIQUE_TYPES.contains(definition.type())
                && ItemUnlock.available(definition.type(), sector) && ItemUnlock.canDrop(definition, sector));
        if (unique == null) {
            return ItemDrops.SiteLoot.NONE;
        }
        return new ItemDrops.SiteLoot(List.of(ItemData.rolled(unique, random.nextLong())), List.of(), Map.of());
    }
}
