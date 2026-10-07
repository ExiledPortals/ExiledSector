package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.StyledText;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableTooltip;
import exiledsector.ui.TooltipExpansion;
import exiledsector.ui.util.FramedPanelPlugin;

import java.util.List;
import java.util.function.Supplier;

public final class SocketableHoverTooltip {

    private static final float PAD = 10f;
    private static final float GAP = 6f;

    private final CustomPanelAPI hostPanel;
    private CustomPanelAPI shownPanel;
    private Object shownKey;
    private Socketable shownSocketable;
    private Supplier<List<StyledText>> shownFooter;
    private PositionAPI shownAnchor;
    private boolean shownExpanded;

    public SocketableHoverTooltip(CustomPanelAPI hostPanel) {
        this.hostPanel = hostPanel;
    }

    public boolean isShowing(Object key) {
        return shownPanel != null && shownKey == key;
    }

    public void show(Object key, Socketable socketable, Supplier<List<StyledText>> footer, PositionAPI anchor) {
        hide();
        boolean expanded = TooltipExpansion.isExpandedOrHeld();
        float tooltipWidth = SocketableTooltip.WIDTH;
        CustomPanelAPI tooltipPanel = Global.getSettings().createCustom(tooltipWidth, anchor.getHeight(),
                new FramedPanelPlugin(SocketableHoverTooltip.class));
        TooltipMakerAPI element = tooltipPanel.createUIElement(tooltipWidth - PAD * 2f, 0f, false);
        SocketableTooltip.write(element, socketable, footer, expanded);
        float contentHeight = element.getHeightSoFar();
        element.getPosition().setSize(tooltipWidth - PAD * 2f, contentHeight);
        float tooltipHeight = contentHeight + PAD * 2f;
        tooltipPanel.getPosition().setSize(tooltipWidth, tooltipHeight);
        tooltipPanel.addUIElement(element).inTL(PAD, PAD);
        PositionAPI hostPosition = hostPanel.getPosition();
        float hostWidth = hostPosition.getWidth();
        float hostHeight = hostPosition.getHeight();
        float anchorLeft = anchor.getX() - hostPosition.getX();
        float anchorTop = hostPosition.getY() + hostHeight - (anchor.getY() + anchor.getHeight());
        float tooltipLeft;
        float tooltipTop;
        if (anchorLeft + anchor.getWidth() + GAP + tooltipWidth <= hostWidth) {
            tooltipLeft = anchorLeft + anchor.getWidth() + GAP;
            tooltipTop = anchorTop;
        } else if (anchorLeft - GAP - tooltipWidth >= 0f) {
            tooltipLeft = anchorLeft - GAP - tooltipWidth;
            tooltipTop = anchorTop;
        } else {
            tooltipLeft = Math.max(0f, Math.min(anchorLeft, hostWidth - tooltipWidth));
            float belowTop = anchorTop + anchor.getHeight() + GAP;
            tooltipTop = belowTop + tooltipHeight <= hostHeight ? belowTop : anchorTop - GAP - tooltipHeight;
        }
        tooltipTop = Math.max(0f, Math.min(tooltipTop, hostHeight - tooltipHeight));
        hostPanel.addComponent(tooltipPanel).inTL(tooltipLeft, tooltipTop);
        shownPanel = tooltipPanel;
        shownKey = key;
        shownSocketable = socketable;
        shownFooter = footer;
        shownAnchor = anchor;
        shownExpanded = expanded;
    }

    public void refreshIfExpansionChanged() {
        if (shownPanel != null && shownExpanded != TooltipExpansion.isExpandedOrHeld()) {
            show(shownKey, shownSocketable, shownFooter, shownAnchor);
        }
    }

    public void hide() {
        if (shownPanel != null) {
            hostPanel.removeComponent(shownPanel);
        }
        shownPanel = null;
        shownKey = null;
        shownSocketable = null;
        shownFooter = null;
        shownAnchor = null;
    }
}
