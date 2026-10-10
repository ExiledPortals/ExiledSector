package exiledsector.skills;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class LearnedPhantomConflicts {

    public static final String COMMON_FILE = "exiledSector_learnedHullModConflicts.json";
    private static final String HULL_MOD_PREFIX = "mod:";
    private static final String HULL_PREFIX = "hull:";
    private static final Logger LOG = Logger.getLogger(LearnedPhantomConflicts.class);
    private static final Map<String, Set<String>> CONFLICT_KEYS_BY_PHANTOM = new LinkedHashMap<>();
    private static Storage storage = new CommonFolderStorage();

    public record Conflict(String hullModId) {

        public boolean isWholeHull() {
            return hullModId == null;
        }
    }

    interface Storage {
        String read() throws IOException;

        void write(String contents) throws IOException;
    }

    private LearnedPhantomConflicts() {
    }

    public static synchronized void load() {
        CONFLICT_KEYS_BY_PHANTOM.clear();
        String contents;
        try {
            contents = storage.read();
        } catch (IOException e) {
            LOG.warn("[ExiledSector] Could not read learned hull mod conflicts from " + COMMON_FILE, e);
            return;
        }
        if (contents == null || contents.isBlank()) {
            return;
        }
        try {
            JSONObject root = new JSONObject(contents);
            Iterator<?> phantomIds = root.keys();
            while (phantomIds.hasNext()) {
                String phantomId = String.valueOf(phantomIds.next());
                JSONArray keys = root.getJSONArray(phantomId);
                for (int i = 0; i < keys.length(); i++) {
                    String key = keys.getString(i);
                    if (stillExists(key)) {
                        CONFLICT_KEYS_BY_PHANTOM.computeIfAbsent(phantomId, id -> new LinkedHashSet<>()).add(key);
                    }
                }
            }
        } catch (JSONException e) {
            LOG.warn("[ExiledSector] Ignoring unreadable learned hull mod conflicts in " + COMMON_FILE, e);
            CONFLICT_KEYS_BY_PHANTOM.clear();
        }
    }

    public static synchronized Conflict conflictFor(String phantomHullModId, String baseHullId, Predicate<String> hasHullMod) {
        Set<String> keys = CONFLICT_KEYS_BY_PHANTOM.get(phantomHullModId);
        if (keys == null) {
            return null;
        }
        if (baseHullId != null && keys.contains(HULL_PREFIX + baseHullId)) {
            return new Conflict(null);
        }
        for (String key : keys) {
            if (key.startsWith(HULL_MOD_PREFIX) && hasHullMod.test(key.substring(HULL_MOD_PREFIX.length()))) {
                return new Conflict(key.substring(HULL_MOD_PREFIX.length()));
            }
        }
        return null;
    }

    public static boolean learnHullMod(String phantomHullModId, String conflictingHullModId) {
        return learn(phantomHullModId, HULL_MOD_PREFIX + conflictingHullModId);
    }

    public static boolean learnHull(String phantomHullModId, String baseHullId) {
        return learn(phantomHullModId, HULL_PREFIX + baseHullId);
    }

    private static synchronized boolean learn(String phantomHullModId, String key) {
        if (!CONFLICT_KEYS_BY_PHANTOM.computeIfAbsent(phantomHullModId, id -> new LinkedHashSet<>()).add(key)) {
            return false;
        }
        LOG.info("[ExiledSector] Learned that " + key + " removes the skill tree's " + phantomHullModId + "; nodes that place it are now"
                + " refused there.");
        save();
        return true;
    }

    private static void save() {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, Set<String>> entry : CONFLICT_KEYS_BY_PHANTOM.entrySet()) {
                root.put(entry.getKey(), new JSONArray(entry.getValue()));
            }
            storage.write(root.toString(2));
        } catch (IOException | JSONException e) {
            LOG.warn("[ExiledSector] Could not save learned hull mod conflicts to " + COMMON_FILE, e);
        }
    }

    private static boolean stillExists(String key) {
        if (key.startsWith(HULL_MOD_PREFIX)) {
            return Global.getSettings().getHullModSpec(key.substring(HULL_MOD_PREFIX.length())) != null;
        }
        if (key.startsWith(HULL_PREFIX)) {
            try {
                return Global.getSettings().getHullSpec(key.substring(HULL_PREFIX.length())) != null;
            } catch (RuntimeException unknownHull) {
                return false;
            }
        }
        return false;
    }

    static synchronized void useStorage(Storage replacement) {
        storage = replacement;
        CONFLICT_KEYS_BY_PHANTOM.clear();
    }

    static void useCommonFolder() {
        useStorage(new CommonFolderStorage());
    }

    private static final class CommonFolderStorage implements Storage {

        @Override
        public String read() throws IOException {
            return Global.getSettings().fileExistsInCommon(COMMON_FILE) ? Global.getSettings().readTextFileFromCommon(COMMON_FILE) : null;
        }

        @Override
        public void write(String contents) throws IOException {
            Global.getSettings().writeTextFileToCommon(COMMON_FILE, contents);
        }
    }
}
