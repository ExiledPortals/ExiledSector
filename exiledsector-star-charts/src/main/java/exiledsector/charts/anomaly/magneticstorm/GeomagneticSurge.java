package exiledsector.charts.anomaly.magneticstorm;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;

import java.util.List;
import java.util.Random;

public final class GeomagneticSurge {

    private GeomagneticSurge() {
    }

    static void setOffAuroras(List<StarCoronaTerrainPlugin> coronas, List<MagneticFieldTerrainPlugin> fields, Random random) {
        for (StarCoronaTerrainPlugin corona : coronas) {
            FlareManager flares = corona.getFlareManager();
            FlareManager.Flare flare = flares == null ? null : flares.getActiveFlare();
            SectorEntityToken star = corona.getParams() == null ? null : corona.getParams().relatedEntity;
            if (flare == null || star == null || star.getLocation() == null) {
                continue;
            }
            for (MagneticFieldTerrainPlugin field : fields) {
                Float facing = facingDegrees(flare, star, field);
                if (facing != null) {
                    Auroras.setOff(field, facing, random);
                }
            }
        }
    }

    static Float facingDegrees(FlareManager.Flare flare, SectorEntityToken star, MagneticFieldTerrainPlugin field) {
        SectorEntityToken parent = field.getParams() == null ? null : field.getParams().relatedEntity;
        if (parent == null || parent.getLocation() == null) {
            return null;
        }
        if (parent == star) {
            return flare.direction;
        }
        return Auroras.covers(flare, Auroras.angleDegrees(star.getLocation(), parent.getLocation()))
                ? Auroras.angleDegrees(parent.getLocation(), star.getLocation()) : null;
    }
}
