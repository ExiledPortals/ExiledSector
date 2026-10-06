package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.socketables.Socketable;
import exiledsector.ui.socket.SocketableCell;
import exiledsector.ui.socket.SocketableHoverTooltip;

import java.util.ArrayList;
import java.util.List;

final class SocketableIconStrip extends BaseCustomUIPanelPlugin {

    static final float CELL_SIZE = 64f;
    private static final float GAP = 8f;
    private static final SocketableCell.Look CELL_LOOK = new SocketableCell.Look(6f, false, 0.55f);

    record Entry(Socketable socketable, List<StyledText> footer) {
    }

    private final SocketableHoverTooltip tooltip;
    private final List<Runnable> queued = new ArrayList<>();

    private SocketableIconStrip(CustomPanelAPI tooltipHost) {
        this.tooltip = new SocketableHoverTooltip(tooltipHost);
    }

    static CustomPanelAPI create(CustomPanelAPI tooltipHost, float width, List<Entry> entries) {
        SocketableIconStrip strip = new SocketableIconStrip(tooltipHost);
        CustomPanelAPI panel = Global.getSettings().createCustom(width, CELL_SIZE, strip);
        for (int i = 0; i < entries.size(); i++) {
            CustomPanelAPI cell = Global.getSettings().createCustom(CELL_SIZE, CELL_SIZE, strip.cellFor(entries.get(i)));
            panel.addComponent(cell).inTL(i * (CELL_SIZE + GAP), 0f);
        }
        return panel;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent()) {
                queued.add(tooltip::hide);
            }
        }
    }

    @Override
    public void advance(float amount) {
        tooltip.refreshIfExpansionChanged();
        if (queued.isEmpty()) {
            return;
        }
        List<Runnable> actions = List.copyOf(queued);
        queued.clear();
        actions.forEach(I18n::forGameText);
    }

    private void hovered(Entry entry, PositionAPI cell) {
        queued.add(() -> tooltip.show(entry, entry.socketable(), entry::footer, cell));
    }

    private void left(Entry entry) {
        queued.add(() -> {
            if (tooltip.isShowing(entry)) {
                tooltip.hide();
            }
        });
    }

    private SocketableCell cellFor(Entry entry) {
        return new SocketableCell(entry.socketable(), CELL_LOOK, new SocketableCell.Listener() {
            @Override
            public void hovered(PositionAPI cell) {
                SocketableIconStrip.this.hovered(entry, cell);
            }

            @Override
            public void left() {
                SocketableIconStrip.this.left(entry);
            }
        });
    }
}
