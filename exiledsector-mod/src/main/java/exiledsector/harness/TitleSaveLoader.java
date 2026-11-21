package exiledsector.harness;

import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.ui.refit.UiReflection;

import java.util.Map;

final class TitleSaveLoader {

    private static final String CAMPAIGN_ENGINE = "com.fs.starfarer.campaign.CampaignEngine";
    private static final String GAME_MANAGER = "com.fs.starfarer.campaign.save.CampaignGameManager";
    private static final String CAMPAIGN_STATE_ID = "Campaign State";
    private static final String CAMPAIGN_STATE_SESSION_KEY = "campaign state in session";
    private static final String TITLE_SCREEN_PANEL = "screenPanel";

    private TitleSaveLoader() {
    }

    static UIPanelAPI titleScreenPanel() throws Throwable {
        return UiReflection.fieldNamed(GameStates.current(), TITLE_SCREEN_PANEL, UIPanelAPI.class);
    }

    static String load(String saveDir) throws Throwable {
        Object appDriver = GameStates.appDriver();
        Object titleState = UiReflection.call(appDriver, "getCurrentState");
        Object campaignState = campaignState(appDriver, titleState);
        if (campaignState == null) {
            return "the title screen has no campaign state to load into";
        }
        UiReflection.callStatic(Class.forName(CAMPAIGN_ENGINE), "resetInstance");
        Object loadError = UiReflection.callOnlyStaticAccepting(Class.forName(GAME_MANAGER), String.class, saveDir, campaignState, campaignState);
        if (loadError != null) {
            return loadError.toString();
        }
        UiReflection.call(titleState, "goToState", CAMPAIGN_STATE_ID);
        return null;
    }

    private static Object campaignState(Object appDriver, Object titleState) throws Throwable {
        Map<?, ?> session = UiReflection.fieldNamed(titleState, "session", Map.class);
        Object fromSession = session == null ? null : session.get(CAMPAIGN_STATE_SESSION_KEY);
        return fromSession != null ? fromSession : UiReflection.call(appDriver, "getState", CAMPAIGN_STATE_ID);
    }
}
