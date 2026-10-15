package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.template.SkillTreeTemplate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateUiStateTest {

    @Test
    void theBarIsHiddenUntilAStartingRootIsChosen() {
        assertFalse(TemplateBarState.of(false, 5, true, false, false, false, false).visible());
        assertTrue(TemplateBarState.of(true, 1, false, false, false, false, false).visible());
    }

    @Test
    void saveNeedsANodeBeyondTheRootAndExplainsWhyWhenDisabled() {
        TemplateBarState rootOnly = TemplateBarState.of(true, 1, false, false, false, false, false);
        TemplateBarState withNodes = TemplateBarState.of(true, 4, false, false, false, false, false);

        assertFalse(rootOnly.saveEnabled());
        assertEquals("ui.template.hint.saveEmpty", rootOnly.saveHintKey());
        assertTrue(withNodes.saveEnabled());
        assertEquals("ui.template.hint.save", withNodes.saveHintKey());
    }

    @Test
    void theAutoAllocateToggleNeedsATemplateToTurnOnButCanAlwaysBeTurnedOff() {
        TemplateBarState noTemplate = TemplateBarState.of(true, 4, false, false, false, false, false);
        assertFalse(noTemplate.autoEnabled());
        assertEquals("ui.template.hint.autoNoTemplate", noTemplate.autoHintKey());
        assertTrue(TemplateBarState.of(true, 4, false, false, false, false, true).autoEnabled());

        TemplateBarState off = TemplateBarState.of(true, 4, true, false, false, false, false);
        TemplateBarState on = TemplateBarState.of(true, 4, true, false, false, false, true);
        assertTrue(off.autoEnabled());
        assertEquals("ui.template.hint.autoOff", off.autoHintKey());
        assertTrue(on.autoOn());
        assertEquals("ui.template.hint.autoOn", on.autoHintKey());

        TemplateBarState running = TemplateBarState.of(true, 4, true, true, false, false, true);
        assertFalse(running.autoEnabled());
        assertFalse(running.saveEnabled());
        assertFalse(running.loadEnabled());
        assertFalse(running.templateModeEnabled());
    }

    @Test
    void templateModeTurnsSaveIntoRenameAndLocksTheToggle() {
        TemplateBarState newPlan = TemplateBarState.of(true, 4, false, false, false, true, false);
        TemplateBarState editing = TemplateBarState.of(true, 4, true, false, false, true, true);

        assertTrue(newPlan.planning());
        assertTrue(newPlan.templateModeEnabled());
        assertEquals("ui.template.button.rename", newPlan.saveLabelKey());
        assertFalse(newPlan.saveEnabled());
        assertEquals("ui.template.hint.renameEmpty", newPlan.saveHintKey());
        assertTrue(editing.saveEnabled());
        assertEquals("ui.template.hint.rename", editing.saveHintKey());
        assertTrue(editing.loadEnabled());
        assertFalse(editing.autoEnabled());
        assertEquals("ui.template.hint.autoPlanning", editing.autoHintKey());
    }

    @Test
    void deletingNeedsASecondClickOnTheSameTemplateBeforeItTimesOut() {
        DeleteConfirmation confirmation = new DeleteConfirmation();

        assertFalse(confirmation.click("a"));
        assertTrue(confirmation.isArmed("a"));
        assertFalse(confirmation.click("b"));
        assertFalse(confirmation.isArmed("a"));
        assertTrue(confirmation.click("b"));

        assertFalse(confirmation.click("c"));
        confirmation.advance(DeleteConfirmation.ARM_SECONDS + 0.1f);
        assertFalse(confirmation.isArmed("c"));
        assertFalse(confirmation.click("c"));
    }

    private static List<SkillTreeTemplate> templates(int count, HullSize hullSize) {
        List<SkillTreeTemplate> templates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            templates.add(new SkillTreeTemplate("id" + i, String.format("t%02d", i), "root", hullSize, List.of()));
        }
        return templates;
    }

    @Test
    void theListStartsOnTheShipsHullSizeAndChipsToggleOtherSizes() {
        List<SkillTreeTemplate> all = new ArrayList<>(templates(2, HullSize.FRIGATE));
        all.add(new SkillTreeTemplate("cruiser", "c", "root", HullSize.CRUISER, List.of()));
        TemplateListState state = new TemplateListState(all, "root", HullSize.CRUISER);

        assertEquals(List.of("cruiser"), state.shown().stream().map(SkillTreeTemplate::id).toList());
        state.toggle(HullSize.FRIGATE);
        assertEquals(3, state.shown().size());
        state.toggle(HullSize.CRUISER);
        assertEquals(2, state.shown().size());
        assertTrue(state.hasAnyForRoot());
        assertFalse(new TemplateListState(all, "other_root", HullSize.CRUISER).hasAnyForRoot());
    }

    @Test
    void anInertBarGreysOutEveryButtonButKeepsItsHints() {
        TemplateBarState inert = TemplateBarState.of(true, 4, true, false, true, false, false);
        TemplateBarState live = TemplateBarState.of(true, 4, true, false, false, false, false);

        assertTrue(live.saveEnabled() && live.loadEnabled() && live.autoEnabled());
        assertTrue(inert.visible());
        assertFalse(inert.saveEnabled());
        assertFalse(inert.loadEnabled());
        assertFalse(inert.autoEnabled());
        assertFalse(inert.templateModeEnabled());
        assertEquals(live.saveHintKey(), inert.saveHintKey());
        assertEquals(live.autoHintKey(), inert.autoHintKey());
    }

    @Test
    void scrollingIsClampedToTheRowsThatExist() {
        TemplateListState state = new TemplateListState(templates(10, HullSize.CRUISER), "root", HullSize.CRUISER);

        state.scroll(-3, 4);
        assertEquals(0, state.scrollOffset());
        state.scroll(100, 4);
        assertEquals(6, state.scrollOffset());
        assertEquals(List.of("id6", "id7", "id8", "id9"), state.window(4).stream().map(SkillTreeTemplate::id).toList());
        assertEquals(10, state.window(20).size());
        assertEquals(0, state.scrollOffset());
    }
}
