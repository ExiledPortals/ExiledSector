package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.ui.SkillTreeRefitButton;
import exiledsector.ui.util.Rects;
import org.apache.log4j.Logger;

import java.util.List;

import static exiledsector.ui.refit.UiReflection.call;

final class SkillTreeChipClickTarget extends BaseCustomUIPanelPlugin {

    private static final Logger LOG = Logger.getLogger(SkillTreeChipClickTarget.class);

    private static boolean failed;

    private Object modList;
    private UIComponentAPI chipRow;
    private ButtonAPI chipIcon;
    private boolean swallowMouseUp;

    static void attach(Object modWidget, Object modList, Object chipRow) {
        if (failed || !(modWidget instanceof UIPanelAPI widget)) return;
        try {
            UIComponentAPI chipRowComponent = chipRow instanceof UIComponentAPI component && modList instanceof UIComponentAPI ? component : null;
            ButtonAPI chipIcon = chipRowComponent == null ? null : iconOf(chipRowComponent);
            PositionAPI widgetPosition = widget.getPosition();
            CustomPanelAPI overlay = existing(widget);
            SkillTreeChipClickTarget clickTarget;
            if (overlay == null) {
                if (chipIcon == null) return;
                clickTarget = new SkillTreeChipClickTarget();
                overlay = Global.getSettings().createCustom(widgetPosition.getWidth(), widgetPosition.getHeight(), clickTarget);
                widget.addComponent(overlay).inTL(0f, 0f);
            } else {
                clickTarget = (SkillTreeChipClickTarget) overlay.getPlugin();
                overlay.getPosition().setSize(widgetPosition.getWidth(), widgetPosition.getHeight());
            }
            clickTarget.modList = modList;
            clickTarget.chipRow = chipIcon == null ? null : chipRowComponent;
            clickTarget.chipIcon = chipIcon;
        } catch (Throwable e) {
            disable(e);
        }
    }

    private static ButtonAPI iconOf(UIComponentAPI row) throws Throwable {
        for (Object child : UiReflection.children(row)) {
            if (child instanceof ButtonAPI button && (button.getText() == null || button.getText().isBlank())) {
                return button;
            }
        }
        return null;
    }

    static void resetForTests() {
        failed = false;
    }

    private static CustomPanelAPI existing(UIPanelAPI widget) throws Throwable {
        for (Object child : UiReflection.children(widget)) {
            if (child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof SkillTreeChipClickTarget) {
                return panel;
            }
        }
        return null;
    }

    private static void disable(Throwable e) {
        failed = true;
        LOG.error("[ExiledSector] Opening the skill tree by clicking its hull mod failed, so it is off until the game restarts; use the refit button instead", e);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (failed) return;
        for (InputEventAPI event : events) {
            if (event.isLMBUpEvent()) {
                if (swallowMouseUp && !event.isConsumed()) event.consume();
                swallowMouseUp = false;
            } else if (!event.isConsumed() && event.isLMBDownEvent() && isOverChip(event.getX(), event.getY()) && open(event)) {
                swallowMouseUp = true;
                event.consume();
            }
        }
    }

    private boolean isOverChip(float x, float y) {
        try {
            return chipIcon != null && modList instanceof UIComponentAPI listComponent
                    && contains(listComponent.getPosition(), x, y)
                    && contains(chipIcon.getPosition(), x, y)
                    && call(modList, "getItems") instanceof List<?> modRows && modRows.contains(chipRow);
        } catch (Throwable e) {
            disable(e);
            return false;
        }
    }

    private static boolean open(InputEventAPI event) {
        try {
            return SkillTreeRefitButton.openPanel(event);
        } catch (RuntimeException | LinkageError e) {
            disable(e);
            return false;
        }
    }

    static boolean contains(PositionAPI position, float x, float y) {
        return Rects.contains(position, x, y);
    }
}
