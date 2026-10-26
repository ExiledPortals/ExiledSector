package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public record ChartLayout(long seed, StarSystemType systemType, boolean generated, Vector2f innerWormholeLocation, SectorEntityToken featureAnchor,
                          List<String> anomalyIds) {

    public ChartLayout {
        anomalyIds = anomalyIds == null ? List.of() : List.copyOf(anomalyIds);
    }

    public ChartLayout(long seed, StarSystemType systemType, boolean generated, Vector2f innerWormholeLocation, SectorEntityToken featureAnchor) {
        this(seed, systemType, generated, innerWormholeLocation, featureAnchor, List.of());
    }
}
