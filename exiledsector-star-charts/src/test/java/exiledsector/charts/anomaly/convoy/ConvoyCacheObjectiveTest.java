package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledlib.MockSector;
import org.junit.jupiter.api.BeforeEach;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.raid.RaidDangerLevels;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ConvoyCacheObjectiveTest {

    @BeforeAll
    static void loadDangerLevels() {
        RaidDangerLevels.load();
    }

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        mockSector.global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        mockSector.close();
    }

    @Test
    void theCacheNeedsMoreMarinesAtHigherTiersAndAHarderOperation() {
        assertEquals(RaidDangerLevel.LOW, ConvoyRules.cacheDanger(1));
        assertEquals(RaidDangerLevel.MEDIUM, ConvoyRules.cacheDanger(8));
        assertEquals(RaidDangerLevel.HIGH, ConvoyRules.cacheDanger(15));
        assertTrue(ConvoyRules.raidDifficulty(15) > ConvoyRules.raidDifficulty(1));
        assertEquals(ConvoyRules.raidDifficulty(1), ConvoyRules.raidDifficulty(0));
    }

    @Test
    void anObjectiveTakesTheTokensOfItsDangerLevelAndShowsNoValueColumns() {
        ConvoySystem convoySystem = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));

        ConvoyCacheObjective objective = new ConvoyCacheObjective(null, convoySystem.script(), 0, RaidDangerLevel.MEDIUM);

        assertEquals(RaidDangerLevel.MEDIUM.marineTokens, objective.getMarinesRequired());
        assertEquals(RaidDangerLevel.MEDIUM, objective.getDangerLevel());
        assertEquals("", objective.getValueString(3));
        assertEquals(0, objective.getProjectedCreditsValue());
    }

    @Test
    void withNothingDeliveredARaidTakesNothingAndLeavesTheCacheAlone() {
        ConvoySystem convoySystem = new ConvoySystem(4);
        SupplyConvoy.INSTANCE.generate(convoySystem.site(ChartGenContext.none()));
        ConvoyState convoy = convoySystem.script().convoys().get(0);
        CargoAPI loot = mock(CargoAPI.class);

        int xp = new ConvoyCacheObjective(null, convoySystem.script(), 0, RaidDangerLevel.LOW).performRaid(loot, new Random(1L), 1f, null);

        assertEquals(0, xp);
        verify(loot, never()).addAll(any());
        assertFalse(convoy.isCacheTaken());
    }
}
