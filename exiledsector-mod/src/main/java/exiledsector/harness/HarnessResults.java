package exiledsector.harness;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HarnessResults {

    static final int MAX_REPORTED_ERRORS = 40;
    private static final Logger LOG = Logger.getLogger(HarnessResults.class);

    record StepResult(int index, String step, boolean passed, String detail, long millis, String caseId) {
    }

    record Screenshot(String name, int width, int height, int parts, String source) {
    }

    record Clip(String name, long startMillis, long endMillis) {
    }

    private final String saveDir;
    private final HarnessLog log;
    private final List<StepResult> steps = new ArrayList<>();
    private final List<Screenshot> screenshots = new ArrayList<>();
    private final Map<String, JSONObject> data = new LinkedHashMap<>();
    private final List<String> skippedCases = new ArrayList<>();
    private final List<String> notRunCases = new ArrayList<>();
    private final List<Clip> clips = new ArrayList<>();
    private String openClipName;
    private long openClipStartMillis;
    private String currentCase;
    private String abortedAfterCase;
    private String runFailure;
    private boolean finished;

    HarnessResults(String saveDir, HarnessLog log) {
        this.saveDir = saveDir;
        this.log = log;
    }

    void add(StepResult result) {
        steps.add(result);
        LOG.info(Harness.LOG_TAG + "step " + result.index() + " " + result.step() + (result.passed() ? " passed" : " FAILED") + ": "
                + result.detail());
        write();
    }

    void startCase(String caseId) {
        currentCase = caseId;
    }

    void endCase() {
        currentCase = null;
    }

    String currentCase() {
        return currentCase;
    }

    void skipCases(List<String> skipped) {
        skippedCases.addAll(skipped);
    }

    void abortAfter(String caseId, List<String> notRun) {
        abortedAfterCase = caseId;
        notRunCases.addAll(notRun);
        currentCase = null;
        write();
    }

    String startClip(String clipName, long startMillis) {
        String interrupted = endClip(startMillis);
        openClipName = clipName;
        openClipStartMillis = startMillis;
        return interrupted;
    }

    String endClip(long endMillis) {
        if (openClipName == null) {
            return null;
        }
        String ended = openClipName;
        clips.add(new Clip(ended, openClipStartMillis, endMillis));
        openClipName = null;
        write();
        return ended;
    }

    void add(Screenshot screenshot) {
        screenshots.add(screenshot);
    }

    void addData(String name, JSONObject value) {
        data.put(name, value);
        write();
    }

    JSONObject data(String name) {
        return data.get(name);
    }

    void failRun(String failure) {
        endClip(System.currentTimeMillis());
        runFailure = failure;
        finished = true;
        write();
    }

    void finish() {
        endClip(System.currentTimeMillis());
        finished = true;
        write();
    }

    void write() {
        try {
            HarnessFiles.write(HarnessFiles.RESULTS, toJson().toString(2));
        } catch (IOException | JSONException | RuntimeException e) {
            LOG.error(Harness.LOG_TAG + "could not write the results", e);
        }
    }

    private JSONArray casesJson() throws JSONException {
        Map<String, JSONObject> cases = new LinkedHashMap<>();
        for (StepResult step : steps) {
            if (step.caseId() == null) {
                continue;
            }
            JSONObject caseJson = cases.computeIfAbsent(step.caseId(), HarnessResults::newCase);
            caseJson.put("steps", caseJson.getInt("steps") + 1);
            if (!step.passed() && caseJson.getBoolean("passed")) {
                caseJson.put("passed", false).put("failedStep", step.step()).put("failure", step.detail());
            }
        }
        return new JSONArray(cases.values());
    }

    private static JSONObject newCase(String caseId) {
        try {
            return new JSONObject().put("id", caseId).put("passed", true).put("steps", 0);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("save", saveDir);
        json.put("finished", finished);
        json.put("passed", finished && runFailure == null && abortedAfterCase == null && steps.stream().allMatch(StepResult::passed));
        if (runFailure != null) {
            json.put("runFailure", runFailure);
        }
        JSONArray stepArray = new JSONArray();
        for (StepResult step : steps) {
            stepArray.put(new JSONObject().put("index", step.index()).put("step", step.step()).put("passed", step.passed())
                    .put("detail", step.detail()).put("millis", step.millis()).put("case", step.caseId() == null ? JSONObject.NULL : step.caseId()));
        }
        json.put("steps", stepArray);
        json.put("cases", casesJson());
        json.put("skippedCases", new JSONArray(skippedCases));
        json.put("notRunCases", new JSONArray(notRunCases));
        if (abortedAfterCase != null) {
            json.put("abortedAfterCase", abortedAfterCase);
        }
        JSONArray screenshotArray = new JSONArray();
        for (Screenshot screenshot : screenshots) {
            screenshotArray.put(new JSONObject().put("name", screenshot.name()).put("width", screenshot.width()).put("height", screenshot.height())
                    .put("parts", screenshot.parts()).put("source", screenshot.source()));
        }
        json.put("screenshots", screenshotArray);
        JSONArray clipArray = new JSONArray();
        for (Clip clip : clips) {
            clipArray.put(new JSONObject().put("name", clip.name()).put("startMillis", clip.startMillis()).put("endMillis", clip.endMillis()));
        }
        json.put("clips", clipArray);
        JSONObject dataJson = new JSONObject();
        for (Map.Entry<String, JSONObject> entry : data.entrySet()) {
            dataJson.put(entry.getKey(), entry.getValue());
        }
        json.put("data", dataJson);
        List<String> errors = log.errorsFrom(0);
        json.put("errorsLogged", errors.size());
        json.put("errorLines", new JSONArray(errors.subList(0, Math.min(MAX_REPORTED_ERRORS, errors.size()))));
        return json;
    }
}
