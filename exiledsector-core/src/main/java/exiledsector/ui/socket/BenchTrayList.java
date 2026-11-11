package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.ui.VanillaText;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

final class BenchTrayList {

    record FilterBar(TooltipMakerAPI element, TextFieldAPI field) {
    }

    static final float FILTER_HEIGHT = 26f;
    private static final float FILTER_LABEL_WIDTH = 60f;
    private static final float ICON_SIZE = 32f;
    private static final float ROW_INSET = 6f;
    private static final float COST_WIDTH = 44f;
    private static final float ROW_MIN_HEIGHT = ICON_SIZE + ROW_INSET * 2f;

    private BenchTrayList() {
    }

    static FilterBar filterBar(CustomPanelAPI panelRoot, float trayWidth, String filterText) {
        float barWidth = trayWidth - HoloPanel.PAD * 2f;
        TooltipMakerAPI element = panelRoot.createUIElement(barWidth, FILTER_HEIGHT, false);
        VanillaText.plainPara(element, Translation.text("ui.craftingBench.tray.filter"), 0f, Misc.getGrayColor())
                .getPosition().inTL(0f, HoloPanel.LINE_PAD * 2f);
        TextFieldAPI field = element.addTextField(barWidth - FILTER_LABEL_WIDTH, FILTER_HEIGHT, Fonts.DEFAULT_SMALL, 0f);
        field.getPosition().inTL(FILTER_LABEL_WIDTH, 0f);
        field.setText(filterText);
        return new FilterBar(element, field);
    }

    static TooltipMakerAPI rows(CustomPanelAPI panelRoot, float trayWidth, float listHeight, BenchTray tray, BenchTrayContext context,
                                Function<BenchTrayEntry, TileInput.Handler> handlerFor, String filterText) {
        float listWidth = trayWidth - HoloPanel.PAD * 2f;
        float rowWidth = listWidth - ItemGrid.SCROLLBAR_ROOM;
        TooltipMakerAPI element = panelRoot.createUIElement(listWidth, listHeight, true);
        element.setParaFont(Fonts.ORBITRON_12);
        VanillaText.plainPara(element, Translation.text(tray.titleKey()), 0f, Misc.getBasePlayerColor());
        element.setParaFontDefault();
        List<BenchTrayEntry> entries = tray.entries(context);
        List<BenchTrayEntry> shown = matching(entries, context, filterText);
        for (BenchTrayEntry entry : shown) {
            element.addCustom(row(entry, context, rowWidth, handlerFor.apply(entry)), HoloPanel.GAP);
        }
        if (shown.isEmpty()) {
            String emptyKey = entries.isEmpty() ? tray.emptyKey() : "ui.craftingBench.tray.noMatch";
            VanillaText.plainPara(element, Translation.text(emptyKey), HoloPanel.GAP, Misc.getGrayColor());
        }
        return element;
    }

    static List<BenchTrayEntry> matching(List<BenchTrayEntry> entries, BenchTrayContext context, String filterText) {
        List<String> words = words(filterText);
        if (words.isEmpty()) {
            return entries;
        }
        return entries.stream().filter(entry -> {
            String searched = entry.searchText(context).toLowerCase(Locale.ROOT);
            return words.stream().allMatch(searched::contains);
        }).toList();
    }

    private static List<String> words(String filterText) {
        if (filterText == null || filterText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(filterText.toLowerCase(Locale.ROOT).trim().split("\\s+")).toList();
    }

    private static CustomPanelAPI row(BenchTrayEntry entry, BenchTrayContext context, float rowWidth, TileInput.Handler rowHandler) {
        boolean available = entry.available(context);
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(rowWidth, ROW_MIN_HEIGHT,
                new BenchTrayRow(entry.iconPaths(), entry.look(), available, ICON_SIZE, ROW_INSET, rowHandler));
        float costLeft = ROW_INSET + ICON_SIZE + HoloPanel.GAP;
        float textLeft = costLeft + COST_WIDTH + HoloPanel.GAP;
        float textWidth = rowWidth - textLeft - ROW_INSET;
        TooltipMakerAPI textElement = rowPanel.createUIElement(textWidth, 0f, false);
        VanillaText.addPara(textElement, entry.rowText(context), 0f, available ? Misc.getTextColor() : Misc.getGrayColor());
        float textHeight = textElement.getHeightSoFar();
        float rowHeight = Math.max(ROW_MIN_HEIGHT, textHeight + ROW_INSET * 2f);
        rowPanel.getPosition().setSize(rowWidth, rowHeight);
        textElement.getPosition().setSize(textWidth, textHeight);
        rowPanel.addUIElement(textElement).inTL(textLeft, (rowHeight - textHeight) / 2f);
        TooltipMakerAPI costElement = rowPanel.createUIElement(COST_WIDTH, 0f, false);
        costElement.setParaFont(Fonts.ORBITRON_12);
        String badge = entry.badge(context);
        Color costColor = available ? Misc.getBrightPlayerColor() : Misc.getGrayColor();
        LabelAPI costLabel = VanillaText.plainPara(costElement, badge == null ? "" : badge, 0f, costColor);
        costElement.setParaFontDefault();
        float costHeight = costLabel.getPosition().getHeight();
        costElement.getPosition().setSize(COST_WIDTH, costHeight);
        rowPanel.addUIElement(costElement).inTL(costLeft, (rowHeight - costHeight) / 2f);
        return rowPanel;
    }
}
