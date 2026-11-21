package exiledsector.loot;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FleetLossTallyTest {

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockSector mockSector;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        lunaSettingsMock.close();
    }

    private static FleetMemberAPI ship(float deploymentPoints) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getDeploymentPointsCost()).thenReturn(deploymentPoints);
        return member;
    }

    private static BattleEngagement engagement(CampaignFleetAPI fleet, float difficultyMultiplier, FleetMemberAPI... lostShips) {
        return new BattleEngagement(member -> fleet, difficultyMultiplier, 0f, List.of(lostShips));
    }

    @Test
    void eachFleetSumsItsEngagementsAtCappedDifficultyAndTakesItsRulesWhenRead() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetLossTally tally = new FleetLossTally();
        tally.record(engagement(fleet, 2f, ship(100f)), anyFleet -> FleetLootRules.SPENT);
        tally.record(engagement(fleet, 5f, ship(100f)), anyFleet -> FleetLootRules.SPENT);

        List<FleetLoss> fleetLosses = tally.fleetLosses(anyFleet -> FleetLootRules.FEEDSTOCK_ONLY);

        assertEquals(1, fleetLosses.size());
        assertEquals(200f, fleetLosses.get(0).deploymentPoints(), 1e-4f);
        assertEquals(2.5f, fleetLosses.get(0).averageDifficulty(), 1e-4f);
        assertSame(FleetLootRules.FEEDSTOCK_ONLY, fleetLosses.get(0).rules());
    }

    @Test
    void eachEngagementKeepsItsCappedDifficultyAndTheRulesItWasRecordedWith() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetLossTally tally = new FleetLossTally();
        tally.record(engagement(fleet, 2f, ship(100f)), anyFleet -> FleetLootRules.SPENT);
        tally.record(engagement(fleet, 5f, ship(60f), ship(40f)), anyFleet -> FleetLootRules.LEVEL_SCALED);

        List<FleetLoss> engagementLosses = tally.engagementLossesAtRecordTime();

        assertEquals(2, engagementLosses.size());
        assertEquals(2f, engagementLosses.get(0).averageDifficulty(), 1e-6f);
        assertSame(FleetLootRules.SPENT, engagementLosses.get(0).rules());
        assertEquals(100f, engagementLosses.get(1).deploymentPoints(), 1e-4f);
        assertEquals(BattleLootRates.DIFFICULTY_CAP, engagementLosses.get(1).averageDifficulty(), 1e-6f);
        assertSame(FleetLootRules.LEVEL_SCALED, engagementLosses.get(1).rules());
    }

    @Test
    void shipsWithoutDeploymentPointsAddNoLoss() {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        FleetLossTally tally = new FleetLossTally();
        tally.record(engagement(fleet, 1f, ship(0f)), anyFleet -> FleetLootRules.LEVEL_SCALED);

        assertTrue(tally.fleetLosses(anyFleet -> FleetLootRules.LEVEL_SCALED).isEmpty());
        assertTrue(tally.engagementLossesAtRecordTime().isEmpty());
    }
}
