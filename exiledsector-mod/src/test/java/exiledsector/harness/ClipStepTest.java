package exiledsector.harness;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClipStepTest {

    @Test
    void startingAClipEndsTheOpenOneAndFinishingEndsTheLast() throws JSONException {
        HarnessResults results = new HarnessResults("save", new HarnessLog());

        assertNull(results.endClip(5L));
        assertNull(results.startClip("atlas_tour", 1_000L));
        assertEquals("atlas_tour", results.startClip("chart_dive", 4_000L));
        results.finish();

        JSONArray clips = results.toJson().getJSONArray("clips");
        assertEquals(2, clips.length());
        assertEquals("atlas_tour", clips.getJSONObject(0).getString("name"));
        assertEquals(1_000L, clips.getJSONObject(0).getLong("startMillis"));
        assertEquals(4_000L, clips.getJSONObject(0).getLong("endMillis"));
        assertEquals("chart_dive", clips.getJSONObject(1).getString("name"));
        assertEquals(4_000L, clips.getJSONObject(1).getLong("startMillis"));
    }

    @Test
    void theStepsAreNamedForTheLog() {
        assertEquals("startClip atlas_tour", ClipStep.start("atlas_tour").name());
        assertEquals("endClip", ClipStep.end().name());
    }
}
