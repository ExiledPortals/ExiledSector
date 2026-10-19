package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.tags.SkillTags;
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
import java.util.concurrent.atomic.AtomicReference;

public final class VolumeSubroutines {

    public static final String DROP_SOURCE_GROUP = "volume_subroutine";
    static final String ID_COLUMN = "id";
    static final String VOLUME_COLUMN = "volume";
    static final String EXCLUDED_NODES_COLUMN = "excluded_nodes";
    static final String DROP_CHANCE_COLUMN = "drop_chance";
    static final String SALVAGE_CHANCE_COLUMN = "salvage_chance";
    static final List<String> VOLUME_ONLY_COLUMNS = List.of(VOLUME_COLUMN, EXCLUDED_NODES_COLUMN, DROP_CHANCE_COLUMN, SALVAGE_CHANCE_COLUMN);
    private static final String LIST_SEPARATOR = ";";
    private static final Logger LOG = Logger.getLogger(VolumeSubroutines.class);
    private static final AtomicReference<Map<String, VolumeBase>> BASES_BY_ID = new AtomicReference<>(Map.of());

    public record VolumeBase(String definitionId, String volume, Set<String> excludedNodeIds, float dropChance, float salvageChance) {

        public VolumeBase {
            excludedNodeIds = excludedNodeIds == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(excludedNodeIds));
        }
    }

    private VolumeSubroutines() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, ItemDefinitions.SUBROUTINES_PATH, LOG, VolumeSubroutines::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, VolumeBase> bases = new LinkedHashMap<>();
        Map<String, String> definitionIdsByVolume = new LinkedHashMap<>();
        Map<String, FactionFleetDrops.Source> fleetSources = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            String definitionId = ModCsv.text(row, ID_COLUMN);
            ItemDefinition definition = ItemDefinitions.get(definitionId);
            if (definition == null || definition.subroutineKind() != SubroutineKind.VOLUME) {
                return;
            }
            try {
                VolumeBase base = parse(definition.id(), row);
                String earlierDefinitionId = definitionIdsByVolume.putIfAbsent(base.volume(), definition.id());
                if (earlierDefinitionId != null) {
                    throw new IllegalArgumentException(definition.id() + " grants from " + base.volume() + ", which already has the volume subroutine "
                            + earlierDefinitionId);
                }
                bases.put(definition.id(), base);
                if (base.dropChance() > 0f) {
                    fleetSources.put(definition.id(), new FactionFleetDrops.Source(base.volume(), Set.of(), base.dropChance()));
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Row " + (index + 1) + " of " + ItemDefinitions.SUBROUTINES_PATH + " grants nothing: " + e.getMessage());
            }
        });
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (definition.subroutineKind() == SubroutineKind.VOLUME && !bases.containsKey(definition.id())) {
                LOG.error(definition.id() + " is a " + SubroutineKind.VOLUME.id() + " subroutine with no usable volume, so it rolls and grants no node");
            }
        }
        BASES_BY_ID.set(Collections.unmodifiableMap(bases));
        FactionFleetDrops.register(DROP_SOURCE_GROUP, fleetSources);
        SubroutineNodePools.use(SubroutineKind.VOLUME, VolumeSubroutines::candidateNodeIds);
    }

    static VolumeBase parse(String definitionId, JSONObject row) {
        String volume = ModCsv.text(row, VOLUME_COLUMN);
        if (!SkillTags.isRegion(volume)) {
            throw new IllegalArgumentException(definitionId + " has volume \"" + volume + "\"; expected a tree region");
        }
        float dropChance = chance(definitionId, DROP_CHANCE_COLUMN, ModCsv.text(row, DROP_CHANCE_COLUMN));
        float salvageChance = chance(definitionId, SALVAGE_CHANCE_COLUMN, ModCsv.text(row, SALVAGE_CHANCE_COLUMN));
        return new VolumeBase(definitionId, volume, nodeIds(ModCsv.text(row, EXCLUDED_NODES_COLUMN)), dropChance, salvageChance);
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

    public static VolumeBase base(String definitionId) {
        return definitionId == null ? null : BASES_BY_ID.get().get(ItemIdAliases.currentId(definitionId));
    }

    public static Collection<VolumeBase> bases() {
        return BASES_BY_ID.get().values();
    }

    public static String volume(String definitionId) {
        VolumeBase base = base(definitionId);
        return base == null ? null : base.volume();
    }

    public static List<String> candidateNodeIds(ItemDefinition definition) {
        VolumeBase base = definition == null ? null : base(definition.id());
        if (base == null) {
            return List.of();
        }
        return candidateNodeIds(base, SkillTree.getDeclaredNodes().values());
    }

    static List<String> candidateNodeIds(VolumeBase base, Collection<SkillNode> declaredNodes) {
        List<String> candidates = new ArrayList<>();
        for (SkillNode node : declaredNodes) {
            if (base.volume().equals(node.getRegion()) && isGrantable(node) && !base.excludedNodeIds().contains(node.getId())) {
                candidates.add(node.getId());
            }
        }
        return candidates;
    }

    static boolean isGrantable(SkillNode node) {
        return node.getType().getTier() == SkillTier.NOTABLE && !node.getType().isOptional();
    }

    public static boolean areaActive(String definitionId) {
        String volume = volume(definitionId);
        if (volume == null) {
            return false;
        }
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (volume.equals(node.getRegion())) {
                return true;
            }
        }
        return false;
    }

    public static List<ItemData> rollSalvage(Random random, float chanceMult) {
        List<ItemData> droppedItems = new ArrayList<>();
        if (!(chanceMult > 0f)) {
            return droppedItems;
        }
        for (VolumeBase base : bases()) {
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

    public static String volumeName(String volume) {
        return Translation.text("hyperspace.region." + volume);
    }

    public static void clear() {
        BASES_BY_ID.set(Map.of());
        FactionFleetDrops.clear(DROP_SOURCE_GROUP);
        SubroutineNodePools.use(SubroutineKind.VOLUME, null);
    }
}
