package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.reward.OfficerPoolLoot;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.loot.Loot;

record SpoilsOfWar(int extraRewards) {

    static final SpoilsOfWar NONE = new SpoilsOfWar(0);
    static final String SEED_PREFIX = "spoilsOfWar|";
    static final String EXTRA_REWARDS_KEY = ChartModifier.CHART_SKIRMISH_OFFICER_EXTRA_REWARDS.anomalyTuningKey();

    static SpoilsOfWar of(ChartRun run) {
        return run == null ? NONE : of(ChartRunEffects.contextOf(run));
    }

    static SpoilsOfWar of(ChartGenContext context) {
        return context == null ? NONE : new SpoilsOfWar(Math.round(context.anomalyPercent(EXTRA_REWARDS_KEY)));
    }

    Loot extraLoot(SectorAPI sector, AtlasOfficer officer, ChartRun run) {
        return OfficerPoolLoot.drawn(sector, officer, run, extraRewards, SEED_PREFIX);
    }
}
