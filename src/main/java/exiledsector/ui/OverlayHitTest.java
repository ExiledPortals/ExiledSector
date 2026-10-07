package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.ui.node.SkillTreeNodeRenderer;

final class OverlayHitTest {

    private final SkillTreeStatPanel statPanel;
    private final SkillTreeReadoutBar ordnancePointsBar;
    private final SkillTreeLevelBar levelBar;
    private final SkillTreeTemplateController templateUi;
    private final SocketPlacement socketPlacement;
    private final SkillTreeNodeRenderer nodeRenderer;
    private final HyperspaceMode hyperspaceMode;

    OverlayHitTest(SkillTreeStatPanel statPanel, SkillTreeReadoutBar ordnancePointsBar, SkillTreeLevelBar levelBar,
                   SkillTreeTemplateController templateUi, SocketPlacement socketPlacement, SkillTreeNodeRenderer nodeRenderer,
                   HyperspaceMode hyperspaceMode) {
        this.statPanel = statPanel;
        this.ordnancePointsBar = ordnancePointsBar;
        this.levelBar = levelBar;
        this.templateUi = templateUi;
        this.socketPlacement = socketPlacement;
        this.nodeRenderer = nodeRenderer;
        this.hyperspaceMode = hyperspaceMode;
    }

    boolean contains(PositionAPI canvasPosition, ScreenRect shipCardFrame, float x, float y) {
        boolean onTree = !hyperspaceMode.isActive();
        return (onTree && statPanel.contains(x, y))
                || ordnancePointsBar.isHovered(canvasPosition, x, y)
                || levelBar.isHovered(canvasPosition, x, y)
                || (!nodeRenderer.isStartingRootInputLocked() && onTree && SkillTreeSearchBar.contains(canvasPosition, x, y))
                || (onTree && templateUi.barContains(x, y))
                || (onTree && socketPlacement.buttonContains(x, y))
                || socketPlacement.panelContains(x, y)
                || shipCardFrame.contains(x, y);
    }
}
