package exiledsector.items;

import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDataIntegrityTest {

    private static JSONObject modifier(String id, String slot, String lines, String name) throws JSONException {
        return ItemFixtures.modifierRow(id, slot, lines, name);
    }

    private static JSONObject subroutine(String id, String subroutineKind) throws JSONException {
        return ItemFixtures.subroutineRow(id).put(ItemDefinition.SUBROUTINE_KIND_COLUMN, subroutineKind);
    }

    private static JSONObject poolItem(String id, String prefixes, String suffixes, boolean unique) throws JSONException {
        return ItemFixtures.row(id, "bridge", prefixes, suffixes).put("unique", unique ? "TRUE" : "");
    }

    private static JSONObject hullItem(String id, String section, String prefixes, String suffixes) throws JSONException {
        return ItemFixtures.row(id, section, prefixes, suffixes);
    }

    private static JSONObject implicit(String section, String implicits) throws JSONException {
        return new JSONObject().put("section", section).put("implicits", implicits);
    }

    private static List<Violation> check(JSONArray modifiers, JSONArray subroutines, JSONArray hullItems, JSONArray implicits) throws JSONException {
        return ItemDataIntegrity.check(modifiers, subroutines, hullItems, implicits);
    }

    private static List<String> errors(JSONArray modifiers, JSONArray subroutines, JSONArray hullItems, JSONArray implicits) throws JSONException {
        return check(modifiers, subroutines, hullItems, implicits).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();
    }

    private static List<String> errors(JSONArray modifiers, JSONArray subroutines, JSONArray implicits) throws JSONException {
        return errors(modifiers, subroutines, validHullItems(), implicits);
    }

    private static JSONArray validModifiers() throws JSONException {
        return new JSONArray()
                .put(modifier("HULL_MULT", "prefix", "HULL_MULT:4:6", "Reinforced"))
                .put(modifier("ARMOR_PERCENT", "suffix", "ARMOR_PERCENT:6:9", "Plating"))
                .put(modifier("HYBRID_ARMOR_HULL", "implicit", "ARMOR_PERCENT:5:10; HULL_PERCENT:3:6", ""));
    }

    private static JSONArray validSubroutines() throws JSONException {
        return new JSONArray().put(subroutine("chip", "MODSPEC_CACHE"));
    }

    private static JSONArray validHullItems() throws JSONException {
        return new JSONArray().put(hullItem("plate", "armor_plating", "@HULL_MULT:10", "@ARMOR_PERCENT"));
    }

    private static JSONArray validImplicits() throws JSONException {
        return new JSONArray().put(implicit("armor_plating", "@HYBRID_ARMOR_HULL"));
    }

    @Test
    void theShippedItemDataHasNoIntegrityErrors() throws Exception {
        List<Violation> violations = check(ItemFixtures.shippedRows(ItemModifiers.DATA_PATH), ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH),
                ItemFixtures.shippedRows(ItemDefinitions.HULL_ITEMS_PATH), ItemFixtures.shippedRows(ItemImplicits.DATA_PATH));

        assertEquals(List.of(), violations.stream().filter(violation -> violation.severity() == Severity.ERROR).toList());
    }

    @Test
    void theShippedSubroutineCorruptionPoolIsCheckedAndCountsAsUsed() throws Exception {
        List<Violation> violations = ItemDataIntegrity.checkAll(ItemFixtures.shippedRows(ItemModifiers.DATA_PATH),
                ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH), ItemFixtures.shippedRows(ItemDefinitions.HULL_ITEMS_PATH),
                ItemFixtures.shippedRows(ItemImplicits.DATA_PATH), new JSONArray(), new JSONArray(), new JSONArray(),
                ItemFixtures.shippedRows(SubroutineImplicits.DATA_PATH), new JSONArray());

        assertEquals(List.of(), violations.stream().filter(violation -> violation.severity() == Severity.ERROR).toList());
        assertTrue(violations.stream().noneMatch(violation -> violation.message().contains("_SUBROUTINE_IMPLICIT")), violations.toString());
    }

    @Test
    void theShippedHullItemCorruptionPoolIsCheckedAndCountsAsUsed() throws Exception {
        List<Violation> violations = ItemDataIntegrity.checkAll(ItemFixtures.shippedRows(ItemModifiers.DATA_PATH),
                ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH), ItemFixtures.shippedRows(ItemDefinitions.HULL_ITEMS_PATH),
                ItemFixtures.shippedRows(ItemImplicits.DATA_PATH), new JSONArray(), new JSONArray(), new JSONArray(), new JSONArray(),
                ItemFixtures.shippedRows(ItemImplicits.CORRUPTION_DATA_PATH));

        assertEquals(List.of(), violations.stream().filter(violation -> violation.severity() == Severity.ERROR).toList());
        assertTrue(violations.stream().noneMatch(violation -> violation.message().contains("_CORRUPTION_IMPLICIT")), violations.toString());
    }

    @Test
    void theSubroutineCorruptionPoolOnlyTakesSubroutineImplicits() throws Exception {
        JSONArray pool = new JSONArray().put(new JSONObject().put(SubroutineImplicits.IMPLICIT_COLUMN, "@HYBRID_ARMOR_HULL"));

        List<String> errors = ItemDataIntegrity.checkAll(validModifiers(), validSubroutines(), validHullItems(), validImplicits(),
                new JSONArray(), new JSONArray(), new JSONArray(), pool, new JSONArray()).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();

        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("HYBRID_ARMOR_HULL"), errors.toString());
    }

    @Test
    void theHullItemCorruptionPoolOnlyTakesHullItemImplicits() throws Exception {
        JSONArray pool = new JSONArray().put(new JSONObject().put(SubroutineImplicits.IMPLICIT_COLUMN, "@ARMOR_PERCENT"));

        List<String> errors = ItemDataIntegrity.checkAll(validModifiers(), validSubroutines(), validHullItems(), validImplicits(),
                new JSONArray(), new JSONArray(), new JSONArray(), new JSONArray(), pool).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();

        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("ARMOR_PERCENT") && errors.get(0).contains("hull item corruption pool"), errors.toString());
    }

    @Test
    void validDataPasses() throws Exception {
        assertEquals(List.of(), check(validModifiers(), validSubroutines(), validHullItems(), validImplicits()));
    }

    @Test
    void modifierIdsAreUniqueWellFormedAndFollowTheNamingConvention() throws Exception {
        JSONArray modifiers = validModifiers()
                .put(modifier("HULL_MULT", "prefix", "HULL_MULT:1:2", "Again"))
                .put(modifier("bad id", "prefix", "HULL_MULT:1:2", "Bad"))
                .put(modifier("STURDY", "prefix", "HULL_MULT:1:2", "Sturdy"))
                .put(modifier("ARMOR_AND_HULL", "prefix", "ARMOR_PERCENT:1:2; HULL_PERCENT:1:2", "Plated"));

        List<String> errors = errors(modifiers, validSubroutines(), validImplicits());

        assertTrue(errors.contains("id HULL_MULT is already used on line 2"), errors.toString());
        assertTrue(errors.contains("id \"bad id\" must be letters, digits and underscores"), errors.toString());
        assertTrue(errors.contains("STURDY has one effect, so its id must be HULL_MULT or HULL_MULT_<QUALIFIER>"), errors.toString());
        assertTrue(errors.contains("ARMOR_AND_HULL has several effects, so its id must start with HYBRID_"), errors.toString());
    }

    @Test
    void modifierLinesMustBeCurrentSkillEffectsWithoutWeights() throws Exception {
        JSONArray modifiers = validModifiers()
                .put(modifier("NOT_AN_EFFECT", "prefix", "NOT_AN_EFFECT:1:2", "Nope"))
                .put(modifier("HULL_PERCENT", "prefix", "HULL_PERCENT:1:2:50", "Weighted"))
                .put(modifier("ARMOR_FLAT", "implicit", "ARMOR_FLAT:1/2/3/4", ""))
                .put(modifier("TOP_SPEED_PERCENT", "sideways", "TOP_SPEED_PERCENT:1:2", "Swift"));

        List<String> errors = errors(modifiers, validSubroutines(), validImplicits());

        assertTrue(errors.contains("NOT_AN_EFFECT uses NOT_AN_EFFECT, which is not a skill effect"), errors.toString());
        assertTrue(errors.contains("HULL_PERCENT line \"HULL_PERCENT:1:2:50\" has a weight; weights belong on the pool entry"), errors.toString());
        assertTrue(errors.contains("ARMOR_FLAT is an implicit, so ARMOR_FLAT must roll a min:max range"), errors.toString());
        assertTrue(errors.contains("TOP_SPEED_PERCENT has slot \"sideways\"; expected implicit, prefix or suffix"), errors.toString());
    }

    @Test
    void everyModifierNamesTheItemTypesItRollsOn() throws Exception {
        JSONArray modifiers = validModifiers()
                .put(ItemFixtures.modifierRow("HULL_PERCENT", "prefix", "", "HULL_PERCENT:1:2", "Sturdy"))
                .put(ItemFixtures.modifierRow("TOP_SPEED_PERCENT", "prefix", "subroutine;star_map", "TOP_SPEED_PERCENT:1:2", "Swift"));

        List<String> errors = errors(modifiers, validSubroutines(), validImplicits());

        assertTrue(errors.contains("HULL_PERCENT needs at least one item type in item_types"), errors.toString());
        assertTrue(errors.contains("TOP_SPEED_PERCENT lists item type \"star_map\"; expected subroutine, unique_subroutine, doctrine, hull_item, star_chart, "
                        + "wormhole_anchor"), errors.toString());
    }

    @Test
    void subroutinesOnlyRollImplicitsAndAnItemOnlyRollsModifiersScopedToItsType() throws Exception {
        JSONArray modifiers = validModifiers()
                .put(ItemFixtures.modifierRow("VENT_RATE_PERCENT", "prefix", "subroutine", "VENT_RATE_PERCENT:1:2", "Venting"))
                .put(ItemFixtures.modifierRow("HULL_PERCENT_IMPLICIT", "implicit", "subroutine", "HULL_PERCENT:1:2", ""));
        JSONArray hullItems = validHullItems().put(hullItem("vent", "reactor", "@VENT_RATE_PERCENT", ""));
        JSONArray implicits = validImplicits().put(implicit("reactor", "@HULL_PERCENT_IMPLICIT"));

        List<String> errors = errors(modifiers, validSubroutines(), hullItems, implicits);

        assertEquals(List.of("VENT_RATE_PERCENT rolls on subroutine, but subroutines only roll corrupted implicits",
                "vent is a hull_item, but VENT_RATE_PERCENT only rolls on subroutine",
                "reactor is a hull_item, but HULL_PERCENT_IMPLICIT only rolls on subroutine"), errors);
    }

    @Test
    void subroutinesNeedAKnownKindAndListNoPrefixesOrSuffixes() throws Exception {
        JSONArray subroutines = validSubroutines()
                .put(subroutine("doctrine", " doctrine "))
                .put(subroutine("expansion", "EXPANSION"))
                .put(subroutine("kindless", ""))
                .put(subroutine("pooled", "MODSPEC_CACHE").put("prefixes", "@HULL_MULT").put("suffixes", "@ARMOR_PERCENT"));

        List<String> errors = errors(validModifiers(), subroutines, validImplicits());

        assertEquals(List.of("expansion has kind \"EXPANSION\"; expected one of MODSPEC_CACHE, DOCTRINE, BYPASS, UNIQUE",
                "kindless has kind \"\"; expected one of MODSPEC_CACHE, DOCTRINE, BYPASS, UNIQUE",
                "pooled lists prefixes or suffixes, but subroutines do not roll them"), errors);
    }

    private static JSONObject uniqueSubroutine(String id, String lines, String source) throws JSONException {
        return subroutine(id, "UNIQUE").put("unique", "true").put(ItemDefinition.UNIQUE_LINES_COLUMN, lines)
                .put(ItemDefinition.UNIQUE_SOURCE_COLUMN, source);
    }

    private static JSONArray modifiersWithUniqueLines() throws JSONException {
        return validModifiers()
                .put(ItemFixtures.modifierRow("TOP_SPEED_PERCENT_RELIC", "prefix", "unique_subroutine", "TOP_SPEED_PERCENT:5:8", ""))
                .put(ItemFixtures.modifierRow("VENT_RATE_PERCENT_RELIC", "suffix", "unique_subroutine", "VENT_RATE_PERCENT:5:8", ""));
    }

    @Test
    void aUniqueSubroutineListsItsLinesAndNamesASource() throws Exception {
        JSONArray subroutines = validSubroutines()
                .put(uniqueSubroutine("relic", "@TOP_SPEED_PERCENT_RELIC; HULL_PERCENT:3:3", "faction:pirates; site:station_research"));

        List<Violation> violations = check(modifiersWithUniqueLines(), subroutines, validHullItems(), validImplicits());

        assertEquals(List.of("VENT_RATE_PERCENT_RELIC rolls on unique_subroutine, so its slot must be prefix"),
                violations.stream().filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList());
    }

    @Test
    void uniqueSubroutineLinesAreCheckedAndOnlyUniqueSubroutinesHaveThem() throws Exception {
        JSONArray subroutines = validSubroutines()
                .put(subroutine("flagless", "UNIQUE").put(ItemDefinition.UNIQUE_LINES_COLUMN, "@TOP_SPEED_PERCENT_RELIC")
                        .put(ItemDefinition.UNIQUE_SOURCE_COLUMN, "faction:pirates"))
                .put(uniqueSubroutine("empty", "", "faction:pirates"))
                .put(uniqueSubroutine("sourceless", "@TOP_SPEED_PERCENT_RELIC", ""))
                .put(uniqueSubroutine("misplaced", "@TOP_SPEED_PERCENT_RELIC", "colony:jangala"))
                .put(uniqueSubroutine("weighted", "@TOP_SPEED_PERCENT_RELIC:5; HULL_PERCENT:1:2:3", "site:wreck"))
                .put(uniqueSubroutine("hull_only", "@HULL_MULT", "site:wreck"))
                .put(uniqueSubroutine("doubled", "@TOP_SPEED_PERCENT_RELIC; TOP_SPEED_PERCENT:1:1", "site:wreck"))
                .put(subroutine("volume_lines", "MODSPEC_CACHE").put(ItemDefinition.UNIQUE_LINES_COLUMN, "HULL_PERCENT:1:2"));

        List<String> errors = errors(modifiersWithUniqueLines(), subroutines, validImplicits());

        assertTrue(errors.contains("flagless is a UNIQUE subroutine, so unique must be true"), errors.toString());
        assertTrue(errors.contains("empty is a UNIQUE subroutine but lists no lines"), errors.toString());
        assertTrue(errors.contains("sourceless is a UNIQUE subroutine but names no source; add faction:FACTION_ID or site:SALVAGE_SITE_ID"),
                errors.toString());
        assertTrue(errors.contains("misplaced: source \"colony:jangala\" is not faction:FACTION_ID or site:SALVAGE_SITE_ID"), errors.toString());
        assertTrue(errors.contains("weighted line \"@TOP_SPEED_PERCENT_RELIC:5\" has a weight or tier range, but every unique subroutine line always rolls"),
                errors.toString());
        assertTrue(errors.contains("weighted line \"HULL_PERCENT:1:2:3\" has a weight, but every unique subroutine line always rolls"), errors.toString());
        assertTrue(errors.contains("hull_only is a unique_subroutine, but HULL_MULT only rolls on hull_item"), errors.toString());
        assertTrue(errors.contains("doubled lists TOP_SPEED_PERCENT twice, as TOP_SPEED_PERCENT_RELIC and TOP_SPEED_PERCENT"), errors.toString());
        assertTrue(errors.contains("volume_lines lists lines, but only UNIQUE subroutines have them"), errors.toString());
    }

    @Test
    void aSocketLimitIsBlankOrAWholeNumberOfZeroOrMore() throws Exception {
        JSONArray subroutines = validSubroutines()
                .put(uniqueSubroutine("once", "@TOP_SPEED_PERCENT_RELIC", "site:wreck").put(ItemDefinition.SOCKET_LIMIT_COLUMN, "1"))
                .put(uniqueSubroutine("unlimited", "@TOP_SPEED_PERCENT_RELIC", "site:wreck").put(ItemDefinition.SOCKET_LIMIT_COLUMN, "0"))
                .put(uniqueSubroutine("negative", "@TOP_SPEED_PERCENT_RELIC", "site:wreck").put(ItemDefinition.SOCKET_LIMIT_COLUMN, "-1"))
                .put(uniqueSubroutine("fractional", "@TOP_SPEED_PERCENT_RELIC", "site:wreck").put(ItemDefinition.SOCKET_LIMIT_COLUMN, "1.5"));

        List<String> errors = errors(modifiersWithUniqueLines(), subroutines, validImplicits()).stream()
                .filter(error -> error.contains("socket limit")).toList();

        assertEquals(List.of("negative has socket limit \"-1\"; expected a whole number of 0 or more, or blank for no limit",
                "fractional has socket limit \"1.5\"; expected a whole number of 0 or more, or blank for no limit"), errors);
    }

    @Test
    void hullItemsNameARealSectionAndIdsAreUniqueAcrossBothFiles() throws Exception {
        JSONArray hullItems = validHullItems()
                .put(hullItem("drifting", "cargo_bay", "@HULL_MULT", ""))
                .put(hullItem("chip", "bridge", "@HULL_MULT", ""));

        List<String> errors = errors(validModifiers(), validSubroutines(), hullItems, validImplicits());

        assertEquals(List.of("drifting has section \"cargo_bay\", which is not a ship section",
                "id chip is already used in " + ItemDefinitions.SUBROUTINES_PATH), errors);
    }

    @Test
    void poolEntriesMustReferenceExistingModifiersOfTheirSlot() throws Exception {
        JSONArray hullItems = validHullItems()
                .put(poolItem("inline", "HULL_MULT:4:6", "", false))
                .put(poolItem("missing", "@NO_SUCH_MODIFIER", "", false))
                .put(poolItem("wrong_side", "@ARMOR_PERCENT", "", false))
                .put(poolItem("twice", "@HULL_MULT; @HULL_MULT", "", false))
                .put(poolItem("heavy", "@HULL_MULT:0", "", false))
                .put(poolItem("plate", "@HULL_MULT", "", false));

        List<String> errors = errors(validModifiers(), validSubroutines(), hullItems, validImplicits());

        assertTrue(errors.contains("inline lists \"HULL_MULT:4:6\" inline; reference a modifier as @ID or @ID:weight"), errors.toString());
        assertTrue(errors.contains("missing references NO_SUCH_MODIFIER, which is not in " + ItemModifiers.DATA_PATH), errors.toString());
        assertTrue(errors.contains("wrong_side lists ARMOR_PERCENT in its prefix slot, but it is a suffix"), errors.toString());
        assertTrue(errors.contains("twice lists HULL_MULT more than once"), errors.toString());
        assertTrue(errors.contains("heavy entry \"@HULL_MULT:0\" needs a weight above zero"), errors.toString());
        assertTrue(errors.contains("id plate is already used on line 2"), errors.toString());
    }

    @Test
    void modifiersOnItemsThatAreNotUniqueNeedANameButUniquesDoNot() throws Exception {
        JSONArray modifiers = validModifiers()
                .put(modifier("HULL_PERCENT", "prefix", "HULL_PERCENT:1:2", ""))
                .put(modifier("TOP_SPEED_PERCENT", "prefix", "TOP_SPEED_PERCENT:1:2", ""));
        JSONArray hullItems = validHullItems()
                .put(poolItem("common", "@HULL_PERCENT", "", false))
                .put(poolItem("relic", "@TOP_SPEED_PERCENT", "", true));

        List<String> errors = errors(modifiers, validSubroutines(), hullItems, validImplicits());

        assertEquals(List.of("HULL_PERCENT rolls on items that are not unique, so it needs a name for their common names"), errors);
    }

    @Test
    void implicitsReferenceImplicitModifiersOnSectionsOnly() throws Exception {
        JSONArray implicits = new JSONArray()
                .put(implicit("subroutine", "@HYBRID_ARMOR_HULL"))
                .put(implicit("reactor", "@HULL_MULT"))
                .put(implicit("engine_room", "TOP_SPEED_PERCENT:5:10"))
                .put(implicit("bridge", "@HYBRID_ARMOR_HULL:5"))
                .put(implicit("bridge", ""));

        List<String> errors = errors(validModifiers(), validSubroutines(), implicits);

        assertTrue(errors.contains("\"subroutine\" is not a ship section"), errors.toString());
        assertTrue(errors.contains("reactor lists HULL_MULT in its implicit slot, but it is a prefix"), errors.toString());
        assertTrue(errors.contains("engine_room lists \"TOP_SPEED_PERCENT:5:10\" inline; reference a modifier as @ID"), errors.toString());
        assertTrue(errors.contains("bridge entry \"@HYBRID_ARMOR_HULL:5\" has a weight, but implicits always roll"), errors.toString());
        assertTrue(errors.contains("bridge is already listed on line 5"), errors.toString());
    }

    @Test
    void unusedModifiersAreOnlyWarnings() throws Exception {
        JSONArray modifiers = validModifiers().put(modifier("HULL_PERCENT", "prefix", "HULL_PERCENT:1:2", "Sturdy"));

        List<Violation> violations = check(modifiers, validSubroutines(), validHullItems(), validImplicits());

        assertEquals(List.of(new Violation(Severity.WARNING, ItemModifiers.DATA_PATH, 5, "HULL_PERCENT is not used by any item or implicit")),
                violations);
    }
}
