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

    private static final Path EDITOR = Path.of("tools/skill_tree_editor.html");
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final String TYPE_LOADER = "exiledsector/skills/loader/SkillTypeLoader.java";
    private static final String TREE_LOADER = "exiledsector/skills/loader/SkillTreeLoader.java";
    private static final String SOCKETABLE_DEFINITIONS = "exiledsector/socketables/SocketableDefinitions.java";
    private static final String SOCKETABLE_DEFINITION = "exiledsector/socketables/SocketableDefinition.java";
    private static final String SOCKETABLE_NAMES = "exiledsector/socketables/SocketableNames.java";
    private static final String SOCKETABLE_DROPS = "exiledsector/socketables/SocketableDrops.java";
    private static final String SALVAGE_COMPAT = "exiledsector/compat/SalvageSiteCompat.java";
    private static final String SOCKETABLE_CRAFTING = "exiledsector/socketables/SocketCraftingCosts.java";

    private static final List<String> LOADERS = List.of(TYPE_LOADER, TREE_LOADER, SOCKETABLE_DEFINITIONS, SOCKETABLE_DEFINITION,
            SOCKETABLE_NAMES, SOCKETABLE_DROPS, SALVAGE_COMPAT, SOCKETABLE_CRAFTING);

    private static final List<String> EDITED_DATA_PATHS = List.of("data/skilltrees/skill_types.json", "data/skilltrees/ship_skill_tree.json",
            "data/config/exiledSector/socketable", "compat/salvage/");

    private record Pin(String description, String loader, List<String> loaderMethods, List<String> editorSources, boolean nameLiterals) {
    }

    private static final List<Pin> PINS = List.of(
            new Pin("skill_types.json root", TYPE_LOADER, List.of("loadAll", "parseSkillTypes"),
                    List.of("serializeSkillTypesFile"), false),
            new Pin("skill_types.json skill type", TYPE_LOADER, List.of("parseSkillType", "parseTemporaryAfterDeploymentSeconds", "entryLabel"),
                    List.of("serializeType"), false),
            new Pin("skill_types.json skill type rebuilt by the type editor dialog", TYPE_LOADER,
                    List.of("parseSkillType", "parseTemporaryAfterDeploymentSeconds"), List.of("openTypeModal#updated"), false),
            new Pin("skill_types.json itemCost", TYPE_LOADER, List.of("parseItemCost"), List.of("serializeType"), false),
            new Pin("skill_types.json effects and hullSizeEffects", TYPE_LOADER, List.of("parseEffects", "parseHullSizeEffects"),
                    List.of("serializeType"), false),
            new Pin("skill_types.json unlockConditions", TYPE_LOADER, List.of("parseUnlockCondition"),
                    List.of("serializeUnlockConditions"), false),
            new Pin("ship_skill_tree.json root, connectorCurves and hiddenConnectors", TREE_LOADER,
                    List.of("loadAll", "parseNodes", "parseConnectorCurves", "parseHiddenConnectors"), List.of("buildTreeExportObject"), false),
            new Pin("ship_skill_tree.json node", TREE_LOADER, List.of("parseNode"), List.of("serializeTreeNode"), false),
            new Pin("ship_skill_tree.json ringBelts", TREE_LOADER, List.of("parseRingBelts"),
                    List.of("buildTreeExportObject", "serializeRingBelt"), false),
            new Pin("ship_skill_tree.json staticImages", TREE_LOADER, List.of("parseStaticImages"),
                    List.of("buildTreeExportObject", "serializeStaticImage"), false),
            new Pin("ship_skill_tree.json stars", TREE_LOADER, List.of("parseStars"),
                    List.of("buildTreeExportObject", "serializeStar"), false),
            new Pin("socketables.csv columns", SOCKETABLE_DEFINITION, List.of("parse"), List.of("var SOCKETABLE_COLUMNS"), true),
            new Pin("socketable_affixes.csv columns", SOCKETABLE_NAMES, List.of("load", "registerAffixes"),
                    List.of("serializeAffixesCsv"), true),
            new Pin("socketable_names.json word fields", SOCKETABLE_NAMES, List.of("registerWords"),
                    List.of("serializeRareNames", "openSocketableNamesModal"), true),
            new Pin("socketable_salvage.csv columns", SOCKETABLE_DROPS, List.of("load", "parse"), List.of("serializeSalvageCsv"), true),
            new Pin("compat/salvage/*.csv columns", SALVAGE_COMPAT, List.of("rows", "missingSites"), List.of("serializeSalvageCsv"), true),
            new Pin("socketable_crafting.csv columns", SOCKETABLE_CRAFTING, List.of("load", "register"), List.of("serializeCraftingCsv"), true));

    private static final Pattern JAVA_KEY_READ = Pattern.compile("(?:\\.\\s*(?:opt|get|has|isNull)[A-Za-z]*\\s*\\(\\s*"
            + "|\\bModCsv\\.text\\s*\\([^,()\"]*,\\s*|\\bModCsv\\.(?:load|rows)\\s*\\(\\s*)\"([^\"\\\\]*)\"");
    private static final Pattern JAVA_METHOD_START = Pattern.compile("([\\w$]+|[>\\]])\\s+([\\w$]+)\\s*\\(");
    private static final Set<String> JAVA_NOT_METHODS = Set.of("if", "for", "while", "switch", "catch", "synchronized", "return", "new",
            "else", "try", "do", "throw", "yield", "assert", "case", "throws");
    private static final Pattern JS_OBJECT_KEY = Pattern.compile("[{,]\\s*([A-Za-z_$][\\w$]*)\\s*:(?!:)");
    private static final Pattern JS_MEMBER_ASSIGNMENT = Pattern.compile("\\.\\s*([A-Za-z_$][\\w$]*)\\s*=(?![=>])");
    private static final Pattern NAME_LIST = Pattern.compile("[A-Za-z_$][\\w$]*(?:,[A-Za-z_$][\\w$]*)*");

    @Test
    void theEditorWritesEveryFieldTheLoadersRead() throws IOException {
        String editor = read(EDITOR);
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
            for (String source : pin.editorSources()) {
                editorKeys.addAll(editorKeys(editor, source, pin.nameLiterals()));
            }
            if (editorKeys.isEmpty()) {
                problems.add(pin.description() + ": found no written fields in " + String.join(" / ", pin.editorSources()) + " in " + EDITOR);
            }
            Set<String> missing = new TreeSet<>(loaderKeys);
            missing.removeAll(editorKeys);
            if (!missing.isEmpty()) {
                problems.add(pin.description() + ": the editor drops " + missing + " on save - " + loaderName(pin.loader()) + " reads "
                        + (missing.size() == 1 ? "it" : "them") + " but " + String.join(" / ", pin.editorSources()) + " in " + EDITOR
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
        assertEquals(Set.of("first", "quoted", "nested", "inner", "assigned", "bracketed"), editorKeys(js, "serializeSample", false));
        assertTrue(editorKeys("function h(){ return ['site,chances', 'not a name', 'x'].join(); }", "h", true)
                .containsAll(Set.of("site", "chances", "x")));
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
            if (code.charAt(read.start()) != scan.stringsKept.charAt(read.start())) {
                continue;
            }
            int innermost = -1;
            for (int i = 0; i < bodies.size(); i++) {
                int[] body = bodies.get(i);
                if (body[0] < read.start() && read.start() < body[1] && (innermost < 0 || body[0] > bodies.get(innermost)[0])) {
                    innermost = i;
                }
            }
            if (innermost >= 0) {
                keys.get(names.get(innermost)).add(read.group(1));
            }
        }
        return keys;
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

    private static String scriptOf(String html) {
        int open = html.indexOf("<script>");
        int close = html.lastIndexOf("</script>");
        return open < 0 || close < open ? html : html.substring(open + "<script>".length(), close);
    }

    private static Set<String> editorKeys(String editor, String source, boolean nameLiterals) {
        String html = scriptOf(editor);
        Scan scan = Scan.javaScript(html);
        int[] range = sourceRange(scan.masked, source);
        Set<String> keys = new TreeSet<>();
        String code = scan.masked.substring(range[0], range[1]);
        addGroups(keys, JS_OBJECT_KEY.matcher(code));
        addGroups(keys, JS_MEMBER_ASSIGNMENT.matcher(code));
        for (int[] literal : scan.literals) {
            if (literal[0] < range[0] || literal[1] > range[1]) {
                continue;
            }
            String value = html.substring(literal[0] + 1, literal[1] - 1);
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

    private static int[] sourceRange(String code, String source) {
        if (source.startsWith("var ")) {
            Matcher declaration = Pattern.compile("\\bvar\\s+" + Pattern.quote(source.substring(4)) + "\\s*=\\s*").matcher(code);
            if (!declaration.find()) {
                fail(source + " not found in " + EDITOR);
            }
            int open = declaration.end();
            char opener = code.charAt(open);
            int close = opener == '[' ? matching(code, open, '[', ']') : opener == '{' ? matching(code, open, '{', '}') : code.indexOf(';', open);
            assertTrue(close > open, "Could not find the end of " + source + " in " + EDITOR);
            assertFalse(declaration.find(), source + " is declared more than once in " + EDITOR);
            return new int[]{open, close + 1};
        }
        String[] parts = source.split("#");
        Matcher function = Pattern.compile("\\bfunction\\s+" + Pattern.quote(parts[0]) + "\\s*\\(").matcher(code);
        if (!function.find()) {
            fail("function " + parts[0] + " not found in " + EDITOR);
        }
        int paramsEnd = matching(code, function.end() - 1, '(', ')');
        int open = skipSpace(code, paramsEnd + 1);
        int close = matching(code, open, '{', '}');
        assertTrue(paramsEnd > 0 && code.charAt(open) == '{' && close > open, "Could not find the body of function " + parts[0] + " in " + EDITOR);
        assertFalse(function.find(), "function " + parts[0] + " is declared more than once in " + EDITOR);
        if (parts.length == 1) {
            return new int[]{open, close + 1};
        }
        Matcher variable = Pattern.compile("\\bvar\\s+" + Pattern.quote(parts[1]) + "\\s*=\\s*\\{").matcher(code).region(open, close);
        if (!variable.find()) {
            fail("var " + parts[1] + " = { ... } not found in function " + parts[0] + " in " + EDITOR);
        }
        int literalEnd = matching(code, variable.end() - 1, '{', '}');
        return new int[]{variable.end() - 1, literalEnd + 1};
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
