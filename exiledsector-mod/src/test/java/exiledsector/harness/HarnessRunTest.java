package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HarnessRunTest {

    private final Map<String, String> commonFiles = new LinkedHashMap<>();
    private final ListenerManagerAPI listeners = mock(ListenerManagerAPI.class);
    private MockedStatic<Global> global;

    @BeforeEach
    void mockTheGame() throws IOException {
        SettingsAPI settings = mock(SettingsAPI.class);
        doAnswer(call -> commonFiles.put(call.getArgument(0), call.getArgument(1))).when(settings).writeTextFileToCommon(anyString(), anyString());
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getListenerManager()).thenReturn(listeners);
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getSettings).thenReturn(settings);
        global.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void releaseTheGame() {
        global.close();
    }

    private static HarnessStep step(String stepName, int framesToFinish, boolean passes) {
        return new HarnessStep() {
            @Override
            public String name() {
                return stepName;
            }

            @Override
            public StepOutcome advance(StepContext context) {
                if (context.frame() + 1 < framesToFinish) {
                    return StepOutcome.RUNNING;
                }
                return passes ? StepOutcome.passed(stepName + " done") : StepOutcome.failed(stepName + " broke");
            }
        };
    }

    private JSONObject results() throws Exception {
        return new JSONObject(commonFiles.get(HarnessFiles.RESULTS));
    }

    @Test
    void stepsRunInOrderAndTheRunQuitsWhenTheLastOneFinishes() throws Exception {
        HarnessLog log = new HarnessLog();
        AtomicInteger quits = new AtomicInteger();
        HarnessScenario scenario = new HarnessScenario(List.of(step("first", 2, true), step("second", 1, true)), true);
        HarnessRun run = new HarnessRun(scenario, log, new HarnessResults("saves/save_test", log), quits::incrementAndGet);
        verify(listeners).addListener(any(ScreenCapture.class), Mockito.eq(true));

        run.advance(0f);
        assertFalse(commonFiles.containsKey(HarnessFiles.RESULTS));
        run.advance(0f);
        assertEquals(1, results().getJSONArray("steps").length());
        run.advance(0f);
        assertFalse(run.isDone());
        run.advance(0f);

        assertTrue(run.isDone());
        assertEquals(1, quits.get());
        JSONObject results = results();
        assertTrue(results.getBoolean("finished"));
        assertTrue(results.getBoolean("passed"));
        assertEquals("saves/save_test", results.getString("save"));
        assertEquals("second", results.getJSONArray("steps").getJSONObject(1).getString("step"));
        verify(listeners).removeListener(any(ScreenCapture.class));
    }

    @Test
    void aThrowingOrFailingStepFailsTheRunButLaterStepsStillRun() throws Exception {
        HarnessLog log = new HarnessLog();
        HarnessStep thrower = new HarnessStep() {
            @Override
            public String name() {
                return "throwing";
            }

            @Override
            public StepOutcome advance(StepContext context) {
                throw new IllegalStateException("no campaign UI");
            }
        };
        HarnessScenario scenario = new HarnessScenario(List.of(thrower, step("failing", 1, false), step("after", 1, true)), false);
        AtomicInteger quits = new AtomicInteger();
        HarnessRun run = new HarnessRun(scenario, log, new HarnessResults("save", log), quits::incrementAndGet);

        for (int frame = 0; frame < 4; frame++) {
            run.advance(0f);
        }

        JSONObject results = results();
        assertTrue(run.isDone());
        assertEquals(0, quits.get());
        assertFalse(results.getBoolean("passed"));
        assertEquals("threw java.lang.IllegalStateException: no campaign UI", results.getJSONArray("steps").getJSONObject(0).getString("detail"));
        assertFalse(results.getJSONArray("steps").getJSONObject(1).getBoolean("passed"));
        assertTrue(results.getJSONArray("steps").getJSONObject(2).getBoolean("passed"));
    }

    @Test
    void aStepThatOutlivesItsTimeoutFails() throws Exception {
        HarnessLog log = new HarnessLog();
        HarnessStep stuck = new HarnessStep() {
            @Override
            public String name() {
                return "stuck";
            }

            @Override
            public StepOutcome advance(StepContext context) {
                return new StepOutcome(false, false, "still waiting for the panel");
            }

            @Override
            public float timeoutSeconds() {
                return 0f;
            }
        };
        HarnessRun run = new HarnessRun(new HarnessScenario(List.of(stuck), true), log, new HarnessResults("save", log), () -> { });

        run.advance(0f);

        JSONObject stepResult = results().getJSONArray("steps").getJSONObject(0);
        assertFalse(stepResult.getBoolean("passed"));
        assertTrue(stepResult.getString("detail").endsWith(": still waiting for the panel"), stepResult.getString("detail"));
    }

    @Test
    void aFailedRunIsWrittenAsFinishedAndFailed() throws Exception {
        HarnessResults results = new HarnessResults("save", new HarnessLog());

        results.failRun("Could not load the save: missing campaign.xml");

        JSONObject written = results();
        assertTrue(written.getBoolean("finished"));
        assertFalse(written.getBoolean("passed"));
        assertEquals("Could not load the save: missing campaign.xml", written.getString("runFailure"));
    }

    @Test
    void screenshotsAreSplitIntoNumberedCommonFiles() throws Exception {
        String base64 = "a".repeat(HarnessFiles.MAX_CHARS_PER_FILE * 2 + 5);

        int parts = HarnessFiles.writeScreenshot("atlas", base64);

        assertEquals(3, parts);
        assertEquals(List.of("exiledsector_harness_shot_atlas_0", "exiledsector_harness_shot_atlas_1", "exiledsector_harness_shot_atlas_2"),
                List.copyOf(commonFiles.keySet()));
        assertEquals(5, commonFiles.get("exiledsector_harness_shot_atlas_2").length());
    }
}
