package exiledsector.harness;

import exiledsector.ui.CanvasCamera;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.ZoomableScreen;
import exiledsector.ui.hyperspace.HyperspaceCamera;

final class ZoomStep implements HarnessStep {

    private final float targetZoom;

    ZoomStep(float targetZoom) {
        this.targetZoom = targetZoom;
    }

    @Override
    public String name() {
        return "zoom " + targetZoom;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        ShownScreens.Shown shown = ShownScreens.top();
        if (shown == null || !(shown.screen() instanceof ZoomableScreen zoomable)) {
            return StepOutcome.failed("the shown screen (" + Harness.describe(shown == null ? null : shown.screen()) + ") has no camera to zoom");
        }
        CanvasCamera camera = zoomable.camera();
        float startZoom = camera.zoom();
        camera.jumpTo(new HyperspaceCamera(camera.centreTreeX(), camera.centreTreeY(), targetZoom));
        return StepOutcome.passed(Harness.describe(shown.screen()) + " zoom " + startZoom + " -> " + camera.zoom());
    }
}
