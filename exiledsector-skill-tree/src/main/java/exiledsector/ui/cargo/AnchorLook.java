package exiledsector.ui.cargo;

import exiledsector.compat.TaniaCompat;
import exiledlib.ui.util.SpriteCache;

public record AnchorLook(String crestFactionId) implements IconArt {

    public enum Style { LOCK_ON, PLATE_SHUTTERS }

    public Style style() {
        return TaniaCompat.FACTION_ID.equals(crestFactionId) ? Style.PLATE_SHUTTERS : Style.LOCK_ON;
    }

    @Override
    public void render(SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h, float alphaMult,
                       int phaseSeed) {
        AnchorIconFx.render(this, sprites, iconPath, look, x, y, w, h, alphaMult, phaseSeed);
    }
}
