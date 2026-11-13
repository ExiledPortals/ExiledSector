package exiledsector.ui.atlas;

import exiledsector.i18n.Translation;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.UiFonts;
import exiledsector.ui.decoration.SensorGhostRenderer;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.LineBatch;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

final class OfficerBoardLegend {

    static final float GLYPH_WIDTH = 28f;
    static final float GLYPH_HEIGHT = 18f;
    static final float GLYPH_GAP = 8f;
    static final float TEXT_WIDTH = 180f;
    static final float COLUMN_GAP = 16f;
    static final float ROW_GAP = 6f;
    static final float HINT_GAP = 10f;
    static final int COLUMNS = 2;
    static final int DASHES = 7;
    static final float ARROWHEAD_LENGTH = 7f;
    static final float SHIELD_SIZE = 14f;
    static final float MARK_SIZE = 12f;
    static final float SLOT_SIZE = 18f;
    static final float GHOST_FOOTPRINT = 30f;
    static final String GHOST_SEED = "officer_legend";
    static final Color ARROW_COLOUR = SkillTreePanelStyle.TOOLTIP_BODY_COLOR;
    static final Color HINT_COLOUR = AtlasScreen.HINT_COLOUR;

    enum Glyph {
        LINK("chart.officer.board.legend.link"),
        IDLE("chart.officer.board.legend.idle"),
        WON("chart.officer.board.legend.won"),
        CHALLENGE("chart.officer.board.legend.challenge"),
        DEFEND("chart.officer.board.legend.defend"),
        UNKNOWN("chart.officer.board.legend.unknown"),
        ROSE("chart.officer.board.legend.rose"),
        FELL("chart.officer.board.legend.fell");

        private final String key;

        Glyph(String key) {
            this.key = key;
        }
    }

    private final SensorGhostRenderer ghosts;
    private final LineBatch arrowLines = new LineBatch(OfficerBoard.ARROW_WIDTH);
    private final LineBatch linkLines = new LineBatch(OfficerBoard.LINK_WIDTH);
    private final Map<Glyph, TextLabel> labels = new EnumMap<>(Glyph.class);
    private final TextLabel hintLabel = new TextLabel(UiFonts.HINT, HINT_COLOUR);

    OfficerBoardLegend(SensorGhostRenderer ghosts) {
        this.ghosts = ghosts;
    }

    void write() {
        for (Glyph glyph : Glyph.values()) {
            labels.computeIfAbsent(glyph, unused -> new TextLabel(UiFonts.BODY, SkillTreePanelStyle.TOOLTIP_BODY_COLOR))
                    .setWrapped(Translation.text(glyph.key), TEXT_WIDTH, Float.MAX_VALUE);
        }
        hintLabel.setWrapped(Translation.text("chart.officer.board.legend.hint"), width(), Float.MAX_VALUE);
    }

    static float columnWidth() {
        return GLYPH_WIDTH + GLYPH_GAP + TEXT_WIDTH;
    }

    static float width() {
        return columnWidth() * COLUMNS + COLUMN_GAP * (COLUMNS - 1);
    }

    static int rowCount() {
        return (Glyph.values().length + COLUMNS - 1) / COLUMNS;
    }

    private float rowHeight(int row) {
        float height = GLYPH_HEIGHT;
        for (int column = 0; column < COLUMNS; column++) {
            int index = row * COLUMNS + column;
            if (index < Glyph.values().length) {
                height = Math.max(height, labels.get(Glyph.values()[index]).height());
            }
        }
        return height;
    }

    float height() {
        float height = 0f;
        for (int row = 0; row < rowCount(); row++) {
            height += rowHeight(row) + ROW_GAP;
        }
        return height - ROW_GAP + HINT_GAP + hintLabel.height();
    }

