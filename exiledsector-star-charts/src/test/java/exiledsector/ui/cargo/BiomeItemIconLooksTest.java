package exiledsector.ui.cargo;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ModifierSources;
import exiledsector.items.PoolEntry;
import exiledsector.items.RolledModifier;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeItemIconLooksTest {

    private static final String CHRONAL = "TIMEFLOW_PERCENT_ABYSSAL_RIFT";
    private static final String BRIDGE = "test_bridge";

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray().put(new JSONObject().put("id", CHRONAL).put("affix", "prefix").put("scopes", "hull_item")
                .put("lines", "TIMEFLOW_PERCENT:3:5").put("name", "Chronal")));
        ItemDefinitions.register(List.of(new ItemDefinition(BRIDGE, ItemKind.BRIDGE, BRIDGE, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f)), List.of(), "")));
        BiomeModifiers.register(new JSONArray().put(new JSONObject().put("modifier", CHRONAL).put("biome", "abyssal_rift").put("item", "bridge")));
    }

    @AfterEach
    void tearDown() {
        BiomeModifiers.unregisterAll();
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    @Test
    void anItemWithABiomeModifierGetsAChargedBorder() {
        Item item = Item.create("rift", BRIDGE, 1L, List.of(new RolledModifier(CHRONAL, ModifierSlot.PREFIX, List.of())));
        CargoIconLook look = CargoIconLook.outlined(Color.BLUE);

        CargoIconLook decorated = BiomeItemIconLooks.INSTANCE.decorated(item, look);

        assertTrue(decorated.arcs());
        assertEquals(Color.BLUE, decorated.borderColor());
        assertNull(look.borderColor());
    }

    @Test
    void anItemWithoutOneKeepsItsLook() {
        Item item = Item.create("plain", BRIDGE, 1L, List.of());
        CargoIconLook look = CargoIconLook.outlined(Color.BLUE);

        assertSame(look, BiomeItemIconLooks.INSTANCE.decorated(item, look));
    }
}
