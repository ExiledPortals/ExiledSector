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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class WormholeAnchorDefinitions {

    public static final String DATA_PATH = "data/config/exiledSector/wormhole_anchors.csv";
    static final String VOLUME_COLUMN = "volume";
    static final String EXIT_NODE_COLUMN = "exit_node";
    static final String DROP_FACTIONS_COLUMN = "drop_factions";
    static final String DROP_CHANCE_COLUMN = "drop_chance";
    static final String LIST_SEPARATOR = ";";
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
        Map<String, FactionFleetDrops.Source> dropSources = new LinkedHashMap<>();
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
                float dropChance = dropChance(definition.id(), ModCsv.text(row, DROP_CHANCE_COLUMN));
                definitions.add(definition);
                targets.put(definition.id(), new AnchorTarget(volume, exitNodeId));
                dropSources.put(definition.id(), new FactionFleetDrops.Source(volume, dropFactions(ModCsv.text(row, DROP_FACTIONS_COLUMN)), dropChance));
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        ItemDefinitions.registerWormholeAnchors(definitions);
        TARGETS_BY_ID.set(Collections.unmodifiableMap(targets));
        FactionFleetDrops.register(ItemType.WORMHOLE_ANCHOR, dropSources);
    }

    static float dropChance(String definitionId, String chanceText) {
        if (chanceText.isEmpty()) {
            return 0f;
        }
        if (!isValidDropChance(chanceText)) {
            LOG.error(definitionId + " in " + DATA_PATH + " has the drop chance \"" + chanceText
                    + "\"; expected a number of 0 or more, so it never drops from fleets");
            return 0f;
        }
        return parsedOrNaN(chanceText);
    }

    static boolean isValidDropChance(String chanceText) {
        float chance = parsedOrNaN(chanceText);
        return chanceText.isEmpty() || chance >= 0f && !Float.isInfinite(chance);
    }

    private static float parsedOrNaN(String numberText) {
        try {
            return Float.parseFloat(numberText);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    static Set<String> dropFactions(String factionsText) {
        Set<String> factionIds = new LinkedHashSet<>();
        for (String factionId : factionsText.split(LIST_SEPARATOR)) {
            if (!factionId.isBlank()) {
                factionIds.add(factionId.trim());
            }
        }
        return factionIds;
    }

    public static String definitionIdFor(String volume) {
        for (Map.Entry<String, AnchorTarget> target : TARGETS_BY_ID.get().entrySet()) {
            if (target.getValue().volume().equals(volume)) {
                return target.getKey();
            }
        }
        return null;
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
        FactionFleetDrops.clear(ItemType.WORMHOLE_ANCHOR);
    }
}
