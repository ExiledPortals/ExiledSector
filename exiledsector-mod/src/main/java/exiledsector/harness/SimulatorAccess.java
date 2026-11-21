package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import exiledsector.ui.refit.RefitShipDisplay;
import exiledsector.ui.refit.UiReflection;

final class SimulatorAccess {

    private static final String OPPONENT_PICKER_FIELD = "fdd";
    private static final int DISMISS_WITH_FIRST_OPTION = 0;

    private SimulatorAccess() {
    }

    static CombatEngineAPI combat() {
        return Global.getCurrentState() == GameState.COMBAT ? Global.getCombatEngine() : null;
    }

    static CombatEngineAPI simulation() {
        CombatEngineAPI engine = combat();
        return engine != null && engine.isSimulation() ? engine : null;
    }

    static boolean pressRunSimulation() throws Throwable {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (campaignUI.getCurrentCoreTab() != CoreUITabId.REFIT) {
            return false;
        }
        Object refitTab = UiReflection.call(UiReflection.coreUI(campaignUI), "getCurrentTab");
        Object designDisplay = UiReflection.call(RefitShipDisplay.refitPanelOf(refitTab), "getDesignDisplay");
        Object runSimulationButton = UiReflection.call(designDisplay, "getRunSimulationButton");
        if (runSimulationButton == null) {
            return false;
        }
        UiReflection.call(designDisplay, "actionPerformed", null, runSimulationButton);
        return true;
    }

    static boolean closeOpponentPicker() throws Throwable {
        Object opponentPicker = UiReflection.fieldNamed(GameStates.current(), OPPONENT_PICKER_FIELD, Object.class);
        if (opponentPicker == null) {
            return false;
        }
        UiReflection.call(opponentPicker, "dismiss", DISMISS_WITH_FIRST_OPTION);
        return true;
    }

    static boolean showingDialog(CombatEngineAPI engine) throws Throwable {
        return Boolean.TRUE.equals(UiReflection.call(engine.getCombatUI(), "isShowingDialog"));
    }

    static boolean showingMap(CombatEngineAPI engine) throws Throwable {
        return Boolean.TRUE.equals(UiReflection.call(engine.getCombatUI(), "isShowingCommandUI"));
    }

    static void closeMapAndUnpause(CombatEngineAPI engine) throws Throwable {
        UiReflection.call(engine.getCombatUI(), "hideWarroom");
        engine.setPaused(false);
    }

    static boolean setAutopilot(CombatEngineAPI engine, boolean on) throws Throwable {
        if (!UiReflection.hasMethod(engine.getCombatUI(), "setAutopilot", 1)) {
            return false;
        }
        UiReflection.call(engine.getCombatUI(), "setAutopilot", on);
        return true;
    }

    static boolean leaveCombat(CombatEngineAPI engine) throws Throwable {
        Object returnState = UiReflection.call(engine, "getStateToReturnTo");
        if (returnState == null) {
            return false;
        }
        UiReflection.call(GameStates.current(), "goToState", returnState);
        return true;
    }
}
