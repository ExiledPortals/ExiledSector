package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.Translation;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.SkillNode;
import exiledsector.skills.template.AutoAllocateRun;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.ui.node.TreeAllocationSession;
import org.lwjgl.input.Keyboard;

import java.util.Objects;

final class SkillTreeTemplateController {

    private final FleetMemberAPI member;
    private final TreeAllocationSession treeSession;
    private final SkillTreeTemplateBar templateBar;
    private final SkillTreeTemplateNameDialog nameDialog = new SkillTreeTemplateNameDialog();
    private final SkillTreeTemplateListOverlay listOverlay;

    private TemplateBarState barState = TemplateBarState.HIDDEN;
    private TemplateAction pendingAction;
    private String templateRootId;

    SkillTreeTemplateController(FleetMemberAPI member, TreeAllocationSession treeSession, SkillTreePanelStyle style) {
        this.member = member;
        this.treeSession = treeSession;
        this.templateBar = new SkillTreeTemplateBar(style);
        this.listOverlay = new SkillTreeTemplateListOverlay(style);
        loadAssignedTemplateOnRootChange();
    }

    boolean isModalOpen() {
        return nameDialog.isBlocking() || listOverlay.isBlocking();
    }

    boolean barContains(float x, float y) {
        return templateBar.contains(x, y);
    }

    private void loadAssignedTemplateOnRootChange() {
        SkillNode startingRoot = treeSession.getStartingRoot();
        String startingRootId = startingRoot == null ? null : startingRoot.getId();
        if (Objects.equals(startingRootId, templateRootId)) {
            return;
        }
        templateRootId = startingRootId;
        if (startingRootId != null) {
            treeSession.setTemplate(SkillTreeTemplateStore.assignedTo(member.getId(), startingRootId));
        }
    }

    void advance(float amount, PositionAPI canvasPosition, boolean barLive) {
        loadAssignedTemplateOnRootChange();
        SkillTreeTemplate template = treeSession.template();
        boolean running = treeSession.isAutoAllocating();
        boolean pointsLeft = template != null && treeSession.hasPointsLeft();
        barState = TemplateBarState.of(templateRootId != null, treeSession.allocatedNodeCount(),
                template != null, running, pointsLeft, !barLive);
        templateBar.update(barState, template == null ? null : template.name());
        AutoAllocateRun.Summary summary = treeSession.takeLastRunSummary();
        if (summary != null) {
            templateBar.showResult(resultText(summary));
        }
        templateBar.advance(amount);
        nameDialog.advance(amount);
        listOverlay.advance(amount);
        if (canvasPosition != null) {
            templateBar.layout(canvasPosition);
        }
    }

    void forgetBarPress() {
        if (!isModalOpen()) {
            pendingAction = null;
        }
    }

    boolean handleLmbDown(float x, float y) {
        if (isModalOpen()) {
            pendingAction = modalActionAt(x, y);
            return true;
        }
        TemplateAction action = templateBar.actionAt(x, y);
        if (action == null) {
            return false;
        }
        pendingAction = action;
        return true;
    }

    boolean handleLmbUp(float x, float y) {
        if (pendingAction == null) {
            return isModalOpen();
        }
        TemplateAction pressed = pendingAction;
        pendingAction = null;
        TemplateAction released = isModalOpen() ? modalActionAt(x, y) : templateBar.actionAt(x, y);
        if (pressed.equals(released)) {
            perform(pressed);
        }
        return true;
    }

