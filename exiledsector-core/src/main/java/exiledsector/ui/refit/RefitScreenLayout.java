package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import org.apache.log4j.Logger;

public record RefitScreenLayout(float frameX, float frameY, float frameWidth, float frameHeight,
                                float shipAreaX, float shipAreaY, float shipAreaWidth, float shipAreaHeight, float shipZoom) {

    static final float MAX_TAB_WIDTH = 1250f;
    static final float MAX_TAB_HEIGHT = 850f;
    public static final float MIN_TAB_WIDTH = 1010f;
    static final float MIN_TAB_HEIGHT = 734f;
    static final float SCREEN_WIDTH_MARGIN = 20f;
    static final float SCREEN_HEIGHT_MARGIN = 74f;
    static final float FRAME_BORDER = 1f;
    static final float FLEET_LIST_WIDTH = 210f;
    static final float TAB_INSET = 3f;
    static final int CURRENT_ZOOM_FIELD = 2;
    private static final Logger LOG = Logger.getLogger(RefitScreenLayout.class);
    private static boolean measureFailureLogged;

    public float shipCentreX() {
        return shipAreaX + shipAreaWidth / 2f;
    }

    public float shipCentreY() {
        return shipAreaY + shipAreaHeight / 2f;
    }

    public float frameTop() {
        return frameY + frameHeight;
    }

    public static RefitScreenLayout measure() {
        try {
            RefitScreenLayout measured = measured(Global.getSector().getCampaignUI());
            if (measured != null) {
                return measured;
            }
        } catch (Throwable e) {
            if (!measureFailureLogged) {
                measureFailureLogged = true;
                LOG.warn("[ExiledSector] Couldn't measure the refit screen; the Hull Upgrades screen uses the vanilla layout sizes instead", e);
            }
        }
        return fallback(Global.getSettings().getScreenWidth(), Global.getSettings().getScreenHeight());
    }

    private static RefitScreenLayout measured(CampaignUIAPI campaignUI) throws Throwable {
        Object tab = UiReflection.call(UiReflection.coreUI(campaignUI), "getCurrentTab");
        Object shipDisplay = UiReflection.call(RefitShipDisplay.refitPanelOf(tab), RefitShipDisplay.GET_SHIP_DISPLAY);
        if (!(tab instanceof UIComponentAPI tabComponent) || !(shipDisplay instanceof UIComponentAPI shipDisplayComponent)) {
            return null;
        }
        PositionAPI framePosition = UiReflection.call(tab, "getParent") instanceof UIComponentAPI wrapper ? wrapper.getPosition() : null;
        PositionAPI tabPosition = tabComponent.getPosition();
        PositionAPI shipPosition = shipDisplayComponent.getPosition();
        float frameX = framePosition != null ? framePosition.getX() : tabPosition.getX() - FRAME_BORDER;
        float frameY = framePosition != null ? framePosition.getY() : tabPosition.getY() - FRAME_BORDER;
        float frameWidth = framePosition != null ? framePosition.getWidth() : tabPosition.getWidth() + FRAME_BORDER * 2f;
        float frameHeight = framePosition != null ? framePosition.getHeight() : tabPosition.getHeight() + FRAME_BORDER * 2f;
        return new RefitScreenLayout(frameX, frameY, frameWidth, frameHeight, shipPosition.getX(), shipPosition.getY(),
                shipPosition.getWidth(), shipPosition.getHeight(), currentZoom(UiReflection.declaredFloats(shipDisplay)));
    }

    static float currentZoom(float[] shipDisplayFloats) {
        if (shipDisplayFloats.length <= CURRENT_ZOOM_FIELD) {
            return 1f;
        }
        float minZoom = shipDisplayFloats[0];
        float maxZoom = shipDisplayFloats[1];
        float zoom = shipDisplayFloats[CURRENT_ZOOM_FIELD];
        return zoom > 0f && minZoom > 0f && zoom >= minZoom - 0.001f && zoom <= maxZoom + 0.001f ? zoom : 1f;
    }

    static RefitScreenLayout fallback(float screenWidth, float screenHeight) {
        float tabWidth = Math.min(MAX_TAB_WIDTH, Math.max(screenWidth - SCREEN_WIDTH_MARGIN, MIN_TAB_WIDTH));
        float tabHeight = Math.min(MAX_TAB_HEIGHT, Math.max(MIN_TAB_HEIGHT, screenHeight - SCREEN_HEIGHT_MARGIN));
        float frameWidth = tabWidth + FRAME_BORDER * 2f;
        float frameHeight = tabHeight + FRAME_BORDER * 2f;
        float frameX = (screenWidth - frameWidth) / 2f;
        float frameY = (screenHeight - frameHeight) / 2f;
        float shipAreaX = frameX + FRAME_BORDER + FLEET_LIST_WIDTH;
        float shipAreaY = frameY + FRAME_BORDER + TAB_INSET;
        return new RefitScreenLayout(frameX, frameY, frameWidth, frameHeight, shipAreaX, shipAreaY,
                tabWidth - FLEET_LIST_WIDTH - TAB_INSET, tabHeight - TAB_INSET * 2f, 1f);
    }
}
