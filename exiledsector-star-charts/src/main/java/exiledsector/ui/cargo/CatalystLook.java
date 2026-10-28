package exiledsector.ui.cargo;

import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.charts.atlas.AtlasCatalysts;

public record CatalystLook(AtlasCatalysts.Family family) implements IconArt {

    @Override
    public void render(SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h, float alphaMult,
                       int phaseSeed) {
        CatalystIconFx.render(this, sprites, iconPath, look, x, y, w, h, alphaMult, phaseSeed);
    }
}
