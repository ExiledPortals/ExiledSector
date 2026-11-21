package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.MapMemory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScarMemoryTest {

    @Test
    void scarsRoundTripInOrder() {
        Map<String, Integer> scars = new LinkedHashMap<>();
        scars.put("overloaded", 2);
        scars.put("battle_hardened", 5);
        scars.put("kited", 1);
        String encoded = ScarMemory.encode(scars);
        assertEquals("overloaded:2;battle_hardened:5;kited:1", encoded);
        assertEquals(List.copyOf(scars.entrySet()), List.copyOf(ScarMemory.decode(encoded).entrySet()));
    }

    @Test
    void emptyAndBrokenEntriesAreDropped() {
        Map<String, Integer> scars = new LinkedHashMap<>();
        scars.put("overloaded", 0);
        scars.put("", 2);
        scars.put("kited", 1);
        assertEquals("kited:1", ScarMemory.encode(scars));
        assertEquals(Map.of("kited", 1), ScarMemory.decode("kited:1;rammed;rammed:many;:3"));
        assertEquals(Map.of(), ScarMemory.decode(null));
        assertEquals("", ScarMemory.encode(null));
    }

    @Test
    void repeatedEntriesAddUp() {
        assertEquals(Map.of("kited", 3), ScarMemory.decode("kited:1;kited:2"));
    }

    @Test
    void writingStoresTheFlagshipAndScars() {
        MemoryAPI memory = MapMemory.create();
        ScarMemory.write(memory, "member-1", Map.of("rammed", 2));
        assertEquals("member-1", ScarMemory.flagshipId(memory));
        assertEquals(Map.of("rammed", 2), ScarMemory.scars(memory));
    }

    @Test
    void writingNoScarsClearsThemButKeepsTheFlagship() {
        MemoryAPI memory = MapMemory.create();
        ScarMemory.write(memory, "member-1", Map.of("rammed", 2));
        ScarMemory.write(memory, "member-1", Map.of());
        assertEquals("member-1", ScarMemory.flagshipId(memory));
        assertFalse(memory.contains(ScarMemory.SCARS_KEY));
        assertEquals(Map.of(), ScarMemory.scars(memory));
    }

    @Test
    void noMemoryMeansNoScars() {
        assertNull(ScarMemory.flagshipId(null));
        assertEquals(Map.of(), ScarMemory.scars(null));
    }
}
