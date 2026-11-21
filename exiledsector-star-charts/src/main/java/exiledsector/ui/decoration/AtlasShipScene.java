package exiledsector.ui.decoration;

public interface AtlasShipScene {

    void advance(float amount, float headingDeg);

    void render(float screenX, float screenY, float zoom, float headingDeg, float alpha);
}
