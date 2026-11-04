package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemName;
import exiledsector.items.SlottedLine;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.socket.AtlasAnomalyPanel.Entry;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.Rects;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class ChartInfoWindow {

    static final float WIDTH = 420f;
    private static final float PAD = 16f;
    private static final float ANOMALY_ICON_SIZE = 44f;
    private static final float ANOMALY_ICON_GAP = 8f;
    private static final float ANOMALY_SLOT_EDGE_ALPHA = 0.35f;
    private static final float ANOMALY_HOVER_GLOW_WIDTH = 8f;
    private static final float ANOMALY_HOVER_GLOW_ALPHA = 0.5f;
    private static final float TITLE_FONT_SIZE = 22f;
    private static final float HEADING_FONT_SIZE = 18f;
    private static final float OBJECTIVE_NAME_FONT_SIZE = 16f;
    private static final float BODY_FONT_SIZE = 15f;
    private static final float SECTION_GAP = 16f;
    private static final float ENTRY_GAP = 10f;
    private static final float LINE_GAP = 3f;
    private static final float SCROLL_STEP = 40f;
    private static final float WRAP_HEIGHT = 10000f;
    private static final float TOOLTIP_MAX_TEXT_HEIGHT = 800f;
    private static final Color ACCENT = SkillTreePanelStyle.GLOW_COLOR;

    private interface Block {

        float gapBefore();

        float height();

        void draw(float left, float top, float alphaMult);

        default void dispose() {
        }
    }

    private record TextBlock(TextLabel title, TextLabel body, float gapBefore) implements Block {

        @Override
        public float height() {
            return title.height() + (body == null ? 0f : LINE_GAP + body.height());
        }

        @Override
        public void draw(float left, float top, float alphaMult) {
            title.setAlpha(alphaMult).draw(left, top);
            if (body != null) {
                body.setAlpha(alphaMult).draw(left, top - title.height() - LINE_GAP);
            }
        }

        @Override
        public void dispose() {
            title.dispose();
            if (body != null) {
                body.dispose();
            }
        }
    }

    private final class TooltipLine implements Block {

        private final TextLabel label;
        private final Entry tooltip;
        private final float gapBefore;

        private TooltipLine(TextLabel label, Entry tooltip, float gapBefore) {
            this.label = label;
            this.tooltip = tooltip;
            this.gapBefore = gapBefore;
        }

        @Override
        public float gapBefore() {
            return gapBefore;
        }

        @Override
        public float height() {
            return label.height();
        }

        @Override
        public void draw(float left, float top, float alphaMult) {
            label.setAlpha(alphaMult).draw(left, top);
            if (tooltip != null && hoverable && Rects.contains(left, top - label.height(), label.width(), label.height(), mouseX, mouseY)) {
                hoveredEntry = tooltip;
            }
        }

        @Override
        public void dispose() {
            label.dispose();
        }
    }

    private record AnomalySlot(Entry entry, boolean hasIcon) {
    }

    private final class AnomalyGrid implements Block {

        private final List<AnomalySlot> slots;

        private AnomalyGrid(List<AnomalySlot> slots) {
            this.slots = slots;
        }

        @Override
        public float gapBefore() {
            return ENTRY_GAP;
        }

        @Override
        public float height() {
            int rows = (slots.size() + perRow() - 1) / perRow();
            return rows * ANOMALY_ICON_SIZE + Math.max(0, rows - 1) * ANOMALY_ICON_GAP;
        }

        @Override
        public void draw(float left, float top, float alphaMult) {
            for (int index = 0; index < slots.size(); index++) {
                AnomalySlot slot = slots.get(index);
                float slotLeft = left + (index % perRow()) * (ANOMALY_ICON_SIZE + ANOMALY_ICON_GAP);
                float slotBottom = top - (index / perRow() + 1) * ANOMALY_ICON_SIZE - (index / perRow()) * ANOMALY_ICON_GAP;
                boolean hovered = hoverable && Rects.contains(slotLeft, slotBottom, ANOMALY_ICON_SIZE, ANOMALY_ICON_SIZE, mouseX, mouseY);
                drawSlot(slot, slotLeft, slotBottom, hovered, alphaMult);
                if (hovered) {
                    hoveredEntry = slot.entry();
                }
            }
        }

        private void drawSlot(AnomalySlot slot, float slotLeft, float slotBottom, boolean hovered, float alphaMult) {
            GLDraw.fillQuad(slotLeft, slotBottom, ANOMALY_ICON_SIZE, ANOMALY_ICON_SIZE, BorderedPanel.BACKGROUND_COLOR, alphaMult);
            if (slot.hasIcon()) {
                SpriteDraw.drawAtCenter(spriteCache, slot.entry().iconPath(), slotLeft + ANOMALY_ICON_SIZE / 2f, slotBottom + ANOMALY_ICON_SIZE / 2f,
                        ANOMALY_ICON_SIZE, ANOMALY_ICON_SIZE, Color.WHITE, alphaMult);
            }
            if (hovered) {
                GLDraw.innerGlow(slotLeft, slotBottom, ANOMALY_ICON_SIZE, ANOMALY_ICON_SIZE, ANOMALY_HOVER_GLOW_WIDTH, ACCENT,
                        ANOMALY_HOVER_GLOW_ALPHA * alphaMult);
            }
            GLDraw.strokeQuad(slotLeft, slotBottom, ANOMALY_ICON_SIZE, ANOMALY_ICON_SIZE, ACCENT, 1f,
                    (hovered ? 1f : ANOMALY_SLOT_EDGE_ALPHA) * alphaMult);
        }

        private static int perRow() {
            return Math.max(1, (int) ((WIDTH - PAD * 2f + ANOMALY_ICON_GAP) / (ANOMALY_ICON_SIZE + ANOMALY_ICON_GAP)));
        }
    }

    private final SpriteCache spriteCache = new SpriteCache(ChartInfoWindow.class);
    private final SkillTreePanelStyle tooltipStyle = new SkillTreePanelStyle();
    private final TextLabel tooltipTitle = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel tooltipBody = new TextLabel(SkillTreePanelStyle.TOOLTIP_BODY_FONT, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
    private final List<Block> blocks = new ArrayList<>();
    private float contentHeight;
    private float scrollOffset;
    private float mouseX;
    private float mouseY;
    private boolean hoverable;
    private Entry hoveredEntry;
    private Entry tooltipEntry;

    void show(ChartInfoText.Content content) {
        dispose();
        Color textColor = Misc.getTextColor();
        Color grayColor = Misc.getGrayColor();
        float fullWidth = WIDTH - PAD * 2f;
        addTitle(content.chartName(), content.summary(), fullWidth, grayColor);
        addAnomalies(content.anomalies(), fullWidth, grayColor);
        addObjective(content.objective(), fullWidth, textColor);
        addLines("chart.panel.rewards", content.rewardLines(), fullWidth, textColor);
        addModifiers(content.modifierLines(), fullWidth, textColor);
        contentHeight = 0f;
        for (Block block : blocks) {
            contentHeight += block.gapBefore() + block.height();
        }
        scrollOffset = 0f;
        hoveredEntry = null;
        tooltipEntry = null;
    }

    float height(float maxHeight) {
        return Math.min(contentHeight + PAD * 2f, maxHeight);
    }

    void scroll(int wheel, float shownHeight) {
        float maxOffset = Math.max(0f, contentHeight - (shownHeight - PAD * 2f));
        scrollOffset = Math.max(0f, Math.min(maxOffset, scrollOffset - Math.signum(wheel) * SCROLL_STEP));
    }

    void render(float left, float bottom, float width, float height, float alphaMult, boolean fullyOpen, float pointerX, float pointerY) {
        float clipBottom = bottom + PAD / 2f;
        float clipHeight = height - PAD;
        mouseX = pointerX;
        mouseY = pointerY;
        hoverable = fullyOpen && Rects.contains(left, clipBottom, width, clipHeight, pointerX, pointerY);
        hoveredEntry = null;
        try (GlScope clip = fullyOpen ? clipTo(left, clipBottom, width, clipHeight) : null) {
            float top = bottom + height - PAD + scrollOffset;
            for (Block block : blocks) {
                top -= block.gapBefore();
                block.draw(left + PAD, top, alphaMult);
                top -= block.height();
            }
        }
    }

    void renderTooltip(float alphaMult) {
        if (hoveredEntry == null) {
            return;
        }
        if (hoveredEntry != tooltipEntry) {
            tooltipEntry = hoveredEntry;
            tooltipTitle.setWrapped(hoveredEntry.title(), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT);
            tooltipBody.setParagraphs(List.of(hoveredEntry.body()), SkillTreePanelStyle.TOOLTIP_MAX_TEXT_WIDTH, TOOLTIP_MAX_TEXT_HEIGHT,
                    SkillTreePanelStyle::standardHighlightColor);
        }
        tooltipStyle.drawTitleBodyTooltip(tooltipTitle, tooltipBody, mouseX, mouseY, alphaMult);
    }

    void dispose() {
        blocks.forEach(Block::dispose);
        blocks.clear();
    }

    private void addTitle(ItemName chartName, String summary, float fullWidth, Color grayColor) {
        String title = chartName == null ? Translation.text("chart.panel.title") : chartName.title();
        Color titleColor = chartName == null ? Color.WHITE : chartName.rarity().color();
        List<StyledText> subtitle = new ArrayList<>();
        if (chartName != null && chartName.baseName() != null) {
            subtitle.add(StyledText.of(chartName.baseName()));
        }
        subtitle.add(StyledText.of(summary));
        TextLabel titleLabel = new TextLabel(TITLE_FONT_SIZE, titleColor).setWrapped(title, fullWidth, WRAP_HEIGHT);
        blocks.add(new TextBlock(titleLabel, paragraph(StyledText.join(StyledText.of("\n"), subtitle), fullWidth, grayColor), 0f));
    }

    private void addAnomalies(List<Entry> anomalies, float fullWidth, Color grayColor) {
        blocks.add(heading("chart.atlas.focus.title"));
        if (anomalies.isEmpty()) {
            blocks.add(new TextBlock(paragraph(StyledText.of(Translation.text("chart.atlas.focus.empty")), fullWidth, grayColor), null, ENTRY_GAP));
            return;
        }
        List<AnomalySlot> slots = new ArrayList<>();
        for (Entry entry : anomalies) {
            slots.add(new AnomalySlot(entry, !entry.iconPath().isEmpty() && spriteCache.ensureLoaded(entry.iconPath())));
        }
        blocks.add(new AnomalyGrid(slots));
    }

    private void addObjective(ChartInfoText.Objective objective, float fullWidth, Color textColor) {
        if (objective == null) {
            return;
        }
        blocks.add(heading("chart.panel.objective"));
        TextLabel name = new TextLabel(OBJECTIVE_NAME_FONT_SIZE, Color.WHITE).setWrapped(objective.name(), fullWidth, WRAP_HEIGHT);
        blocks.add(new TextBlock(name, paragraph(objective.body(), fullWidth, textColor), ENTRY_GAP));
    }

    private void addLines(String headingKey, List<StyledText> lines, float fullWidth, Color textColor) {
        if (lines.isEmpty()) {
            return;
        }
        blocks.add(heading(headingKey));
        blocks.add(new TextBlock(paragraph(StyledText.join(StyledText.of("\n"), lines), fullWidth, textColor), null, ENTRY_GAP));
    }

    private void addModifiers(List<SlottedLine> lines, float fullWidth, Color textColor) {
        if (lines.isEmpty()) {
            return;
        }
        blocks.add(heading("chart.panel.modifiers"));
        float gapBefore = ENTRY_GAP;
        for (SlottedLine line : lines) {
            Entry tooltip = line.hasDetails() ? new Entry(line.detailTitle() == null ? "" : line.detailTitle(), "",
                    StyledText.join(StyledText.of("\n"), line.details())) : null;
            blocks.add(new TooltipLine(paragraph(line.text(), fullWidth, textColor), tooltip, gapBefore));
            gapBefore = 0f;
        }
    }

    private TextBlock heading(String key) {
        return new TextBlock(new TextLabel(HEADING_FONT_SIZE, Misc.getHighlightColor()).set(Translation.text(key)), null, SECTION_GAP);
    }

    private static TextLabel paragraph(StyledText text, float maxWidth, Color color) {
        return new TextLabel(BODY_FONT_SIZE, color).setParagraphs(List.of(text), maxWidth, WRAP_HEIGHT, SkillTreePanelStyle::standardHighlightColor);
    }

    private static GlScope clipTo(float left, float bottom, float width, float height) {
        float screenScale = Global.getSettings().getScreenScaleMult();
        return GlScope.clip((int) Math.floor(left * screenScale), (int) Math.floor(bottom * screenScale),
                (int) Math.ceil(width * screenScale), (int) Math.ceil(height * screenScale));
    }
}
