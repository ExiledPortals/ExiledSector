package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.ArrayList;
import java.util.List;

final class DumpUiStep implements HarnessStep {

    static final int MOST_ENTRIES = 400;

    @Override
    public String name() {
        return "dumpUi";
    }

    @Override
    public StepOutcome advance(StepContext context) throws Throwable {
        List<Object> roots = ScreenTargets.roots();
        if (roots.isEmpty()) {
            return StepOutcome.passed("no core tab or dialog is open");
        }
        List<String> entries = new ArrayList<>();
        for (Object root : roots) {
            UiTree.find(root, component -> {
                String entry = describe(component);
                if (entry != null) {
                    entries.add(entry);
                }
                return entries.size() >= MOST_ENTRIES;
            });
        }
        return StepOutcome.passed(entries.size() + " shown components: " + String.join("; ", entries));
    }

    private static String describe(Object component) {
        if (component instanceof ButtonAPI button) {
            String label = button.getText() != null && !button.getText().isBlank() ? "\"" + button.getText().trim() + "\""
                    : "[" + (button.getCustomData() == null ? "no text" : button.getCustomData()) + "]";
            return onScreen(button.getPosition()) ? "button " + label + " " + where(button.getPosition()) : null;
        }
        if (component instanceof CustomPanelAPI panel && panel.getPlugin() != null) {
            return onScreen(panel.getPosition()) ? panel.getPlugin().getClass().getSimpleName() + " " + where(panel.getPosition()) : null;
        }
        return null;
    }

    private static boolean onScreen(PositionAPI position) {
        return position.getCenterX() >= 0f && position.getCenterY() >= 0f && position.getCenterX() <= Global.getSettings().getScreenWidth()
                && position.getCenterY() <= Global.getSettings().getScreenHeight() && position.getWidth() > 0f;
    }

    private static String where(PositionAPI position) {
        return "@" + Math.round(position.getCenterX()) + "," + Math.round(position.getCenterY()) + " " + Math.round(position.getWidth()) + "x"
                + Math.round(position.getHeight());
    }
}
