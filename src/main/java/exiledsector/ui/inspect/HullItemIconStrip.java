package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.hullitems.HullItem;
import exiledsector.ui.socket.HullItemCell;
import exiledsector.ui.socket.HullItemHoverTooltip;

import java.util.ArrayList;
import java.util.List;

final class HullItemIconStrip extends BaseCustomUIPanelPlugin {

    static final float CELL_SIZE = 64f;
    private static final float GAP = 8f;
    private static final HullItemCell.Look CELL_LOOK = new HullItemCell.Look(6f, false, 0.55f);

    record Entry(HullItem hullItem, HullSize hullSize, List<StyledText> footer) {
    }

    private final HullItemHoverTooltip hoverTooltip;
    private final List<Runnable> queuedActions = new ArrayList<>();

    private HullItemIconStrip(CustomPanelAPI tooltipHost) {
        this.hoverTooltip = new HullItemHoverTooltip(tooltipHost);
    }

    static CustomPanelAPI create(CustomPanelAPI tooltipHost, float stripWidth, List<Entry> entries) {
        HullItemIconStrip strip = new HullItemIconStrip(tooltipHost);
        int cellsPerRow = cellsPerRow(stripWidth);
        CustomPanelAPI stripPanel = Global.getSettings().createCustom(stripWidth, height(entries.size(), cellsPerRow), strip);
        for (int i = 0; i < entries.size(); i++) {
            CustomPanelAPI cellPanel = Global.getSettings().createCustom(CELL_SIZE, CELL_SIZE, strip.cellFor(entries.get(i)));
            stripPanel.addComponent(cellPanel).inTL((i % cellsPerRow) * (CELL_SIZE + GAP), (i / cellsPerRow) * (CELL_SIZE + GAP));
        }
        return stripPanel;
    }

    static int cellsPerRow(float stripWidth) {
        return Math.max(1, (int) ((stripWidth + GAP) / (CELL_SIZE + GAP)));
    }

    static float height(int entryCount, int cellsPerRow) {
        int rows = Math.max(1, (entryCount + cellsPerRow - 1) / cellsPerRow);
        return rows * CELL_SIZE + (rows - 1) * GAP;
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
        queuedActions.add(() -> hoverTooltip.show(entry, entry.hullItem(), entry.hullSize(), entry::footer, cellPosition));
    }

    private void left(Entry entry) {
        queuedActions.add(() -> {
            if (hoverTooltip.isShowing(entry)) {
                hoverTooltip.hide();
            }
        });
    }

    private HullItemCell cellFor(Entry entry) {
        return new HullItemCell(entry.hullItem(), CELL_LOOK, new HullItemCell.Listener() {
            @Override
            public void hovered(PositionAPI cellPosition) {
                HullItemIconStrip.this.hovered(entry, cellPosition);
            }

            @Override
            public void left() {
                HullItemIconStrip.this.left(entry);
            }
        });
    }
}
