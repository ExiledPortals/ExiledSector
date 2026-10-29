package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.i18n.Translation;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.ui.stats.ShipStatSheet;
import exiledsector.ui.stats.ShipStatSheets;
import exiledsector.ui.stats.StatSheetLayout;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.Rects;
import java.util.List;
import java.util.Set;

final class SkillTreeStatPanel {

    private static final float PADDING = 22f;
    private static final float COMPACT_CONTENT_WIDTH = 430f;
    private static final float MIN_SHEET_COLUMN_WIDTH = 400f;
    private static final float SHEET_COLUMN_GAP = 24f;
    private static final float TOGGLE_GAP = 8f;
    private static final float SCROLL_STEP = 60f;

    private record Viewport(float left, float bottom, float width, float height) {

        float contentLeft() {
            return left + PADDING;
        }

        float contentTop() {
            return bottom + height - PADDING;
        }

        float contentHeight() {
            return height - PADDING * 2f;
        }

        boolean contains(float x, float y) {
            return Rects.contains(left, bottom, width, height, x, y);
        }

        boolean contentContains(float x, float y) {
            return Rects.contains(contentLeft(), bottom + PADDING, width - PADDING * 2f, contentHeight(), x, y);
        }
    }

    private static final class View {

        private final ShipStatSheet.Detail detail;
        private final HoloFrame frame = new HoloFrame(HoloFrame.Look.PANEL);
        private ShipStatSheet sheet;
        private int builtGeneration = -1;
        private StatSheetLayout layout;
        private ShipStatSheet layoutSheet;
        private float layoutWidth = Float.NaN;
        private int layoutColumns;
        private float scrollOffset;
        private Viewport drawnViewport;

        View(ShipStatSheet.Detail detail) {
            this.detail = detail;
        }

        StatSheetLayout layout(float width, int columns) {
            if (layout == null || layoutSheet != sheet || layoutWidth != width || layoutColumns != columns) {
                if (layout != null) {
                    layout.dispose();
                }
                layout = StatSheetLayout.build(sheet, width, columns);
                layoutSheet = sheet;
                layoutWidth = width;
                layoutColumns = columns;
            }
            return layout;
        }

        float maxScroll(Viewport viewport) {
            return layout == null ? 0f : Math.max(0f, layout.height() - viewport.contentHeight());
        }
    }

    private final FleetMemberAPI member;
    private final ShipVariantAPI variant;
    private final StatBreakdownTooltip breakdownTooltip;
    private final SkillTreeUiButton toggleButton = new SkillTreeUiButton("");
    private final SkillTreeUiButton expandButton = new SkillTreeUiButton("");
    private final String showLabel = Translation.text("ui.stats.show");
    private final String hideLabel = Translation.text("ui.stats.hide");
    private final String expandLabel = Translation.text("ui.stats.expand");
    private final String collapseLabel = Translation.text("ui.stats.collapse");
    private final View compactView = new View(ShipStatSheet.Detail.STANDARD);
    private final View sheetView = new View(ShipStatSheet.Detail.FULL);
    private final float toggleWidth;
    private final float expandWidth;

    private ShipOpBudget budget;
    private int statsRevision;
    private int sheetGeneration;
    private boolean panelOpen;
    private boolean expanded;
    private StatSheetLayout.Region hoveredRegion;
    private float placedToggleRight = Float.NaN;
    private float placedToggleTop = Float.NaN;

    SkillTreeStatPanel(FleetMemberAPI member, ShipVariantAPI variant, SkillTreePanelStyle panelStyle) {
        this.member = member;
        this.variant = variant;
        this.breakdownTooltip = new StatBreakdownTooltip(panelStyle);
        toggleWidth = buttonWidth(toggleButton, hideLabel, showLabel);
        expandWidth = buttonWidth(expandButton, expandLabel, collapseLabel);
        toggleButton.setLabel(showLabel);
        expandButton.setLabel(expandLabel);
    }

    private static float buttonWidth(SkillTreeUiButton button, String firstLabel, String secondLabel) {
        button.setLabel(firstLabel);
        float firstWidth = button.preferredWidth();
        button.setLabel(secondLabel);
        return Math.max(ChromeMetrics.MIN_BUTTON_WIDTH, Math.max(firstWidth, button.preferredWidth()));
    }

    void refresh(ShipOpBudget budget, int revision) {
        if (this.budget != null && revision == statsRevision) return;
        this.budget = budget;
        statsRevision = revision;
        sheetGeneration++;
    }

    boolean isOpen() {
        return panelOpen;
    }

    boolean isExpanded() {
        return panelOpen && expanded;
    }

    void toggle() {
        if (panelOpen) {
            close();
        } else {
            open();
        }
    }

    private void open() {
        panelOpen = true;
        expanded = false;
        compactView.frame.open();
        toggleButton.setLabel(hideLabel);
        expandButton.setLabel(expandLabel);
        SkillTreeSounds.panelOpened();
    }

