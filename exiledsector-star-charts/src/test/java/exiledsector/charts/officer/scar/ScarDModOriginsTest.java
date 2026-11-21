package exiledsector.charts.officer.scar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScarDModOriginsTest {

    @BeforeEach
    void loadScars() throws Exception {
        ScarFixtures.loadShipped();
    }

    private static Map<String, Integer> scars(Object... scarsAndStacks) {
        Map<String, Integer> scars = new LinkedHashMap<>();
        for (int index = 0; index < scarsAndStacks.length; index += 2) {
            scars.put((String) scarsAndStacks[index], (Integer) scarsAndStacks[index + 1]);
        }
        return scars;
    }

    @Test
    void recordedOriginsPlaceEachDModUnderItsScar() {
        Map<String, List<String>> byScar = ScarDModOrigins.byScar(scars("flak_scarred", 1, ScarTable.FALLBACK_SCAR_ID, 1),
                List.of("damaged_mounts", "faulty_grid"), Map.of("damaged_mounts", ScarTable.FALLBACK_SCAR_ID, "faulty_grid", "flak_scarred"));

        assertEquals(List.of("faulty_grid"), byScar.get("flak_scarred"));
        assertEquals(List.of("damaged_mounts"), byScar.get(ScarTable.FALLBACK_SCAR_ID));
    }

    @Test
    void anUnrecordedDModGoesToTheScarNamingItThenToTheOnlyScarLeftWithoutOne() {
        Map<String, List<String>> byScar = ScarDModOrigins.byScar(scars(ScarTable.FALLBACK_SCAR_ID, 1, "flak_scarred", 1),
                List.of("faulty_grid", "damaged_mounts"), Map.of());

        assertEquals(List.of("damaged_mounts"), byScar.get("flak_scarred"));
        assertEquals(List.of("faulty_grid"), byScar.get(ScarTable.FALLBACK_SCAR_ID));
    }

    @Test
    void anUnrecordedDModStaysUnplacedWhenSeveralScarsCouldHaveAddedIt() {
        Map<String, List<String>> byScar = ScarDModOrigins.byScar(scars(ScarTable.FALLBACK_SCAR_ID, 1, "humbled", 1),
                List.of("faulty_grid"), Map.of());

        assertEquals(List.of(), byScar.get(ScarTable.FALLBACK_SCAR_ID));
        assertEquals(List.of(), byScar.get("humbled"));
    }

    @Test
    void aScarHoldsNoMoreUnrecordedDModsThanItHasStacks() {
        Map<String, List<String>> byScar = ScarDModOrigins.byScar(scars(ScarTable.FALLBACK_SCAR_ID, 1),
                List.of("faulty_grid", "comp_armor"), Map.of());

        assertEquals(List.of("faulty_grid"), byScar.get(ScarTable.FALLBACK_SCAR_ID));
    }
}
