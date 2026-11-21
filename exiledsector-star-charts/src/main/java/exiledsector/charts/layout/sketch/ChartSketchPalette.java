package exiledsector.charts.layout.sketch;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.impl.campaign.procgen.CategoryGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.impl.campaign.procgen.TerrainGenDataSpec;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ChartSketchPalette(List<StarKind> stars, Map<String, Float> categories, Map<String, List<BodyKind>> bodies,
                                 Map<String, Map<String, Float>> categoryParentMultipliers) {

    public record StarKind(String typeId, float youngFrequency, float averageFrequency, float oldFrequency, float minRadius, float maxRadius,
                           boolean nebula) {

        float frequency(StarAge age) {
            return switch (age) {
                case YOUNG -> youngFrequency;
                case AVERAGE -> averageFrequency;
                case OLD -> oldFrequency;
                default -> 0f;
            };
        }
    }

    public record MoonOdds(float chance, float minMoons, float maxMoons) {

        public static final MoonOdds NONE = new MoonOdds(0f, 0f, 0f);
    }

    public record BodyKind(String typeId, float frequency, float minRadius, float maxRadius, MoonOdds moons,
                           Map<String, Float> parentMultipliers, boolean nebulaMoonOnly) {

        public BodyKind {
            moons = moons == null ? MoonOdds.NONE : moons;
            parentMultipliers = Map.copyOf(parentMultipliers);
        }

        public BodyKind(String typeId, float frequency, float minRadius, float maxRadius, MoonOdds moons, Map<String, Float> parentMultipliers) {
            this(typeId, frequency, minRadius, maxRadius, moons, parentMultipliers, false);
        }

        public BodyKind(String typeId, float frequency, float minRadius, float maxRadius) {
            this(typeId, frequency, minRadius, maxRadius, MoonOdds.NONE, Map.of());
        }

        public float parentMultiplier(String parentCategory) {
            return parentMultipliers.getOrDefault(parentCategory, 1f);
        }
    }

    public static final ChartSketchPalette EMPTY = new ChartSketchPalette(List.of(), Map.of(), Map.of());
    static final String PARENT_COLUMN_PREFIX = "cat_";
    static final Set<String> SKIPPED_CATEGORIES = Set.of("cat_terrain_lagrange", "cat_terrain_accretion", "cat_magfield");
    private static final Logger LOG = Logger.getLogger(ChartSketchPalette.class);
    private static ChartSketchPalette loaded;

    public ChartSketchPalette {
        stars = List.copyOf(stars);
        categories = Collections.unmodifiableMap(new LinkedHashMap<>(categories));
        Map<String, List<BodyKind>> copied = new LinkedHashMap<>();
        bodies.forEach((category, kinds) -> copied.put(category, List.copyOf(kinds)));
        bodies = Collections.unmodifiableMap(copied);
        Map<String, Map<String, Float>> copiedMultipliers = new LinkedHashMap<>();
        categoryParentMultipliers.forEach((category, multipliers) -> copiedMultipliers.put(category, Map.copyOf(multipliers)));
        categoryParentMultipliers = Collections.unmodifiableMap(copiedMultipliers);
    }

    public ChartSketchPalette(List<StarKind> stars, Map<String, Float> categories, Map<String, List<BodyKind>> bodies) {
        this(stars, categories, bodies, Map.of());
    }

    public float categoryParentMultiplier(String category, String parentCategory) {
        Map<String, Float> multipliers = categoryParentMultipliers.get(category);
        return multipliers == null ? 1f : multipliers.getOrDefault(parentCategory, 1f);
    }

    public static ChartSketchPalette get() {
        if (loaded == null) {
            loaded = fromSettings();
        }
        return loaded;
    }

    private static ChartSketchPalette fromSettings() {
        SettingsAPI settings = Global.getSettings();
        if (settings == null) {
            return EMPTY;
        }
        try {
            List<StarKind> stars = new ArrayList<>();
            for (StarGenDataSpec data : settings.getAllSpecs(StarGenDataSpec.class)) {
                stars.add(new StarKind(data.getId(), data.getFreqYOUNG(), data.getFreqAVERAGE(), data.getFreqOLD(), data.getMinRadius(),
                        data.getMaxRadius(), data.hasTag(StarSystemType.NEBULA.name())));
            }
            Map<String, Float> categories = new LinkedHashMap<>();
            Map<String, Map<String, Float>> categoryParentMultipliers = new LinkedHashMap<>();
            for (CategoryGenDataSpec data : settings.getAllSpecs(CategoryGenDataSpec.class)) {
                if (!SKIPPED_CATEGORIES.contains(data.getCategory())) {
                    categories.put(data.getCategory(), data.getFrequency());
                    categoryParentMultipliers.put(data.getCategory(), parentMultipliers(data.getMultipliers()));
                }
            }
            Map<String, List<BodyKind>> bodies = new LinkedHashMap<>();
            for (PlanetGenDataSpec data : settings.getAllSpecs(PlanetGenDataSpec.class)) {
                addBody(bodies, data.getCategory(), new BodyKind(data.getId(), data.getFrequency(), data.getMinRadius(), data.getMaxRadius(),
                        new MoonOdds(data.getProbOrbits(), data.getMinOrbits(), data.getMaxOrbits()), parentMultipliers(data.getMultipliers()),
                        data.hasTag(StarSystemGenerator.TAG_NOT_NEBULA_UNLESS_MOON)));
            }
            for (TerrainGenDataSpec data : settings.getAllSpecs(TerrainGenDataSpec.class)) {
                addBody(bodies, data.getCategory(), new BodyKind(data.getId(), data.getFrequency(), data.getMinRadius(), data.getMaxRadius(),
                        MoonOdds.NONE, parentMultipliers(data.getMultipliers()), data.hasTag(StarSystemGenerator.TAG_NOT_NEBULA_UNLESS_MOON)));
            }
            return new ChartSketchPalette(stars, categories, bodies, categoryParentMultipliers);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Reading the system generation tables for atlas previews failed", e);
            return EMPTY;
        }
    }

    static Map<String, Float> parentMultipliers(Map<String, Float> multipliers) {
        Map<String, Float> byParent = new LinkedHashMap<>();
        if (multipliers != null) {
            multipliers.forEach((column, multiplier) -> {
                if (column != null && column.startsWith(PARENT_COLUMN_PREFIX) && multiplier != null) {
                    byParent.put(column, multiplier);
                }
            });
        }
        return byParent;
    }

    private static void addBody(Map<String, List<BodyKind>> bodies, String category, BodyKind kind) {
        if (category != null && kind.typeId() != null && kind.frequency() > 0f) {
            bodies.computeIfAbsent(category, key -> new ArrayList<>()).add(kind);
        }
    }
}
