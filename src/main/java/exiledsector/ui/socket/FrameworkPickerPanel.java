package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.socketables.HullFramework;
import exiledsector.socketables.HullFrameworkTooltip;

import java.awt.Color;
import java.util.List;

public final class FrameworkPickerPanel extends HoloPanel {

    public interface Listener {

        void chosen(HullFramework framework);

        void closed();
    }

    public record Entry(HullFramework framework, String blockReason) {
    }

    public static final float WIDTH = 460f;
    private static final int COLUMNS = 2;
    private static final float SCROLLBAR_ROOM = 14f;
    private static final float NAME_HEIGHT = 44f;
    private static final float TOOLTIP_WIDTH = 520f;
    private static final Color BLOCKED_NAME_COLOR = new Color(130, 130, 130);

    private enum Control {CLOSE}

    private final Listener pickerListener;
    private final HullSize hullSize;
    private List<Entry> entries;
    private int otherSizeCount;
    private UIComponentAPI headerElement;
    private TooltipMakerAPI listElement;
    private final SocketableHoverTooltip hoverTooltip;

    private FrameworkPickerPanel(CustomPanelAPI hostPanel, HullSize hullSize, List<Entry> entries, int otherSizeCount, Listener pickerListener) {
        super(hostPanel, FrameworkPickerPanel.class);
        this.hullSize = hullSize;
        this.entries = entries;
        this.otherSizeCount = otherSizeCount;
        this.pickerListener = pickerListener;
        this.hoverTooltip = new SocketableHoverTooltip(hostPanel);
    }

    public static FrameworkPickerPanel open(CustomPanelAPI hostPanel, float panelLeft, float panelTop, float panelHeight, HullSize hullSize,
                                            List<Entry> entries, int otherSizeCount, Listener pickerListener) {
        FrameworkPickerPanel pickerPanel = new FrameworkPickerPanel(hostPanel, hullSize, entries, otherSizeCount, pickerListener);
        pickerPanel.attach(panelLeft, panelTop, WIDTH, panelHeight);
        return pickerPanel;
    }

    public void update(List<Entry> updatedEntries, int updatedOtherSizeCount) {
        queue(() -> {
            entries = updatedEntries;
            otherSizeCount = updatedOtherSizeCount;
            rebuildList();
        });
    }

    @Override
    protected void build() {
        TooltipMakerAPI element = startHeader("framework.picker.title", "ui.socketStorage.close", Control.CLOSE);
        headerElement = finishHeader(element, headerElement, CLOSE_BUTTON_WIDTH + GAP);
        rebuildList();
    }

    @Override
    protected UIComponentAPI[] chromeComponents() {
        return new UIComponentAPI[]{headerElement};
    }

    @Override
    protected UIComponentAPI[] contentComponents() {
        return new UIComponentAPI[]{listElement == null ? null
                : listElement.getExternalScroller() != null ? listElement.getExternalScroller() : listElement};
    }

    @Override
    protected void onScrollInside() {
        queue(hoverTooltip::hide);
    }

    @Override
    protected void advanceOpen(float amount) {
        hoverTooltip.refreshIfExpansionChanged();
    }

    @Override
    protected void onClose() {
        hoverTooltip.hide();
        pickerListener.closed();
    }

    @Override
    public void buttonPressed(Object id) {
        if (id == Control.CLOSE) {
            queue(this::close);
        }
    }

    private void rebuildList() {
        hoverTooltip.hide();
        if (listElement != null) {
            panelRoot.removeComponent(listElement.getExternalScroller() != null ? listElement.getExternalScroller() : listElement);
        }
        float listWidth = panelWidth - PAD * 2f;
        float listTop = PAD + HEADER_HEIGHT + GAP;
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, panelHeight - listTop - PAD, true);
        String sizeName = Translation.text("hullSize." + hullSize.name());
        element.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), Translation.msg("framework.picker.hint").arg("size", sizeName).text());
        if (entries.isEmpty()) {
            element.addPara("%s", GAP * 2f, Misc.getGrayColor(), Misc.getGrayColor(),
                    Translation.msg("framework.picker.none").arg("size", sizeName).text());
        }
        float gridWidth = listWidth - SCROLLBAR_ROOM;
        float cellWidth = (gridWidth - GAP * (COLUMNS - 1)) / COLUMNS;
        float cellHeight = cellWidth + NAME_HEIGHT;
        for (int start = 0; start < entries.size(); start += COLUMNS) {
            CustomPanelAPI rowPanel = Global.getSettings().createCustom(gridWidth, cellHeight, null);
            for (int i = start; i < Math.min(entries.size(), start + COLUMNS); i++) {
                rowPanel.addComponent(cellFor(entries.get(i), cellWidth, cellHeight)).inTL((i - start) * (cellWidth + GAP), 0f);
            }
            element.addCustom(rowPanel, GAP * 2f);
        }
        if (otherSizeCount > 0) {
            element.addPara("%s", GAP * 3f, Misc.getGrayColor(), Misc.getGrayColor(),
                    Translation.msg("framework.picker.otherSizes").count(otherSizeCount).arg("count", otherSizeCount).text());
        }
        panelRoot.addUIElement(element).inTL(PAD, listTop);
        listElement = element;
        refreshOpacity();
    }

    private void showTooltip(Entry entry, PositionAPI cellPosition) {
        List<StyledText> footer = List.of(entry.blockReason() != null ? StyledText.of(entry.blockReason())
                : Translation.styled("framework.picker.clickToInstall"));
        hoverTooltip.show(entry, TOOLTIP_WIDTH, (tooltip, expanded) -> HullFrameworkTooltip.write(tooltip, entry.framework(), () -> footer),
                cellPosition);
    }

    private void hideTooltip(Entry entry) {
        if (hoverTooltip.isShowing(entry)) {
            hoverTooltip.hide();
        }
    }

    private CustomPanelAPI cellFor(Entry entry, float cellWidth, float cellHeight) {
        HullFramework framework = entry.framework();
        boolean blocked = entry.blockReason() != null;
        CustomPanelAPI cellPanel = Global.getSettings().createCustom(cellWidth, cellHeight, null);
        TooltipMakerAPI cellElement = cellPanel.createUIElement(cellWidth, cellHeight, false);
        CustomPanelAPI iconPanel = Global.getSettings().createCustom(cellWidth, cellWidth,
                new FrameworkCell(framework, blocked, () -> queue(() -> pickerListener.chosen(framework)),
                        cellPosition -> queue(() -> showTooltip(entry, cellPosition)), () -> queue(() -> hideTooltip(entry))));
        cellElement.addCustom(iconPanel, 0f);
        Color nameColor = blocked ? BLOCKED_NAME_COLOR : framework.rarity().color();
        LabelAPI nameLabel = cellElement.addPara("%s", LINE_PAD, nameColor, nameColor, framework.name());
        nameLabel.setAlignment(Alignment.MID);
        nameLabel.getPosition().setSize(cellWidth, NAME_HEIGHT - LINE_PAD);
        cellPanel.addUIElement(cellElement).inTL(0f, 0f);
        return cellPanel;
    }
}
