package exiledsector.tools;

import exiledsector.i18n.HeadlessGameText;
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
    void unknownTypesAndBrokenRequestsReturnAnError() throws Exception {
        EditorDescriber describer = new EditorDescriber();

        assertTrue(new JSONObject(describer.respond(request(savedTypes, "no_such_type", null).toString())).has("error"));
        assertTrue(new JSONObject(describer.respond("not json")).has("error"));
    }
}
