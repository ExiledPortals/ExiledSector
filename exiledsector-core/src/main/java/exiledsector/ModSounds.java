package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import org.lwjgl.util.vector.Vector2f;

public final class ModSounds {

    public static final String ENABLED_FIELD_ID = "exiledSector_skillTreeSounds";
    public static final boolean DEFAULT_ENABLED = true;

    private static volatile boolean enabled = DEFAULT_ENABLED;

    private ModSounds() {
    }

    public static void reloadSettings() {
        enabled = ModSettings.booleanOr(ENABLED_FIELD_ID, DEFAULT_ENABLED);
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabledForTests(boolean testEnabled) {
        enabled = testEnabled;
    }

    public static void playUi(String soundId) {
        SoundPlayerAPI player = Global.getSoundPlayer();
        if (player != null && enabled) {
            player.playUISound(soundId, 1f, 1f);
        }
    }

    public static void playAt(String soundId, float volume, Vector2f location, Vector2f velocity) {
        SoundPlayerAPI player = Global.getSoundPlayer();
        if (player != null && enabled) {
            player.playSound(soundId, 1f, volume, location, velocity);
        }
    }

    public static String orSilent(String soundId) {
        return enabled ? soundId : null;
    }
}
