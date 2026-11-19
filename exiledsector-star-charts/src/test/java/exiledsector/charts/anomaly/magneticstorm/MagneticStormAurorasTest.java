package exiledsector.charts.anomaly.magneticstorm;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin.MagneticFieldParams;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin.CoronaParams;
import com.fs.starfarer.api.util.FaderUtil;
import exiledsector.charts.anomaly.stormfront.StormFrontTerrainPlugin;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagneticStormAurorasTest {

    private static final float MIDDLE_RADIUS = 1000f;
    private static final float BAND_WIDTH = 400f;
    private static final float TOLERANCE_DEGREES = 1f;

    private static SectorEntityToken body(float x, float y) {
        PlanetAPI body = mock(PlanetAPI.class);
        when(body.getLocation()).thenReturn(new Vector2f(x, y));
        return body;
    }

    private static MagneticFieldTerrainPlugin field(SectorEntityToken parent) {
        MagneticFieldTerrainPlugin field = mock(MagneticFieldTerrainPlugin.class);
        FlareManager flares = new FlareManager(field);
        when(field.getFlareManager()).thenReturn(flares);
        when(field.getParams()).thenReturn(new MagneticFieldParams(BAND_WIDTH, MIDDLE_RADIUS, parent, 800f, 1200f, Color.BLUE, 0.5f, Color.RED, Color.GREEN));
        when(field.getFlareColorRange()).thenAnswer(invocation -> field.getParams().auroraColorRange);
        when(field.getFlareArcMin()).thenReturn(30f);
        when(field.getFlareArcMax()).thenReturn(80f);
        when(field.getFlareFadeInMin()).thenReturn(1f);
        when(field.getFlareFadeInMax()).thenReturn(2f);
        when(field.getFlareFadeOutMin()).thenReturn(2f);
        when(field.getFlareFadeOutMax()).thenReturn(5f);
        when(field.getFlareShortenFlatModMin()).thenReturn(0.8f);
        when(field.getFlareShortenFlatModMax()).thenReturn(0.8f);
        when(field.getFlareExtraLengthMultMin()).thenReturn(1f);
        when(field.getFlareExtraLengthMultMax()).thenReturn(1f);
        return field;
    }

    private static StormFrontTerrainPlugin front(Predicate<Vector2f> covers, SectorEntityToken entity) {
        StormFrontTerrainPlugin front = mock(StormFrontTerrainPlugin.class);
        when(front.containsPoint(any(Vector2f.class), anyFloat())).thenAnswer(invocation -> covers.test(invocation.getArgument(0)));
        when(front.getEntity()).thenReturn(entity);
        return front;
    }

    private static FlareManager.Flare flare(float direction, float arc) {
        FlareManager.Flare flare = new FlareManager.Flare();
        flare.direction = direction;
        flare.arc = arc;
        flare.fader = new FaderUtil(0f, 1f, 1f, false, true);
        return flare;
    }

    private static StarCoronaTerrainPlugin corona(SectorEntityToken star, FlareManager.Flare activeFlare) {
        StarCoronaTerrainPlugin corona = mock(StarCoronaTerrainPlugin.class);
        FlareManager flares = new FlareManager(corona);
        if (activeFlare != null) {
            flares.getFlares().add(activeFlare);
        }
        when(corona.getFlareManager()).thenReturn(flares);
        when(corona.getParams()).thenReturn(new CoronaParams(500f, 750f, star, 5f, 1f, 1f));
        return corona;
    }

    private static float angleBetween(float first, float second) {
        return Math.abs(Auroras.signedDifference(first, second));
    }

    @Test
    void anAuroraIsAVanillaFlareFacingTheTriggerWithinTheFieldsOwnRanges() {
        MagneticFieldTerrainPlugin field = field(body(0f, 0f));

        assertTrue(Auroras.setOff(field, 370f, new Random(1L)));

        FlareManager.Flare aurora = field.getFlareManager().getActiveFlare();
        assertEquals(10f, aurora.direction, 0.001f);
        assertTrue(aurora.arc >= 30f && aurora.arc <= 80f, "arc " + aurora.arc);
        assertEquals(0.8f, aurora.shortenFlatMod, 0.001f);
        assertEquals(1f, aurora.extraLengthMult, 0.001f);
        assertTrue(aurora.fader.getDurationIn() >= 1f && aurora.fader.getDurationIn() <= 2f);
        assertTrue(aurora.fader.getDurationOut() >= 2f && aurora.fader.getDurationOut() <= 5f);
        assertTrue(aurora.fader.isFadingIn());
        assertEquals(List.of(Color.RED, Color.GREEN), aurora.colors);
    }

    @Test
    void anAuroraAlreadyFacingTheTriggerIsLeftToPlayOut() {
        MagneticFieldTerrainPlugin field = field(body(0f, 0f));
        Auroras.setOff(field, 90f, new Random(2L));

        assertFalse(Auroras.setOff(field, 95f, new Random(3L)));
        assertEquals(1, field.getFlareManager().getFlares().size());
    }

    @Test
    void anAuroraElsewhereInTheFieldGivesWayToTheNewOne() {
        MagneticFieldTerrainPlugin field = field(body(0f, 0f));
        Auroras.setOff(field, 0f, new Random(4L));
        FlareManager.Flare earlier = field.getFlareManager().getActiveFlare();

        assertTrue(Auroras.setOff(field, 180f, new Random(5L)));
        assertEquals(180f, field.getFlareManager().getActiveFlare().direction, 0.001f);
        assertSame(earlier, field.getFlareManager().getFlares().get(1));
    }

    @Test
    void aFieldWithoutAuroraColoursHasNoAurora() {
        MagneticFieldTerrainPlugin field = field(body(0f, 0f));
        field.getParams().auroraColorRange.clear();

        assertFalse(Auroras.setOff(field, 0f, new Random(6L)));
        assertNull(field.getFlareManager().getActiveFlare());
    }

    @Test
    void aStormFrontCrossingOneSideOfAFieldSetsOffAnAuroraOnThatSide() {
        MagneticFieldTerrainPlugin field = field(body(0f, 0f));
        StormFrontTerrainPlugin front = front(point -> point.y > 600f, null);

        assertEquals(90f, IonFront.crossingDegrees(front, field), TOLERANCE_DEGREES);
        IonFront.setOffAuroras(List.of(front), List.of(field), new Random(7L));
        assertEquals(90f, field.getFlareManager().getActiveFlare().direction, TOLERANCE_DEGREES);
    }

    @Test
    void aStormFrontCoveringTheWholeFieldFacesTheAuroraTowardsTheFront() {
        MagneticFieldTerrainPlugin field = field(body(0f, 0f));

        assertEquals(180f, IonFront.crossingDegrees(front(point -> true, body(-4000f, 0f)), field), TOLERANCE_DEGREES);
    }

    @Test
    void aStormFrontThatMissesTheFieldOrHasPassedSetsOffNothing() {
        MagneticFieldTerrainPlugin field = field(body(0f, 0f));
        StormFrontTerrainPlugin missing = front(point -> point.x > 5000f, null);
        StormFrontTerrainPlugin passed = front(point -> true, null);
        when(passed.isDone()).thenReturn(true);

        assertNull(IonFront.crossingDegrees(missing, field));
        IonFront.setOffAuroras(List.of(missing, passed), List.of(field), new Random(8L));
        assertNull(field.getFlareManager().getActiveFlare());
    }

    @Test
    void aCoronaFlareSetsOffTheStarsFieldInItsDirectionAndWorldsInItsArcFacingTheStar() {
        SectorEntityToken star = body(0f, 0f);
        MagneticFieldTerrainPlugin starField = field(star);
        MagneticFieldTerrainPlugin litWorld = field(body(0f, 5000f));
        MagneticFieldTerrainPlugin darkWorld = field(body(0f, -5000f));

        GeomagneticSurge.setOffAuroras(List.of(corona(star, flare(90f, 40f))), List.of(starField, litWorld, darkWorld), new Random(9L));

        assertEquals(90f, starField.getFlareManager().getActiveFlare().direction, TOLERANCE_DEGREES);
        assertEquals(270f, litWorld.getFlareManager().getActiveFlare().direction, TOLERANCE_DEGREES);
        assertNull(darkWorld.getFlareManager().getActiveFlare());
    }

    @Test
    void aCoronaWithoutAFlareSetsOffNothing() {
        SectorEntityToken star = body(0f, 0f);
        MagneticFieldTerrainPlugin starField = field(star);

        GeomagneticSurge.setOffAuroras(List.of(corona(star, null)), List.of(starField), new Random(10L));

        assertNull(starField.getFlareManager().getActiveFlare());
    }

    @Test
    void theSystemCheckOnlyLooksForTriggersItsLinesAskFor() {
        SectorEntityToken star = body(0f, 0f);
        MagneticFieldTerrainPlugin starField = field(star);
        StarSystemAPI system = mock(StarSystemAPI.class);
        List<CampaignTerrainAPI> terrain = new ArrayList<>();
        for (Object plugin : List.of(starField, corona(star, flare(45f, 40f)))) {
            CampaignTerrainAPI entity = mock(CampaignTerrainAPI.class);
            when(entity.getPlugin()).thenReturn((com.fs.starfarer.api.campaign.CampaignTerrainPlugin) plugin);
            terrain.add(entity);
        }
        when(system.getTerrainCopy()).thenReturn(terrain);

        Auroras.setOffIn(system, new MagneticStormTuning(true, 50f, false, 1), new Random(11L));
        assertNull(starField.getFlareManager().getActiveFlare());

        Auroras.setOffIn(system, new MagneticStormTuning(false, 0f, true, 0), new Random(12L));
        assertTrue(angleBetween(45f, starField.getFlareManager().getActiveFlare().direction) < TOLERANCE_DEGREES);
    }
}
