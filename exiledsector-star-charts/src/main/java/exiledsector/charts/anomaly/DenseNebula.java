package exiledsector.charts.anomaly;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.List;

public final class DenseNebula implements StarRule, AtlasRule {

    public static final String ANOMALY_ID = "dense_nebula";
    public static final DenseNebula INSTANCE = new DenseNebula();
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
    public List<StyledText> rewardLines() {
        return List.of(Translation.msg(PREFIX + "effect.charts").arg("value", Math.round(SettlingDust.STAR_CHART_PERCENT_PER_STACK))
                .arg("max", SettlingDust.BASE_MAX_STACKS).styled());
    }
}
