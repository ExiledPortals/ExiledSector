package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.skills.skilleffect.Fade;
import exiledsector.ui.cargo.AnimatedItemIcons;
import exiledlib.skills.skilleffect.SpriteCache;

import java.awt.Color;
import java.util.List;

final class CyclingIcon extends BaseCustomUIPanelPlugin {

    private static final float HOLD_SECONDS = 1.6f;
    private static final float FADE_SECONDS = 0.6f;
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
        cycleSeconds = (cycleSeconds + amount) % (iconPaths.size() * (HOLD_SECONDS + FADE_SECONDS));
    }

    @Override
    public void render(float alphaMult) {
        if (iconPosition == null || iconPaths.isEmpty()) {
            return;
        }
        float step = HOLD_SECONDS + FADE_SECONDS;
        int currentIndex = (int) (cycleSeconds / step) % iconPaths.size();
        float secondsIntoStep = cycleSeconds - currentIndex * step;
        float fade = secondsIntoStep <= HOLD_SECONDS ? 0f : (secondsIntoStep - HOLD_SECONDS) / FADE_SECONDS;
        float eased = Fade.smooth(fade);
        draw(iconPaths.get(currentIndex), (1f - eased) * alphaMult);
        if (eased > 0f) {
            draw(iconPaths.get((currentIndex + 1) % iconPaths.size()), eased * alphaMult);
        }
    }

    private void draw(String path, float alpha) {
        AnimatedItemIcons.drawAtCenter(ICONS, path, iconPosition.getCenterX(), iconPosition.getCenterY(), iconPosition.getWidth(),
                iconPosition.getHeight(), Color.WHITE, alpha);
    }
}
