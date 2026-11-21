package exiledsector.harness;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PersistentSnapshotTest {

    static final class Stored {
        private final List<String> owned = new ArrayList<>();
        private List<TreeSet<String>> loadouts = new ArrayList<>();
        private int points;
        private transient Map<String, String> index = new HashMap<>();
    }

    @Test
    void restoringPutsBackTheSameObjectWithItsOldContents() throws Throwable {
        Stored stored = new Stored();
        stored.owned.add("chart");
        stored.loadouts.add(new TreeSet<>(List.of("root")));
        stored.points = 3;
        Map<String, Object> persistentData = new HashMap<>(Map.of("state", stored));
        PersistentSnapshot snapshot = PersistentSnapshot.of(persistentData, "state");

        stored.owned.add("second chart");
        stored.loadouts.get(0).add("breach_node");
        stored.points = 0;
        persistentData.put("state", new Stored());
        snapshot.restore(persistentData);

        assertSame(stored, persistentData.get("state"));
        assertEquals(List.of("chart"), stored.owned);
        assertEquals(List.of(new TreeSet<>(List.of("root"))), stored.loadouts);
        assertEquals(3, stored.points);
        assertNull(stored.index);
    }

    @Test
    void stateThatDidNotExistAtTheBaselineIsRemoved() throws Throwable {
        Map<String, Object> persistentData = new HashMap<>();
        PersistentSnapshot snapshot = PersistentSnapshot.of(persistentData, "state");

        persistentData.put("state", new Stored());
        snapshot.restore(persistentData);

        assertFalse(persistentData.containsKey("state"));
    }
}
