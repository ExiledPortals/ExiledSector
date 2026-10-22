package exiledlib;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class GameStaticsExtension implements BeforeAllCallback {

    private static final String[] GAME_CLASSES_WITH_STATIC_STATE = {"com.fs.starfarer.api.util.Misc", "lunalib.lunaSettings.LunaSettings"};
    private static boolean loaded;

    @Override
    public void beforeAll(ExtensionContext context) throws ClassNotFoundException {
        synchronized (GameStaticsExtension.class) {
            if (loaded) {
                return;
            }
            loaded = true;
        }
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(Mockito.mock(SettingsAPI.class));
            global.when(() -> Global.getLogger(ArgumentMatchers.any())).thenAnswer(call -> Logger.getLogger(call.<Class<?>>getArgument(0)));
            for (String className : GAME_CLASSES_WITH_STATIC_STATE) {
                Class.forName(className, true, GameStaticsExtension.class.getClassLoader());
            }
        }
    }
}
