package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.Seeds;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.DroppableDefinitions;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemType;
import exiledsector.items.ItemUnlock;
import exiledsector.items.UniqueDropRoll;
import exiledsector.loot.Loot;

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

    static Loot bossReward(SectorAPI sector, ChartRun run) {
        List<RelayChoice> picks = RelayChoices.picks(run);
        ChartGenContext runContext = RelayTuning.of(run);
        Loot chosen = picks.isEmpty() ? Loot.NONE
                : bossReward(picks, RewardContext.forRun(sector, run, run.contentFaction()), run.tier(), runContext,
                Seeds.random(REWARD_SEED_PREFIX + run.runId()));
        Loot echo = echoUnique(sector, run, runContext, Seeds.random(ECHO_SEED_PREFIX + run.runId()));
        return echo.isEmpty() ? chosen : chosen.plus(echo);
    }

    static Loot bossReward(List<RelayChoice> picks, RewardContext context, int runTier, ChartGenContext runContext, Random random) {
        List<RewardGroup> groups = rewardGroups(picks, runContext, random);
        return OfficerRewardDrop.roll(OfficerRewardPool.drawn(groups, context, random), new ChartLootContext(withRewardRarity(runContext), runTier), random);
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

    static Loot echoUnique(SectorAPI sector, ChartRun run, ChartGenContext runContext, Random random) {
        float chance = RelayTuning.stageUniqueChance(runContext);
        if (chance <= 0f || !RelayStates.allWoken(run)) {
            return Loot.NONE;
        }
        ItemDefinition unique = UniqueDropRoll.roll(random, chance, runContext.itemChances(),
                picker -> DroppableDefinitions.pickUniqueFromAnySource(picker, definition -> ECHO_UNIQUE_TYPES.contains(definition.type())
                        && ItemUnlock.available(definition.type(), sector) && ItemUnlock.canDrop(definition, sector)));
        if (unique == null) {
            return Loot.NONE;
        }
        return new Loot(List.of(ItemData.rolled(unique, random.nextLong())), List.of(), Map.of());
    }
}