    boolean handleKey(InputEventAPI event) {
        if (nameDialog.isOpen()) {
            SkillTreeTextField.KeyResult result = nameDialog.handleKey(event);
            if (result == SkillTreeTextField.KeyResult.SUBMIT) {
                perform(TemplateAction.of(TemplateAction.Kind.DIALOG_SAVE));
            } else if (result == SkillTreeTextField.KeyResult.CANCEL) {
                nameDialog.close();
            }
            return true;
        }
        if (listOverlay.isOpen()) {
            if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                listOverlay.close();
            }
            return true;
        }
        return false;
    }

    boolean handleScroll(InputEventAPI event) {
        if (listOverlay.isOpen()) {
            listOverlay.scroll(event.getEventValue() > 0 ? -1 : 1, event.getX(), event.getY());
        }
        return isModalOpen();
    }

    void renderBar(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        templateBar.render(canvasPosition, mouseX, mouseY, alphaMult);
    }

    void renderBarTooltip(float mouseX, float mouseY, float alphaMult) {
        templateBar.renderTooltip(mouseX, mouseY, alphaMult);
    }

    void renderModals(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        nameDialog.render(canvasPosition, mouseX, mouseY, alphaMult);
        listOverlay.render(canvasPosition, mouseX, mouseY, alphaMult);
    }

    private TemplateAction modalActionAt(float x, float y) {
        if (nameDialog.isOpen()) {
            return nameDialog.actionAt(x, y);
        }
        return listOverlay.isOpen() ? listOverlay.actionAt(x, y) : TemplateAction.NONE;
    }

    private void perform(TemplateAction action) {
        SkillNode startingRoot = treeSession.getStartingRoot();
        switch (action.kind()) {
            case OPEN_SAVE -> openSaveDialog(startingRoot);
            case OPEN_LOAD -> openList(startingRoot);
            case AUTO_ALLOCATE -> {
                if (barState.autoEnabled()) {
                    treeSession.startAutoAllocate();
                }
            }
            case DIALOG_SAVE -> saveTemplate(startingRoot);
            case DIALOG_CANCEL -> nameDialog.close();
            case SELECT -> selectTemplate(action.templateId());
            case DELETE -> deleteTemplate(action.templateId());
            case TOGGLE_HULL -> listOverlay.toggle(action.hullSize());
            case CLEAR -> {
                SkillTreeTemplateStore.clearAssignment(member.getId());
                treeSession.setTemplate(null);
                listOverlay.close();
            }
            case CLOSE -> listOverlay.close();
            default -> {
            }
        }
    }

    private void openSaveDialog(SkillNode startingRoot) {
        if (startingRoot == null || !barState.saveEnabled()) {
            return;
        }
        treeSession.closeDropdown();
        SkillTreeSounds.panelOpened();
        nameDialog.open(startingRoot.getId(), treeSession.allocatedNodeCount() - 1, SkillTreeTemplateStore.all());
    }

    private void openList(SkillNode startingRoot) {
        if (startingRoot == null || !barState.loadEnabled()) {
            return;
        }
        treeSession.closeDropdown();
        SkillTreeSounds.panelOpened();
        listOverlay.open(startingRoot.getId(), startingRoot.getType().getDisplayName(), hullSize(), SkillTreeTemplateStore.all(), assignedTemplateId());
    }

    private void saveTemplate(SkillNode startingRoot) {
        if (startingRoot == null || !nameDialog.canSave()) {
            return;
        }
        String name = nameDialog.name();
        SkillTreeTemplateStore.save(name, startingRoot.getId(), hullSize(), treeSession.captureTemplateSteps());
        nameDialog.close();
        templateBar.showResult(Translation.msg("ui.template.result.saved").arg("name", name).text());
    }

    private void selectTemplate(String templateId) {
        SkillTreeTemplate template = SkillTreeTemplateStore.find(templateId);
        if (template == null) {
            return;
        }
        SkillTreeTemplateStore.assign(member.getId(), templateId);
        treeSession.setTemplate(template);
        listOverlay.close();
    }

    private void deleteTemplate(String templateId) {
        if (!listOverlay.confirmDelete(templateId)) {
            return;
        }
        SkillTreeTemplateStore.delete(templateId);
        if (templateId.equals(assignedTemplateId())) {
            treeSession.setTemplate(null);
        }
        listOverlay.setTemplates(SkillTreeTemplateStore.all(), assignedTemplateId());
    }

    private String assignedTemplateId() {
        SkillTreeTemplate template = treeSession.template();
        return template == null ? null : template.id();
    }

    private HullSize hullSize() {
        return member.getHullSpec() == null ? null : member.getHullSpec().getHullSize();
    }

    private static String resultText(AutoAllocateRun.Summary summary) {
        if (summary.allocated() == 0) {
            return Translation.text("ui.template.result.none");
        }
        return Translation.msg("ui.template.result").count(summary.allocated()).text();
    }
}
