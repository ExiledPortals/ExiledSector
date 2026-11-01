package exiledsector.skills;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullModConflictsTest {

    private static JSONObject row(String hullModId, String conflicts) throws JSONException {
        return new JSONObject().put(HullModConflicts.HULL_MOD_COLUMN, hullModId).put(HullModConflicts.CONFLICTS_COLUMN, conflicts);
    }

    private static HullModConflicts conflicts(JSONObject... rows) throws JSONException {
        return HullModConflicts.fromRows(new JSONArray(List.of(rows)));
    }

    private static SkillType.Builder type(String id) {
        return new SkillType.Builder(id, id, "a.png", SkillTier.NOTABLE);
    }

    @Test
    void aRowConflictsBothWaysWithEveryHullModItLists() throws JSONException {
        HullModConflicts conflicts = conflicts(row("their_shield_mod", "frontemitter; adaptiveshields"));

        assertEquals(Set.of("frontemitter", "adaptiveshields"), conflicts.conflictsOf("their_shield_mod"));
        assertEquals(Set.of("their_shield_mod"), conflicts.conflictsOf("frontemitter"));
        assertEquals(Set.of("their_shield_mod"), conflicts.conflictsOf("adaptiveshields"));
    }

    @Test
    void rowsAboutTheSameHullModAddUp() throws JSONException {
        HullModConflicts conflicts = conflicts(row("first_mod", "heavyarmor"), row("second_mod", "heavyarmor"));

        assertEquals(Set.of("first_mod", "second_mod"), conflicts.conflictsOf("heavyarmor"));
    }

    @Test
    void blankEntriesSelfConflictsAndRowsWithoutAHullModAreIgnored() throws JSONException {
        HullModConflicts conflicts = conflicts(row("their_mod", " ; their_mod ;  heavyarmor ; "), row("", "frontemitter"));

        assertEquals(Set.of("heavyarmor"), conflicts.conflictsOf("their_mod"));
        assertTrue(conflicts.conflictsOf("frontemitter").isEmpty());
    }

    @Test
    void aNodeExcludesTheHullModsItsOwnHullModConflictsWith() throws JSONException {
        HullModConflicts conflicts = conflicts(row("their_shield_mod", "adaptiveshields"));
        SkillType omni = type("adaptiveshields").phantomHullModIds(List.of("adaptiveshields")).hullModConflicts(conflicts).build();

        assertEquals(List.of("their_shield_mod", "adaptiveshields"), omni.getExclusiveAndOwnHullModIds());
    }

    @Test
    void twoNodesWhoseHullModsConflictAreExclusiveWithEachOther() throws JSONException {
        HullModConflicts conflicts = conflicts(row("frontemitter", "adaptiveshields"));
        SkillType front = type("frontemitter").phantomHullModIds(List.of("frontemitter")).hullModConflicts(conflicts).build();
        SkillType omni = type("adaptiveshields").phantomHullModIds(List.of("adaptiveshields")).hullModConflicts(conflicts).build();
        SkillType armor = type("heavyarmor").phantomHullModIds(List.of("heavyarmor")).hullModConflicts(conflicts).build();

        assertTrue(front.isExclusiveWith(omni));
        assertTrue(omni.isExclusiveWith(front));
        assertFalse(front.isExclusiveWith(armor));
    }

    @Test
    void aNodeWithoutAHullModOfItsOwnTakesTheConflictsOfTheHullModsItStandsIn() throws JSONException {
        HullModConflicts conflicts = conflicts(row("their_sensor_mod", "hiressensors"));
        SkillType sensors = type("hiressensors").exclusiveHullModIds(List.of("hiressensors")).hullModConflicts(conflicts).build();

        assertEquals(List.of("hiressensors", "their_sensor_mod"), sensors.getExclusiveAndOwnHullModIds());
        assertEquals("hiressensors", sensors.getEquivalentHullModId());
    }
}
