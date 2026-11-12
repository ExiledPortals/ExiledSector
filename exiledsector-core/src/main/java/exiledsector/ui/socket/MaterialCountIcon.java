package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

final class MaterialCountIcon extends BaseCustomUIPanelPlugin {

    private static final SpriteCache ICONS = new SpriteCache(MaterialCountIcon.class);

    private final String iconPath;
    private final CargoIconLook iconLook;
    private PositionAPI iconPosition;

    MaterialCountIcon(String itemId, String iconPath) {
        this.iconPath = iconPath;
        CargoIconLook materialLook = CargoIconLooks.forMaterial(itemId);
        this.iconLook = materialLook == null ? null : materialLook.unframed();
    }

    @Override
    public void positionChanged(PositionAPI iconPosition) {
        this.iconPosition = iconPosition;
    }

    @Override
    public void render(float alphaMult) {
        if (iconPosition == null) {
            return;
        }
        TileChrome.drawIcon(ICONS, iconPath, iconLook, iconPosition, iconPosition.getWidth(), alphaMult, 0f, System.identityHashCode(this));
    }
}
