package exiledsector.charts.officer.scar;

import exiledsector.i18n.Style;
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
        List<StyledText> lines = ScarText.tooltipLines(scars, Map.of("humbled", List.of("faulty_grid")), ScarFixtures.UNSHIELDED_WARSHIP);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).plain().startsWith("Kinetic-Scarred x2: "), lines.get(0).plain());
        assertEquals("Kinetic-Scarred x2: 20% reduced kinetic damage taken, +2 to flagship level", lines.get(0).plain());
        assertEquals("Humbled: +5 to flagship level, faulty_grid", lines.get(1).plain());
    }

    @Test
    void scarEffectsReadAsListItemsWithoutNamingTheShip() {
        HullFacts everyFeature = new HullFacts(true, true, true, true, true, true, true, true, true, true);
        for (ScarRow row : ScarTable.rows()) {
            String line = ScarText.tooltipLines(Map.of(row.id(), 1), Map.of(), everyFeature).get(0).plain();
            assertTrue(!line.replace("strikecraft launched from this ship", "").contains("this ship") &&!line.contains(".,") && !line.contains(", I") && !line.contains(", T"), line);
        }
        StyledText item = ScarText.listItem(StyledText.parse("Strikecraft launched from this ship deal <good>15%</good> increased damage."));
        assertEquals("strikecraft launched from this ship deal <good>15%</good> increased damage", item.toMarkup());
        assertEquals("<good>15%</good> reduced EMP damage taken", ScarText.listItem(StyledText.parse("<good>15%</good> reduced EMP damage taken.")).toMarkup());
    }

    @Test
    void everyScarEffectIsColouredAsBadForThePlayer() {
        for (ScarRow row : ScarTable.rows()) {
            for (ScarRow.Effect effect : row.effects()) {
                StyledText line = ScarText.playerView(effect.effect(), effect.magnitudePerStack());
                String label = row.id() + " " + effect.effect() + ": " + line.toMarkup();
                assertTrue(line.spans().stream().noneMatch(span -> span.style() == Style.GOOD), label);
                assertTrue(line.spans().stream().anyMatch(span -> span.style() == Style.BAD), label);
            }
        }
    }
}
