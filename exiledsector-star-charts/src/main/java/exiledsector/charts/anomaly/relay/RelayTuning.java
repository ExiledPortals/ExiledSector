package exiledsector.charts.anomaly.relay;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

final class RelayTuning {

    static final String REWARD_RARITY_KEY = ChartModifier.CHART_RELAY_REWARD_RARITY_PERCENT.anomalyTuningKey();
    static final String ONWARD_SYSTEMS_KEY = ChartModifier.CHART_RELAY_ONWARD_SYSTEMS.anomalyTuningKey();
    static final String EXTRA_CHOICES_KEY = ChartModifier.CHART_RELAY_EXTRA_CHOICES.anomalyTuningKey();
    static final String EXTRA_REWARD_COPIES_KEY = ChartModifier.CHART_RELAY_EXTRA_REWARD_COPIES.anomalyTuningKey();
    static final String EXTRA_SCARS_KEY = ChartModifier.CHART_RELAY_EXTRA_SCARS.anomalyTuningKey();
    static final String EXTRA_RELAYS_KEY = ChartModifier.CHART_RELAY_EXTRA_RELAYS.anomalyTuningKey();
    static final String SCAR_REWARDS_KEY = ChartModifier.CHART_RELAY_SCAR_REWARDS.anomalyTuningKey();
    static final String STAGE_UNIQUE_CHANCE_KEY = ChartModifier.CHART_RELAY_STAGE_UNIQUE_CHANCE.anomalyTuningKey();
    static final String SHARED_BROADCAST_KEY = ChartModifier.CHART_RELAY_SHARED_BROADCAST.anomalyTuningKey();
    static final String FULL_ARRAY_KEY = ChartModifier.CHART_RELAY_FULL_ARRAY.anomalyTuningKey();
    static final int SCARS_PER_EXTRA_REWARD = 2;

    private RelayTuning() {
    }

    static ChartGenContext of(ChartRun run) {
        return ChartRunEffects.contextOf(run);
    }

    static float rewardRarityPercent(ChartGenContext context) {
        return context.anomalyPercent(REWARD_RARITY_KEY);
    }

    static int onwardSystems(ChartGenContext context) {
        return Math.max(1, count(context, ONWARD_SYSTEMS_KEY));
    }

    static int choiceCount(ChartGenContext context) {
        return OfficerRewardPool.OFFER_COUNT + count(context, EXTRA_CHOICES_KEY);
    }

    static int rewardCopies(ChartGenContext context) {
        return 1 + count(context, EXTRA_REWARD_COPIES_KEY);
    }

    static int extraScars(ChartGenContext context) {
        return count(context, EXTRA_SCARS_KEY);
    }

    static int extraRelays(ChartGenContext context) {
        return Math.min(count(context, EXTRA_RELAYS_KEY), RelayRules.MAX_RELAYS - RelayKind.values().length);
    }

    static int scarRewards(ChartGenContext context, int relayScars) {
        return count(context, SCAR_REWARDS_KEY) * (relayScars / SCARS_PER_EXTRA_REWARD);
    }

    static float stageUniqueChance(ChartGenContext context) {
        return Math.max(0f, context.anomalyPercent(STAGE_UNIQUE_CHANCE_KEY)) / ChartGenContext.PERCENT;
    }

    static boolean sharedBroadcast(ChartGenContext context) {
        return count(context, SHARED_BROADCAST_KEY) > 0;
    }

    static boolean fullArray(ChartGenContext context) {
        return count(context, FULL_ARRAY_KEY) > 0;
    }

    private static int count(ChartGenContext context, String key) {
        return context == null ? 0 : Math.max(0, Math.round(context.anomalyPercent(key)));
    }
}
