package exiledsector.harness;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class HarnessSuite {

    private HarnessSuite() {
    }

    static boolean isSuite(JSONObject scenario) {
        return scenario.has("groups");
    }

    static HarnessScenario parse(JSONObject scenario, SuiteCase.Filter filter, Predicate<String> modEnabled) throws JSONException {
        JSONArray groups = scenario.getJSONArray("groups");
        ErrorCheckStep.Cursor errorCursor = new ErrorCheckStep.Cursor();
        SuiteSteps.Baseline baseline = new SuiteSteps.Baseline();
        List<HarnessStep> steps = new ArrayList<>();
        steps.add(new SuiteSteps.CaptureBaselineStep(baseline));
        List<String> skipped = new ArrayList<>();
        boolean started = filter.startAfter() == null;
        for (int groupIndex = 0; groupIndex < groups.length(); groupIndex++) {
            JSONObject group = groups.getJSONObject(groupIndex);
            String groupId = group.getString("id");
            List<String> missingMods = HarnessScenario.strings(group.optJSONArray("requiresMods")).stream().filter(modEnabled.negate()).toList();
            JSONArray cases = group.getJSONArray("cases");
            for (int caseIndex = 0; caseIndex < cases.length(); caseIndex++) {
                JSONObject caseJson = cases.getJSONObject(caseIndex);
                SuiteCase suiteCase = suiteCase(groupId, group, caseJson);
                if (!started) {
                    started = suiteCase.id().equals(filter.startAfter());
                    continue;
                }
                if (!filter.selects(suiteCase)) {
                    continue;
                }
                if (!missingMods.isEmpty()) {
                    skipped.add(suiteCase.id() + ": needs " + missingMods);
                    continue;
                }
                steps.add(new SuiteSteps.StartCaseStep(suiteCase));
                steps.add(ErrorCheckStep.ignoreErrorsSoFar(errorCursor));
                addSteps(steps, group.optJSONArray("setup"), errorCursor, suiteCase.seed());
                addSteps(steps, caseJson.getJSONArray("steps"), errorCursor, suiteCase.seed());
                steps.add(ErrorCheckStep.expectNoErrors(errorCursor));
                steps.add(new SuiteSteps.TeardownStep(suiteCase, baseline));
            }
        }
        JSONObject newGame = scenario.optJSONObject("newGame");
        return new HarnessScenario(List.copyOf(steps), scenario.optBoolean("quitWhenDone", true), newGame == null ? null : NewGameSetup.parse(newGame),
                List.copyOf(skipped));
    }

    private static SuiteCase suiteCase(String groupId, JSONObject group, JSONObject caseJson) throws JSONException {
        String id = SuiteCase.fullId(groupId, caseJson.getString("id"));
        Set<String> tags = new LinkedHashSet<>();
        tags.add(groupId);
        tags.addAll(HarnessScenario.strings(group.optJSONArray("tags")));
        tags.addAll(HarnessScenario.strings(caseJson.optJSONArray("tags")));
        long seed = caseJson.has("seed") ? caseJson.getLong("seed") : SuiteCase.defaultSeed(id);
        return new SuiteCase(id, groupId, List.copyOf(tags), seed);
    }

    private static void addSteps(List<HarnessStep> steps, JSONArray stepArray, ErrorCheckStep.Cursor errorCursor, long seed) throws JSONException {
        for (int index = 0; stepArray != null && index < stepArray.length(); index++) {
            steps.add(HarnessScenario.step(stepArray.getJSONObject(index), errorCursor, seed));
        }
    }
}
