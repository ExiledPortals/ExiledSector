package exiledsector.ui.decoration;

interface EscortPath {

    float CRUISING_ENGINE_LEVEL = 0.6f;

    float xAt(float seconds, float headingDeg);

    float yAt(float seconds, float headingDeg);

    float facingAt(float seconds, float headingDeg);

    default float engineLevelAt(float seconds) {
        return CRUISING_ENGINE_LEVEL;
    }
}
