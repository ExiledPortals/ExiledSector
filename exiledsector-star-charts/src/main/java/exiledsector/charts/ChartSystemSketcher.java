package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartSketchPalette.BodyKind;
import exiledsector.charts.ChartSketchPalette.StarKind;
import exiledsector.charts.ChartSystemSketch.Belt;
import exiledsector.charts.ChartSystemSketch.Body;
import exiledsector.charts.phenomenon.ChartPhenomenon;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleFunction;

public final class ChartSystemSketcher {

    static final long SKETCH_SALT = 0x534b45544348L;
    static final String NOTHING = "cat_nothing";
    static final String RINGS = "cat_terrain_rings";
    static final String FALLBACK_STAR = "star_white";
    static final float FALLBACK_STAR_RADIUS = 600f;
    static final float FALLBACK_PLANET_RADIUS = 120f;
    static final float CLOSE_STAR_GAP = 400f;
    static final float CLOSE_STAR_DAYS = 45f;
    static final float FIRST_ORBIT_BASE = 1000f;
    static final float FIRST_ORBIT_RANGE = 1000f;
    static final float ORBIT_GAP_BASE = 600f;
    static final float ORBIT_GAP_RANGE = 1200f;
    static final float BELT_WIDTH_BASE = 200f;
    static final float BELT_WIDTH_RANGE = 250f;
    static final float FAR_STAR_PAD = 1500f;
    static final int DEFAULT_MIN_ORBITS = 2;
    static final int DEFAULT_MAX_ORBITS = 7;
    static final int BELT_VARIANTS = 4;

    private ChartSystemSketcher() {
    }

    public static ChartSystemSketch sketch(long seed, ChartLayoutRecipe recipe, String phenomenonId, ChartSketchPalette palette) {
        ChartLayoutRecipe layoutRecipe = recipe == null ? ChartLayoutRecipe.ORDINARY : recipe;
        ChartPhenomena.Definition definition = ChartPhenomena.definition(phenomenonId);
        ChartPhenomenon phenomenon = definition != null && ChartPois.modRequirementMet(definition.requiredMod()) ? ChartPhenomena.byId(phenomenonId) : null;
        StarSystemType systemType = ChartPhenomenon.systemTypeOr(phenomenon,
                ChartLayoutGenerator.typeFor(new ChartLayoutRequest(seed, null).withRecipe(layoutRecipe)));
        StarAge age = ChartLayoutGenerator.pickAge(seed);
        Random random = ChartLayoutGenerator.derivedRandom(seed, SKETCH_SALT);
        boolean nebula = systemType == StarSystemType.NEBULA;

        List<Body> stars = new ArrayList<>();
        List<Body> planets = new ArrayList<>();
        List<Belt> belts = new ArrayList<>();
        float centreReach;
        if (systemType == StarSystemType.DEEP_SPACE) {
            centreReach = placeDeepSpaceCentre(palette, random, planets);
        } else {
            List<StarKind> centreKinds = new ArrayList<>();
            centreKinds.add(forcedStar(phenomenon, palette, nebula, layoutRecipe, age, random));
            for (int i = 0; i < closeStars(systemType); i++) {
                centreKinds.add(pickStar(palette, nebula, layoutRecipe, age, random));
            }
            centreReach = placeCentre(centreKinds, random, stars);
        }

        float outermost = addOrbits(layoutRecipe, palette, random, centreReach, planets, belts);

        float farAngle = random.nextFloat() * 360f;
        for (int i = 0; i < farStars(systemType); i++) {
            StarKind kind = pickStar(palette, nebula, layoutRecipe, age, random);
            float radius = starRadius(kind, random);
            float orbit = Math.max(outermost, centreReach) + FAR_STAR_PAD + radius;
            float angle = i == 0 ? farAngle : farAngle + 60f + 180f * random.nextFloat();
            stars.add(new Body(kind.typeId(), radius, orbit, orbit / (3f + random.nextFloat() * 2f), angle));
        }
        return new ChartSystemSketch(systemType, stars, planets, belts);
    }

    static int closeStars(StarSystemType systemType) {
        return switch (systemType) {
            case BINARY_CLOSE, TRINARY_1CLOSE_1FAR -> 1;
            case TRINARY_2CLOSE -> 2;
            default -> 0;
        };
    }

    static int farStars(StarSystemType systemType) {
        return switch (systemType) {
            case BINARY_FAR, TRINARY_1CLOSE_1FAR -> 1;
            case TRINARY_2FAR -> 2;
            default -> 0;
        };
    }

    private static float placeDeepSpaceCentre(ChartSketchPalette palette, Random random, List<Body> planets) {
        String typeId = ChartDeepSpace.pickCentre(random);
        BodyKind kind = null;
        for (List<BodyKind> kinds : palette.bodies().values()) {
            for (BodyKind candidate : kinds) {
                if (candidate.typeId().equals(typeId)) {
                    kind = candidate;
                }
            }
        }
        float radius = kind == null ? FALLBACK_PLANET_RADIUS : planetRadius(kind, random);
        planets.add(new Body(typeId, radius, 0f, 0f, random.nextFloat() * 360f));
        return radius;
    }

