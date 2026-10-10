package exiledsector.ui.socket;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class TileInput {

    private TileInput() {
    }

    static void process(List<InputEventAPI> events, PositionAPI tilePosition, boolean blocked, IconTileCell.Hooks hooks,
                        Consumer<Boolean> setHovered, BooleanSupplier hovered) {
        if (tilePosition == null) {
            return;
        }
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent()) {
                setHovered.accept(false);
            }
            if (event.isConsumed()) {
                continue;
            }
            boolean inside = tilePosition.containsEvent(event);
            if (event.isMouseMoveEvent()) {
                if (inside != hovered.getAsBoolean()) {
                    setHovered.accept(inside);
                    if (inside) {
                        hooks.onHover().accept(tilePosition);
                    } else {
                        hooks.onLeave().run();
                    }
                }
            } else if (inside && event.isLMBDownEvent()) {
                if (!blocked) {
                    hooks.onClick().run();
                }
                event.consume();
            }
        }
    }
}
