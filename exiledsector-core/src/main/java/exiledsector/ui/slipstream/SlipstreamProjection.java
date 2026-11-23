package exiledsector.ui.slipstream;

import exiledsector.ui.TreeViewport;

public interface SlipstreamProjection {

    int X = 0;
    int Y = 1;
    int PLACED_LENGTH = 3;

    @FunctionalInterface
    interface Placement {
        void place(float worldX, float worldY, float[] out);
    }

    TreeViewport view();

    void place(float worldX, float worldY, float[] out);

    static SlipstreamProjection flat(TreeViewport view) {
        return of(view, (worldX, worldY, out) -> {
            out[X] = view.screenX(worldX);
            out[Y] = view.screenY(worldY);
        });
    }

    static SlipstreamProjection of(TreeViewport view, Placement placement) {
        return new SlipstreamProjection() {
            @Override
            public TreeViewport view() {
                return view;
            }

            @Override
            public void place(float worldX, float worldY, float[] out) {
                placement.place(worldX, worldY, out);
            }
        };
    }
}
