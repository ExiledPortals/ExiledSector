package exiledsector.ui.infestation;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;

final class InfestationSprites {

    static final String FRAGMENT_PATH = "graphics/ships/threat/fragment_single.png";
    static final String FRAGMENT_GLOW_PATH = "graphics/fx/hit_glow.png";
    static final String SHROUD_SHEET_PATH = "graphics/ships/shroud/shrouded_blobs.png";
    static final String SHROUD_SPOTS_PATH = "graphics/ships/shroud/spot_cluster.png";
    static final int SHROUD_SHEET_SIDE = 4;
    static final int SHROUD_SHEET_CELLS = SHROUD_SHEET_SIDE * SHROUD_SHEET_SIDE;

    private final SpriteCache spriteCache = new SpriteCache(InfestationSprites.class);

    SpriteAPI fragment() {
        return spriteCache.texture(FRAGMENT_PATH);
    }

    SpriteAPI fragmentGlow() {
        return spriteCache.texture(FRAGMENT_GLOW_PATH);
    }

    SpriteAPI shroudSheet() {
        return spriteCache.texture(SHROUD_SHEET_PATH);
    }

    SpriteAPI shroudSpots() {
        return spriteCache.texture(SHROUD_SPOTS_PATH);
    }
}
