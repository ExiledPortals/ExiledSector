package exiledsector.harness;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessScenarioTest {

    @Test
    void everyKnownStepParsesIntoItsStepInOrder() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse("""
                {"steps": [
                  {"step": "wait", "frames": 5, "seconds": 1.5},
                  {"step": "mark", "text": "before"},
                  {"step": "openAtlas"},
                  {"step": "openAtlasTree"},
                  {"step": "openSkillTree", "ship": "2"},
                  {"step": "closeScreen"},
                  {"step": "zoom", "to": 0.25},
                  {"step": "screenshot", "name": "atlas tree"},
                  {"step": "expectScreen", "name": "atlasTree"},
                  {"step": "expectNoErrors"},
                  {"step": "ignoreErrorsSoFar"},
                  {"step": "expectLog", "contains": "[perf]"}
                ]}""");

        List<String> names = scenario.steps().stream().map(HarnessStep::name).toList();
        assertEquals(List.of("wait 5 frames and 1.5 s", "mark before", "openAtlas", "openAtlasTree", "openSkillTree 2", "closeScreen", "zoom 0.25",
                "screenshot atlas_tree", "expectScreen AtlasTreeScreen", "expectNoErrors", "ignoreErrorsSoFar", "expectLog \"[perf]\""), names);
        assertTrue(scenario.quitWhenDone());
        assertEquals(HarnessScenario.DEFAULT_LOG_WAIT_SECONDS + HarnessStep.DEFAULT_TIMEOUT_SECONDS, scenario.steps().get(11).timeoutSeconds());
    }

    @Test
    void simulatorStepsParseWithTheirDefaults() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse("""
                {"steps": [
                  {"step": "simulate"},
                  {"step": "spawn", "variant": "hound_Standard"},
                  {"step": "spawn", "variant": "wolf_CS", "side": "ally", "count": 2, "distance": 800},
                  {"step": "autopilot"},
                  {"step": "speed", "mult": 3},
                  {"step": "recordStats"},
                  {"step": "recordStats", "ship": "enemy", "name": "target"},
                  {"step": "recordDamage", "seconds": 10, "untilNoEnemies": true},
                  {"step": "endSimulation"}
                ]}""");

        List<String> names = scenario.steps().stream().map(HarnessStep::name).toList();
        assertEquals(List.of("simulate flagship", "spawn 1 hound_Standard for the enemy side", "spawn 2 wolf_CS for the player side", "autopilot on",
                "speed x3.0", "recordStats player as stats_player", "recordStats enemy as target",
                "recordDamage damage for 10.0 combat s or until no enemies are left", "endSimulation"), names);
        assertEquals(SimulateStep.TIMEOUT_SECONDS, scenario.steps().get(0).timeoutSeconds());
        assertEquals(10f * 2f + HarnessStep.DEFAULT_TIMEOUT_SECONDS, scenario.steps().get(7).timeoutSeconds());
    }

    @Test
    void simulatorStepsRejectImpossibleSettings() {
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"spawn\", \"variant\": \"x\", \"side\": \"neutral\"}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"spawn\", \"variant\": \"x\", \"count\": 0}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"speed\", \"mult\": 0}]}"));
        assertThrows(JSONException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"spawn\"}]}"));
    }

    @Test
    void openSkillTreeDefaultsToTheFlagshipAndTheRunCanStayOpen() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse("{\"quitWhenDone\": false, \"steps\": [{\"step\": \"openSkillTree\"}]}");

        assertEquals("openSkillTree " + OpenSkillTreeStep.FLAGSHIP, scenario.steps().get(0).name());
        assertFalse(scenario.quitWhenDone());
    }

    @Test
    void aNewGameScenarioKeepsItsChoicesAndSetupSteps() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse("""
                {"newGame": {"choices": ["nex_NGCDevStart"], "characterName": "Tester"},
                 "steps": [{"step": "addShip", "variant": "eagle_Assault", "count": 2}, {"step": "saveGame"}]}
                """);

        assertEquals(new NewGameSetup(List.of("nex_NGCDevStart"), "Tester"), scenario.newGame());
        assertEquals(List.of("addShip 2 eagle_Assault", "saveGame"), scenario.steps().stream().map(HarnessStep::name).toList());
    }

    @Test
    void aNewGameDefaultsToTheVanillaDevStartAndASaveScenarioHasNone() throws JSONException {
        HarnessScenario newGame = HarnessScenario.parse("{\"newGame\": {}, \"steps\": []}");
        HarnessScenario loadSave = HarnessScenario.parse("{\"steps\": []}");

        assertEquals(new NewGameSetup(NewGameSetup.DEFAULT_CHOICES, NewGameSetup.DEFAULT_CHARACTER_NAME), newGame.newGame());
        assertNull(loadSave.newGame());
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"newGame\": {\"choices\": []}, \"steps\": []}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"addShip\", \"variant\": \"x\", \"count\": 0}]}"));
    }

    @Test
    void anUnknownStepNamesTheKnownOnes() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"openEverything\"}]}"));

        assertTrue(thrown.getMessage().contains("openEverything"));
        assertTrue(thrown.getMessage().contains(HarnessScenario.KNOWN_STEPS));
    }

    @Test
    void aStepMissingItsRequiredFieldIsRejected() {
        assertThrows(JSONException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"zoom\"}]}"));
        assertThrows(JSONException.class, () -> HarnessScenario.parse("{\"steps\": [{\"wait\": 3}]}"));
    }

    @Test
    void errorChecksInOneScenarioShareTheirCursor() throws Throwable {
        HarnessScenario scenario = HarnessScenario.parse("{\"steps\": [{\"step\": \"ignoreErrorsSoFar\"}, {\"step\": \"expectNoErrors\"}]}");
        HarnessLog log = new HarnessLog();
        log.doAppend(HarnessLogTest.event(org.apache.log4j.Level.ERROR, "loading broke"));
        StepContext context = HarnessStepsTest.context(log, 0, 0f);

        assertTrue(scenario.steps().get(0).advance(context).passed());
        StepOutcome afterIgnoring = scenario.steps().get(1).advance(context);

        assertInstanceOf(ErrorCheckStep.class, scenario.steps().get(1));
        assertTrue(afterIgnoring.passed(), afterIgnoring.detail());
    }
}
