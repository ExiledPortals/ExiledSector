package exiledsector.items;

import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemVolumesTest {

    private final Set<String> installed = new HashSet<>();
    private final Set<String> switchedOn = new HashSet<>();

    @AfterEach
    void tearDown() {
        ItemVolumes.bind(null);
        ItemDefinitions.clear();
    }

    private void bindAreas() {
        ItemVolumes.bind(new ItemVolumes.Toggles() {
            @Override
            public Set<String> volumes() {
                return Set.of("tania", "nightcross");
            }

            @Override
            public boolean modInstalled(String volume) {
                return installed.contains(volume);
            }

            @Override
            public boolean switchedOn(String volume) {
                return switchedOn.contains(volume);
            }
        });
    }

    private static void registerItems() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(HullItemRows.row("plain", "bridge", "HULL_PERCENT:1:1"))
                .put(HullItemRows.row("tania_item", "bridge", "HULL_PERCENT:1:1").put(ItemVolumes.VOLUME_COLUMN, "tania"))
                .put(HullItemRows.row("nightcross_item", "bridge", "HULL_PERCENT:1:1").put(ItemVolumes.VOLUME_COLUMN, "nightcross")));
    }

    @Test
    void itemsOfAnVolumeWhoseModIsMissingAreNeverLoaded() throws Exception {
        installed.add("nightcross");
        bindAreas();

        registerItems();

        assertNotNull(ItemDefinitions.get("plain"));
        assertNull(ItemDefinitions.get("tania_item"));
        assertEquals("nightcross", ItemDefinitions.get("nightcross_item").volume());
    }

    @Test
    void itemsOfASwitchedOffVolumeStayLoadedButNeverDrop() throws Exception {
        installed.addAll(Set.of("tania", "nightcross"));
        switchedOn.add("tania");
        bindAreas();

        registerItems();

        assertTrue(ItemUnlock.canDrop(ItemDefinitions.get("plain"), null));
        assertTrue(ItemUnlock.canDrop(ItemDefinitions.get("tania_item"), null));
        assertFalse(ItemUnlock.canDrop(ItemDefinitions.get("nightcross_item"), null));
        switchedOn.add("nightcross");
        assertTrue(ItemUnlock.canDrop(ItemDefinitions.get("nightcross_item"), null));
    }
}
