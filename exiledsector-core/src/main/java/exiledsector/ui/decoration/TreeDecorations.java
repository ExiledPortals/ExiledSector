package exiledsector.ui.decoration;

import exiledsector.skills.decoration.Decorations;

import java.util.ArrayList;
import java.util.List;

public final class TreeDecorations {

    private final List<DecorationRenderer> renderers;

    private TreeDecorations(List<DecorationRenderer> renderers) {
        this.renderers = List.copyOf(renderers);
    }

    public static TreeDecorations of(Decorations decorations) {
        return new TreeDecorations(sceneryOf(decorations));
    }

    public static TreeDecorations of(Decorations decorations, List<DecorationRenderer> extraRenderers) {
        List<DecorationRenderer> renderers = new ArrayList<>(sceneryOf(decorations));
        renderers.addAll(extraRenderers);
        return new TreeDecorations(renderers);
    }

    private static List<DecorationRenderer> sceneryOf(Decorations decorations) {
        return List.of(
                new SkillTreeStaticImageRenderer(decorations.images()),
                new SkillTreeStarRenderer(decorations.stars()),
                new SkillTreeRingBeltRenderer(decorations.ringBelts()),
                new AbyssSerpentRenderer(decorations.serpents()),
                new ThreatUnitRenderer(decorations.threatUnits()),
                new ShroudedMawRenderer(decorations.shroudedMaws()),
                new HypershuntRenderer(decorations.hypershunts()));
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
