package exiledlib.items;

import exiledlib.ModCsv;
import exiledlib.modifiers.Modifier;
import exiledlib.modifiers.ModifierLine;
import exiledlib.modifiers.ModifierSlot;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class CorruptionImplicitPool {

    public static final String IMPLICIT_COLUMN = "implicit";
    private final Supplier<String> dataPath;
    private final ItemType itemType;
    private final Logger log;
    private final AtomicReference<List<Modifier>> implicits = new AtomicReference<>(List.of());

    public CorruptionImplicitPool(Supplier<String> dataPath, ItemType itemType, Logger log) {
        this.dataPath = dataPath;
        this.itemType = itemType;
        this.log = log;
    }

    public void load() {
        ModCsv.load(IMPLICIT_COLUMN, dataPath.get(), log, this::register);
    }

    public void register(JSONArray rows) throws JSONException {
        Map<String, Modifier> byId = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            try {
                Modifier implicit = referencedImplicit(ModCsv.text(row, IMPLICIT_COLUMN));
                if (byId.putIfAbsent(implicit.id(), implicit) != null) {
                    throw new IllegalArgumentException(implicit.id() + " is listed more than once");
                }
            } catch (IllegalArgumentException e) {
                log.error("Skipping row " + (index + 1) + " of " + dataPath.get() + ": " + e.getMessage());
            }
        });
        implicits.set(List.copyOf(byId.values()));
    }

    private Modifier referencedImplicit(String entryText) {
        if (!ItemModifiers.isReference(entryText)) {
            throw new IllegalArgumentException("implicit \"" + entryText + "\" must reference a modifier as @ID");
        }
        String referenceId = entryText.substring(ItemModifiers.REFERENCE_MARK.length()).trim();
        if (referenceId.contains(":")) {
            throw new IllegalArgumentException("implicit \"" + entryText + "\" has a weight, but every pool entry is equally likely");
        }
        return ItemModifiers.referenced(referenceId, ModifierSlot.IMPLICIT, itemType);
    }

    public List<Modifier> implicits() {
        return implicits.get();
    }

    public Modifier byId(String modifierId) {
        for (Modifier implicit : implicits()) {
            if (implicit.id().equals(modifierId)) {
                return implicit;
            }
        }
        return null;
    }

    public ModifierLine line(String effectName) {
        for (Modifier implicit : implicits()) {
            ModifierLine line = implicit.line(effectName);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    public void clear() {
        implicits.set(List.of());
    }
}
