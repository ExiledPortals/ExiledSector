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

    record StepResult(int index, String step, boolean passed, String detail, long millis) {
    }

    record Screenshot(String name, int width, int height, int parts, String source) {
    }

    private final String saveDir;
    private final HarnessLog log;
    private final List<StepResult> steps = new ArrayList<>();
    private final List<Screenshot> screenshots = new ArrayList<>();
    private final Map<String, JSONObject> data = new LinkedHashMap<>();
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
        runFailure = failure;
        finished = true;
        write();
    }

    void finish() {
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

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("save", saveDir);
        json.put("finished", finished);
        json.put("passed", finished && runFailure == null && steps.stream().allMatch(StepResult::passed));
        if (runFailure != null) {
            json.put("runFailure", runFailure);
        }
        JSONArray stepArray = new JSONArray();
        for (StepResult step : steps) {
            stepArray.put(new JSONObject().put("index", step.index()).put("step", step.step()).put("passed", step.passed())
                    .put("detail", step.detail()).put("millis", step.millis()));
        }
        json.put("steps", stepArray);
        JSONArray screenshotArray = new JSONArray();
        for (Screenshot screenshot : screenshots) {
            screenshotArray.put(new JSONObject().put("name", screenshot.name()).put("width", screenshot.width()).put("height", screenshot.height())
                    .put("parts", screenshot.parts()).put("source", screenshot.source()));
        }
        json.put("screenshots", screenshotArray);
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
