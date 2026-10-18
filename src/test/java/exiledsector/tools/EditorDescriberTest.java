package exiledsector.tools;

import exiledsector.i18n.HeadlessGameText;
import exiledsector.i18n.LocaleChain;
import exiledsector.i18n.RealCatalogue;
import exiledsector.i18n.Translation;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorDescriberTest {

    private static HeadlessGameText headlessGameText;
    private static JSONObject savedTypes;

    @BeforeAll
    static void setUp() throws Exception {
        headlessGameText = HeadlessGameText.install(() -> true);
        RealSkillData.load();
        savedTypes = RealSkillData.readJson(RealSkillData.projectRoot().resolve(RealSkillData.TYPES_FILE));
    }

    @AfterAll
    static void tearDown() {
        RealSkillData.clear();
        headlessGameText.close();
    }

    private static JSONObject request(JSONObject types, String typeId, String hullSize) throws Exception {
        return new JSONObject().put("types", types).put("typeId", typeId).put("hullSize", hullSize == null ? JSONObject.NULL : hullSize);
    }

    @Test
    void modifierLinesAreDescribedAsTheirRollRangeOrHullSizeValues() throws Exception {
        JSONArray lines = new JSONArray()
                .put(new JSONObject().put("effect", "MAX_COMBAT_READINESS_PERCENT").put("min", 3).put("max", 5))
                .put(new JSONObject().put("effect", "ELECTRONIC_WARFARE").put("hullValues", new JSONArray().put(1).put(1).put(2).put(2)))
                .put(new JSONObject().put("effect", "NOT_AN_EFFECT").put("min", 1).put("max", 2));

        JSONArray described = new JSONObject(new EditorDescriber().respond(new JSONObject().put("modifierLines", lines).toString()))
                .getJSONArray("lines");

        assertEquals(3, described.length());
        assertTrue(described.getString(0).contains("3") && described.getString(0).contains("5"), described.getString(0));
        assertTrue(described.getString(1).contains("1/1/2/2"), described.getString(1));
        assertTrue(described.isNull(2));
    }

    @Test
    void aSavedTypeIsDescribedWithItsTitleAndHighlightedEffects() throws Exception {
        JSONObject response = new JSONObject(new EditorDescriber().respond(request(savedTypes, "flux_capacity", null).toString()));

        assertEquals("Flux Capacity", response.getString("title"));
        String firstLine = response.getJSONArray("effects").getString(0);
        assertTrue(firstLine.contains("<good>"), firstLine);
    }

    @Test
    void anUnsavedEditIsDescribedStraightAway() throws Exception {
        JSONObject edited = new JSONObject(savedTypes.toString());
        JSONArray typeArray = edited.getJSONArray("skillTypes");
        for (int i = 0; i < typeArray.length(); i++) {
            JSONObject type = typeArray.getJSONObject(i);
            if ("flux_capacity".equals(type.getString("id"))) {
                type.getJSONArray("effects").getJSONObject(0).put("magnitude", 37);
                type.put("name", "Edited Capacity");
            }
        }
        JSONObject response = new JSONObject(new EditorDescriber().respond(request(edited, "flux_capacity", "FRIGATE").toString()));

        assertEquals("Edited Capacity", response.getString("title"));
        assertTrue(response.getJSONArray("effects").getString(0).contains("37"), response.toString());
    }

    @Test
    void unsavedStringsAreUsedAndTheKeysBehindTheTooltipAreReported() throws Exception {
        JSONObject strings = new JSONObject(RealCatalogue.entries(LocaleChain.ENGLISH));
        strings.put("stat.fluxCapacity", "reworded capacity");
        JSONObject response = new JSONObject(new EditorDescriber().respond(request(savedTypes, "flux_capacity", "FRIGATE").put("strings", strings).toString()));

        assertTrue(response.getJSONArray("effects").getString(0).contains("reworded capacity"), response.toString());
        String keys = response.getJSONArray("keys").toString();
        assertTrue(keys.contains("\"stat.fluxCapacity\""), keys);
        assertEquals("flux capacity", Translation.text("stat.fluxCapacity"));
    }

    @Test
    void unknownTypesAndBrokenRequestsReturnAnError() throws Exception {
        EditorDescriber describer = new EditorDescriber();

        assertTrue(new JSONObject(describer.respond(request(savedTypes, "no_such_type", null).toString())).has("error"));
        assertTrue(new JSONObject(describer.respond("not json")).has("error"));
    }
}