    void close() {
        if (!panelOpen) return;
        panelOpen = false;
        expanded = false;
        compactView.frame.close();
        sheetView.frame.close();
        toggleButton.setLabel(showLabel);
        expandButton.setLabel(expandLabel);
        hoveredRegion = null;
    }

    void toggleExpanded() {
        if (!panelOpen) return;
        expanded = !expanded;
        (expanded ? sheetView : compactView).frame.open();
        (expanded ? compactView : sheetView).frame.close();
        expandButton.setLabel(expanded ? collapseLabel : expandLabel);
        hoveredRegion = null;
        SkillTreeSounds.panelOpened();
    }

    boolean collapse() {
        if (!isExpanded()) return false;
        toggleExpanded();
        return true;
    }

    void advance(float amount) {
        compactView.frame.advance(amount);
        sheetView.frame.advance(amount);
    }

    boolean isToggleHit(float x, float y) {
        return toggleButton.isClickable(x, y);
    }

    boolean isExpandHit(float x, float y) {
        return panelOpen && expandButton.isClickable(x, y);
    }

    boolean contains(float x, float y) {
        if (toggleButton.contains(x, y) || (panelOpen && expandButton.contains(x, y))) return true;
        Viewport viewport = activeView().drawnViewport;
        return panelOpen && viewport != null && viewport.contains(x, y);
    }

    boolean sheetContains(float x, float y) {
        Viewport viewport = sheetView.drawnViewport;
        return isExpanded() && viewport != null && viewport.contains(x, y);
    }

    boolean handleScroll(float x, float y, int wheelValue) {
        View view = activeView();
        Viewport viewport = view.drawnViewport;
        if (!panelOpen || viewport == null || !viewport.contains(x, y)) {
            return false;
        }
        float step = wheelValue > 0 ? -SCROLL_STEP : SCROLL_STEP;
        view.scrollOffset = Math.max(0f, Math.min(view.maxScroll(viewport), view.scrollOffset + step));
        return true;
    }

    Set<String> hoveredNodeIds() {
        return hoveredRegion == null ? Set.of() : hoveredRegion.breakdown().nodeIds();
    }

    void render(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult, float toggleAlpha, boolean toggleLive) {
        toggleButton.setEnabled(toggleLive);
        expandButton.setEnabled(toggleLive);
        placeButtons(canvasPosition, toggleAlpha);
        hoveredRegion = null;
        compactView.drawnViewport = null;
        if (SkillTreePanelStyle.font() != null && (panelOpen || compactView.frame.isVisible())) {
            ensureSheet(compactView);
            renderView(compactView, compactViewport(canvasPosition), 1, mouseX, mouseY, alphaMult, toggleLive && !expanded);
        }
        if (toggleAlpha > 0f) {
            toggleButton.render(mouseX, mouseY, alphaMult * toggleAlpha);
            if (panelOpen) {
                expandButton.render(mouseX, mouseY, alphaMult * toggleAlpha);
            }
        }
    }

    void renderSheet(PositionAPI canvasPosition, float mouseX, float mouseY, float alphaMult, boolean live) {
        sheetView.drawnViewport = null;
        if (SkillTreePanelStyle.font() == null || !(isExpanded() || sheetView.frame.isVisible())) {
            return;
        }
        ensureSheet(sheetView);
        Viewport viewport = sheetViewport(canvasPosition);
        float contentWidth = viewport.width() - PADDING * 2f;
        int columns = Math.max(1, (int) ((contentWidth + SHEET_COLUMN_GAP) / (MIN_SHEET_COLUMN_WIDTH + SHEET_COLUMN_GAP)));
        renderView(sheetView, viewport, columns, mouseX, mouseY, alphaMult, live && expanded);
    }

    void renderTooltip(float mouseX, float mouseY, float alphaMult) {
        if (hoveredRegion != null) {
            breakdownTooltip.render(hoveredRegion.breakdown(), mouseX, mouseY, alphaMult);
        }
    }

    private void renderView(View view, Viewport viewport, int columns, float mouseX, float mouseY, float alphaMult, boolean live) {
        if (view.sheet == null || view.sheet.isEmpty() || !view.frame.isVisible()) {
            return;
        }
        StatSheetLayout layout = view.layout(viewport.width() - PADDING * 2f, columns);
        Viewport fitted = view == compactView ? fittedCompact(viewport, layout) : viewport;
        view.drawnViewport = fitted;
        view.scrollOffset = Math.min(view.scrollOffset, view.maxScroll(fitted));
        float contentTop = fitted.contentTop() + view.scrollOffset;
        StatSheetLayout.Region hovered = live && fitted.contentContains(mouseX, mouseY)
                ? layout.regionAt(fitted.contentLeft(), contentTop, mouseX, mouseY) : null;
        if (hovered != null) {
            hoveredRegion = hovered;
        }
        view.frame.render(fitted.left(), fitted.bottom(), fitted.width(), fitted.height(), SkillTreePanelStyle.GLOW_COLOR, alphaMult,
                (frameLeft, frameBottom, frameWidth, frameHeight, frameAlpha) -> renderClipped(layout, fitted, contentTop, frameAlpha, hovered,
                        view.frame.contentAlpha()));
    }

