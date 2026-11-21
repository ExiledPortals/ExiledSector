package exiledsector.charts.anomaly.holdouts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.content.FakeChartContentEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoldoutRestraintTest {

    private static final String HOLDOUT_FLEET_ID = "holdout1";
    private static final float HOLDOUT_STRENGTH = 100f;

    private final List<CampaignFleetAPI> fleetsInSystem = new ArrayList<>();
    private StarSystemAPI system;
    private CampaignFleetAPI holdout;
    private CampaignFleetAIAPI holdoutAi;
    private HoldoutRestraint restraint;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(fleetsInSystem);
        holdout = fleet("pirates", HOLDOUT_STRENGTH);
        holdoutAi = mock(CampaignFleetAIAPI.class);
        when(holdout.getAI()).thenReturn(holdoutAi);
        when(holdout.isAlive()).thenReturn(true);
        when(system.getEntityById(HOLDOUT_FLEET_ID)).thenReturn(holdout);
        restraint = new HoldoutRestraint("run1", 6, system, "pirates", List.of(HOLDOUT_FLEET_ID));
    }

    private CampaignFleetAPI fleet(String factionId, float strength) {
        MemoryAPI memory = FakeChartContentEngine.memory();
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        when(fleet.getFaction()).thenReturn(faction);
        when(fleet.getEffectiveStrength()).thenReturn(strength);
        fleetsInSystem.add(fleet);
        return fleet;
    }

    @Test
    void theHoldoutsOnlySeekFightsTheyOverwhelm() {
        CampaignFleetAPI outmatched = fleet("hegemony", HOLDOUT_STRENGTH / HoldoutRestraint.OVERWHELMING_ADVANTAGE);
        CampaignFleetAPI evenMatch = fleet("hegemony", HOLDOUT_STRENGTH * 0.9f);

        restraint.check(null, null, HoldoutRestraint.CHECK_DAYS);

        verify(holdoutAi, never()).doNotAttack(outmatched, HoldoutRestraint.RESTRAINT_DAYS);
        verify(holdoutAi).doNotAttack(evenMatch, HoldoutRestraint.RESTRAINT_DAYS);
    }

    @Test
    void theHoldoutsTurnOnEveryOtherFactionThatEntersTheSystem() {
        fleet("skirmish_attackers", HOLDOUT_STRENGTH);
        CampaignFleetAPI player = fleet("player", HOLDOUT_STRENGTH);
        when(player.isPlayerFleet()).thenReturn(true);

        restraint.check(null, null, HoldoutRestraint.CHECK_DAYS);

        assertTrue(holdout.getMemoryWithoutUpdate().getBoolean(ChartAnomalyFleets.HOSTILE_TO_FACTION_PREFIX + "skirmish_attackers"));
        assertFalse(holdout.getMemoryWithoutUpdate().getBoolean(ChartAnomalyFleets.HOSTILE_TO_FACTION_PREFIX + "player"));
        verify(holdoutAi).doNotAttack(player, HoldoutRestraint.RESTRAINT_DAYS);
    }

    @Test
    void theirOwnFactionAndDeadHoldoutsAreLeftAlone() {
        fleet("pirates", HOLDOUT_STRENGTH);

        restraint.check(null, null, HoldoutRestraint.CHECK_DAYS);
        when(holdout.isAlive()).thenReturn(false);
        fleet("hegemony", HOLDOUT_STRENGTH);
        restraint.check(null, null, HoldoutRestraint.CHECK_DAYS);

        verify(holdoutAi, never()).doNotAttack(any(), anyFloat());
        assertFalse(holdout.getMemoryWithoutUpdate().getBoolean(ChartAnomalyFleets.HOSTILE_TO_FACTION_PREFIX + "pirates"));
        assertFalse(holdout.getMemoryWithoutUpdate().getBoolean(ChartAnomalyFleets.HOSTILE_TO_FACTION_PREFIX + "hegemony"));
    }
}
