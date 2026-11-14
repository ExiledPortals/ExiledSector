package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.CombatEngineAPI;

public final class ChartCombatPlugins {

    private ChartCombatPlugins() {
    }

    public static boolean isCampaignBattle(CombatEngineAPI engine) {
        return engine != null && engine.isInCampaign() && !engine.isSimulation() && !engine.isInCampaignSim();
    }
}
