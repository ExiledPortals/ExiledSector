package exiledsector.persistence;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeTemplateStoreTest {

    private static final List<TemplateStep> STEPS = List.of(new TemplateStep("armor_1", null), new TemplateStep("slot_2", "hull"));

    private MockedStatic<Global> globalMock;
    private Map<String, Object> persistentData;
    private Iterator<String> ids;

    @BeforeEach
    void setUp() {
        persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        ids = List.of("t1", "t2", "t3").iterator();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private SkillTreeTemplate save(String name, String root) {
        return SkillTreeTemplateStore.save(name, root, HullSize.CRUISER, STEPS, ids::next);
    }

    @Test
    void readingAnEmptyStoreCreatesNothingInTheSave() {
        assertTrue(SkillTreeTemplateStore.all().isEmpty());
        assertNull(SkillTreeTemplateStore.find("t1"));
        assertNull(SkillTreeTemplateStore.assignedTo("ship-a", "root_low_tech_1"));
        SkillTreeTemplateStore.clearAssignment("ship-a");
        SkillTreeTemplateStore.pruneAssignments(id -> true);

        assertTrue(persistentData.isEmpty());
    }

    @Test
    void savedTemplatesAreStoredAsPlainStringsAndReadBackInOrder() {
        SkillTreeTemplate first = save("  Brawler ", "root_low_tech_1");
        save("Kite", "root_low_tech_1");

        Object stored = persistentData.get("exiledSector_skillTreeTemplates");
        assertInstanceOf(Map.class, stored);
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) stored).entrySet()) {
            assertInstanceOf(String.class, entry.getKey());
            assertInstanceOf(String.class, entry.getValue());
        }
        assertEquals("Brawler", first.name());
        assertEquals(List.of("t1", "t2"), SkillTreeTemplateStore.all().stream().map(SkillTreeTemplate::id).toList());
        assertEquals(first, SkillTreeTemplateStore.find("t1"));
    }

    @Test
    void aShipsAssignmentOnlyCountsWhileTheTemplateExistsAndMatchesItsRoot() {
        save("Brawler", "root_low_tech_1");
        SkillTreeTemplateStore.assign("ship-a", "t1");

        assertEquals("t1", SkillTreeTemplateStore.assignedTo("ship-a", "root_low_tech_1").id());
        assertNull(SkillTreeTemplateStore.assignedTo("ship-a", "root_high_tech_1"));
        assertNull(SkillTreeTemplateStore.assignedTo("ship-b", "root_low_tech_1"));

        SkillTreeTemplateStore.clearAssignment("ship-a");
        assertNull(SkillTreeTemplateStore.assignedTo("ship-a", "root_low_tech_1"));
    }

    @Test
    void deletingATemplateRemovesEveryAssignmentToIt() {
        save("Brawler", "root_low_tech_1");
        save("Kite", "root_low_tech_1");
        SkillTreeTemplateStore.assign("ship-a", "t1");
        SkillTreeTemplateStore.assign("ship-b", "t1");
        SkillTreeTemplateStore.assign("ship-c", "t2");

        assertTrue(SkillTreeTemplateStore.delete("t1"));
        assertFalse(SkillTreeTemplateStore.delete("t1"));

        assertNull(SkillTreeTemplateStore.find("t1"));
        assertEquals(Map.of("ship-c", "t2"), persistentData.get("exiledSector_skillTreeTemplateAssignments"));
    }

    @Test
    void pruningDropsAssignmentsToMissingTemplatesAndShipsNoLongerNeeded() {
        save("Brawler", "root_low_tech_1");
        SkillTreeTemplateStore.assign("kept", "t1");
        SkillTreeTemplateStore.assign("gone-ship", "t1");
        SkillTreeTemplateStore.assign("dangling", "deleted-template");

        SkillTreeTemplateStore.pruneAssignments(Set.of("kept", "dangling")::contains);

        assertEquals(Map.of("kept", "t1"), persistentData.get("exiledSector_skillTreeTemplateAssignments"));
    }

    @Test
    void aTemplateThatCannotBeReadIsSkipped() {
        save("Brawler", "root_low_tech_1");
        Map<String, String> stored = new HashMap<>();
        stored.put("broken", "{not json");
        persistentData.put("exiledSector_skillTreeTemplates", stored);

        assertTrue(SkillTreeTemplateStore.all().isEmpty());
        assertNull(SkillTreeTemplateStore.find("broken"));
    }

    @Test
    void editingATemplateInPlaceKeepsItsNameAndRenamingKeepsItsSteps() {
        save("Brawler", "root_low_tech_1");
        List<TemplateStep> newSteps = List.of(new TemplateStep("armor_1", null));

        SkillTreeTemplate edited = SkillTreeTemplateStore.replaceSteps("t1", newSteps);
        SkillTreeTemplate renamed = SkillTreeTemplateStore.rename("t1", "  Kite ");

        assertEquals("Brawler", edited.name());
        assertEquals(newSteps, SkillTreeTemplateStore.find("t1").steps());
        assertEquals("Kite", renamed.name());
        assertEquals("Kite", SkillTreeTemplateStore.find("t1").name());
        assertNull(SkillTreeTemplateStore.replaceSteps("missing", newSteps));
        assertNull(SkillTreeTemplateStore.replaceSteps(null, newSteps));
    }

    @Test
    void assignmentCountCountsEveryShipUsingATemplate() {
        save("Brawler", "root_low_tech_1");
        assertEquals(0, SkillTreeTemplateStore.assignmentCount("t1"));
        SkillTreeTemplateStore.assign("ship-a", "t1");
        SkillTreeTemplateStore.assign("ship-b", "t1");

        assertEquals(2, SkillTreeTemplateStore.assignmentCount("t1"));
        assertEquals(0, SkillTreeTemplateStore.assignmentCount(null));
    }
}
