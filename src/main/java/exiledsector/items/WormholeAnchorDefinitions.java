package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.skills.SkillTree;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class WormholeAnchorDefinitions {

    public static final String DATA_PATH = "data/config/exiledSector/wormhole_anchors.csv";
    static final String VOLUME_COLUMN = "volume";
    static final String EXIT_NODE_COLUMN = "exit_node";
    private static final Logger LOG = Logger.getLogger(WormholeAnchorDefinitions.class);
    private static final AtomicReference<Map<String, AnchorTarget>> TARGETS_BY_ID = new AtomicReference<>(Map.of());

    public record AnchorTarget(String volume, String exitNodeId) {
    }

    private WormholeAnchorDefinitions() {
    }

    public static void load() {
        ModCsv.load("id", DATA_PATH, LOG, WormholeAnchorDefinitions::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        List<ItemDefinition> definitions = new ArrayList<>();
        Map<String, AnchorTarget> targets = new LinkedHashMap<>();
        Map<String, String> definitionIdsByVolume = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            try {
                ItemDefinition definition = ItemDefinition.parseWormholeAnchor(row);
                String volume = ModCsv.text(row, VOLUME_COLUMN);
                if (!isVolume(volume)) {
                    throw new IllegalArgumentException(definition.id() + " has volume \"" + volume + "\"; expected a tree region other than "
                            + SkillTags.CORE_REGION);
                }
                String exitNodeId = ModCsv.text(row, EXIT_NODE_COLUMN);
                if (exitNodeId.isEmpty()) {
                    throw new IllegalArgumentException(definition.id() + " has no exit node");
                }
                ItemDefinition otherItem = ItemDefinitions.get(definition.id());
                if ((otherItem != null && otherItem.type() != ItemType.WORMHOLE_ANCHOR) || targets.containsKey(definition.id())) {
                    throw new IllegalArgumentException(definition.id() + " is already defined");
                }
                String earlierAnchor = definitionIdsByVolume.putIfAbsent(volume, definition.id());
                if (earlierAnchor != null) {
                    throw new IllegalArgumentException(definition.id() + " leads to " + volume + ", which already has the anchor " + earlierAnchor);
                }
                if (!isKnownUnlock(definition.unlock())) {
                    LOG.warn(definition.id() + " in " + DATA_PATH + " has the unknown unlock \"" + definition.unlock()
                            + "\", so it stays inert; use an item unlock id, " + ItemUnlock.CLEARED_CHART_TIER_PREFIX + "N or leave it blank");
                }
                definitions.add(definition);
                targets.put(definition.id(), new AnchorTarget(volume, exitNodeId));
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        ItemDefinitions.registerWormholeAnchors(definitions);
        TARGETS_BY_ID.set(Collections.unmodifiableMap(targets));
    }

    static boolean isKnownUnlock(String unlockId) {
        return ItemUnlock.isKnownUnlock(unlockId);
    }

    static boolean isVolume(String region) {
        return SkillTags.isRegion(region) && !SkillTags.CORE_REGION.equals(region);
    }

    public static AnchorTarget target(String definitionId) {
        return definitionId == null ? null : TARGETS_BY_ID.get().get(ItemIdAliases.currentId(definitionId));
    }

    public static String volume(String definitionId) {
        AnchorTarget target = target(definitionId);
        return target == null ? null : target.volume();
    }

    public static String exitNodeId(String definitionId) {
        AnchorTarget target = target(definitionId);
        return target == null ? null : target.exitNodeId();
    }

    public static boolean areaActive(String definitionId) {
        String exitNodeId = exitNodeId(definitionId);
        return exitNodeId != null && SkillTree.get(exitNodeId) != null;
    }

    public static void clear() {
        TARGETS_BY_ID.set(Map.of());
        ItemDefinitions.clearWormholeAnchors();
    }
}
