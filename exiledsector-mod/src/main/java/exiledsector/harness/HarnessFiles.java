package exiledsector.harness;

import com.fs.starfarer.api.Global;

import java.io.IOException;

final class HarnessFiles {

    static final String SCENARIO = "exiledsector_harness_scenario";
    static final String RESULTS = "exiledsector_harness_results";
    static final String SCREENSHOT_PREFIX = "exiledsector_harness_shot_";
    static final int MAX_CHARS_PER_FILE = 1_000_000;

    private HarnessFiles() {
    }

    static String readScenario() throws IOException {
        String scenario = Global.getSettings().readTextFileFromCommon(SCENARIO);
        if (scenario == null || scenario.isBlank()) {
            throw new IOException("saves/common/" + SCENARIO + ".data is missing or empty");
        }
        return scenario;
    }

    static void write(String name, String text) throws IOException {
        Global.getSettings().writeTextFileToCommon(name, text);
    }

    static int writeScreenshot(String screenshotName, String base64Png) throws IOException {
        int parts = 0;
        for (int start = 0; start < base64Png.length(); start += MAX_CHARS_PER_FILE) {
            write(SCREENSHOT_PREFIX + screenshotName + "_" + parts, base64Png.substring(start, Math.min(base64Png.length(), start + MAX_CHARS_PER_FILE)));
            parts++;
        }
        return parts;
    }
}
