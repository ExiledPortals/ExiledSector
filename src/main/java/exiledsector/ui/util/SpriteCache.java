package exiledsector.ui.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SpriteCache {

    private final Logger logger;
    private final Set<String> loadedSprites = new HashSet<>();
    private final Set<String> failedSprites = new HashSet<>();
    private final Map<String, SpriteAPI> drawnSprites = new HashMap<>();
    private final Map<String, SpriteAPI> textures = new HashMap<>();

    public SpriteCache(Class<?> owner) {
        this.logger = Logger.getLogger(owner);
    }

    public boolean ensureLoaded(String path) {
        if (loadedSprites.contains(path)) return true;
        if (failedSprites.contains(path)) return false;
        try {
            Global.getSettings().loadTexture(path);
            loadedSprites.add(path);
            return true;
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to load texture " + path, e);
            failedSprites.add(path);
            return false;
        }
    }

    public SpriteAPI sprite(String path) {
        return cached(drawnSprites, path);
    }

    public SpriteAPI texture(String path) {
        return cached(textures, path);
    }

    private SpriteAPI cached(Map<String, SpriteAPI> sprites, String path) {
        SpriteAPI sprite = sprites.get(path);
        if (sprite == null && ensureLoaded(path)) {
            sprite = Global.getSettings().getSprite(path);
            sprites.put(path, sprite);
        }
        return sprite;
    }
}
