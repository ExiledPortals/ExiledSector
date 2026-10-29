package exiledsector.charts.layout.abyss;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.compat.RandomAssortmentCompat;

import java.util.List;

public final class AbyssArt {

    static final String CORE = "graphics/fx/rat_center.png";
    static final String HALO = "graphics/fx/rat_halo.png";
    static final String BAND = "graphics/fx/abyss_portal_textures_small.png";
    static final String VIGNETTE = "graphics/fx/rat_darkness_vignette.png";
    static final String FOG = "graphics/terrain/rat_depths1.png";
    static final String FOG_MAP = "graphics/terrain/rat_depths1_map.png";
    static final String BACKGROUND = "graphics/backgrounds/abyss/Abyss2.jpg";
    static final List<String> TEXTURES = List.of(CORE, HALO, BAND, VIGNETTE, FOG, FOG_MAP, BACKGROUND);
    private static final SpriteCache SPRITES = new SpriteCache(AbyssArt.class);

    private AbyssArt() {
    }

    public static void preload() {
        if (RandomAssortmentCompat.isModEnabled()) {
            TEXTURES.forEach(SPRITES::ensureLoaded);
        }
    }

    static SpriteAPI sprite(String path) {
        return RandomAssortmentCompat.isModEnabled() ? SPRITES.sprite(path) : null;
    }

    static SpriteAPI texture(String path) {
        return RandomAssortmentCompat.isModEnabled() ? SPRITES.texture(path) : null;
    }
}
