package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.data.EnumKeys;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartSystemFactory;
import exiledsector.charts.layout.abyss.AbyssPalette;
import exiledsector.charts.wormhole.ChartWormholePlacement;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.StarRule;
import exiledsector.charts.run.cleanup.ChartUsedNames;
import exiledsector.Seeds;
import exiledsector.items.WeightedPick;
import exiledsector.charts.anomaly.LayoutShaper;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ChartLayoutGenerator {

    public static final List<StarSystemType> ALLOWED_TYPES = List.of(StarSystemType.SINGLE, StarSystemType.BINARY_CLOSE,
            StarSystemType.BINARY_FAR, StarSystemType.TRINARY_2CLOSE, StarSystemType.TRINARY_1CLOSE_1FAR, StarSystemType.TRINARY_2FAR);
    public static final List<StarSystemType> RECIPE_TYPES = List.of(StarSystemType.SINGLE, StarSystemType.BINARY_CLOSE, StarSystemType.BINARY_FAR,
            StarSystemType.TRINARY_2CLOSE, StarSystemType.TRINARY_1CLOSE_1FAR, StarSystemType.TRINARY_2FAR, StarSystemType.DEEP_SPACE);
    static final Map<StarSystemType, Float> TYPE_WEIGHTS = Map.of(StarSystemType.SINGLE, 10f, StarSystemType.BINARY_CLOSE, 3f,
            StarSystemType.BINARY_FAR, 3f, StarSystemType.TRINARY_2CLOSE, 1f, StarSystemType.TRINARY_1CLOSE_1FAR, 1f,
            StarSystemType.TRINARY_2FAR, 1f);
    static final List<StarAge> AGES = List.of(StarAge.YOUNG, StarAge.AVERAGE, StarAge.OLD);
    static final List<String> CIVILISATION_CONDITIONS = List.of(Conditions.RUINS_SCATTERED, Conditions.RUINS_WIDESPREAD,
            Conditions.RUINS_EXTENSIVE, Conditions.RUINS_VAST, Conditions.DECIVILIZED, Conditions.DECIVILIZED_SUBPOP, Conditions.POLLUTION);
    static final long TYPE_SALT = 0x43484152545459L;
    static final long AGE_SALT = 0x434841525441L;
    static final long NAME_SALT = 0x434841525453L;
    static final long PLACEMENT_SALT = 0x43484152545057L;
    static final long LAYOUT_SALT = 0x4348415254594fL;
    static final long ABYSS_SALT = 0x43484152544142L;
    private static final Logger LOG = Logger.getLogger(ChartLayoutGenerator.class);
    private static final ChartLayoutEngine VANILLA_ENGINE = new VanillaChartLayoutEngine();
    private static ChartLayoutEngine engine = VANILLA_ENGINE;
    private static boolean failureReported;

    private ChartLayoutGenerator() {
    }

    public static boolean isAllowed(StarSystemType systemType) {
        return ALLOWED_TYPES.contains(systemType);
    }

    public static StarSystemType parseType(String text) {
        return parse(text, ALLOWED_TYPES);
    }

    public static StarSystemType parseRecipeType(String text) {
        return parse(text, RECIPE_TYPES);
    }

    private static StarSystemType parse(String text, List<StarSystemType> types) {
        StarSystemType parsed = EnumKeys.parse(StarSystemType.class, text);
        return parsed != null && types.contains(parsed) ? parsed : null;
    }

    public static StarSystemType systemTypeFor(ChartLayoutRequest request, Collection<String> anomalyIds) {
        return StarRule.systemTypeOr(ChartAnomalies.starOf(anomalyIds), typeFor(request));
    }

    public static boolean hasStar(StarSystemType systemType) {
        return systemType != StarSystemType.DEEP_SPACE && systemType != StarSystemType.NEBULA;
    }

    public static StarSystemType typeFor(ChartLayoutRequest request) {
        if (request.systemType() != null) {
            return request.systemType();
        }
        Map<StarSystemType, Float> weights = request.recipe().shape().systemTypeWeights();
        return pickType(Seeds.derived(request.seed(), TYPE_SALT), weights.isEmpty() ? TYPE_WEIGHTS : weights);
    }

    static StarSystemType pickType(Random typeRandom, Map<StarSystemType, Float> weights) {
        Map<StarSystemType, Float> ordered = new LinkedHashMap<>();
        RECIPE_TYPES.forEach(allowed -> ordered.put(allowed, weights.getOrDefault(allowed, 0f)));
        StarSystemType picked = WeightedPick.pick(ordered, typeRandom);
        return picked == null ? StarSystemType.SINGLE : picked;
    }

    public static StarAge pickAge(long seed) {
        return AGES.get(Seeds.derived(seed, AGE_SALT).nextInt(AGES.size()));
    }

    public static AbyssPalette abyssPalette(long seed, ChartLayoutRecipe recipe) {
        return recipe == null ? null : WeightedPick.pick(recipe.look().abyssPalettes(), Seeds.derived(seed, ABYSS_SALT));
    }

    static ChartLayoutRecipe settleAbyss(long seed, ChartLayoutRecipe recipe) {
        if (recipe == null || !recipe.look().abyssal()) {
            return recipe;
        }
        return new ChartLayoutRecipe(recipe.look().withAbyssPalette(abyssPalette(seed, recipe)), recipe.shape());
    }

    public static ChartLayout generate(SectorAPI sector, StarSystemAPI system, String runId, ChartLayoutRequest request) {
        long seed = request.seed();
        List<String> anomalyIds = ChartAnomalies.planned(request.chartContext());
        List<ChartAnomaly> anomalies = ChartAnomalies.anomaliesOf(anomalyIds, ChartAnomaly.class);
        StarSystemType systemType = systemTypeFor(request, anomalyIds);
        Set<Object> namesBefore = ChartUsedNames.snapshot();
        Built built;
        try {
            built = buildWithVanilla(system, runId, systemType, seed, request.recipe(), anomalies);
            if (!built.generated()) {
                scrub(system);
                ChartSystemFactory.addFallbackStar(system, runId);
            }
        } finally {
            ChartUsedNames.releaseAddedSince(namesBefore);
        }
        removeJumpPoints(system);
        removeHyperspacePresence(system);
        prepareMarkets(system, sector.getEconomy(), request.recipe().look().keepRuins());
        if (!request.recipe().look().musicSetId().isEmpty()) {
            system.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, request.recipe().look().musicSetId());
        }
        ChartBodyNames.assign(system, ChartBodyNames.designation(Seeds.derived(seed, NAME_SALT)));
        system.updateAllOrbits();
        SectorEntityToken anchor = built.featureAnchor();
        Vector2f avoid = anchor == null || anchor.getLocation() == null ? null : new Vector2f(anchor.getLocation());
        Vector2f innerWormhole = ChartWormholePlacement.choose(system, Seeds.derived(seed, PLACEMENT_SALT), avoid);
        List<String> shapedAnomalyIds = anomalyIds.stream()
                .filter(id -> built.generated() ? !built.unshapedAnomalyIds().contains(id) : needsNoLayout(id)).toList();
        return new ChartLayout(seed, systemType, built.generated(), innerWormhole, anchor, shapedAnomalyIds);
    }

    private static boolean needsNoLayout(String anomalyId) {
        return ChartAnomalies.role(anomalyId, StarRule.class) == null && ChartAnomalies.role(anomalyId, LayoutShaper.class) == null;
    }

    private record Built(boolean generated, SectorEntityToken featureAnchor, List<String> unshapedAnomalyIds) {
    }

    private static Built buildWithVanilla(StarSystemAPI system, String runId, StarSystemType systemType, long seed, ChartLayoutRecipe recipe,
                                          List<ChartAnomaly> anomalies) {
        try {
            ChartLayoutEngine.Result result = engine.build(system, ChartIds.starId(runId), systemType, pickAge(seed), Seeds.derived(seed, LAYOUT_SALT),
                    settleAbyss(seed, recipe), anomalies);
            if (system.getStar() == null && (systemType != StarSystemType.DEEP_SPACE || system.getCenter() == null)) {
                throw new IllegalStateException("the generator finished without a star");
            }
            return new Built(true, result.featureAnchor(), result.unshapedAnomalyIds());
        } catch (RuntimeException | LinkageError e) {
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] Generating a Star Chart layout (seed " + seed + ", " + systemType
                        + ") failed; using a single star instead. Later failures are logged at debug level", e);
            } else {
                LOG.debug("[ExiledSector] Generating a Star Chart layout (seed " + seed + ", " + systemType + ") failed again", e);
            }
            return new Built(false, null, List.of());
        }
    }

    static void scrub(StarSystemAPI system) {
        for (SectorEntityToken entity : new ArrayList<>(system.getAllEntities())) {
            system.removeEntity(entity);
        }
        system.setStar(null);
        system.setSecondary(null);
        system.setTertiary(null);
        system.setCenter(null);
        system.setType(StarSystemType.SINGLE);
    }

    static void removeJumpPoints(StarSystemAPI system) {
        for (SectorEntityToken jumpPoint : new ArrayList<>(system.getJumpPoints())) {
            system.removeEntity(jumpPoint);
        }
    }

    static void removeHyperspacePresence(StarSystemAPI system) {
        system.setHasSystemwideNebula(false);
        SectorEntityToken anchor = system.getHyperspaceAnchor();
        if (anchor == null) {
            return;
        }
        LocationAPI anchorLocation = anchor.getContainingLocation();
        if (anchorLocation != null) {
            anchorLocation.removeEntity(anchor);
        }
        system.setHyperspaceAnchor(null);
    }

    static void prepareMarkets(StarSystemAPI system, EconomyAPI economy, boolean keepRuins) {
        for (PlanetAPI planet : system.getPlanets()) {
            planet.addTag(Tags.NOT_RANDOM_MISSION_TARGET);
            MarketAPI market = planet.getMarket();
            if (planet.isStar() || market == null) {
                continue;
            }
            if (economy != null && market.getId() != null && economy.getMarket(market.getId()) == market) {
                economy.removeMarket(market);
            }
            market.setPlanetConditionMarketOnly(true);
            if (!keepRuins) {
                CIVILISATION_CONDITIONS.forEach(market::removeCondition);
            }
            market.setSurveyLevel(SurveyLevel.SEEN);
            for (MarketConditionAPI condition : market.getConditions()) {
                if (!condition.requiresSurveying()) {
                    condition.setSurveyed(true);
                }
            }
        }
    }

    static void useEngineForTests(ChartLayoutEngine testEngine) {
        engine = testEngine;
        failureReported = false;
    }

    public static void resetForTests() {
        engine = VANILLA_ENGINE;
        failureReported = false;
    }
}
