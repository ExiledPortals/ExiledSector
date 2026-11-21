package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HarnessSuiteTest {

    private static final String SUITE = """
            {"groups": [
              {"id": "excavation_sites", "tags": ["dense_nebula"], "setup": [{"step": "mark", "text": "setup"}], "cases": [
                {"id": "open_shaft", "seed": 7, "steps": [{"step": "openChart"}]},
                {"id": "collapsed_shaft", "tags": ["raid"], "steps": [{"step": "mark", "text": "raid"}]}
              ]},
              {"id": "abyssal_storm", "requiresMods": ["assortment_of_things"], "cases": [
                {"id": "base", "steps": [{"step": "mark", "text": "storm"}]}
              ]}
            ]}""";

    private final Map<String, String> commonFiles = new LinkedHashMap<>();
    private MockedStatic<Global> global;

    @BeforeEach
    void mockTheGame() throws IOException {
        SettingsAPI settings = mock(SettingsAPI.class);
        doAnswer(call -> commonFiles.put(call.getArgument(0), call.getArgument(1))).when(settings).writeTextFileToCommon(anyString(), anyString());
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getListenerManager()).thenReturn(mock(ListenerManagerAPI.class));
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getSettings).thenReturn(settings);
        global.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void releaseTheGame() {
        global.close();
    }

    private static List<String> names(HarnessScenario scenario) {
        return scenario.steps().stream().map(HarnessStep::name).toList();
    }

    @Test
    void everyCaseRunsBetweenItsStartAndTeardownWithTheGroupSetupFirst() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse(SUITE, SuiteCase.Filter.ALL, modId -> true);

        assertEquals(List.of("captureBaseline",
                "case excavation_sites/open_shaft", "ignoreErrorsSoFar", "mark setup", "openChart seed 7", "expectNoErrors", "teardown excavation_sites/open_shaft",
                "case excavation_sites/collapsed_shaft", "ignoreErrorsSoFar", "mark setup", "mark raid", "expectNoErrors", "teardown excavation_sites/collapsed_shaft",
                "case abyssal_storm/base", "ignoreErrorsSoFar", "mark storm", "expectNoErrors", "teardown abyssal_storm/base"), names(scenario));
        assertTrue(scenario.skippedCases().isEmpty());
    }

    @Test
    void aCaseWithoutASeedGetsOneFromItsIdAndKeepsItsGroupsTags() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse(SUITE, new SuiteCase.Filter(null, "excavation_sites/collapsed_shaft", null, null), modId -> true);

        SuiteCase suiteCase = ((SuiteSteps.StartCaseStep) scenario.steps().get(1)).suiteCase();
        assertEquals(SuiteCase.defaultSeed("excavation_sites/collapsed_shaft"), suiteCase.seed());
        assertEquals(List.of("excavation_sites", "dense_nebula", "raid"), suiteCase.tags());
    }

    @Test
    void filtersPickAGroupATagOrTheCasesAfterOne() throws JSONException {
        assertEquals(13, HarnessScenario.parse(SUITE, new SuiteCase.Filter("excavation_sites", null, null, null), modId -> true).steps().size());
        assertEquals(13, HarnessScenario.parse(SUITE, new SuiteCase.Filter(null, null, "dense_nebula", null), modId -> true).steps().size());
        assertEquals(List.of("captureBaseline", "case excavation_sites/collapsed_shaft", "ignoreErrorsSoFar", "mark setup", "mark raid", "expectNoErrors",
                        "teardown excavation_sites/collapsed_shaft"),
                names(HarnessScenario.parse(SUITE, new SuiteCase.Filter(null, null, "raid", null), modId -> true)));
        assertEquals("case abyssal_storm/base",
                HarnessScenario.parse(SUITE, new SuiteCase.Filter(null, null, null, "excavation_sites/collapsed_shaft"), modId -> true).steps().get(1).name());
    }

    @Test
    void aGroupNeedingAMissingModIsSkipped() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse(SUITE, SuiteCase.Filter.ALL, modId -> !modId.equals("assortment_of_things"));

        assertFalse(names(scenario).contains("case abyssal_storm/base"));
        assertEquals(List.of("abyssal_storm/base: needs [assortment_of_things]"), scenario.skippedCases());
    }

    @Test
    void aFailedStepSkipsToItsCasesTeardownAndAFailedTeardownStopsTheSession() throws Exception {
        List<String> ran = new ArrayList<>();
        SuiteSteps.Baseline neverCaptured = new SuiteSteps.Baseline();
        SuiteCase first = new SuiteCase("group/first", "group", List.of("group"), 1L);
        SuiteCase second = new SuiteCase("group/second", "group", List.of("group"), 2L);
        HarnessScenario scenario = new HarnessScenario(List.of(new SuiteSteps.StartCaseStep(first), recording("breaks", false, ran),
                recording("skipped", true, ran), new SuiteSteps.TeardownStep(first, neverCaptured), new SuiteSteps.StartCaseStep(second),
                recording("never", true, ran), new SuiteSteps.TeardownStep(second, neverCaptured)), false, null, List.of("other/case: needs [mod]"));
        HarnessLog log = new HarnessLog();
        HarnessRun run = new HarnessRun(scenario, log, new HarnessResults("save", log), () -> {
        });

        for (int frame = 0; frame < 10 && !run.isDone(); frame++) {
            run.advance(0f);
        }

        assertTrue(run.isDone());
        assertEquals(List.of("breaks"), ran);
        JSONObject results = new JSONObject(commonFiles.get(HarnessFiles.RESULTS));
        assertFalse(results.getBoolean("passed"));
        assertEquals("group/first", results.getString("abortedAfterCase"));
        assertEquals(List.of("group/second"), strings(results.getJSONArray("notRunCases")));
        assertEquals(List.of("other/case: needs [mod]"), strings(results.getJSONArray("skippedCases")));
        JSONObject firstCase = results.getJSONArray("cases").getJSONObject(0);
        assertEquals("group/first", firstCase.getString("id"));
        assertEquals("breaks", firstCase.getString("failedStep"));
        assertEquals(3, firstCase.getInt("steps"));
        assertEquals("teardown group/first", results.getJSONArray("steps").getJSONObject(2).getString("step"));
        assertEquals("group/first", results.getJSONArray("steps").getJSONObject(2).getString("case"));
    }

    private static HarnessStep recording(String stepName, boolean passes, List<String> ran) {
        return new HarnessStep() {
            @Override
            public String name() {
                return stepName;
            }

            @Override
            public StepOutcome advance(StepContext context) {
                ran.add(stepName);
                return passes ? StepOutcome.passed(stepName) : StepOutcome.failed(stepName);
            }
        };
    }

    private static List<String> strings(JSONArray array) throws JSONException {
        List<String> values = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            values.add(array.getString(index));
        }
        return values;
    }
}
