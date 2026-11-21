package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FleetHullVisualsTest {

    @Test
    void skinsShareOneReadOfTheirBaseHullFile() throws Exception {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.loadJSON("data/hulls/visuals_test_base.ship")).thenReturn(new JSONObject("{\"center\":[12,34]}"));
        when(settings.loadJSON("data/hulls/visuals_test_skin_a.ship")).thenThrow(new IOException("no file"));
        when(settings.loadJSON("data/hulls/visuals_test_skin_b.ship")).thenThrow(new IOException("no file"));
        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);

            FleetHullVisuals base = FleetHullVisuals.of(hull("visuals_test_base", "visuals_test_base"));
            FleetHullVisuals skinA = FleetHullVisuals.of(hull("visuals_test_skin_a", "visuals_test_base"));
            FleetHullVisuals skinB = FleetHullVisuals.of(hull("visuals_test_skin_b", "visuals_test_base"));

            assertEquals(12f, base.centerX());
            assertEquals(34f, skinA.centerY());
            assertEquals(12f, skinB.centerX());
        }
        verify(settings, times(1)).loadJSON("data/hulls/visuals_test_base.ship");
        verify(settings, times(1)).loadJSON("data/hulls/visuals_test_skin_a.ship");
        verify(settings, times(1)).loadJSON("data/hulls/visuals_test_skin_b.ship");
    }

    private static ShipHullSpecAPI hull(String hullId, String baseHullId) {
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullId()).thenReturn(hullId);
        when(hull.getBaseHullId()).thenReturn(baseHullId);
        return hull;
    }
}
