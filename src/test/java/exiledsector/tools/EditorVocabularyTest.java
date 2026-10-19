package exiledsector.tools;

import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.unlock.BlueprintCategory;
import exiledsector.skills.unlock.UnlockConditionType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorVocabularyTest {

    private static final String UPDATE_PROPERTY = "exiledsector.updateGolden";

    @Test
    void theCommittedManifestMatchesTheJavaRegistries() throws IOException {
        String expected = EditorVocabulary.generate();
        Path manifest = RealSkillData.projectRoot().resolve(EditorVocabulary.FILE);
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            Files.writeString(manifest, expected, StandardCharsets.UTF_8);
        }
        String regenerate = " - regenerate it with: mvn -q test -Dtest=" + EditorVocabularyTest.class.getSimpleName() + " -D" + UPDATE_PROPERTY
                + "=true";
        assertTrue(Files.isRegularFile(manifest), "Missing " + manifest + regenerate);
        String committed = Files.readString(manifest, StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(expected, committed, EditorVocabulary.FILE + " is out of date with the effect, tag and item registries" + regenerate);
    }

    @Test
    void theManifestIsValidJsonWithEveryListTheEditorReads() throws JSONException {
        JSONObject manifest = new JSONObject(EditorVocabulary.generate());
        for (String key : new String[]{"effects", "tiers", "unlockConditionTypes", "parameterlessUnlockConditionTypes", "blueprintCategories",
                "itemTypes", "itemKinds", "subroutineKinds", "modifierScopes", "doctrineBehaviours", "radiusSizes", "uniqueSourceKinds", "itemUnlocks", "chartTierRange", "chartEffects", "chartSystemTypes", "chartPoiKinds", "chartDerelictTypes",
                "chartTierDefaults", "chartDropSources", "chartLootDropTypes", "chartLootScopes", "chartColonyItemGroups"}) {
            assertTrue(manifest.getJSONArray(key).length() > 0, key);
        }
        JSONObject tags = manifest.getJSONObject("tags");
        for (String group : new String[]{"theme", "region", "requirement", "gate"}) {
            assertTrue(tags.getJSONArray(group).length() > 0, group);
        }
        JSONArray effects = manifest.getJSONArray("effects");
        assertTrue(effects.getJSONObject(0).has("name"));
    }

    @Test
    void enumValuesUseTheCamelCaseTheSkillTypeLoaderReads() {
        assertEquals("characterStat", EditorVocabulary.camelCase(UnlockConditionType.CHARACTER_STAT.name()));
        assertEquals("minShipLevel", EditorVocabulary.camelCase(UnlockConditionType.MIN_SHIP_LEVEL.name()));
        assertEquals("hullmod", EditorVocabulary.camelCase(BlueprintCategory.HULLMOD.name()));
    }

    @Test
    void theEditorLoadsTheManifestThroughTheServer() throws IOException {
        Path root = RealSkillData.projectRoot();
        String server = Files.readString(root.resolve("tools/editor/server/routes/data.ts"), StandardCharsets.UTF_8);
        String editor = Files.readString(root.resolve("tools/editor/src/api/vocabulary.ts"), StandardCharsets.UTF_8);
        assertTrue(server.contains("'" + EditorVocabulary.FILE.getFileName() + "'"), "the editor server no longer serves " + EditorVocabulary.FILE);
        assertTrue(server.contains("'/api/vocabulary'"), "the editor server no longer serves /api/vocabulary");
        assertTrue(editor.contains("'/api/vocabulary'"), "the editor no longer loads /api/vocabulary");
    }
}
