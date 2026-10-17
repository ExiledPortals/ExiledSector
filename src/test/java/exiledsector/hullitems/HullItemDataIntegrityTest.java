package exiledsector.hullitems;

import exiledsector.hullitems.HullItemDataIntegrity.Severity;
import exiledsector.hullitems.HullItemDataIntegrity.Violation;
import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemDataIntegrityTest {

    private static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    private static JSONObject modifier(String id, String slot, String lines, String name) throws JSONException {
        return HullItemFixtures.modifierRow(id, slot, lines, name);
    }

    private static JSONObject definition(String id, String prefixes, String suffixes, boolean unique) throws JSONException {
        return HullItemFixtures.row(id, "subroutine", prefixes, suffixes).put("unique", unique ? "TRUE" : "");
    }

    private static JSONObject implicit(String type, String implicits) throws JSONException {
        return new JSONObject().put("type", type).put("implicits", implicits);
    }

    private static List<String> errors(JSONArray modifiers, JSONArray definitions, JSONArray implicits) throws JSONException {
        return HullItemDataIntegrity.check(modifiers, definitions, implicits).stream()
                .filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();
    }

    private static JSONArray validModifiers() throws JSONException {
        return new JSONArray()
                .put(modifier("HULL_MULT", "prefix", "HULL_MULT:4:6", "Reinforced"))
                .put(modifier("ARMOR_PERCENT", "suffix", "ARMOR_PERCENT:6:9", "Plating"))
                .put(modifier("HYBRID_ARMOR_HULL", "implicit", "ARMOR_PERCENT:5:10; HULL_PERCENT:3:6", ""));
    }

    private static JSONArray validDefinitions() throws JSONException {
        return new JSONArray().put(definition("chip", "@HULL_MULT:10", "@ARMOR_PERCENT", false));
    }

    private static JSONArray validImplicits() throws JSONException {
        return new JSONArray().put(implicit("armor_plating", "@HYBRID_ARMOR_HULL"));
    }

    @Test
    void theShippedHullItemDataHasNoIntegrityErrors() throws Exception {
        List<Violation> violations = HullItemDataIntegrity.check(csvRows(HullItemModifiers.DATA_PATH), csvRows(HullItemDefinitions.DATA_PATH),
                csvRows(HullItemImplicits.DATA_PATH));

        assertEquals(List.of(), violations.stream().filter(violation -> violation.severity() == Severity.ERROR).toList());
    }

    @Test
    void validDataPasses() throws Exception {
        assertEquals(List.of(), HullItemDataIntegrity.check(validModifiers(), validDefinitions(), validImplicits()));
    }

    @Test
    void modifierIdsAreUniqueWellFormedAndFollowTheNamingConvention() throws Exception {
        JSONArray modifiers = validModifiers()
                .put(modifier("HULL_MULT", "prefix", "HULL_MULT:1:2", "Again"))
                .put(modifier("bad id", "prefix", "HULL_MULT:1:2", "Bad"))
                .put(modifier("STURDY", "prefix", "HULL_MULT:1:2", "Sturdy"))
                .put(modifier("ARMOR_AND_HULL", "prefix", "ARMOR_PERCENT:1:2; HULL_PERCENT:1:2", "Plated"));

        List<String> errors = errors(modifiers, validDefinitions(), validImplicits());

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

        List<String> errors = errors(modifiers, validDefinitions(), validImplicits());

        assertTrue(errors.contains("NOT_AN_EFFECT uses NOT_AN_EFFECT, which is not a skill effect"), errors.toString());
        assertTrue(errors.contains("HULL_PERCENT line \"HULL_PERCENT:1:2:50\" has a weight; weights belong on the pool entry"), errors.toString());
        assertTrue(errors.contains("ARMOR_FLAT is an implicit, so ARMOR_FLAT must roll a min:max range"), errors.toString());
        assertTrue(errors.contains("TOP_SPEED_PERCENT has slot \"sideways\"; expected implicit, prefix or suffix"), errors.toString());
    }

    @Test
    void poolEntriesMustReferenceExistingModifiersOfTheirSlot() throws Exception {
        JSONArray definitions = validDefinitions()
                .put(definition("inline", "HULL_MULT:4:6", "", false))
                .put(definition("missing", "@NO_SUCH_MODIFIER", "", false))
                .put(definition("wrong_side", "@ARMOR_PERCENT", "", false))
                .put(definition("twice", "@HULL_MULT; @HULL_MULT", "", false))
                .put(definition("heavy", "@HULL_MULT:0", "", false))
                .put(definition("chip", "@HULL_MULT", "", false));

        List<String> errors = errors(validModifiers(), definitions, validImplicits());

        assertTrue(errors.contains("inline lists \"HULL_MULT:4:6\" inline; reference a modifier as @ID or @ID:weight"), errors.toString());
        assertTrue(errors.contains("missing references NO_SUCH_MODIFIER, which is not in " + HullItemModifiers.DATA_PATH), errors.toString());
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
        JSONArray definitions = validDefinitions()
                .put(definition("common", "@HULL_PERCENT", "", false))
                .put(definition("relic", "@TOP_SPEED_PERCENT", "", true));

        List<String> errors = errors(modifiers, definitions, validImplicits());

        assertEquals(List.of("HULL_PERCENT rolls on items that are not unique, so it needs a name for their common names"), errors);
    }

    @Test
    void implicitsReferenceImplicitModifiersOnSectionTypesOnly() throws Exception {
        JSONArray implicits = new JSONArray()
                .put(implicit("subroutine", "@HYBRID_ARMOR_HULL"))
                .put(implicit("reactor", "@HULL_MULT"))
                .put(implicit("engine_room", "TOP_SPEED_PERCENT:5:10"))
                .put(implicit("bridge", "@HYBRID_ARMOR_HULL:5"))
                .put(implicit("bridge", ""));

        List<String> errors = errors(validModifiers(), validDefinitions(), implicits);

        assertTrue(errors.contains("\"subroutine\" is not a hull item type that fits a ship section"), errors.toString());
        assertTrue(errors.contains("reactor lists HULL_MULT in its implicit slot, but it is a prefix"), errors.toString());
        assertTrue(errors.contains("engine_room lists \"TOP_SPEED_PERCENT:5:10\" inline; reference a modifier as @ID"), errors.toString());
        assertTrue(errors.contains("bridge entry \"@HYBRID_ARMOR_HULL:5\" has a weight, but implicits always roll"), errors.toString());
        assertTrue(errors.contains("bridge is already listed on line 5"), errors.toString());
    }

    @Test
    void unusedModifiersAreOnlyWarnings() throws Exception {
        JSONArray modifiers = validModifiers().put(modifier("HULL_PERCENT", "prefix", "HULL_PERCENT:1:2", "Sturdy"));

        List<Violation> violations = HullItemDataIntegrity.check(modifiers, validDefinitions(), validImplicits());

        assertEquals(List.of(new Violation(Severity.WARNING, HullItemModifiers.DATA_PATH, 5, "HULL_PERCENT is not used by any hull item or implicit")),
                violations);
    }
}
