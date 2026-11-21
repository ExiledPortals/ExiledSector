package exiledsector.ui.decoration;

public interface DecorationRenderer {

    void advance(float amount);

    void render(DecorationScene scene, DecorationDepth depth);
}
