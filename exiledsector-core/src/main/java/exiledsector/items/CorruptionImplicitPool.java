package exiledsector.items;

import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
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
        CsvRows.forEachRow(rows, dataPath.get(), log, (index, row) -> {
            Modifier implicit = ItemImplicits.referencedImplicit(ModCsv.text(row, IMPLICIT_COLUMN), itemType);
            if (byId.putIfAbsent(implicit.id(), implicit) != null) {
                throw new IllegalArgumentException(implicit.id() + " is listed more than once");
            }
        });
        implicits.set(List.copyOf(byId.values()));
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
