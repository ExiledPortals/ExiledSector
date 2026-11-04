package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class AtlasCameraMemoryTest {

    @Test
    void recallsTheCentreLastRememberedForTheSameSector() {
        SectorAPI sector = mock(SectorAPI.class);
        AtlasCameraMemory.remember(sector, 120f, -45f);
        AtlasCameraMemory.Centre centre = AtlasCameraMemory.recall(sector);
        assertEquals(120f, centre.treeX());
        assertEquals(-45f, centre.treeY());
    }

    @Test
    void forgetsTheCentreOnceAnotherSaveIsLoaded() {
        AtlasCameraMemory.remember(mock(SectorAPI.class), 120f, -45f);
        assertNull(AtlasCameraMemory.recall(mock(SectorAPI.class)));
        assertNull(AtlasCameraMemory.recall(null));
    }
}
