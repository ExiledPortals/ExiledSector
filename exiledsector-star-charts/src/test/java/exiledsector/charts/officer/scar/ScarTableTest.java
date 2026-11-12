package exiledsector.charts.officer.scar;

import exiledsector.skills.skilleffect.scar.ScarSkillEffect;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScarTableTest {

    private static final int SHIPPED_SCARS = 49;

    @Test
    void theShippedTableLoadsEveryScarWithoutProblems() throws Exception {
        assertEquals(List.of(), ScarFixtures.loadShipped());
        assertEquals(SHIPPED_SCARS, ScarTable.rows().size());
        assertTrue(ScarTable.exists(ScarTable.FALLBACK_SCAR_ID));
        assertNull(ScarTable.row(ScarTable.FALLBACK_SCAR_ID).detector());
    }

    @Test
    void rowsKeepTheTableOrder() throws Exception {
        ScarFixtures.loadShipped();
        List<ScarRow> rows = ScarTable.rows();
        assertEquals("flak_scarred", rows.get(0).id());
        assertEquals("torpedo_burned", rows.get(1).id());
        assertEquals(ScarTable.FALLBACK_SCAR_ID, rows.get(rows.size() - 1).id());
    }

    @Test
    void everyShippedScarButTheFallbackHasADetectorAndThreshold() throws Exception {
        ScarFixtures.loadShipped();
        for (ScarRow row : ScarTable.rows()) {
            if (!ScarTable.FALLBACK_SCAR_ID.equals(row.id())) {
                assertNotNull(row.detector(), row.id());
                assertTrue(row.threshold() > 0f, row.id());
            }
        }
    }

    @Test
    void everyShippedScarHasADModToTake() throws Exception {
        ScarFixtures.loadShipped();
        for (ScarRow row : ScarTable.rows()) {
            assertTrue(row.randomDMod() || !row.dModIds().isEmpty(), row.id());
        }
    }

    @Test
    void shippedScarsUseOnlyScarEffects() throws Exception {
        ScarFixtures.loadShipped();
        Set<String> scarEffectNames = new HashSet<>();
        for (ScarSkillEffect effect : ScarSkillEffect.values()) {
            scarEffectNames.add(effect.name());
        }
        for (ScarRow row : ScarTable.rows()) {
            for (ScarRow.Effect entry : row.effects()) {
                assertTrue(scarEffectNames.contains(entry.effect().name()), row.id() + " " + entry.effect().name());
            }
        }
    }

    @Test
    void anEffectEntryReadsFeatureEffectAndMagnitude() {
        ScarRow.Effect effect = ScarTable.effect("shields:SCAR_SHIELD_DAMAGE_TAKEN_PERCENT:-8");
        assertNotNull(effect);
        assertEquals(HullFeature.SHIELDS, effect.feature());
        assertEquals(ScarSkillEffect.SCAR_SHIELD_DAMAGE_TAKEN_PERCENT, effect.effect());
        assertEquals(-8f, effect.magnitudePerStack());
    }

    @Test
    void malformedEffectEntriesAreRejected() {
        assertNull(ScarTable.effect("shields:SCAR_SHIELD_DAMAGE_TAKEN_PERCENT"));
        assertNull(ScarTable.effect("wings:SCAR_SHIELD_DAMAGE_TAKEN_PERCENT:5"));
        assertNull(ScarTable.effect("any:NO_SUCH_EFFECT:5"));
        assertNull(ScarTable.effect("any:SCAR_SHIELD_DAMAGE_TAKEN_PERCENT:lots"));
    }

    @Test
    void aRowWithAnUnknownDetectorIsSkippedAndReported() throws Exception {
        try {
            JSONArray rows = new JSONArray()
                    .put(new JSONObject().put("id", "made_up").put("detector", "no_such_detector").put("threshold", "1"))
                    .put(new JSONObject().put("id", ScarTable.FALLBACK_SCAR_ID).put("dmods", "random"));
            assertFalse(ScarTable.register(rows).isEmpty());
            assertFalse(ScarTable.exists("made_up"));
            assertTrue(ScarTable.exists(ScarTable.FALLBACK_SCAR_ID));
        } finally {
            ScarFixtures.loadShipped();
        }
    }

    @Test
    void aTableWithoutTheFallbackIsReported() throws Exception {
        try {
            JSONArray rows = new JSONArray()
                    .put(new JSONObject().put("id", "overloaded").put("detector", "overloads").put("threshold", "1"));
            assertFalse(ScarTable.register(rows).isEmpty());
        } finally {
            ScarFixtures.loadShipped();
        }
    }

    @Test
    void theRandomDModEntryIsAFlagNotAnId() throws Exception {
        ScarFixtures.loadShipped();
        ScarRow coreHunter = ScarTable.row("core_hunter");
        assertEquals(List.of("faulty_auto"), coreHunter.dModIds());
        assertTrue(coreHunter.randomDMod());
        assertFalse(ScarTable.row("flak_scarred").randomDMod());
    }
}
