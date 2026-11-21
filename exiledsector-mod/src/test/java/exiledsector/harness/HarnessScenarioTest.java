package exiledsector.harness;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    void fleetSetupStepsParseTheirLoadoutsAndDefaults() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse("""
                {"steps": [
                  {"step": "addShip", "variant": "astral_Elite", "name": "ISS Aldebaran Rising", "weapons": {"WS 001": "harpoon"},
                   "wings": ["dagger_wing"], "hullMods": ["eccm"], "flagship": true, "replaceFleet": true},
                  {"step": "levelShip", "ship": "0", "level": 15},
                  {"step": "buildTree", "root": "root_midline", "goals": ["expanded_deck_crew", "armor"]},
                  {"step": "readyFleet"},
                  {"step": "clearFleets"},
                  {"step": "clearFleets", "radius": 2000}
                ]}""");

        List<String> names = scenario.steps().stream().map(HarnessStep::name).toList();
        assertEquals(List.of("addShip 1 astral_Elite as ISS Aldebaran Rising (flagship)", "levelShip 0 to 15", "buildTree from root_midline toward 2 goals",
                "readyFleet", "clearFleets", "clearFleets within 2000"), names);
        AddShipStep.Fitting fitting = new AddShipStep.Fitting(true, Map.of("WS 001", "harpoon"), List.of("dagger_wing"), List.of("eccm"), 44, 0);
        assertEquals(new AddShipStep.Loadout("ISS Aldebaran Rising", fitting, true, true), AddShipStep.Loadout.parse(new JSONObject("""
                {"name": "ISS Aldebaran Rising", "strip": true, "weapons": {"WS 001": "harpoon"}, "wings": ["dagger_wing"], "hullMods": ["eccm"],
                 "vents": 44, "capacitors": 0, "flagship": true, "replaceFleet": true}""")));
        assertEquals(AddShipStep.Loadout.STOCK, AddShipStep.Loadout.parse(new JSONObject("{}")));
        assertFalse(AddShipStep.Fitting.STOCK.customisesVariant());
        assertTrue(AddShipStep.Fitting.parse(new JSONObject("{\"strip\": true}")).customisesVariant());
        assertTrue(AddShipStep.Fitting.parse(new JSONObject("{\"vents\": 0}")).customisesVariant());
    }

    @Test
    void fleetSetupStepsRejectImpossibleSettings() {
        assertThrows(IllegalArgumentException.class,
                () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"addShip\", \"variant\": \"x\", \"count\": 2, \"flagship\": true}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"levelShip\", \"level\": 0}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"buildTree\", \"root\": \"r\", \"goals\": []}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"readyFleet\", \"suppliesShare\": 2}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"clearFleets\", \"radius\": -1}]}"));
    }

    @Test
    void mouseStepsParseTheirTargetsAndDefaults() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse("""
                {"steps": [
                  {"step": "openRefit", "ship": "ISS Tereshkova III"},
                  {"step": "moveMouse", "to": "skillTreeButton", "seconds": 0.8},
                  {"step": "moveMouse", "to": "node:root_high_tech"},
                  {"step": "moveMouse", "to": "1250, 420.5", "seconds": 1},
                  {"step": "click"},
                  {"step": "scroll", "notches": 12, "seconds": 3},
                  {"step": "scroll", "direction": "in"}
                ]}""");

        List<String> names = scenario.steps().stream().map(HarnessStep::name).toList();
        assertEquals(List.of("openRefit ISS Tereshkova III", "moveMouse to skillTreeButton over 0.8 s", "moveMouse to node:root_high_tech over 0.0 s",
                "moveMouse to 1250, 420.5 over 1.0 s", "click", "scroll out 12 notches over 3.0 s", "scroll in 1 notches over 0.0 s"), names);
    }

    @Test
    void mouseStepsRejectUnknownTargetsAndDirections() {
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"moveMouse\", \"to\": \"somewhere\"}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"moveMouse\", \"to\": \"12,ab\"}]}"));
        assertThrows(IllegalArgumentException.class,
                () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"moveMouse\", \"to\": \"centre\", \"seconds\": -1}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"scroll\", \"direction\": \"up\"}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"scroll\", \"notches\": 0}]}"));
    }

    @Test
    void theMouseCalibrationMapsRobotPointsOntoWindowPixels() {
        HarnessMouse.Calibration calibration = HarnessMouse.Calibration.between(1000, 600, 300f, 200f, 1200, 800, 600f, 500f);

        assertEquals(1.5f, calibration.scaleX());
        assertEquals(1000, calibration.robotX(300f));
        assertEquals(1100, calibration.robotX(450f));
        assertEquals(700, calibration.robotY(350f));
        assertNull(HarnessMouse.Calibration.between(1000, 600, 300f, 200f, 1200, 800, 300f, 500f));
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
    void thePhasePullStepsParseWithTheirDefaults() throws JSONException {
        HarnessScenario scenario = HarnessScenario.parse("""
                {"steps": [
                  {"step": "foundColony", "nearHypershunt": true, "industries": ["exiledSector_phase_displacement_gate"], "supply": {"volatiles": 10}},
                  {"step": "foundColony", "size": 6},
                  {"step": "addCargo", "commodity": "fuel", "amount": 1500},
                  {"step": "openChart"},
                  {"step": "openChart", "seed": 7},
                  {"step": "lockPhasePlanet"},
                  {"step": "defeatChartBoss"},
                  {"step": "leaveChart"},
                  {"step": "passDays", "days": 60},
                  {"step": "passDays", "days": 2.5, "speed": 10},
                  {"step": "expectPhasePull"},
                  {"step": "expectPhasePull", "name": "halfway"},
                  {"step": "expectPhasedPlanet"}
                ]}""");

        assertEquals(List.of("foundColony size 4 near a hypershunt", "foundColony size 6", "addCargo 1500 fuel", "openChart seed 1", "openChart seed 7", "lockPhasePlanet",
                "defeatChartBoss", "leaveChart", "passDays 60.0 at x50", "passDays 2.5 at x10", "expectPhasePull phasePull", "expectPhasePull halfway",
                "expectPhasedPlanet"), scenario.steps().stream().map(HarnessStep::name).toList());
        assertTrue(scenario.steps().get(8).timeoutSeconds() > HarnessStep.DEFAULT_TIMEOUT_SECONDS);
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"foundColony\", \"size\": 2}]}"));
        assertThrows(IllegalArgumentException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"passDays\", \"days\": 0}]}"));
        assertThrows(JSONException.class, () -> HarnessScenario.parse("{\"steps\": [{\"step\": \"passDays\"}]}"));
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
