package exiledsector.charts;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.Random;

final class VanillaChartLayoutEngine implements ChartLayoutEngine {

    @Override
    public void build(StarSystemAPI system, String starId, StarSystemType systemType, StarAge age, Random random) {
        Random previous = StarSystemGenerator.random;
        StarSystemGenerator.random = random;
        try {
            new ChartStarSystemGenerator(age, systemType).generateLayout(system, starId);
        } finally {
            StarSystemGenerator.random = previous;
        }
    }
}
