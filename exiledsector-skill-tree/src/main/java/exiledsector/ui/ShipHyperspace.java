package exiledsector.ui;

import exiledsector.i18n.Translation;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.hyperspace.HyperspaceAnchor;
import exiledsector.ui.hyperspace.HyperspaceMode;
import exiledsector.ui.node.TreeAllocationSession;

final class ShipHyperspace {

    private static final HyperspaceMode.Framing SHIP_FRAMING = new HyperspaceMode.Framing(SkillTags.CORE_VOLUME, SkillTags.ABYSSAL_VOLUMES);

    private ShipHyperspace() {
    }

    static HyperspaceMode mode(TreeCamera camera, SkillTreeStatPanel statPanel, TreeAllocationSession treeSession) {
        return new HyperspaceMode(camera, () -> mapContent(treeSession), SHIP_FRAMING, volume -> Translation.text("hyperspace.volume." + volume),
                statPanel::close);
    }

    private static HyperspaceMode.MapContent mapContent(TreeAllocationSession treeSession) {
        return new HyperspaceMode.MapContent(SkillTree.getDecorations(),
                HyperspaceAnchor.lockedVolumes(SkillTree.getAllNodes().values(), SkillNode::getVolume, treeSession::isHidden), treeSession.wormholePairs());
    }
}
