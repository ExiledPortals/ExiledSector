package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

final class ThemeGeneratorLoading {

    private ThemeGeneratorLoading() {
    }

    static void ensureLoaded() throws ClassNotFoundException {
        try (MockedStatic<Global> global = mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
            Class.forName(BaseThemeGenerator.class.getName(), true, ThemeGeneratorLoading.class.getClassLoader());
        }
    }
}
