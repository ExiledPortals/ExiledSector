package exiledsector.ui.socket;

import exiledsector.ModSettings;
import org.lwjgl.input.Keyboard;

public final class SocketStorageConfig {

    public static final String FAVOURITE_KEY_FIELD_ID = "exiledSector_socketStorageFavouriteKey";
    public static final int DEFAULT_FAVOURITE_KEY = Keyboard.KEY_F;

    private SocketStorageConfig() {
    }

    public static int favouriteKey() {
        return ModSettings.intOr(FAVOURITE_KEY_FIELD_ID, DEFAULT_FAVOURITE_KEY);
    }
}
