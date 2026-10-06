package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.TemplateFilter;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.ReusableText;
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

    private final ModalFrame frame = new ModalFrame(SkillTreeTemplateListOverlay.class);
    private final SkillTreePanelStyle style;
    private final Map<HullSize, SkillTreeUiButton> chips = new EnumMap<>(HullSize.class);
    private final SkillTreeUiButton clear = new SkillTreeUiButton(Translation.text("ui.template.list.clear"));
    private final SkillTreeUiButton close = new SkillTreeUiButton(Translation.text("ui.template.list.close"));
    private final List<RowSlot> slots = new ArrayList<>();
    private final DeleteConfirmation deleteConfirmation = new DeleteConfirmation();
    private final String deleteText = Translation.text("ui.template.list.delete");
    private final String confirmText = Translation.text("ui.template.list.confirmDelete");
    private final String emptyRootText = Translation.text("ui.template.list.empty");
    private final String noMatchText = Translation.text("ui.template.list.noMatch");
    private final ReusableText title = new ReusableText(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE,
            SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final ReusableText emptyLine = new ReusableText(FONT_SIZE, META_COLOR);

    private boolean open;
    private TemplateListState state;
    private String assignedId;
    private ScreenRect box = ScreenRect.NONE;
    private ScreenRect listArea = ScreenRect.NONE;
    private int visibleRows;

    SkillTreeTemplateListOverlay(SkillTreePanelStyle style) {
        this.style = style;
        for (HullSize hullSize : TemplateFilter.FILTERABLE) {
            chips.put(hullSize, new SkillTreeUiButton(Translation.text("hullSize." + hullSize.name())));
        }
    }

    void open(String rootNodeId, String rootName, HullSize currentHullSize, Collection<SkillTreeTemplate> templates, String assignedId) {
        this.state = new TemplateListState(templates, rootNodeId, currentHullSize);
        this.assignedId = assignedId;
        this.open = true;
        frame.open();
        deleteConfirmation.disarm();
        for (RowSlot slot : slots) {
            slot.unbind();
        }
        title.set(Translation.msg("ui.template.list.title").arg("root", rootName).text());
    }

    void setTemplates(Collection<SkillTreeTemplate> templates, String assignedId) {
        this.assignedId = assignedId;
        state.setTemplates(templates);
        for (RowSlot slot : slots) {
            slot.unbind();
        }
    }

    void close() {
        open = false;
        frame.close();
        box = ScreenRect.NONE;
        listArea = ScreenRect.NONE;
        deleteConfirmation.disarm();
    }

    boolean isOpen() {
        return open;
    }

    void toggle(HullSize hullSize) {
        state.toggle(hullSize);
        deleteConfirmation.disarm();
    }

    boolean confirmDelete(String templateId) {
        return deleteConfirmation.click(templateId);
    }

    void scroll(int rows, float x, float y) {
        if (listArea.contains(x, y)) {
            state.scroll(rows, visibleRows);
        }
    }

    void advance(float amount) {
        frame.advance(amount);
        deleteConfirmation.advance(amount);
    }

    TemplateAction actionAt(float x, float y) {
        if (!box.contains(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.CLOSE);
        }
        for (Map.Entry<HullSize, SkillTreeUiButton> chip : chips.entrySet()) {
            if (chip.getValue().isClickable(x, y)) {
                return TemplateAction.forHullSize(chip.getKey());
            }
        }
        for (RowSlot slot : slots) {
            if (slot.template == null) {
                continue;
            }
            if (slot.delete.isClickable(x, y)) {
                return TemplateAction.forTemplate(TemplateAction.Kind.DELETE, slot.template.id());
            }
            if (slot.bounds.contains(x, y)) {
                return TemplateAction.forTemplate(TemplateAction.Kind.SELECT, slot.template.id());
            }
        }
        if (clear.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.CLEAR);
        }
        if (close.isClickable(x, y)) {
            return TemplateAction.of(TemplateAction.Kind.CLOSE);
        }
        return TemplateAction.NONE;
    }

    void render(PositionAPI position, float mouseX, float mouseY, float alphaMult) {
        float width = Math.min(MAX_WIDTH, position.getWidth() - SIDE_CLEARANCE * 2f);
        float height = Math.min(MAX_HEIGHT, position.getHeight() - VERTICAL_CLEARANCE * 2f);
        ScreenRect drawn = frame.render(position, width, height, style.getAccentColor(), alphaMult,
                (frameBox, frameAlpha) -> renderContent(frameBox.left(), frameBox.bottom(), frameBox.width(), frameBox.height(),
                        mouseX, mouseY, frameAlpha));
        if (open && drawn != null) {
            box = drawn;
        }
    }

    private void renderContent(float left, float bottom, float width, float height, float mouseX, float mouseY, float alphaMult) {
        LazyFont font = SkillTreePanelStyle.font();

        float top = bottom + height - PADDING;
        title.draw(left + PADDING, top);
        top -= SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE + 12f;
        float chipBottom = top - CHIP_HEIGHT;
        float chipLeft = left + PADDING;
        for (Map.Entry<HullSize, SkillTreeUiButton> chip : chips.entrySet()) {
            SkillTreeUiButton button = chip.getValue();
            float chipWidth = button.preferredWidth();
            button.place(chipLeft, chipBottom, chipWidth, CHIP_HEIGHT);
            button.setSelected(state.isSelected(chip.getKey()));
            button.setTextColor(state.isSelected(chip.getKey()) ? null : META_COLOR);
            button.render(mouseX, mouseY, alphaMult);
            chipLeft += chipWidth + CHIP_GAP;
        }

        float footerBottom = bottom + PADDING;
        clear.place(left + PADDING, footerBottom, FOOTER_BUTTON_WIDTH, FOOTER_HEIGHT);
        clear.setEnabled(assignedId != null);
        close.place(left + width - PADDING - FOOTER_BUTTON_WIDTH, footerBottom, FOOTER_BUTTON_WIDTH, FOOTER_HEIGHT);
        clear.render(mouseX, mouseY, alphaMult);
        close.render(mouseX, mouseY, alphaMult);

        float listTop = chipBottom - 12f;
        float listBottom = footerBottom + FOOTER_HEIGHT + 12f;
        listArea = new ScreenRect(left + PADDING, listBottom, width - PADDING * 2f, Math.max(0f, listTop - listBottom));
        visibleRows = Math.max(0, (int) ((listTop - listBottom + ROW_GAP) / (ROW_HEIGHT + ROW_GAP)));
        renderRows(font, mouseX, mouseY, alphaMult, listTop);
    }

    private void renderRows(LazyFont font, float mouseX, float mouseY, float alphaMult, float listTop) {
        List<SkillTreeTemplate> window = state.window(visibleRows);
        while (slots.size() < window.size()) {
            slots.add(new RowSlot());
        }
        for (int i = 0; i < slots.size(); i++) {
            RowSlot slot = slots.get(i);
            if (i >= window.size() || font == null) {
                slot.unbind();
                continue;
            }
            float rowBottom = listTop - (i + 1) * ROW_HEIGHT - i * ROW_GAP;
            slot.bind(font, window.get(i), listArea.width() - DELETE_WIDTH - 24f);
            slot.render(listArea.left(), rowBottom, listArea.width(), mouseX, mouseY, alphaMult);
        }
        if (window.isEmpty()) {
            emptyLine.set(state.hasAnyForRoot() ? noMatchText : emptyRootText).draw(listArea.left(), listTop - 8f);
        }
    }

    private final class RowSlot {

        private final SkillTreeUiButton delete = new SkillTreeUiButton(deleteText);
        private SkillTreeTemplate template;
        private boolean boundArmed;
        private boolean boundAssigned;
        private final ReusableText name = new ReusableText(FONT_SIZE, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
        private final ReusableText meta = new ReusableText(FONT_SIZE, META_COLOR);
        private ScreenRect bounds = ScreenRect.NONE;

        void bind(LazyFont font, SkillTreeTemplate value, float textWidth) {
            boolean armed = deleteConfirmation.isArmed(value.id());
            boolean assigned = value.id().equals(assignedId);
            if (value.equals(template) && armed == boundArmed && assigned == boundAssigned) {
                return;
            }
            template = value;
            boundArmed = armed;
            boundAssigned = assigned;
            meta.set(metaText(value, assigned)).setColor(assigned ? SkillTreePanelStyle.POSITIVE_STAT_COLOR : META_COLOR);
            float nameWidth = textWidth - meta.width() - META_GAP;
            String fittedName = SkillTreeTextField.fitStart(value.name(), nameWidth, text -> font.calcWidth(text, FONT_SIZE));
            name.set(fittedName);
            delete.setLabel(armed ? confirmText : deleteText);
            delete.setTextColor(armed ? CONFIRM_COLOR : null);
        }

        void unbind() {
            template = null;
            bounds = ScreenRect.NONE;
            delete.hide();
        }

        void render(float left, float bottom, float width, float mouseX, float mouseY, float alphaMult) {
            bounds = new ScreenRect(left, bottom, width - DELETE_WIDTH - 8f, ROW_HEIGHT);
            float fill = bounds.contains(mouseX, mouseY) ? ROW_HOVER_ALPHA : ROW_FILL_ALPHA;
            GLDraw.fillQuad(left, bottom, width, ROW_HEIGHT, SkillTreePanelStyle.GLOW_COLOR, fill * alphaMult);
            if (boundAssigned) {
                GLDraw.strokeQuad(left, bottom, width, ROW_HEIGHT, style.getAccentColor(), 1.5f, alphaMult);
            }
            float textY = bottom + ROW_HEIGHT / 2f + FONT_SIZE / 2f;
            name.draw(left + 12f, textY);
            meta.draw(left + width - DELETE_WIDTH - 12f - meta.width(), textY);
            delete.place(left + width - DELETE_WIDTH, bottom + 4f, DELETE_WIDTH, ROW_HEIGHT - 8f);
            delete.render(mouseX, mouseY, alphaMult);
        }

        private String metaText(SkillTreeTemplate value, boolean assigned) {
            List<StyledText> parts = new ArrayList<>();
            if (TemplateFilter.isFilterable(value.hullSize())) {
                parts.add(Translation.styled("hullSize." + value.hullSize().name()));
            }
            parts.add(Translation.msg("ui.template.list.nodes").count(value.knownStepCount()).styled());
            if (assigned) {
                parts.add(Translation.styled("ui.template.list.inUse"));
            }
            return Translation.list(parts).plain();
        }
    }
}
