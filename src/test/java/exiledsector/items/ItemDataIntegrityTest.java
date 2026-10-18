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

    private static JSONObject subroutine(String id, String prefixes, String suffixes, boolean unique) throws JSONException {
        return ItemFixtures.row(id, "subroutine", prefixes, suffixes).put("unique", unique ? "TRUE" : "");
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
        return new JSONArray().put(subroutine("chip", "@HULL_MULT:10", "@ARMOR_PERCENT", false));
    }

    private static JSONArray validHullItems() throws JSONException {
        return new JSONArray().put(hullItem("plate", "armor_plating", "@HULL_MULT", ""));
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
        assertTrue(errors.contains("TOP_SPEED_PERCENT lists item type \"star_map\"; expected subroutine, hull_item"), errors.toString());
    }

    @Test
    void anItemOnlyRollsModifiersScopedToItsType() throws Exception {
        JSONArray modifiers = validModifiers()
                .put(ItemFixtures.modifierRow("VENT_RATE_PERCENT", "prefix", "hull_item", "VENT_RATE_PERCENT:1:2", "Venting"))
                .put(ItemFixtures.modifierRow("HULL_PERCENT_IMPLICIT", "implicit", "subroutine", "HULL_PERCENT:1:2", ""));
        JSONArray subroutines = validSubroutines().put(subroutine("vented_chip", "@VENT_RATE_PERCENT", "", false));
        JSONArray hullItems = validHullItems().put(hullItem("vent", "reactor", "@VENT_RATE_PERCENT", ""));
        JSONArray implicits = validImplicits().put(implicit("reactor", "@HULL_PERCENT_IMPLICIT"));

        List<String> errors = errors(modifiers, subroutines, hullItems, implicits);

        assertEquals(List.of("vented_chip is a subroutine, but VENT_RATE_PERCENT only rolls on hull_item",
                "reactor is a hull_item, but HULL_PERCENT_IMPLICIT only rolls on subroutine"), errors);
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
        JSONArray subroutines = validSubroutines()
                .put(subroutine("inline", "HULL_MULT:4:6", "", false))
                .put(subroutine("missing", "@NO_SUCH_MODIFIER", "", false))
                .put(subroutine("wrong_side", "@ARMOR_PERCENT", "", false))
                .put(subroutine("twice", "@HULL_MULT; @HULL_MULT", "", false))
                .put(subroutine("heavy", "@HULL_MULT:0", "", false))
                .put(subroutine("chip", "@HULL_MULT", "", false));

        List<String> errors = errors(validModifiers(), subroutines, validImplicits());

        assertTrue(errors.contains("inline lists \"HULL_MULT:4:6\" inline; reference a modifier as @ID or @ID:weight"), errors.toString());
        assertTrue(errors.contains("missing references NO_SUCH_MODIFIER, which is not in " + ItemModifiers.DATA_PATH), errors.toString());
        assertTrue(errors.contains("wrong_side lists ARMOR_PERCENT in its prefix slot, but it is a suffix"), errors.toString());
        assertTrue(errors.contains("twice lists HULL_MULT more than once"), errors.toString());
        assertTrue(errors.contains("heavy entry \"@HULL_MULT:0\" needs a weight above zero"), errors.toString());
        assertTrue(errors.contains("id chip is already used on line 2"), errors.toString());
    }

    @Test
    void modifiersOnItemsThatAreNotUniqueNeedANameButUniquesDoNot() throws Exception {
        JSONArray modifiers = validModifiers()
                .put(modifier("HULL_PERCENT", "prefix", "HULL_PERCENT:1:2", ""))
                .put(modifier("TOP_SPEED_PERCENT", "prefix", "TOP_SPEED_PERCENT:1:2", ""));
        JSONArray subroutines = validSubroutines()
                .put(subroutine("common", "@HULL_PERCENT", "", false))
                .put(subroutine("relic", "@TOP_SPEED_PERCENT", "", true));

        List<String> errors = errors(modifiers, subroutines, validImplicits());

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
