package exiledsector.charts.content;

import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartAnomalyFleetsTest {

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadShipped();
    }

    @AfterEach
    void tearDown() {
        ChartTiers.resetForTests();
    }

    @Test
    void anomalyCombatFleetsAskForTheTiersOfficerLevelBonusAndFreightersForNone() {
        ChartTier tier = ChartTiers.forTier(15);

        FleetParamsV3 combat = ChartAnomalyFleets.params(ChartAnomalyFleets.combat(Factions.PIRATES, FleetTypes.PATROL_LARGE, tier, 200f), new Random(1));
        FleetParamsV3 boss = ChartAnomalyFleets.params(ChartAnomalyFleets.bossCombat(Factions.PIRATES, FleetTypes.TASK_FORCE, tier, 400f), new Random(1));
        FleetParamsV3 freighters = ChartAnomalyFleets.params(
                ChartAnomalyFleets.freighters(Factions.INDEPENDENT, FleetTypes.TRADE, tier, 50f, 20f, 30f), new Random(1));

        assertEquals(tier.officerLevelBonus(), combat.officerLevelBonus);
        assertEquals(tier.officerLevelBonus() + ChartContentPlanner.BOSS_OFFICER_LEVEL_BONUS, boss.officerLevelBonus);
        assertEquals(0, freighters.officerLevelBonus);
    }
}
