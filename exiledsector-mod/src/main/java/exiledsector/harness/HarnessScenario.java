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

    static final String KNOWN_STEPS = "wait, mark, startClip, endClip, showcase, openAtlas, openAtlasTree, openSkillTree, openRefit, closeScreen, zoom, focusAtlasSystem, screenshot, expectScreen, "
            + "expectNoErrors, ignoreErrorsSoFar, expectLog, simulate, spawn, autopilot, speed, recordStats, recordDamage, endSimulation, "
            + "moveMouse, click, scroll, dumpUi, growAtlas, aimAtlas, officerTurns, dressOfficers, addShip, levelShip, buildTree, clearFleets, readyFleet, saveGame, addItem, addSpecial, addSchematic, addCargo, foundColony, visitColony, visitNearest, console, openChart, lockPhasePlanet, defeatChartBoss, leaveChart, passDays, expectPhasePull, "
            + "expectPhasedPlanet";
    static final int DEFAULT_COLONY_SIZE = 4;
    static final long DEFAULT_CHART_SEED = 1L;
    static final float DEFAULT_LOG_WAIT_SECONDS = 10f;
    static final double READY_SUPPLIES_SHARE = 0.6d;

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
            case "startClip" -> ClipStep.start(step.getString("name"));
            case "endClip" -> ClipStep.end();
            case "showcase" -> new ShowcaseStep(step.optBoolean("hideUI", false), step.optBoolean("paused", false));
            case "openAtlas" -> new OpenOverlayStep(stepType, "AtlasScreen", AtlasScreen::open);
            case "openAtlasTree" -> new OpenOverlayStep(stepType, "AtlasTreeScreen", AtlasTreeScreen::open);
            case "openSkillTree" -> new OpenSkillTreeStep(step.optString("ship", OpenSkillTreeStep.FLAGSHIP));
            case "openRefit" -> new OpenSkillTreeStep(step.optString("ship", OpenSkillTreeStep.FLAGSHIP), false);
            case "moveMouse" -> new MoveMouseStep(step.getString("to"), (float) step.optDouble("seconds", 0d));
            case "click" -> new ClickStep(step.optString("button", ClickStep.LEFT), step.optBoolean("ctrl", false));
            case "scroll" -> new ScrollStep(ScrollStep.scrollsOut(step.optString("direction", "out")), step.optInt("notches", 1), (float) step.optDouble("seconds", 0d));
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
            case "addShip" -> new AddShipStep(step.getString("variant"), step.optInt("count", 1), AddShipStep.Loadout.parse(step));
            case "levelShip" -> new LevelShipStep(step.optString("ship", OpenSkillTreeStep.FLAGSHIP), step.getInt("level"));
            case "buildTree" -> new BuildTreeStep(step.getString("root"), strings(step.getJSONArray("goals")), step.optInt("nodes", 0));
            case "clearFleets" -> new ClearFleetsStep((float) step.optDouble("radius", 0d));
            case "readyFleet" -> new ReadyFleetStep((float) step.optDouble("suppliesShare", READY_SUPPLIES_SHARE));
            case "saveGame" -> new SaveGameStep();
            case "addItem" -> new AddItemStep(step.getString("item"), step.optInt("count", 1), step.optLong("seed", AddItemStep.ROLL_SEED),
                    step.optInt("modifiers", AddItemStep.RANDOM_MODIFIERS));
            case "addSpecial" -> new AddSpecialStep(step.getString("item"), step.optString("data", ""), step.optInt("count", 1));
            case "addSchematic" -> new AddSchematicStep(step.getString("tier"), step.optInt("count", 1));
            case "dumpUi" -> new DumpUiStep();
            case "visitColony" -> new VisitColonyStep();
            case "aimAtlas" -> new AimAtlasStep();
            case "visitNearest" -> new VisitNearestStep(step.getString("type"));
            case "console" -> new ConsoleStep(step.getString("command"));
            case "officerTurns" -> new OfficerTurnsStep(step.optInt("turns", OfficerTurnsStep.DEFAULT_TURNS), step.optInt("moves", 0), step.optBoolean("skirmish", false));
            case "dressOfficers" -> new DressOfficersStep(range(step, "scars"), range(step, "rewards"), step.optLong("seed", DressOfficersStep.DRESS_SEED));
            case "growAtlas" -> new GrowAtlasStep(step.optInt("systems", 0), step.optInt("tier", GrowAtlasStep.DEFAULT_TIER), step.optBoolean("identifyOfficers", false));
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

    private static DressOfficersStep.Range range(JSONObject step, String key) throws JSONException {
        JSONArray bounds = step.optJSONArray(key);
        if (bounds == null) {
            int exact = step.getInt(key);
            return new DressOfficersStep.Range(exact, exact);
        }
        if (bounds.length() != 2) {
            throw new IllegalArgumentException(key + " needs a count or a range like [3, 5]");
        }
        return new DressOfficersStep.Range(bounds.getInt(0), bounds.getInt(1));
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
