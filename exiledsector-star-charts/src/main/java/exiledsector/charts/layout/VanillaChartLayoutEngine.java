package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.anomaly.ChartAnomaly;

import java.util.List;
import java.util.Random;

final class VanillaChartLayoutEngine implements ChartLayoutEngine {

    @Override
    public SectorEntityToken build(StarSystemAPI system, String starId, StarSystemType systemType, StarAge age, Random random, ChartLayoutRecipe recipe,
                                   List<ChartAnomaly> anomalies) {
        Random previous = StarSystemGenerator.random;
        StarSystemGenerator.random = random;
        try {
            ChartStarSystemGenerator generator = new ChartStarSystemGenerator(age, systemType, recipe, anomalies);
            generator.generateLayout(system, starId);
            return generator.featureAnchor();
        } finally {
            StarSystemGenerator.random = previous;
        }
    }
}
