package exiledsector.harness;

import exiledsector.ui.atlas.AtlasScreen;
import exiledsector.ui.atlas.AtlasTreeScreen;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

record HarnessScenario(List<HarnessStep> steps, boolean quitWhenDone, NewGameSetup newGame, List<String> skippedCases) {

    static final String KNOWN_STEPS = "wait, mark, openAtlas, openAtlasTree, openSkillTree, closeScreen, zoom, focusAtlasSystem, screenshot, expectScreen, "
            + "expectNoErrors, ignoreErrorsSoFar, expectLog, simulate, spawn, autopilot, speed, recordStats, recordDamage, endSimulation, "
            + "addShip, saveGame, addCargo, foundColony, openChart, lockPhasePlanet, defeatChartBoss, leaveChart, passDays, expectPhasePull, "
            + "expectPhasedPlanet";
    static final int DEFAULT_COLONY_SIZE = 4;
    static final long DEFAULT_CHART_SEED = 1L;
    static final float DEFAULT_LOG_WAIT_SECONDS = 10f;

    HarnessScenario(List<HarnessStep> steps, boolean quitWhenDone) {
        this(steps, quitWhenDone, null, List.of());
    }

    static HarnessScenario parse(String scenarioText) throws JSONException {
        return parse(scenarioText, SuiteCase.Filter.ALL, modId -> true);
    }

    static HarnessScenario parse(String scenarioText, SuiteCase.Filter filter, Predicate<String> modEnabled) throws JSONException {
        JSONObject scenario = new JSONObject(scenarioText);
        if (HarnessSuite.isSuite(scenario)) {
            return HarnessSuite.parse(scenario, filter, modEnabled);
        }
        JSONArray stepArray = scenario.getJSONArray("steps");
        ErrorCheckStep.Cursor errorCursor = new ErrorCheckStep.Cursor();
        List<HarnessStep> steps = new ArrayList<>();
        for (int index = 0; index < stepArray.length(); index++) {
            steps.add(step(stepArray.getJSONObject(index), errorCursor, DEFAULT_CHART_SEED));
        }
        JSONObject newGame = scenario.optJSONObject("newGame");
        return new HarnessScenario(List.copyOf(steps), scenario.optBoolean("quitWhenDone", true), newGame == null ? null : NewGameSetup.parse(newGame), List.of());
    }

    static HarnessStep step(JSONObject step, ErrorCheckStep.Cursor errorCursor, long defaultSeed) throws JSONException {
        String stepType = step.getString("step");
        return switch (stepType) {
            case "wait" -> new WaitStep(step.optInt("frames", 0), (float) step.optDouble("seconds", 0d));
            case "mark" -> new MarkStep(step.optString("text", ""));
            case "openAtlas" -> new OpenOverlayStep(stepType, "AtlasScreen", AtlasScreen::open);
            case "openAtlasTree" -> new OpenOverlayStep(stepType, "AtlasTreeScreen", AtlasTreeScreen::open);
            case "openSkillTree" -> new OpenSkillTreeStep(step.optString("ship", OpenSkillTreeStep.FLAGSHIP));
            case "closeScreen" -> new CloseScreenStep();
            case "zoom" -> new ZoomStep((float) step.getDouble("to"));
            case "focusAtlasSystem" -> new FocusAtlasSystemStep(step.optInt("system", 0), strings(step.optJSONArray("anomalies")),
                    strings(step.optJSONArray("catalysts")));
            case "screenshot" -> new ScreenshotStep(step.getString("name"));
            case "expectScreen" -> new ExpectScreenStep(step.getString("name"));
            case "expectNoErrors" -> ErrorCheckStep.expectNoErrors(errorCursor);
            case "ignoreErrorsSoFar" -> ErrorCheckStep.ignoreErrorsSoFar(errorCursor);
            case "expectLog" -> new ExpectLogStep(step.getString("contains"), (float) step.optDouble("withinSeconds", DEFAULT_LOG_WAIT_SECONDS));
            case "simulate" -> new SimulateStep(step.optString("ship", OpenSkillTreeStep.FLAGSHIP));
            case "spawn" -> new SpawnStep(step.getString("variant"), SpawnStep.side(step.optString("side", CombatShips.ENEMY)),
                    (float) step.optDouble("distance", SpawnStep.DEFAULT_DISTANCE), step.optInt("count", 1));
            case "autopilot" -> new AutopilotStep(step.optBoolean("on", true));
            case "speed" -> new SpeedStep((float) step.getDouble("mult"));
            case "recordStats" -> {
                String shipSelector = step.optString("ship", CombatShips.PLAYER);
                yield new RecordStatsStep(shipSelector, step.optString("name", "stats_" + shipSelector));
            }
            case "recordDamage" -> new RecordDamageStep(step.optString("name", "damage"), (float) step.optDouble("seconds", RecordDamageStep.DEFAULT_COMBAT_SECONDS),
                    step.optBoolean("untilNoEnemies", false), (float) step.optDouble("minEnemyDamage", 0d));
            case "endSimulation" -> new EndSimulationStep();
            case "addShip" -> new AddShipStep(step.getString("variant"), step.optInt("count", 1));
            case "saveGame" -> new SaveGameStep();
            case "addCargo" -> new AddCargoStep(step.getString("commodity"), step.getInt("amount"));
            case "foundColony" -> new FoundColonyStep(step.optInt("size", DEFAULT_COLONY_SIZE), strings(step.optJSONArray("industries")),
                    amounts(step.optJSONObject("supply")), step.optBoolean("nearHypershunt", false));
            case "openChart" -> new OpenChartStep(OpenChartStep.Options.parse(step, defaultSeed));
            case "lockPhasePlanet" -> new LockPhasePlanetStep();
            case "defeatChartBoss" -> new DefeatChartBossStep();
            case "leaveChart" -> new LeaveChartStep();
            case "passDays" -> new PassDaysStep((float) step.getDouble("days"), step.optInt("speed", PassDaysStep.DEFAULT_SPEED));
            case "expectPhasePull" -> new ExpectPhasePullStep(step.optString("name", "phasePull"));
            case "expectPhasedPlanet" -> new ExpectPhasedPlanetStep();
            default -> throw new IllegalArgumentException("unknown step \"" + stepType + "\"; known steps: " + KNOWN_STEPS);
        };
    }

    static List<String> strings(JSONArray array) throws JSONException {
        List<String> values = new ArrayList<>();
        for (int index = 0; array != null && index < array.length(); index++) {
            values.add(array.getString(index));
        }
        return values;
    }

    private static Map<String, Integer> amounts(JSONObject object) throws JSONException {
        Map<String, Integer> values = new LinkedHashMap<>();
        String[] keys = object == null ? null : JSONObject.getNames(object);
        for (int index = 0; keys != null && index < keys.length; index++) {
            values.put(keys[index], object.getInt(keys[index]));
        }
        return values;
    }
}
