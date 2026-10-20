package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class SubroutineImplicits {

    static final String DATA_PATH = "data/config/exiledSector/subroutine_corruption_implicits.csv";
    static final String IMPLICIT_COLUMN = "implicit";
    private static final Logger LOG = Logger.getLogger(SubroutineImplicits.class);
    private static final AtomicReference<List<Modifier>> CORRUPTION_POOL = new AtomicReference<>(List.of());

    private SubroutineImplicits() {
    }

    public static void load() {
        ModCsv.load(IMPLICIT_COLUMN, DATA_PATH, LOG, SubroutineImplicits::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, Modifier> byId = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            try {
                Modifier implicit = referencedImplicit(ModCsv.text(row, IMPLICIT_COLUMN));
                if (byId.putIfAbsent(implicit.id(), implicit) != null) {
                    throw new IllegalArgumentException(implicit.id() + " is listed more than once");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        CORRUPTION_POOL.set(List.copyOf(byId.values()));
    }

    private static Modifier referencedImplicit(String entryText) {
        if (!ItemModifiers.isReference(entryText)) {
            throw new IllegalArgumentException("implicit \"" + entryText + "\" must reference a modifier as @ID");
        }
        String referenceId = entryText.substring(ItemModifiers.REFERENCE_MARK.length()).trim();
        if (referenceId.contains(":")) {
            throw new IllegalArgumentException("implicit \"" + entryText + "\" has a weight, but every pool entry is equally likely");
        }
        return ItemModifiers.referenced(referenceId, ModifierSlot.IMPLICIT, ItemType.SUBROUTINE);
    }

    public static List<Modifier> corruptionPool() {
        return CORRUPTION_POOL.get();
    }

    static Modifier byId(String modifierId) {
        for (Modifier implicit : corruptionPool()) {
            if (implicit.id().equals(modifierId)) {
                return implicit;
            }
        }
        return null;
    }

    static ModifierLine line(String effectName) {
        for (Modifier implicit : corruptionPool()) {
            ModifierLine line = implicit.line(effectName);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    public static void clear() {
        CORRUPTION_POOL.set(List.of());
    }
}
