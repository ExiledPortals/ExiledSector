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
import exiledsector.ui.node.SkillTreeNodeRenderer;
import org.lwjgl.input.Keyboard;

final class SkillTreeTemplateController {

    private static final float OFF_SCREEN = -Float.MAX_VALUE;

    private final FleetMemberAPI member;
    private final SkillTreeNodeRenderer nodeRenderer;
    private final SkillTreeTemplateBar bar;
    private final SkillTreeTemplateNameDialog nameDialog = new SkillTreeTemplateNameDialog();
    private final SkillTreeTemplateListOverlay listOverlay;

    private TemplateBarState state = TemplateBarState.HIDDEN;
    private TemplateAction pendingAction;

    SkillTreeTemplateController(FleetMemberAPI member, SkillTreeNodeRenderer nodeRenderer, SkillTreePanelStyle style) {
        this.member = member;
        this.nodeRenderer = nodeRenderer;
        this.bar = new SkillTreeTemplateBar(style);
        this.listOverlay = new SkillTreeTemplateListOverlay(style);
        SkillNode root = nodeRenderer.getStartingRoot();
        if (root != null) {
            nodeRenderer.setTemplate(SkillTreeTemplateStore.assignedTo(member.getId(), root.getId()));
        }
    }

    boolean isModalOpen() {
        return nameDialog.isOpen() || listOverlay.isOpen();
    }

    boolean barContains(float x, float y) {
        return bar.contains(x, y);
    }

    void advance(float amount, PositionAPI position, boolean onHyperspaceMap) {
        SkillTreeTemplate template = nodeRenderer.template();
        boolean running = nodeRenderer.isAutoAllocating();
        boolean pointsLeft = template != null && nodeRenderer.hasPointsLeft();
        state = TemplateBarState.of(nodeRenderer.getStartingRoot() != null, nodeRenderer.allocatedNodeCount(),
                template != null, running, pointsLeft, onHyperspaceMap);
        bar.update(state, template == null ? null : template.name());
        AutoAllocateRun.Summary summary = nodeRenderer.takeLastRunSummary();
        if (summary != null) {
            bar.showResult(resultText(summary));
        }
        bar.advance(amount);
        nameDialog.advance(amount);
        listOverlay.advance(amount);
        if (position != null) {
            bar.layout(position);
        }
    }

    boolean handleLmbDown(float x, float y) {
        if (isModalOpen()) {
            pendingAction = modalActionAt(x, y);
            return true;
        }
        TemplateAction action = bar.actionAt(x, y);
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
        TemplateAction released = isModalOpen() ? modalActionAt(x, y) : bar.actionAt(x, y);
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

    void renderBar(PositionAPI position, float mouseX, float mouseY, float alphaMult) {
        boolean modal = isModalOpen();
        bar.render(position, modal ? OFF_SCREEN : mouseX, modal ? OFF_SCREEN : mouseY, alphaMult);
    }

    void renderBarTooltip(float mouseX, float mouseY, float alphaMult) {
        if (!isModalOpen()) {
            bar.renderTooltip(mouseX, mouseY, alphaMult);
        }
    }

    void renderModals(PositionAPI position, float mouseX, float mouseY, float alphaMult) {
        nameDialog.render(position, mouseX, mouseY, alphaMult);
        listOverlay.render(position, mouseX, mouseY, alphaMult);
    }

    private TemplateAction modalActionAt(float x, float y) {
        return nameDialog.isOpen() ? nameDialog.actionAt(x, y) : listOverlay.actionAt(x, y);
    }

    private void perform(TemplateAction action) {
        SkillNode root = nodeRenderer.getStartingRoot();
        switch (action.kind()) {
            case OPEN_SAVE -> openSaveDialog(root);
            case OPEN_LOAD -> openList(root);
            case AUTO_ALLOCATE -> {
                if (state.autoEnabled()) {
                    nodeRenderer.startAutoAllocate();
                }
            }
            case DIALOG_SAVE -> saveTemplate(root);
            case DIALOG_CANCEL -> nameDialog.close();
            case SELECT -> selectTemplate(action.templateId());
            case DELETE -> deleteTemplate(action.templateId());
            case TOGGLE_HULL -> listOverlay.toggle(action.hullSize());
            case CLEAR -> {
                SkillTreeTemplateStore.clearAssignment(member.getId());
                nodeRenderer.setTemplate(null);
                listOverlay.close();
            }
            case CLOSE -> listOverlay.close();
            default -> {
            }
        }
    }

    private void openSaveDialog(SkillNode root) {
        if (root == null || !state.saveEnabled()) {
            return;
        }
        nodeRenderer.closeDropdown();
        nameDialog.open(root.getId(), nodeRenderer.allocatedNodeCount() - 1, SkillTreeTemplateStore.all());
    }

    private void openList(SkillNode root) {
        if (root == null || !state.loadEnabled()) {
            return;
        }
        nodeRenderer.closeDropdown();
        SkillTreeSounds.panelOpened();
        listOverlay.open(root.getId(), root.getType().getDisplayName(), hullSize(), SkillTreeTemplateStore.all(), assignedTemplateId());
    }

    private void saveTemplate(SkillNode root) {
        if (root == null || !nameDialog.canSave()) {
            return;
        }
        String name = nameDialog.name();
        SkillTreeTemplateStore.save(name, root.getId(), hullSize(), nodeRenderer.captureTemplateSteps());
        nameDialog.close();
        bar.showResult(Translation.msg("ui.template.result.saved").arg("name", name).text());
    }

    private void selectTemplate(String templateId) {
        SkillTreeTemplate template = SkillTreeTemplateStore.find(templateId);
        if (template == null) {
            return;
        }
        SkillTreeTemplateStore.assign(member.getId(), templateId);
        nodeRenderer.setTemplate(template);
        listOverlay.close();
    }

    private void deleteTemplate(String templateId) {
        if (!listOverlay.confirmDelete(templateId)) {
            return;
        }
        SkillTreeTemplateStore.delete(templateId);
        if (templateId.equals(assignedTemplateId())) {
            nodeRenderer.setTemplate(null);
        }
        listOverlay.setTemplates(SkillTreeTemplateStore.all(), assignedTemplateId());
    }

    private String assignedTemplateId() {
        SkillTreeTemplate template = nodeRenderer.template();
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
