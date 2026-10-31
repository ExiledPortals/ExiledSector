package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.anomaly.ChartAnomaly;

import java.util.List;
import java.util.Random;

interface ChartLayoutEngine {

    record Result(SectorEntityToken featureAnchor, List<String> unshapedAnomalyIds) {

        public Result {
            unshapedAnomalyIds = unshapedAnomalyIds == null ? List.of() : List.copyOf(unshapedAnomalyIds);
        }
    }

    Result build(StarSystemAPI system, String starId, StarSystemType systemType, StarAge age, Random random, ChartLayoutRecipe recipe,
                            List<ChartAnomaly> anomalies);
}
