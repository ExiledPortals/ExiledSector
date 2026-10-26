package exiledsector.skills.loader;

import exiledsector.skills.decoration.AbyssSerpent;
import exiledsector.skills.decoration.Decoration;
import exiledsector.skills.decoration.DecorationKind;
import exiledsector.skills.decoration.Decorations;
import exiledsector.skills.decoration.RingBelt;
import exiledsector.skills.decoration.Star;
import exiledsector.skills.decoration.StaticImage;
import exiledsector.skills.decoration.ThreatUnit;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecorationLoaderTest {

    private static Decorations parse(String decorationsJson) throws Exception {
        return DecorationLoader.parse(new JSONObject("{ \"nodes\": [], \"decorations\": [" + decorationsJson + "] }"), "test tree");
    }

    private static final String IMAGE = "\"kind\": \"image\", \"x\": 0, \"y\": 0, \"width\": 100, \"height\": 100, \"imagePath\": \"a.png\"";

    @Test
    void aTreeWithoutDecorationsHasNone() throws Exception {
        assertSame(Decorations.NONE, DecorationLoader.parse(new JSONObject("{ \"nodes\": [] }"), "test tree"));
    }

    @Test
    void keepsEveryKindInFileOrderAndSortsThemByKind() throws Exception {
        Decorations decorations = parse("{ \"kind\": \"star\", \"id\": \"sun\", \"x\": 0, \"y\": 0, \"radius\": 400 },"
                + "{ \"id\": \"cloud\", " + IMAGE + " },"
                + "{ \"kind\": \"ringBelt\", \"id\": \"belt\", \"x\": 0, \"y\": 0, \"innerRadius\": 10, \"outerRadius\": 20, \"ringArtPath\": \"a.png\" },"
                + "{ \"kind\": \"threatUnit\", \"id\": \"hive\", \"x\": 0, \"y\": 0, \"unitType\": \"hive\", \"length\": 300 },"
                + "{ \"kind\": \"serpent\", \"id\": \"worm\", \"x\": 0, \"y\": 0, \"orbitRadius\": 900, \"orbitSeconds\": 120 }");

        assertEquals(List.of("sun", "cloud", "belt", "hive", "worm"), decorations.all().stream().map(Decoration::getId).toList());
        assertEquals(List.of(DecorationKind.STAR, DecorationKind.IMAGE, DecorationKind.RING_BELT, DecorationKind.THREAT_UNIT, DecorationKind.SERPENT),
                decorations.all().stream().map(Decoration::kind).toList());
        assertEquals(1, decorations.stars().size());
        assertEquals(1, decorations.images().size());
        assertEquals(1, decorations.ringBelts().size());
        assertEquals(1, decorations.threatUnits().size());
        assertEquals(1, decorations.serpents().size());
    }

    @Test
    void parsesAllFieldsOfAStaticImage() throws Exception {
        StaticImage image = parse("{ \"kind\": \"image\", \"id\": \"image_1\", \"x\": 1800, \"y\": 600, \"width\": 900, \"height\": 750,"
                + "\"imagePath\": \"graphics/backgrounds/static_images/hyperspace_cloud_03.png\", \"rotation\": 45, \"rotationSpeed\": 0.75 }").images().get(0);

        assertEquals("image_1", image.getId());
        assertEquals(1800f, image.getX());
        assertEquals(600f, image.getY());
        assertEquals(900f, image.getWidth());
        assertEquals(750f, image.getHeight());
        assertEquals("graphics/backgrounds/static_images/hyperspace_cloud_03.png", image.getImagePath());
        assertEquals(45f, image.getRotation());
        assertEquals(0.75f, image.getRotationSpeed());
    }

    @Test
    void anImageIsStillOnTheTreePlaneAndFacingUpUnlessItSaysOtherwise() throws Exception {
        List<StaticImage> images = parse("{ \"id\": \"backdrop\", " + IMAGE + ", \"behindStars\": true, \"parallax\": 0.8 },"
                + "{ \"id\": \"cloud\", " + IMAGE + " }").images();

        assertTrue(images.get(0).isBehindStars());
        assertEquals(0.8f, images.get(0).getParallax());
        assertFalse(images.get(1).isBehindStars());
        assertEquals(1f, images.get(1).getParallax());
        assertEquals(0f, images.get(1).getRotation());
        assertEquals(0f, images.get(1).getRotationSpeed());
    }

    @Test
    void parsesAllFieldsOfARingBelt() throws Exception {
        RingBelt belt = parse("{ \"kind\": \"ringBelt\", \"id\": \"outer_ringbelt\", \"x\": 100, \"y\": -50, \"innerRadius\": 2850, \"outerRadius\": 3150,"
                + "\"ringArtPath\": \"graphics/planets/ring_band_dust.png\", \"rotation\": 45, \"rotationSpeed\": -0.8 }").ringBelts().get(0);

        assertEquals("outer_ringbelt", belt.getId());
        assertEquals(100f, belt.getX());
        assertEquals(-50f, belt.getY());
        assertEquals(2850f, belt.getInnerRadius());
        assertEquals(3150f, belt.getOuterRadius());
        assertEquals("graphics/planets/ring_band_dust.png", belt.getRingArtPath());
        assertEquals(45f, belt.getRotation());
        assertEquals(-0.8f, belt.getRotationSpeed());
    }

    @Test
    void parsesAllFieldsOfAStarAndDefaultsTheRest() throws Exception {
        List<Star> stars = parse("{ \"kind\": \"star\", \"id\": \"central_star\", \"x\": 10, \"y\": -20, \"radius\": 850, \"starType\": \"star_blue_giant\","
                + "\"color\": \"#ffaa33\", \"overlayPath\": \"graphics/fx/crest.png\", \"overlaySize\": 0.9, \"overlayAlpha\": 0.4 },"
                + "{ \"kind\": \"star\", \"id\": \"plain_star\", \"x\": 0, \"y\": 0, \"radius\": 200 }").stars();

        Star star = stars.get(0);
        assertEquals("central_star", star.getId());
        assertEquals(10f, star.getX());
        assertEquals(-20f, star.getY());
        assertEquals(850f, star.getRadius());
        assertEquals("star_blue_giant", star.getStarType());
        assertEquals("#ffaa33", star.getColor());
        assertEquals(new Star.Overlay("graphics/fx/crest.png", 0.9f, 0.4f), star.getOverlay());
        assertEquals("star_yellow", stars.get(1).getStarType());
        assertNull(stars.get(1).getColor());
        assertNull(stars.get(1).getOverlay());
    }

    @Test
    void parsesAThreatUnitAndDefaultsItToAnUnturnedFabricator() throws Exception {
        List<ThreatUnit> units = parse("{ \"kind\": \"threatUnit\", \"id\": \"hive\", \"x\": 5, \"y\": 6, \"unitType\": \"hive\", \"length\": 450.5, \"rotation\": 35,"
                + "\"tags\": [\"threat\"] },"
                + "{ \"kind\": \"threatUnit\", \"id\": \"fab\", \"x\": 0, \"y\": 0, \"length\": 600 }").threatUnits();

        assertEquals(ThreatUnit.HIVE, units.get(0).getUnitType());
        assertEquals(450.5f, units.get(0).getLength());
        assertEquals(35f, units.get(0).getRotation());
        assertEquals("threat", units.get(0).getRegion());
        assertEquals(ThreatUnit.FABRICATOR, units.get(1).getUnitType());
        assertEquals(0f, units.get(1).getRotation());
    }

    @Test
    void parsesASerpentsOrbitAndWobbleAndDefaultsItToAPlainClockwiseCircle() throws Exception {
        List<AbyssSerpent> serpents = parse("{ \"kind\": \"serpent\", \"id\": \"worm\", \"x\": 10, \"y\": 20, \"orbitRadius\": 1663, \"orbitSeconds\": 190,"
                + "\"clockwise\": false, \"startAngle\": 180, \"wobble\": 0.15, \"wobbleWaves\": 7, \"wobblePhase\": 74.5, \"tags\": [\"abyss\"] },"
                + "{ \"kind\": \"serpent\", \"id\": \"plain\", \"x\": 0, \"y\": 0, \"orbitRadius\": 500, \"orbitSeconds\": 100 }").serpents();

        assertEquals(new AbyssSerpent.Orbit(1663f, 190f, false, 180f), serpents.get(0).getOrbit());
        assertEquals(new AbyssSerpent.Wobble(0.15f, 7, 74.5f), serpents.get(0).getWobble());
        assertEquals("abyss", serpents.get(0).getRegion());
        assertEquals(new AbyssSerpent.Orbit(500f, 100f, true, 0f), serpents.get(1).getOrbit());
        assertEquals(AbyssSerpent.Wobble.NONE, serpents.get(1).getWobble());
    }

    @Test
    void skipsAMalformedOrUnknownDecorationWithoutLosingTheRest() throws Exception {
        Decorations decorations = parse("{ \"kind\": \"ringBelt\", \"id\": \"broken\", \"x\": 0, \"y\": 0, \"innerRadius\": 10, \"outerRadius\": 20 },"
                + "{ \"kind\": \"comet\", \"id\": \"comet\", \"x\": 0, \"y\": 0 },"
                + "{ \"id\": \"no_kind\", \"x\": 0, \"y\": 0 },"
                + "{ \"kind\": \"ringBelt\", \"id\": \"belt\", \"x\": 0, \"y\": 0, \"innerRadius\": 10, \"outerRadius\": 20, \"ringArtPath\": \"a.png\" }");

        assertEquals(List.of("belt"), decorations.all().stream().map(Decoration::getId).toList());
    }

    @Test
    void theRetiredPerKindListsAreNoLongerRead() throws Exception {
        Decorations decorations = DecorationLoader.parse(new JSONObject("{ \"nodes\": [],"
                + "\"stars\": [ {\"id\": \"old_star\", \"x\": 0, \"y\": 0, \"radius\": 100} ],"
                + "\"decorations\": [ {\"kind\": \"star\", \"id\": \"new_star\", \"x\": 0, \"y\": 0, \"radius\": 100} ] }"), "test tree");

        assertEquals(List.of("new_star"), decorations.stars().stream().map(Star::getId).toList());
    }
}
