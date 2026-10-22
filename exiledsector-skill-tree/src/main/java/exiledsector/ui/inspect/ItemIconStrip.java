package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.i18n.I18n;
import exiledlib.i18n.StyledText;
import exiledsector.items.Item;
import exiledsector.ui.socket.ItemCell;
import exiledsector.ui.socket.ItemHoverTooltip;

import java.util.ArrayList;
import java.util.List;

final class ItemIconStrip extends BaseCustomUIPanelPlugin {

    static final float CELL_SIZE = 64f;
    private static final float GAP = 8f;
    private static final ItemCell.Look CELL_LOOK = new ItemCell.Look(6f, false, 0.55f);

    record Entry(Item item, HullSize hullSize, List<StyledText> footer) {
    }

    private final ItemHoverTooltip hoverTooltip;
    private final List<Runnable> queuedActions = new ArrayList<>();

    private ItemIconStrip(CustomPanelAPI tooltipHost) {
        this.hoverTooltip = new ItemHoverTooltip(tooltipHost);
    }

    static CustomPanelAPI create(CustomPanelAPI tooltipHost, float stripWidth, List<Entry> entries) {
        ItemIconStrip strip = new ItemIconStrip(tooltipHost);
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
        queuedActions.add(() -> hoverTooltip.show(entry, entry.item(), entry.hullSize(), expanded -> entry.footer(), cellPosition));
    }

    private void left(Entry entry) {
        queuedActions.add(() -> {
            if (hoverTooltip.isShowing(entry)) {
                hoverTooltip.hide();
            }
        });
    }

    private ItemCell cellFor(Entry entry) {
        return new ItemCell(entry.item(), CELL_LOOK, new ItemCell.Listener() {
            @Override
            public void hovered(PositionAPI cellPosition) {
                ItemIconStrip.this.hovered(entry, cellPosition);
            }

            @Override
            public void left() {
                ItemIconStrip.this.left(entry);
            }
        });
    }
}
