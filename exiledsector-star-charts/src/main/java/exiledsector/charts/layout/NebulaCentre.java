package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.ArrayList;

public final class NebulaCentre {

    private NebulaCentre() {
    }

    public static void holdStill(StarSystemAPI system) {
        if (system == null || system.getType() != StarSystemType.NEBULA) {
            return;
        }
        SectorEntityToken centre = system.getCenter();
        for (SectorEntityToken entity : new ArrayList<>(system.getAllEntities())) {
            SectorEntityToken focus = entity.getOrbitFocus();
            if (focus != null && (focus == centre || isNebulaCentre(focus))) {
                entity.setOrbit(null);
            }
        }
    }

    static boolean isNebulaCentre(SectorEntityToken focus) {
        return focus instanceof PlanetAPI planet && planet.getSpec() != null && planet.getSpec().isNebulaCenter();
    }
}