    void draw(float left, float top, float alpha) {
        float rowTop = top;
        for (int row = 0; row < rowCount(); row++) {
            float height = rowHeight(row);
            float centreY = rowTop - height / 2f;
            for (int column = 0; column < COLUMNS; column++) {
                int index = row * COLUMNS + column;
                if (index >= Glyph.values().length) {
                    continue;
                }
                Glyph glyph = Glyph.values()[index];
                float glyphLeft = left + column * (columnWidth() + COLUMN_GAP);
                drawGlyph(glyph, glyphLeft, centreY, alpha);
                TextLabel label = labels.get(glyph);
                label.setAlpha(alpha).draw(glyphLeft + GLYPH_WIDTH + GLYPH_GAP, centreY + label.height() / 2f);
            }
            rowTop -= height + ROW_GAP;
        }
        try (GlScope scope = GlScope.flat()) {
            linkLines.flush();
            arrowLines.flush();
        }
        linkLines.clear();
        arrowLines.clear();
        hintLabel.setAlpha(alpha).draw(left, rowTop + ROW_GAP - HINT_GAP);
    }

    private void drawGlyph(Glyph glyph, float left, float centreY, float alpha) {
        float centreX = left + GLYPH_WIDTH / 2f;
        float right = left + GLYPH_WIDTH;
        float half = GLYPH_HEIGHT / 2f;
        switch (glyph) {
            case LINK -> linkLines.add(centreX - half / 2f, centreY - half, centreX + half / 2f, centreY + half, Color.WHITE,
                    OfficerBoard.HOVERED_LINK_ALPHA * alpha);
            case WON -> addArrow(left, right, centreY, false, alpha);
            case CHALLENGE -> addArrow(left, right, centreY, true, alpha);
            case DEFEND -> {
                try (GlScope scope = GlScope.flat()) {
                    OfficerBoard.shield(centreX, centreY, SHIELD_SIZE, OfficerBoard.SHIELD_COLOUR, alpha);
                }
            }
            case ROSE, FELL -> {
                boolean rose = glyph == Glyph.ROSE;
                try (GlScope scope = GlScope.flat()) {
                    OfficerBoard.triangle(centreX, centreY, MARK_SIZE, rose, rose ? OfficerBoard.ROSE_COLOUR : OfficerBoard.FELL_COLOUR, alpha);
                }
            }
            case IDLE -> drawSlot(centreX, centreY, OfficerBoard.DIMMED_TINT, OfficerBoard.DIMMED_ALPHA * alpha);
            case UNKNOWN -> {
                GLDraw.fillQuad(centreX - SLOT_SIZE / 2f, centreY - SLOT_SIZE / 2f, SLOT_SIZE, SLOT_SIZE, OfficerBoard.SLOT_FILL,
                        OfficerBoard.DIMMED_ALPHA * alpha);
                ghosts.draw(centreX, centreY, GHOST_FOOTPRINT, alpha, GHOST_SEED);
            }
        }
    }

    private static void drawSlot(float centreX, float centreY, Color frame, float alpha) {
        float slotLeft = centreX - SLOT_SIZE / 2f;
        float slotBottom = centreY - SLOT_SIZE / 2f;
        GLDraw.fillQuad(slotLeft, slotBottom, SLOT_SIZE, SLOT_SIZE, OfficerBoard.SLOT_FILL, alpha);
        GLDraw.strokeQuad(slotLeft, slotBottom, SLOT_SIZE, SLOT_SIZE, frame, OfficerBoard.FRAME_WIDTH, alpha);
    }

    private void addArrow(float left, float right, float centreY, boolean dashed, float alpha) {
        float arrowAlpha = OfficerBoardArrows.RESTING_ALPHA * alpha;
        float tipX = right;
        if (dashed) {
            float dash = (right - left) / (DASHES * 2f - 1f);
            for (int index = 0; index < DASHES; index++) {
                float dashLeft = left + index * dash * 2f;
                arrowLines.add(dashLeft, centreY, Math.min(tipX, dashLeft + dash), centreY, ARROW_COLOUR, arrowAlpha);
            }
        } else {
            arrowLines.add(left, centreY, tipX, centreY, ARROW_COLOUR, arrowAlpha);
        }
        float spread = ARROWHEAD_LENGTH * OfficerBoard.ARROWHEAD_SPREAD;
        arrowLines.add(tipX, centreY, tipX - ARROWHEAD_LENGTH, centreY + spread, ARROW_COLOUR, arrowAlpha);
        arrowLines.add(tipX, centreY, tipX - ARROWHEAD_LENGTH, centreY - spread, ARROW_COLOUR, arrowAlpha);
    }
}
