package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.socketables.Socketable;
import exiledsector.ui.socket.SocketableHoverTooltip;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class SocketableIconStrip extends BaseCustomUIPanelPlugin {

    static final float CELL_SIZE = 64f;
    private static final float GAP = 8f;
    private static final float ICON_INSET = 6f;
    private static final float BORDER_WIDTH = 1.5f;
    private static final float IDLE_BORDER_ALPHA = 0.55f;
    private static final Color CELL_BACKGROUND = new Color(0, 0, 0, 200);
    private static final SpriteCache ICONS = new SpriteCache(SocketableIconStrip.class);

    record Entry(Socketable socketable, List<StyledText> footer) {
    }

    private final SocketableHoverTooltip tooltip;
    private final List<Runnable> queued = new ArrayList<>();

    private SocketableIconStrip(CustomPanelAPI tooltipHost) {
        this.tooltip = new SocketableHoverTooltip(tooltipHost);
    }

    static CustomPanelAPI create(CustomPanelAPI tooltipHost, float width, List<Entry> entries) {
        SocketableIconStrip strip = new SocketableIconStrip(tooltipHost);
        CustomPanelAPI panel = Global.getSettings().createCustom(width, CELL_SIZE, strip);
        for (int i = 0; i < entries.size(); i++) {
            CustomPanelAPI cell = Global.getSettings().createCustom(CELL_SIZE, CELL_SIZE, new Cell(strip, entries.get(i)));
            panel.addComponent(cell).inTL(i * (CELL_SIZE + GAP), 0f);
        }
        return panel;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isMouseScrollEvent()) {
                queued.add(tooltip::hide);
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (queued.isEmpty()) {
            return;
        }
        List<Runnable> actions = List.copyOf(queued);
        queued.clear();
        actions.forEach(I18n::forGameText);
    }

    private void hovered(Entry entry, PositionAPI cell) {
        queued.add(() -> tooltip.show(entry, entry.socketable(), entry::footer, cell));
    }

    private void left(Entry entry) {
        queued.add(() -> {
            if (tooltip.isShowing(entry)) {
                tooltip.hide();
            }
        });
    }

    private static final class Cell extends BaseCustomUIPanelPlugin {

        private final SocketableIconStrip strip;
        private final Entry entry;
        private PositionAPI position;
        private boolean hovered;

        Cell(SocketableIconStrip strip, Entry entry) {
            this.strip = strip;
            this.entry = entry;
        }

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void renderBelow(float alphaMult) {
            if (position == null) {
                return;
            }
            GLDraw.fillQuad(position.getX(), position.getY(), position.getWidth(), position.getHeight(), CELL_BACKGROUND, alphaMult);
            GLDraw.strokeQuad(position.getX(), position.getY(), position.getWidth(), position.getHeight(), entry.socketable().rarity().color(),
                    BORDER_WIDTH, (hovered ? 1f : IDLE_BORDER_ALPHA) * alphaMult);
        }

        @Override
        public void render(float alphaMult) {
            if (position == null) {
                return;
            }
            float size = CELL_SIZE - ICON_INSET * 2f;
            SpriteDraw.drawAtCenter(ICONS, entry.socketable().iconPath(), position.getCenterX(), position.getCenterY(), size, size,
                    Color.WHITE, alphaMult);
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
            if (position == null) {
                return;
            }
            for (InputEventAPI event : events) {
                if (event.isMouseScrollEvent()) {
                    hovered = false;
                } else if (!event.isConsumed() && event.isMouseMoveEvent()) {
                    boolean inside = position.containsEvent(event);
                    if (inside != hovered) {
                        hovered = inside;
                        if (inside) {
                            strip.hovered(entry, position);
                        } else {
                            strip.left(entry);
                        }
                    }
                }
            }
        }
    }
}
