package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.CategoryGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import exiledsector.charts.feature.ChartFeature;
import exiledsector.charts.feature.FeatureSite;
import exiledsector.charts.phenomenon.ChartPhenomenon;
import exiledsector.charts.phenomenon.PhenomenonSite;
import exiledsector.charts.phenomenon.StellarFlares;
import exiledsector.compat.UnknownSkiesCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ChartStarSystemGenerator extends StarSystemGenerator {

    static final float BINARY_PAD = 1500f;
    static final Map<String, List<String>> BACKGROUNDS_BY_NEBULA = Map.of(
            NEBULA_NONE, List.of("graphics/backgrounds/background2.jpg", "graphics/backgrounds/background4.jpg"),
            NEBULA_BLUE, List.of("graphics/backgrounds/background5.jpg"),
            NEBULA_AMBER, List.of("graphics/backgrounds/background6.jpg"),
            NEBULA_DEFAULT, List.of("graphics/backgrounds/background1.jpg", "graphics/backgrounds/background2.jpg"));

    private final ChartLayoutRecipe recipe;
    private final ChartPhenomenon phenomenon;
    private SectorEntityToken featureAnchor;
    private PlanetAPI deepSpaceCentre;
    private String bodyIdPrefix;
    private boolean primaryForced;

    ChartStarSystemGenerator(StarAge age, StarSystemType chartType, ChartLayoutRecipe recipe, ChartPhenomenon phenomenon) {
        super(new CustomConstellationParams(age));
        this.phenomenon = phenomenon;
        systemType = ChartPhenomenon.systemTypeOr(phenomenon, chartType);
        this.recipe = recipe == null ? ChartLayoutRecipe.ORDINARY : recipe;
    }

    @Override
    public void pickNebulaAndBackground() {
        float nebulaChance = Float.isNaN(recipe.look().nebulaChance()) ? constellationAgeData.getProbNebula() : recipe.look().nebulaChance();
        boolean hasNebula = nebulaChance > random.nextFloat() || systemType == StarSystemType.NEBULA;
        String picked = hasNebula ? nebulaTypes.get(constellationAge) : null;
        nebulaType = picked == null || !BACKGROUNDS_BY_NEBULA.containsKey(picked) ? NEBULA_NONE : picked;
        List<String> pool = NEBULA_NONE.equals(nebulaType) ? recipe.look().backgrounds() : recipe.look().nebulaBackgrounds();
        List<String> backgrounds = new ArrayList<>(pool.isEmpty() ? BACKGROUNDS_BY_NEBULA.get(nebulaType) : pool);
        backgrounds.addAll(UnknownSkiesCompat.backgrounds(constellationAge, !NEBULA_NONE.equals(nebulaType)));
        backgroundName = backgrounds.get(random.nextInt(backgrounds.size()));
    }

    @Override
    public PlanetSpecAPI pickStar(StarAge age) {
        if (phenomenon != null && phenomenon.primaryStar() != null && !primaryForced) {
            primaryForced = true;
            for (PlanetSpecAPI spec : Global.getSettings().getAllPlanetSpecs()) {
                if (spec.isStar() && phenomenon.primaryStar().equals(spec.getPlanetType())) {
                    return spec;
                }
            }
        }
        WeightedRandomPicker<PlanetSpecAPI> picker = new WeightedRandomPicker<>(random);
        for (PlanetSpecAPI spec : Global.getSettings().getAllPlanetSpecs()) {
            if (!spec.isStar()) {
                continue;
            }
            Object found = Global.getSettings().getSpec(StarGenDataSpec.class, spec.getPlanetType(), true);
            if (!(found instanceof StarGenDataSpec data) || data.hasTag(StarSystemType.NEBULA.name()) != (systemType == StarSystemType.NEBULA)) {
                continue;
            }
            float weight = recipe.starWeight(spec.getPlanetType(), frequency(data, age));
            if (weight > 0f) {
                picker.add(spec, weight);
            }
        }
        return picker.pick();
    }

    @Override
    public CategoryGenDataSpec pickCategory(GenContext context, String extraMult, boolean nothingOk) {
        String starType = star != null ? star.getTypeId() : null;
        if (context.center instanceof PlanetAPI centre && centre.isStar()) {
            starType = centre.getTypeId();
        }
        WeightedRandomPicker<CategoryGenDataSpec> picker = new WeightedRandomPicker<>(random);
        for (CategoryGenDataSpec categoryData : Global.getSettings().getAllSpecs(CategoryGenDataSpec.class)) {
            if (context.excludeCategories.contains(categoryData.getCategory())) {
                continue;
            }
            boolean nothing = CAT_NOTHING.equals(categoryData.getCategory());
            if (!nothingOk && nothing) {
                continue;
            }
            float weight = categoryData.getFrequency();
            if (context.age != null) {
                weight *= categoryData.getMultiplier(context.age);
            }
            if (starType != null) {
                weight *= categoryData.getMultiplier(starType);
            }
            if (context.parentCategory != null) {
                weight *= categoryData.getMultiplier(context.parentCategory);
            }
            for (String column : context.multipliers) {
                weight *= categoryData.getMultiplier(column);
            }
            if (extraMult != null) {
                weight *= categoryData.getMultiplier(extraMult);
            }
            weight = recipe.categoryWeight(categoryData.getCategory(), weight);
            if (weight > 0f && (nothing || !isCategoryEmpty(categoryData, context, extraMult, nothingOk))) {
                picker.add(categoryData, weight);
            }
        }
        return picker.pick();
    }

    @Override
    protected GenResult addPlanetsAndTerrain(float maxOrbitRadius) {
        float orbitLimit = Math.min(maxOrbitRadius, recipe.shape().orbitRadiusLimit());
        if (!recipe.shape().setsOrbits()) {
            return super.addPlanetsAndTerrain(orbitLimit);
        }
        int numOrbits = Math.round(getNormalRandom(recipe.shape().minOrbits(), recipe.shape().maxOrbits()));
        if (numOrbits <= 0) {
            return null;
        }
        float currentRadius = centerRadius + STARTING_RADIUS_STAR_BASE + STARTING_RADIUS_STAR_RANGE * random.nextFloat();
        GenContext context = new GenContext(this, system, systemCenter, starData, null, 0, starAge.name(), currentRadius, orbitLimit, null, -1);
        if (systemType == StarSystemType.BINARY_CLOSE || systemType == StarSystemType.TRINARY_1CLOSE_1FAR) {
            context.multipliers.add(COL_BINARY);
        }
        if (systemType == StarSystemType.TRINARY_2CLOSE) {
            context.multipliers.add(COL_TRINARY);
        }
        GenResult result = addOrbitingEntities(context, numOrbits, false, true, false, true);
        result.context = context;
        return result;
    }

    void generateLayout(StarSystemAPI system, String starId) {
        init(system, constellationAge);
        system.setType(systemType);
        pickNebulaAndBackground();
        system.setBackgroundTextureFilename(backgroundName);
        star = null;
        secondary = null;
        tertiary = null;
        systemCenter = null;
        bodyIdPrefix = starId;
        if (systemType == StarSystemType.DEEP_SPACE) {
            addDeepSpaceCentre();
        } else if (!addStars(starId)) {
            throw new IllegalStateException("no star type could be picked for a " + systemType + " Star Chart layout");
        }
        for (PlanetAPI body : pickedStars()) {
            StellarFlares.setFlareChance(body, 0f);
            UnknownSkiesCompat.varyStar(body, random);
        }
        system.setLightColor(recipe.look().tinted(system.getLightColor()));
        updateAgeAfterPickingStar();
        GenResult result = addPlanetsAndTerrain(MAX_ORBIT_RADIUS);
        featureAnchor = buildFeature();
        if (featureAnchor == null) {
            featureAnchor = deepSpaceCentre;
        }
        addFarStars(result);
        addSystemwideNebula();
        if (systemType == StarSystemType.NEBULA) {
            hideNebulaCentre();
        }
        if (phenomenon != null) {
            phenomenon.shape(new PhenomenonSite(system, centre(), visibleStars(),
                    ChartWormholePlacement.outermost(system, List.of()), random));
        }
        system.setAge(starAge);
    }

    private void addDeepSpaceCentre() {
        systemCenter = system.initNonStarCenter();
        systemCenter.addTag(Tags.AMBIENT_LS);
        system.setLightColor(ChartDeepSpace.AMBIENT_LIGHT);
        starData = (StarGenDataSpec) Global.getSettings().getSpec(StarGenDataSpec.class, ChartDeepSpace.centreDataId(constellationAge), false);
        String centreType = ChartDeepSpace.pickCentre(random);
        Object found = Global.getSettings().getSpec(PlanetGenDataSpec.class, centreType, true);
        if (!(found instanceof PlanetGenDataSpec planetData)) {
            throw new IllegalStateException("the deep space centre " + centreType + " has no planet generation data");
        }
        GenContext context = new GenContext(this, system, systemCenter, starData, null, 0, constellationAge.name(), 0f, MAX_ORBIT_RADIUS, null, -1);
        context.lagrangeParent = new GeneratedPlanet(null, null, false, 0f, 0f, 0f);
        context.excludeCategories.addAll(List.of("cat_hab2", "cat_hab3", "cat_hab4", "cat_hab5"));
        GenResult result = addPlanet(context, planetData, false, true);
        if (result == null || result.entities.isEmpty() || !(result.entities.get(0) instanceof PlanetAPI centre)) {
            throw new IllegalStateException("the deep space centre " + centreType + " could not be added");
        }
        centre.setOrbit(null);
        centre.setLocation(0f, 0f);
        if (ChartDeepSpace.BARREN_DESCRIPTION.equals(centre.getSpec().getDescriptionId())) {
            centre.setDescriptionIdOverride(ChartDeepSpace.DEEP_SPACE_BARREN_DESCRIPTION);
        }
        centerRadius = centre.getRadius();
        deepSpaceCentre = centre;
    }

    private void hideNebulaCentre() {
        PlanetAPI centreStar = star;
        system.removeEntity(centreStar);
        StarCoronaTerrainPlugin corona = Misc.getCoronaFor(centreStar);
        if (corona != null) {
            system.removeEntity(corona.getEntity());
        }
        system.setStar(null);
        system.initNonStarCenter();
        for (SectorEntityToken entity : new ArrayList<>(system.getAllEntities())) {
            if (entity.getOrbitFocus() == centreStar || entity.getOrbitFocus() == system.getCenter()) {
                entity.setOrbit(null);
            }
        }
        system.getCenter().addTag(Tags.AMBIENT_LS);
        system.setStar(centreStar);
        systemCenter = system.getCenter();
    }

    private List<PlanetAPI> visibleStars() {
        List<PlanetAPI> stars = new ArrayList<>();
        for (PlanetAPI body : pickedStars()) {
            if (body != null && system.getPlanets().contains(body)) {
                stars.add(body);
            }
        }
        return stars;
    }

    SectorEntityToken featureAnchor() {
        return featureAnchor;
    }

    private SectorEntityToken buildFeature() {
        ChartFeature feature = ChartFeatures.byId(recipe.shape().featureId());
        SectorEntityToken centre = centre();
        if (feature == null || centre == null) {
            return null;
        }
        StarAge age = starAge;
        return feature.build(new FeatureSite(system, centre, ChartWormholePlacement.outermost(system, farStars()), bodyIdPrefix + "_feature", random,
                planet -> PlanetConditionGenerator.generateConditionsForPlanet(planet, age)));
    }

    private List<PlanetAPI> pickedStars() {
        return Arrays.asList(star, secondary, tertiary);
    }

    private SectorEntityToken centre() {
        return systemCenter != null ? systemCenter : star;
    }

    private static float frequency(StarGenDataSpec data, StarAge age) {
        return switch (age) {
            case YOUNG -> data.getFreqYOUNG();
            case AVERAGE -> data.getFreqAVERAGE();
            case OLD -> data.getFreqOLD();
            default -> 0f;
        };
    }

    private void addFarStars(GenResult result) {
        float primaryOrbitalRadius = primaryOrbitalRadius(result);
        float orbitAngle = random.nextFloat() * 360f;
        float baseOrbitRadius = primaryOrbitalRadius + BINARY_PAD + farStarReach();
        float orbitDays = baseOrbitRadius / (3f + random.nextFloat() * 2f);
        if (systemType == StarSystemType.BINARY_FAR && secondary != null) {
            addFarStar(secondary, orbitAngle, baseOrbitRadius, orbitDays);
        } else if (systemType == StarSystemType.TRINARY_1CLOSE_1FAR && tertiary != null) {
            addFarStar(tertiary, orbitAngle, baseOrbitRadius, orbitDays);
        } else if (systemType == StarSystemType.TRINARY_2FAR && secondary != null && tertiary != null) {
            addFarStar(secondary, orbitAngle, baseOrbitRadius, orbitDays);
            addFarStar(tertiary, orbitAngle + 60f + 180f * random.nextFloat(), baseOrbitRadius, orbitDays);
        }
    }

    private float farStarReach() {
        float reach = 0f;
        for (PlanetAPI farStar : farStars()) {
            StarCoronaTerrainPlugin corona = Misc.getCoronaFor(farStar);
            float coronaWidth = corona == null || corona.getParams() == null ? farStar.getRadius() : corona.getParams().bandWidthInEngine;
            reach = Math.max(reach, farStar.getRadius() + coronaWidth);
        }
        return reach;
    }

    private float primaryOrbitalRadius(GenResult result) {
        float planets = result != null ? result.orbitalWidth * 0.5f : star == null ? 0f : star.getRadius();
        return featureAnchor == null ? planets : Math.max(planets, ChartWormholePlacement.outermost(system, farStars()));
    }

    private List<PlanetAPI> farStars() {
        return switch (systemType) {
            case BINARY_FAR -> nonNull(secondary);
            case TRINARY_1CLOSE_1FAR -> nonNull(tertiary);
            case TRINARY_2FAR -> nonNull(secondary, tertiary);
            default -> List.of();
        };
    }

    private static List<PlanetAPI> nonNull(PlanetAPI... bodies) {
        return Arrays.stream(bodies).filter(Objects::nonNull).toList();
    }

    @Override
    protected void addJumpPoints(GenResult result, boolean farStarMode) {
    }

    @Override
    protected void addJumpPoint(LagrangePoint point, String name) {
    }

    @Override
    protected void addStableLocations() {
    }
}
