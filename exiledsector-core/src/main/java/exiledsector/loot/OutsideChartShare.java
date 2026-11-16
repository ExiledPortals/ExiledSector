package exiledsector.loot;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.plugins.LevelupPlugin;

public final class OutsideChartShare {

    static final float SHARE_AT_LEVEL_ONE = 0.5f;
    static final int FALLBACK_LEVEL_CAP = 15;

    private OutsideChartShare() {
    }

    public static float current() {
        return at(playerLevel(), levelCap());
    }

    public static float at(int playerLevel, int levelCap) {
        if (levelCap <= 1) {
            return 1f;
        }
        float levelProgress = Math.max(0f, Math.min(1f, (playerLevel - 1f) / (levelCap - 1f)));
        return SHARE_AT_LEVEL_ONE + (1f - SHARE_AT_LEVEL_ONE) * levelProgress;
    }

    static int playerLevel() {
        SectorAPI sector = Global.getSector();
        MutableCharacterStatsAPI playerStats = sector == null ? null : sector.getPlayerStats();
        return playerStats == null ? 1 : playerStats.getLevel();
    }

    static int levelCap() {
        SettingsAPI settings = Global.getSettings();
        LevelupPlugin levelupPlugin = settings == null ? null : settings.getLevelupPlugin();
        return levelupPlugin == null ? FALLBACK_LEVEL_CAP : levelupPlugin.getMaxLevel();
    }
}
