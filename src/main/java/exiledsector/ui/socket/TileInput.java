package exiledsector.ui.socket;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;

final class TileInput {

    interface Handler {

        void entered(PositionAPI tilePosition);

        void left();

        boolean clicked();

        default boolean rightClicked() {
            return false;
        }
    }

    private final Handler tileHandler;
    private final boolean scrollClearsHover;
    private boolean hovered;

    TileInput(Handler tileHandler, boolean scrollClearsHover) {
        this.tileHandler = tileHandler;
        this.scrollClearsHover = scrollClearsHover;
    }

    static TileInput forHooks(IconTileCell.Hooks hooks, boolean blocked) {
        return new TileInput(new Handler() {
            @Override
            public void entered(PositionAPI tilePosition) {
                hooks.onHover().accept(tilePosition);
            }

            @Override
            public void left() {
                hooks.onLeave().run();
            }

            @Override
            public boolean clicked() {
                if (!blocked) {
                    hooks.onClick().run();
                }
                return true;
            }
        }, true);
    }

    boolean isHovered() {
        return hovered;
    }

    void process(List<InputEventAPI> events, PositionAPI tilePosition) {
        if (tilePosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (scrollClearsHover && event.isMouseScrollEvent()) {
                hovered = false;
            }
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = tilePosition.containsEvent(event);
            if (event.isMouseMoveEvent()) {
                if (inside != hovered) {
                    hovered = inside;
                    if (inside) {
                        tileHandler.entered(tilePosition);
                    } else {
                        tileHandler.left();
                    }
                }
            } else if (inside && event.isLMBDownEvent() && tileHandler.clicked()) {
                event.consume();
            } else if (inside && event.isRMBDownEvent() && tileHandler.rightClicked()) {
                event.consume();
            }
        }
    }
}
