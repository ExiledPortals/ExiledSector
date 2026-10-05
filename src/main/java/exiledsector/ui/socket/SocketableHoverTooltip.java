package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.StyledText;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableTooltip;
import exiledsector.ui.TooltipExpansion;
import exiledsector.ui.util.BorderedPanel;

import java.util.List;
import java.util.function.Supplier;

public final class SocketableHoverTooltip {

    private static final float PAD = 10f;
    private static final float GAP = 6f;

    private final CustomPanelAPI host;
    private CustomPanelAPI shown;
    private Object shownFor;
    private Socketable shownSocketable;
    private Supplier<List<StyledText>> shownFooter;
    private PositionAPI shownAnchor;
    private boolean shownExpanded;

    public SocketableHoverTooltip(CustomPanelAPI host) {
        this.host = host;
    }

    public boolean isShowing(Object key) {
        return shown != null && shownFor == key;
    }

    public void show(Object key, Socketable socketable, Supplier<List<StyledText>> footer, PositionAPI anchor) {
        hide();
        boolean expanded = TooltipExpansion.isExpandedOrHeld();
        float width = SocketableTooltip.WIDTH;
        CustomPanelAPI panel = Global.getSettings().createCustom(width, anchor.getHeight(), new Frame());
        TooltipMakerAPI element = panel.createUIElement(width - PAD * 2f, 0f, false);
        SocketableTooltip.write(element, socketable, footer, expanded);
        float contentHeight = element.getHeightSoFar();
        element.getPosition().setSize(width - PAD * 2f, contentHeight);
        float height = contentHeight + PAD * 2f;
        panel.getPosition().setSize(width, height);
        panel.addUIElement(element).inTL(PAD, PAD);
        PositionAPI hostPosition = host.getPosition();
        float hostWidth = hostPosition.getWidth();
        float hostHeight = hostPosition.getHeight();
        float anchorLeft = anchor.getX() - hostPosition.getX();
        float anchorTop = hostPosition.getY() + hostHeight - (anchor.getY() + anchor.getHeight());
        float left;
        float top;
        if (anchorLeft + anchor.getWidth() + GAP + width <= hostWidth) {
            left = anchorLeft + anchor.getWidth() + GAP;
            top = anchorTop;
        } else if (anchorLeft - GAP - width >= 0f) {
            left = anchorLeft - GAP - width;
            top = anchorTop;
        } else {
            left = Math.max(0f, Math.min(anchorLeft, hostWidth - width));
            float below = anchorTop + anchor.getHeight() + GAP;
            top = below + height <= hostHeight ? below : anchorTop - GAP - height;
        }
        top = Math.max(0f, Math.min(top, hostHeight - height));
        host.addComponent(panel).inTL(left, top);
        shown = panel;
        shownFor = key;
        shownSocketable = socketable;
        shownFooter = footer;
        shownAnchor = anchor;
        shownExpanded = expanded;
    }

    public void refreshIfExpansionChanged() {
        if (shown != null && shownExpanded != TooltipExpansion.isExpandedOrHeld()) {
            show(shownFor, shownSocketable, shownFooter, shownAnchor);
        }
    }

    public void hide() {
        if (shown != null) {
            host.removeComponent(shown);
        }
        shown = null;
        shownFor = null;
        shownSocketable = null;
        shownFooter = null;
        shownAnchor = null;
    }

    private static final class Frame extends BaseCustomUIPanelPlugin {

        private final BorderedPanel frame = new BorderedPanel(SocketableHoverTooltip.class);
        private PositionAPI position;

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void renderBelow(float alphaMult) {
            if (position != null) {
                frame.draw(position.getX(), position.getY(), position.getWidth(), position.getHeight(), alphaMult);
            }
        }
    }
}
