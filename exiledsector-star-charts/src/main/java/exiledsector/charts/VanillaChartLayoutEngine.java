package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.phenomenon.ChartPhenomenon;

import java.util.Random;

final class VanillaChartLayoutEngine implements ChartLayoutEngine {

    @Override
    public SectorEntityToken build(StarSystemAPI system, String starId, StarSystemType systemType, StarAge age, Random random, ChartLayoutRecipe recipe,
                                   ChartPhenomenon phenomenon) {
        Random previous = StarSystemGenerator.random;
        StarSystemGenerator.random = random;
        try {
            ChartStarSystemGenerator generator = new ChartStarSystemGenerator(age, systemType, recipe, phenomenon);
            generator.generateLayout(system, starId);
            return generator.featureAnchor();
        } finally {
            StarSystemGenerator.random = previous;
        }
    }
}
