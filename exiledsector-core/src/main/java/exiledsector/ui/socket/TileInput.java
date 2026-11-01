package exiledsector.ui.socket;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class TileInput {

    interface Handler {

        void entered(PositionAPI tilePosition);

        void left();

        boolean clicked();

        default boolean rightClicked() {
            return false;
        }

        static Handler of(Consumer<PositionAPI> entered, Runnable left, BooleanSupplier clicked, BooleanSupplier rightClicked) {
            return new Handler() {
                @Override
                public void entered(PositionAPI tilePosition) {
                    entered.accept(tilePosition);
                }

                @Override
                public void left() {
                    left.run();
                }

                @Override
                public boolean clicked() {
                    return clicked.getAsBoolean();
                }

                @Override
                public boolean rightClicked() {
                    return rightClicked.getAsBoolean();
                }
            };
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
        return new TileInput(Handler.of(hooks.onHover(), hooks.onLeave(), () -> {
            if (!blocked) {
                hooks.onClick().run();
            }
            return true;
        }, () -> false), true);
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
