package exiledsector.ui;

import exiledsector.skills.SkillNode;

import java.util.Map;

public final class ResearchStationLayer {

    static final float MIN_HIT_PIXELS = 12f;
    static final ResearchStationLayer NONE = new ResearchStationLayer(null, Map.of(), new float[0], new float[0], new float[0], new boolean[0],
            0f, 1f, true);

    private final SkillNode stationNode;
    private final Map<SkillNode, Integer> slotByNode;
    private final float[] shiftX;
    private final float[] shiftY;
    private final float[] alpha;
    private final boolean[] interactive;
    private final float stationHitRadius;
    private float surroundingAlpha;
    private boolean surroundingInteractive;

    ResearchStationLayer(SkillNode stationNode, Map<SkillNode, Integer> slotByNode, float[] shiftX, float[] shiftY, float[] alpha,
                         boolean[] interactive, float stationHitRadius, float surroundingAlpha, boolean surroundingInteractive) {
        this.stationNode = stationNode;
        this.slotByNode = slotByNode;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
        this.alpha = alpha;
        this.interactive = interactive;
        this.stationHitRadius = stationHitRadius;
        this.surroundingAlpha = surroundingAlpha;
        this.surroundingInteractive = surroundingInteractive;
    }

    void setSurroundings(float alpha, boolean interactive) {
        surroundingAlpha = alpha;
        surroundingInteractive = interactive;
    }

    public boolean holds(SkillNode node) {
        return slotByNode.containsKey(node);
    }

    public boolean isStation(SkillNode node) {
        return node != null && node == stationNode;
    }

    public float stationHitHalfSize(float zoom) {
        return Math.max(stationHitRadius * zoom, MIN_HIT_PIXELS);
    }

    TreeViewport viewportFor(SkillNode node, TreeViewport treeViewport) {
        int slot = slotByNode.get(node);
        float zoom = treeViewport.zoom();
        return new TreeViewport(treeViewport.centerX() + shiftX[slot] * zoom, treeViewport.centerY() - shiftY[slot] * zoom, zoom,
                treeViewport.left(), treeViewport.bottom(), treeViewport.right(), treeViewport.top());
    }

    float alphaFor(SkillNode node) {
        Integer slot = slotByNode.get(node);
        return slot == null ? surroundingAlpha : alpha[slot];
    }

    boolean isInteractive(SkillNode node) {
        Integer slot = slotByNode.get(node);
        return slot == null ? surroundingInteractive : interactive[slot];
    }
}
