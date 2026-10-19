package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicReference;

public final class ModspecCaches {

    public static final String DROP_SOURCE_GROUP = "modspec_cache";
    static final String ID_COLUMN = "id";
    static final String EXCLUDED_NODES_COLUMN = "excluded_nodes";
    static final String NODE_COUNT_CHANCES_COLUMN = "node_count_chances";
    static final String DROP_CHANCE_COLUMN = "drop_chance";
    static final String SALVAGE_CHANCE_COLUMN = "salvage_chance";
    static final List<String> CACHE_ONLY_COLUMNS = List.of(EXCLUDED_NODES_COLUMN, NODE_COUNT_CHANCES_COLUMN, DROP_CHANCE_COLUMN,
            SALVAGE_CHANCE_COLUMN);
    static final long NODE_COUNT_SALT = 0x3C61_7A0E_D59B_2F48L;
    private static final String LIST_SEPARATOR = ";";
    private static final Logger LOG = Logger.getLogger(ModspecCaches.class);
    private static final AtomicReference<Map<String, CacheBase>> BASES_BY_ID = new AtomicReference<>(Map.of());

    public record CacheBase(String definitionId, Set<String> excludedNodeIds, List<Float> nodeCountChances, float dropChance,
                            float salvageChance) {

        public CacheBase {
            excludedNodeIds = excludedNodeIds == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(excludedNodeIds));
            nodeCountChances = nodeCountChances == null || nodeCountChances.isEmpty() ? List.of(1f) : List.copyOf(nodeCountChances);
        }

