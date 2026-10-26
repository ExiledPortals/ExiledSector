package exiledsector.items;

import exiledlib.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.radius.RadiusSize;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static exiledsector.items.ItemDataIntegrity.ENTRY_SEPARATOR;
import static exiledsector.items.ItemDataIntegrity.FIRST_DATA_LINE;

public final class SubroutineDataRules implements ItemDataRules {

    public static final SubroutineDataRules INSTANCE = new SubroutineDataRules();

    private SubroutineDataRules() {
    }

    @Override
    public ItemType type() {
        return ItemType.SUBROUTINE;
    }

    @Override
    public boolean rollsAffixes() {
        return false;
    }

    @Override
    public void checkItemFiles(ItemDataIntegrity integrity) throws IOException, JSONException {
        integrity.checkDefinitions(ItemDataIntegrity.orEmpty(ModCsv.rows("id", SubroutineDefinitions.dataPath())), SubroutineDefinitions.dataPath(),
                ItemType.SUBROUTINE);
        integrity.checkCorruptionPool(ModCsv.rows(SubroutineImplicits.IMPLICIT_COLUMN, SubroutineImplicits.DATA_PATH), SubroutineImplicits.DATA_PATH,
                "the subroutine corruption pool", ItemType.SUBROUTINE);
    }

    @Override
    public List<ItemDataIntegrity.Violation> checkSeparately(Collection<String> definitionIds) throws IOException, JSONException {
        JSONArray modifierRows = ItemDataIntegrity.orEmpty(ModCsv.rows("id", ItemModifiers.dataPath()));
        JSONArray subroutineRows = ItemDataIntegrity.orEmpty(ModCsv.rows("id", SubroutineDefinitions.dataPath()));
        JSONArray doctrineRows = ItemDataIntegrity.orEmpty(ModCsv.rows(DoctrineArchives.ID_COLUMN, DoctrineArchives.DATA_PATH));
        List<ItemDataIntegrity.Violation> violations = new ArrayList<>(checkDoctrineArchives(modifierRows, subroutineRows, doctrineRows,
                SkillTree.getAllTypes()));
        violations.addAll(checkModspecCaches(subroutineRows, SkillTree.getDeclaredNodes()));
        return violations;
    }

    public static List<ItemDataIntegrity.Violation> checkDoctrineArchives(JSONArray modifierRows, JSONArray subroutineRows, JSONArray doctrineRows,
                                                                          Map<String, SkillType> types) throws JSONException {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        integrity.checkModifiers(ItemDataIntegrity.orEmpty(modifierRows), ItemModifiers.dataPath());
        integrity.clearViolations();
        checkDoctrineRows(integrity, ItemDataIntegrity.orEmpty(subroutineRows), ItemDataIntegrity.orEmpty(doctrineRows), types);
        return integrity.violations();
    }

    public static List<ItemDataIntegrity.Violation> checkModspecCaches(JSONArray subroutineRows, Map<String, SkillNode> treeNodes)
            throws JSONException {
        ItemDataIntegrity integrity = new ItemDataIntegrity();
        checkCacheRows(integrity, ItemDataIntegrity.orEmpty(subroutineRows), treeNodes);
        return integrity.violations();
    }

