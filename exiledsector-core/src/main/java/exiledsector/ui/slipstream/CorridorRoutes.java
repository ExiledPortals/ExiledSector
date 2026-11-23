package exiledsector.ui.slipstream;

import java.util.Random;

public final class CorridorRoutes implements SlipstreamRoutes {

    @FunctionalInterface
    public interface Obstacles {
        void block(CorridorGrid grid);
    }

    private final float widthViewShare;
    private final float minWidth;
    private final float maxWidth;
    private final Obstacles obstacles;

    public CorridorRoutes(float widthViewShare, float minWidth, float maxWidth, Obstacles obstacles) {
        this.widthViewShare = widthViewShare;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.obstacles = obstacles;
    }

    public float widthFor(ViewWindow view) {
        return Math.max(minWidth, Math.min(maxWidth, view.shortSide() * widthViewShare));
    }

    @Override
    public SlipstreamRoute plan(ViewWindow view, Random random) {
        float width = widthFor(view);
        CorridorGrid grid = CorridorGrid.covering(view, width);
        obstacles.block(grid);
        return CorridorSearch.route(grid, view, width, random);
    }
}