    private static void renderClipped(StatSheetLayout layout, Viewport viewport, float contentTop, float alphaMult, StatSheetLayout.Region hovered,
                                      float revealed) {
        float screenScale = Global.getSettings().getScreenScaleMult();
        float clipTop = viewport.bottom() + viewport.height() - PADDING / 2f;
        float revealedBottom = viewport.bottom() + viewport.height() * (1f - Math.min(1f, revealed));
        float clipBottomY = Math.max(viewport.bottom() + PADDING / 2f, revealedBottom);
        if (clipBottomY >= clipTop) {
            return;
        }
        int clipLeft = (int) Math.floor(viewport.left() * screenScale);
        int clipBottom = (int) Math.floor(clipBottomY * screenScale);
        int clipWidth = (int) Math.ceil(viewport.width() * screenScale);
        int clipHeight = (int) Math.ceil((clipTop - clipBottomY) * screenScale);
        try (GlScope clip = GlScope.clip(clipLeft, clipBottom, clipWidth, clipHeight)) {
            layout.render(viewport.contentLeft(), contentTop, alphaMult, hovered);
        }
    }

    private View activeView() {
        return expanded ? sheetView : compactView;
    }

    private void ensureSheet(View view) {
        if (view.sheet == null || view.builtGeneration != sheetGeneration) {
            view.sheet = buildSheet(view.detail);
            view.builtGeneration = sheetGeneration;
        }
    }

    private ShipStatSheet buildSheet(ShipStatSheet.Detail detail) {
        if (member == null) {
            return new ShipStatSheet(List.of());
        }
        return ShipStatSheets.build(new ShipStatSheets.Input(member.getStats(), member.getHullSpec(), member, ShipSkillDataManager.get(member.getId()),
                SkillTreeHullMod.resolvedTree(member, variant), budget, detail));
    }

    private Viewport compactViewport(PositionAPI canvasPosition) {
        float width = COMPACT_CONTENT_WIDTH + PADDING * 2f;
        float top = panelTop(canvasPosition);
        float bottom = canvasPosition.getY() + ChromeMetrics.EDGE_MARGIN;
        return new Viewport(canvasRight(canvasPosition) - width, bottom, width, top - bottom);
    }

    private static Viewport fittedCompact(Viewport available, StatSheetLayout layout) {
        float height = Math.min(available.height(), layout.height() + PADDING * 2f);
        return new Viewport(available.left(), available.bottom() + available.height() - height, available.width(), height);
    }

    private Viewport sheetViewport(PositionAPI canvasPosition) {
        float left = canvasPosition.getX() + ChromeMetrics.EDGE_MARGIN * 2f + SkillTreeReadoutBar.BAR_WIDTH;
        float top = panelTop(canvasPosition);
        float bottom = canvasPosition.getY() + ChromeMetrics.EDGE_MARGIN;
        return new Viewport(left, bottom, canvasRight(canvasPosition) - left, top - bottom);
    }

    private static float panelTop(PositionAPI canvasPosition) {
        return canvasPosition.getY() + canvasPosition.getHeight() - ChromeMetrics.EDGE_MARGIN - ChromeMetrics.CONTROL_HEIGHT - TOGGLE_GAP;
    }

    private static float canvasRight(PositionAPI canvasPosition) {
        return canvasPosition.getX() + canvasPosition.getWidth() - ChromeMetrics.EDGE_MARGIN;
    }

    private void placeButtons(PositionAPI canvasPosition, float toggleAlpha) {
        if (toggleAlpha <= 0f) {
            toggleButton.hide();
            expandButton.hide();
            placedToggleRight = Float.NaN;
            return;
        }
        float toggleRight = canvasRight(canvasPosition);
        float toggleTop = canvasPosition.getY() + canvasPosition.getHeight() - ChromeMetrics.EDGE_MARGIN;
        if (toggleRight == placedToggleRight && toggleTop == placedToggleTop) return;
        float buttonBottom = toggleTop - ChromeMetrics.CONTROL_HEIGHT;
        toggleButton.place(toggleRight - toggleWidth, buttonBottom, toggleWidth, ChromeMetrics.CONTROL_HEIGHT);
        expandButton.place(toggleRight - toggleWidth - TOGGLE_GAP - expandWidth, buttonBottom, expandWidth, ChromeMetrics.CONTROL_HEIGHT);
        placedToggleRight = toggleRight;
        placedToggleTop = toggleTop;
    }
}
