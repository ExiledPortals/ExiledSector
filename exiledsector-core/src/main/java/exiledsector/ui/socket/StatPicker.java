package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemStatPool;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.FramedPanelPlugin;
import exiledsector.ui.util.Rects;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

final class StatPicker {

    record Choice(ModifierSearch.Row row, String statName) {
    }

    record ListBounds(float left, float top, float width, float height) {
    }

    static final float CHOICE_HEIGHT = 20f;
    private static final float CHOICE_TEXT_INSET = 6f;
    private static final float FIELD_TEXT_INSET = 8f;
    private static final String ELLIPSIS = "...";

    private final ModifierSearch.Row row;
    private final TextFieldAPI field;
    private final String shownLabel;
    private String polledText;
    private boolean focused;
    private CustomPanelAPI listHost;
    private CustomPanelAPI listPanel;

    StatPicker(ModifierSearch.Row row, TextFieldAPI field) {
        this.row = row;
        this.field = field;
        this.shownLabel = row.hasStat() ? fitted(ItemStatPool.label(row.statName), field.getPosition().getWidth() - FIELD_TEXT_INSET * 2f,
                text -> Global.getSettings().computeStringWidth(text, Fonts.DEFAULT_SMALL)) : "";
        field.setText(shownLabel);
        this.polledText = shownLabel;
    }

    boolean poll() {
        boolean focusedNow = field.hasFocus();
        String text = field.getText();
        boolean typed = !text.equals(polledText);
        boolean gainedFocus = focusedNow && !focused;
        focused = focusedNow;
        polledText = text;
        return gainedFocus || (typed && focusedNow);
    }

    boolean isFor(ModifierSearch.Row candidate) {
        return row == candidate;
    }

    void focus() {
        field.grabFocus();
    }

    String filterText() {
        String typed = field.getText().trim();
        return row.hasStat() && typed.equals(shownLabel.trim()) ? "" : typed;
    }

    static String fitted(String text, float maxWidth, ToDoubleFunction<String> measure) {
        if (measure.applyAsDouble(text) <= maxWidth) {
            return text;
        }
        int keptChars = text.length();
        while (keptChars > 0 && measure.applyAsDouble(text.substring(0, keptChars).stripTrailing() + ELLIPSIS) > maxWidth) {
            keptChars--;
        }
        return text.substring(0, keptChars).stripTrailing() + ELLIPSIS;
    }

    boolean isListOpen() {
        return listPanel != null;
    }

    boolean contains(float x, float y) {
        return fieldContains(x, y) || (listPanel != null && Rects.contains(listPanel.getPosition(), x, y));
    }

    boolean fieldContains(float x, float y) {
        return Rects.contains(field.getPosition(), x, y);
    }

    UIComponentAPI listPanel() {
        return listPanel;
    }

