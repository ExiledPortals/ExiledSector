package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasShipGroupPingTest {

    private static final float DELTA = 0.001f;

    @Test
    void aPingSweepsOutOverItsDurationThenWaitsForTheNextOne() {
        AtlasShipGroup.SensorPing ping = new AtlasShipGroup.SensorPing(6f, 0f);

        assertEquals(0f, ping.progressAt(0f), DELTA);
        assertEquals(0.5f, ping.progressAt(AtlasShipGroup.PING_SECONDS / 2f), DELTA);
        assertEquals(-1f, ping.progressAt(AtlasShipGroup.PING_SECONDS + 0.1f), DELTA);
        assertEquals(0.5f, ping.progressAt(6f + AtlasShipGroup.PING_SECONDS / 2f), DELTA);
    }

    @Test
    void itsPhaseShiftsWhenItFires() {
        AtlasShipGroup.SensorPing ping = new AtlasShipGroup.SensorPing(6f, 2f);

        assertEquals(0f, ping.progressAt(4f), DELTA);
        assertEquals(-1f, ping.progressAt(0f), DELTA);
    }
}
