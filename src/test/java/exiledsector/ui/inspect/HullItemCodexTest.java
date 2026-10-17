package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.hullitems.PoolEntry;
import exiledsector.hullitems.SocketType;
import exiledsector.hullitems.HullItemDefinition;
import exiledsector.hullitems.HullItemDefinitions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullItemCodexTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private MemoryAPI sectorMemory;

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        sectorMemory = mock(MemoryAPI.class);
        when(sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        HullItemCodex.refreshUnlocks(null);
    }

    private static HullItemDefinition definition(String id, boolean unique, String unlock) {
        return new HullItemDefinition(id, SocketType.BRIDGE, id, HullItemDefinition.FALLBACK_ICON, 1f, unique, "",
                List.of(new PoolEntry("HULL_MULT", 1f, 2f, 1f)), List.of(), unlock);
    }

    @Test
    void lockedUniquesStayHiddenUntilTheirConditionIsMetInThisGame() {
        HullItemDefinition gated = definition("gated", true, "defeated_ziggurat");
        HullItemDefinition ungated = definition("ungated", true, "");
        HullItemDefinition common = definition("common", false, "");

        assertFalse(HullItemCodex.isRevealed(gated));
        assertTrue(HullItemCodex.isRevealed(ungated));
        assertTrue(HullItemCodex.isRevealed(common));

        HullItemCodex.refreshUnlocks(sector);
        assertFalse(HullItemCodex.isRevealed(gated));
    }

    @Test
    void anUnlockOnlyCountsForTheGameItWasCheckedIn() {
        HullItemDefinition gated = definition("gated", true, "defeated_ziggurat");
        HullItemDefinitions.clear();
        try {
            JSONArray rows = new JSONArray().put(new JSONObject().put("id", "gated").put("kind", "bridge")
                    .put("unique", "true").put("prefixes", "HULL_MULT:1:2").put("unlock", "defeated_ziggurat"));
            HullItemDefinitions.register(rows);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        when(sectorMemory.getBoolean("$defeatedZiggurat")).thenReturn(true);

        HullItemCodex.refreshUnlocks(sector);
        assertTrue(HullItemCodex.isRevealed(gated));

        globalMock.when(Global::getSector).thenReturn(mock(SectorAPI.class));
        assertFalse(HullItemCodex.isRevealed(gated));
        HullItemDefinitions.clear();
    }

    @Test
    void chancesShowOneDecimalOrTwoBelowOnePercent() {
        assertEquals("25", HullItemCodexEntry.percentText(0.25));
        assertEquals("7.5", HullItemCodexEntry.percentText(0.075));
        assertEquals("0.25", HullItemCodexEntry.percentText(0.0025));
        assertEquals("0.01", HullItemCodexEntry.percentText(0.00001));
        assertEquals("99.9", HullItemCodexEntry.percentText(0.99999));
        assertEquals("100", HullItemCodexEntry.percentText(1.0));
    }
}
