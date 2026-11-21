package exiledsector.ui.infestation;

interface FragmentSink {

    void fragment(float worldX, float worldY, float worldSize, float angleDeg, float brightness);

    void glow(float worldX, float worldY, float worldSize, float brightness);
}
