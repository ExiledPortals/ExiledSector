package exiledsector.compat;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.skills.unlock.RelockedGates;
import exiledsector.skills.unlock.UnlockConditionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LostSectorUnlocksTest {

    private final Map<String, Object> persistentData = new HashMap<>();
    private SectorAPI sector;
    private StarSystemAPI frost;

    @BeforeEach
    void setUp() {
        Map<String, Object> memoryValues = new HashMap<>();
        MemoryAPI sectorMemory = mock(MemoryAPI.class);
        when(sectorMemory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(memoryValues.get(invocation.getArgument(0))));
        doAnswer(invocation -> memoryValues.put(invocation.getArgument(0), invocation.getArgument(1))).when(sectorMemory).set(anyString(), any());
        frost = mock(StarSystemAPI.class);
        SectorEntityToken star = mock(SectorEntityToken.class);
        when(star.getContainingLocation()).thenReturn(frost);
        sector = mock(SectorAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        when(sector.getPersistentData()).thenReturn(persistentData);
        when(sector.getEntityById(LostSectorUnlocks.FROST_STAR_ID)).thenReturn(star);
    }

    @Test
    void theGateStaysShutUntilThePlayerEntersTheFrozenHeartSystem() {
        assertFalse(LostSectorUnlocks.isFrozenHeartSystemVisited(sector));
        when(frost.isEnteredByPlayer()).thenReturn(true);
        assertTrue(LostSectorUnlocks.isFrozenHeartSystemVisited(sector));
    }

    @Test
    void aVisitIsRememberedEvenIfTheSystemLaterReportsOtherwise() {
        when(frost.isEnteredByPlayer()).thenReturn(true);
        LostSectorUnlocks.isFrozenHeartSystemVisited(sector);
        when(frost.isEnteredByPlayer()).thenReturn(false);
        assertTrue(LostSectorUnlocks.isFrozenHeartSystemVisited(sector));
    }

    @Test
    void theSystemIsFoundByItsStoredNameWhenTheStarIsMissing() {
        when(sector.getEntityById(LostSectorUnlocks.FROST_STAR_ID)).thenReturn(null);
        persistentData.put(LostSectorUnlocks.FROST_SYSTEM_NAME_KEY, "Glacies");
        when(sector.getStarSystem("Glacies")).thenReturn(frost);
        assertSame(frost, LostSectorUnlocks.frostSystem(sector));
    }

    @Test
    void aStarOutsideAStarSystemFallsBackToTheStoredName() {
        SectorEntityToken strayStar = mock(SectorEntityToken.class);
        when(strayStar.getContainingLocation()).thenReturn(mock(LocationAPI.class));
        when(sector.getEntityById(LostSectorUnlocks.FROST_STAR_ID)).thenReturn(strayStar);
        assertNull(LostSectorUnlocks.frostSystem(sector));
    }

    @Test
    void theAreaStaysUsableWhenTheSystemCannotBeFound() {
        when(sector.getEntityById(LostSectorUnlocks.FROST_STAR_ID)).thenReturn(null);
        assertTrue(LostSectorUnlocks.isFrozenHeartSystemVisited(sector));
    }

    @Test
    void noSectorMeansNotVisited() {
        assertFalse(LostSectorUnlocks.isFrozenHeartSystemVisited(null));
    }

    @Test
    void enteringTheFrozenHeartSystemReopensARelockedGateAndRemembersTheVisit() {
        RelockedGates.relock(sector, List.of(UnlockConditionType.LOST_SECTOR_FROZEN_HEART_VISITED));

        LostSectorUnlocks.recordLocation(sector, mock(StarSystemAPI.class));
        assertTrue(RelockedGates.isRelocked(sector, UnlockConditionType.LOST_SECTOR_FROZEN_HEART_VISITED));

        LostSectorUnlocks.recordLocation(sector, frost);
        assertFalse(RelockedGates.isRelocked(sector, UnlockConditionType.LOST_SECTOR_FROZEN_HEART_VISITED));
        assertTrue(sector.getMemoryWithoutUpdate().getBoolean(LostSectorUnlocks.FROZEN_HEART_SYSTEM_VISITED_FLAG));
    }
}
