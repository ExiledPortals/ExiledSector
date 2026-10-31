package exiledsector.ui.decoration;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.decoration.Decorations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class TreeDecorations {

    private final List<DecorationRenderer> renderers;

    TreeDecorations(List<DecorationRenderer> renderers) {
        this.renderers = List.copyOf(renderers);
    }

    public static TreeDecorations forActiveTree(Predicate<SkillNode> nodeHidden) {
        List<DecorationRenderer> renderers = new ArrayList<>(sceneryOf(SkillTree.getDecorations()));
        renderers.add(FactionFleetRenderer.forActiveTree(nodeHidden));
        return new TreeDecorations(renderers);
    }

    static TreeDecorations of(Decorations decorations) {
        return new TreeDecorations(sceneryOf(decorations));
    }

    private static List<DecorationRenderer> sceneryOf(Decorations decorations) {
        return List.of(
                new SkillTreeStaticImageRenderer(decorations.images()),
                new SkillTreeStarRenderer(decorations.stars()),
                new SkillTreeRingBeltRenderer(decorations.ringBelts()),
                new AbyssSerpentRenderer(decorations.serpents()),
                new ThreatUnitRenderer(decorations.threatUnits()));
    }

    public void advance(float amount) {
        for (DecorationRenderer renderer : renderers) {
            renderer.advance(amount);
        }
    }

    public void renderBehindTree(DecorationScene scene) {
        render(scene, false);
    }

    public void renderOverTree(DecorationScene scene) {
        render(scene, true);
    }

    private void render(DecorationScene scene, boolean overTree) {
        for (DecorationDepth depth : DecorationDepth.values()) {
            if (depth.isOverTree() != overTree) {
                continue;
            }
            for (DecorationRenderer renderer : renderers) {
                renderer.render(scene, depth);
            }
        }
    }
}
