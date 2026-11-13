package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.IconCrossFade;

import java.util.List;

final class CyclingIcon extends BaseCustomUIPanelPlugin {

    private static final SpriteCache ICONS = new SpriteCache(CyclingIcon.class);

    private final List<String> iconPaths;
    private PositionAPI iconPosition;
    private float cycleSeconds;

    CyclingIcon(List<String> iconPaths) {
        this.iconPaths = List.copyOf(iconPaths);
    }

    String firstIconPath() {
        return iconPaths.isEmpty() ? null : iconPaths.get(0);
    }

    @Override
    public void positionChanged(PositionAPI iconPosition) {
        this.iconPosition = iconPosition;
    }

    @Override
    public void advance(float amount) {
        cycleSeconds = (cycleSeconds + amount) % IconCrossFade.periodSeconds(iconPaths.size());
    }

    @Override
    public void render(float alphaMult) {
        if (iconPosition == null) {
            return;
        }
        IconCrossFade.drawAtCenter(ICONS, iconPaths, cycleSeconds, iconPosition.getCenterX(), iconPosition.getCenterY(), iconPosition.getWidth(),
                iconPosition.getHeight(), alphaMult);
    }
}
