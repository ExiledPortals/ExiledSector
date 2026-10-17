package exiledsector.hullitems;

import exiledsector.ModCsv;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.skilleffect.SkillEffect;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HullItemDataIntegrity {

    public enum Severity {ERROR, WARNING}

    public record Violation(Severity severity, String file, int line, String message) {

        @Override
        public String toString() {
            return severity + " " + file + " line " + line + ": " + message;
        }
    }

    static final String HYBRID_ID_PREFIX = "HYBRID_";
    private static final Logger LOG = Logger.getLogger(HullItemDataIntegrity.class);
    private static final String ENTRY_SEPARATOR = ";";
    private static final String WEIGHT_SEPARATOR = ":";
    private static final int FIRST_DATA_LINE = 2;

    private record ModifierRow(String id, ModifierSlot slot, List<String> effectNames, boolean named, int line) {
    }

    private final List<Violation> violations = new ArrayList<>();
    private final Map<String, ModifierRow> modifiersById = new LinkedHashMap<>();
    private final Set<String> referencedIds = new HashSet<>();
    private final Map<String, Integer> idsNeedingNames = new LinkedHashMap<>();

    private HullItemDataIntegrity() {
    }

    public static List<Violation> check(JSONArray modifierRows, JSONArray definitionRows, JSONArray implicitRows) throws JSONException {
        HullItemDataIntegrity integrity = new HullItemDataIntegrity();
        integrity.checkModifiers(modifierRows);
        integrity.checkDefinitions(definitionRows);
        integrity.checkImplicits(implicitRows);
        integrity.checkNames();
        integrity.checkReferences();
        return List.copyOf(integrity.violations);
    }

    public static void logLoaded() {
        try {
            JSONArray modifierRows = ModCsv.rows("id", HullItemModifiers.DATA_PATH);
            JSONArray definitionRows = ModCsv.rows("id", HullItemDefinitions.DATA_PATH);
            JSONArray implicitRows = ModCsv.rows("type", HullItemImplicits.DATA_PATH);
            if (modifierRows == null || definitionRows == null || implicitRows == null) {
                return;
            }
            for (Violation violation : check(modifierRows, definitionRows, implicitRows)) {
                if (violation.severity() == Severity.ERROR) {
                    LOG.error(violation);
                }
            }
        } catch (IOException | JSONException e) {
            LOG.error("Could not check the hull item data", e);
        }
    }

    private void error(String file, int line, String message) {
        violations.add(new Violation(Severity.ERROR, file, line, message));
    }

    private void warning(String file, int line, String message) {
        violations.add(new Violation(Severity.WARNING, file, line, message));
    }

    private void checkModifiers(JSONArray rows) throws JSONException {
        String file = HullItemModifiers.DATA_PATH;
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String modifierId = ModCsv.text(row, "id");
            if (modifierId.isEmpty()) {
                error(file, line, "the row has no id");
                return;
            }
            if (!HullItemModifiers.ID_PATTERN.matcher(modifierId).matches()) {
                error(file, line, "id \"" + modifierId + "\" must be letters, digits and underscores");
            }
            ModifierRow earlier = modifiersById.get(modifierId);
            if (earlier != null) {
                error(file, line, "id " + modifierId + " is already used on line " + earlier.line());
                return;
            }
            String slotText = ModCsv.text(row, "slot");
            ModifierSlot slot = ModifierSlot.byIdOrNull(slotText);
            if (slot == null) {
                error(file, line, modifierId + " has slot \"" + slotText + "\"; expected implicit, prefix or suffix");
            }
            List<String> effectNames = checkLines(file, line, modifierId, slot, ModCsv.text(row, "lines"));
            checkIdConvention(file, line, modifierId, effectNames);
            modifiersById.put(modifierId, new ModifierRow(modifierId, slot, effectNames,
                    !ModCsv.text(row, "name").isEmpty(), line));
        });
    }

    private List<String> checkLines(String file, int line, String modifierId, ModifierSlot slot, String linesText) {
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
            checkEffectName(file, line, modifierId, effectName);
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

    private void checkEffectName(String file, int line, String modifierId, String effectName) {
        try {
            String currentName = SkillEffect.byName(effectName).name();
            if (!currentName.equals(effectName)) {
                error(file, line, modifierId + " uses the old effect name " + effectName + "; use " + currentName);
            }
        } catch (IllegalArgumentException e) {
            error(file, line, modifierId + " uses " + effectName + ", which is not a skill effect");
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

    private void checkDefinitions(JSONArray rows) throws JSONException {
        String file = HullItemDefinitions.DATA_PATH;
        Map<String, Integer> linesById = new HashMap<>();
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
            }
            if (HullItemType.byIdOrNull(ModCsv.text(row, "kind")) == null) {
                error(file, line, definitionId + " has unknown kind \"" + ModCsv.text(row, "kind") + "\"");
            }
            boolean unique = "true".equalsIgnoreCase(ModCsv.text(row, "unique"));
            List<ModifierRow> pool = new ArrayList<>();
            checkPool(file, line, definitionId, ModCsv.text(row, "prefixes"), ModifierSlot.PREFIX, unique, pool);
            checkPool(file, line, definitionId, ModCsv.text(row, "suffixes"), ModifierSlot.SUFFIX, unique, pool);
            if (pool.isEmpty()) {
                error(file, line, definitionId + " has no prefixes or suffixes");
            }
        });
    }

    private void checkPool(String file, int line, String definitionId, String poolText, ModifierSlot slot, boolean unique,
                           List<ModifierRow> pool) {
        for (String entryText : poolText.split(ENTRY_SEPARATOR)) {
            String trimmed = entryText.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!HullItemModifiers.isReference(trimmed)) {
                error(file, line, definitionId + " lists \"" + trimmed + "\" inline; reference a modifier as @ID or @ID:weight");
                continue;
            }
            String[] fields = trimmed.substring(HullItemModifiers.REFERENCE_MARK.length()).split(WEIGHT_SEPARATOR, -1);
            if (fields.length > 2) {
                error(file, line, definitionId + " entry \"" + trimmed + "\" is not @ID or @ID:weight");
                continue;
            }
            if (fields.length == 2) {
                checkWeight(file, line, definitionId, trimmed, fields[1]);
            }
            ModifierRow modifier = referencedModifier(file, line, definitionId, fields[0].trim(), slot);
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

    private void checkWeight(String file, int line, String definitionId, String entryText, String weightText) {
        try {
            if (!(Float.parseFloat(weightText.trim()) > 0f)) {
                error(file, line, definitionId + " entry \"" + entryText + "\" needs a weight above zero");
            }
        } catch (NumberFormatException e) {
            error(file, line, definitionId + " entry \"" + entryText + "\" has a weight that is not a number");
        }
    }

    private ModifierRow referencedModifier(String file, int line, String ownerId, String referenceId, ModifierSlot slot) {
        ModifierRow modifier = modifiersById.get(referenceId);
        if (modifier == null) {
            error(file, line, ownerId + " references " + referenceId + ", which is not in " + HullItemModifiers.DATA_PATH);
            return null;
        }
        referencedIds.add(referenceId);
        if (modifier.slot() != null && modifier.slot() != slot) {
            error(file, line, ownerId + " lists " + referenceId + " in its " + slot.id() + " slot, but it is a " + modifier.slot().id());
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
        String file = HullItemImplicits.DATA_PATH;
        Map<String, Integer> linesByType = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String typeId = ModCsv.text(row, "type");
            HullItemType type = HullItemType.byIdOrNull(typeId);
            if (type == null || !type.fitsSection()) {
                error(file, line, "\"" + typeId + "\" is not a hull item type that fits a ship section");
            }
            Integer earlierLine = linesByType.putIfAbsent(typeId, line);
            if (earlierLine != null) {
                error(file, line, typeId + " is already listed on line " + earlierLine);
            }
            List<ModifierRow> implicits = new ArrayList<>();
            for (String entryText : ModCsv.text(row, "implicits").split(ENTRY_SEPARATOR)) {
                checkImplicitEntry(file, line, typeId, entryText.trim(), implicits);
            }
        });
    }

    private void checkImplicitEntry(String file, int line, String typeId, String entryText, List<ModifierRow> implicits) {
        if (entryText.isEmpty()) {
            return;
        }
        if (!HullItemModifiers.isReference(entryText)) {
            error(file, line, typeId + " lists \"" + entryText + "\" inline; reference a modifier as @ID");
            return;
        }
        String referenceId = entryText.substring(HullItemModifiers.REFERENCE_MARK.length()).trim();
        if (referenceId.contains(WEIGHT_SEPARATOR)) {
            error(file, line, typeId + " entry \"" + entryText + "\" has a weight, but implicits always roll");
            return;
        }
        ModifierRow implicit = referencedModifier(file, line, typeId, referenceId, ModifierSlot.IMPLICIT);
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
        String file = HullItemModifiers.DATA_PATH;
        idsNeedingNames.forEach((modifierId, line) -> {
            if (!modifiersById.get(modifierId).named()) {
                error(file, line, modifierId + " rolls on items that are not unique, so it needs a name for their common names");
            }
        });
        for (ModifierRow modifier : modifiersById.values()) {
            if (modifier.slot() == ModifierSlot.IMPLICIT && modifier.named()) {
                warning(file, modifier.line(), modifier.id() + " is an implicit, and implicits never show a name");
            }
        }
    }

    private void checkReferences() {
        for (ModifierRow modifier : modifiersById.values()) {
            if (!referencedIds.contains(modifier.id())) {
                warning(HullItemModifiers.DATA_PATH, modifier.line(), modifier.id() + " is not used by any hull item or implicit");
            }
        }
    }
}
