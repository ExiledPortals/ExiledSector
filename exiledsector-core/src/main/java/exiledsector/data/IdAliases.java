package exiledsector.data;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class IdAliases {

    private final String path;
    private final String aliasColumn;
    private final String currentIdColumn;
    private final Logger log;
    private final AtomicReference<Map<String, String>> currentIdsByAlias = new AtomicReference<>(Map.of());

    public IdAliases(String path, String aliasColumn, String currentIdColumn, Logger log) {
        this.path = path;
        this.aliasColumn = aliasColumn;
        this.currentIdColumn = currentIdColumn;
        this.log = log;
    }

    public String path() {
        return path;
    }

    public void load() {
        ModCsv.load(aliasColumn, path, log, this::register);
    }

    public void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String alias = ModCsv.text(row, aliasColumn);
            String currentId = ModCsv.text(row, currentIdColumn);
            if (alias.isEmpty() || currentId.isEmpty()) {
                return;
            }
            if (loaded.putIfAbsent(alias, currentId) != null) {
                log.error("Skipping row " + (index + 1) + " of " + path + ": " + alias + " is listed more than once");
            }
        });
        currentIdsByAlias.set(Map.copyOf(loaded));
    }

    public void clear() {
        currentIdsByAlias.set(Map.of());
    }

    public Map<String, String> currentIdsByAlias() {
        return currentIdsByAlias.get();
    }

    public String currentId(String id) {
        return id == null ? null : currentIdsByAlias.get().getOrDefault(id, id);
    }
}
