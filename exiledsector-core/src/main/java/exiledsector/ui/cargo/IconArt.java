package exiledsector.ui.cargo;

import exiledlib.skills.skilleffect.render.SpriteCache;

public interface IconArt {

    void render(SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h, float alphaMult, int phaseSeed);
}
