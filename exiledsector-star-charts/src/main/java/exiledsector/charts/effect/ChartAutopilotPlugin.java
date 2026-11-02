package exiledsector.charts.effect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.compat.ApiMember;
import exiledsector.compat.OptionalApi;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.List;

public class ChartAutopilotPlugin extends BaseEveryFrameCombatPlugin {

    static final String TURN_OFF_SETTING = "shipControlsTurnOffAutopilot";
    static final String TOGGLE_CONTROL = "C2_TOGGLE_AUTOPILOT";
    static final int NO_KEY = -1;
    static final ApiMember SET_AUTOPILOT = ApiMember.instanceMethod("com.fs.starfarer.combat.CombatState", "setAutopilot", "void",
            ApiMember.BOOLEAN);
    private static final OptionalApi COMBAT_UI_API = new OptionalApi(List.of(SET_AUTOPILOT), Logger.getLogger(ChartAutopilotPlugin.class),
            Level.ERROR, "[ExiledSector] Turning on the player ship's autopilot failed; the Star Chart's forced autopilot is off for this battle",
            OptionalApi.RuntimeFailure.DISABLE);
    private static Boolean savedTurnOffSetting;

    private CombatEngineAPI engine;
    private boolean active;
    private boolean announced;
    private int toggleKey = NO_KEY;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        announced = false;
        active = engine != null && engine.isInCampaign() && !engine.isSimulation() && !engine.isInCampaignSim()
                && ChartPlayerModifiers.autopilotForced();
        holdTurnOffSetting(Global.getSettings(), active);
        toggleKey = active ? toggleKeyCode(Global.getSettings()) : NO_KEY;
    }

    static void holdTurnOffSetting(SettingsAPI settings, boolean forced) {
        if (forced) {
            if (savedTurnOffSetting == null) {
                savedTurnOffSetting = settings.getBoolean(TURN_OFF_SETTING);
            }
            settings.setBoolean(TURN_OFF_SETTING, false);
        } else if (savedTurnOffSetting != null) {
            settings.setBoolean(TURN_OFF_SETTING, savedTurnOffSetting);
            savedTurnOffSetting = null;
        }
    }

    static int toggleKeyCode(SettingsAPI settings) {
        try {
            String keyName = settings.getControlStringForEnumName(TOGGLE_CONTROL);
            return keyName == null || keyName.isEmpty() ? NO_KEY : settings.getCodeFor(keyName);
        } catch (RuntimeException e) {
            return NO_KEY;
        }
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        if (!active || toggleKey == NO_KEY || events == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (!event.isConsumed() && (event.isKeyDownEvent() || event.isKeyUpEvent()) && event.getEventValue() == toggleKey) {
                event.consume();
            }
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!active || !needsAutopilot(engine.getPlayerShip())) {
            return;
        }
        active = COMBAT_UI_API.call(api -> {
            api.handle(SET_AUTOPILOT).invoke(engine.getCombatUI(), true);
            return true;
        }, false);
        if (active && !announced) {
            announced = true;
            engine.getCombatUI().addMessage(0, Misc.getNegativeHighlightColor(),
                    I18n.forGameText(() -> Translation.msg("chart.combat.autopilotLocked").text()));
        }
    }

    static boolean needsAutopilot(ShipAPI playerShip) {
        return playerShip != null && !playerShip.isShuttlePod() && !playerShip.isHulk() && playerShip.getShipAI() == null;
    }
}
