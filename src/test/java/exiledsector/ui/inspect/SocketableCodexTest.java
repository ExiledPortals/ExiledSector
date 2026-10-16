package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.socketables.PoolEntry;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableDefinitions;
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

class SocketableCodexTest {

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
        SocketableCodex.refreshUnlocks(null);
    }

    private static SocketableDefinition definition(String id, boolean unique, String unlock) {
        return new SocketableDefinition(id, SocketType.BRIDGE, id, SocketableDefinition.FALLBACK_ICON, 1f, unique, "",
                List.of(new PoolEntry("HULL_MULT", 1f, 2f, 1f)), List.of(), unlock);
    }

    @Test
    void lockedUniquesStayHiddenUntilTheirConditionIsMetInThisGame() {
        SocketableDefinition gated = definition("gated", true, "defeated_ziggurat");
        SocketableDefinition ungated = definition("ungated", true, "");
        SocketableDefinition common = definition("common", false, "");

        assertFalse(SocketableCodex.isRevealed(gated));
        assertTrue(SocketableCodex.isRevealed(ungated));
        assertTrue(SocketableCodex.isRevealed(common));

        SocketableCodex.refreshUnlocks(sector);
        assertFalse(SocketableCodex.isRevealed(gated));
    }

    @Test
    void anUnlockOnlyCountsForTheGameItWasCheckedIn() {
        SocketableDefinition gated = definition("gated", true, "defeated_ziggurat");
        SocketableDefinitions.clear();
        try {
            JSONArray rows = new JSONArray().put(new JSONObject().put("id", "gated").put("kind", "bridge")
                    .put("unique", "true").put("prefixes", "HULL_MULT:1:2").put("unlock", "defeated_ziggurat"));
            SocketableDefinitions.register(rows);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        when(sectorMemory.getBoolean("$defeatedZiggurat")).thenReturn(true);

        SocketableCodex.refreshUnlocks(sector);
        assertTrue(SocketableCodex.isRevealed(gated));

        globalMock.when(Global::getSector).thenReturn(mock(SectorAPI.class));
        assertFalse(SocketableCodex.isRevealed(gated));
        SocketableDefinitions.clear();
    }

    @Test
    void chancesShowOneDecimalOrTwoBelowOnePercent() {
        assertEquals("25", SocketableCodexEntry.percentText(0.25));
        assertEquals("7.5", SocketableCodexEntry.percentText(0.075));
        assertEquals("0.25", SocketableCodexEntry.percentText(0.0025));
        assertEquals("0.01", SocketableCodexEntry.percentText(0.00001));
        assertEquals("99.9", SocketableCodexEntry.percentText(0.99999));
        assertEquals("100", SocketableCodexEntry.percentText(1.0));
    }
}
