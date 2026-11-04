package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
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

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

final class StatPicker {

    record Choice(ModifierSearch.Row row, String statName) {
    }

    record ListBounds(float left, float top, float width, float height) {
    }

    static final float CHOICE_HEIGHT = 20f;
    private static final int VISIBLE_CHOICES = 12;
    private static final float CHOICE_TEXT_PADDING = 20f;

    private final ModifierSearch.Row row;
    private final TextFieldAPI field;
    private String polledText;
    private boolean focused;
    private CustomPanelAPI listHost;
    private CustomPanelAPI listPanel;

    StatPicker(ModifierSearch.Row row, TextFieldAPI field) {
        this.row = row;
        this.field = field;
        this.polledText = field.getText();
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

    String filterText() {
        String typed = field.getText().trim();
        return row.hasStat() && typed.equals(ItemStatPool.label(row.statName)) ? "" : typed;
    }

    boolean isListOpen() {
        return listPanel != null;
    }

    boolean contains(float x, float y) {
        return Rects.contains(field.getPosition(), x, y) || (listPanel != null && Rects.contains(listPanel.getPosition(), x, y));
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

    static float listHeight(int choiceCount) {
        int shownChoices = Math.max(1, Math.min(VISIBLE_CHOICES, choiceCount));
        return shownChoices * CHOICE_HEIGHT + (shownChoices - 1) * HoloPanel.LINE_PAD + HoloPanel.GAP * 2f;
    }

    static ListBounds place(float fieldLeft, float fieldTop, float fieldBottom, float fieldWidth, float wantedWidth, float wantedHeight,
                            float paneWidth, float paneHeight) {
        float listWidth = Math.min(paneWidth - HoloPanel.PAD * 2f, Math.max(fieldWidth, wantedWidth));
        float listLeft = Math.max(HoloPanel.PAD, Math.min(fieldLeft, paneWidth - HoloPanel.PAD - listWidth));
        float roomBelow = paneHeight - HoloPanel.PAD - fieldBottom - HoloPanel.LINE_PAD;
        float roomAbove = fieldTop - HoloPanel.LINE_PAD - HoloPanel.PAD;
        boolean below = roomBelow >= wantedHeight || roomBelow >= roomAbove;
        float listHeight = Math.max(0f, Math.min(wantedHeight, below ? roomBelow : roomAbove));
        float listTop = below ? fieldBottom + HoloPanel.LINE_PAD : fieldTop - HoloPanel.LINE_PAD - listHeight;
        return new ListBounds(listLeft, listTop, listWidth, listHeight);
    }

    void openList(CustomPanelAPI paneRoot, PositionAPI panePosition, List<String> statNames, Consumer<Object> buttonListener) {
        closeList();
        float paneTopY = panePosition.getY() + panePosition.getHeight();
        PositionAPI fieldPosition = field.getPosition();
        float fieldTop = paneTopY - (fieldPosition.getY() + fieldPosition.getHeight());
        float fieldBottom = paneTopY - fieldPosition.getY();
        float wantedHeight = listHeight(statNames.size());
        boolean scrolls = statNames.size() > VISIBLE_CHOICES;
        float wantedWidth = widestLabel(statNames) + CHOICE_TEXT_PADDING + HoloPanel.GAP * 2f + (scrolls ? ItemGrid.SCROLLBAR_ROOM : 0f);
        ListBounds bounds = place(fieldPosition.getX() - panePosition.getX(), fieldTop, fieldBottom, fieldPosition.getWidth(), wantedWidth,
                wantedHeight, panePosition.getWidth(), panePosition.getHeight());
        float listInnerHeight = Math.max(0f, bounds.height() - HoloPanel.GAP * 2f);
        float choiceWidth = bounds.width() - HoloPanel.GAP * 2f - (scrolls || wantedHeight > bounds.height() ? ItemGrid.SCROLLBAR_ROOM : 0f);
        listPanel = Global.getSettings().createCustom(bounds.width(), bounds.height(), new FramedPanelPlugin(buttonListener));
        TooltipMakerAPI element = listPanel.createUIElement(bounds.width() - HoloPanel.GAP * 2f, listInnerHeight, true);
        if (statNames.isEmpty()) {
            VanillaText.plainPara(element, Translation.text("ui.advancedSearch.noStats"), 0f, Misc.getGrayColor());
        }
        ChipFlow.Colors colors = ChipFlow.Colors.player();
        element.setAreaCheckboxFont(Fonts.DEFAULT_SMALL);
        float choicePad = 0f;
        for (String statName : statNames) {
            ButtonAPI choiceButton = element.addAreaCheckbox(ItemStatPool.label(statName), new Choice(row, statName), colors.base(), colors.dark(),
                    colors.bright(), choiceWidth, CHOICE_HEIGHT, choicePad);
            choiceButton.setChecked(statName.equals(row.statName));
            choicePad = HoloPanel.LINE_PAD;
        }
        element.setAreaCheckboxFontDefault();
        listPanel.addUIElement(element).inTL(HoloPanel.GAP, HoloPanel.GAP);
        paneRoot.addComponent(listPanel).inTL(bounds.left(), bounds.top());
        listHost = paneRoot;
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
