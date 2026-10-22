package exiledsector.skills.npc;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NpcTreeRecordsTest {

    @Test
    void theRecordsMapIsCreatedInFleetMemoryOnceAndReused() {
        Map<String, Object> store = new HashMap<>();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.get(NpcTreeRecords.MEMORY_KEY)).thenAnswer(invocation -> store.get(NpcTreeRecords.MEMORY_KEY));
        doAnswer(invocation -> store.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());

        Map<String, String> first = NpcTreeRecords.of(memory);
        first.put("m1", NpcTreeRecords.NOT_LEVELLED);

        assertSame(first, NpcTreeRecords.of(memory));
        verify(memory).set(NpcTreeRecords.MEMORY_KEY, first);
    }

    @Test
    void onlyNpcTreeTagsCountAsLevelledRecords() {
        assertTrue(NpcTreeRecords.isLevelled("exiledSector_npcTree|bulwark|3|root,a"));
        assertFalse(NpcTreeRecords.isLevelled(NpcTreeRecords.NOT_LEVELLED));
        assertFalse(NpcTreeRecords.isLevelled(null));
    }
}
