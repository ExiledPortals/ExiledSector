package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class RootCircuit {

    static final String DATA_PATH = "data/config/exiledSector/root_circuit.json";
    static final float SPARK_INTERVAL_SECONDS = 5f;
    static final float SPARK_SPEED_RADII_PER_SECOND = 1.2f;
    static final RootCircuit EMPTY = new RootCircuit(1f, List.of());
    private static final Logger LOG = Logger.getLogger(RootCircuit.class);
    private static final double PHASE_STEP = 0.6180339887;
    private static RootCircuit loaded;

    record Trace(float[] xs, float[] ys, float[] distances, float phaseSeconds, float[] segmentAngles) {

        float length() {
            return distances[distances.length - 1];
        }

        float tripSeconds() {
            return length() / SPARK_SPEED_RADII_PER_SECOND;
        }

        void pointAt(float distance, float[] out) {
            float clamped = Math.max(0f, Math.min(length(), distance));
            int segment = 1;
            while (segment < distances.length - 1 && distances[segment] < clamped) {
                segment++;
            }
            float segmentLength = distances[segment] - distances[segment - 1];
            float along = segmentLength <= 0f ? 0f : (clamped - distances[segment - 1]) / segmentLength;
            out[0] = xs[segment - 1] + (xs[segment] - xs[segment - 1]) * along;
            out[1] = ys[segment - 1] + (ys[segment] - ys[segment - 1]) * along;
        }

        int sparkDistances(float timeSeconds, float[] out) {
            float sinceLastLaunch = ((timeSeconds - phaseSeconds) % SPARK_INTERVAL_SECONDS + SPARK_INTERVAL_SECONDS) % SPARK_INTERVAL_SECONDS;
            int count = 0;
            for (float flight = sinceLastLaunch; flight <= tripSeconds() && count < out.length; flight += SPARK_INTERVAL_SECONDS) {
                out[count++] = flight * SPARK_SPEED_RADII_PER_SECOND;
            }
            return count;
        }
    }

    private final float extentRadii;
    private final List<Trace> traces;

    private RootCircuit(float extentRadii, List<Trace> traces) {
        this.extentRadii = extentRadii;
        this.traces = traces;
    }

    static RootCircuit get() {
        if (loaded == null) {
            loaded = load();
        }
        return loaded;
    }

    private static RootCircuit load() {
        try {
            return parse(Global.getSettings().loadJSON(DATA_PATH));
        } catch (IOException | JSONException | RuntimeException e) {
            LOG.error("Couldn't load the root circuit from " + DATA_PATH + "; roots draw without it", e);
            return EMPTY;
        }
    }

    static RootCircuit parse(JSONObject json) throws JSONException {
        JSONArray traceArray = json.getJSONArray("traces");
        List<Trace> traces = new ArrayList<>();
        for (int traceIndex = 0; traceIndex < traceArray.length(); traceIndex++) {
            JSONArray pointArray = traceArray.getJSONObject(traceIndex).getJSONArray("points");
            float[] xs = new float[pointArray.length()];
            float[] ys = new float[pointArray.length()];
            float[] distances = new float[pointArray.length()];
            for (int pointIndex = 0; pointIndex < pointArray.length(); pointIndex++) {
                JSONArray point = pointArray.getJSONArray(pointIndex);
                xs[pointIndex] = (float) point.getDouble(0);
                ys[pointIndex] = (float) point.getDouble(1);
                if (pointIndex > 0) {
                    distances[pointIndex] = distances[pointIndex - 1]
                            + (float) Math.hypot(xs[pointIndex] - xs[pointIndex - 1], ys[pointIndex] - ys[pointIndex - 1]);
                }
            }
            float[] segmentAngles = new float[Math.max(0, xs.length - 1)];
            for (int segment = 0; segment < segmentAngles.length; segment++) {
                segmentAngles[segment] = (float) Math.toDegrees(Math.atan2(ys[segment + 1] - ys[segment], xs[segment + 1] - xs[segment]));
            }
            float phaseShare = (float) ((traceIndex * PHASE_STEP) % 1.0);
            traces.add(new Trace(xs, ys, distances, phaseShare * SPARK_INTERVAL_SECONDS, segmentAngles));
        }
        return new RootCircuit((float) json.getDouble("extentRadii"), List.copyOf(traces));
    }

    float extentRadii() {
        return extentRadii;
    }

    float longestTraceRadii() {
        float longest = 0f;
        for (Trace trace : traces) {
            longest = Math.max(longest, trace.length());
        }
        return longest;
    }

    List<Trace> traces() {
        return traces;
    }
}
