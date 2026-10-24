package exiledsector.ui;

import exiledsector.ui.hyperspace.HyperspaceCamera;
import exiledsector.ui.node.TreeAllocationSession;

final class TreeCamera extends CanvasCamera {

    private StartingRootCameraFollow startingRootFollow;

    void beginStartingRootFollow(TreeAllocationSession treeSession) {
        if (treeSession.isStartingRootMoving() && startingRootFollow == null) {
            startingRootFollow = new StartingRootCameraFollow(centreTreeX(), centreTreeY(),
                    treeSession.startingRootCameraTargetX(), treeSession.startingRootCameraTargetY(), zoom(),
                    treeSession.isStartingRootFlyingOut() ? SmoothZoom.MIN_ZOOM : StartingRootCameraFollow.CHOOSING_ZOOM);
        }
    }

    void applyStartingRootFollow(TreeAllocationSession treeSession, boolean wasMoving) {
        if (!wasMoving) {
            return;
        }
        float followProgress = treeSession.startingRootCameraProgress();
        jumpZoomTo(startingRootFollow.zoom(followProgress));
        centreOn(startingRootFollow.x(treeSession.startingRootCameraTargetX(), followProgress),
                startingRootFollow.y(treeSession.startingRootCameraTargetY(), followProgress));
        if (!treeSession.isStartingRootMoving()) {
            startingRootFollow = null;
        }
    }

    void jumpTo(HyperspaceCamera hyperspaceCamera) {
        jumpZoomTo(hyperspaceCamera.zoom());
        centreOn(hyperspaceCamera.x(), hyperspaceCamera.y());
    }

    HyperspaceCamera current() {
        return new HyperspaceCamera(centreTreeX(), centreTreeY(), zoom());
    }
}
