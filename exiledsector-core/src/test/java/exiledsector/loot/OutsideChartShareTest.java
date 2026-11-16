package exiledsector.loot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.plugins.LevelupPlugin;
import exiledsector.MockSector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutsideChartShareTest {

    @Test
    void theShareRisesFromHalfAtLevelOneToFullAtTheLevelCap() {
        assertEquals(0.5f, OutsideChartShare.at(1, 15), 1e-6f);
        assertEquals(0.75f, OutsideChartShare.at(8, 15), 1e-6f);
        assertEquals(1f, OutsideChartShare.at(15, 15), 1e-6f);
        assertEquals(1f, OutsideChartShare.at(30, 15), 1e-6f);
        assertEquals(0.5f, OutsideChartShare.at(0, 15), 1e-6f);
        assertEquals(1f, OutsideChartShare.at(1, 1), 1e-6f);
    }

    @Test
    void theShareReadsThePlayersLevelAgainstTheGamesLevelCap() {
        try (MockSector mockSector = new MockSector()) {
            MutableCharacterStatsAPI playerStats = mock(MutableCharacterStatsAPI.class);
            when(playerStats.getLevel()).thenReturn(11);
            when(mockSector.sector.getPlayerStats()).thenReturn(playerStats);
            SettingsAPI settings = mock(SettingsAPI.class);
            LevelupPlugin levelupPlugin = mock(LevelupPlugin.class);
            when(levelupPlugin.getMaxLevel()).thenReturn(21);
            when(settings.getLevelupPlugin()).thenReturn(levelupPlugin);
            mockSector.global.when(Global::getSettings).thenReturn(settings);

            assertEquals(0.75f, OutsideChartShare.current(), 1e-6f);
            assertEquals(0.75f, FleetLootRules.REGULAR.share(), 1e-6f);
            assertEquals(1f, FleetLootRules.full(LootMultipliers.NONE).share(), 1e-6f);
            assertEquals(0f, FleetLootRules.FEEDSTOCK_ONLY.share(), 1e-6f);
        }
    }

    @Test
    void withoutASectorTheShareIsTheLevelOneShare() {
        assertEquals(OutsideChartShare.SHARE_AT_LEVEL_ONE, OutsideChartShare.current(), 1e-6f);
    }
}