    static List<String> matching(List<String> statNames, Function<String, String> labeller, String typed) {
        String needle = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return statNames;
        }
        return statNames.stream().filter(statName -> labeller.apply(statName).toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    static ListBounds place(float fieldLeft, float fieldBottom, float fieldWidth, float wantedWidth, float paneWidth, float paneHeight) {
        float listWidth = Math.min(paneWidth - HoloPanel.PAD * 2f, Math.max(fieldWidth, wantedWidth));
        float listLeft = Math.max(HoloPanel.PAD, Math.min(fieldLeft, paneWidth - HoloPanel.PAD - listWidth));
        float listTop = fieldBottom + HoloPanel.LINE_PAD;
        float roomBelow = Math.max(0f, paneHeight - HoloPanel.PAD - listTop);
        return new ListBounds(listLeft, listTop, listWidth, roomBelow);
    }

    static float stackHeight(List<Float> choiceHeights) {
        float stackHeight = 0f;
        for (float choiceHeight : choiceHeights) {
            stackHeight += choiceHeight;
        }
        return stackHeight + Math.max(0, choiceHeights.size() - 1) * HoloPanel.LINE_PAD;
    }

    void openList(CustomPanelAPI paneRoot, PositionAPI panePosition, List<String> statNames, Consumer<Object> buttonListener) {
        closeList();
        float paneTopY = panePosition.getY() + panePosition.getHeight();
        PositionAPI fieldPosition = field.getPosition();
        float fieldBottom = paneTopY - fieldPosition.getY();
        float wantedWidth = widestLabel(statNames) + CHOICE_TEXT_INSET * 2f + HoloPanel.GAP * 2f + ItemGrid.SCROLLBAR_ROOM;
        ListBounds room = place(fieldPosition.getX() - panePosition.getX(), fieldBottom, fieldPosition.getWidth(), wantedWidth,
                panePosition.getWidth(), panePosition.getHeight());
        List<String> labels = statNames.isEmpty() ? List.of(Translation.text("ui.advancedSearch.noStats"))
                : statNames.stream().map(ItemStatPool::label).toList();
        float roomInside = Math.max(0f, room.height() - HoloPanel.GAP * 2f);
        float choiceWidth = room.width() - HoloPanel.GAP * 2f;
        List<Float> choiceHeights = choiceHeights(paneRoot, labels, choiceWidth);
        if (stackHeight(choiceHeights) > roomInside) {
            choiceWidth -= ItemGrid.SCROLLBAR_ROOM;
            choiceHeights = choiceHeights(paneRoot, labels, choiceWidth);
        }
        float listInnerHeight = Math.min(roomInside, stackHeight(choiceHeights));
        float listHeight = listInnerHeight + HoloPanel.GAP * 2f;
        listPanel = Global.getSettings().createCustom(room.width(), listHeight, new FramedPanelPlugin(buttonListener));
        TooltipMakerAPI element = listPanel.createUIElement(room.width() - HoloPanel.GAP * 2f, listInnerHeight, true);
        element.setParaFont(Fonts.DEFAULT_SMALL);
        ChipFlow.Colors colors = ChipFlow.Colors.player();
        float choiceTop = 0f;
        for (int index = 0; index < labels.size(); index++) {
            float choiceHeight = choiceHeights.get(index);
            if (!statNames.isEmpty()) {
                String statName = statNames.get(index);
                ButtonAPI choiceButton = element.addAreaCheckbox("", new Choice(row, statName), colors.base(), colors.dark(), colors.bright(),
                        choiceWidth, choiceHeight, 0f);
                choiceButton.setChecked(statName.equals(row.statName));
                choiceButton.getPosition().inTL(0f, choiceTop);
            }
            Color textColor = statNames.isEmpty() ? Misc.getGrayColor() : colors.base();
            LabelAPI choiceLabel = VanillaText.plainPara(element, labels.get(index), 0f, textColor);
            float textHeight = choiceLabel.autoSizeToWidth(choiceWidth - CHOICE_TEXT_INSET * 2f).getHeight();
            choiceLabel.getPosition().inTL(CHOICE_TEXT_INSET, choiceTop + (choiceHeight - textHeight) / 2f);
            choiceTop += choiceHeight + HoloPanel.LINE_PAD;
        }
        element.setHeightSoFar(stackHeight(choiceHeights));
        element.setParaFontDefault();
        listPanel.addUIElement(element).inTL(HoloPanel.GAP, HoloPanel.GAP);
        paneRoot.addComponent(listPanel).inTL(room.left(), room.top());
        listHost = paneRoot;
    }

    private static List<Float> choiceHeights(CustomPanelAPI paneRoot, List<String> labels, float choiceWidth) {
        float textWidth = choiceWidth - CHOICE_TEXT_INSET * 2f;
        TooltipMakerAPI measure = paneRoot.createUIElement(textWidth, 0f, false);
        measure.setParaFont(Fonts.DEFAULT_SMALL);
        List<Float> choiceHeights = new ArrayList<>(labels.size());
        for (String label : labels) {
            float textHeight = VanillaText.plainPara(measure, label, 0f, Misc.getTextColor()).autoSizeToWidth(textWidth).getHeight();
            choiceHeights.add(Math.max(CHOICE_HEIGHT, textHeight + HoloPanel.LINE_PAD * 2f));
        }
        return choiceHeights;
    }

    void closeList() {
        if (listPanel == null) {
            return;
        }
        listHost.removeComponent(listPanel);
        listPanel = null;
        listHost = null;
    }

    private static float widestLabel(List<String> statNames) {
        float widest = 0f;
        for (String statName : statNames) {
            widest = Math.max(widest, Global.getSettings().computeStringWidth(ItemStatPool.label(statName), Fonts.DEFAULT_SMALL));
        }
        return widest;
    }
}
