package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;
import exiledsector.modifiers.ValueScaling;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.radius.RadiusSize;
import exiledsector.skills.tags.SkillTags;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ItemDataIntegrity {

    public enum Severity {ERROR, WARNING}

    public record Violation(Severity severity, String file, int line, String message) {

        @Override
        public String toString() {
            return severity + " " + file + " line " + line + ": " + message;
        }
    }

    static final String HYBRID_ID_PREFIX = "HYBRID_";
    private static final Logger LOG = Logger.getLogger(ItemDataIntegrity.class);
    private static final String ENTRY_SEPARATOR = ";";
    private static final String WEIGHT_SEPARATOR = ":";
    private static final int FIRST_DATA_LINE = 2;

    private record ModifierRow(String id, ModifierSlot slot, Set<ModifierScope> scopes, List<String> effectNames, boolean named, int line,
                               String file) {
    }

    private final List<Violation> violations = new ArrayList<>();
    private final Map<String, ModifierRow> modifiersById = new LinkedHashMap<>();
    private final Set<String> referencedIds = new HashSet<>();
    private final Map<String, Integer> idsNeedingNames = new LinkedHashMap<>();
    private final Map<String, String> definitionSourcesById = new HashMap<>();

    private ItemDataIntegrity() {
    }

    public static List<Violation> check(JSONArray modifierRows, JSONArray subroutineRows, JSONArray hullItemRows, JSONArray implicitRows)
            throws JSONException {
        return checkWithStarCharts(modifierRows, subroutineRows, hullItemRows, implicitRows, new JSONArray(), new JSONArray(), new JSONArray());
    }

    public static List<Violation> checkWithStarCharts(JSONArray modifierRows, JSONArray subroutineRows, JSONArray hullItemRows,
                                                      JSONArray implicitRows, JSONArray chartModifierRows, JSONArray chartRows,
                                                      JSONArray chartImplicitRows) throws JSONException {
        return checkAll(modifierRows, subroutineRows, hullItemRows, implicitRows, chartModifierRows, chartRows, chartImplicitRows, new JSONArray(),
                new JSONArray());
    }

    public static List<Violation> checkAll(JSONArray modifierRows, JSONArray subroutineRows, JSONArray hullItemRows, JSONArray implicitRows,
                                           JSONArray chartModifierRows, JSONArray chartRows, JSONArray chartImplicitRows,
                                           JSONArray subroutineImplicitRows, JSONArray hullItemCorruptionRows) throws JSONException {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkModifiers(modifierRows, ItemModifiers.DATA_PATH, false);
        integrity.checkModifiers(chartModifierRows, StarChartItems.MODIFIERS_PATH, true);
        integrity.checkDefinitions(orEmpty(subroutineRows), ItemDefinitions.SUBROUTINES_PATH, ItemType.SUBROUTINE);
        integrity.checkDefinitions(hullItemRows, ItemDefinitions.HULL_ITEMS_PATH, ItemType.HULL_ITEM);
        integrity.checkDefinitions(chartRows, StarChartDefinitions.DATA_PATH, ItemType.STAR_CHART);
        integrity.checkImplicits(implicitRows);
        integrity.checkStarChartImplicits(chartImplicitRows);
        integrity.checkCorruptionPool(orEmpty(subroutineImplicitRows), SubroutineImplicits.DATA_PATH, "the subroutine corruption pool", ItemType.SUBROUTINE);
        integrity.checkCorruptionPool(orEmpty(hullItemCorruptionRows), ItemImplicits.CORRUPTION_DATA_PATH, "the hull item corruption pool",
                ItemType.HULL_ITEM);
        integrity.checkNames();
        integrity.checkReferences();
        return List.copyOf(integrity.violations);
    }

    public static void logLoaded() {
        try {
            JSONArray modifierRows = ModCsv.rows("id", ItemModifiers.DATA_PATH);
            JSONArray subroutineRows = ModCsv.rows("id", ItemDefinitions.SUBROUTINES_PATH);
            JSONArray hullItemRows = ModCsv.rows("id", ItemDefinitions.HULL_ITEMS_PATH);
            JSONArray implicitRows = ModCsv.rows("section", ItemImplicits.DATA_PATH);
            if (modifierRows == null || hullItemRows == null || implicitRows == null) {
                return;
            }
            JSONArray chartModifierRows = orEmpty(ModCsv.rows("id", StarChartItems.MODIFIERS_PATH));
            JSONArray chartRows = orEmpty(ModCsv.rows("id", StarChartDefinitions.DATA_PATH));
            JSONArray chartImplicitRows = orEmpty(ModCsv.rows(StarChartImplicits.SYSTEM_TYPE_COLUMN, StarChartImplicits.DATA_PATH));
            JSONArray subroutineImplicitRows = orEmpty(ModCsv.rows(SubroutineImplicits.IMPLICIT_COLUMN, SubroutineImplicits.DATA_PATH));
            JSONArray hullItemCorruptionRows = orEmpty(ModCsv.rows(CorruptionImplicitPool.IMPLICIT_COLUMN, ItemImplicits.CORRUPTION_DATA_PATH));
            List<Violation> violations = new ArrayList<>(checkAll(modifierRows, subroutineRows, hullItemRows, implicitRows,
                    chartModifierRows, chartRows, chartImplicitRows, subroutineImplicitRows, hullItemCorruptionRows));
            JSONArray anchorRows = orEmpty(ModCsv.rows("id", WormholeAnchorDefinitions.DATA_PATH));
            violations.addAll(checkWormholeAnchors(anchorRows, SkillTree.getDeclaredNodes(), definitionIds(orEmpty(subroutineRows), hullItemRows, chartRows)));
            JSONArray doctrineRows = orEmpty(ModCsv.rows(DoctrineArchives.ID_COLUMN, DoctrineArchives.DATA_PATH));
            violations.addAll(checkDoctrineArchives(modifierRows, orEmpty(subroutineRows), doctrineRows, SkillTree.getAllTypes()));
            violations.addAll(checkModspecCaches(orEmpty(subroutineRows), SkillTree.getDeclaredNodes()));
            for (Violation violation : violations) {
                if (violation.severity() == Severity.ERROR) {
                    LOG.error(violation);
                }
            }
        } catch (IOException | JSONException e) {
            LOG.error("Could not check the item data", e);
        }
    }

    private static Set<String> definitionIds(JSONArray... definitionRowSets) throws JSONException {
        Set<String> definitionIds = new HashSet<>();
        for (JSONArray rows : definitionRowSets) {
            ModCsv.forEach(rows, (index, row) -> definitionIds.add(ModCsv.text(row, "id")));
        }
        return definitionIds;
    }

    public static List<Violation> checkWormholeAnchors(JSONArray anchorRows, Map<String, SkillNode> treeNodes, Collection<String> otherDefinitionIds)
            throws JSONException {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkAnchorRows(orEmpty(anchorRows), treeNodes, otherDefinitionIds);
        return List.copyOf(integrity.violations);
    }

    public static List<Violation> checkDoctrineArchives(JSONArray modifierRows, JSONArray subroutineRows, JSONArray doctrineRows,
                                                        Map<String, SkillType> types) throws JSONException {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkModifiers(orEmpty(modifierRows), ItemModifiers.DATA_PATH, false);
        integrity.violations.clear();
        integrity.checkDoctrineRows(orEmpty(subroutineRows), orEmpty(doctrineRows), types);
        return List.copyOf(integrity.violations);
    }

    public static List<Violation> checkModspecCaches(JSONArray subroutineRows, Map<String, SkillNode> treeNodes) throws JSONException {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkCacheRows(orEmpty(subroutineRows), treeNodes);
        return List.copyOf(integrity.violations);
    }

    private void checkCacheRows(JSONArray subroutineRows, Map<String, SkillNode> treeNodes) throws JSONException {
        String file = ItemDefinitions.SUBROUTINES_PATH;
        ModCsv.forEach(subroutineRows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String definitionId = ModCsv.text(row, "id");
            if (SubroutineKind.byIdOrNull(ModCsv.text(row, ItemDefinition.SUBROUTINE_KIND_COLUMN)) != SubroutineKind.MODSPEC_CACHE) {
                for (String column : ModspecCaches.CACHE_ONLY_COLUMNS) {
                    if (!ModCsv.text(row, column).isEmpty()) {
                        error(file, line, definitionId + " fills " + column + ", but only " + SubroutineKind.MODSPEC_CACHE.id() + " subroutines use it");
                    }
                }
                return;
            }
            String nodeCountChances = ModCsv.text(row, ModspecCaches.NODE_COUNT_CHANCES_COLUMN);
            if (!ModspecCaches.isValidNodeCountChances(nodeCountChances)) {
                error(file, line, definitionId + " has the node count chances \"" + nodeCountChances
                        + "\"; expected numbers of 0 or more separated by ;, at least one above 0");
            }
            checkCacheChances(file, line, definitionId, row);
            checkCachePool(file, line, definitionId, ModspecCaches.nodeIds(ModCsv.text(row, ModspecCaches.EXCLUDED_NODES_COLUMN)), treeNodes);
        });
    }

    private void checkCacheChances(String file, int line, String definitionId, JSONObject row) {
        String dropChance = ModCsv.text(row, ModspecCaches.DROP_CHANCE_COLUMN);
        String salvageChance = ModCsv.text(row, ModspecCaches.SALVAGE_CHANCE_COLUMN);
        if (!ModspecCaches.isValidChance(dropChance)) {
            error(file, line, definitionId + " has the drop chance \"" + dropChance + "\"; expected a number of 0 or more");
        }
        if (!ModspecCaches.isValidChance(salvageChance)) {
            error(file, line, definitionId + " has the salvage chance \"" + salvageChance + "\"; expected a number of 0 or more");
        }
        if (isZeroChance(dropChance) && isZeroChance(salvageChance)) {
            warning(file, line, definitionId + " has no drop chance and no salvage chance, so it never drops");
        }
    }

    private static boolean isZeroChance(String chanceText) {
        try {
            return chanceText.isEmpty() || !(Float.parseFloat(chanceText) > 0f);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private void checkCachePool(String file, int line, String definitionId, Set<String> excludedNodeIds, Map<String, SkillNode> treeNodes) {
        if (treeNodes.isEmpty()) {
            return;
        }
        for (String excludedNodeId : excludedNodeIds) {
            SkillNode excludedNode = treeNodes.get(excludedNodeId);
            if (excludedNode == null) {
                error(file, line, definitionId + " excludes " + excludedNodeId + ", which is not in the skill tree");
            } else if (!ModspecCaches.isGrantable(excludedNode)) {
                error(file, line, definitionId + " excludes " + excludedNodeId + ", which is not a notable");
            }
        }
        ModspecCaches.CacheBase base = new ModspecCaches.CacheBase(definitionId, excludedNodeIds, List.of(), 0f, 0f);
        if (ModspecCaches.candidateNodeIds(base, treeNodes.values()).isEmpty()) {
            error(file, line, definitionId + " has no notable left to roll");
        }
    }

    private void checkDoctrineRows(JSONArray subroutineRows, JSONArray doctrineRows, Map<String, SkillType> types) throws JSONException {
        String file = DoctrineArchives.DATA_PATH;
        Set<String> doctrineIds = new LinkedHashSet<>();
        ModCsv.forEach(subroutineRows, (index, row) -> {
            if (SubroutineKind.byIdOrNull(ModCsv.text(row, ItemDefinition.SUBROUTINE_KIND_COLUMN)) == SubroutineKind.DOCTRINE) {
                doctrineIds.add(ModCsv.text(row, "id"));
            }
        });
        Map<String, Integer> linesById = new HashMap<>();
        ModCsv.forEach(doctrineRows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String definitionId = ModCsv.text(row, DoctrineArchives.ID_COLUMN);
            if (definitionId.isEmpty()) {
                error(file, line, "the row has no id");
                return;
            }
            Integer earlierLine = linesById.putIfAbsent(definitionId, line);
            if (earlierLine != null) {
                error(file, line, "id " + definitionId + " is already used on line " + earlierLine);
            }
            if (!doctrineIds.contains(definitionId)) {
                error(file, line, definitionId + " is not a " + SubroutineKind.DOCTRINE.id() + " subroutine in " + ItemDefinitions.SUBROUTINES_PATH);
            }
            checkDoctrineRow(file, line, definitionId, row, types);
        });
        for (String doctrineId : doctrineIds) {
            if (!linesById.containsKey(doctrineId)) {
                error(ItemDefinitions.SUBROUTINES_PATH, 0, doctrineId + " is a " + SubroutineKind.DOCTRINE.id() + " subroutine with no row in " + file);
            }
        }
        for (ModifierRow modifier : modifiersById.values()) {
            if (modifier.scopes().contains(ModifierScope.DOCTRINE) && !referencedIds.contains(modifier.id())) {
                warning(modifier.file(), modifier.line(), modifier.id() + " is not used by any doctrine archive");
            }
        }
    }

    private void checkDoctrineRow(String file, int line, String definitionId, JSONObject row, Map<String, SkillType> types) {
        String behaviourText = ModCsv.text(row, DoctrineArchives.BEHAVIOUR_COLUMN);
        DoctrineBehaviour behaviour = DoctrineBehaviour.byIdOrNull(behaviourText);
        if (behaviour == null) {
            error(file, line, definitionId + " has behaviour \"" + behaviourText + "\"; expected one of " + DoctrineBehaviour.ids());
        }
        String radiusText = ModCsv.text(row, DoctrineArchives.RADIUS_COLUMN);
        if (RadiusSize.byDataKeyOrNull(radiusText) == null) {
            error(file, line, definitionId + " has radius \"" + radiusText + "\"; expected small, medium or large");
        }
        Integer minLines = DoctrineArchives.lineCount(ModCsv.text(row, DoctrineArchives.MIN_LINES_COLUMN));
        Integer maxLines = DoctrineArchives.lineCount(ModCsv.text(row, DoctrineArchives.MAX_LINES_COLUMN));
        if (minLines == null || maxLines == null || minLines < 1 || maxLines < minLines) {
            error(file, line, definitionId + " needs min_lines of 1 or more and max_lines of at least min_lines");
        }
        int listedTypes = checkReplacementPool(file, line, definitionId, ModCsv.text(row, DoctrineArchives.NOTABLES_COLUMN), SkillTier.NOTABLE, types)
                + checkReplacementPool(file, line, definitionId, ModCsv.text(row, DoctrineArchives.KEYSTONES_COLUMN), SkillTier.KEYSTONE, types)
                + checkReplacementPool(file, line, definitionId, ModCsv.text(row, DoctrineArchives.SMALL_NODES_COLUMN), SkillTier.SMALL, types);
        String modifiersText = ModCsv.text(row, DoctrineArchives.MODIFIERS_COLUMN);
        if (behaviour == DoctrineBehaviour.TRANSFORM) {
            if (listedTypes == 0) {
                error(file, line, definitionId + " transforms nodes but lists no replacement notables, keystones or small nodes");
            }
            if (!modifiersText.isEmpty()) {
                error(file, line, definitionId + " transforms nodes, so it lists no modifiers");
            }
        } else if (behaviour == DoctrineBehaviour.AUGMENT) {
            if (listedTypes > 0) {
                error(file, line, definitionId + " augments nodes, so it lists no replacement nodes");
            }
            if (checkDoctrineModifiers(file, line, definitionId, modifiersText) == 0) {
                error(file, line, definitionId + " augments nodes but lists no modifiers");
            }
        }
    }

    private int checkReplacementPool(String file, int line, String definitionId, String poolText, SkillTier tier, Map<String, SkillType> types) {
        List<String> typeIds = DoctrineArchives.typeIds(poolText);
        for (String typeId : typeIds) {
            SkillType type = types.get(typeId);
            if (type == null) {
                error(file, line, definitionId + " lists " + typeId + ", which is not a skill type");
                continue;
            }
            if (type.getTier() != tier) {
                error(file, line, definitionId + " lists " + typeId + " as a " + tier + " replacement, but it is a " + type.getTier() + " node");
            }
            if (type.isOptional() || type.getItemCost() != null) {
                error(file, line, definitionId + " lists " + typeId + ", but optional nodes and nodes with an item cost can't be replacements");
            }
        }
        return typeIds.size();
    }

    private int checkDoctrineModifiers(String file, int line, String definitionId, String modifiersText) {
        List<ModifierRow> pool = new ArrayList<>();
        for (String entryText : modifiersText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!ItemModifiers.isReference(trimmed)) {
                error(file, line, definitionId + " lists \"" + trimmed + "\" inline; reference a modifier as @ID or @ID:weight");
                continue;
            }
            String[] fields = trimmed.substring(ItemModifiers.REFERENCE_MARK.length()).split(WEIGHT_SEPARATOR, -1);
            if (fields.length > 2) {
                error(file, line, definitionId + " entry \"" + trimmed + "\" is not @ID or @ID:weight");
                continue;
            }
            if (fields.length == 2) {
                checkWeight(file, line, definitionId, trimmed, fields[1]);
            }
            ModifierRow modifier = referencedModifier(file, line, definitionId, fields[0].trim(), ModifierSlot.PREFIX, ModifierScope.DOCTRINE);
            if (modifier != null) {
                checkRepeats(file, line, definitionId, modifier, pool);
                pool.add(modifier);
            }
        }
        return pool.size();
    }

    private void checkAnchorRows(JSONArray rows, Map<String, SkillNode> treeNodes, Collection<String> otherDefinitionIds) throws JSONException {
        String file = WormholeAnchorDefinitions.DATA_PATH;
        Map<String, Integer> linesById = new HashMap<>();
        Map<String, Integer> linesByVolume = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String definitionId = ModCsv.text(row, "id");
            if (definitionId.isEmpty()) {
                error(file, line, "the row has no id");
                return;
            }
            Integer earlierLine = linesById.putIfAbsent(definitionId, line);
            if (earlierLine != null) {
                error(file, line, "id " + definitionId + " is already used on line " + earlierLine);
            } else if (otherDefinitionIds.contains(definitionId)) {
                error(file, line, "id " + definitionId + " is already used by a subroutine, hull item or star chart");
            }
            String volume = ModCsv.text(row, WormholeAnchorDefinitions.VOLUME_COLUMN);
            if (!WormholeAnchorDefinitions.isVolume(volume)) {
                error(file, line, definitionId + " has volume \"" + volume + "\"; expected a tree region other than " + SkillTags.CORE_REGION);
            } else {
                Integer volumeLine = linesByVolume.putIfAbsent(volume, line);
                if (volumeLine != null) {
                    error(file, line, definitionId + " leads to " + volume + ", which already has an anchor on line " + volumeLine);
                }
            }
            checkAnchorExit(file, line, definitionId, volume, ModCsv.text(row, WormholeAnchorDefinitions.EXIT_NODE_COLUMN), treeNodes);
            String unlock = ModCsv.text(row, "unlock");
            if (!WormholeAnchorDefinitions.isKnownUnlock(unlock)) {
                error(file, line, definitionId + " has the unknown unlock \"" + unlock + "\"; use an item unlock id, "
                        + ItemUnlock.CLEARED_CHART_TIER_PREFIX + "N or leave it blank");
            }
            String dropChance = ModCsv.text(row, WormholeAnchorDefinitions.DROP_CHANCE_COLUMN);
            if (!WormholeAnchorDefinitions.isValidDropChance(dropChance)) {
                error(file, line, definitionId + " has the drop chance \"" + dropChance + "\"; expected a number of 0 or more");
            }
            String color = ModCsv.text(row, WormholeAnchorDefinitions.COLOR_COLUMN);
            if (!WormholeAnchorDefinitions.isValidColor(color)) {
                error(file, line, definitionId + " has the color \"" + color + "\"; expected #RRGGBB or blank");
            }
        });
    }

    private void checkAnchorExit(String file, int line, String definitionId, String volume, String exitNodeId, Map<String, SkillNode> treeNodes) {
        if (exitNodeId.isEmpty()) {
            error(file, line, definitionId + " has no exit node");
            return;
        }
        SkillNode exitNode = treeNodes.get(exitNodeId);
        if (exitNode == null) {
            error(file, line, definitionId + " exits at " + exitNodeId + ", which is not in the skill tree");
            return;
        }
        if (!volume.equals(exitNode.getRegion())) {
            error(file, line, definitionId + " exits at " + exitNodeId + ", which is in " + exitNode.getRegion() + " rather than " + volume);
        }
        SkillTier exitTier = exitNode.getType().getTier();
        if (exitTier != SkillTier.WORMHOLE_EXIT) {
            error(file, line, definitionId + " exits at " + exitNodeId + ", which is a " + exitTier + " node rather than a wormhole exit");
        }
    }

    private static JSONArray orEmpty(JSONArray rows) {
        return rows == null ? new JSONArray() : rows;
    }

    private void error(String file, int line, String message) {
        violations.add(new Violation(Severity.ERROR, file, line, message));
    }

    private void warning(String file, int line, String message) {
        violations.add(new Violation(Severity.WARNING, file, line, message));
    }

    private void checkModifiers(JSONArray rows, String file, boolean starChartFile) throws JSONException {
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String modifierId = ModCsv.text(row, "id");
            if (modifierId.isEmpty()) {
                error(file, line, "the row has no id");
                return;
            }
            if (!ItemModifiers.ID_PATTERN.matcher(modifierId).matches()) {
                error(file, line, "id \"" + modifierId + "\" must be letters, digits and underscores");
            }
            ModifierRow earlier = modifiersById.get(modifierId);
            if (earlier != null) {
                error(file, line, "id " + modifierId + " is already used on line " + earlier.line()
                        + (earlier.file().equals(file) ? "" : " of " + earlier.file()));
                return;
            }
            String slotText = ModCsv.text(row, "slot");
            ModifierSlot slot = ModifierSlot.byIdOrNull(slotText);
            if (slot == null) {
                error(file, line, modifierId + " has slot \"" + slotText + "\"; expected implicit, prefix or suffix");
            }
            Set<ModifierScope> scopes = checkScopes(file, line, modifierId, ModCsv.text(row, "item_types"));
            checkScopesBelongInFile(file, line, modifierId, scopes, slot, starChartFile);
            if (scopes.contains(ModifierScope.UNIQUE_SUBROUTINE) && slot != null && slot != ModifierSlot.PREFIX) {
                error(file, line, modifierId + " rolls on " + ModifierScope.UNIQUE_SUBROUTINE.id() + ", so its slot must be "
                        + ModifierSlot.PREFIX.id());
            }
            if (scopes.contains(ModifierScope.DOCTRINE) && slot != null && slot != ModifierSlot.PREFIX) {
                error(file, line, modifierId + " rolls on " + ModifierScope.DOCTRINE.id() + ", so its slot must be " + ModifierSlot.PREFIX.id());
            }
            List<String> effectNames = checkLines(file, line, modifierId, slot, effectsFor(file, line, scopes), ModCsv.text(row, "lines"),
                    starChartFile);
            checkIdConvention(file, line, modifierId, effectNames);
            modifiersById.put(modifierId, new ModifierRow(modifierId, slot, scopes, effectNames,
                    !ModCsv.text(row, "name").isEmpty(), line, file));
        });
    }

    private void checkScopesBelongInFile(String file, int line, String modifierId, Set<ModifierScope> scopes, ModifierSlot slot,
                                         boolean starChartFile) {
        if (scopes.isEmpty()) {
            return;
        }
        if (scopes.contains(ModifierScope.SUBROUTINE) && slot != null && slot != ModifierSlot.IMPLICIT) {
            error(file, line, modifierId + " rolls on " + ModifierScope.SUBROUTINE.id() + ", but subroutines only roll corrupted implicits");
        }
        if (scopes.contains(ModifierScope.WORMHOLE_ANCHOR)) {
            error(file, line, modifierId + " rolls on " + ModifierScope.WORMHOLE_ANCHOR.id() + ", but wormhole anchors do not roll modifiers");
        }
        if (starChartFile && !scopes.equals(Set.of(ModifierScope.STAR_CHART))) {
            error(file, line, modifierId + " is in " + file + ", so its item_types must be " + ModifierScope.STAR_CHART.id() + " only");
        } else if (!starChartFile && scopes.contains(ModifierScope.STAR_CHART)) {
            error(file, line, modifierId + " rolls on " + ModifierScope.STAR_CHART.id() + "; star chart modifiers belong in "
                    + StarChartItems.MODIFIERS_PATH);
        }
    }

    private Set<ModifierScope> checkScopes(String file, int line, String modifierId, String itemTypesText) {
        try {
            return ItemModifiers.parseScopes(modifierId, itemTypesText);
        } catch (IllegalArgumentException e) {
            error(file, line, e.getMessage());
            return Set.of();
        }
    }

    private ModifierEffects effectsFor(String file, int line, Set<ModifierScope> scopes) {
        try {
            return ItemEffects.forTypes(ItemModifiers.itemTypesOf(scopes));
        } catch (IllegalArgumentException e) {
            error(file, line, e.getMessage());
            return ItemEffects.SKILL_EFFECTS;
        }
    }

    private List<String> checkLines(String file, int line, String modifierId, ModifierSlot slot, ModifierEffects effects, String linesText,
                                    boolean starChartFile) {
        List<String> effectNames = new ArrayList<>();
        for (String lineText : linesText.split(ENTRY_SEPARATOR)) {
            String trimmed = lineText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ModifierLine.Parsed parsed;
            try {
                parsed = ModifierLine.parse(trimmed);
            } catch (IllegalArgumentException e) {
                error(file, line, modifierId + ": " + e.getMessage());
                continue;
            }
            String effectName = parsed.line().effectName();
            if (parsed.trailingField() != null) {
                error(file, line, modifierId + " line \"" + trimmed + "\" has a weight; weights belong on the pool entry");
            }
            if (slot == ModifierSlot.IMPLICIT && parsed.line().scalesWithHullSize()) {
                error(file, line, modifierId + " is an implicit, so " + effectName + " must roll a min:max range");
            }
            checkTierBands(file, line, modifierId, parsed.line(), starChartFile);
            checkEffectName(file, line, modifierId, effects, effectName);
            if (effectNames.contains(effectName)) {
                error(file, line, modifierId + " lists " + effectName + " more than once");
            }
            effectNames.add(effectName);
        }
        if (effectNames.isEmpty()) {
            error(file, line, modifierId + " has no lines");
        }
        return effectNames;
    }

    private void checkTierBands(String file, int line, String modifierId, ModifierLine modifierLine, boolean starChartFile) {
        if (!modifierLine.scalesWithTier()) {
            return;
        }
        if (!starChartFile) {
            error(file, line, modifierId + " gives " + modifierLine.effectName() + " tier bands, but only star chart modifiers in "
                    + StarChartItems.MODIFIERS_PATH + " have tiers");
            return;
        }
        if (modifierLine.scaling() instanceof ValueScaling.PerTierBand tierBands
                && tierBands.bandStartTiers().get(tierBands.bandStartTiers().size() - 1) > StarChart.HIGHEST_TIER) {
            error(file, line, modifierId + " starts a tier band above tier " + StarChart.HIGHEST_TIER);
        }
    }

    private void checkEffectName(String file, int line, String modifierId, ModifierEffects effects, String effectName) {
        String currentName = effects.currentName(effectName);
        if (currentName == null) {
            error(file, line, modifierId + " uses " + effectName + ", which is not a skill effect");
        } else if (!currentName.equals(effectName)) {
            error(file, line, modifierId + " uses the old effect name " + effectName + "; use " + currentName);
        }
    }

    private void checkIdConvention(String file, int line, String modifierId, List<String> effectNames) {
        if (effectNames.size() == 1) {
            String effectName = effectNames.get(0);
            if (!modifierId.equals(effectName) && !modifierId.startsWith(effectName + "_")) {
                error(file, line, modifierId + " has one effect, so its id must be " + effectName + " or " + effectName + "_<QUALIFIER>");
            }
        } else if (effectNames.size() > 1 && !modifierId.startsWith(HYBRID_ID_PREFIX)) {
            error(file, line, modifierId + " has several effects, so its id must start with " + HYBRID_ID_PREFIX);
        }
    }

    private void checkDefinitions(JSONArray rows, String file, ItemType itemType) throws JSONException {
        Map<String, Integer> linesById = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String definitionId = ModCsv.text(row, "id");
            if (definitionId.isEmpty()) {
                error(file, line, "the row has no id");
                return;
            }
            Integer earlierLine = linesById.putIfAbsent(definitionId, line);
            String otherSource = definitionSourcesById.putIfAbsent(definitionId, file);
            if (earlierLine != null) {
                error(file, line, "id " + definitionId + " is already used on line " + earlierLine);
            } else if (otherSource != null) {
                error(file, line, "id " + definitionId + " is already used in " + otherSource);
            }
            if (itemType == ItemType.HULL_ITEM) {
                checkSection(file, line, definitionId, ModCsv.text(row, "section"));
            }
            if (itemType == ItemType.STAR_CHART) {
                checkSystemType(file, line, definitionId, ModCsv.text(row, StarChartDefinitions.SYSTEM_TYPE_COLUMN));
                checkMinTier(file, line, definitionId, ModCsv.text(row, StarChartDefinitions.MIN_TIER_COLUMN));
            }
            if (itemType == ItemType.SUBROUTINE) {
                checkSubroutine(file, line, definitionId, row);
                return;
            }
            boolean unique = "true".equalsIgnoreCase(ModCsv.text(row, "unique"));
            List<ModifierRow> pool = new ArrayList<>();
            checkPool(file, line, definitionId, ModCsv.text(row, "prefixes"), ModifierSlot.PREFIX, itemType, unique, pool);
            checkPool(file, line, definitionId, ModCsv.text(row, "suffixes"), ModifierSlot.SUFFIX, itemType, unique, pool);
            if (pool.isEmpty()) {
                error(file, line, definitionId + " has no prefixes or suffixes");
            }
        });
    }

    private void checkSubroutine(String file, int line, String definitionId, JSONObject row) {
        String kindText = ModCsv.text(row, ItemDefinition.SUBROUTINE_KIND_COLUMN);
        SubroutineKind subroutineKind = SubroutineKind.byIdOrNull(kindText);
        if (subroutineKind == null) {
            error(file, line, definitionId + " has kind \"" + kindText + "\"; expected one of " + SubroutineKind.ids());
        }
        if (!ModCsv.text(row, "prefixes").isEmpty() || !ModCsv.text(row, "suffixes").isEmpty()) {
            error(file, line, definitionId + " lists prefixes or suffixes, but subroutines do not roll them");
        }
        String linesText = ModCsv.text(row, ItemDefinition.UNIQUE_LINES_COLUMN);
        String sourcesText = ModCsv.text(row, ItemDefinition.UNIQUE_SOURCE_COLUMN);
        checkSources(file, line, definitionId, sourcesText);
        String socketLimitText = ModCsv.text(row, ItemDefinition.SOCKET_LIMIT_COLUMN);
        if (ItemDefinition.parsedSocketLimit(socketLimitText) == null) {
            error(file, line, definitionId + " has socket limit \"" + socketLimitText + "\"; expected a whole number of 0 or more, or blank for no limit");
        }
        if (subroutineKind != SubroutineKind.UNIQUE) {
            if (!linesText.isEmpty()) {
                error(file, line, definitionId + " lists lines, but only " + SubroutineKind.UNIQUE.id() + " subroutines have them");
            }
            if (!sourcesText.isEmpty()) {
                warning(file, line, definitionId + " names a source, but sources only apply to " + SubroutineKind.UNIQUE.id() + " subroutines");
            }
            return;
        }
        if (!"true".equalsIgnoreCase(ModCsv.text(row, "unique"))) {
            error(file, line, definitionId + " is a " + SubroutineKind.UNIQUE.id() + " subroutine, so unique must be true");
        }
        if (checkUniqueLines(file, line, definitionId, linesText) == 0) {
            error(file, line, definitionId + " is a " + SubroutineKind.UNIQUE.id() + " subroutine but lists no lines");
        }
        if (sourcesText.isEmpty()) {
            error(file, line, definitionId + " is a " + SubroutineKind.UNIQUE.id() + " subroutine but names no source; add faction:FACTION_ID or "
                    + "site:SALVAGE_SITE_ID");
        }
    }

    private void checkSources(String file, int line, String definitionId, String sourcesText) {
        try {
            UniqueSource.parseList(sourcesText);
        } catch (IllegalArgumentException e) {
            error(file, line, definitionId + ": " + e.getMessage());
        }
    }

    private int checkUniqueLines(String file, int line, String definitionId, String linesText) {
        List<ModifierRow> listed = new ArrayList<>();
        for (String entryText : linesText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ModifierRow lineRow = ItemModifiers.isReference(trimmed) ? referencedUniqueLine(file, line, definitionId, trimmed)
                    : inlineUniqueLine(file, line, definitionId, trimmed);
            if (lineRow != null) {
                checkRepeats(file, line, definitionId, lineRow, listed);
                listed.add(lineRow);
            }
        }
        return listed.size();
    }

    private ModifierRow referencedUniqueLine(String file, int line, String definitionId, String entryText) {
        String referenceText = entryText.substring(ItemModifiers.REFERENCE_MARK.length()).trim();
        if (referenceText.contains(WEIGHT_SEPARATOR)) {
            error(file, line, definitionId + " line \"" + entryText + "\" has a weight or tier range, but every unique subroutine line always rolls");
            return null;
        }
        return referencedModifier(file, line, definitionId, referenceText, ModifierSlot.PREFIX, ModifierScope.UNIQUE_SUBROUTINE);
    }

    private ModifierRow inlineUniqueLine(String file, int line, String definitionId, String entryText) {
        ModifierLine.Parsed parsed;
        try {
            parsed = ModifierLine.parse(entryText);
        } catch (IllegalArgumentException e) {
            error(file, line, definitionId + ": " + e.getMessage());
            return null;
        }
        if (parsed.trailingField() != null) {
            error(file, line, definitionId + " line \"" + entryText + "\" has a weight, but every unique subroutine line always rolls");
        }
        if (parsed.line().scalesWithTier()) {
            error(file, line, definitionId + " gives " + parsed.line().effectName() + " tier bands, but only star charts have tiers");
        }
        String effectName = parsed.line().effectName();
        checkEffectName(file, line, definitionId, ItemEffects.forType(ItemType.SUBROUTINE), effectName);
        return new ModifierRow(effectName, ModifierSlot.PREFIX, Set.of(ModifierScope.UNIQUE_SUBROUTINE), List.of(effectName), false, line, file);
    }

    private void checkSection(String file, int line, String definitionId, String sectionId) {
        ItemKind kind = ItemKind.byIdOrNull(sectionId);
        if (kind == null || kind.type() != ItemType.HULL_ITEM) {
            error(file, line, definitionId + " has section \"" + sectionId + "\", which is not a ship section");
        }
    }

    private void checkSystemType(String file, int line, String ownerId, String systemTypeText) {
        if (StarChartDefinitions.normalisedSystemType(systemTypeText) == null) {
            error(file, line, ownerId + " has system type \"" + systemTypeText + "\"; expected one of " + StarChartDefinitions.SYSTEM_TYPES
                    + " or " + StarChartDefinitions.ANY_SYSTEM_TYPE);
        }
    }

    private void checkMinTier(String file, int line, String definitionId, String minTierText) {
        if (StarChartDefinitions.parsedMinTier(minTierText) == null) {
            error(file, line, definitionId + " has minimum tier \"" + minTierText + "\"; expected a whole number from " + StarChart.LOWEST_TIER
                    + " to " + StarChart.HIGHEST_TIER + " or blank");
        }
    }

    private void checkPool(String file, int line, String definitionId, String poolText, ModifierSlot slot, ItemType itemType, boolean unique,
                           List<ModifierRow> pool) {
        for (String entryText : poolText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!ItemModifiers.isReference(trimmed)) {
                error(file, line, definitionId + " lists \"" + trimmed + "\" inline; reference a modifier as @ID or @ID:weight");
                continue;
            }
            String[] fields = trimmed.substring(ItemModifiers.REFERENCE_MARK.length()).split(WEIGHT_SEPARATOR, -1);
            boolean tierGated = fields.length >= 2 && PoolEntry.isTierRangeText(fields[fields.length - 1]);
            int weightFields = fields.length - 1 - (tierGated ? 1 : 0);
            if (weightFields > 1 || fields.length > 3) {
                error(file, line, definitionId + " entry \"" + trimmed + "\" is not @ID, @ID:weight, @ID:tMIN-MAX or @ID:weight:tMIN-MAX");
                continue;
            }
            if (weightFields == 1) {
                checkWeight(file, line, definitionId, trimmed, fields[1]);
            }
            if (tierGated) {
                checkTierGate(file, line, definitionId, trimmed, fields[fields.length - 1], itemType);
            }
            ModifierRow modifier = referencedModifier(file, line, definitionId, fields[0].trim(), slot, itemType);
            if (modifier == null) {
                continue;
            }
            checkRepeats(file, line, definitionId, modifier, pool);
            pool.add(modifier);
            if (!unique) {
                idsNeedingNames.putIfAbsent(modifier.id(), modifier.line());
            }
        }
    }

    private void checkTierGate(String file, int line, String definitionId, String entryText, String tierText, ItemType itemType) {
        if (itemType != ItemType.STAR_CHART) {
            error(file, line, definitionId + " entry \"" + entryText + "\" has a tier range, but only star charts have tiers");
            return;
        }
        try {
            TierRange tiers = PoolEntry.parseTierRange(tierText, entryText);
            if (tiers.minTier() > StarChart.HIGHEST_TIER || tiers.maxTier() != Integer.MAX_VALUE && tiers.maxTier() > StarChart.HIGHEST_TIER) {
                error(file, line, definitionId + " entry \"" + entryText + "\" reaches past tier " + StarChart.HIGHEST_TIER);
            }
        } catch (IllegalArgumentException e) {
            error(file, line, definitionId + ": " + e.getMessage());
        }
    }

    private void checkWeight(String file, int line, String definitionId, String entryText, String weightText) {
        try {
            if (!(Float.parseFloat(weightText.trim()) > 0f)) {
                error(file, line, definitionId + " entry \"" + entryText + "\" needs a weight above zero");
            }
        } catch (NumberFormatException e) {
            error(file, line, definitionId + " entry \"" + entryText + "\" has a weight that is not a number");
        }
    }

    private ModifierRow referencedModifier(String file, int line, String ownerId, String referenceId, ModifierSlot slot, ItemType itemType) {
        return referencedModifier(file, line, ownerId, referenceId, slot, ModifierScope.of(itemType));
    }

    private ModifierRow referencedModifier(String file, int line, String ownerId, String referenceId, ModifierSlot slot, ModifierScope scope) {
        ModifierRow modifier = modifiersById.get(referenceId);
        if (modifier == null) {
            String modifierFile = scope == ModifierScope.STAR_CHART ? StarChartItems.MODIFIERS_PATH : ItemModifiers.DATA_PATH;
            error(file, line, ownerId + " references " + referenceId + ", which is not in " + modifierFile);
            return null;
        }
        referencedIds.add(referenceId);
        if (modifier.slot() != null && modifier.slot() != slot) {
            error(file, line, ownerId + " lists " + referenceId + " in its " + slot.id() + " slot, but it is a " + modifier.slot().id());
        }
        if (!modifier.scopes().isEmpty() && !modifier.scopes().contains(scope)) {
            error(file, line, ownerId + " is a " + scope.id() + ", but " + referenceId + " only rolls on "
                    + modifier.scopes().stream().map(ModifierScope::id).collect(Collectors.joining(", ")));
        }
        return modifier;
    }

    private void checkRepeats(String file, int line, String ownerId, ModifierRow modifier, List<ModifierRow> pool) {
        for (ModifierRow listed : pool) {
            if (listed.id().equals(modifier.id())) {
                error(file, line, ownerId + " lists " + modifier.id() + " more than once");
            } else if (listed.effectNames().size() == 1 && modifier.effectNames().size() == 1
                    && listed.effectNames().get(0).equals(modifier.effectNames().get(0))) {
                error(file, line, ownerId + " lists " + modifier.effectNames().get(0) + " twice, as " + listed.id() + " and " + modifier.id());
            }
        }
    }

    private void checkImplicits(JSONArray rows) throws JSONException {
        String file = ItemImplicits.DATA_PATH;
        Map<String, Integer> linesByType = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String typeId = ModCsv.text(row, "section");
            ItemKind type = ItemKind.byIdOrNull(typeId);
            if (type == null || !type.fitsSection()) {
                error(file, line, "\"" + typeId + "\" is not a ship section");
            }
            Integer earlierLine = linesByType.putIfAbsent(typeId, line);
            if (earlierLine != null) {
                error(file, line, typeId + " is already listed on line " + earlierLine);
            }
            List<ModifierRow> implicits = new ArrayList<>();
            for (String entryText : ModCsv.text(row, "implicits").split(ENTRY_SEPARATOR)) {
                checkImplicitEntry(file, line, typeId, entryText.trim(), implicits, ItemType.HULL_ITEM);
            }
        });
    }

    private void checkStarChartImplicits(JSONArray rows) throws JSONException {
        if (rows == null) {
            return;
        }
        String file = StarChartImplicits.DATA_PATH;
        Map<String, Integer> linesByType = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String systemTypeText = ModCsv.text(row, StarChartImplicits.SYSTEM_TYPE_COLUMN);
            checkSystemType(file, line, "the row", systemTypeText);
            String systemType = StarChartDefinitions.normalisedSystemType(systemTypeText);
            Integer earlierLine = systemType == null ? null : linesByType.putIfAbsent(systemType, line);
            if (earlierLine != null) {
                error(file, line, systemType + " is already listed on line " + earlierLine);
            }
            List<ModifierRow> implicits = new ArrayList<>();
            for (String entryText : ModCsv.text(row, "implicits").split(ENTRY_SEPARATOR)) {
                checkImplicitEntry(file, line, systemTypeText, entryText.trim(), implicits, ItemType.STAR_CHART);
            }
        });
    }

    private void checkCorruptionPool(JSONArray rows, String file, String poolName, ItemType itemType) throws JSONException {
        List<ModifierRow> implicits = new ArrayList<>();
        ModCsv.forEach(rows, (index, row) -> checkImplicitEntry(file, index + FIRST_DATA_LINE, poolName,
                ModCsv.text(row, CorruptionImplicitPool.IMPLICIT_COLUMN), implicits, itemType));
    }

    private void checkImplicitEntry(String file, int line, String typeId, String entryText, List<ModifierRow> implicits, ItemType itemType) {
        if (entryText.isEmpty()) {
            return;
        }
        if (!ItemModifiers.isReference(entryText)) {
            error(file, line, typeId + " lists \"" + entryText + "\" inline; reference a modifier as @ID");
            return;
        }
        String referenceId = entryText.substring(ItemModifiers.REFERENCE_MARK.length()).trim();
        if (referenceId.contains(WEIGHT_SEPARATOR)) {
            error(file, line, typeId + " entry \"" + entryText + "\" has a weight, but implicits always roll");
            return;
        }
        ModifierRow implicit = referencedModifier(file, line, typeId, referenceId, ModifierSlot.IMPLICIT, itemType);
        if (implicit == null) {
            return;
        }
        for (ModifierRow listed : implicits) {
            if (listed.id().equals(implicit.id())) {
                error(file, line, typeId + " lists " + implicit.id() + " more than once");
            } else if (listed.effectNames().stream().anyMatch(implicit.effectNames()::contains)) {
                error(file, line, typeId + " implicits " + listed.id() + " and " + implicit.id() + " share an effect");
            }
        }
        implicits.add(implicit);
    }

    private void checkNames() {
        idsNeedingNames.forEach((modifierId, line) -> {
            ModifierRow modifier = modifiersById.get(modifierId);
            if (!modifier.named()) {
                error(modifier.file(), line, modifierId + " rolls on items that are not unique, so it needs a name for their common names");
            }
        });
        for (ModifierRow modifier : modifiersById.values()) {
            if (modifier.slot() == ModifierSlot.IMPLICIT && modifier.named()) {
                warning(modifier.file(), modifier.line(), modifier.id() + " is an implicit, and implicits never show a name");
            }
        }
    }

    private void checkReferences() {
        for (ModifierRow modifier : modifiersById.values()) {
            if (!referencedIds.contains(modifier.id()) && !modifier.scopes().contains(ModifierScope.DOCTRINE)) {
                warning(modifier.file(), modifier.line(), modifier.id() + " is not used by any item or implicit");
            }
        }
    }
}
