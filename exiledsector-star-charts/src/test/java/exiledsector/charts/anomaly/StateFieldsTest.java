package exiledsector.charts.anomaly;

import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateFieldsTest {

    enum Colour {
        RED,
        BLUE
    }

    record Sample(String name, Colour colour, long seed, float share, boolean flagged) {
    }

    private static String encode(Sample sample) {
        return StateFields.join(sample.name(), sample.colour().name(), StateFields.seed(sample.seed()), StateFields.decimal(sample.share()),
                StateFields.flag(sample.flagged()));
    }

    private static Sample decode(String encoded) {
        return StateFields.decode(encoded, 5, fields -> new Sample(fields.text(0), fields.enumValue(1, Colour.class), fields.seed(2), fields.decimal(3),
                fields.flag(4)));
    }

    @Test
    void typedFieldsSurviveEncodingAndBrokenTextDecodesToNothing() {
        Sample sample = new Sample("rock", Colour.BLUE, Long.MIN_VALUE, 0.25f, true);

        assertEquals(sample, decode(encode(sample)));
        assertNull(decode("rock,GREEN,1,0.5,1"));
        assertNull(decode("rock,RED,not-a-seed,0.5,1"));
        assertNull(decode("rock,RED,1"));
        assertNull(decode(null));
        assertEquals(List.of("a", "b"), StateFields.list("a,,b"));
        assertTrue(StateFields.fits("planet_7"));
        assertFalse(StateFields.fits("a;b"));
    }

    @Test
    void anAnomalysStateKeepsItsFieldsUnderItsOwnIdOnTheRun() {
        ChartRun run = new ChartRun("run1", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        AnomalyState state = AnomalyState.of(run, "test");

        state.put("count", 4);
        state.put("colour", Colour.RED);
        state.put("broken", "x");

        assertEquals("4", run.anomalyState("test.count"));
        assertEquals(4, state.integer("count"));
        assertEquals(Colour.RED, state.enumValue("colour", Colour.class));
        assertEquals(0, state.integer("broken"));
        assertNull(state.enumValue("broken", Colour.class));
        state.put("count", (String) null);
        assertFalse(state.has("count"));
        assertNull(AnomalyState.of(null, "test").text("count"));
    }
}
