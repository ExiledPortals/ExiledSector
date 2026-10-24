package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledlib.MockSector;
import exiledsector.charts.StarChartSettings;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartProgressPointsTest {

    private final Map<String, Object> sectorValues = new HashMap<>();
    private MockSector mockSector;
    private CampaignUIAPI campaignUi;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        campaignUi = mock(CampaignUIAPI.class);
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.contains(anyString())).thenAnswer(call -> sectorValues.containsKey(call.<String>getArgument(0)));
        when(memory.getInt(anyString())).thenAnswer(call -> ((Number) sectorValues.get(call.<String>getArgument(0))).intValue());
        doAnswer(call -> sectorValues.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(memory);
        when(mockSector.sector.getCampaignUI()).thenReturn(campaignUi);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getColor(anyString())).thenReturn(Color.GREEN);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        HullUpgradePoints.install();
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS);
    }

    @AfterEach
    void tearDown() {
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS);
        StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        mockSector.close();
    }

    @Test
    void everyPlayerShipFollowsTheHighestClearedTierAndStoredPointsDoNotCount() {
        ShipSkillData freshShip = new ShipSkillData();
        ShipSkillData schematicShip = new ShipSkillData();
        schematicShip.setStoredHullUpgradePoints(5);

        assertTrue(HullUpgradePoints.chartsEnabled());
        assertEquals(1, HullUpgradePoints.of(freshShip));
        ItemUnlock.recordChartCleared(mockSector.sector, 9);
        assertEquals(2, HullUpgradePoints.of(freshShip));
        assertEquals(2, HullUpgradePoints.of(schematicShip));
        ItemUnlock.recordChartCleared(mockSector.sector, 10);
        assertEquals(3, HullUpgradePoints.of(freshShip));
        ItemUnlock.recordChartCleared(mockSector.sector, 15);
        assertEquals(4, HullUpgradePoints.of(schematicShip));
        assertEquals(5, schematicShip.getStoredHullUpgradePoints());
    }

    @Test
    void aClearThatRaisesThePointsSaysSoAndOneThatDoesNotStaysQuiet() {
        ItemUnlock.recordChartCleared(mockSector.sector, 4);
        verify(campaignUi, never()).addMessage(anyString(), any(Color.class));

        ItemUnlock.recordChartCleared(mockSector.sector, 5);
        verify(campaignUi, times(1)).addMessage(contains("2"), any(Color.class));

        ItemUnlock.recordChartCleared(mockSector.sector, 7);
        verify(campaignUi, times(1)).addMessage(anyString(), any(Color.class));
    }

    @Test
    void withStarChartsSwitchedOffEveryShipStaysAtOnePoint() {
        StarChartSettings.setEnabledForTests(false);
        sectorValues.put("$exiledSector_chartsClearedTier", 15);

        assertEquals(1, HullUpgradePoints.of(new ShipSkillData()));
    }
}
