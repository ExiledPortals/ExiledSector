package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.campaign.listeners.CampaignUIRenderingListener;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.charts.objective.ChartObjectiveIntel;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.util.BorderedPanel;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.HoloFrame;
import exiledsector.ui.util.Rects;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;

public final class ChartInfoTab implements CampaignUIRenderingListener, CampaignInputListener {

    static final float TAB_WIDTH = 24f;
    static final float TAB_HEIGHT = 78f;
    static final float TAB_BOTTOM_BELOW_SCREEN_TOP = 200f;
    static final float WINDOW_BOTTOM_MARGIN = 160f;
    private static final float TAB_ICON_SIZE = 18f;
    private static final float CHEVRON_HALF_WIDTH = 4f;
    private static final float CHEVRON_HALF_HEIGHT = 7f;
    private static final float CHEVRON_LINE_WIDTH = 2f;
    private static final float HOVER_GLOW_WIDTH = 8f;
    private static final float HOVER_GLOW_ALPHA = 0.35f;
    private static final float IDLE_EDGE_ALPHA = 0.6f;
    private static final float REFRESH_SECONDS = 0.5f;
    private static final float MAX_FRAME_SECONDS = 0.1f;
    private static final float NANOS_PER_SECOND = 1e9f;
    private static final int INPUT_PRIORITY = 1200;
    private static final Color ACCENT = SkillTreePanelStyle.GLOW_COLOR;
    private static final String CLICK_SOUND = "ui_button_pressed";

    private final HoloFrame frame = new HoloFrame(HoloFrame.Look.PANEL);
    private final ChartInfoWindow window = new ChartInfoWindow();
    private final SpriteCache spriteCache = new SpriteCache(ChartInfoTab.class);
    private final HoloFrame.Content windowContent = this::renderWindow;
    private ChartInfoText.Key shownKey;
    private boolean expanded;
    private float secondsSinceRefresh;
    private long lastRenderNanos;
    private float mouseX;
    private float mouseY;

    @Override
    public void renderInUICoordsBelowUI(ViewportAPI viewport) {
    }

    @Override
    public void renderInUICoordsAboveUIBelowTooltips(ViewportAPI viewport) {
        float amount = secondsSinceLastRender();
        ChartRun run = shownRun(Global.getSector());
        if (run == null) {
            return;
        }
        frame.advance(amount);
        float alphaMult = viewport.getAlphaMult();
        if (frame.isVisible()) {
            refresh(run, amount);
            float windowHeight = windowHeight();
            frame.render(windowLeft(), tabTop() - windowHeight, ChartInfoWindow.WIDTH, windowHeight, ACCENT, alphaMult, windowContent);
        }
        drawTab(alphaMult);
        if (frame.isVisible() && frame.contentAlpha() >= 1f) {
            window.renderTooltip(alphaMult);
        }
    }

    @Override
    public void renderInUICoordsAboveUIAndTooltips(ViewportAPI viewport) {
    }

    @Override
    public int getListenerInputPriority() {
        return INPUT_PRIORITY;
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {
        if (shownRun(Global.getSector()) == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (!event.isMouseEvent()) {
                continue;
            }
            mouseX = event.getX();
            mouseY = event.getY();
            if (event.isConsumed()) {
                continue;
            }
            if (event.isLMBDownEvent() && overTab(mouseX, mouseY)) {
                toggle();
                event.consume();
            } else if (frame.isVisible() && overWindow(mouseX, mouseY)) {
                if (event.isMouseScrollEvent()) {
                    window.scroll(event.getEventValue(), windowHeight());
                }
                if (!event.isMouseMoveEvent()) {
                    event.consume();
                }
            }
        }
    }

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {
    }

    static ChartRun shownRun(SectorAPI sector) {
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        if (campaignUI == null || campaignUI.isShowingDialog() || campaignUI.isShowingMenu() || campaignUI.getCurrentCoreTab() != null
                || campaignUI.isHideUI()) {
            return null;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        return playerFleet == null ? null : ChartRuns.liveIn(playerFleet.getContainingLocation());
    }

    private void toggle() {
        expanded = !expanded;
        if (expanded) {
            frame.open();
        } else {
            frame.close();
        }
        Global.getSoundPlayer().playUISound(CLICK_SOUND, 1f, 1f);
    }

    private void refresh(ChartRun run, float amount) {
        secondsSinceRefresh += amount;
        boolean sameRun = shownKey != null && shownKey.runId().equals(run.runId());
        if (sameRun && (secondsSinceRefresh < REFRESH_SECONDS || shownKey.matches(run))) {
            return;
        }
        secondsSinceRefresh = 0f;
        shownKey = ChartInfoText.Key.of(run);
        window.show(ChartInfoText.of(run));
    }

    private void renderWindow(float left, float bottom, float width, float height, float alphaMult) {
        window.render(left, bottom, width, height, alphaMult, frame.contentAlpha() >= 1f, mouseX, mouseY);
    }

    private void drawTab(float alphaMult) {
        float left = tabLeft();
        float bottom = tabBottom();
        boolean hovered = overTab(mouseX, mouseY);
        GLDraw.fillQuad(left, bottom, TAB_WIDTH, TAB_HEIGHT, BorderedPanel.BACKGROUND_COLOR, alphaMult);
        if (hovered) {
            GLDraw.innerGlow(left, bottom, TAB_WIDTH, TAB_HEIGHT, HOVER_GLOW_WIDTH, ACCENT, HOVER_GLOW_ALPHA * alphaMult);
        }
        GLDraw.strokeQuad(left, bottom, TAB_WIDTH, TAB_HEIGHT, ACCENT, 1f, (hovered ? 1f : IDLE_EDGE_ALPHA) * alphaMult);
        float centerX = left + TAB_WIDTH / 2f;
        SpriteDraw.drawAtCenter(spriteCache, ChartObjectiveIntel.iconPath(), centerX, bottom + TAB_HEIGHT * 0.7f, TAB_ICON_SIZE, TAB_ICON_SIZE,
                Color.WHITE, alphaMult);
        GLDraw.chevron(centerX, bottom + TAB_HEIGHT * 0.3f, CHEVRON_HALF_WIDTH, CHEVRON_HALF_HEIGHT, !expanded, ACCENT, CHEVRON_LINE_WIDTH, alphaMult);
    }

    private float secondsSinceLastRender() {
        long now = System.nanoTime();
        float amount = lastRenderNanos == 0L ? 0f : Math.min(MAX_FRAME_SECONDS, (now - lastRenderNanos) / NANOS_PER_SECOND);
        lastRenderNanos = now;
        return amount;
    }

    private boolean overTab(float x, float y) {
        return Rects.contains(tabLeft(), tabBottom(), TAB_WIDTH, TAB_HEIGHT, x, y);
    }

    private boolean overWindow(float x, float y) {
        float windowHeight = windowHeight();
        return Rects.contains(windowLeft(), tabTop() - windowHeight, ChartInfoWindow.WIDTH, windowHeight, x, y);
    }

    private float windowHeight() {
        return window.height(tabTop() - WINDOW_BOTTOM_MARGIN);
    }

    private static float tabLeft() {
        return Global.getSettings().getScreenWidth() - TAB_WIDTH;
    }

    private static float tabBottom() {
        return Global.getSettings().getScreenHeight() - TAB_BOTTOM_BELOW_SCREEN_TOP;
    }

    private static float tabTop() {
        return tabBottom() + TAB_HEIGHT;
    }

    private static float windowLeft() {
        return tabLeft() - ChartInfoWindow.WIDTH;
    }
}
