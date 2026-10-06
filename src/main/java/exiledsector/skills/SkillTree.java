package exiledsector.skills;

import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.RingBelt;
import exiledsector.skills.layout.Star;
import exiledsector.skills.layout.StaticImage;
import exiledsector.skills.loader.SkillTreeLoader;
import exiledsector.skills.loader.SkillTypeLoader;
import exiledsector.skills.loader.TreeRegionFilter;
import exiledsector.skills.loader.WormholePairValidator;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SkillTree {

    private static final Map<String, SkillNode> NODES = new LinkedHashMap<>();
    private static final Map<String, SkillType> TYPES = new LinkedHashMap<>();
    private static final Map<String, ConnectorCurve> CURVES = new LinkedHashMap<>();
    private static final Set<String> HIDDEN_CONNECTOR_KEYS = new HashSet<>();
    private static final List<StaticImage> STATIC_IMAGES = new ArrayList<>();
    private static final List<RingBelt> RING_BELTS = new ArrayList<>();
    private static final List<Star> STARS = new ArrayList<>();
    private static final Map<String, SkillNode> NODES_VIEW = Collections.unmodifiableMap(NODES);
    private static final Map<String, SkillType> TYPES_VIEW = Collections.unmodifiableMap(TYPES);
    private static final Map<String, SkillNode> DECLARED_NODES = new LinkedHashMap<>();
    private static SkillTreeLoader.ParsedTree declared;
    private static Set<String> disabledRegions = Set.of();
    private static boolean loadedCompletely;
    private static SkillTreeTopology topology;

    private SkillTree() {
    }

    public static void load() {
        clear();

        SkillTypeLoader.LoadedTypes loadedTypes = SkillTypeLoader.loadAll();
        Map<String, SkillType> types = loadedTypes.types();
        TYPES.putAll(types);

        declared = SkillTreeLoader.loadAll(types);
        for (SkillNode node : declared.nodes) {
            DECLARED_NODES.put(node.getId(), node);
        }
        loadedCompletely = declared.declaredNodeCount > 0 && DECLARED_NODES.size() == declared.declaredNodeCount
                && TYPES.size() == loadedTypes.declaredCount();
        for (String issue : WormholePairValidator.findIssues(DECLARED_NODES.values())) {
            Logger.getLogger(SkillTree.class).error(issue);
        }
        activate();
    }

    public static void applyDisabledRegions(Set<String> regions) {
        disabledRegions = Set.copyOf(regions);
        activate();
    }

    private static void activate() {
        if (declared == null) return;

        SkillTreeLoader.ParsedTree active = TreeRegionFilter.apply(declared, disabledRegions);
        clearLayout();
        NODES.clear();
        for (SkillNode node : active.nodes) {
            register(node);
        }
        CURVES.putAll(active.connectorCurves);
        HIDDEN_CONNECTOR_KEYS.addAll(active.hiddenConnectors);
        STATIC_IMAGES.addAll(active.staticImages);
        RING_BELTS.addAll(active.ringBelts);
        STARS.addAll(active.stars);
        topology = SkillTreeTopology.of(NODES.values());
    }

    public static void clear() {
        clearNodes();
        clearTypes();
        clearLayout();
    }

    private static void clearLayout() {
        CURVES.clear();
        HIDDEN_CONNECTOR_KEYS.clear();
        STATIC_IMAGES.clear();
        RING_BELTS.clear();
        STARS.clear();
    }

    public static SkillNode getDeclared(String nodeId) {
        SkillNode node = DECLARED_NODES.get(nodeId);
        return node != null ? node : NODES.get(nodeId);
    }

    public static void clearNodes() {
        NODES.clear();
        DECLARED_NODES.clear();
        declared = null;
        topology = null;
    }

    public static void clearTypes() {
        TYPES.clear();
    }

    public static SkillTreeTopology topology() {
        if (topology == null) {
            topology = SkillTreeTopology.of(NODES.values());
        }
        return topology;
    }

    public static boolean isLoadedCompletely() {
        return loadedCompletely;
    }

    public static void register(SkillNode node) {
        if (NODES.containsKey(node.getId())) {
            Logger.getLogger(SkillTree.class).error("Duplicate skill node id \"" + node.getId() + "\" - the earlier definition was overwritten.");
        }
        NODES.put(node.getId(), node);
        topology = null;
    }

    public static void unregister(String nodeId) {
        NODES.remove(nodeId);
        topology = null;
    }

    public static SkillNode get(String nodeId) {
        return NODES.get(nodeId);
    }

    public static Map<String, SkillNode> getAllNodes() {
        return NODES_VIEW;
    }

    public static SkillType getType(String typeId) {
        return TYPES.get(typeId);
    }

    public static void registerType(SkillType type) {
        if (TYPES.containsKey(type.getId())) {
            Logger.getLogger(SkillTree.class).error("Duplicate skill type id \"" + type.getId() + "\" - the earlier definition was overwritten.");
        }
        TYPES.put(type.getId(), type);
    }

    public static Map<String, SkillType> getAllTypes() {
        return TYPES_VIEW;
    }

    public static ConnectorCurve getCurve(String aId, String bId) {
        return CURVES.get(curveKey(aId, bId));
    }

    public static String curveKey(String aId, String bId) {
        return aId.compareTo(bId) <= 0 ? aId + "|" + bId : bId + "|" + aId;
    }

    public static boolean isConnectorVisible(String aId, String bId) {
        return !HIDDEN_CONNECTOR_KEYS.contains(curveKey(aId, bId));
    }

    public static List<StaticImage> getStaticImages() {
        return STATIC_IMAGES;
    }

    public static List<RingBelt> getRingBelts() {
        return RING_BELTS;
    }

    public static List<Star> getStars() {
        return STARS;
    }
}
