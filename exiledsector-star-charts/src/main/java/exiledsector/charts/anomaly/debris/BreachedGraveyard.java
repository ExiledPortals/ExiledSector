package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public final class BreachedGraveyard {

    private BreachedGraveyard() {
    }

    public static Vector2f breachCentreOr(AnomalyContentSite site, Vector2f pickedCentre) {
        if (!DebrisTuning.of(site.context()).breachField()) {
            return pickedCentre;
        }
        Vector2f farthest = null;
        for (AnomalousDebrisTerrainPlugin field : AnomalousDebris.fieldsIn(site.system())) {
            SectorEntityToken fieldEntity = field.getEntity();
            Vector2f fieldCentre = fieldEntity == null ? null : fieldEntity.getLocation();
            if (fieldCentre != null && (farthest == null || site.distanceToEntry(fieldCentre) > site.distanceToEntry(farthest))) {
                farthest = fieldCentre;
            }
        }
        return farthest == null ? pickedCentre : new Vector2f(farthest);
    }

    static AnomalousDebris.Field breachFieldFor(AnomalyContentSite site, DebrisTuning tuning, Random random) {
        if (!tuning.breachField() || site.system() == null) {
            return null;
        }
        for (SectorEntityToken core : site.system().getEntitiesWithTag(SpatialBreach.CORE_TAG)) {
            if (core.getLocation() != null) {
                return new AnomalousDebris.Field(new Vector2f(core.getLocation()), DebrisRules.fieldRadius(random));
            }
        }
        return null;
    }
}
