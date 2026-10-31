package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.AnimatedItemIcons;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.util.GLDraw;

import java.awt.Color;

final class TileChrome {

    static final Color BACKGROUND = new Color(0, 0, 0, 200);

    record Frame(Color glowColor, float glowWidth, float glowAlpha, Color borderColor, float borderWidth, float borderAlpha) {

        Frame(Color accent, float glowWidth, float glowAlpha, float borderWidth, float borderAlpha) {
            this(accent, glowWidth, glowAlpha, accent, borderWidth, borderAlpha);
        }
    }

    private TileChrome() {
    }

    static void draw(PositionAPI tilePosition, Frame frame, float alphaMult) {
        float x = tilePosition.getX();
        float y = tilePosition.getY();
        float width = tilePosition.getWidth();
        float height = tilePosition.getHeight();
        GLDraw.fillQuad(x, y, width, height, BACKGROUND, alphaMult);
        if (frame.glowAlpha() > 0f) {
            GLDraw.innerGlow(x, y, width, height, frame.glowWidth(), frame.glowColor(), frame.glowAlpha() * alphaMult);
        }
        GLDraw.strokeQuad(x, y, width, height, frame.borderColor(), frame.borderWidth(), frame.borderAlpha() * alphaMult);
    }

    static void drawIcon(SpriteCache icons, String iconPath, CargoIconLook look, PositionAPI tilePosition, float iconSize, float alphaMult,
                         float glowMult, int phaseSeed) {
        float centreX = tilePosition.getCenterX();
        float centreY = tilePosition.getCenterY();
        if (look == null) {
            AnimatedItemIcons.drawAtCenter(icons, iconPath, centreX, centreY, iconSize, iconSize, Color.WHITE, alphaMult);
            return;
        }
        CargoIconFx.render(icons, iconPath, look, centreX - iconSize / 2f, centreY - iconSize / 2f, iconSize, iconSize, alphaMult, glowMult,
                phaseSeed, -1f);
    }
}
