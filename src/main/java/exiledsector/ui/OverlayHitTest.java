package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.node.SkillTreeNodeRenderer;

final class OverlayHitTest {

    private final SkillTreeStatPanel statPanel;
    private final SkillTreeReadoutBar ordnancePointsBar;
    private final SkillTreeLevelBar levelBar;
    private final SkillTreeTemplateController templateUi;
    private final SocketPlacement placement;
    private final SkillTreeNodeRenderer nodeRenderer;
    private final HyperspaceMode hyperspace;

    OverlayHitTest(SkillTreeStatPanel statPanel, SkillTreeReadoutBar ordnancePointsBar, SkillTreeLevelBar levelBar,
                   SkillTreeTemplateController templateUi, SocketPlacement placement, SkillTreeNodeRenderer nodeRenderer,
                   HyperspaceMode hyperspace) {
        this.statPanel = statPanel;
        this.ordnancePointsBar = ordnancePointsBar;
        this.levelBar = levelBar;
        this.templateUi = templateUi;
        this.placement = placement;
        this.nodeRenderer = nodeRenderer;
        this.hyperspace = hyperspace;
    }

    boolean contains(PositionAPI position, ScreenRect shipCard, float x, float y) {
        boolean onTree = !hyperspace.isActive();
        return (onTree && statPanel.contains(x, y))
                || ordnancePointsBar.isHovered(position, x, y)
                || levelBar.isHovered(position, x, y)
                || (!nodeRenderer.isStartingRootInputLocked() && onTree && SkillTreeSearchBar.contains(position, x, y))
                || (onTree && templateUi.barContains(x, y))
                || (onTree && placement.buttonContains(x, y))
                || placement.panelContains(x, y)
                || shipCard.contains(x, y);
    }
}
