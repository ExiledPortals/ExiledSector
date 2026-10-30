package exiledsector.charts.wormhole;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import exiledsector.MockSector;
import exiledsector.charts.MapMemory;
import exiledsector.items.JanusDevice;
import exiledsector.items.WormholeTechItems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LimboBlueprintCacheTest {

    private MockSector mockSector;
    private final MemoryAPI sectorMemory = MapMemory.create();
    private final MemoryAPI playerMemory = MapMemory.create();
    private final CargoAPI blueprintCargo = mock(CargoAPI.class);

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(mockSector.sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
        mockSector.cargo();
        FactoryAPI factory = mock(FactoryAPI.class);
        when(factory.createCargo(true)).thenReturn(blueprintCargo);
        mockSector.global.when(Global::getFactory).thenReturn(factory);
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getColor(anyString())).thenReturn(Color.WHITE);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static SpecialItemData blueprint() {
        return argThat(data -> data != null && WormholeTechItems.BLUEPRINT.equals(data.getId()) && data.getData() == null);
    }

    private CargoAPI limboCache() {
        MemoryAPI cacheMemory = MapMemory.create();
        cacheMemory.set(LimboBlueprintCache.CACHE_MEMORY_KEY, true);
        CargoAPI anchors = mock(CargoAPI.class);
        cacheMemory.set(BaseSalvageSpecial.EXTRA_SALVAGE, new BaseSalvageSpecial.ExtraSalvage(anchors));
        SectorEntityToken cache = mock(SectorEntityToken.class);
        when(cache.getMemoryWithoutUpdate()).thenReturn(cacheMemory);
        StarSystemAPI limbo = mock(StarSystemAPI.class);
        when(limbo.getAllEntities()).thenReturn(List.of(mock(SectorEntityToken.class), cache));
        when(mockSector.sector.getStarSystem(LimboBlueprintCache.LIMBO_SYSTEM)).thenReturn(limbo);
        return anchors;
    }

    @Test
    void theBlueprintJoinsTheLimboCachesSalvageOnce() {
        CargoAPI anchors = limboCache();

        assertEquals(LimboBlueprintCache.Outcome.PLACED, LimboBlueprintCache.settle(mockSector.sector));
        assertEquals(LimboBlueprintCache.Outcome.ALREADY_SETTLED, LimboBlueprintCache.settle(mockSector.sector));

        verify(blueprintCargo, times(1)).addSpecial(blueprint(), eq(1f));
        verify(anchors, times(1)).addAll(blueprintCargo);
        verify(mockSector.cargo(), never()).addSpecial(any(), anyFloat());
    }

    @Test
    void aSaveWhoseCacheWasAlreadyOpenedGetsTheBlueprintInCargo() {
        playerMemory.set(LimboBlueprintCache.CALIBRATION_DATA_KEY, true);

        assertEquals(LimboBlueprintCache.Outcome.GRANTED, LimboBlueprintCache.settle(mockSector.sector));
        assertEquals(LimboBlueprintCache.Outcome.ALREADY_SETTLED, LimboBlueprintCache.settle(mockSector.sector));

        verify(mockSector.cargo(), times(1)).addSpecial(blueprint(), eq(1f));
    }

    @Test
    void aSectorWithoutTheCacheGrantsTheBlueprintWithTheJanusDevice() {
        assertEquals(LimboBlueprintCache.Outcome.WAITING, LimboBlueprintCache.settle(mockSector.sector));
        verify(mockSector.cargo(), never()).addSpecial(any(), anyFloat());

        sectorMemory.set(JanusDevice.FLAG, true);

        assertEquals(LimboBlueprintCache.Outcome.GRANTED, LimboBlueprintCache.settle(mockSector.sector));
        assertEquals(LimboBlueprintCache.Outcome.ALREADY_SETTLED, LimboBlueprintCache.settle(mockSector.sector));
        verify(mockSector.cargo(), times(1)).addSpecial(blueprint(), eq(1f));
    }

    @Test
    void anOpenedCacheThatStillStandsTakesTheBlueprintInsteadOfCargo() {
        limboCache();
        playerMemory.set(LimboBlueprintCache.CALIBRATION_DATA_KEY, true);

        assertEquals(LimboBlueprintCache.Outcome.PLACED, LimboBlueprintCache.settle(mockSector.sector));
        verify(mockSector.cargo(), never()).addSpecial(any(), anyFloat());
    }
}
