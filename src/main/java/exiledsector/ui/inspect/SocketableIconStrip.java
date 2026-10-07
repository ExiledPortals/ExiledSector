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

    private final SocketableHoverTooltip hoverTooltip;
    private final List<Runnable> queuedActions = new ArrayList<>();

    private SocketableIconStrip(CustomPanelAPI tooltipHost) {
        this.hoverTooltip = new SocketableHoverTooltip(tooltipHost);
    }

    static CustomPanelAPI create(CustomPanelAPI tooltipHost, float stripWidth, List<Entry> entries) {
        SocketableIconStrip strip = new SocketableIconStrip(tooltipHost);
        CustomPanelAPI stripPanel = Global.getSettings().createCustom(stripWidth, CELL_SIZE, strip);
        for (int i = 0; i < entries.size(); i++) {
            CustomPanelAPI cellPanel = Global.getSettings().createCustom(CELL_SIZE, CELL_SIZE, strip.cellFor(entries.get(i)));
            stripPanel.addComponent(cellPanel).inTL(i * (CELL_SIZE + GAP), 0f);
        }
        return stripPanel;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent()) {
                queuedActions.add(hoverTooltip::hide);
            }
        }
    }

    @Override
    public void advance(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
        if (queuedActions.isEmpty()) {
            return;
        }
        List<Runnable> actions = List.copyOf(queuedActions);
        queuedActions.clear();
        actions.forEach(I18n::forGameText);
    }

    private void hovered(Entry entry, PositionAPI cellPosition) {
        queuedActions.add(() -> hoverTooltip.show(entry, entry.socketable(), entry::footer, cellPosition));
    }

    private void left(Entry entry) {
        queuedActions.add(() -> {
            if (hoverTooltip.isShowing(entry)) {
                hoverTooltip.hide();
            }
        });
    }

    private SocketableCell cellFor(Entry entry) {
        return new SocketableCell(entry.socketable(), CELL_LOOK, new SocketableCell.Listener() {
            @Override
            public void hovered(PositionAPI cellPosition) {
                SocketableIconStrip.this.hovered(entry, cellPosition);
            }

            @Override
            public void left() {
                SocketableIconStrip.this.left(entry);
            }
        });
    }
}
