package exiledsector.items;

import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.CargoIconAnimation;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;

public abstract class AnimatedCargoItemPlugin extends BaseSpecialItemPlugin {

    private static final SpriteCache SPRITES = new SpriteCache(AnimatedCargoItemPlugin.class);

    private final CargoIconAnimation animation = new CargoIconAnimation();
    private CargoIconLook look;
    private String iconPath;

    protected abstract CargoIconLook cargoLook();

    protected abstract String cargoIconPath();

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        if (look == null) {
            look = cargoLook();
            iconPath = cargoIconPath();
        }
        CargoIconFx.render(SPRITES, iconPath, look, x, y, w, h, alphaMult, glowMult, animation.phaseSeed(), animation.burstAge(look, stack));
    }
}
