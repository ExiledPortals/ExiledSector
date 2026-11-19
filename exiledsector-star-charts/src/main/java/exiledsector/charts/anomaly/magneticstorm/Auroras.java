package exiledsector.charts.anomaly.magneticstorm;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.FaderUtil;
import exiledsector.charts.anomaly.AnomalyTerrains;
import exiledsector.charts.anomaly.stormfront.StormFront;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

public final class Auroras {

    static final float FULL_TURN = 360f;

    private Auroras() {
    }

    public static void setOffIn(LocationAPI system, MagneticStormTuning tuning, Random random) {
        if (!tuning.setsOffAuroras()) {
            return;
        }
        List<MagneticFieldTerrainPlugin> fields = AnomalyTerrains.in(system, MagneticFieldTerrainPlugin.class);
        if (fields.isEmpty()) {
            return;
        }
        if (tuning.ionFrontAuroras()) {
            IonFront.setOffAuroras(StormFront.frontsIn(system), fields, random);
        }
        if (tuning.flareAuroras()) {
            GeomagneticSurge.setOffAuroras(AnomalyTerrains.in(system, StarCoronaTerrainPlugin.class), fields, random);
        }
    }

    static boolean setOff(MagneticFieldTerrainPlugin field, float directionDegrees, Random random) {
        FlareManager flares = field.getFlareManager();
        List<Color> colours = field.getFlareColorRange();
        if (flares == null || colours == null || colours.isEmpty() || covers(flares.getActiveFlare(), directionDegrees)) {
            return false;
        }
        FlareManager.Flare aurora = new FlareManager.Flare();
        aurora.direction = normalised(directionDegrees);
        aurora.arc = between(random, field.getFlareArcMin(), field.getFlareArcMax());
        aurora.extraLengthMult = between(random, field.getFlareExtraLengthMultMin(), field.getFlareExtraLengthMultMax());
        aurora.extraLengthFlat = between(random, field.getFlareExtraLengthFlatMin(), field.getFlareExtraLengthFlatMax());
        aurora.shortenFlatMod = between(random, field.getFlareShortenFlatModMin(), field.getFlareShortenFlatModMax());
        aurora.fader = new FaderUtil(0f, between(random, field.getFlareFadeInMin(), field.getFlareFadeInMax()),
                between(random, field.getFlareFadeOutMin(), field.getFlareFadeOutMax()), false, true);
        aurora.colors.addAll(colours);
        flares.getFlares().add(0, aurora);
        aurora.fader.fadeIn();
        return true;
    }

    static boolean covers(FlareManager.Flare flare, float directionDegrees) {
        return flare != null && Math.abs(signedDifference(flare.direction, directionDegrees)) <= flare.arc / 2f;
    }

    static float angleDegrees(Vector2f from, Vector2f to) {
        return normalised((float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x)));
    }

    static float signedDifference(float fromDegrees, float toDegrees) {
        float difference = normalised(toDegrees - fromDegrees);
        return difference > FULL_TURN / 2f ? difference - FULL_TURN : difference;
    }

    static float normalised(float degrees) {
        float turned = degrees % FULL_TURN;
        return turned < 0f ? turned + FULL_TURN : turned;
    }

    private static float between(Random random, float min, float max) {
        return min + (max - min) * random.nextFloat();
    }
}
