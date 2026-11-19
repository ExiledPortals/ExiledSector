package exiledsector.charts.anomaly.magneticstorm;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.anomaly.stormfront.StormBattles;
import exiledsector.charts.anomaly.stormfront.StormFrontTerrainPlugin;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

public final class IonFront {

    static final int RING_SAMPLES = 36;
    private static final float WHOLE_RING_SUM_LENGTH = 0.05f;

    private IonFront() {
    }

    static void setOffAuroras(List<StormFrontTerrainPlugin> fronts, List<MagneticFieldTerrainPlugin> fields, Random random) {
        for (StormFrontTerrainPlugin front : fronts) {
            if (front.isDone()) {
                continue;
            }
            for (MagneticFieldTerrainPlugin field : fields) {
                Float crossing = crossingDegrees(front, field);
                if (crossing != null) {
                    Auroras.setOff(field, crossing, random);
                }
            }
        }
    }

    static Float crossingDegrees(StormFrontTerrainPlugin front, MagneticFieldTerrainPlugin field) {
        MagneticFieldParams params = field.getParams();
        SectorEntityToken centre = params == null ? null : params.relatedEntity;
        if (centre == null || centre.getLocation() == null) {
            return null;
        }
        Vector2f centreLocation = centre.getLocation();
        float sumX = 0f;
        float sumY = 0f;
        Float firstCrossed = null;
        for (int sample = 0; sample < RING_SAMPLES; sample++) {
            float degrees = sample * Auroras.FULL_TURN / RING_SAMPLES;
            float cos = (float) Math.cos(Math.toRadians(degrees));
            float sin = (float) Math.sin(Math.toRadians(degrees));
            Vector2f onRing = new Vector2f(centreLocation.x + cos * params.middleRadius, centreLocation.y + sin * params.middleRadius);
            if (front.containsPoint(onRing, params.bandWidthInEngine / 2f)) {
                sumX += cos;
                sumY += sin;
                firstCrossed = firstCrossed == null ? degrees : firstCrossed;
            }
        }
        if (firstCrossed == null) {
            return null;
        }
        if (Math.hypot(sumX, sumY) >= WHOLE_RING_SUM_LENGTH) {
            return Auroras.normalised((float) Math.toDegrees(Math.atan2(sumY, sumX)));
        }
        SectorEntityToken frontEntity = front.getEntity();
        return frontEntity == null || frontEntity.getLocation() == null ? firstCrossed : Auroras.angleDegrees(centreLocation, frontEntity.getLocation());
    }

    public static boolean holdsPlayerBattle() {
        StormFrontTerrainPlugin front = StormBattles.playerFront();
        ChartRun run = front == null ? null : ChartRuns.live(front.runId());
        return run != null && run.anomalyIds().contains(MagneticStorm.ANOMALY_ID);
    }
}
