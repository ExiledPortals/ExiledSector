package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.template.AutoAllocateRun;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateNames;
import exiledsector.skills.template.TemplateStep;
import exiledsector.ui.node.TreeAllocationSession;
import org.lwjgl.input.Keyboard;

import java.util.List;
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
    private SkillTreeTemplate statusTemplate;
    private boolean statusPlanning;
    private String statusText;

    SkillTreeTemplateController(FleetMemberAPI member, TreeAllocationSession treeSession, SkillTreePanelStyle style) {
        this.member = member;
        this.treeSession = treeSession;
        this.templateBar = new SkillTreeTemplateBar(style);
        this.listOverlay = new SkillTreeTemplateListOverlay(style);
        treeSession.onPlanChange(this::savePlan);
        loadAssignedTemplateOnRootChange();
    }

    boolean isModalOpen() {
        return nameDialog.isBlocking() || listOverlay.isBlocking();
    }

    boolean barContains(float x, float y) {
        return templateBar.contains(x, y);
    }

    boolean isPlanning() {
        return treeSession.isPlanning();
    }

    void exitTemplateMode() {
        if (treeSession.isPlanning()) {
            treeSession.stopPlanning();
            SkillTreeSounds.panelOpened();
        }
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
        boolean planning = treeSession.isPlanning();
        barState = TemplateBarState.of(templateRootId != null, treeSession.allocatedNodeCount(), template != null,
                treeSession.isAutoAllocating(), !barLive, planning, treeSession.isAutoAllocateOn());
        templateBar.update(barState, statusText(template, planning));
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

    private String statusText(SkillTreeTemplate template, boolean planning) {
        if (template == statusTemplate && planning == statusPlanning) {
            return statusText;
        }
        statusTemplate = template;
        statusPlanning = planning;
        if (!planning) {
            statusText = template == null ? null : Translation.msg("ui.template.current").arg("name", template.name()).text();
        } else if (template == null) {
            statusText = Translation.text("ui.template.planningNew");
        } else {
            int otherShipCount = SkillTreeTemplateStore.assignmentCount(template.id()) - 1;
            statusText = otherShipCount > 0
                    ? Translation.msg("ui.template.planningShared").count(otherShipCount).arg("name", template.name()).text()
                    : Translation.msg("ui.template.planning").arg("name", template.name()).text();
        }
        return statusText;
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
            case TOGGLE_TEMPLATE_MODE -> toggleTemplateMode();
            case OPEN_SAVE -> openSaveDialog(startingRoot);
            case OPEN_LOAD -> openList(startingRoot);
            case TOGGLE_AUTO_ALLOCATE -> toggleAutoAllocate();
            case DIALOG_SAVE -> saveFromDialog(startingRoot);
            case DIALOG_CANCEL -> nameDialog.close();
            case SELECT -> selectTemplate(action.templateId());
            case DELETE -> deleteTemplate(action.templateId());
            case TOGGLE_HULL -> listOverlay.toggle(action.hullSize());
            case CLEAR -> {
                SkillTreeTemplateStore.clearAssignment(member.getId());
                treeSession.setAutoAllocate(false);
                treeSession.setTemplate(null);
                listOverlay.close();
            }
            case CLOSE -> listOverlay.close();
            default -> {
            }
        }
    }

    private void toggleTemplateMode() {
        if (treeSession.isPlanning()) {
            exitTemplateMode();
        } else if (barState.templateModeEnabled() && treeSession.startPlanning()) {
            SkillTreeSounds.panelOpened();
        }
    }

    private void toggleAutoAllocate() {
        if (!barState.autoEnabled()) {
            return;
        }
        boolean autoAllocate = !treeSession.isAutoAllocateOn();
        treeSession.setAutoAllocate(autoAllocate);
        if (autoAllocate && treeSession.template() != null && treeSession.hasPointsLeft()) {
            treeSession.startAutoAllocate();
        }
    }

    private void openSaveDialog(SkillNode startingRoot) {
        if (startingRoot == null || !barState.saveEnabled()) {
            return;
        }
        treeSession.closeDropdown();
        SkillTreeSounds.panelOpened();
        SkillTreeTemplate template = treeSession.template();
        if (treeSession.isPlanning() && template != null) {
            nameDialog.openRename(template, SkillTreeTemplateStore.all());
        } else {
            nameDialog.open(startingRoot.getId(), treeSession.allocatedNodeCount() - 1, SkillTreeTemplateStore.all());
        }
    }

    private void openList(SkillNode startingRoot) {
        if (startingRoot == null || !barState.loadEnabled()) {
            return;
        }
        treeSession.closeDropdown();
        SkillTreeSounds.panelOpened();
        listOverlay.open(startingRoot.getId(), startingRoot.getType().getDisplayName(), hullSize(), SkillTreeTemplateStore.all(), assignedTemplateId());
    }

    private void saveFromDialog(SkillNode startingRoot) {
        if (startingRoot == null || !nameDialog.canSave()) {
            return;
        }
        String name = nameDialog.name();
        String renamedTemplateId = nameDialog.renamedTemplateId();
        nameDialog.close();
        if (renamedTemplateId != null) {
            SkillTreeTemplate renamed = SkillTreeTemplateStore.rename(renamedTemplateId, name);
            if (renamed != null && renamedTemplateId.equals(assignedTemplateId())) {
                treeSession.planSaved(renamed);
            }
            return;
        }
        SkillTreeTemplateStore.save(name, startingRoot.getId(), hullSize(), treeSession.captureTemplateSteps());
        templateBar.showResult(Translation.msg("ui.template.result.saved").arg("name", name).text());
    }

    private void savePlan() {
        SkillNode startingRoot = treeSession.getStartingRoot();
        if (startingRoot == null) {
            return;
        }
        List<TemplateStep> steps = treeSession.capturePlanSteps();
        SkillTreeTemplate saved = SkillTreeTemplateStore.replaceSteps(treeSession.plannedTemplateId(), steps);
        if (saved == null) {
            String defaultName = Translation.msg("ui.template.defaultName").arg("hull", hullName()).text();
            saved = SkillTreeTemplateStore.save(TemplateNames.unique(defaultName, startingRoot.getId(), SkillTreeTemplateStore.all()),
                    startingRoot.getId(), hullSize(), steps);
        }
        SkillTreeTemplateStore.assign(member.getId(), saved.id());
        treeSession.planSaved(saved);
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
        for (String shipId : SkillTreeTemplateStore.assignedShipIds(templateId)) {
            ShipSkillData shipData = ShipSkillDataManager.find(shipId);
            if (shipData != null) {
                shipData.setAutoAllocateTemplate(false);
            }
        }
        SkillTreeTemplateStore.delete(templateId);
        if (templateId.equals(assignedTemplateId())) {
            treeSession.setAutoAllocate(false);
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

    private String hullName() {
        return member.getHullSpec() == null ? "" : member.getHullSpec().getHullName();
    }

    private static String resultText(AutoAllocateRun.Summary summary) {
        if (summary.allocated() == 0) {
            return Translation.text("ui.template.result.none");
        }
        return Translation.msg("ui.template.result").count(summary.allocated()).text();
    }
}
