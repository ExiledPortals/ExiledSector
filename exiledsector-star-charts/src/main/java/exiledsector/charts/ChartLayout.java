package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import org.lwjgl.util.vector.Vector2f;

public record ChartLayout(long seed, StarSystemType systemType, boolean generated, Vector2f innerWormholeLocation, SectorEntityToken featureAnchor) {

    public ChartLayout(long seed, StarSystemType systemType, boolean generated, Vector2f innerWormholeLocation) {
        this(seed, systemType, generated, innerWormholeLocation, null);
    }
}
