package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JanusDeviceTest {

    private static SectorAPI sectorWithFlag(boolean flagSet) {
        SectorAPI sector = mock(SectorAPI.class);
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sectorMemory.getBoolean(JanusDevice.FLAG)).thenReturn(flagSet);
        return sector;
    }

    @Test
    void theDeviceIsObtainedOnlyOnceThePlayerCanUseGates() {
        assertTrue(JanusDevice.obtained(sectorWithFlag(true)));
        assertFalse(JanusDevice.obtained(sectorWithFlag(false)));
    }

    @Test
    void unreadableMemoryCountsAsNotObtainedWithoutThrowing() {
        SectorAPI brokenSector = mock(SectorAPI.class);
        when(brokenSector.getMemoryWithoutUpdate()).thenThrow(new IllegalStateException("no memory"));

        assertDoesNotThrow(() -> JanusDevice.obtained(brokenSector));
        assertFalse(JanusDevice.obtained(brokenSector));
        assertFalse(JanusDevice.obtained(null));
    }
}