    private static float placeCentre(List<StarKind> kinds, Random random, List<Body> stars) {
        if (kinds.size() == 1) {
            float radius = starRadius(kinds.get(0), random);
            stars.add(new Body(kinds.get(0).typeId(), radius, 0f, 0f, 0f));
            return radius;
        }
        List<Float> radii = new ArrayList<>();
        float largest = 0f;
        for (StarKind kind : kinds) {
            float radius = starRadius(kind, random);
            radii.add(radius);
            largest = Math.max(largest, radius);
        }
        float circle = largest + CLOSE_STAR_GAP;
        float startAngle = random.nextFloat() * 360f;
        float days = CLOSE_STAR_DAYS * (1f + random.nextFloat());
        for (int i = 0; i < kinds.size(); i++) {
            stars.add(new Body(kinds.get(i).typeId(), radii.get(i), circle, days, startAngle + 360f * i / kinds.size()));
        }
        return circle + largest;
    }

    private static float addOrbits(ChartLayoutRecipe recipe, ChartSketchPalette palette, Random random, float centreReach, List<Body> planets,
                                   List<Belt> belts) {
        ChartShape shape = recipe.shape();
        int orbits = shape.setsOrbits()
                ? shape.minOrbits() + random.nextInt(shape.maxOrbits() - shape.minOrbits() + 1)
                : DEFAULT_MIN_ORBITS + random.nextInt(DEFAULT_MAX_ORBITS - DEFAULT_MIN_ORBITS + 1);
        float limit = shape.orbitRadiusLimit();
        float orbit = centreReach + FIRST_ORBIT_BASE + FIRST_ORBIT_RANGE * random.nextFloat();
        float outermost = centreReach;
        for (int i = 0; i < orbits && orbit < limit; i++) {
            String category = pickCategory(recipe, palette, random);
            if (RINGS.equals(category)) {
                BodyKind kind = pick(palette.bodies().get(category), BodyKind::frequency, random);
                float width = BELT_WIDTH_BASE + BELT_WIDTH_RANGE * random.nextFloat();
                float middle = orbit + width / 2f;
                belts.add(new Belt(kind.typeId(), middle, width, orbitDays(middle, random), random.nextInt(BELT_VARIANTS)));
                outermost = orbit + width;
                orbit += width;
            } else if (category != null && !NOTHING.equals(category)) {
                BodyKind kind = pick(palette.bodies().get(category), BodyKind::frequency, random);
                float radius = planetRadius(kind, random);
                float middle = orbit + radius;
                planets.add(new Body(kind.typeId(), radius, middle, orbitDays(middle, random), random.nextFloat() * 360f));
                outermost = orbit + radius * 2f;
                orbit += radius * 2f;
            }
            orbit += ORBIT_GAP_BASE + ORBIT_GAP_RANGE * random.nextFloat();
        }
        return outermost;
    }

    private static float orbitDays(float orbitRadius, Random random) {
        return orbitRadius / (20f + random.nextFloat() * 5f);
    }

    private static String pickCategory(ChartLayoutRecipe recipe, ChartSketchPalette palette, Random random) {
        List<Map.Entry<String, Float>> weighted = new ArrayList<>();
        for (Map.Entry<String, Float> entry : palette.categories().entrySet()) {
            String category = entry.getKey();
            boolean hasBodies = NOTHING.equals(category) || palette.bodies().containsKey(category);
            float weight = recipe.categoryWeight(category, entry.getValue());
            if (hasBodies && weight > 0f) {
                weighted.add(Map.entry(category, weight));
            }
        }
        Map.Entry<String, Float> picked = pick(weighted, Map.Entry::getValue, random);
        return picked == null ? null : picked.getKey();
    }

    private static StarKind forcedStar(ChartPhenomenon phenomenon, ChartSketchPalette palette, boolean nebula, ChartLayoutRecipe recipe,
                                       StarAge age, Random random) {
        String forced = phenomenon == null ? null : phenomenon.primaryStar();
        for (StarKind kind : palette.stars()) {
            if (kind.typeId().equals(forced)) {
                return kind;
            }
        }
        return pickStar(palette, nebula, recipe, age, random);
    }

    private static StarKind pickStar(ChartSketchPalette palette, boolean nebula, ChartLayoutRecipe recipe, StarAge age, Random random) {
        List<Map.Entry<StarKind, Float>> weighted = new ArrayList<>();
        for (StarKind kind : palette.stars()) {
            float weight = kind.nebula() == nebula ? recipe.starWeight(kind.typeId(), kind.frequency(age)) : 0f;
            if (weight > 0f) {
                weighted.add(Map.entry(kind, weight));
            }
        }
        Map.Entry<StarKind, Float> picked = pick(weighted, Map.Entry::getValue, random);
        return picked == null ? new StarKind(FALLBACK_STAR, 1f, 1f, 1f, FALLBACK_STAR_RADIUS, FALLBACK_STAR_RADIUS, false) : picked.getKey();
    }

    private static float starRadius(StarKind kind, Random random) {
        return between(kind.minRadius(), kind.maxRadius(), random);
    }

    private static float planetRadius(BodyKind kind, Random random) {
        float radius = between(kind.minRadius(), kind.maxRadius(), random);
        return radius > 0f ? radius : FALLBACK_PLANET_RADIUS;
    }

    private static float between(float min, float max, Random random) {
        return max > min ? min + (max - min) * random.nextFloat() : min;
    }

    private static <T> T pick(List<T> options, ToDoubleFunction<T> weightOf, Random random) {
        double total = 0.0;
        for (T option : options) {
            total += weightOf.applyAsDouble(option);
        }
        double roll = random.nextDouble() * total;
        for (T option : options) {
            roll -= weightOf.applyAsDouble(option);
            if (roll < 0.0) {
                return option;
            }
        }
        return options.isEmpty() ? null : options.get(options.size() - 1);
    }
}
