package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.UiFonts;
import exiledsector.ui.decoration.SensorGhostRenderer;
import exiledsector.ui.util.GLDraw;
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
    static final float PADDING = 10f;
    static final int COLUMNS = 2;
    static final int ARROW_SEGMENTS = 13;
    static final float ARROWHEAD_LENGTH = 7f;
    static final float DEFEND_MARK_SIZE = 18f;
    static final float RANK_MARK_WIDTH = 16f;
    static final float SLOT_SIZE = 18f;
    static final float GHOST_FOOTPRINT = 30f;
    static final String GHOST_SEED = "officer_legend";

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
    private final OfficerBoardBeams beams;
    private final SpriteCache sprites;
    private final Map<Glyph, TextLabel> labels = new EnumMap<>(Glyph.class);

    OfficerBoardLegend(SensorGhostRenderer ghosts, OfficerBoardBeams beams, SpriteCache sprites) {
        this.ghosts = ghosts;
        this.beams = beams;
        this.sprites = sprites;
    }

    void write() {
        for (Glyph glyph : Glyph.values()) {
            labels.computeIfAbsent(glyph, unused -> new TextLabel(UiFonts.BODY, SkillTreePanelStyle.TOOLTIP_BODY_COLOR))
                    .setWrapped(Translation.text(glyph.key), TEXT_WIDTH, Float.MAX_VALUE);
        }
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
        return height - ROW_GAP;
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
        beams.flush();
    }

    void drawTooltip(Color accent, float mouseX, float mouseY, float alpha) {
        float boxWidth = (float) Math.ceil(width() + PADDING * 2f);
        float boxHeight = (float) Math.ceil(height() + PADDING * 2f);
        float boxX = Math.round(OfficerBoardTooltip.boxLeft(mouseX, boxWidth, Global.getSettings().getScreenWidth()));
        float boxY = Math.round(OfficerBoardTooltip.boxBottom(mouseY, boxHeight, Global.getSettings().getScreenHeight()));
        GLDraw.fillQuad(boxX, boxY, boxWidth, boxHeight, SkillTreePanelStyle.TOOLTIP_BACKGROUND_COLOR, alpha);
        GLDraw.strokeQuad(boxX, boxY, boxWidth, boxHeight, accent, SkillTreePanelStyle.TOOLTIP_BORDER_THICKNESS, alpha);
        draw(boxX + PADDING, boxY + boxHeight - PADDING, alpha);
    }

    private void drawGlyph(Glyph glyph, float left, float centreY, float alpha) {
        float centreX = left + GLYPH_WIDTH / 2f;
        float right = left + GLYPH_WIDTH;
        float half = GLYPH_HEIGHT / 2f;
        switch (glyph) {
            case LINK -> beams.link(centreX - half / 2f, centreY - half, centreX + half / 2f, centreY + half, true, alpha);
            case WON -> addArrow(left, right, centreY, OfficerBoardArrows.Style.SOLID, alpha);
            case CHALLENGE -> addArrow(left, right, centreY, OfficerBoardArrows.Style.DASHED, alpha);
            case DEFEND -> OfficerBoard.defendMark(sprites, centreX, centreY, DEFEND_MARK_SIZE, alpha);
            case ROSE, FELL -> OfficerBoard.rankMark(sprites, centreX, centreY, RANK_MARK_WIDTH, glyph == Glyph.ROSE, alpha);
            case IDLE -> drawSlot(centreX, centreY, OfficerBoard.DIMMED_TINT, OfficerBoard.DIMMED_ALPHA * alpha);
            case UNKNOWN -> ghosts.draw(centreX, centreY, GHOST_FOOTPRINT, alpha, GHOST_SEED);
        }
    }

    private static void drawSlot(float centreX, float centreY, Color frame, float alpha) {
        float slotLeft = centreX - SLOT_SIZE / 2f;
        float slotBottom = centreY - SLOT_SIZE / 2f;
        GLDraw.fillQuad(slotLeft, slotBottom, SLOT_SIZE, SLOT_SIZE, OfficerBoard.SLOT_FILL, alpha);
        GLDraw.strokeQuad(slotLeft, slotBottom, SLOT_SIZE, SLOT_SIZE, frame, OfficerBoard.FRAME_WIDTH, alpha);
    }

    private void addArrow(float left, float right, float centreY, OfficerBoardArrows.Style style, float alpha) {
        beams.arrow(OfficerBoardBeams.straightLine(left, right, centreY, ARROW_SEGMENTS), style, ARROWHEAD_LENGTH, OfficerBoardArrows.RESTING_ALPHA * alpha);
    }
}
