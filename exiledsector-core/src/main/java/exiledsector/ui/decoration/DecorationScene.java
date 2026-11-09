package exiledsector.ui.decoration;

import exiledsector.ui.TreeViewport;

import java.util.Set;

public record DecorationScene(TreeViewport viewport, float sceneryAlpha, float treeAlpha, Set<String> mapAnchorIds, StarMap starMap) {

    public record StarMap(float radius, float amount, Set<String> hiddenStarIds) {
        public static final StarMap NONE = new StarMap(0f, 0f, Set.of());
    }

    public float alphaFor(String decorationId) {
        return mapAnchorIds.contains(decorationId) ? sceneryAlpha : sceneryAlpha * treeAlpha;
    }
}
