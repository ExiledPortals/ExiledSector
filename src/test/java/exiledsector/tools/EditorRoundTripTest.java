package exiledsector.tools;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class EditorRoundTripTest {

    private static final Path EDITOR_SOURCES = Path.of("tools/editor/src");
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final String TREE_SERIALIZATION = "pages/tree/model/serialization.ts";
    private static final String ITEM_DEFINITIONS_MODEL = "pages/items/model/definitions.ts";
    private static final String ITEM_NAMES_MODEL = "pages/items/model/names.ts";
    private static final String ITEM_DROPS_MODEL = "pages/items/model/drops.ts";
    private static final String ITEM_CRAFTING_MODEL = "pages/items/model/crafting.ts";
    private static final String HULL_ITEM_IMPLICITS_MODEL = "pages/items/model/implicits.ts";
    private static final String ITEM_MODIFIERS_MODEL = "pages/items/model/modifiers.ts";
    private static final String CHART_TABLES_MODEL = "pages/items/model/chartTables.ts";
    private static final String WORMHOLE_ANCHORS_MODEL = "pages/items/model/anchors.ts";

    private static final String TYPE_LOADER = "exiledsector/skills/loader/SkillTypeLoader.java";
    private static final String TREE_LOADER = "exiledsector/skills/loader/SkillTreeLoader.java";
    private static final String ITEM_DEFINITIONS = "exiledsector/items/ItemDefinitions.java";
    private static final String ITEM_DEFINITION = "exiledsector/items/ItemDefinition.java";
    private static final String ITEM_NAMES = "exiledsector/items/ItemNames.java";
    private static final String ITEM_DROPS = "exiledsector/items/ItemDrops.java";
    private static final String SALVAGE_COMPAT = "exiledsector/compat/SalvageSiteCompat.java";
    private static final String ITEM_CRAFTING = "exiledsector/items/ItemCraftingCosts.java";
    private static final String HULL_ITEM_IMPLICITS = "exiledsector/items/ItemImplicits.java";
    private static final String ITEM_MODIFIERS = "exiledsector/items/ItemModifiers.java";
    private static final String STAR_CHART_DEFINITIONS = "exiledsector/items/StarChartDefinitions.java";
    private static final String STAR_CHART_IMPLICITS = "exiledsector/items/StarChartImplicits.java";
    private static final String STAR_CHART_ITEMS = "exiledsector/items/StarChartItems.java";
    private static final String STAR_CHART_NAMES = "exiledsector/items/StarChartNames.java";
    private static final String CHART_TIERS = "exiledsector/charts/ChartTiers.java";
    private static final String CHART_POIS = "exiledsector/charts/ChartPois.java";
    private static final String CHART_FACTIONS = "exiledsector/charts/ChartFactions.java";
    private static final String CHART_BLOCKED_ABILITIES = "exiledsector/charts/ChartBlockedAbilities.java";
    private static final String CHART_DROPS = "exiledsector/charts/ChartDrops.java";
    private static final String WORMHOLE_ANCHOR_DEFINITIONS = "exiledsector/items/WormholeAnchorDefinitions.java";

    private static final List<String> LOADERS = List.of(TYPE_LOADER, TREE_LOADER, ITEM_DEFINITIONS, ITEM_DEFINITION,
            ITEM_NAMES, ITEM_DROPS, SALVAGE_COMPAT, ITEM_CRAFTING, HULL_ITEM_IMPLICITS, ITEM_MODIFIERS, STAR_CHART_DEFINITIONS, STAR_CHART_IMPLICITS,
            STAR_CHART_ITEMS, STAR_CHART_NAMES, CHART_TIERS, CHART_POIS, CHART_FACTIONS, CHART_BLOCKED_ABILITIES, CHART_DROPS, WORMHOLE_ANCHOR_DEFINITIONS);

    private static final List<String> EDITED_DATA_PATHS = List.of("data/skilltrees/skill_types.json", "data/skilltrees/ship_skill_tree.json",
            "data/config/exiledSector/item_modifiers", "data/config/exiledSector/item_names", "data/config/exiledSector/item_crafting",
            "data/config/exiledSector/item_salvage", "data/config/exiledSector/hull_item", "data/config/exiledSector/subroutines", "compat/salvage/",
            "data/config/exiledSector/star_chart", "data/config/exiledSector/chart_", "data/config/exiledSector/wormhole_anchors");

    private record EditorSource(String file, String symbol) {
        String label() {
            return EDITOR_SOURCES.resolve(file).toString().replace('\\', '/') + "#" + symbol;
        }
    }

    private record Pin(String description, String loader, List<String> loaderMethods, List<EditorSource> editorSources, boolean nameLiterals) {
        String editorLabels() {
            return editorSources.stream().map(EditorSource::label).collect(Collectors.joining(" / "));
        }
    }

    private static EditorSource tree(String symbol) {
        return new EditorSource(TREE_SERIALIZATION, symbol);
    }

    private static final List<Pin> PINS = List.of(
            new Pin("skill_types.json root", TYPE_LOADER, List.of("loadAll", "parseSkillTypes"),
                    List.of(tree("skillTypesFileObject")), false),
            new Pin("skill_types.json skill type", TYPE_LOADER, List.of("parseSkillType", "parseTemporaryAfterDeploymentSeconds", "entryLabel"),
                    List.of(tree("serializeType")), false),
            new Pin("skill_types.json itemCost", TYPE_LOADER, List.of("parseItemCost"), List.of(tree("serializeType")), false),
            new Pin("skill_types.json effects and hullSizeEffects", TYPE_LOADER, List.of("parseEffects", "parseHullSizeEffects"),
                    List.of(tree("serializeType")), false),
            new Pin("skill_types.json unlockConditions", TYPE_LOADER, List.of("parseUnlockCondition"),
                    List.of(tree("serializeUnlockConditions")), false),
            new Pin("ship_skill_tree.json root, connectorCurves and hiddenConnectors", TREE_LOADER,
                    List.of("loadAll", "parseNodes", "parseConnectorCurves", "parseHiddenConnectors"), List.of(tree("treeFileObject")), false),
            new Pin("ship_skill_tree.json node", TREE_LOADER, List.of("parseNode"), List.of(tree("serializeTreeNode")), false),
            new Pin("ship_skill_tree.json ringBelts", TREE_LOADER, List.of("parseRingBelts"),
                    List.of(tree("treeFileObject"), tree("serializeRingBelt")), false),
            new Pin("ship_skill_tree.json staticImages", TREE_LOADER, List.of("parseStaticImages"),
                    List.of(tree("treeFileObject"), tree("serializeStaticImage")), false),
            new Pin("ship_skill_tree.json stars", TREE_LOADER, List.of("parseStars"),
                    List.of(tree("treeFileObject"), tree("serializeStar")), false),
            new Pin("subroutines.csv and hull_items.csv row key", ITEM_DEFINITIONS, List.of("rows"),
                    List.of(new EditorSource(ITEM_DEFINITIONS_MODEL, "SUBROUTINE_COLUMNS"), new EditorSource(ITEM_DEFINITIONS_MODEL, "HULL_ITEM_COLUMNS")), true),
            new Pin("subroutines.csv columns", ITEM_DEFINITION, List.of("parseSubroutine", "definitionId", "parseShared", "parseCommon"),
                    List.of(new EditorSource(ITEM_DEFINITIONS_MODEL, "SUBROUTINE_COLUMNS")), true),
            new Pin("hull_items.csv columns", ITEM_DEFINITION, List.of("parse", "parseHullItem", "definitionId", "parseShared", "parseCommon"),
                    List.of(new EditorSource(ITEM_DEFINITIONS_MODEL, "HULL_ITEM_COLUMNS")), true),
            new Pin("item_modifiers.csv columns", ITEM_MODIFIERS, List.of("load", "catalogueOf", "parse"),
                    List.of(new EditorSource(ITEM_MODIFIERS_MODEL, "serializeModifiersCsv")), true),
            new Pin("item_names.json word fields", ITEM_NAMES, List.of("registerWords"),
                    List.of(new EditorSource(ITEM_NAMES_MODEL, "serializeRareNames"), new EditorSource(ITEM_NAMES_MODEL, "RARE_NAME_WORD_LISTS"),
                            new EditorSource(ITEM_NAMES_MODEL, "newRareNameWords")), true),
            new Pin("item_salvage.csv columns", ITEM_DROPS, List.of("load", "parse"),
                    List.of(new EditorSource(ITEM_DROPS_MODEL, "serializeSalvageCsv")), true),
            new Pin("compat/salvage/*.csv columns", SALVAGE_COMPAT, List.of("rows", "missingSites"),
                    List.of(new EditorSource(ITEM_DROPS_MODEL, "serializeSalvageCsv")), true),
            new Pin("item_crafting.csv columns", ITEM_CRAFTING, List.of("load", "register"),
                    List.of(new EditorSource(ITEM_CRAFTING_MODEL, "serializeCraftingCsv")), true),
            new Pin("hull_item_implicits.csv columns", HULL_ITEM_IMPLICITS, List.of("load", "register"),
                    List.of(new EditorSource(HULL_ITEM_IMPLICITS_MODEL, "serializeImplicitsCsv")), true),
            new Pin("star_charts.csv columns", STAR_CHART_DEFINITIONS, List.of("load", "register"),
                    List.of(new EditorSource(ITEM_DEFINITIONS_MODEL, "STAR_CHART_COLUMNS")), true),
            new Pin("star_charts.csv shared item columns", ITEM_DEFINITION, List.of("parse", "definitionId", "parseShared", "parseCommon"),
                    List.of(new EditorSource(ITEM_DEFINITIONS_MODEL, "STAR_CHART_COLUMNS")), true),
            new Pin("star_chart_modifiers.csv columns", STAR_CHART_ITEMS, List.of("load"),
                    List.of(new EditorSource(ITEM_MODIFIERS_MODEL, "serializeModifiersCsv")), true),
            new Pin("star_chart_modifiers.csv shared modifier columns", ITEM_MODIFIERS, List.of("catalogueOf", "parse"),
                    List.of(new EditorSource(ITEM_MODIFIERS_MODEL, "serializeModifiersCsv")), true),
            new Pin("star_chart_implicits.csv columns", STAR_CHART_IMPLICITS, List.of("load", "register"),
                    List.of(new EditorSource(HULL_ITEM_IMPLICITS_MODEL, "serializeStarChartImplicitsCsv")), true),
            new Pin("star_chart_names.json word fields", STAR_CHART_NAMES, List.of("register"),
                    List.of(new EditorSource(ITEM_NAMES_MODEL, "serializeStarChartNames"), new EditorSource(ITEM_NAMES_MODEL, "RARE_NAME_WORD_LISTS"),
                            new EditorSource(ITEM_NAMES_MODEL, "newRareNameWords")), true),
            new Pin("star_chart_tiers.csv columns", CHART_TIERS, List.of("load", "register"),
                    List.of(new EditorSource(CHART_TABLES_MODEL, "CHART_TIER_COLUMNS")), true),
            new Pin("chart_poi.csv columns", CHART_POIS, List.of("load", "register", "parse"),
                    List.of(new EditorSource(CHART_TABLES_MODEL, "CHART_POI_COLUMNS")), true),
            new Pin("chart_factions.csv columns", CHART_FACTIONS, List.of("load", "register"),
                    List.of(new EditorSource(CHART_TABLES_MODEL, "CHART_FACTION_COLUMNS")), true),
            new Pin("chart_blocked_abilities.csv columns", CHART_BLOCKED_ABILITIES, List.of("load"),
                    List.of(new EditorSource(CHART_TABLES_MODEL, "CHART_BLOCKED_ABILITY_COLUMNS")), true),
            new Pin("chart_drops.csv columns", CHART_DROPS, List.of("load", "register"),
                    List.of(new EditorSource(CHART_TABLES_MODEL, "CHART_DROP_COLUMNS")), true),
            new Pin("wormhole_anchors.csv columns", WORMHOLE_ANCHOR_DEFINITIONS, List.of("load", "register"),
                    List.of(new EditorSource(WORMHOLE_ANCHORS_MODEL, "WORMHOLE_ANCHOR_COLUMNS")), true),
            new Pin("wormhole_anchors.csv shared item columns", ITEM_DEFINITION, List.of("definitionId", "parseCommon"),
                    List.of(new EditorSource(WORMHOLE_ANCHORS_MODEL, "WORMHOLE_ANCHOR_COLUMNS")), true));

    private static final String JAVA_KEY_READ_PREFIX = "(?:\\.\\s*(?:opt|get|has|isNull)[A-Za-z]*\\s*\\(\\s*"
            + "|\\bModCsv\\.text\\s*\\([^,()\"]*,\\s*|\\bModCsv\\.(?:load|rows)\\s*\\(\\s*|\\bcsv\\.(?:integer|number|flag)\\s*\\([^,()\"]*,\\s*)";
    private static final Pattern JAVA_KEY_READ = Pattern.compile(JAVA_KEY_READ_PREFIX + "\"([^\"\\\\]*)\"");
    private static final Pattern JAVA_CONSTANT_KEY_READ = Pattern.compile(JAVA_KEY_READ_PREFIX + "([A-Z][A-Z0-9_]*)\\s*[,)]");
    private static final Pattern JAVA_STRING_CONSTANT = Pattern.compile("\\bstatic\\s+final\\s+String\\s+([A-Z][A-Z0-9_]*)\\s*=\\s*\"([^\"\\\\]*)\"\\s*;");
    private static final Pattern JAVA_METHOD_START = Pattern.compile("([\\w$]+|[>\\]])\\s+([\\w$]+)\\s*\\(");
    private static final Set<String> JAVA_NOT_METHODS = Set.of("if", "for", "while", "switch", "catch", "synchronized", "return", "new",
            "else", "try", "do", "throw", "yield", "assert", "case", "throws");
    private static final Pattern JS_OBJECT_KEY = Pattern.compile("[{,]\\s*([A-Za-z_$][\\w$]*)\\s*:(?!:)");
    private static final Pattern JS_MEMBER_ASSIGNMENT = Pattern.compile("\\.\\s*([A-Za-z_$][\\w$]*)\\s*=(?![=>])");
    private static final Pattern NAME_LIST = Pattern.compile("[A-Za-z_$][\\w$]*(?:,[A-Za-z_$][\\w$]*)*");

    @Test
    void theEditorWritesEveryFieldTheLoadersRead() throws IOException {
        Map<String, String> editorFiles = new TreeMap<>();
        for (Pin pin : PINS) {
            for (EditorSource source : pin.editorSources()) {
                if (!editorFiles.containsKey(source.file())) {
                    editorFiles.put(source.file(), read(EDITOR_SOURCES.resolve(source.file())));
                }
            }
        }
        Map<String, Map<String, Set<String>>> keysByLoader = new LinkedHashMap<>();
        for (String loader : LOADERS) {
            keysByLoader.put(loader, keysReadByMethod(read(MAIN_SOURCES.resolve(loader))));
        }
        List<String> problems = new ArrayList<>();
        for (Pin pin : PINS) {
            Map<String, Set<String>> methods = keysByLoader.get(pin.loader());
            Set<String> loaderKeys = new TreeSet<>();
            for (String method : pin.loaderMethods()) {
                Set<String> keys = methods.get(method);
                if (keys == null || keys.isEmpty()) {
                    problems.add(pin.description() + ": " + loaderName(pin.loader()) + "." + method + " reads no fields any more - update the pin in "
                            + EditorRoundTripTest.class.getSimpleName());
                } else {
                    loaderKeys.addAll(keys);
                }
            }
            Set<String> editorKeys = new TreeSet<>();
            for (EditorSource source : pin.editorSources()) {
                editorKeys.addAll(editorKeys(editorFiles.get(source.file()), source.symbol(), source.label(), pin.nameLiterals()));
            }
            if (editorKeys.isEmpty()) {
                problems.add(pin.description() + ": found no written fields in " + pin.editorLabels());
            }
            Set<String> missing = new TreeSet<>(loaderKeys);
            missing.removeAll(editorKeys);
            if (!missing.isEmpty()) {
                problems.add(pin.description() + ": the editor drops " + missing + " on save - " + loaderName(pin.loader()) + " reads "
                        + (missing.size() == 1 ? "it" : "them") + " but " + pin.editorLabels()
                        + " never write" + (pin.editorSources().size() == 1 ? "s " : " ") + (missing.size() == 1 ? "it" : "them"));
            }
        }
        assertTrue(problems.isEmpty(), "Editor saves would silently delete loader fields:\n" + String.join("\n", problems));
    }

    @Test
    void everyLoaderMethodThatReadsFieldsIsPinnedToTheEditor() throws IOException {
        List<String> unpinned = new ArrayList<>();
        for (String loader : LOADERS) {
            Set<String> pinned = PINS.stream().filter(pin -> pin.loader().equals(loader))
                    .flatMap(pin -> pin.loaderMethods().stream()).collect(Collectors.toSet());
            keysReadByMethod(read(MAIN_SOURCES.resolve(loader))).forEach((method, keys) -> {
                if (!keys.isEmpty() && !pinned.contains(method)) {
                    unpinned.add(loaderName(loader) + "." + method + " reads " + keys);
                }
            });
        }
        assertTrue(unpinned.isEmpty(), "Pin these loader methods to the editor serializer that writes their fields in "
                + EditorRoundTripTest.class.getSimpleName() + ":\n" + String.join("\n", unpinned));
    }

    @Test
    void everyClassThatReadsAnEditedFileIsAKnownLoader() throws IOException {
        Set<String> readers = new TreeSet<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = read(file);
                if (EDITED_DATA_PATHS.stream().anyMatch(source::contains)) {
                    readers.add(MAIN_SOURCES.relativize(file).toString().replace('\\', '/'));
                }
            }
        }
        readers.removeAll(LOADERS);
        assertTrue(readers.isEmpty(), "These classes read a file the skill tree editor saves; add them to LOADERS and pin their fields in "
                + EditorRoundTripTest.class.getSimpleName() + ": " + readers);
    }

    @Test
    void theExtractorsTolerateFormatting() {
        String java = String.join("\n",
                "class Sample {",
                "    // json.optString(\"commented\")",
                "    private static",
                "    Map < String , Object >",
                "    parseThing ( JSONObject json )",
                "            throws JSONException",
                "    {",
                "        String brace = \"}{\";",
                "        char c = '}';",
                "        if (json . has ( \"alpha\" )) {",
                "            json.optJSONArray(\"beta\");",
                "        }",
                "        return json",
                "                .getDouble(\"gamma\");",
                "    }",
                "    /* json.getString(\"block\") */",
                "    static <T> T other(JSONObject j) { return j.getString(\"delta\"); }",
                "}");
        Map<String, Set<String>> keys = keysReadByMethod(java);
        assertEquals(Set.of("alpha", "beta", "gamma"), keys.get("parseThing"));
        assertEquals(Set.of("delta"), keys.get("other"));
        assertFalse(keys.values().stream().anyMatch(set -> set.contains("commented") || set.contains("block")));

        String constantsJava = String.join("\n",
                "class Constants {",
                "    static final String TIER_COLUMN = \"tier\";",
                "    private static final String UNREAD = \"unread\";",
                "    static final String PATH_CONSTANT = \"data/config/x.csv\";",
                "    static void register(JSONObject row, ChartCsv csv) {",
                "        csv.integer(row, TIER_COLUMN, 0, label);",
                "        csv.number(row, \"fleet_power\", 1f, label);",
                "        csv.flag(row , \"boss\", false, label);",
                "        ModCsv.text(row, OTHER_COLUMN);",
                "        String unrelated = UNREAD;",
                "        json.optString(PATH_CONSTANT);",
                "    }",
                "    static void load() { ModCsv.load(TIER_COLUMN, PATH, LOG, Constants::register); }",
                "}");
        Map<String, Set<String>> constantKeys = keysReadByMethod(constantsJava);
        assertEquals(Set.of("tier", "fleet_power", "boss"), constantKeys.get("register"));
        assertEquals(Set.of("tier"), constantKeys.get("load"));

        String js = String.join("\n",
                "function serializeSample (t) {",
                "  var re = /[\"'{]/g, half = t.size / 2;",
                "  var out = {",
                "    first : t.a, 'quoted': t.b,",
                "    nested: { inner: 1 }",
                "  };",
                "  var text = '{ notAKey: 1 }';",
                "  out . assigned = t.c;",
                "  out['bracketed'] = t.d;",
                "  if (t.e == t.f) out.compared === 1;",
                "  return out;",
                "}",
                "function after(){ return { outside: 1 }; }");
        assertEquals(Set.of("first", "quoted", "nested", "inner", "assigned", "bracketed"), editorKeys(js, "serializeSample", "sample", false));
        assertTrue(editorKeys("function h(){ return ['site,chances', 'not a name', 'x'].join(); }", "h", "sample", true)
                .containsAll(Set.of("site", "chances", "x")));

        String ts = String.join("\n",
                "export function typed(entry: { a: number }, ratio = 2): Record<string, unknown> {",
                "  const out: Record<string, unknown> = { kept: entry.a / ratio };",
                "  out.later = 1;",
                "  return out;",
                "}",
                "export const COLUMNS = ['id', 'kind'] as const;");
        assertEquals(Set.of("kept", "later"), editorKeys(ts, "typed", "sample", false));
        assertEquals(Set.of("id", "kind"), editorKeys(ts, "COLUMNS", "sample", true));
    }

    private static String loaderName(String loader) {
        return loader.substring(loader.lastIndexOf('/') + 1, loader.length() - ".java".length());
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Map<String, Set<String>> keysReadByMethod(String source) {
        Scan scan = Scan.java(source);
        String code = scan.masked;
        List<int[]> bodies = new ArrayList<>();
        List<String> names = new ArrayList<>();
        Matcher start = JAVA_METHOD_START.matcher(code);
        while (start.find()) {
            String name = start.group(2);
            if (JAVA_NOT_METHODS.contains(name) || JAVA_NOT_METHODS.contains(start.group(1))) {
                continue;
            }
            int close = matching(code, start.end() - 1, '(', ')');
            int brace = skipThrowsClause(code, close + 1);
            if (close < 0 || brace < 0) {
                continue;
            }
            int end = matching(code, brace, '{', '}');
            if (end > 0) {
                bodies.add(new int[]{brace, end});
                names.add(name);
            }
        }
        Map<String, Set<String>> keys = new TreeMap<>();
        names.forEach(name -> keys.putIfAbsent(name, new TreeSet<>()));
        Matcher read = JAVA_KEY_READ.matcher(scan.stringsKept);
        while (read.find()) {
            if (code.charAt(read.start()) == scan.stringsKept.charAt(read.start())) {
                addKeyToInnermostMethod(keys, bodies, names, read.start(), read.group(1));
            }
        }
        Map<String, String> constants = new TreeMap<>();
        Matcher constant = JAVA_STRING_CONSTANT.matcher(scan.stringsKept);
        while (constant.find()) {
            constants.put(constant.group(1), constant.group(2));
        }
        Matcher constantRead = JAVA_CONSTANT_KEY_READ.matcher(code);
        while (constantRead.find()) {
            String constantValue = constants.get(constantRead.group(1));
            if (constantValue != null && !constantValue.contains("/")) {
                addKeyToInnermostMethod(keys, bodies, names, constantRead.start(), constantValue);
            }
        }
        return keys;
    }

    private static void addKeyToInnermostMethod(Map<String, Set<String>> keys, List<int[]> bodies, List<String> names, int position, String key) {
        int innermost = -1;
        for (int i = 0; i < bodies.size(); i++) {
            int[] body = bodies.get(i);
            if (body[0] < position && position < body[1] && (innermost < 0 || body[0] > bodies.get(innermost)[0])) {
                innermost = i;
            }
        }
        if (innermost >= 0) {
            keys.get(names.get(innermost)).add(key);
        }
    }

    private static int skipThrowsClause(String code, int from) {
        int i = skipSpace(code, from);
        if (code.startsWith("throws", i)) {
            i += "throws".length();
            while (i < code.length() && (Character.isJavaIdentifierPart(code.charAt(i)) || code.charAt(i) == '.' || code.charAt(i) == ','
                    || Character.isWhitespace(code.charAt(i)))) {
                i++;
            }
        }
        return i < code.length() && code.charAt(i) == '{' ? i : -1;
    }

    private static Set<String> editorKeys(String source, String symbol, String label, boolean nameLiterals) {
        Scan scan = Scan.javaScript(source);
        int[] range = sourceRange(scan.masked, symbol, label);
        Set<String> keys = new TreeSet<>();
        String code = scan.masked.substring(range[0], range[1]);
        addGroups(keys, JS_OBJECT_KEY.matcher(code));
        addGroups(keys, JS_MEMBER_ASSIGNMENT.matcher(code));
        for (int[] literal : scan.literals) {
            if (literal[0] < range[0] || literal[1] > range[1]) {
                continue;
            }
            String value = source.substring(literal[0] + 1, literal[1] - 1);
            char before = previousNonSpace(scan.masked, literal[0] - 1);
            char after = nextNonSpace(scan.masked, literal[1]);
            if ((before == '{' || before == ',') && after == ':') {
                keys.add(value);
            } else if (before == '[' && after == ']' && assignedAfterBracket(scan.masked, literal[1])) {
                keys.add(value);
            } else if (nameLiterals && NAME_LIST.matcher(value).matches()) {
                keys.addAll(List.of(value.split(",")));
            }
        }
        return keys;
    }

    private static boolean assignedAfterBracket(String code, int from) {
        int i = skipSpace(code, from);
        i = skipSpace(code, i + 1);
        return i < code.length() && code.charAt(i) == '=' && (i + 1 >= code.length() || code.charAt(i + 1) != '=');
    }

    private static int[] sourceRange(String code, String symbol, String label) {
        Matcher function = Pattern.compile("\\bfunction\\s+" + Pattern.quote(symbol) + "\\s*\\(").matcher(code);
        if (function.find()) {
            int paramsEnd = matching(code, function.end() - 1, '(', ')');
            int open = skipSpace(code, paramsEnd + 1);
            if (open < code.length() && code.charAt(open) == ':') {
                open = code.indexOf('{', open);
            }
            int close = matching(code, open, '{', '}');
            assertTrue(paramsEnd > 0 && close > open, "Could not find the body of " + label);
            assertFalse(function.find(), label + " is declared more than once");
            return new int[]{open, close + 1};
        }
        Matcher declaration = Pattern.compile("\\b(?:var|let|const)\\s+" + Pattern.quote(symbol) + "\\b[^=]*=\\s*").matcher(code);
        if (!declaration.find()) {
            fail(label + " not found");
        }
        int open = declaration.end();
        char opener = code.charAt(open);
        int close = opener == '[' ? matching(code, open, '[', ']') : opener == '{' ? matching(code, open, '{', '}') : code.indexOf(';', open);
        assertTrue(close > open, "Could not find the end of " + label);
        assertFalse(declaration.find(), label + " is declared more than once");
        return new int[]{open, close + 1};
    }

    private static void addGroups(Set<String> keys, Matcher matcher) {
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
    }

    private static int matching(String code, int open, char opener, char closer) {
        if (open < 0 || open >= code.length() || code.charAt(open) != opener) {
            return -1;
        }
        int depth = 0;
        for (int i = open; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c == opener) {
                depth++;
            } else if (c == closer && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int skipSpace(String code, int from) {
        int i = Math.max(from, 0);
        while (i < code.length() && Character.isWhitespace(code.charAt(i))) {
            i++;
        }
        return i;
    }

    private static char previousNonSpace(String code, int from) {
        int i = from;
        while (i >= 0 && Character.isWhitespace(code.charAt(i))) {
            i--;
        }
        return i < 0 ? '\0' : code.charAt(i);
    }

    private static char nextNonSpace(String code, int from) {
        int i = skipSpace(code, from);
        return i < code.length() ? code.charAt(i) : '\0';
    }

    private static final class Scan {
        final String masked;
        final String stringsKept;
        final List<int[]> literals = new ArrayList<>();

        private Scan(String masked, String stringsKept) {
            this.masked = masked;
            this.stringsKept = stringsKept;
        }

        static Scan java(String source) {
            return scan(source, false);
        }

        static Scan javaScript(String source) {
            return scan(source, true);
        }

        private static Scan scan(String source, boolean javaScript) {
            StringBuilder masked = new StringBuilder(source);
            StringBuilder stringsKept = new StringBuilder(source);
            List<int[]> literals = new ArrayList<>();
            int i = 0;
            while (i < source.length()) {
                char c = source.charAt(i);
                char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
                int end;
                if (c == '/' && next == '/') {
                    end = endOfLine(source, i);
                    blank(masked, i, end);
                    blank(stringsKept, i, end);
                } else if (c == '/' && next == '*') {
                    int close = source.indexOf("*/", i + 2);
                    end = close < 0 ? source.length() : close + 2;
                    blank(masked, i, end);
                    blank(stringsKept, i, end);
                } else if (!javaScript && source.startsWith("\"\"\"", i)) {
                    int close = source.indexOf("\"\"\"", i + 3);
                    end = close < 0 ? source.length() : close + 3;
                    blank(masked, i + 3, end - 3);
                } else if (c == '"' || c == '\'' || (javaScript && c == '`')) {
                    end = endOfQuoted(source, i, c);
                    blank(masked, i + 1, end - 1);
                    literals.add(new int[]{i, end});
                } else if (javaScript && c == '/' && startsRegex(masked, i)) {
                    end = endOfRegex(source, i);
                    blank(masked, i + 1, end - 1);
                } else {
                    end = i + 1;
                }
                i = end;
            }
            Scan scan = new Scan(masked.toString(), stringsKept.toString());
            scan.literals.addAll(literals);
            return scan;
        }

        private static void blank(StringBuilder text, int from, int to) {
            for (int i = from; i < to && i < text.length(); i++) {
                if (text.charAt(i) != '\n' && text.charAt(i) != '\r') {
                    text.setCharAt(i, ' ');
                }
            }
        }

        private static int endOfLine(String source, int from) {
            int newline = source.indexOf('\n', from);
            return newline < 0 ? source.length() : newline;
        }

        private static int endOfQuoted(String source, int open, char quote) {
            for (int i = open + 1; i < source.length(); i++) {
                char c = source.charAt(i);
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    return i + 1;
                } else if (c == '\n' && quote != '`') {
                    return i;
                }
            }
            return source.length();
        }

        private static boolean startsRegex(CharSequence code, int slash) {
            int i = slash - 1;
            while (i >= 0 && Character.isWhitespace(code.charAt(i))) {
                i--;
            }
            if (i < 0) {
                return true;
            }
            char before = code.charAt(i);
            if ("(,=:[!&|?{};+-*%<>~^".indexOf(before) >= 0) {
                return true;
            }
            int wordEnd = i + 1;
            while (i >= 0 && Character.isJavaIdentifierPart(code.charAt(i))) {
                i--;
            }
            String word = code.subSequence(i + 1, wordEnd).toString();
            return word.equals("return") || word.equals("typeof") || word.equals("case") || word.equals("in");
        }

        private static int endOfRegex(String source, int open) {
            boolean inClass = false;
            for (int i = open + 1; i < source.length(); i++) {
                char c = source.charAt(i);
                if (c == '\\') {
                    i++;
                } else if (c == '[') {
                    inClass = true;
                } else if (c == ']') {
                    inClass = false;
                } else if (c == '/' && !inClass) {
                    return i + 1;
                } else if (c == '\n') {
                    return i;
                }
            }
            return source.length();
        }
    }
}
