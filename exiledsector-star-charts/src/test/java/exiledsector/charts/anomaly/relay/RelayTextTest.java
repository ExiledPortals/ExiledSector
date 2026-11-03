package exiledsector.charts.anomaly.relay;

import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RelayTextTest {

    private static final AtlasAnomalyRecord.Chain FIRST_OF_THREE = new AtlasAnomalyRecord.Chain(1, 3, null);
    private static final AtlasAnomalyRecord.Chain LAST_OF_THREE = new AtlasAnomalyRecord.Chain(3, 3, null);

    private static ChartRun runWithAwake(RelayKind... kinds) {
        ChartRun run = ChartRelaysTest.liveRelayRun();
        for (RelayKind kind : kinds) {
            RelayStates.add(run, kind, RelayStates.WOKEN);
        }
        return run;
    }

    private static ChartRun allAwake() {
        return runWithAwake(RelayKind.values());
    }

    @Test
    void eachWokenRelaySaysHowManyAreStillDormant() {
        assertEquals(List.of("Nav Buoy is awake, 2 relays still dormant"),
                RelayText.wokenMessages(runWithAwake(RelayKind.NAV_BUOY), "Nav Buoy", FIRST_OF_THREE));
        assertEquals(List.of("Comm Relay is awake, 1 relay still dormant"),
                RelayText.wokenMessages(runWithAwake(RelayKind.NAV_BUOY, RelayKind.COMM_RELAY), "Comm Relay", FIRST_OF_THREE));
    }

    @Test
    void theLastWokenRelayPointsAtTheNextStageOrTheFinalBoss() {
        assertEquals(List.of("Sensor Array is awake", "All three relays are awake", "Their trace leads to stage 2 of 3 in another Atlas system"),
                RelayText.wokenMessages(allAwake(), "Sensor Array", FIRST_OF_THREE));
        assertEquals("Destroy the Star Chart Boss for a unique hull item or subroutine",
                RelayText.wokenMessages(allAwake(), "Sensor Array", LAST_OF_THREE).get(2));
        assertEquals(List.of("Sensor Array is awake", "All three relays are awake"), RelayText.wokenMessages(allAwake(), "Sensor Array", null));
    }

    @Test
    void aBossAlreadyDestroyedOrLostOnlyChangesTheLastStage() {
        ChartRun beaten = allAwake();
        beaten.markObjectiveComplete();
        ChartRun lost = allAwake();
        lost.markObjectiveLost();

        assertEquals("Their trace leads to stage 2 of 3 in another Atlas system", RelayText.wokenMessages(lost, "Nav Buoy", FIRST_OF_THREE).get(2));
        assertEquals(2, RelayText.wokenMessages(beaten, "Nav Buoy", LAST_OF_THREE).size());
        assertEquals(2, RelayText.wokenMessages(lost, "Nav Buoy", LAST_OF_THREE).size());
    }

    @Test
    void eachStageHasItsOwnTrailAndALogForEveryRelay() {
        Set<String> texts = new HashSet<>();
        for (AtlasAnomalyRecord.Chain stage : List.of(FIRST_OF_THREE, new AtlasAnomalyRecord.Chain(2, 3, null), LAST_OF_THREE)) {
            texts.add(Translation.text(RelayText.trailKey(stage)));
            for (RelayKind kind : RelayKind.values()) {
                texts.add(Translation.text(RelayText.logKey(kind, stage)));
            }
        }

        assertEquals(12, texts.size());
        texts.forEach(text -> assertFalse(text.startsWith(RelayText.PREFIX), text));
        assertEquals(RelayText.logKey(RelayKind.NAV_BUOY, new AtlasAnomalyRecord.Chain(2, 4, null)),
                RelayText.logKey(RelayKind.NAV_BUOY, new AtlasAnomalyRecord.Chain(3, 4, null)));
        assertEquals(RelayText.trailKey(FIRST_OF_THREE), RelayText.trailKey(null));
    }

    @Test
    void theStatusFollowsHowManyRelaysAreAwake() {
        assertEquals("Three dormant Domain-era relays sit in the system.", RelayText.status(runWithAwake(), FIRST_OF_THREE));
        assertEquals("1 of the three Domain-era relays is awake.", RelayText.status(runWithAwake(RelayKind.SENSOR_ARRAY), FIRST_OF_THREE));
        assertEquals("2 of the three Domain-era relays are awake.",
                RelayText.status(runWithAwake(RelayKind.SENSOR_ARRAY, RelayKind.NAV_BUOY), FIRST_OF_THREE));
        assertEquals("All three Domain-era relays are awake. Their trace leads to stage 2 of 3 in another Atlas system.",
                RelayText.status(allAwake(), FIRST_OF_THREE));
        assertEquals("All three Domain-era relays are awake. Destroy the Star Chart Boss for a unique hull item or subroutine.",
                RelayText.status(allAwake(), LAST_OF_THREE));
        assertEquals("All three Domain-era relays are awake.", RelayText.status(allAwake(), null));
    }
}
