package exiledsector.charts.anomaly.relay;

import exiledsector.charts.ChartIds;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.Loot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelayBossRewardTest {

    private static final List<RelayChoice> PICKS = List.of(new RelayChoice(RewardGroup.ENGRAM, "kited", List.of("rammed")),
            new RelayChoice(RewardGroup.UNIQUE, "overloaded", List.of("burned")));

    private static ChartGenContext lines(ChartModifier effect, float value) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(effect, value)));
    }

    private static ChartRun relayRun(boolean allWoken) {
        ChartRun run = new ChartRun("run1", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(4, "pirates", "boss", 3, 2, List.of(RelayNetwork.ANOMALY_ID)));
        for (RelaySlot slot : RelayStates.slots(run)) {
            if (allWoken) {
                RelayStates.add(run, slot, RelayStates.WOKEN);
            }
        }
        return run;
    }

    @Test
    void withoutTreeLinesEachPickAddsItsGroupOnce() {
        assertEquals(List.of(RewardGroup.ENGRAM, RewardGroup.UNIQUE), RelayBossReward.rewardGroups(PICKS, ChartGenContext.none(), new Random(1L)));
    }

    @Test
    void doubleDownAddsEveryPickedGroupTwice() {
        List<RewardGroup> groups = RelayBossReward.rewardGroups(PICKS, lines(ChartModifier.CHART_RELAY_EXTRA_REWARD_COPIES, 1f), new Random(1L));

        assertEquals(List.of(RewardGroup.ENGRAM, RewardGroup.ENGRAM, RewardGroup.UNIQUE, RewardGroup.UNIQUE), groups);
    }

    @Test
    void scarCollectorAddsARewardFromAPickedGroupForEveryTwoRelayScars() {
        List<RewardGroup> groups = RelayBossReward.rewardGroups(PICKS, lines(ChartModifier.CHART_RELAY_SCAR_REWARDS, 1f), new Random(1L));

        assertEquals(4, groups.size());
        assertTrue(List.of(RewardGroup.ENGRAM, RewardGroup.UNIQUE).containsAll(groups.subList(2, 4)));
        assertEquals(1, RelayTuning.scarRewards(lines(ChartModifier.CHART_RELAY_SCAR_REWARDS, 1f), 3));
        assertEquals(0, RelayTuning.scarRewards(ChartGenContext.none(), 4));
    }

    @Test
    void relayRewardRarityRaisesTheItemRarityOfTheRewardDropsOnly() {
        ChartGenContext runContext = lines(ChartModifier.CHART_RELAY_REWARD_RARITY_PERCENT, 10f);

        assertEquals(0f, runContext.itemRarityPercent(), 0.001f);
        assertEquals(10f, RelayBossReward.withRewardRarity(runContext).itemRarityPercent(), 0.001f);
        ChartGenContext plain = ChartGenContext.none();
        assertSame(plain, RelayBossReward.withRewardRarity(plain));
    }

    @Test
    void earlyEchoNeedsItsLineEveryRelayAwakeAndAStageWithoutTheGuaranteedUnique() {
        ChartGenContext echo = lines(ChartModifier.CHART_RELAY_STAGE_UNIQUE_CHANCE, 100f);

        assertEquals(1f, RelayTuning.stageUniqueChance(echo), 0.001f);
        assertSame(Loot.NONE, RelayBossReward.echoUnique(null, relayRun(true), ChartGenContext.none(), new Random(1L)));
        assertSame(Loot.NONE, RelayBossReward.echoUnique(null, relayRun(false), echo, new Random(1L)));
        ChartGenContext lastStage = echo.withAtlasNode(new AtlasRunNode(7L, "pirates", List.of(), List.of(RelayNetwork.ANOMALY_ID))
                .withExtraEffects(List.of(), true));
        assertTrue(lastStage.guaranteedBossUnique());
        assertSame(Loot.NONE, RelayBossReward.echoUnique(null, relayRun(true), lastStage, new Random(1L)));
    }

    @Test
    void extraRelaysAndChoicesAreCountedFromTheirLines() {
        assertEquals(1, RelayTuning.extraRelays(lines(ChartModifier.CHART_RELAY_EXTRA_RELAYS, 3f)));
        assertEquals(RelayRules.MAX_RELAYS, RelayKind.values().length + RelayTuning.extraRelays(lines(ChartModifier.CHART_RELAY_EXTRA_RELAYS, 3f)));
        assertEquals(2, RelayTuning.onwardSystems(lines(ChartModifier.CHART_RELAY_ONWARD_SYSTEMS, 2f)));
        assertEquals(1, RelayTuning.onwardSystems(ChartGenContext.none()));
        assertEquals(4, RelayTuning.choiceCount(lines(ChartModifier.CHART_RELAY_EXTRA_CHOICES, 1f)));
    }
}
