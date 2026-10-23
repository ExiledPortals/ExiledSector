package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.StarChartSettings;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CampaignUIAPI campaignUi;

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        campaignUi = mock(CampaignUIAPI.class);
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.contains(anyString())).thenAnswer(call -> sectorValues.containsKey(call.<String>getArgument(0)));
        when(memory.getInt(anyString())).thenAnswer(call -> ((Number) sectorValues.get(call.<String>getArgument(0))).intValue());
        doAnswer(call -> sectorValues.put(call.getArgument(0), call.getArgument(1))).when(memory).set(anyString(), any());
        when(sector.getMemoryWithoutUpdate()).thenReturn(memory);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        when(sector.getCampaignUI()).thenReturn(campaignUi);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getColor(anyString())).thenReturn(Color.GREEN);
        globalMock.when(Global::getSettings).thenReturn(settings);
        HullUpgradePoints.install();
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.STAR_CHART_PROGRESS);
    }

    @AfterEach
    void tearDown() {
        HullUpgradesSetting.setPointsModeForTests(HullUpgradesSetting.PointsMode.REFIT_SCHEMATICS);
        StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        globalMock.close();
    }

    @Test
    void everyPlayerShipFollowsTheHighestClearedTierAndStoredPointsDoNotCount() {
        ShipSkillData freshShip = new ShipSkillData();
        ShipSkillData schematicShip = new ShipSkillData();
        schematicShip.setStoredHullUpgradePoints(5);

        assertTrue(HullUpgradePoints.chartsEnabled());
        assertEquals(1, HullUpgradePoints.of(freshShip));
        ItemUnlock.recordChartCleared(sector, 9);
        assertEquals(2, HullUpgradePoints.of(freshShip));
        assertEquals(2, HullUpgradePoints.of(schematicShip));
        ItemUnlock.recordChartCleared(sector, 10);
        assertEquals(3, HullUpgradePoints.of(freshShip));
        ItemUnlock.recordChartCleared(sector, 15);
        assertEquals(4, HullUpgradePoints.of(schematicShip));
        assertEquals(5, schematicShip.getStoredHullUpgradePoints());
    }

    @Test
    void aClearThatRaisesThePointsSaysSoAndOneThatDoesNotStaysQuiet() {
        ItemUnlock.recordChartCleared(sector, 4);
        verify(campaignUi, never()).addMessage(anyString(), any(Color.class));

        ItemUnlock.recordChartCleared(sector, 5);
        verify(campaignUi, times(1)).addMessage(contains("2"), any(Color.class));

        ItemUnlock.recordChartCleared(sector, 7);
        verify(campaignUi, times(1)).addMessage(anyString(), any(Color.class));
    }

    @Test
    void withStarChartsSwitchedOffEveryShipStaysAtOnePoint() {
        StarChartSettings.setEnabledForTests(false);
        sectorValues.put("$exiledSector_chartsClearedTier", 15);

        assertEquals(1, HullUpgradePoints.of(new ShipSkillData()));
    }
}
