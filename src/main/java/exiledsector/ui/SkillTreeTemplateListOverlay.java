package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateFilter;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class SkillTreeTemplateListOverlay {

    private static final float MAX_WIDTH = 720f;
    private static final float MAX_HEIGHT = 620f;
    private static final float SIDE_CLEARANCE = 200f;
    private static final float VERTICAL_CLEARANCE = 80f;
    private static final float PADDING = 20f;
    private static final float CHIP_HEIGHT = 32f;
    private static final float CHIP_GAP = 8f;
    private static final float ROW_HEIGHT = 44f;
    private static final float ROW_GAP = 4f;
    private static final float DELETE_WIDTH = 110f;
    private static final float META_GAP = 16f;
    private static final float FOOTER_BUTTON_WIDTH = 170f;
    private static final float FOOTER_HEIGHT = 36f;
    private static final float ROW_FILL_ALPHA = 0.06f;
    private static final float ROW_HOVER_ALPHA = 0.2f;
    private static final float FONT_SIZE = SkillTreePanelStyle.TOOLTIP_BODY_FONT_SIZE;
    private static final Color META_COLOR = new Color(170, 170, 170);
    private static final Color CONFIRM_COLOR = SkillTreePanelStyle.NEGATIVE_STAT_COLOR;

    private final HoloFrame modalFrame = new HoloFrame(SkillTreeTemplateListOverlay.class, HoloFrame.Look.MODAL);
    private final SkillTreePanelStyle panelStyle;
    private final Map<HullSize, SkillTreeUiButton> hullSizeChips = new EnumMap<>(HullSize.class);
    private final SkillTreeUiButton clearButton = new SkillTreeUiButton(Translation.text("ui.template.list.clear"));
    private final SkillTreeUiButton closeButton = new SkillTreeUiButton(Translation.text("ui.template.list.close"));
    private final List<RowSlot> rowSlots = new ArrayList<>();
    private final DeleteConfirmation deleteConfirmation = new DeleteConfirmation();
    private final String deleteText = Translation.text("ui.template.list.delete");
    private final String confirmText = Translation.text("ui.template.list.confirmDelete");
    private final String emptyRootText = Translation.text("ui.template.list.empty");
    private final String noMatchText = Translation.text("ui.template.list.noMatch");
    private final TextLabel titleLine = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE,
            SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel emptyLine = new TextLabel(FONT_SIZE, META_COLOR);

    private boolean overlayOpen;
    private TemplateListState templateListState;
    private String assignedTemplateId;
    private ScreenRect overlayBox = ScreenRect.NONE;
    private ScreenRect listArea = ScreenRect.NONE;
    private int visibleRows;

    SkillTreeTemplateListOverlay(SkillTreePanelStyle panelStyle) {
        this.panelStyle = panelStyle;
        for (HullSize hullSize : TemplateFilter.FILTERABLE) {
            hullSizeChips.put(hullSize, new SkillTreeUiButton(Translation.text("hullSize." + hullSize.name())));
        }
    }

    void open(String rootNodeId, String rootName, HullSize currentHullSize, Collection<SkillTreeTemplate> templates, String assignedTemplateId) {
        this.templateListState = new TemplateListState(templates, rootNodeId, currentHullSize);
        this.assignedTemplateId = assignedTemplateId;
        this.overlayOpen = true;
        modalFrame.open();
        hideButtons();
        deleteConfirmation.disarm();
        titleLine.set(Translation.msg("ui.template.list.title").arg("root", rootName).text());
    }

    void setTemplates(Collection<SkillTreeTemplate> templates, String assignedTemplateId) {
        this.assignedTemplateId = assignedTemplateId;
        templateListState.setTemplates(templates);
        for (RowSlot slot : rowSlots) {
            slot.unbind();
        }
    }

    void close() {
        overlayOpen = false;
        modalFrame.close();
        hideButtons();
        overlayBox = ScreenRect.NONE;
        listArea = ScreenRect.NONE;
        deleteConfirmation.disarm();
    }

    boolean isOpen() {
        return overlayOpen;
    }

    boolean isBlocking() {
        return overlayOpen || modalFrame.isBlocking();
    }

    private void hideButtons() {
        for (SkillTreeUiButton chip : hullSizeChips.values()) {
            chip.hide();
        }
        clearButton.hide();
        closeButton.hide();
        for (RowSlot slot : rowSlots) {
            slot.unbind();
        }
    }

    void toggle(HullSize hullSize) {
        templateListState.toggle(hullSize);
        deleteConfirmation.disarm();
    }

    boolean confirmDelete(String templateId) {
        return deleteConfirmation.click(templateId);
    }

    void scroll(int rows, float x, float y) {
        if (listArea.contains(x, y)) {
            templateListState.scroll(rows, visibleRows);
        }
    }

    void advance(float amount) {
        modalFrame.advance(amount);
        deleteConfirmation.advance(amount);
    }

    TemplateAction actionAt(float x, float y) {
        if (!overlayBox.contains(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.CLOSE);
        }
        for (Map.Entry<HullSize, SkillTreeUiButton> chip : hullSizeChips.entrySet()) {
            if (chip.getValue().isClickable(x, y)) {
                return TemplateAction.forHullSize(chip.getKey());
            }
        }
        for (RowSlot slot : rowSlots) {
            if (slot.template == null) {
                continue;
            }
            if (slot.deleteButton.isClickable(x, y)) {
                return TemplateAction.forTemplate(TemplateAction.Kind.DELETE, slot.template.id());
            }
            if (slot.rowBounds.contains(x, y)) {
                return TemplateAction.forTemplate(TemplateAction.Kind.SELECT, slot.template.id());
            }
        }
        if (clearButton.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.CLEAR);
        }
        if (closeButton.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.CLOSE);
        }
        return TemplateAction.NONE;
    }

    void render(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult) {
        float overlayWidth = Math.min(MAX_WIDTH, canvasPosition.getWidth() - SIDE_CLEARANCE * 2f);
        float overlayHeight = Math.min(MAX_HEIGHT, canvasPosition.getHeight() - VERTICAL_CLEARANCE * 2f);
        if (!modalFrame.isVisible()) {
            return;
        }
        ScreenRect drawnBox = ScreenRect.centeredIn(canvasPosition, overlayWidth, overlayHeight);
        modalFrame.renderBackdrop(canvasPosition, alphaMult);
        modalFrame.render(drawnBox.left(), drawnBox.bottom(), overlayWidth, overlayHeight, panelStyle.getAccentColor(), alphaMult,
                (frameLeft, frameBottom, frameWidth, frameHeight, frameAlpha) -> renderContent(frameLeft, frameBottom, frameWidth, frameHeight,
                        mouseX, mouseY, frameAlpha));
        if (overlayOpen) {
            overlayBox = drawnBox;
        }
    }

    private void renderContent(float overlayLeft, float overlayBottom, float overlayWidth, float overlayHeight, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();

        float cursorTop = overlayBottom + overlayHeight - PADDING;
        titleLine.setAlpha(alphaMult).draw(overlayLeft + PADDING, cursorTop);
        cursorTop -= SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE + 12f;
        float chipBottom = cursorTop - CHIP_HEIGHT;
        float chipLeft = overlayLeft + PADDING;
        for (Map.Entry<HullSize, SkillTreeUiButton> chip : hullSizeChips.entrySet()) {
            SkillTreeUiButton button = chip.getValue();
            float chipWidth = button.preferredWidth();
            button.place(chipLeft, chipBottom, chipWidth, CHIP_HEIGHT);
            button.setSelected(templateListState.isSelected(chip.getKey()));
            button.setTextColor(templateListState.isSelected(chip.getKey()) ? null : META_COLOR);
            button.render(mouseX, mouseY, alphaMult);
            chipLeft += chipWidth + CHIP_GAP;
        }

        float footerBottom = overlayBottom + PADDING;
        clearButton.place(overlayLeft + PADDING, footerBottom, FOOTER_BUTTON_WIDTH, FOOTER_HEIGHT);
        clearButton.setEnabled(assignedTemplateId != null);
        closeButton.place(overlayLeft + overlayWidth - PADDING - FOOTER_BUTTON_WIDTH, footerBottom, FOOTER_BUTTON_WIDTH, FOOTER_HEIGHT);
        clearButton.render(mouseX, mouseY, alphaMult);
        closeButton.render(mouseX, mouseY, alphaMult);

        float listTop = chipBottom - 12f;
        float listBottom = footerBottom + FOOTER_HEIGHT + 12f;
        listArea = new ScreenRect(overlayLeft + PADDING, listBottom, overlayWidth - PADDING * 2f, Math.max(0f, listTop - listBottom));
        visibleRows = Math.max(0, (int) ((listTop - listBottom + ROW_GAP) / (ROW_HEIGHT + ROW_GAP)));
        renderRows(font, mouseX, mouseY, alphaMult, listTop);
    }

    private void renderRows(LazyFont font, float mouseX, float mouseY, float alphaMult, float listTop) {
        List<SkillTreeTemplate> visibleTemplates = templateListState.window(visibleRows);
        while (rowSlots.size() < visibleTemplates.size()) {
            rowSlots.add(new RowSlot());
        }
        for (int i = 0; i < rowSlots.size(); i++) {
            RowSlot slot = rowSlots.get(i);
            if (i >= visibleTemplates.size() || font == null) {
                slot.unbind();
                continue;
            }
            float rowBottom = listTop - (i + 1) * ROW_HEIGHT - i * ROW_GAP;
            slot.bind(font, visibleTemplates.get(i), listArea.width() - DELETE_WIDTH - 24f);
            slot.render(listArea.left(), rowBottom, listArea.width(), mouseX, mouseY, alphaMult);
        }
        if (visibleTemplates.isEmpty()) {
            emptyLine.set(templateListState.hasAnyForRoot() ? noMatchText : emptyRootText).setAlpha(alphaMult).draw(listArea.left(), listTop - 8f);
        }
    }

    private final class RowSlot {

        private final SkillTreeUiButton deleteButton = new SkillTreeUiButton(deleteText);
        private SkillTreeTemplate template;
        private boolean boundArmed;
        private boolean boundAssigned;
        private final TextLabel nameLine = new TextLabel(FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
        private final TextLabel metaLine = new TextLabel(FONT_SIZE, META_COLOR);
        private ScreenRect rowBounds = ScreenRect.NONE;

        void bind(LazyFont font, SkillTreeTemplate rowTemplate, float textWidth) {
            boolean armed = deleteConfirmation.isArmed(rowTemplate.id());
            boolean assigned = rowTemplate.id().equals(assignedTemplateId);
            if (rowTemplate.equals(template) && armed == boundArmed && assigned == boundAssigned) {
                return;
            }
            template = rowTemplate;
            boundArmed = armed;
            boundAssigned = assigned;
            metaLine.set(metaText(rowTemplate, assigned)).setColor(assigned ? SkillTreePanelStyle.POSITIVE_STAT_COLOR : META_COLOR);
            float nameWidth = textWidth - metaLine.width() - META_GAP;
            String fittedName = SkillTreeTextField.fitStart(rowTemplate.name(), nameWidth, text -> font.calcWidth(text, FONT_SIZE));
            nameLine.set(fittedName);
            deleteButton.setLabel(armed ? confirmText : deleteText);
            deleteButton.setTextColor(armed ? CONFIRM_COLOR : null);
        }

        void unbind() {
            template = null;
            rowBounds = ScreenRect.NONE;
            deleteButton.hide();
        }

        void render(float rowLeft, float rowBottom, float rowWidth, float mouseX, float mouseY, float alphaMult) {
            rowBounds = new ScreenRect(rowLeft, rowBottom, rowWidth - DELETE_WIDTH - 8f, ROW_HEIGHT);
            float fillAlpha = rowBounds.contains(mouseX, mouseY) ? ROW_HOVER_ALPHA : ROW_FILL_ALPHA;
            GLDraw.fillQuad(rowLeft, rowBottom, rowWidth, ROW_HEIGHT, SkillTreePanelStyle.GLOW_COLOR, fillAlpha * alphaMult);
            if (boundAssigned) {
                GLDraw.strokeQuad(rowLeft, rowBottom, rowWidth, ROW_HEIGHT, panelStyle.getAccentColor(), 1.5f, alphaMult);
            }
            float textY = rowBottom + ROW_HEIGHT / 2f + FONT_SIZE / 2f;
            nameLine.setAlpha(alphaMult).draw(rowLeft + 12f, textY);
            metaLine.setAlpha(alphaMult).draw(rowLeft + rowWidth - DELETE_WIDTH - 12f - metaLine.width(), textY);
            deleteButton.place(rowLeft + rowWidth - DELETE_WIDTH, rowBottom + 4f, DELETE_WIDTH, ROW_HEIGHT - 8f);
            deleteButton.render(mouseX, mouseY, alphaMult);
        }

        private String metaText(SkillTreeTemplate rowTemplate, boolean assigned) {
            List<StyledText> parts = new ArrayList<>();
            if (TemplateFilter.isFilterable(rowTemplate.hullSize())) {
                parts.add(Translation.styled("hullSize." + rowTemplate.hullSize().name()));
            }
            parts.add(Translation.msg("ui.template.list.nodes").count(rowTemplate.knownStepCount()).styled());
            if (assigned) {
                parts.add(Translation.styled("ui.template.list.inUse"));
            }
            return Translation.list(parts).plain();
        }
    }
}
