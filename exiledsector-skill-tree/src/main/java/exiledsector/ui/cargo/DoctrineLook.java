package exiledsector.ui.cargo;

import exiledlib.skills.skilleffect.render.SpriteCache;

public record DoctrineLook(DoctrineScreen screen, String factionId, String emblemPath) implements IconArt {

    public static DoctrineLook of(DoctrineScreen screen, String factionId) {
        return new DoctrineLook(screen, factionId, DoctrineIconFx.EMBLEM_PREFIX + factionId + ".png");
    }

    @Override
    public void render(SpriteCache sprites, String iconPath, CargoIconLook look, float x, float y, float w, float h, float alphaMult,
                       int phaseSeed) {
        DoctrineIconFx.render(this, sprites, iconPath, look, x, y, w, h, alphaMult, phaseSeed);
    }
}
