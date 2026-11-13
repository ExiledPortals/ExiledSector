package exiledsector.charts.anomaly;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.List;

public final class DenseNebula implements StarRule, LootShaper, RunParticipant, AtlasRule {

    public static final String ANOMALY_ID = "dense_nebula";
    public static final DenseNebula INSTANCE = new DenseNebula();
    public static final int MAX_DUST = 10;
    public static final float STAR_CHART_PERCENT_PER_DUST = 10f;
    static final String DUST_FIELD = "dust";
    private static final String PREFIX = "chart.anomaly.dense_nebula.";

    private DenseNebula() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public StarSystemType systemType() {
        return StarSystemType.NEBULA;
    }

    @Override
    public void battleWon(AnomalyRun run) {
        int dust = dust(run.run());
        if (dust < MAX_DUST) {
            AnomalyState.of(run.run(), ANOMALY_ID).put(DUST_FIELD, dust + 1);
        }
    }

    @Override
    public float bossStarChartPercent(ChartRun run) {
        return dust(run) * STAR_CHART_PERCENT_PER_DUST;
    }

    public static int dust(ChartRun run) {
        return Math.max(0, Math.min(MAX_DUST, AnomalyState.of(run, ANOMALY_ID).integer(DUST_FIELD)));
    }

    @Override
    public String intelText(AnomalyRun run) {
        int dust = dust(run.run());
        float percent = bossStarChartPercent(run.run());
        return I18n.forGameText(() -> Translation.msg(PREFIX + "intel").arg("count", dust).arg("max", MAX_DUST)
                .arg("value", Math.round(percent)).text());
    }

    @Override
    public List<StyledText> rewardLines() {
        return List.of(Translation.msg(PREFIX + "effect.charts").arg("value", Math.round(STAR_CHART_PERCENT_PER_DUST)).arg("max", MAX_DUST).styled());
    }
}
