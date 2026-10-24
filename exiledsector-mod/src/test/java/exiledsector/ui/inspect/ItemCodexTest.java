package exiledsector.ui.inspect;

import exiledlib.MockSector;
import exiledsector.modifiers.ModifierSlot;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.items.PoolEntry;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemCodexTest {

    private MockSector mockSector;
    private MemoryAPI sectorMemory;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        sectorMemory = mock(MemoryAPI.class);
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        ItemCodex.refreshUnlocks(null);
    }

    private static ItemDefinition definition(String id, boolean unique, String unlock) {
        return new ItemDefinition(id, ItemKind.BRIDGE, id, ItemDefinition.FALLBACK_ICON, 1f, unique, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_MULT", 1f, 2f, 1f)), List.of(), unlock);
    }

    @Test
    void lockedUniquesStayHiddenUntilTheirConditionIsMetInThisGame() {
        ItemDefinition gated = definition("gated", true, "defeated_ziggurat");
        ItemDefinition ungated = definition("ungated", true, "");
        ItemDefinition common = definition("common", false, "");

        assertFalse(ItemCodex.isRevealed(gated));
        assertTrue(ItemCodex.isRevealed(ungated));
        assertTrue(ItemCodex.isRevealed(common));

        ItemCodex.refreshUnlocks(mockSector.sector);
        assertFalse(ItemCodex.isRevealed(gated));
    }

    @Test
    void anUnlockOnlyCountsForTheGameItWasCheckedIn() {
        ItemDefinition gated = definition("gated", true, "defeated_ziggurat");
        ItemDefinitions.clear();
        try {
            JSONArray rows = new JSONArray().put(new JSONObject().put("id", "gated").put("section", "bridge")
                    .put("unique", "true").put("prefixes", "HULL_MULT:1:2").put("unlock", "defeated_ziggurat"));
            ItemDefinitions.register(rows);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        when(sectorMemory.getBoolean("$defeatedZiggurat")).thenReturn(true);

        ItemCodex.refreshUnlocks(mockSector.sector);
        assertTrue(ItemCodex.isRevealed(gated));

        mockSector.global.when(Global::getSector).thenReturn(mock(SectorAPI.class));
        assertFalse(ItemCodex.isRevealed(gated));
        ItemDefinitions.clear();
    }

    @Test
    void chancesShowOneDecimalOrTwoBelowOnePercent() {
        assertEquals("25", ItemCodexEntry.percentText(0.25));
        assertEquals("7.5", ItemCodexEntry.percentText(0.075));
        assertEquals("0.25", ItemCodexEntry.percentText(0.0025));
        assertEquals("0.01", ItemCodexEntry.percentText(0.00001));
        assertEquals("99.9", ItemCodexEntry.percentText(0.99999));
        assertEquals("100", ItemCodexEntry.percentText(1.0));
    }
}
