package exiledsector.charts.phasing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhasedPlanetRecordTest {

    @Test
    void aRecordSurvivesEncoding() {
        PhasedPlanetRecord record = new PhasedPlanetRecord("run1", "planet_3", "Charted Space III", "frozen", 140f,
                "graphics/planets/frozen01.jpg", List.of("cold", "volatiles_abundant"), "Charted Space", "frozen_fringe",
                List.of("black_hole"), 12, List.of("exiledSector_cryo_locked_aquifers", "exiledSector_time_slipped_crust"));

        assertEquals(record, PhasedPlanetRecord.decode(record.encode()));
    }

    @Test
    void missingOptionalFieldsStayMissing() {
        PhasedPlanetRecord record = new PhasedPlanetRecord("run1", "planet_3", "Rock", "barren", 80f, null, List.of(), null, null,
                null, 1, null);

        PhasedPlanetRecord decoded = PhasedPlanetRecord.decode(record.encode());

        assertEquals(record, decoded);
        assertNull(decoded.texture());
        assertNull(decoded.regionId());
        assertEquals(List.of(), decoded.traitConditionIds());
    }

    @Test
    void damagedTextDecodesToNothing() {
        assertNull(PhasedPlanetRecord.decode(null));
        assertNull(PhasedPlanetRecord.decode(""));
        assertNull(PhasedPlanetRecord.decode("not a record"));
        assertNull(PhasedPlanetRecord.decode("{\"name\":\"No type\"}"));
    }
}
