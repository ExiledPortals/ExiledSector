package exiledsector.skills;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ClearedSkillTreeExtension.class)
class OptionRenamesTest {

    @AfterEach
    void tearDown() {
        NodeReplacements.clear();
    }

    @Test
    void eachSelectionOnARenamedTypeMovesToItsNewIdOnceAndTheRestAreLeftAlone() throws Exception {
        SkillTree.registerType(new SkillType.Builder("new_option", "New", "", SkillTier.SMALL).build());
        SkillTree.registerType(new SkillType.Builder("kept_option", "Kept", "", SkillTier.SMALL).build());
        NodeReplacements.registerTypeRenames(new JSONArray().put(new JSONObject().put("old", "old_option").put("new", "new_option")));
        ShipSkillData data = new ShipSkillData();
        data.choiceSelections.put("choice_1", "old_option");
        data.choiceSelections.put("choice_2", "kept_option");
        int revisionBefore = data.revision();

        assertTrue(OptionRenames.apply(data));
        assertEquals("new_option", data.getChoiceSelection("choice_1"));
        assertEquals("kept_option", data.getChoiceSelection("choice_2"));
        assertTrue(data.revision() > revisionBefore);

        assertFalse(OptionRenames.apply(data));
        assertFalse(OptionRenames.apply(null));
        ShipSkillData otherShip = new ShipSkillData();
        otherShip.choiceSelections.put("choice_1", "old_option");
        assertEquals(1, OptionRenames.applyAll(List.of(data, otherShip)));
    }
}
