package exiledsector.skills.tags;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.i18n.Translation;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ThemeTags {

    public static final String PATH = ExiledSector.configPath("theme_tags.csv");
    static final String TAG_COLUMN = "tag";
    private static final String PARENT_COLUMN = "parent";
    private static final String SCOPES_COLUMN = "scopes";
    private static final String NAME_KEY_PREFIX = "tag.";
    private static final Logger LOG = Logger.getLogger(ThemeTags.class);
    private static final AtomicReference<Map<String, ThemeTag>> TAGS_BY_ID = new AtomicReference<>(Map.of());

    private record ThemeTag(String id, String parent, Set<ThemeTagScope> scopes) {

        ThemeTag withoutParent() {
            return new ThemeTag(id, null, scopes);
        }
    }

    private ThemeTags() {
    }

    public static void load() {
        ModCsv.load(TAG_COLUMN, PATH, LOG, ThemeTags::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, ThemeTag> tagsById = new LinkedHashMap<>();
        CsvRows.forEachRow(rows, PATH, LOG, (index, row) -> {
            ThemeTag tag = parse(row);
            if (tag != null) {
                tagsById.put(tag.id(), tag);
            }
        });
        dropBrokenParents(tagsById);
        TAGS_BY_ID.set(Collections.unmodifiableMap(tagsById));
    }

    public static void clear() {
        TAGS_BY_ID.set(Map.of());
    }

    public static List<String> ids() {
        return List.copyOf(TAGS_BY_ID.get().keySet());
    }

    public static List<String> ids(ThemeTagScope scope) {
        return TAGS_BY_ID.get().values().stream().filter(tag -> tag.scopes().contains(scope)).map(ThemeTag::id).toList();
    }

    public static boolean isThemeTag(String tag) {
        return tag != null && TAGS_BY_ID.get().containsKey(tag);
    }

    public static boolean allows(String tag, ThemeTagScope scope) {
        return scopes(tag).contains(scope);
    }

    public static Set<ThemeTagScope> scopes(String tag) {
        ThemeTag themeTag = tag == null ? null : TAGS_BY_ID.get().get(tag);
        return themeTag == null ? Set.of() : themeTag.scopes();
    }

    public static String parent(String tag) {
        ThemeTag themeTag = tag == null ? null : TAGS_BY_ID.get().get(tag);
        return themeTag == null ? null : themeTag.parent();
    }

    public static List<String> withAncestors(String tag) {
        List<String> tagAndAncestors = new ArrayList<>();
        for (String current = tag; current != null; current = parent(current)) {
            tagAndAncestors.add(current);
        }
        return List.copyOf(tagAndAncestors);
    }

    public static Set<String> allWithAncestors(Collection<String> tags) {
        Set<String> expandedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            expandedTags.addAll(withAncestors(tag));
        }
        return Collections.unmodifiableSet(expandedTags);
    }

    public static List<String> ancestorsListedBeside(String tag, Collection<String> listedTags) {
        return withAncestors(tag).stream().skip(1).filter(listedTags::contains).toList();
    }

    public static String displayName(String tag) {
        return Translation.text(NAME_KEY_PREFIX + tag);
    }

    private static ThemeTag parse(JSONObject row) {
        String id = ModCsv.text(row, TAG_COLUMN);
        if (id.isEmpty()) {
            return null;
        }
        String parent = ModCsv.text(row, PARENT_COLUMN);
        return new ThemeTag(id, parent.isEmpty() ? null : parent, scopes(id, ModCsv.text(row, SCOPES_COLUMN)));
    }

    private static Set<ThemeTagScope> scopes(String id, String scopesText) {
        Set<ThemeTagScope> scopes = EnumSet.noneOf(ThemeTagScope.class);
        for (String scopeId : CsvRows.entries(scopesText)) {
            ThemeTagScope scope = ThemeTagScope.byId(scopeId);
            if (scope == null) {
                throw new IllegalArgumentException(id + " has the unknown scope \"" + scopeId + "\"");
            }
            scopes.add(scope);
        }
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException(id + " has no scopes");
        }
        return Collections.unmodifiableSet(scopes);
    }

    private static void dropBrokenParents(Map<String, ThemeTag> tagsById) {
        for (ThemeTag tag : List.copyOf(tagsById.values())) {
            if (tag.parent() == null) {
                continue;
            }
            if (!tagsById.containsKey(tag.parent())) {
                LOG.error(tag.id() + " in " + PATH + " has the unknown parent \"" + tag.parent() + "\", so it has no parent");
                tagsById.put(tag.id(), tag.withoutParent());
            } else if (leadsBackTo(tag.id(), tagsById)) {
                LOG.error(tag.id() + " in " + PATH + " is its own ancestor through \"" + tag.parent() + "\", so it has no parent");
                tagsById.put(tag.id(), tag.withoutParent());
            }
        }
    }

    private static boolean leadsBackTo(String id, Map<String, ThemeTag> tagsById) {
        Set<String> visited = new HashSet<>();
        for (String ancestor = tagsById.get(id).parent(); ancestor != null && visited.add(ancestor); ancestor = parentIn(ancestor, tagsById)) {
            if (ancestor.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String parentIn(String id, Map<String, ThemeTag> tagsById) {
        ThemeTag tag = tagsById.get(id);
        return tag == null ? null : tag.parent();
    }
}
