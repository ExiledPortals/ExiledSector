package exiledsector.skills.npc;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.HashMap;
import java.util.Map;

public final class NpcTreeRecords {

    public static final String MEMORY_KEY = "$exiledSector_npcTrees";
    public static final String NOT_LEVELLED = "-";

    private NpcTreeRecords() {
    }

    // fleet memory is a raw Object store; this key is only ever written as a HashMap<String, String>
    @SuppressWarnings("unchecked")
    public static Map<String, String> of(MemoryAPI memory) {
        Object storedRecords = memory.get(MEMORY_KEY);
        if (storedRecords instanceof Map<?, ?>) {
            return (Map<String, String>) storedRecords;
        }
        Map<String, String> recordsByMemberId = new HashMap<>();
        memory.set(MEMORY_KEY, recordsByMemberId);
        return recordsByMemberId;
    }

    public static boolean isLevelled(String treeRecord) {
        return treeRecord != null && treeRecord.startsWith(NpcTreeTag.PREFIX);
    }
}
