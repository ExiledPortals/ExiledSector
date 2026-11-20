package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.MapMemory;
import exiledsector.charts.atlas.TestBorders.Border;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemUnlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AtlasProgressTest {

    @Test
    void completingASystemReleasesItUnlessItsNewHolderTookItDuringTheRun() {
        AtlasState releasedState = new AtlasState(TestBorders.SEED);
        AtlasGenerator releasedGenerator = TestBorders.generator(releasedState);
        Border released = TestBorders.border(releasedGenerator, releasedState, "tritachyon", "hegemony");

        AtlasProgress.complete(releasedGenerator, releasedState, released.contested(), 1);

        assertTrue(releasedState.isCompleted(released.contested()));
        assertFalse(releasedState.isClaimed(released.contested()));

        AtlasState takenState = new AtlasState(TestBorders.SEED);
        AtlasGenerator takenGenerator = TestBorders.generator(takenState);
        Border taken = TestBorders.border(takenGenerator, takenState, "tritachyon", "hegemony");
        takenState.claim(taken.contested(), taken.attacker().commandCentre());

        AtlasProgress.complete(takenGenerator, takenState, taken.contested(), 1, true);

        assertTrue(takenState.isCompleted(taken.contested()));
        assertEquals(taken.attacker().commandCentre(), takenState.commandCentreOf(taken.contested()));
    }

    @Test
    void completingARunRecordsItsClearedTierOnceTheSystemIsCleared() {
        AtlasRuntime.invalidate();
        try (MockSector mockSector = new MockSector()) {
            SectorAPI sector = mockSector.sector;
            MemoryAPI sectorMemory = MapMemory.create();
            MemoryAPI playerMemory = MapMemory.create();
            when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
            when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
            AtlasState state = new AtlasState(TestBorders.SEED);
            mockSector.persistentData.put(AtlasState.PERSISTENT_KEY, state);
            AtlasCell cell = new AtlasCell(2, 0);
            ChartRun run = new ChartRun("progressRun", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
            run.recordContent(new ChartContent(5, "hegemony", "boss", 4, 6));
            run.recordAtlasNode(new AtlasRunNode(cell.pack(), "hegemony", List.of(), List.of()));

            AtlasProgress.completed(sector, run, null);

            assertTrue(state.isCompleted(cell));
            assertEquals(5, ItemUnlock.highestClearedChartTier(sector));
        } finally {
            AtlasRuntime.invalidate();
        }
    }

    @Test
    void aCompletedRunRevealsWithTheAtlasItWasOpenedWithNotTheOneSelectedNow() {
        AtlasRuntime.invalidate();
        try (MockSector mockSector = new MockSector()) {
            SectorAPI sector = mockSector.sector;
            MemoryAPI sectorMemory = MapMemory.create();
            MemoryAPI playerMemory = MapMemory.create();
            when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
            when(sector.getPlayerMemoryWithoutUpdate()).thenReturn(playerMemory);
            AtlasState state = new AtlasState(TestBorders.SEED);
            mockSector.persistentData.put(AtlasState.PERSISTENT_KEY, state);
            AtlasCell cell = new AtlasCell(2, 0);
            AtlasEffects openedWith = AtlasEffects.of(List.of(new AtlasEffects.Line(AtlasModifier.ATLAS_VISIBILITY_LANES, 1f)));
            ChartRun run = new ChartRun("lockedRun", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
            run.recordContent(new ChartContent(5, "hegemony", "boss", 4, 6));
            run.recordAtlasNode(new AtlasRunNode(cell.pack(), "hegemony", List.of(), List.of()));
            run.recordAtlasMapEffects(openedWith);

            AtlasProgress.completed(sector, run, null);

            assertEquals(openedWith, run.atlasMapEffects());
            assertEquals(openedWith, state.completedWith(cell));
            assertEquals(AtlasEffects.NONE, AtlasRuntime.generator(sector).effects());
        } finally {
            AtlasRuntime.invalidate();
        }
    }

    @Test
    void aSystemChangesHandsWhenTheRunLeavesItWithANewHolder() {
        AtlasCell defenderCentre = new AtlasCell(0, 0);
        AtlasCell attackerCentre = new AtlasCell(4, 0);

        assertTrue(AtlasProgress.changedHands(defenderCentre, attackerCentre));
        assertTrue(AtlasProgress.changedHands(null, attackerCentre));
        assertFalse(AtlasProgress.changedHands(defenderCentre, defenderCentre));
        assertFalse(AtlasProgress.changedHands(defenderCentre, null));
    }
}