    @Override
    public void checkDefinitionRow(ItemDataIntegrity integrity, String file, int line, String definitionId, JSONObject row) {
        String kindText = ModCsv.text(row, SubroutineDefinitions.SUBROUTINE_KIND_COLUMN);
        SubroutineKind subroutineKind = SubroutineKind.byIdOrNull(kindText);
        if (subroutineKind == null) {
            integrity.error(file, line, definitionId + " has kind \"" + kindText + "\"; expected one of " + SubroutineKind.ids());
        }
        if (!ModCsv.text(row, "prefixes").isEmpty() || !ModCsv.text(row, "suffixes").isEmpty()) {
            integrity.error(file, line, definitionId + " lists prefixes or suffixes, but subroutines do not roll them");
        }
        String linesText = ModCsv.text(row, SubroutineDefinitions.UNIQUE_LINES_COLUMN);
        String sourcesText = ModCsv.text(row, SubroutineDefinitions.UNIQUE_SOURCE_COLUMN);
        integrity.checkUniqueSources(file, line, definitionId, sourcesText);
        String socketLimitText = ModCsv.text(row, SubroutineDefinitions.SOCKET_LIMIT_COLUMN);
        if (SubroutineDefinitions.parsedSocketLimit(socketLimitText) == null) {
            integrity.error(file, line, definitionId + " has socket limit \"" + socketLimitText
                    + "\"; expected a whole number of 0 or more, or blank for no limit");
        }
        String radiusText = ModCsv.text(row, UniqueSubroutineRadii.RADIUS_COLUMN);
        if (subroutineKind != SubroutineKind.UNIQUE) {
            if (!radiusText.isEmpty()) {
                integrity.error(file, line, definitionId + " has a radius, but only " + SubroutineKind.UNIQUE.id() + " subroutines read it");
            }
            if (!linesText.isEmpty()) {
                integrity.error(file, line, definitionId + " lists lines, but only " + SubroutineKind.UNIQUE.id() + " subroutines have them");
            }
            if (!sourcesText.isEmpty()) {
                integrity.warning(file, line, definitionId + " names a source, but sources only apply to " + SubroutineKind.UNIQUE.id()
                        + " subroutines");
            }
            return;
        }
        if (!"true".equalsIgnoreCase(ModCsv.text(row, "unique"))) {
            integrity.error(file, line, definitionId + " is a " + SubroutineKind.UNIQUE.id() + " subroutine, so unique must be true");
        }
        if (!radiusText.isEmpty() && RadiusSize.byDataKeyOrNull(radiusText) == null) {
            integrity.error(file, line, UniqueSubroutineRadii.radiusProblem(definitionId, radiusText));
        }
        if (checkUniqueLines(integrity, file, line, definitionId, linesText) == 0) {
            integrity.error(file, line, definitionId + " is a " + SubroutineKind.UNIQUE.id() + " subroutine but lists no lines");
        }
        if (sourcesText.isEmpty()) {
            integrity.error(file, line, definitionId + " is a " + SubroutineKind.UNIQUE.id() + " subroutine but names no source; add faction:FACTION_ID or "
                    + "site:SALVAGE_SITE_ID");
        }
    }

    private static int checkUniqueLines(ItemDataIntegrity integrity, String file, int line, String definitionId, String linesText) {
        List<ItemDataIntegrity.ModifierRow> listed = new ArrayList<>();
        for (String entryText : linesText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ItemDataIntegrity.ModifierRow lineRow = ItemModifiers.isReference(trimmed)
                    ? referencedUniqueLine(integrity, file, line, definitionId, trimmed)
                    : inlineUniqueLine(integrity, file, line, definitionId, trimmed);
            if (lineRow != null) {
                integrity.checkRepeats(file, line, definitionId, lineRow, listed);
                listed.add(lineRow);
            }
        }
        return listed.size();
    }

    private static ItemDataIntegrity.ModifierRow referencedUniqueLine(ItemDataIntegrity integrity, String file, int line, String definitionId,
                                                                      String entryText) {
        ModifierReference reference = ModifierReference.parse(entryText);
        if (!reference.hasShape(false, false)) {
            integrity.error(file, line, definitionId + " line \"" + entryText
                    + "\" has a weight or tier range, but every unique subroutine line always rolls");
            return null;
        }
        return integrity.referencedModifier(file, line, definitionId, reference.id(), ModifierSlot.PREFIX, ModifierScope.UNIQUE_SUBROUTINE);
    }

    private static ItemDataIntegrity.ModifierRow inlineUniqueLine(ItemDataIntegrity integrity, String file, int line, String definitionId,
                                                                  String entryText) {
        ModifierLine.Parsed parsed;
        try {
            parsed = ModifierLine.parse(entryText);
        } catch (IllegalArgumentException e) {
            integrity.error(file, line, definitionId + ": " + e.getMessage());
            return null;
        }
        if (parsed.trailingField() != null) {
            integrity.error(file, line, definitionId + " line \"" + entryText + "\" has a weight, but every unique subroutine line always rolls");
        }
        if (parsed.line().scalesWithTier()) {
            integrity.error(file, line, definitionId + " gives " + parsed.line().effectName() + " tier bands, but only star charts have tiers");
        }
        String effectName = parsed.line().effectName();
        integrity.checkEffectName(file, line, definitionId, ItemEffects.forType(ItemType.SUBROUTINE), effectName);
        return new ItemDataIntegrity.ModifierRow(effectName, ModifierSlot.PREFIX, Set.of(ModifierScope.UNIQUE_SUBROUTINE), List.of(effectName),
                false, line, file);
    }