        public int nodeCount(long seed) {
            float total = 0f;
            for (float chance : nodeCountChances) {
                total += chance;
            }
            double roll = new SplittableRandom(seed ^ NODE_COUNT_SALT).nextDouble() * total;
            for (int index = 0; index < nodeCountChances.size(); index++) {
                roll -= nodeCountChances.get(index);
                if (roll < 0d) {
                    return index + 1;
                }
            }
            return nodeCountChances.size();
        }
    }

    private ModspecCaches() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, ItemDefinitions.SUBROUTINES_PATH, LOG, ModspecCaches::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, CacheBase> bases = new LinkedHashMap<>();
        Map<String, FactionFleetDrops.Source> fleetSources = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            ItemDefinition definition = ItemDefinitions.get(ModCsv.text(row, ID_COLUMN));
            if (definition == null || definition.subroutineKind() != SubroutineKind.MODSPEC_CACHE) {
                return;
            }
            try {
                CacheBase base = parse(definition.id(), row);
                bases.put(definition.id(), base);
                if (base.dropChance() > 0f) {
                    fleetSources.put(definition.id(), FactionFleetDrops.Source.anyFaction(base.dropChance()));
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Row " + (index + 1) + " of " + ItemDefinitions.SUBROUTINES_PATH + " allocates nothing: " + e.getMessage());
            }
        });
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (definition.subroutineKind() == SubroutineKind.MODSPEC_CACHE && !bases.containsKey(definition.id())) {
                LOG.error(definition.id() + " is a " + SubroutineKind.MODSPEC_CACHE.id() + " subroutine with no usable row, so it rolls no node");
            }
        }
        BASES_BY_ID.set(Collections.unmodifiableMap(bases));
        FactionFleetDrops.register(DROP_SOURCE_GROUP, fleetSources);
        SubroutineNodePools.use(SubroutineKind.MODSPEC_CACHE, ModspecCaches::candidateNodeIds, ModspecCaches::nodeCount);
    }

    static CacheBase parse(String definitionId, JSONObject row) {
        String nodeCountText = ModCsv.text(row, NODE_COUNT_CHANCES_COLUMN);
        if (!isValidNodeCountChances(nodeCountText)) {
            throw new IllegalArgumentException(definitionId + " has the " + NODE_COUNT_CHANCES_COLUMN + " \"" + nodeCountText
                    + "\"; expected numbers of 0 or more separated by " + LIST_SEPARATOR + ", at least one above 0");
        }
        float dropChance = chance(definitionId, DROP_CHANCE_COLUMN, ModCsv.text(row, DROP_CHANCE_COLUMN));
        float salvageChance = chance(definitionId, SALVAGE_CHANCE_COLUMN, ModCsv.text(row, SALVAGE_CHANCE_COLUMN));
        return new CacheBase(definitionId, nodeIds(ModCsv.text(row, EXCLUDED_NODES_COLUMN)), nodeCountChances(nodeCountText), dropChance,
                salvageChance);
    }

    static boolean isValidNodeCountChances(String chancesText) {
        if (chancesText == null || chancesText.isBlank()) {
            return false;
        }
        boolean anyAboveZero = false;
        for (String chanceText : chancesText.split(LIST_SEPARATOR, -1)) {
            if (!isValidChance(chanceText.trim()) || chanceText.isBlank()) {
                return false;
            }
            anyAboveZero |= Float.parseFloat(chanceText.trim()) > 0f;
        }
        return anyAboveZero;
    }

    static List<Float> nodeCountChances(String chancesText) {
        List<Float> chances = new ArrayList<>();
        for (String chanceText : chancesText.split(LIST_SEPARATOR, -1)) {
            chances.add(Float.parseFloat(chanceText.trim()));
        }
        return chances;
    }

    static float chance(String definitionId, String column, String chanceText) {
        if (chanceText.isEmpty()) {
            return 0f;
        }
        if (!isValidChance(chanceText)) {
            LOG.error(definitionId + " in " + ItemDefinitions.SUBROUTINES_PATH + " has the " + column + " \"" + chanceText
                    + "\"; expected a number of 0 or more, so it never drops that way");
            return 0f;
        }
        return Float.parseFloat(chanceText);
    }

    static boolean isValidChance(String chanceText) {
        return WormholeAnchorDefinitions.isValidDropChance(chanceText);
    }

    static Set<String> nodeIds(String nodeIdsText) {
        Set<String> nodeIds = new LinkedHashSet<>();
        for (String nodeId : nodeIdsText.split(LIST_SEPARATOR)) {
            if (!nodeId.isBlank()) {
                nodeIds.add(nodeId.trim());
            }
        }
        return nodeIds;
    }

    public static CacheBase base(String definitionId) {
        return definitionId == null ? null : BASES_BY_ID.get().get(ItemIdAliases.currentId(definitionId));
    }

    public static Collection<CacheBase> bases() {
        return BASES_BY_ID.get().values();
    }

    static int nodeCount(ItemDefinition definition, long seed) {
        CacheBase base = definition == null ? null : base(definition.id());
        return base == null ? 1 : base.nodeCount(seed);
    }

    public static List<String> candidateNodeIds(ItemDefinition definition) {
        CacheBase base = definition == null ? null : base(definition.id());
        if (base == null) {
            return List.of();
        }
        return candidateNodeIds(base, SkillTree.getAllNodes().values());
    }

    static List<String> candidateNodeIds(CacheBase base, Collection<SkillNode> activeNodes) {
        List<String> candidates = new ArrayList<>();
        for (SkillNode node : activeNodes) {
            if (isGrantable(node) && !base.excludedNodeIds().contains(node.getId())) {
                candidates.add(node.getId());
            }
        }
        return candidates;
    }

    static boolean isGrantable(SkillNode node) {
        return node.getType().getTier() == SkillTier.NOTABLE && !node.getType().isOptional();
    }

    public static List<ItemData> rollSalvage(Random random, float chanceMult) {
        List<ItemData> droppedItems = new ArrayList<>();
        if (!(chanceMult > 0f)) {
            return droppedItems;
        }
        for (CacheBase base : bases()) {
            if (!(base.salvageChance() > 0f) || random.nextFloat() >= base.salvageChance() * chanceMult) {
                continue;
            }
            ItemDefinition definition = ItemDefinitions.get(base.definitionId());
            if (definition != null && FactionFleetDrops.available(definition)) {
                droppedItems.add(ItemData.rolled(definition, random.nextLong()));
            }
        }
        return droppedItems;
    }

    public static void clear() {
        BASES_BY_ID.set(Map.of());
        FactionFleetDrops.clear(DROP_SOURCE_GROUP);
        SubroutineNodePools.use(SubroutineKind.MODSPEC_CACHE, null);
    }
}
