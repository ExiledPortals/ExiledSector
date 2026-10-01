package exiledsector.ui.node;

import exiledsector.skills.SkillTree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

final class ConnectorFills {

    static final float FILL_SECONDS = 0.5f;

    private final Map<String, Fill> fillsByEdge = new HashMap<>();
    private final Map<String, Float> pulseDelays = new HashMap<>();

    void start(String fromId, String toId) {
        fillsByEdge.put(SkillTree.curveKey(fromId, toId), new Fill(fromId, toId));
    }

    void schedulePulse(String nodeId) {
        pulseDelays.put(nodeId, FILL_SECONDS);
    }

    void cancel(String nodeId) {
        pulseDelays.remove(nodeId);
        fillsByEdge.values().removeIf(fill -> fill.fromId.equals(nodeId) || fill.toId.equals(nodeId));
    }

    boolean isEmpty() {
        return fillsByEdge.isEmpty() && pulseDelays.isEmpty();
    }

    void advance(float amount, Consumer<String> onPulseDue) {
        if (isEmpty()) return;
        Iterator<Fill> fills = fillsByEdge.values().iterator();
        while (fills.hasNext()) {
            Fill fill = fills.next();
            fill.elapsed += amount;
            if (fill.elapsed >= FILL_SECONDS) {
                fills.remove();
            }
        }
        Iterator<Map.Entry<String, Float>> pulses = pulseDelays.entrySet().iterator();
        while (pulses.hasNext()) {
            Map.Entry<String, Float> pulse = pulses.next();
            float remaining = pulse.getValue() - amount;
            if (remaining <= 0f) {
                pulses.remove();
                onPulseDue.accept(pulse.getKey());
            } else {
                pulse.setValue(remaining);
            }
        }
    }

    FillRange filledRange(String startId, String endId) {
        if (fillsByEdge.isEmpty()) return FillRange.FULL;
        Fill fill = fillsByEdge.get(SkillTree.curveKey(startId, endId));
        if (fill == null) return FillRange.FULL;
        float fraction = Math.min(1f, fill.elapsed / FILL_SECONDS);
        return fill.fromId.equals(startId) ? new FillRange(0f, fraction) : new FillRange(1f - fraction, 1f);
    }

    record FillRange(float start, float end) {
        static final FillRange FULL = new FillRange(0f, 1f);

        boolean isFull() {
            return start <= 0f && end >= 1f;
        }

        boolean contains(float from, float to) {
            return from >= start && to <= end;
        }
    }

    private static final class Fill {
        final String fromId;
        final String toId;
        float elapsed;

        Fill(String fromId, String toId) {
            this.fromId = fromId;
            this.toId = toId;
        }
    }
}
