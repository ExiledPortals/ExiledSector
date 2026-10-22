package exiledsector.ui;

import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.SkillNode;

public record TreeLayers(TreeViewport treeViewport, TreeViewport carriedViewport, float treeAlpha, float carriedAlpha,
                         boolean carriedInteractive, float exoshipRadius) {

    public static TreeLayers treeOnly(TreeViewport viewport, float treeAlpha) {
        return new TreeLayers(viewport, viewport, treeAlpha, 0f, false, 0f);
    }

    public TreeViewport viewportFor(SkillNode node) {
        return ExoshipNodes.isCarried(node) ? carriedViewport : treeViewport;
    }

    public float alphaFor(SkillNode node) {
        return ExoshipNodes.isCarried(node) ? carriedAlpha : treeAlpha;
    }

    public boolean isInteractive(SkillNode node) {
        return ExoshipNodes.isCarried(node) == carriedInteractive;
    }

    public TreeLayers withAlpha(float alphaMult) {
        return new TreeLayers(treeViewport, carriedViewport, treeAlpha * alphaMult, carriedAlpha * alphaMult, carriedInteractive, exoshipRadius);
    }
}
