package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartCombatPluginsTest {

    @Test
    void onlyARealCampaignBattleCounts() {
        CombatEngineAPI campaign = engine(true, false, false);
        assertTrue(ChartCombatPlugins.isCampaignBattle(campaign));

        assertFalse(ChartCombatPlugins.isCampaignBattle(null));
        assertFalse(ChartCombatPlugins.isCampaignBattle(engine(false, false, false)));
        assertFalse(ChartCombatPlugins.isCampaignBattle(engine(true, true, false)));
        assertFalse(ChartCombatPlugins.isCampaignBattle(engine(true, false, true)));
    }

    private static CombatEngineAPI engine(boolean inCampaign, boolean simulation, boolean campaignSim) {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.isInCampaign()).thenReturn(inCampaign);
        when(engine.isSimulation()).thenReturn(simulation);
        when(engine.isInCampaignSim()).thenReturn(campaignSim);
        return engine;
    }
}
