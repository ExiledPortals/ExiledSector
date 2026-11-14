package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModJsonTest {

    private static final String PATH = "data/config/exiledSector/example.json";

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void readsThePlainFileOrTheMergedFileOfThisMod() throws Exception {
        JSONObject plain = new JSONObject().put("source", "plain");
        JSONObject merged = new JSONObject().put("source", "merged");
        when(settings.loadJSON(PATH)).thenReturn(plain);
        when(settings.getMergedJSONForMod(PATH, "exiledSector")).thenReturn(merged);
        List<JSONObject> handled = new ArrayList<>();

        ModJson.load(PATH, true, mock(Logger.class), handled::add);

        assertSame(plain, ModJson.json(PATH, false));
        assertSame(plain, ModJson.jsonOrNull(PATH, false, mock(Logger.class)));
        assertEquals(List.of(merged), handled);
    }

    @Test
    void aMissingFileOrARejectingHandlerIsLoggedWithItsPathInsteadOfThrown() throws Exception {
        RuntimeException missing = new RuntimeException("not found");
        when(settings.loadJSON(PATH)).thenThrow(missing);
        when(settings.getMergedJSONForMod(PATH, "exiledSector")).thenReturn(new JSONObject());
        Logger log = mock(Logger.class);

        assertNull(ModJson.jsonOrNull(PATH, false, log));
        ModJson.load(PATH, true, log, json -> {
            throw new JSONException("bad entry");
        });

        verify(log).error("Failed to load " + PATH, missing);
        verify(log).error(eq("Failed to load " + PATH), any(JSONException.class));
    }
}
