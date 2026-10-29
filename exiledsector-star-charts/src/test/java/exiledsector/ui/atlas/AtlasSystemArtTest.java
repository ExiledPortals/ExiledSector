package exiledsector.ui.atlas;

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

    private static final List<String> ALL_ANOMALIES = List.of("black_hole", "neutron_star", "asteroid_storm", "magnetic_storm", "stellar_flares",
            "spatial_breach", "powerful_boss", "faction_skirmish", "relay_network", "supply_convoy", "anomalous_debris", "excavation_sites",
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
}
