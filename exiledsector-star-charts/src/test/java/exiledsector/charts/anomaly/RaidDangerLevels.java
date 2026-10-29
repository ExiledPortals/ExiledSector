package exiledsector.charts.anomaly;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public final class RaidDangerLevels {

    private static boolean loaded;

    private RaidDangerLevels() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(Mockito.mock(SettingsAPI.class));
            RaidDangerLevel.values();
        }
        loaded = true;
    }
}
