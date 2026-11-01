package exiledsector.charts.atlas;

import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasNamesTest {

    @Test
    void aListedAnomalyUsesItsTranslatedName() {
        assertEquals(Translation.text("chart.atlas.anomaly." + SpatialBreach.ANOMALY_ID), AtlasNames.anomaly(SpatialBreach.ANOMALY_ID));
    }

    @Test
    void anIdWithoutAStringFallsBackToTheTidiedId() {
        assertEquals("Not A Listed Anomaly", AtlasNames.anomaly("not_a_listed-anomaly"));
        assertEquals("", AtlasNames.anomaly(null));
    }
}
