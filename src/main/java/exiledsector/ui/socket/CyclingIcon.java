package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;

final class CyclingIcon extends BaseCustomUIPanelPlugin {

    private static final float HOLD_SECONDS = 1.6f;
    private static final float FADE_SECONDS = 0.6f;
    private static final SpriteCache ICONS = new SpriteCache(CyclingIcon.class);

    private final List<String> paths;
    private PositionAPI position;
    private float time;

    CyclingIcon(List<String> paths) {
        this.paths = List.copyOf(paths);
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void advance(float amount) {
        time = (time + amount) % (paths.size() * (HOLD_SECONDS + FADE_SECONDS));
    }

    @Override
    public void render(float alphaMult) {
        if (position == null || paths.isEmpty()) {
            return;
        }
        float step = HOLD_SECONDS + FADE_SECONDS;
        int current = (int) (time / step) % paths.size();
        float into = time - current * step;
        float fade = into <= HOLD_SECONDS ? 0f : (into - HOLD_SECONDS) / FADE_SECONDS;
        float eased = fade * fade * (3f - 2f * fade);
        draw(paths.get(current), (1f - eased) * alphaMult);
        if (eased > 0f) {
            draw(paths.get((current + 1) % paths.size()), eased * alphaMult);
        }
    }

    private void draw(String path, float alpha) {
        SpriteDraw.drawAtCenter(ICONS, path, position.getCenterX(), position.getCenterY(), position.getWidth(), position.getHeight(),
                Color.WHITE, alpha);
    }
}
