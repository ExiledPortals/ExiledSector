package exiledsector.ui.infestation;

import exiledsector.ui.TreeLayers;

interface InfestationPainter {

    void renderUnderNodes(TreeLayers layers, float elapsedSeconds);

    void renderOverNodes(TreeLayers layers, float elapsedSeconds);
}
