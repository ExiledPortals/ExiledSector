package exiledsector.ui;

import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.SkillNode;

public record TreeLayers(TreeViewport treeViewport, TreeViewport carriedViewport, float treeAlpha, float carriedAlpha,
                         boolean carriedInteractive, float exoshipRadius, ResearchStationLayer station) {

    public TreeLayers(TreeViewport treeViewport, TreeViewport carriedViewport, float treeAlpha, float carriedAlpha,
                      boolean carriedInteractive, float exoshipRadius) {
        this(treeViewport, carriedViewport, treeAlpha, carriedAlpha, carriedInteractive, exoshipRadius, ResearchStationLayer.NONE);
    }

    public static TreeLayers treeOnly(TreeViewport viewport, float treeAlpha) {
        return new TreeLayers(viewport, viewport, treeAlpha, 0f, false, 0f);
    }

    public TreeLayers withStation(ResearchStationLayer stationLayer) {
        return new TreeLayers(treeViewport, carriedViewport, treeAlpha, carriedAlpha, carriedInteractive, exoshipRadius, stationLayer);
    }

    public TreeViewport viewportFor(SkillNode node) {
        if (ExoshipNodes.isCarried(node)) {
            return carriedViewport;
        }
        return station.holds(node) ? station.viewportFor(node, treeViewport) : treeViewport;
    }

    public float alphaFor(SkillNode node) {
        return ExoshipNodes.isCarried(node) ? carriedAlpha : treeAlpha * station.alphaFor(node);
    }

    public boolean isInteractive(SkillNode node) {
        boolean carried = ExoshipNodes.isCarried(node);
        return carried == carriedInteractive && (carried || station.isInteractive(node));
    }

    public TreeLayers withAlpha(float alphaMult) {
        return new TreeLayers(treeViewport, carriedViewport, treeAlpha * alphaMult, carriedAlpha * alphaMult, carriedInteractive, exoshipRadius,
                station);
    }
}
