package exiledsector.charts;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.Random;

interface ChartLayoutEngine {

    void build(StarSystemAPI pocket, String starId, StarSystemType systemType, StarAge age, Random random);
}
