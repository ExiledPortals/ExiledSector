package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class AtlasPlanetRings {

    enum Kind { NONE, GIANT_RINGS, ASTEROID_BELT }

    static final String ICE_TEXTURE = "rings_ice0";
    static final String DUST_TEXTURE = "rings_dust0";
    static final String ASTEROID_TEXTURE = "rings_asteroids0";
    static final int TEXTURE_SLOTS = 4;
    static final float RINGED_GIANT_BODY_SHARE = 0.5f;
    static final float BELTED_PLANET_BODY_SHARE = 0.6f;
    static final float BELT_INNER_SHARE = 0.72f;
    static final float BELT_OUTER_SHARE = 0.98f;
    static final int BELT_ROCKS = 36;
    static final int ROCK_SPRITES = 12;

    private static final float RING_EDGE_JITTER = 0.015f;
    private static final float RING_DEGREES_PER_SECOND = 2.2f;
    private static final float BELT_DEGREES_PER_SECOND = 1.6f;
    private static final float ROCK_MIN_DEGREES_PER_SECOND = 1.8f;
    private static final float ROCK_DEGREES_SPREAD = 1.6f;
    private static final float ROCK_MIN_SIZE_SHARE = 0.018f;
    private static final float ROCK_SIZE_SPREAD = 0.03f;
    private static final float ROCK_MAX_SPIN_DEGREES_PER_SECOND = 40f;

    record Band(String textureId, int slot, float innerShare, float outerShare, float startDegrees, float degreesPerSecond, float alpha) {
    }

    record Rock(int sprite, float orbitShare, float startDegrees, float degreesPerSecond, float sizeShare, float facingDegrees,
                float spinDegreesPerSecond) {
    }

    private record BandShape(float innerShare, float outerShare, float speedShare, float alpha) {
    }

    private static final BandShape[] GIANT_BANDS = {
            new BandShape(0.56f, 0.66f, 1.3f, 0.5f),
            new BandShape(0.64f, 0.84f, 1.1f, 1f),
            new BandShape(0.66f, 0.82f, 0.8f, 0.55f),
            new BandShape(0.87f, 0.98f, 0.7f, 0.85f),
    };
    private static final BandShape[] BELT_BANDS = {
            new BandShape(BELT_INNER_SHARE, 0.88f, 1.05f, 0.9f),
            new BandShape(0.82f, BELT_OUTER_SHARE, 1f, 0.9f),
    };

    private final Kind kind;
    private final List<Band> bands;
    private final List<Rock> rocks;

    private AtlasPlanetRings(Kind kind, List<Band> bands, List<Rock> rocks) {
        this.kind = kind;
        this.bands = List.copyOf(bands);
        this.rocks = List.copyOf(rocks);
    }

    static Kind kindOf(boolean gasGiant, String siteLight, AtlasBodyArt.Field field) {
        if (field != AtlasBodyArt.Field.NONE) {
            return Kind.NONE;
        }
        if (gasGiant) {
            return Kind.GIANT_RINGS;
        }
        return siteLight != null ? Kind.ASTEROID_BELT : Kind.NONE;
    }

    static AtlasPlanetRings of(Kind kind, boolean icy, long seed) {
        Random random = new Random(seed);
        return switch (kind) {
            case NONE -> null;
            case GIANT_RINGS -> new AtlasPlanetRings(kind, giantBands(icy, random), List.of());
            case ASTEROID_BELT -> new AtlasPlanetRings(kind, beltBands(random), beltRocks(random));
        };
    }

    private static List<Band> giantBands(boolean icy, Random random) {
        List<Band> bands = new ArrayList<>();
        for (int index = 0; index < GIANT_BANDS.length; index++) {
            BandShape shape = GIANT_BANDS[index];
            boolean outermost = index == GIANT_BANDS.length - 1;
            String texture = icy || outermost ? ICE_TEXTURE : DUST_TEXTURE;
            float inner = shape.innerShare() + jitter(random);
            float outer = Math.min(BELT_OUTER_SHARE, shape.outerShare() + jitter(random));
            bands.add(new Band(texture, random.nextInt(TEXTURE_SLOTS), inner, outer, random.nextFloat() * 360f,
                    RING_DEGREES_PER_SECOND * shape.speedShare(), shape.alpha()));
        }
        return bands;
    }

    private static float jitter(Random random) {
        return (random.nextFloat() * 2f - 1f) * RING_EDGE_JITTER;
    }

    private static List<Band> beltBands(Random random) {
        List<Band> bands = new ArrayList<>();
        for (BandShape shape : BELT_BANDS) {
            bands.add(new Band(ASTEROID_TEXTURE, random.nextInt(TEXTURE_SLOTS), shape.innerShare(), shape.outerShare(), random.nextFloat() * 360f,
                    BELT_DEGREES_PER_SECOND * shape.speedShare(), shape.alpha()));
        }
        return bands;
    }

    private static List<Rock> beltRocks(Random random) {
        List<Rock> rocks = new ArrayList<>();
        for (int index = 0; index < BELT_ROCKS; index++) {
            float depth = (random.nextFloat() + random.nextFloat()) / 2f;
            float orbit = BELT_INNER_SHARE + (BELT_OUTER_SHARE - BELT_INNER_SHARE) * depth;
            float speed = (ROCK_MIN_DEGREES_PER_SECOND + random.nextFloat() * ROCK_DEGREES_SPREAD) * BELT_INNER_SHARE / orbit;
            float spin = (random.nextFloat() * 2f - 1f) * ROCK_MAX_SPIN_DEGREES_PER_SECOND;
            rocks.add(new Rock(random.nextInt(ROCK_SPRITES), orbit, random.nextFloat() * 360f, speed,
                    ROCK_MIN_SIZE_SHARE + random.nextFloat() * ROCK_SIZE_SPREAD, random.nextFloat() * 360f, spin));
        }
        return rocks;
    }

    Kind kind() {
        return kind;
    }

    List<Band> bands() {
        return bands;
    }

    List<Rock> rocks() {
        return rocks;
    }

    float bodyShare() {
        return kind == Kind.GIANT_RINGS ? RINGED_GIANT_BODY_SHARE : BELTED_PLANET_BODY_SHARE;
    }
}
