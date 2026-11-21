package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.RunFactionDress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartTransponderReputationTest {

    private static final Predicate<String> NOBODY_HOSTILE = factionId -> false;

    private static CampaignFleetAPI fleet(String factionId, boolean chartFleet) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MemoryAPI memory = mock(MemoryAPI.class);
        FactionAPI faction = mock(FactionAPI.class);
        when(faction.getId()).thenReturn(factionId);
        when(memory.contains(ChartIds.TIER_MEMORY_KEY)).thenReturn(chartFleet);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(memory);
        when(fleet.getFaction()).thenReturn(faction);
        return fleet;
    }

    private static CampaignFleetAPI skirmishFleet(String copyFactionId, String realFactionId) {
        CampaignFleetAPI fleet = fleet(copyFactionId, true);
        when(fleet.getMemoryWithoutUpdate().getString(ChartIds.REAL_FACTION_MEMORY_KEY)).thenReturn(realFactionId);
        return fleet;
    }

    @Test
    void fightingASkirmishCopyCostsReputationWithTheRealFactionBehindIt() {
        List<CampaignFleetAPI> enemies = List.of(skirmishFleet(RunFactionDress.ATTACKER_FACTION_ID, "tritachyon"),
                skirmishFleet(RunFactionDress.DEFENDER_FACTION_ID, "hegemony"));

        assertEquals(Set.of("tritachyon", "hegemony"), ChartTransponderReputation.factionsToPenalise(enemies, true, NOBODY_HOSTILE));
        assertEquals(Set.of("hegemony"), ChartTransponderReputation.factionsToPenalise(enemies, true, "tritachyon"::equals));
        assertTrue(ChartTransponderReputation.factionsToPenalise(List.of(skirmishFleet(RunFactionDress.DEFENDER_FACTION_ID, "")), true,
                NOBODY_HOSTILE).isEmpty());
    }

    @Test
    void aTransponderOnBattleCostsEachNonHostileChartFactionOnce() {
        List<CampaignFleetAPI> enemies = List.of(fleet("hegemony", true), fleet("hegemony", true), fleet("tritachyon", true));

        assertEquals(Set.of("hegemony", "tritachyon"), ChartTransponderReputation.factionsToPenalise(enemies, true, NOBODY_HOSTILE));
    }

    @Test
    void theTransponderOffKeepsReputationUntouched() {
        List<CampaignFleetAPI> enemies = List.of(fleet("hegemony", true));

        assertTrue(ChartTransponderReputation.factionsToPenalise(enemies, false, NOBODY_HOSTILE).isEmpty());
    }

    @Test
    void hostileFactionsNeverCostReputation() {
        List<CampaignFleetAPI> enemies = List.of(fleet("pirates", true), fleet("hegemony", true));

        assertEquals(Set.of("hegemony"), ChartTransponderReputation.factionsToPenalise(enemies, true, "pirates"::equals));
    }

    @Test
    void onlyChartFleetsOfRealFactionsCount() {
        List<CampaignFleetAPI> enemies = List.of(fleet("hegemony", false), fleet(RunFactionDress.ALLY_FACTION_ID, true),
                fleet(Factions.PLAYER, true), fleet(Factions.NEUTRAL, true));

        assertTrue(ChartTransponderReputation.factionsToPenalise(enemies, true, NOBODY_HOSTILE).isEmpty());
        assertTrue(ChartTransponderReputation.factionsToPenalise(null, true, NOBODY_HOSTILE).isEmpty());
    }
}
