package exiledsector.ui.infestation;

import java.awt.Color;

interface PuffSink {

    void puff(float worldX, float worldY, float worldSize, float angleDeg, int cell, Color tint, float alpha);
}
