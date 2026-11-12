package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.turn.OfficerTurns;
import exiledsector.charts.officer.turn.TurnResult;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.UiFonts;
import exiledsector.ui.decoration.SensorGhostRenderer;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.LineBatch;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TextLabel;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class OfficerBoard {

    static final float OPEN_SECONDS = 0.2f;
    static final float INSET = 16f;
    static final float HEADER_HEIGHT = 40f;
    static final float FOOTER_HEIGHT = 28f;
    static final float LABEL_COLUMN_WIDTH = 120f;
    static final int GHOST_COUNT = 7;
    static final float GHOST_SIZE_RATIO = 0.3f;
    static final float GHOST_SCATTER_RATIO = 0.35f;
    static final String GHOST_SEED_PREFIX = "officer_";
    static final float FLAG_SHARE = 0.45f;
    static final float DIMMED_ALPHA = 0.4f;
    static final float FRAME_WIDTH = 1.5f;
    static final float HOVERED_FRAME_WIDTH = 3f;
    static final float ROW_LINE_ALPHA = 0.08f;
    static final float ARROW_WIDTH = 2f;
    static final float ARROW_TRIM_SHARE = 0.6f;
    static final float SOLID_BULGE = 0.1f;
    static final float DASHED_BULGE = 0.2f;
    static final int ARROW_SEGMENTS = 24;
    static final float ARROWHEAD_SHARE = 0.18f;
    static final float ARROWHEAD_SPREAD = 0.5f;
    static final float MARK_SHARE = 0.25f;
    static final float SHIELD_SHARE = 0.28f;
    static final Color SLOT_FILL = new Color(20, 24, 30);
    static final Color DIMMED_TINT = new Color(150, 150, 150);
    static final Color ROSE_COLOUR = SkillTreePanelStyle.POSITIVE_STAT_COLOR;
    static final Color FELL_COLOUR = SkillTreePanelStyle.NEGATIVE_STAT_COLOR;
    static final Color SHIELD_COLOUR = SkillTreePanelStyle.GLOW_COLOR;
    static final Color FALLBACK_FRAME_COLOUR = new Color(150, 150, 150);

    private record Face(boolean identified, boolean dimmed, String portraitPath, String logoPath, float logoAspect, Color frameColour,
                        AtlasCell commandCentre) {
    }

    private record ArrowShape(OfficerBoardArrows.Arrow arrow, float[] points, Color colour) {
    }

    private final SpriteCache sprites = new SpriteCache(OfficerBoard.class);
    private final SensorGhostRenderer ghosts = new SensorGhostRenderer(GHOST_COUNT, GHOST_SIZE_RATIO, GHOST_SCATTER_RATIO);
    private final LineBatch arrowLines = new LineBatch(ARROW_WIDTH);
    private final OfficerBoardTooltip tooltip = new OfficerBoardTooltip(sprites);
    private final TextLabel titleLabel = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final TextLabel hintLabel = new TextLabel(UiFonts.HINT, AtlasScreen.HINT_COLOUR);
    private final List<TextLabel> rankLabels = new ArrayList<>();
    private final Map<Integer, AtlasOfficer> records = new HashMap<>();
    private final Map<Integer, Face> faces = new HashMap<>();
    private AtlasGenerator generator;
    private OfficerBoardShuffle shuffle;
    private List<OfficerBoardArrows.Arrow> arrows = List.of();
    private Set<Integer> shields = Set.of();
    private List<ArrowShape> arrowShapes = List.of();
    private boolean replayable;
    private boolean open;
    private float openLevel;
    private float shuffleSeconds = OfficerBoardShuffle.DURATION_SECONDS;
    private Integer hoveredRecordId;
    private float regionLeft;
    private float regionBottom;
    private float regionWidth;
    private float regionHeight;
    private OfficerBoardGeometry geometry;

    void open(AtlasGenerator atlasGenerator, AtlasState state) {
        generator = atlasGenerator;
        TurnResult lastTurn = OfficerTurns.lastTurn(state);
        records.clear();
        faces.clear();
        List<OfficerBoardLayout.Entry> entries = new ArrayList<>();
        for (AtlasOfficer officer : state.officers()) {
            records.put(officer.recordId(), officer);
            entries.add(new OfficerBoardLayout.Entry(officer.recordId(), officer.rankId(), officer.renown()));
        }
        lastTurn.fallen().forEach(records::putIfAbsent);
        List<String> rankIds = new ArrayList<>();
        for (OfficerRank rank : OfficerRanks.all()) {
            rankIds.add(rank.id());
        }
        shuffle = OfficerBoardShuffle.of(rankIds, entries, lastTurn.standingChanges());
        records.forEach((recordId, officer) -> faces.put(recordId, face(officer)));
        arrows = OfficerBoardArrows.arrows(lastTurn.resolvedIntents(), OfficerTurns.declaredIntents(state), this::settledAndIdentified);
        shields = OfficerBoardArrows.shields(OfficerTurns.declaredIntents(state), this::settledAndIdentified);
        replayable = lastTurn.turn() > 0;
        boolean unseen = OfficerTurns.hasUnseenTurn(state);
        shuffleSeconds = unseen && replayable ? 0f : OfficerBoardShuffle.DURATION_SECONDS;
        if (unseen) {
            state.markOfficerTurnSeen();
        }
        I18n.forGameText(() -> writeLabels(shuffle.after()));
        geometry = null;
        hoveredRecordId = null;
        open = true;
    }

    private boolean settledAndIdentified(int recordId) {
        AtlasOfficer officer = records.get(recordId);
        return officer != null && officer.identified() && shuffle.after().contains(recordId);
    }

    private Face face(AtlasOfficer officer) {
        FactionAPI faction = OfficerText.faction(officer);
        String logoPath = faction == null ? null : faction.getLogo();
        SpriteAPI logo = logoPath == null || logoPath.isEmpty() ? null : sprites.texture(logoPath);
        float aspect = logo == null || logo.getWidth() <= 0f ? 0f : logo.getHeight() / logo.getWidth();
        return new Face(officer.identified(), !officer.isActive(), officer.identified() ? OfficerText.portraitPath(officer) : null,
                aspect > 0f ? logoPath : null, aspect, faction == null ? FALLBACK_FRAME_COLOUR : faction.getBaseUIColor(),
                officer.isActive() ? officer.commandCentre() : null);
    }

    private void writeLabels(OfficerBoardLayout layout) {
        titleLabel.set(Translation.text("chart.officer.board.title"));
        hintLabel.set(Translation.text("chart.officer.board.hint"));
        rankLabels.clear();
        for (int row = 0; row < layout.rowCount(); row++) {
            TextLabel label = new TextLabel(UiFonts.BODY, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
            label.setWrapped(OfficerRanks.rank(layout.rankIdOfRow(row)).displayName(), LABEL_COLUMN_WIDTH - INSET, HEADER_HEIGHT);
            rankLabels.add(label);
        }
    }

    void close() {
        open = false;
        hoveredRecordId = null;
    }

    boolean isOpen() {
        return open;
    }

    boolean isShowing() {
        return open || openLevel > 0f;
    }

    boolean canReplay() {
        return replayable;
    }

    void replay() {
        shuffleSeconds = 0f;
    }

    void place(float left, float bottom, float width, float height) {
        if (geometry != null && left == regionLeft && bottom == regionBottom && width == regionWidth && height == regionHeight) {
            return;
        }
        regionLeft = left;
        regionBottom = bottom;
        regionWidth = width;
        regionHeight = height;
        geometry = OfficerBoardGeometry.fit(rowsLeft(), rowsBottom(), rowsWidth(), rowsHeight(), shuffle.after().rowCount(), shuffle.widestRow());
        arrowShapes = arrowShapes();
    }

    private float rowsLeft() {
        return regionLeft + INSET + LABEL_COLUMN_WIDTH;
    }

    private float rowsBottom() {
        return regionBottom + INSET + FOOTER_HEIGHT;
    }

    private float rowsWidth() {
        return regionWidth - INSET * 2f - LABEL_COLUMN_WIDTH;
    }

    private float rowsHeight() {
        return regionHeight - INSET * 2f - HEADER_HEIGHT - FOOTER_HEIGHT;
    }

    boolean contains(float x, float y) {
        return x >= regionLeft && x <= regionLeft + regionWidth && y >= regionBottom && y <= regionBottom + regionHeight;
    }

    private List<ArrowShape> arrowShapes() {
        List<ArrowShape> shapes = new ArrayList<>();
        float trim = geometry.portraitSize() * ARROW_TRIM_SHARE;
        for (OfficerBoardArrows.Arrow arrow : arrows) {
            OfficerBoardLayout.Seat from = shuffle.after().seat(arrow.fromRecordId());
            OfficerBoardLayout.Seat to = shuffle.after().seat(arrow.toRecordId());
            float bulge = arrow.style() == OfficerBoardArrows.Style.SOLID ? SOLID_BULGE : DASHED_BULGE;
            float[] points = OfficerBoardArrows.curve(geometry.centreX(from), geometry.centreY(from), geometry.centreX(to), geometry.centreY(to), trim,
                    bulge, ARROW_SEGMENTS);
            if (points.length > 0) {
                shapes.add(new ArrowShape(arrow, points, faces.get(arrow.fromRecordId()).frameColour()));
            }
        }
        return List.copyOf(shapes);
    }

    void advance(float amount, float mouseX, float mouseY) {
        openLevel = Fade.step(openLevel, open, amount / OPEN_SECONDS);
        if (!isShowing()) {
            return;
        }
        ghosts.advance(amount);
        if (shuffleSeconds < OfficerBoardShuffle.DURATION_SECONDS) {
            shuffleSeconds = Math.min(OfficerBoardShuffle.DURATION_SECONDS, shuffleSeconds + amount);
        }
        hoveredRecordId = open && geometry != null ? officerAt(mouseX, mouseY) : null;
    }

    private Integer officerAt(float x, float y) {
        float half = geometry.portraitSize() / 2f;
        for (OfficerBoardShuffle.Mover mover : shuffle.movers()) {
            Face face = faces.get(mover.recordId());
            if (mover.departs() || face == null || !face.identified()) {
                continue;
            }
            float centreX = centreX(mover);
            float centreY = centreY(mover);
            if (Math.abs(x - centreX) <= half && Math.abs(y - centreY) <= half) {
                return mover.recordId();
            }
        }
        return null;
    }

    AtlasCell clickedCommandCentre(float x, float y) {
        Integer recordId = geometry == null ? null : officerAt(x, y);
        Face face = recordId == null ? null : faces.get(recordId);
        return face == null ? null : face.commandCentre();
    }

    boolean clickedOfficer(float x, float y) {
        return geometry != null && officerAt(x, y) != null;
    }

    private float centreX(OfficerBoardShuffle.Mover mover) {
        if (mover.arrives()) {
            return geometry.centreX(mover.to());
        }
        if (mover.departs()) {
            return geometry.centreX(mover.from());
        }
        return OfficerBoardShuffle.lerp(geometry.centreX(mover.from()), geometry.centreX(mover.to()), OfficerBoardShuffle.slideProgress(shuffleSeconds));
    }

    private float centreY(OfficerBoardShuffle.Mover mover) {
        if (mover.arrives()) {
            return geometry.centreY(mover.to());
        }
        if (mover.departs()) {
            return geometry.centreY(mover.from());
        }
        return OfficerBoardShuffle.lerp(geometry.centreY(mover.from()), geometry.centreY(mover.to()), OfficerBoardShuffle.slideProgress(shuffleSeconds));
    }

    private float moverAlpha(OfficerBoardShuffle.Mover mover) {
        if (mover.arrives()) {
            return OfficerBoardShuffle.arriveAlpha(shuffleSeconds);
        }
        if (mover.departs()) {
            return OfficerBoardShuffle.departAlpha(shuffleSeconds);
        }
        return 1f;
    }

    void render(float alphaMult, float mouseX, float mouseY, Color accent) {
        float alpha = Fade.smooth(openLevel) * alphaMult;
        if (alpha <= 0f || geometry == null) {
            return;
        }
        GLDraw.fillQuad(regionLeft, regionBottom, regionWidth, regionHeight, Color.BLACK, alpha);
        BorderedPanel.draw(regionLeft, regionBottom, regionWidth, regionHeight, alpha);
        titleLabel.setAlpha(alpha).draw(regionLeft + INSET, regionBottom + regionHeight - INSET);
        hintLabel.setAlpha(alpha).draw(regionLeft + INSET, regionBottom + INSET + hintLabel.height());
        drawRows(alpha);
        drawArrows(alpha * OfficerBoardShuffle.arrowAlpha(shuffleSeconds));
        for (OfficerBoardShuffle.Mover mover : shuffle.movers()) {
            float moverAlpha = moverAlpha(mover) * alpha;
            if (moverAlpha > 0f) {
                drawOfficer(mover.recordId(), centreX(mover), centreY(mover), moverAlpha);
            }
        }
        drawShields(alpha * OfficerBoardShuffle.arrowAlpha(shuffleSeconds));
        drawRankMarks(alpha * OfficerBoardShuffle.markAlpha(shuffleSeconds));
        if (open && hoveredRecordId != null) {
            AtlasOfficer hovered = records.get(hoveredRecordId);
            tooltip.show(hovered, generator);
            tooltip.draw(accent, mouseX, mouseY, alpha);
        }
    }

    private void drawRows(float alpha) {
        float lineLeft = regionLeft + INSET;
        float lineWidth = regionWidth - INSET * 2f;
        for (int row = 0; row < rankLabels.size(); row++) {
            TextLabel label = rankLabels.get(row);
            float centreY = geometry.rowCentreY(row);
            label.setAlpha(alpha).draw(lineLeft, centreY + label.height() / 2f);
            if (row > 0) {
                GLDraw.fillQuad(lineLeft, centreY + geometry.rowPitch() / 2f, lineWidth, 1f, Color.WHITE, ROW_LINE_ALPHA * alpha);
            }
        }
    }

    private void drawOfficer(int recordId, float centreX, float centreY, float alpha) {
        float size = geometry.portraitSize();
        float left = centreX - size / 2f;
        float bottom = centreY - size / 2f;
        Face face = faces.get(recordId);
        if (face == null || !face.identified()) {
            GLDraw.fillQuad(left, bottom, size, size, SLOT_FILL, alpha * DIMMED_ALPHA);
            ghosts.draw(centreX, centreY, size, alpha, GHOST_SEED_PREFIX + recordId);
            return;
        }
        float shown = face.dimmed() ? alpha * DIMMED_ALPHA : alpha;
        Color tint = face.dimmed() ? DIMMED_TINT : Color.WHITE;
        GLDraw.fillQuad(left, bottom, size, size, SLOT_FILL, alpha);
        if (face.portraitPath() != null) {
            SpriteDraw.drawAtCenter(sprites, face.portraitPath(), centreX, centreY, size, size, tint, shown);
        }
        if (face.logoPath() != null) {
            float flagWidth = size * FLAG_SHARE;
            float flagHeight = flagWidth * face.logoAspect();
            SpriteDraw.drawAtCenter(sprites, face.logoPath(), left + size - flagWidth / 2f, bottom + flagHeight / 2f, flagWidth, flagHeight, tint, shown);
        }
        boolean hovered = Integer.valueOf(recordId).equals(hoveredRecordId);
        GLDraw.strokeQuad(left, bottom, size, size, face.frameColour(), hovered ? HOVERED_FRAME_WIDTH : FRAME_WIDTH, shown);
    }

    private void drawArrows(float alpha) {
        if (alpha <= 0f || arrowShapes.isEmpty()) {
            return;
        }
        float headLength = geometry.portraitSize() * ARROWHEAD_SHARE;
        for (ArrowShape shape : arrowShapes) {
            float arrowAlpha = alpha * OfficerBoardArrows.alpha(shape.arrow(), hoveredRecordId);
            float[] points = shape.points();
            int segments = points.length / 2 - 1;
            boolean dashed = shape.arrow().style() == OfficerBoardArrows.Style.DASHED;
            for (int segment = 0; segment < segments; segment++) {
                if (!dashed || segment % 2 == 0) {
                    arrowLines.add(points[segment * 2], points[segment * 2 + 1], points[segment * 2 + 2], points[segment * 2 + 3], shape.colour(), arrowAlpha);
                }
            }
            addArrowhead(points, headLength, shape.colour(), arrowAlpha);
        }
        try (GlScope scope = GlScope.flat()) {
            arrowLines.flush();
        }
        arrowLines.clear();
    }

    private void addArrowhead(float[] points, float headLength, Color colour, float alpha) {
        int last = points.length - 2;
        float tipX = points[last];
        float tipY = points[last + 1];
        float dx = tipX - points[last - 2];
        float dy = tipY - points[last - 1];
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0f) {
            return;
        }
        float backX = -dx / length * headLength;
        float backY = -dy / length * headLength;
        float sideX = -backY * ARROWHEAD_SPREAD;
        float sideY = backX * ARROWHEAD_SPREAD;
        arrowLines.add(tipX, tipY, tipX + backX + sideX, tipY + backY + sideY, colour, alpha);
        arrowLines.add(tipX, tipY, tipX + backX - sideX, tipY + backY - sideY, colour, alpha);
    }

    private void drawShields(float alpha) {
        if (alpha <= 0f || shields.isEmpty()) {
            return;
        }
        float size = geometry.portraitSize();
        float shieldSize = size * SHIELD_SHARE;
        try (GlScope scope = GlScope.flat()) {
            for (int recordId : shields) {
                OfficerBoardLayout.Seat seat = shuffle.after().seat(recordId);
                float centreX = geometry.centreX(seat) + size / 2f;
                float centreY = geometry.centreY(seat) + size / 2f;
                shield(centreX, centreY, shieldSize, SHIELD_COLOUR, alpha);
            }
        }
    }

    private static void shield(float centreX, float centreY, float size, Color colour, float alpha) {
        float half = size / 2f;
        float shoulderY = centreY + half * 0.2f;
        Misc.setColor(Color.BLACK, alpha);
        shieldOutline(GL11.GL_POLYGON, centreX, centreY, half + 1.5f, shoulderY);
        Misc.setColor(colour, alpha);
        shieldOutline(GL11.GL_POLYGON, centreX, centreY, half, shoulderY);
    }

    private static void shieldOutline(int mode, float centreX, float centreY, float half, float shoulderY) {
        GL11.glBegin(mode);
        GL11.glVertex2f(centreX - half, centreY + half);
        GL11.glVertex2f(centreX + half, centreY + half);
        GL11.glVertex2f(centreX + half, shoulderY);
        GL11.glVertex2f(centreX, centreY - half);
        GL11.glVertex2f(centreX - half, shoulderY);
        GL11.glEnd();
    }

    private void drawRankMarks(float alpha) {
        if (alpha <= 0f) {
            return;
        }
        float size = geometry.portraitSize();
        float markSize = size * MARK_SHARE;
        try (GlScope scope = GlScope.flat()) {
            for (OfficerBoardShuffle.Mover mover : shuffle.movers()) {
                if (mover.mark() == OfficerBoardShuffle.RankMark.NONE) {
                    continue;
                }
                boolean rose = mover.mark() == OfficerBoardShuffle.RankMark.ROSE;
                float centreX = centreX(mover) - size / 2f;
                float centreY = centreY(mover) + size / 2f;
                triangle(centreX, centreY, markSize, rose, rose ? ROSE_COLOUR : FELL_COLOUR, alpha);
            }
        }
    }

    private static void triangle(float centreX, float centreY, float size, boolean pointsUp, Color colour, float alpha) {
        float half = size / 2f;
        float tipY = pointsUp ? centreY + half : centreY - half;
        float baseY = pointsUp ? centreY - half : centreY + half;
        Misc.setColor(colour, alpha);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(centreX, tipY);
        GL11.glVertex2f(centreX - half, baseY);
        GL11.glVertex2f(centreX + half, baseY);
        GL11.glEnd();
    }
}
