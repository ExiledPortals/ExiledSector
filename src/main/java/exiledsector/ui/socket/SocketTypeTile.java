package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.socketables.SocketType;
import exiledsector.ui.socket.HullUpgradePanel.SocketState;
import exiledsector.ui.framework.FrameworkSocketFlair;
import exiledsector.ui.util.GLDraw;

import java.awt.Color;
import java.util.List;

final class SocketTypeTile extends BaseCustomUIPanelPlugin {

    private static final Color BACKGROUND = new Color(0, 0, 0, 200);
    private static final float BORDER_WIDTH = 1.5f;
    private static final float FLAIR_SHARE = 0.3f;
    private static final float HOVER_SECONDS = 0.15f;
    private static final float DIMMED_ALPHA = 0.45f;
    private static final float MAX_STEP_SECONDS = 0.1f;

    private final SocketType socketType;
    private final SocketState state;
    private final String itemIconPath;
    private final IconTileCell.Hooks hooks;
    private PositionAPI tilePosition;
    private boolean hovered;
    private float hoverLevel;
    private float elapsedSeconds;

    SocketTypeTile(SocketType socketType, SocketState state, String itemIconPath, IconTileCell.Hooks hooks) {
        this.socketType = socketType;
        this.state = state;
        this.itemIconPath = itemIconPath;
        this.hooks = hooks;
    }

    @Override
    public void positionChanged(PositionAPI tilePosition) {
        this.tilePosition = tilePosition;
    }

    @Override
    public void advance(float amount) {
        float step = Math.min(MAX_STEP_SECONDS, amount);
        elapsedSeconds += step;
        float hoverStep = step / HOVER_SECONDS;
        hoverLevel = Math.max(0f, Math.min(1f, hoverLevel + (hovered && isClickable() ? hoverStep : -hoverStep)));
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (tilePosition == null) {
            return;
        }
        Color accent = state == SocketState.RESTRICTED ? FrameworkSocketFlair.inactiveColor() : FrameworkSocketFlair.colorOf(socketType);
        float x = tilePosition.getX();
        float y = tilePosition.getY();
        GLDraw.fillQuad(x, y, tilePosition.getWidth(), tilePosition.getHeight(), BACKGROUND, alphaMult);
        GLDraw.strokeQuad(x, y, tilePosition.getWidth(), tilePosition.getHeight(), accent, BORDER_WIDTH,
                (state == SocketState.UNLOCKED ? 1f : DIMMED_ALPHA + 0.5f * hoverLevel) * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (tilePosition == null) {
            return;
        }
        boolean dimmed = state != SocketState.UNLOCKED;
        FrameworkSocketFlair.SocketLook look = new FrameworkSocketFlair.SocketLook(hoverLevel, state == SocketState.UNLOCKED,
                state != SocketState.RESTRICTED, state == SocketState.UNLOCKED ? itemIconPath : null);
        FrameworkSocketFlair.render(tilePosition.getCenterX(), tilePosition.getCenterY(), tilePosition.getWidth() * FLAIR_SHARE,
                FrameworkSocketFlair.colorOf(socketType), look, elapsedSeconds, (dimmed ? DIMMED_ALPHA : 1f) * alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        TileInput.process(events, tilePosition, !isClickable(), hooks, value -> hovered = value, () -> hovered);
    }

    private boolean isClickable() {
        return state == SocketState.UNLOCKED || state == SocketState.AVAILABLE;
    }
}
