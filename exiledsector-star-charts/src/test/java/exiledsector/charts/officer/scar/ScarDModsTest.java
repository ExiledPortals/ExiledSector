package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScarDModsTest {

    private static final Map<String, Set<String>> TAGS = Map.of(
            "comp_armor", Set.of(Tags.HULLMOD_DMOD, ScarDMods.DAMAGE),
            "comp_hull", Set.of(Tags.HULLMOD_DMOD, ScarDMods.DAMAGE),
            "comp_structure", Set.of(Tags.HULLMOD_DMOD, ScarDMods.DAMAGE, ScarDMods.DAMAGE_STRUCT),
            "damaged_struts", Set.of(Tags.HULLMOD_DMOD, ScarDMods.DAMAGE, ScarDMods.DAMAGE_STRUCT),
            "degraded_shields", Set.of(Tags.HULLMOD_DMOD, ScarDMods.DAMAGE, ScarDMods.REQ_SHIELDS),
            "damaged_deck", Set.of(Tags.HULLMOD_DMOD, ScarDMods.FIGHTER_BAY_DAMAGE),
            "fragile_subsystems", Set.of(Tags.HULLMOD_DMOD, ScarDMods.DAMAGE, ScarDMods.PEAK_TIME),
            "faulty_auto", Set.of(Tags.HULLMOD_DMOD),
            "glitched_sensors", Set.of(Tags.HULLMOD_DMOD, ScarDMods.DAMAGE, ScarDMods.NOT_AUTO),
            "increased_maintenance", Set.of(Tags.HULLMOD_DMOD, ScarDMods.CIV_ONLY));
    private static final Function<String, Set<String>> TAGS_OF = TAGS::get;
    private static final List<String> POOL = List.copyOf(TAGS.keySet()).stream().sorted().toList();

    private static ScarRow dModRow(boolean random, String... dModIds) {
        return new ScarRow("test", ScarDetector.OVERLOADS, 1f, "", List.of(), List.of(dModIds), random, 0);
    }

    private static String pick(ScarRow row, List<String> held, HullFacts hull) {
        return ScarDMods.pick(row, held, hull, TAGS_OF, POOL, new Random(7L));
    }

    @Test
    void theListedDModsAreTakenInOrder() {
        ScarRow row = dModRow(false, "degraded_shields", "comp_armor");
        assertEquals("degraded_shields", pick(row, List.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals("comp_armor", pick(row, List.of("degraded_shields"), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aDModTheHullCannotTakeIsSkipped() {
        ScarRow row = dModRow(false, "degraded_shields", "comp_armor");
        assertEquals("comp_armor", pick(row, List.of(), ScarFixtures.UNSHIELDED_WARSHIP));
        assertEquals("comp_armor", pick(dModRow(false, "damaged_deck", "comp_armor"), List.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals("damaged_deck", pick(dModRow(false, "damaged_deck", "comp_armor"), List.of(), ScarFixtures.OMNI_CARRIER));
        assertEquals("comp_armor", pick(dModRow(false, "fragile_subsystems", "comp_armor"), List.of(), ScarFixtures.AUTOMATED_TIMELESS));
        assertEquals("comp_armor", pick(dModRow(false, "glitched_sensors", "comp_armor"), List.of(), ScarFixtures.AUTOMATED_TIMELESS));
    }

    @Test
    void aNewHullKeepsOnlyTheDModsItCanTake() {
        List<String> held = List.of("degraded_shields", "comp_structure", "damaged_struts", "comp_armor", "damaged_deck");
        assertEquals(List.of("comp_structure", "comp_armor"), ScarDMods.suiting(held, ScarFixtures.UNSHIELDED_WARSHIP, TAGS_OF));
        assertEquals(List.of(), ScarDMods.suiting(held, null, TAGS_OF));
    }

    @Test
    void onlyOneStructuralDModIsTaken() {
        ScarRow row = dModRow(false, "comp_structure", "comp_hull");
        assertEquals("comp_hull", pick(row, List.of("damaged_struts"), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void civilianOnlyDModsStayOffWarships() {
        assertEquals("comp_hull", pick(dModRow(false, "increased_maintenance", "comp_hull"), List.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void whenTheListIsSpentARandomDModIsTaken() {
        ScarRow row = dModRow(true, "comp_armor");
        String picked = pick(row, List.of("comp_armor"), ScarFixtures.UNSHIELDED_WARSHIP);
        assertTrue(Set.of("comp_hull", "comp_structure", "damaged_struts", "fragile_subsystems", "glitched_sensors").contains(picked), picked);
    }

    @Test
    void theRandomPoolHonoursTheHull() {
        ScarRow row = dModRow(true);
        for (int seed = 0; seed < 50; seed++) {
            String picked = ScarDMods.pick(row, List.of("comp_armor", "comp_hull", "comp_structure"), ScarFixtures.UNSHIELDED_WARSHIP,
                    TAGS_OF, POOL, new Random(seed));
            assertTrue(Set.of("fragile_subsystems", "glitched_sensors").contains(picked), picked);
        }
    }

    @Test
    void aSpentListFallsBackToAnyApplyingDMod() {
        String picked = pick(dModRow(false, "comp_armor"), List.of("comp_armor"), ScarFixtures.SHIELDED_WARSHIP);
        assertTrue(TAGS.containsKey(picked) && !"comp_armor".equals(picked), picked);
    }

    @Test
    void aShipWithFiveDModsTakesNoMore() {
        List<String> five = List.of("comp_armor", "comp_hull", "comp_structure", "degraded_shields", "glitched_sensors");
        assertEquals(ScarDMods.MAX_DMODS, five.size());
        assertNull(pick(dModRow(true, "fragile_subsystems"), five, ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void noHullMeansNoDMod() {
        assertNull(pick(dModRow(true, "comp_armor"), List.of(), null));
    }

    @Test
    void aMissingRowStillTakesARandomDMod() {
        assertTrue(TAGS.containsKey(pick(null, List.of(), ScarFixtures.SHIELDED_WARSHIP)));
    }
}
