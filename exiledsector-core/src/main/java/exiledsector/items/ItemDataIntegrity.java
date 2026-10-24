package exiledsector.items;

import exiledlib.ModCsv;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    static final String ENTRY_SEPARATOR = ";";
    static final String WEIGHT_SEPARATOR = ":";
    static final int FIRST_DATA_LINE = 2;
    private static final Logger LOG = Logger.getLogger(ItemDataIntegrity.class);
    private static final DomainContributions<ItemDataRules> RULES = DomainContributions.fromEveryDomain(ExiledSectorDomain::itemDataRules);

    record ModifierRow(String id, ModifierSlot slot, Set<ModifierScope> scopes, List<String> effectNames, boolean named, int line,
                       String file) {
    }

    private final List<Violation> violations = new ArrayList<>();
    private final Map<String, ModifierRow> modifiersById = new LinkedHashMap<>();
    private final Set<String> referencedIds = new HashSet<>();
    private final Map<String, Integer> idsNeedingNames = new LinkedHashMap<>();
    private final Map<String, String> definitionSourcesById = new HashMap<>();

    static List<ItemDataRules> rules() {
        return RULES.all().stream().sorted(Comparator.comparing(ItemDataRules::type)).toList();
    }

    private static ItemDataRules rulesFor(ItemType type) {
        return RULES.all().stream().filter(rules -> rules.type() == type).findFirst().orElse(null);
    }

    public static void logLoaded() {
        try {
            JSONArray modifierRows = ModCsv.rows("id", ItemModifiers.dataPath());
            if (modifierRows == null) {
                return;
            }
            ItemDataIntegrity integrity = new ItemDataIntegrity();
            integrity.checkModifiers(modifierRows, ItemModifiers.dataPath());
            for (ItemDataRules rules : rules()) {
                rules.checkModifierFiles(integrity);
            }
            for (ItemDataRules rules : rules()) {
                rules.checkItemFiles(integrity);
            }
            List<Violation> violations = new ArrayList<>(integrity.finish());
            for (ItemDataRules rules : rules()) {
                violations.addAll(rules.checkSeparately(integrity.definitionSourcesById.keySet()));
            }
            for (Violation violation : violations) {
                if (violation.severity() == Severity.ERROR) {
                    LOG.error(violation);
                }
            }
        } catch (IOException | JSONException e) {
            LOG.error("Could not check the item data", e);
        }
    }

    public List<Violation> finish() {
        checkNames();
        checkReferences();
        return violations();
    }

    public List<Violation> violations() {
        return List.copyOf(violations);
    }

    void clearViolations() {
        violations.clear();
    }

    Map<String, ModifierRow> modifiersById() {
        return modifiersById;
    }

    boolean isReferenced(String modifierId) {
        return referencedIds.contains(modifierId);
    }

    static JSONArray orEmpty(JSONArray rows) {
        return rows == null ? new JSONArray() : rows;
    }

    void error(String file, int line, String message) {
        violations.add(new Violation(Severity.ERROR, file, line, message));
    }

    void warning(String file, int line, String message) {
        violations.add(new Violation(Severity.WARNING, file, line, message));
    }

    public void checkModifiers(JSONArray rows, String file) throws JSONException {
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
            checkScopeSlots(file, line, modifierId, scopes, slot);
            if (scopes.contains(ModifierScope.UNIQUE_SUBROUTINE) && slot != null && slot != ModifierSlot.PREFIX) {
                error(file, line, modifierId + " rolls on " + ModifierScope.UNIQUE_SUBROUTINE.id() + ", so its slot must be "
                        + ModifierSlot.PREFIX.id());
            }
            if (scopes.contains(ModifierScope.DOCTRINE) && slot != null && slot != ModifierSlot.PREFIX) {
                error(file, line, modifierId + " rolls on " + ModifierScope.DOCTRINE.id() + ", so its slot must be " + ModifierSlot.PREFIX.id());
            }
            List<ModifierLine> parsedLines = new ArrayList<>();
            List<String> effectNames = checkLines(file, line, modifierId, slot, effectsFor(file, line, scopes), ModCsv.text(row, "lines"),
                    parsedLines);
            for (ItemDataRules rules : rules()) {
                rules.checkModifierRow(this, file, line, modifierId, scopes, parsedLines);
            }
            checkIdConvention(file, line, modifierId, effectNames);
            modifiersById.put(modifierId, new ModifierRow(modifierId, slot, scopes, effectNames,
                    !ModCsv.text(row, "name").isEmpty(), line, file));
        });
    }

    private void checkScopeSlots(String file, int line, String modifierId, Set<ModifierScope> scopes, ModifierSlot slot) {
        if (scopes.contains(ModifierScope.SUBROUTINE) && slot != null && slot != ModifierSlot.IMPLICIT) {
            error(file, line, modifierId + " rolls on " + ModifierScope.SUBROUTINE.id() + ", but subroutines only roll corrupted implicits");
        }
        if (scopes.contains(ModifierScope.WORMHOLE_ANCHOR)) {
            error(file, line, modifierId + " rolls on " + ModifierScope.WORMHOLE_ANCHOR.id() + ", but wormhole anchors do not roll modifiers");
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
                                    List<ModifierLine> parsedLines) {
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
            parsedLines.add(parsed.line());
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

    void checkEffectName(String file, int line, String modifierId, ModifierEffects effects, String effectName) {
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

    public void checkDefinitions(JSONArray rows, String file, ItemType itemType) throws JSONException {
        ItemDataRules rules = rulesFor(itemType);
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
            if (rules != null) {
                rules.checkDefinitionRow(this, file, line, definitionId, row);
                if (!rules.rollsAffixes()) {
                    return;
                }
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

    private void checkSection(String file, int line, String definitionId, String sectionId) {
        ItemKind kind = ItemKind.byIdOrNull(sectionId);
        if (kind == null || kind.type() != ItemType.HULL_ITEM) {
            error(file, line, definitionId + " has section \"" + sectionId + "\", which is not a ship section");
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
            ModifierRow modifier = referencedModifier(file, line, definitionId, fields[0].trim(), slot, ModifierScope.of(itemType));
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
        ItemDataRules rules = rulesFor(itemType);
        if (rules != null) {
            rules.checkTierGate(this, file, line, definitionId, entryText, tierText);
        } else {
            error(file, line, definitionId + " entry \"" + entryText + "\" has a tier range, but only star charts have tiers");
        }
    }

    void checkWeight(String file, int line, String definitionId, String entryText, String weightText) {
        try {
            if (!(Float.parseFloat(weightText.trim()) > 0f)) {
                error(file, line, definitionId + " entry \"" + entryText + "\" needs a weight above zero");
            }
        } catch (NumberFormatException e) {
            error(file, line, definitionId + " entry \"" + entryText + "\" has a weight that is not a number");
        }
    }

    ModifierRow referencedModifier(String file, int line, String ownerId, String referenceId, ModifierSlot slot, ModifierScope scope) {
        ModifierRow modifier = modifiersById.get(referenceId);
        if (modifier == null) {
            error(file, line, ownerId + " references " + referenceId + ", which is not in " + modifierFileFor(scope));
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

    private static String modifierFileFor(ModifierScope scope) {
        for (ItemDataRules rules : rules()) {
            if (rules.modifierFile() != null && ModifierScope.of(rules.type()) == scope) {
                return rules.modifierFile();
            }
        }
        return ItemModifiers.dataPath();
    }

    void checkRepeats(String file, int line, String ownerId, ModifierRow modifier, List<ModifierRow> pool) {
        for (ModifierRow listed : pool) {
            if (listed.id().equals(modifier.id())) {
                error(file, line, ownerId + " lists " + modifier.id() + " more than once");
            } else if (listed.effectNames().size() == 1 && modifier.effectNames().size() == 1
                    && listed.effectNames().get(0).equals(modifier.effectNames().get(0))) {
                error(file, line, ownerId + " lists " + modifier.effectNames().get(0) + " twice, as " + listed.id() + " and " + modifier.id());
            }
        }
    }

    public void checkKindImplicits(JSONArray rows, String file) throws JSONException {
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

    public void checkCorruptionPool(JSONArray rows, String file, String poolName, ItemType itemType) throws JSONException {
        List<ModifierRow> implicits = new ArrayList<>();
        ModCsv.forEach(orEmpty(rows), (index, row) -> checkImplicitEntry(file, index + FIRST_DATA_LINE, poolName,
                ModCsv.text(row, CorruptionImplicitPool.IMPLICIT_COLUMN), implicits, itemType));
    }

    void checkImplicitEntry(String file, int line, String typeId, String entryText, List<ModifierRow> implicits, ItemType itemType) {
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
        ModifierRow implicit = referencedModifier(file, line, typeId, referenceId, ModifierSlot.IMPLICIT, ModifierScope.of(itemType));
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
