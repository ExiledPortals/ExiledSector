package exiledsector.ui.cargo;

import exiledlib.skills.skilleffect.SpriteCache;

import java.util.List;

public record CacheLook(List<String> chipIconPaths) implements IconArt {

    public CacheLook {
        chipIconPaths = chipIconPaths == null ? List.of() : List.copyOf(chipIconPaths);
    }

    @Override
    public void render(SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h, float alphaMult,
                       int phaseSeed) {
        CacheIconFx.render(this, sprites, iconPath, look, x, y, w, h, alphaMult, phaseSeed);
    }
}
