package exiledsector.ui.atlas;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.layout.sketch.ChartSystemSketch;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Body;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasSystemArtTest {

    private static final List<String> ALL_ANOMALIES = List.of("black_hole", "neutron_star", "magnetic_storm", "stellar_flares",
            "spatial_breach", "faction_skirmish", "relay_network", "supply_convoy", "anomalous_debris", "excavation_sites",
            "storm_front", "derelict_forge", "listening_post");

    private static ChartSystemSketch system(String starType, int planets) {
        List<Body> bodies = new java.util.ArrayList<>();
        for (int index = 0; index < planets; index++) {
            bodies.add(new Body("barren", 100f, 2000f + index * 1000f, 100f, index * 40f));
        }
        return new ChartSystemSketch(null, List.of(new Body(starType, 600f, 0f, 0f, 0f)), bodies, List.of());
    }

    private static long count(AtlasSystemArt.Layout layout, AtlasSystemArt.Kind kind) {
        return layout.pieces().stream().filter(piece -> piece.kind() == kind).count();
    }

    @Test
    void nebulaSystemsGetPuffsInTheirAgesColour() {
        assertEquals(AtlasSystemArt.NEBULA_BLUE_SHEET, AtlasSystemArt.nebulaSheet(system("nebula_center_young", 0)));
        assertEquals(AtlasSystemArt.NEBULA_SHEET, AtlasSystemArt.nebulaSheet(system("nebula_center_average", 0)));
        assertEquals(AtlasSystemArt.NEBULA_AMBER_SHEET, AtlasSystemArt.nebulaSheet(system("nebula_center_old", 0)));
        assertNull(AtlasSystemArt.nebulaSheet(system("star_yellow", 0)));

        assertEquals(AtlasSystemArt.NEBULA_PUFFS, count(AtlasSystemArt.layout(1L, system("nebula_center_old", 0), List.of()),
                AtlasSystemArt.Kind.NEBULA_PUFF));
        assertEquals(AtlasSystemArt.DENSE_NEBULA_PUFFS, count(AtlasSystemArt.layout(1L, system("nebula_center_old", 0), List.of("dense_nebula")),
                AtlasSystemArt.Kind.NEBULA_PUFF));
        assertEquals(0, count(AtlasSystemArt.layout(1L, system("star_yellow", 3), List.of()), AtlasSystemArt.Kind.NEBULA_PUFF));
    }

    @Test
    void aMagneticStormRingsOnlyARealStarNeverABlackHoleOrANebulaCentre() {
        assertEquals(1, count(AtlasSystemArt.layout(5L, system("star_yellow", 0), List.of("magnetic_storm")), AtlasSystemArt.Kind.AURORA));
        assertEquals(0, count(AtlasSystemArt.layout(5L, system("black_hole", 0), List.of("magnetic_storm")), AtlasSystemArt.Kind.AURORA));
        ChartSystemSketch nebula = new ChartSystemSketch(StarSystemType.NEBULA, List.of(new Body("nebula_center_old", 600f, 0f, 0f, 0f)),
                List.of(new Body("barren", 100f, 2000f, 0f, 0f)), List.of());
        List<AtlasSystemArt.Piece> auroras = AtlasSystemArt.layout(5L, nebula, List.of("magnetic_storm")).pieces().stream()
                .filter(piece -> piece.kind() == AtlasSystemArt.Kind.AURORA).toList();
        assertEquals(1, auroras.size());
        assertEquals(0, auroras.get(0).planet());
    }

    @Test
    void everyAnomalyAddsItsOwnVignetteWithinTheSystem() {
        ChartSystemSketch sketch = system("star_yellow", 4);
        for (String anomalyId : ALL_ANOMALIES) {
            AtlasSystemArt.Layout layout = AtlasSystemArt.layout(7L, sketch, List.of(anomalyId));

            assertFalse(layout.pieces().isEmpty(), anomalyId);
            assertTrue(layout.pieces().stream().allMatch(piece -> piece.orbit() <= 1f && piece.planet() < sketch.planets().size()), anomalyId);
        }
    }

    @Test
    void everyFreeFloatingAnomalyOrbitsTheStar() {
        Set<AtlasSystemArt.Kind> onABody = EnumSet.of(AtlasSystemArt.Kind.ACCRETION, AtlasSystemArt.Kind.PULSAR, AtlasSystemArt.Kind.AURORA,
                AtlasSystemArt.Kind.FLARE, AtlasSystemArt.Kind.EXCAVATION);
        AtlasSystemArt.Layout layout = AtlasSystemArt.layout(3L, system("star_yellow", 4), ALL_ANOMALIES);

        List<AtlasSystemArt.Piece> floating = layout.pieces().stream().filter(piece -> !onABody.contains(piece.kind())).toList();
        assertFalse(floating.isEmpty());
        for (AtlasSystemArt.Piece piece : floating) {
            assertEquals(-1, piece.planet(), piece.kind() + " circles a planet");
            assertTrue(piece.orbit() > 0f && piece.speed() > 0f, piece.kind() + " does not orbit the star");
        }
    }

    @Test
    void theSameSystemAlwaysGetsTheSameScene() {
        ChartSystemSketch sketch = system("nebula_center_young", 3);

        assertEquals(AtlasSystemArt.layout(11L, sketch, ALL_ANOMALIES), AtlasSystemArt.layout(11L, sketch, ALL_ANOMALIES));
    }

    private static AtlasSystemArt.Frame frame(float tiltLevel) {
        OrbitalPlaneCamera camera = new OrbitalPlaneCamera();
        camera.aim(500f, 400f, 300f, tiltLevel);
        AtlasSystemArt.Frame frame = new AtlasSystemArt.Frame(camera);
        frame.radius = 400f;
        frame.reach = 300f;
        return frame;
    }

    private static AtlasSystemArt.Piece orbiting(AtlasSystemArt.Kind kind, float angle) {
        return new AtlasSystemArt.Piece(kind, -1, 0.8f, angle, 0.05f, 0f, 0);
    }

    @Test
    void untiltedAnomaliesQueueInLayoutOrderWithTheRelayLinksLast() {
        AtlasSystemArt.Layout layout = new AtlasSystemArt.Layout(null, List.of(orbiting(AtlasSystemArt.Kind.WRECK, 0f),
                orbiting(AtlasSystemArt.Kind.RELAY, 90f), orbiting(AtlasSystemArt.Kind.RELAY, 270f)));
        DepthOrder<AtlasSystemPreview.Layer> order = new DepthOrder<>();

        new AtlasSystemArt().queueAnomalies(layout, frame(0f), order);
        order.sortBackToFront();

        assertEquals(4, order.size());
        int[] expectedPieces = {0, 1, 2, 1};
        int[] expectedParts = {0, AtlasSystemArt.RELAY_SPRITE_PART, AtlasSystemArt.RELAY_SPRITE_PART, AtlasSystemArt.RELAY_LINK_PART};
        for (int rank = 0; rank < order.size(); rank++) {
            assertEquals(AtlasSystemPreview.Layer.ANOMALY, order.kind(rank));
            assertEquals(expectedPieces[rank], order.index(rank), "piece at rank " + rank);
            assertEquals(expectedParts[rank], order.part(rank), "part at rank " + rank);
        }
    }

    @Test
    void tiltedAnomaliesAtTheBackOfTheirOrbitQueueBehindThoseAtTheFront() {
        AtlasSystemArt.Layout layout = new AtlasSystemArt.Layout(null, List.of(orbiting(AtlasSystemArt.Kind.WRECK, -90f),
                orbiting(AtlasSystemArt.Kind.WRECK, 90f)));
        DepthOrder<AtlasSystemPreview.Layer> order = new DepthOrder<>();

        new AtlasSystemArt().queueAnomalies(layout, frame(1f), order);
        order.sortBackToFront();

        assertEquals(1, order.index(0));
        assertEquals(0, order.index(1));
    }

    @Test
    void anAccretionDiscSplitsIntoAHalfBehindTheStarAndAHalfInFrontOnceTilted() {
        AtlasSystemArt.Layout layout = new AtlasSystemArt.Layout(null, List.of(orbiting(AtlasSystemArt.Kind.ACCRETION, 0f)));
        AtlasSystemArt.Frame frame = frame(1f);
        frame.starPlanePixels = 40f;
        DepthOrder<AtlasSystemPreview.Layer> order = new DepthOrder<>();

        new AtlasSystemArt().queueAnomalies(layout, frame, order);
        order.sortBackToFront();

        assertEquals(2, order.size());
        assertTrue(order.depth(0) * order.depth(1) < 0f, "one half on each side of the star");
    }

    @Test
    void anomaliesStayOffSystemsTooSmallForArt() {
        AtlasSystemArt.Frame frame = frame(1f);
        frame.radius = AtlasSystemArt.MIN_ART_RADIUS - 1f;
        DepthOrder<AtlasSystemPreview.Layer> order = new DepthOrder<>();

        new AtlasSystemArt().queueAnomalies(new AtlasSystemArt.Layout(null, List.of(orbiting(AtlasSystemArt.Kind.WRECK, 0f))), frame, order);

        assertEquals(0, order.size());
    }
}