    private static void checkCacheRows(ItemDataIntegrity integrity, JSONArray subroutineRows, Map<String, SkillNode> treeNodes)
            throws JSONException {
        String file = SubroutineDefinitions.dataPath();
        ModCsv.forEach(subroutineRows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String definitionId = ModCsv.text(row, "id");
            if (SubroutineKind.byIdOrNull(ModCsv.text(row, SubroutineDefinitions.SUBROUTINE_KIND_COLUMN)) != SubroutineKind.MODSPEC_CACHE) {
                for (String column : ModspecCaches.CACHE_ONLY_COLUMNS) {
                    if (!ModCsv.text(row, column).isEmpty()) {
                        integrity.error(file, line, definitionId + " fills " + column + ", but only " + SubroutineKind.MODSPEC_CACHE.id()
                                + " subroutines use it");
                    }
                }
                return;
            }
            String nodeCountProblem = ModspecCaches.nodeCountProblem(definitionId, ModCsv.text(row, ModspecCaches.NODE_COUNT_CHANCES_COLUMN));
            if (nodeCountProblem != null) {
                integrity.error(file, line, nodeCountProblem);
            }
            checkCacheChances(integrity, file, line, definitionId, row);
            checkCachePool(integrity, file, line, definitionId, ModspecCaches.nodeIds(ModCsv.text(row, ModspecCaches.EXCLUDED_NODES_COLUMN)),
                    treeNodes);
        });
    }

    private static void checkCacheChances(ItemDataIntegrity integrity, String file, int line, String definitionId, JSONObject row) {
        String dropChance = ModCsv.text(row, ModspecCaches.DROP_CHANCE_COLUMN);
        String salvageChance = ModCsv.text(row, ModspecCaches.SALVAGE_CHANCE_COLUMN);
        if (!CsvRows.isValidChance(dropChance)) {
            integrity.error(file, line, definitionId + " has the drop chance \"" + dropChance + "\"; expected a number of 0 or more");
        }
        if (!CsvRows.isValidChance(salvageChance)) {
            integrity.error(file, line, definitionId + " has the salvage chance \"" + salvageChance + "\"; expected a number of 0 or more");
        }
        if (isZeroChance(dropChance) && isZeroChance(salvageChance)) {
            integrity.warning(file, line, definitionId + " has no drop chance and no salvage chance, so it never drops");
        }
    }

    private static boolean isZeroChance(String chanceText) {
        try {
            return chanceText.isEmpty() || !(Float.parseFloat(chanceText) > 0f);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static void checkCachePool(ItemDataIntegrity integrity, String file, int line, String definitionId, Set<String> excludedNodeIds,
                                       Map<String, SkillNode> treeNodes) {
        if (treeNodes.isEmpty()) {
            return;
        }
        for (String excludedNodeId : excludedNodeIds) {
            SkillNode excludedNode = treeNodes.get(excludedNodeId);
            if (excludedNode == null) {
                integrity.error(file, line, definitionId + " excludes " + excludedNodeId + ", which is not in the skill tree");
            } else if (!ModspecCaches.isGrantable(excludedNode)) {
                integrity.error(file, line, definitionId + " excludes " + excludedNodeId + ", which is not a notable");
            }
        }
        ModspecCaches.CacheBase base = new ModspecCaches.CacheBase(definitionId, excludedNodeIds, List.of(), 0f, 0f);
        if (ModspecCaches.candidateNodeIds(base, treeNodes.values()).isEmpty()) {
            integrity.error(file, line, definitionId + " has no notable left to roll");
        }
    }

    private static void checkDoctrineRows(ItemDataIntegrity integrity, JSONArray subroutineRows, JSONArray doctrineRows, Map<String, SkillType> types)
            throws JSONException {
        String file = DoctrineArchives.DATA_PATH;
        Set<String> doctrineIds = new LinkedHashSet<>();
        ModCsv.forEach(subroutineRows, (index, row) -> {
            if (SubroutineKind.byIdOrNull(ModCsv.text(row, SubroutineDefinitions.SUBROUTINE_KIND_COLUMN)) == SubroutineKind.DOCTRINE) {
                doctrineIds.add(ModCsv.text(row, "id"));
            }
        });
        Map<String, Integer> linesById = new HashMap<>();
        ModCsv.forEach(doctrineRows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String definitionId = ModCsv.text(row, DoctrineArchives.ID_COLUMN);
            if (definitionId.isEmpty()) {
                integrity.error(file, line, "the row has no id");
                return;
            }
            Integer earlierLine = linesById.putIfAbsent(definitionId, line);
            if (earlierLine != null) {
                integrity.error(file, line, "id " + definitionId + " is already used on line " + earlierLine);
            }
            if (!doctrineIds.contains(definitionId)) {
                integrity.error(file, line, definitionId + " is not a " + SubroutineKind.DOCTRINE.id() + " subroutine in "
                        + SubroutineDefinitions.dataPath());
            }
            checkDoctrineRow(integrity, file, line, definitionId, row, types);
        });
        for (String doctrineId : doctrineIds) {
            if (!linesById.containsKey(doctrineId)) {
                integrity.error(SubroutineDefinitions.dataPath(), 0, doctrineId + " is a " + SubroutineKind.DOCTRINE.id()
                        + " subroutine with no row in " + file);
            }
        }
        for (ItemDataIntegrity.ModifierRow modifier : integrity.modifiersById().values()) {
            if (modifier.scopes().contains(ModifierScope.DOCTRINE) && !integrity.isReferenced(modifier.id())) {
                integrity.warning(modifier.file(), modifier.line(), modifier.id() + " is not used by any doctrine archive");
            }
        }
    }

    private static void checkDoctrineRow(ItemDataIntegrity integrity, String file, int line, String definitionId, JSONObject row,
                                         Map<String, SkillType> types) {
        DoctrineArchives.problems(definitionId, row).forEach(problem -> integrity.error(file, line, problem));
        checkReplacementPool(integrity, file, line, definitionId, ModCsv.text(row, DoctrineArchives.NOTABLES_COLUMN), SkillTier.NOTABLE, types);
        checkReplacementPool(integrity, file, line, definitionId, ModCsv.text(row, DoctrineArchives.KEYSTONES_COLUMN), SkillTier.KEYSTONE, types);
        checkReplacementPool(integrity, file, line, definitionId, ModCsv.text(row, DoctrineArchives.SMALL_NODES_COLUMN), SkillTier.SMALL, types);
        if (DoctrineBehaviour.byIdOrNull(ModCsv.text(row, DoctrineArchives.BEHAVIOUR_COLUMN)) == DoctrineBehaviour.AUGMENT
                && checkDoctrineModifiers(integrity, file, line, definitionId, ModCsv.text(row, DoctrineArchives.MODIFIERS_COLUMN)) == 0) {
            integrity.error(file, line, definitionId + " augments nodes but lists no modifiers");
        }
    }

    private static void checkReplacementPool(ItemDataIntegrity integrity, String file, int line, String definitionId, String poolText, SkillTier tier,
                                            Map<String, SkillType> types) {
        List<String> typeIds = DoctrineArchives.typeIds(poolText);
        for (String typeId : typeIds) {
            SkillType type = types.get(typeId);
            if (type == null) {
                integrity.error(file, line, definitionId + " lists " + typeId + ", which is not a skill type");
                continue;
            }
            if (type.getTier() != tier) {
                integrity.error(file, line, definitionId + " lists " + typeId + " as a " + tier + " replacement, but it is a " + type.getTier()
                        + " node");
            }
            if (type.isOptional() || type.getItemCost() != null) {
                integrity.error(file, line, definitionId + " lists " + typeId
                        + ", but optional nodes and nodes with an item cost can't be replacements");
            }
        }
    }

    private static int checkDoctrineModifiers(ItemDataIntegrity integrity, String file, int line, String definitionId, String modifiersText) {
        List<ItemDataIntegrity.ModifierRow> pool = new ArrayList<>();
        for (String entryText : modifiersText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!ItemModifiers.isReference(trimmed)) {
                integrity.error(file, line, definitionId + " lists \"" + trimmed + "\" inline; reference a modifier as @ID or @ID:weight");
                continue;
            }
            ModifierReference reference = ModifierReference.parse(trimmed);
            if (!reference.hasShape(true, false)) {
                integrity.error(file, line, definitionId + " entry \"" + trimmed + "\" is not @ID or @ID:weight");
                continue;
            }
            if (reference.weightText() != null) {
                integrity.checkWeight(file, line, definitionId, trimmed, reference.weightText());
            }
            ItemDataIntegrity.ModifierRow modifier = integrity.referencedModifier(file, line, definitionId, reference.id(), ModifierSlot.PREFIX,
                    ModifierScope.DOCTRINE);
            if (modifier != null) {
                integrity.checkRepeats(file, line, definitionId, modifier, pool);
                pool.add(modifier);
            }
        }
        return pool.size();
    }
}
