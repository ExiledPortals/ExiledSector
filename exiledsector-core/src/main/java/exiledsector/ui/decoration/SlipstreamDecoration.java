package exiledsector.ui.decoration;

import exiledsector.ui.slipstream.SlipstreamSky;

public final class SlipstreamDecoration implements DecorationRenderer {

    private final SlipstreamSky sky;

    public SlipstreamDecoration(SlipstreamSky sky) {
        this.sky = sky;
    }

    @Override
    public void advance(float amount) {
        sky.advance(amount);
    }

    @Override
    public void render(DecorationScene scene, DecorationDepth depth) {
        if (depth == DecorationDepth.SLIPSTREAMS) {
            sky.render(scene.viewport(), scene.sceneryAlpha() * scene.treeAlpha());
        }
    }
}
