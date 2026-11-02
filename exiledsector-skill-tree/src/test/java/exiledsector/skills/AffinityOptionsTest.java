package exiledsector.skills;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffinityOptionsTest {

    @Test
    void eachOldSelectionIsMappedOnceAndTheRestAreLeftAlone() {
        ShipSkillData data = new ShipSkillData();
        data.optionalSelections.put("affinity_1", "hull");
        data.optionalSelections.put("affinity_2", "flux_capacity");
        data.optionalSelections.put("affinity_3", "midline_affinity");
        data.optionalSelections.put("shield_node", "shield_turn_rate");
        int revisionBefore = data.revision();

        assertTrue(AffinityOptions.migrate(data));
        assertEquals("low_tech_affinity", data.getOptionalSelection("affinity_1"));
        assertEquals("high_tech_affinity", data.getOptionalSelection("affinity_2"));
        assertEquals("midline_affinity", data.getOptionalSelection("affinity_3"));
        assertEquals("shield_turn_rate", data.getOptionalSelection("shield_node"));
        assertTrue(data.revision() > revisionBefore);

        assertFalse(AffinityOptions.migrate(data));
        assertEquals(1, AffinityOptions.migrateAll(List.of(data, oldShip())));
    }

    private static ShipSkillData oldShip() {
        ShipSkillData data = new ShipSkillData();
        data.optionalSelections.put("affinity_1", "fuel_crew_cargo");
        return data;
    }
}
