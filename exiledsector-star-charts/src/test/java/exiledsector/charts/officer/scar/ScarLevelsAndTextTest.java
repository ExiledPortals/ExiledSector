package exiledsector.charts.officer.scar;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScarLevelsAndTextTest {

    @BeforeEach
    void loadScars() throws Exception {
        ScarFixtures.loadShipped();
    }

    @Test
    void eachStackAddsALevelPlusTheRowsExtraLevels() {
        Map<String, Integer> scars = new LinkedHashMap<>();
        scars.put("overloaded", 2);
        scars.put("abandoned", 1);
        scars.put(ScarTable.FALLBACK_SCAR_ID, 3);
        assertEquals(2 + 4 + 9, ScarLevels.extraLevels(scars));
    }

    @Test
    void unknownScarsStillCountOneLevelPerStack() {
        assertEquals(2, ScarLevels.extraLevels(Map.of("retired_scar", 2), scarId -> null));
        assertEquals(0, ScarLevels.extraLevels(null));
    }

    @Test
    void everyScarHasAName() {
        for (ScarRow row : ScarTable.rows()) {
            assertTrue(Translation.has(ScarText.NAME_KEY_PREFIX + row.id()), row.id());
        }
        assertEquals("Flux-Pinned", ScarText.name("flux_pinned"));
        assertEquals("retired_scar", ScarText.name("retired_scar"));
    }

    @Test
    void theEpithetUsesTheMostStackedScarAndTheFirstOnTies() {
        Map<String, Integer> scars = new LinkedHashMap<>();
        scars.put("kited", 1);
        scars.put("rammed", 2);
        scars.put("overloaded", 2);
        assertEquals("rammed", ScarText.mostStacked(scars));
        assertEquals("Vera Sollis the Rammed", ScarText.epithet("Vera Sollis", scars));
        assertEquals("Vera Sollis", ScarText.epithet("Vera Sollis", Map.of()));
        assertNull(ScarText.mostStacked(null));
    }

    @Test
    void tooltipLinesNameEachScarWithItsStacksAndFittingEffects() {
        Map<String, Integer> scars = new LinkedHashMap<>();
        scars.put("kinetic_scarred", 2);
        scars.put("humbled", 1);
        List<StyledText> lines = ScarText.tooltipLines(scars, ScarFixtures.UNSHIELDED_WARSHIP);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).plain().startsWith("Kinetic-Scarred x2: "), lines.get(0).plain());
        assertTrue(lines.get(0).plain().contains("20"), lines.get(0).plain());
        assertEquals("Humbled", lines.get(1).plain());
    }
}
