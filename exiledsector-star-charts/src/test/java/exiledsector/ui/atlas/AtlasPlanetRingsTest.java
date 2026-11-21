package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Field;
import exiledsector.ui.atlas.AtlasPlanetRings.Band;
import exiledsector.ui.atlas.AtlasPlanetRings.Kind;
import exiledsector.ui.atlas.AtlasPlanetRings.Rock;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasPlanetRingsTest {

    private static final String BOLT_HOLE_LIGHT = "#ffbe5a";

    @Test
    void gasGiantsGetRingsAndBoltHolePlanetsGetAsteroidBelts() {
        assertEquals(Kind.GIANT_RINGS, AtlasPlanetRings.kindOf(true, null, Field.NONE));
        assertEquals(Kind.ASTEROID_BELT, AtlasPlanetRings.kindOf(false, BOLT_HOLE_LIGHT, Field.NONE));
        assertEquals(Kind.NONE, AtlasPlanetRings.kindOf(false, null, Field.NONE));
        assertNull(AtlasPlanetRings.of(Kind.NONE, false, 1L));
    }

    @Test
    void magneticFieldPlanetsStayBare() {
        for (Field field : EnumSet.complementOf(EnumSet.of(Field.NONE))) {
            assertEquals(Kind.NONE, AtlasPlanetRings.kindOf(true, null, field), field.name());
            assertEquals(Kind.NONE, AtlasPlanetRings.kindOf(false, BOLT_HOLE_LIGHT, field), field.name());
        }
    }

    @Test
    void ringsSitBetweenTheShrunkenGiantAndItsNodeRing() {
        for (long seed = 0; seed < 200; seed++) {
            AtlasPlanetRings rings = AtlasPlanetRings.of(Kind.GIANT_RINGS, seed % 2 == 0, seed);
            for (Band band : rings.bands()) {
                assertTrue(band.innerShare() > rings.bodyShare(), "seed " + seed);
                assertTrue(band.outerShare() <= 1f, "seed " + seed);
                assertTrue(band.innerShare() < band.outerShare(), "seed " + seed);
                assertTrue(band.slot() >= 0 && band.slot() < AtlasPlanetRings.TEXTURE_SLOTS, "seed " + seed);
            }
            assertTrue(rings.rocks().isEmpty());
        }
    }

    @Test
    void iceGiantsWearIceRingsWhileGasGiantsMixInDust() {
        AtlasPlanetRings icy = AtlasPlanetRings.of(Kind.GIANT_RINGS, true, 7L);
        assertTrue(icy.bands().stream().allMatch(band -> band.textureId().equals(AtlasPlanetRings.ICE_TEXTURE)));
        AtlasPlanetRings warm = AtlasPlanetRings.of(Kind.GIANT_RINGS, false, 7L);
        assertTrue(warm.bands().stream().anyMatch(band -> band.textureId().equals(AtlasPlanetRings.DUST_TEXTURE)));
        assertTrue(warm.bands().stream().anyMatch(band -> band.textureId().equals(AtlasPlanetRings.ICE_TEXTURE)));
    }

    @Test
    void beltRocksOrbitInsideTheBeltClearOfThePlanet() {
        for (long seed = 0; seed < 50; seed++) {
            AtlasPlanetRings belt = AtlasPlanetRings.of(Kind.ASTEROID_BELT, false, seed);
            assertEquals(AtlasPlanetRings.BELT_ROCKS, belt.rocks().size());
            assertFalse(belt.bands().isEmpty());
            assertTrue(belt.bands().stream().allMatch(band -> band.textureId().equals(AtlasPlanetRings.ASTEROID_TEXTURE)));
            for (Rock rock : belt.rocks()) {
                assertTrue(rock.orbitShare() >= AtlasPlanetRings.BELT_INNER_SHARE && rock.orbitShare() <= AtlasPlanetRings.BELT_OUTER_SHARE);
                assertTrue(rock.orbitShare() - rock.sizeShare() > belt.bodyShare(), "seed " + seed);
                assertTrue(rock.sprite() >= 0 && rock.sprite() < AtlasPlanetRings.ROCK_SPRITES);
                assertTrue(rock.degreesPerSecond() > 0f);
            }
        }
    }

    @Test
    void theSameNodeAlwaysGetsTheSameRings() {
        AtlasPlanetRings first = AtlasPlanetRings.of(Kind.ASTEROID_BELT, false, 99L);
        AtlasPlanetRings second = AtlasPlanetRings.of(Kind.ASTEROID_BELT, false, 99L);
        assertEquals(first.bands(), second.bands());
        assertEquals(first.rocks(), second.rocks());
    }
}
