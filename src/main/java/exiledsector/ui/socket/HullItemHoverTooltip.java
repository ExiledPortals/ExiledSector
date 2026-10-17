package exiledsector.ui.socket;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.hullitems.HullItem;
import exiledsector.hullitems.HullItemTooltip;
import exiledsector.ui.TooltipExpansion;
import exiledsector.ui.inspect.HullItemCodex;
import exiledsector.ui.util.FramedPanelPlugin;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class HullItemHoverTooltip {

    public enum Placement {BESIDE, BELOW}

    private static final float PAD = 10f;
    private static final float GAP = 6f;

    private final CustomPanelAPI hostPanel;
    private CustomPanelAPI shownPanel;
    private Object shownKey;
    private float shownWidth;
    private BiConsumer<TooltipMakerAPI, Boolean> shownWriter;
    private PositionAPI shownAnchor;
    private Placement shownPlacement;
    private float shownInnerPad;
    private boolean shownExpanded;
    private String shownCodexDefinitionId;

    public HullItemHoverTooltip(CustomPanelAPI hostPanel) {
        this.hostPanel = hostPanel;
    }

    public boolean isShowing(Object key) {
        return shownPanel != null && shownKey == key;
    }

    public void show(Object key, HullItem hullItem, Supplier<List<StyledText>> footer, PositionAPI anchor) {
        show(key, hullItem, null, footer, anchor);
    }

    public void show(Object key, HullItem hullItem, HullSize hullSize, Supplier<List<StyledText>> footer, PositionAPI anchor) {
        String definitionId = hullItem == null ? null : hullItem.definitionId();
        boolean codexLinked = HullItemCodex.existingEntryId(definitionId) != null && !Global.getSettings().isShowingCodex();
        String codexDefinitionId = codexLinked ? definitionId : null;
        Supplier<List<StyledText>> shownFooter = codexDefinitionId == null ? footer : () -> withCodexHint(footer.get());
        show(key, HullItemTooltip.WIDTH, (element, expanded) -> HullItemTooltip.write(element, hullItem, hullSize, shownFooter, expanded),
                anchor, Placement.BESIDE, PAD, codexDefinitionId);
    }

    private static List<StyledText> withCodexHint(List<StyledText> footerLines) {
        List<StyledText> lines = new ArrayList<>(footerLines);
        lines.add(Translation.styled("socketable.tooltip.codexHint"));
        return lines;
    }

    public void show(Object key, float tooltipWidth, BiConsumer<TooltipMakerAPI, Boolean> writer, PositionAPI anchor) {
        show(key, tooltipWidth, writer, anchor, Placement.BESIDE, PAD, null);
    }

    public void show(Object key, float tooltipWidth, BiConsumer<TooltipMakerAPI, Boolean> writer, PositionAPI anchor, Placement placement,
                     float innerPad) {
        show(key, tooltipWidth, writer, anchor, placement, innerPad, null);
    }

    private void show(Object key, float tooltipWidth, BiConsumer<TooltipMakerAPI, Boolean> writer, PositionAPI anchor, Placement placement,
                      float innerPad, String codexDefinitionId) {
        hide();
        boolean expanded = TooltipExpansion.isExpandedOrHeld();
        FramedPanelPlugin panelPlugin = new FramedPanelPlugin();
        if (codexDefinitionId != null) {
            panelPlugin.onKeyDown(Keyboard.KEY_F2, () -> HullItemCodex.open(codexDefinitionId));
        }
        CustomPanelAPI tooltipPanel = Global.getSettings().createCustom(tooltipWidth, anchor.getHeight(), panelPlugin);
        TooltipMakerAPI element = tooltipPanel.createUIElement(tooltipWidth - innerPad * 2f, 0f, false);
        I18n.forGameText(() -> writer.accept(element, expanded));
        float contentHeight = element.getHeightSoFar();
        element.getPosition().setSize(tooltipWidth - innerPad * 2f, contentHeight);
        float tooltipHeight = contentHeight + innerPad * 2f;
        tooltipPanel.getPosition().setSize(tooltipWidth, tooltipHeight);
        tooltipPanel.addUIElement(element).inTL(innerPad, innerPad);
        PositionAPI hostPosition = hostPanel.getPosition();
        float hostWidth = hostPosition.getWidth();
        float hostHeight = hostPosition.getHeight();
        float anchorLeft = anchor.getX() - hostPosition.getX();
        float anchorTop = hostPosition.getY() + hostHeight - (anchor.getY() + anchor.getHeight());
        float tooltipLeft;
        float tooltipTop;
        if (placement == Placement.BELOW) {
            tooltipLeft = Math.max(0f, Math.min(anchorLeft, hostWidth - tooltipWidth));
            tooltipTop = anchorTop + anchor.getHeight() + GAP;
        } else if (anchorLeft + anchor.getWidth() + GAP + tooltipWidth <= hostWidth) {
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
        shownWidth = tooltipWidth;
        shownWriter = writer;
        shownAnchor = anchor;
        shownPlacement = placement;
        shownInnerPad = innerPad;
        shownExpanded = expanded;
        shownCodexDefinitionId = codexDefinitionId;
    }

    public void refreshIfExpansionChanged() {
        if (shownPanel != null && shownExpanded != TooltipExpansion.isExpandedOrHeld()) {
            show(shownKey, shownWidth, shownWriter, shownAnchor, shownPlacement, shownInnerPad, shownCodexDefinitionId);
        }
    }

    public void hide() {
        if (shownPanel != null) {
            hostPanel.removeComponent(shownPanel);
        }
        shownPanel = null;
        shownKey = null;
        shownWriter = null;
        shownAnchor = null;
        shownPlacement = null;
        shownCodexDefinitionId = null;
    }
}
