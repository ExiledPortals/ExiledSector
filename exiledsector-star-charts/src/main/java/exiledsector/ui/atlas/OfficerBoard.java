package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.FactionAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
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
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TextLabel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class OfficerBoard {

    static final float OPEN_SECONDS = 0.2f;
    static final float INSET = 16f;
    static final float HEADER_HEIGHT = 40f;
    static final float MAX_LABEL_WIDTH = 104f;
    static final float BACKGROUND_ALPHA = 0.8f;
    static final int GHOST_COUNT = 7;
    static final float GHOST_SIZE_RATIO = 0.3f;
    static final float GHOST_SCATTER_RATIO = 0.35f;
    static final String GHOST_SEED_PREFIX = "officer_";
    static final float DIMMED_ALPHA = 0.4f;
    static final float FRAME_WIDTH = 1.5f;
    static final float HOVERED_FRAME_WIDTH = 3f;
    static final float ARROW_TRIM_SHARE = 0.6f;
    static final float SOLID_BULGE = 0.1f;
    static final float DASHED_BULGE = 0.2f;
    static final int ARROW_SEGMENTS = 24;
    static final float ARROWHEAD_SHARE = 0.18f;
    static final float MARK_SHARE = 0.25f;
    static final float DEFEND_MARK_SHARE = 0.28f;
    static final String RANK_MARK_PATH = "graphics/icons/ranks/rank1.png";
    static final float RANK_MARK_ASPECT = 11f / 16f;
    static final String DEFEND_MARK_PATH = "graphics/warroom/taskicons/icon_defend.png";
    static final String HELP_ICON_PATH = "graphics/ui/icons/fleettab/more_info.png";
    static final float HELP_ICON_SIZE = 24f;
    static final Color SLOT_FILL = new Color(20, 24, 30);
    static final Color DIMMED_TINT = new Color(150, 150, 150);
    static final Color ROSE_COLOUR = SkillTreePanelStyle.POSITIVE_STAT_COLOR;
    static final Color FELL_COLOUR = SkillTreePanelStyle.NEGATIVE_STAT_COLOR;
    static final Color FALLBACK_FRAME_COLOUR = new Color(150, 150, 150);

    private record Face(boolean identified, boolean dimmed, String portraitPath, Color frameColour, AtlasCell commandCentre) {
    }

    private record ArrowShape(OfficerBoardArrows.Arrow arrow, float[] points) {

        @Override
        public boolean equals(Object other) {
            return other instanceof ArrowShape shape && arrow.equals(shape.arrow) && Arrays.equals(points, shape.points);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arrow, Arrays.hashCode(points));
        }

        @Override
        public String toString() {
            return "ArrowShape[arrow=" + arrow + ", points=" + Arrays.toString(points) + "]";
        }
    }

    private final SpriteCache sprites = new SpriteCache(OfficerBoard.class);
    private final SensorGhostRenderer ghosts = new SensorGhostRenderer(GHOST_COUNT, GHOST_SIZE_RATIO, GHOST_SCATTER_RATIO);
    private final OfficerBoardBeams beams = new OfficerBoardBeams();
    private final OfficerBoardLegend legend = new OfficerBoardLegend(ghosts, beams, sprites);
    private final OfficerBoardTooltip tooltip = new OfficerBoardTooltip(sprites);
    private final TextLabel titleLabel = new TextLabel(UiFonts.TITLE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR);
    private final List<TextLabel> rankLabels = new ArrayList<>();
    private final Map<Integer, AtlasOfficer> records = new HashMap<>();
    private final Map<Integer, Face> faces = new HashMap<>();
    private OfficerBoardShuffle shuffle;
    private List<OfficerBoardLayout.Link> shownLinks = List.of();
    private List<OfficerBoardArrows.Arrow> arrows = List.of();
    private Set<Integer> shields = Set.of();
    private List<ArrowShape> arrowShapes = List.of();
    private boolean replayable;
    private boolean open;
    private float openLevel;
    private float shuffleSeconds = OfficerBoardShuffle.DURATION_SECONDS;
    private Integer hoveredRecordId;
    private boolean helpHovered;
    private float regionLeft;
    private float regionBottom;
    private float regionWidth;
    private float regionHeight;
    private float availableLeft;
    private float availableBottom;
    private float availableWidth;
    private float availableHeight;
    private float labelColumnWidth;
    private OfficerBoardGeometry geometry;

    void open(AtlasState state) {
        TurnResult lastTurn = OfficerTurns.lastTurn(state);
        records.clear();
        faces.clear();
        List<OfficerBoardLayout.Entry> entries = new ArrayList<>();
        for (AtlasOfficer officer : state.officers()) {
            records.put(officer.recordId(), officer);
            entries.add(new OfficerBoardLayout.Entry(officer.recordId(), officer.rankId(), officer.renown(), officer.seat()));
        }
        lastTurn.fallen().forEach(records::putIfAbsent);
        shuffle = OfficerBoardShuffle.of(OfficerRanks.all(), entries, lastTurn.standingChanges());
        records.forEach((recordId, officer) -> faces.put(recordId, face(officer)));
        shownLinks = linksBetweenIdentified(shuffle.after());
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
        helpHovered = false;
        open = true;
    }

    private List<OfficerBoardLayout.Link> linksBetweenIdentified(OfficerBoardLayout layout) {
        List<OfficerBoardLayout.Link> links = new ArrayList<>();
        for (OfficerBoardLayout.Link link : layout.links()) {
            if (identified(layout.recordIdAt(link.subordinate())) && identified(layout.recordIdAt(link.superior()))) {
                links.add(link);
            }
        }
        return List.copyOf(links);
    }

    private boolean identified(Integer recordId) {
        Face face = recordId == null ? null : faces.get(recordId);
        return face != null && face.identified();
    }

    private boolean settledAndIdentified(int recordId) {
        AtlasOfficer officer = records.get(recordId);
        return officer != null && officer.identified() && shuffle.after().contains(recordId);
    }

    private Face face(AtlasOfficer officer) {
        FactionAPI faction = OfficerText.faction(officer);
        return new Face(officer.identified(), !officer.isActive(), officer.identified() ? OfficerText.portraitPath(officer) : null,
                faction == null ? FALLBACK_FRAME_COLOUR : faction.getBaseUIColor(), officer.isActive() ? officer.commandCentre() : null);
    }

    private void writeLabels(OfficerBoardLayout layout) {
        titleLabel.set(Translation.text("chart.officer.board.title"));
        rankLabels.clear();
        float widestLabel = 0f;
        for (int row = 0; row < layout.rowCount(); row++) {
            TextLabel label = new TextLabel(UiFonts.BODY, SkillTreePanelStyle.TOOLTIP_BODY_COLOR);
            label.setWrapped(OfficerRanks.rank(layout.rankIdOfRow(row)).displayName(), MAX_LABEL_WIDTH, HEADER_HEIGHT);
            rankLabels.add(label);
            widestLabel = Math.max(widestLabel, label.width());
        }
        labelColumnWidth = (float) Math.ceil(widestLabel) + INSET;
        legend.write();
    }

    void close() {
        open = false;
        hoveredRecordId = null;
        helpHovered = false;
        tooltip.cleanup();
    }

    void openHoveredCodex() {
        if (open && hoveredRecordId != null) {
            tooltip.openCodex(hoveredRecordId);
        }
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
        if (geometry != null && left == availableLeft && bottom == availableBottom && width == availableWidth && height == availableHeight) {
            return;
        }
        availableLeft = left;
        availableBottom = bottom;
        availableWidth = width;
        availableHeight = height;
        float rowsPreferredWidth = OfficerBoardGeometry.preferredRowsWidth(shuffle.widestRow());
        float contentWidth = Math.max(titleLabel.width() + INSET + HELP_ICON_SIZE, labelColumnWidth + rowsPreferredWidth);
        regionWidth = Math.min(width, contentWidth + INSET * 2f);
        regionHeight = Math.min(height, OfficerBoardGeometry.preferredRowsHeight(shuffle.after().rowCount()) + INSET * 2f + HEADER_HEIGHT);
        regionLeft = left + width - regionWidth;
        regionBottom = bottom;
        geometry = OfficerBoardGeometry.fit(rowsLeft(), rowsBottom(), rowsWidth(), rowsHeight(), shuffle.after().rowCount(), shuffle.widestRow());
        arrowShapes = arrowShapes();
    }

    float left() {
        return regionLeft;
    }

    private float rowsLeft() {
        return regionLeft + INSET + labelColumnWidth;
    }

    private float rowsBottom() {
        return regionBottom + INSET;
    }

    private float rowsWidth() {
        return regionWidth - INSET * 2f - labelColumnWidth;
    }

    private float rowsHeight() {
        return regionHeight - INSET * 2f - HEADER_HEIGHT;
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
                shapes.add(new ArrowShape(arrow, points));
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
        beams.advance(amount);
        if (shuffleSeconds < OfficerBoardShuffle.DURATION_SECONDS) {
            shuffleSeconds = Math.min(OfficerBoardShuffle.DURATION_SECONDS, shuffleSeconds + amount);
        }
        hoveredRecordId = open && geometry != null ? officerAt(mouseX, mouseY) : null;
        helpHovered = open && geometry != null && Math.abs(mouseX - helpIconCentreX()) <= HELP_ICON_SIZE / 2f
                && Math.abs(mouseY - helpIconCentreY()) <= HELP_ICON_SIZE / 2f;
    }

    private float helpIconCentreX() {
        return regionLeft + regionWidth - INSET - HELP_ICON_SIZE / 2f;
    }

    private float helpIconCentreY() {
        return regionBottom + regionHeight - INSET - titleLabel.height() / 2f;
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
        GLDraw.fillQuad(regionLeft, regionBottom, regionWidth, regionHeight, Color.BLACK, BACKGROUND_ALPHA * alpha);
        BorderedPanel.drawEdges(regionLeft, regionBottom, regionWidth, regionHeight, alpha);
        titleLabel.setAlpha(alpha).draw(regionLeft + INSET, regionBottom + regionHeight - INSET);
        SpriteDraw.drawAtCenter(sprites, HELP_ICON_PATH, helpIconCentreX(), helpIconCentreY(), HELP_ICON_SIZE, HELP_ICON_SIZE, Color.WHITE, alpha);
        drawRows(alpha);
        drawLinks(alpha);
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
            tooltip.show(hovered);
            tooltip.draw(accent, mouseX, mouseY, alpha);
        } else if (helpHovered) {
            legend.drawTooltip(accent, mouseX, mouseY, alpha);
        }
    }

    private void drawRows(float alpha) {
        float labelLeft = regionLeft + INSET;
        for (int row = 0; row < rankLabels.size(); row++) {
            TextLabel label = rankLabels.get(row);
            label.setAlpha(alpha).draw(labelLeft, geometry.rowCentreY(row) + label.height() / 2f);
        }
    }

    private void drawLinks(float alpha) {
        float half = geometry.portraitSize() / 2f;
        OfficerBoardLayout.Seat hoveredSeat = hoveredRecordId == null ? null : shuffle.after().seat(hoveredRecordId);
        for (OfficerBoardLayout.Link link : shownLinks) {
            beams.link(geometry.centreX(link.subordinate()), geometry.centreY(link.subordinate()) + half, geometry.centreX(link.superior()),
                    geometry.centreY(link.superior()) - half, hoveredSeat != null && link.touches(hoveredSeat), alpha);
        }
        beams.flush();
    }

    private void drawOfficer(int recordId, float centreX, float centreY, float alpha) {
        float size = geometry.portraitSize();
        float left = centreX - size / 2f;
        float bottom = centreY - size / 2f;
        Face face = faces.get(recordId);
        if (face == null || !face.identified()) {
            ghosts.draw(centreX, centreY, size, alpha, GHOST_SEED_PREFIX + recordId);
            return;
        }
        float shown = face.dimmed() ? alpha * DIMMED_ALPHA : alpha;
        Color tint = face.dimmed() ? DIMMED_TINT : Color.WHITE;
        GLDraw.fillQuad(left, bottom, size, size, SLOT_FILL, alpha);
        if (face.portraitPath() != null) {
            SpriteDraw.drawAtCenter(sprites, face.portraitPath(), centreX, centreY, size, size, tint, shown);
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
            beams.arrow(shape.points(), shape.arrow().style(), headLength, alpha * OfficerBoardArrows.alpha(shape.arrow(), hoveredRecordId));
        }
        beams.flush();
    }

    private void drawShields(float alpha) {
        if (alpha <= 0f || shields.isEmpty()) {
            return;
        }
        float size = geometry.portraitSize();
        for (int recordId : shields) {
            OfficerBoardLayout.Seat seat = shuffle.after().seat(recordId);
            defendMark(sprites, geometry.centreX(seat) + size / 2f, geometry.centreY(seat) + size / 2f, size * DEFEND_MARK_SHARE, alpha);
        }
    }

    static void defendMark(SpriteCache sprites, float centreX, float centreY, float size, float alpha) {
        SpriteDraw.drawAtCenter(sprites, DEFEND_MARK_PATH, centreX, centreY, size, size, Color.WHITE, alpha);
    }

    private void drawRankMarks(float alpha) {
        if (alpha <= 0f) {
            return;
        }
        float size = geometry.portraitSize();
        for (OfficerBoardShuffle.Mover mover : shuffle.movers()) {
            if (mover.mark() != OfficerBoardShuffle.RankMark.NONE) {
                rankMark(sprites, centreX(mover) - size / 2f, centreY(mover) + size / 2f, size * MARK_SHARE,
                        mover.mark() == OfficerBoardShuffle.RankMark.ROSE, alpha);
            }
        }
    }

    static void rankMark(SpriteCache sprites, float centreX, float centreY, float width, boolean rose, float alpha) {
        SpriteDraw.drawAtCenter(sprites, RANK_MARK_PATH, centreX, centreY, width, width * RANK_MARK_ASPECT, rose ? ROSE_COLOUR : FELL_COLOUR, alpha,
                rose ? 0f : 180f);
    }
}
