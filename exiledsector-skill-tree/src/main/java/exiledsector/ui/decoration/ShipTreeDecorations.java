package exiledsector.ui.decoration;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.ui.hyperspace.HyperspaceAnchor;
import exiledsector.ui.slipstream.SlipstreamSky;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class ShipTreeDecorations {

    private ShipTreeDecorations() {
    }

    public static TreeDecorations forActiveTree(Predicate<SkillNode> nodeHidden, Predicate<SkillNode> nodeConcealed) {
        Set<String> concealedVolumes = HyperspaceAnchor.lockedVolumes(SkillTree.getAllNodes().values(), SkillNode::getVolume, nodeConcealed);
        return TreeDecorations.of(SkillTree.getDecorations().outsideVolumes(concealedVolumes), List.of(FactionFleetRenderer.forActiveTree(nodeHidden),
                new SlipstreamDecoration(new SlipstreamSky(TreeSlipstreamRoutes.forActiveTree(), SlipstreamSky.Pace.RARE, new Random()))));
    }
}
