package exiledsector.ui.decoration;

record GuardOrbitPath(ErraticOrbit orbit) implements EscortPath {

    @Override
    public float xAt(float seconds, float headingDeg) {
        return orbit.xAt(seconds);
    }

    @Override
    public float yAt(float seconds, float headingDeg) {
        return orbit.yAt(seconds);
    }

    @Override
    public float facingAt(float seconds, float headingDeg) {
        return orbit.headingDegreesAt(seconds);
    }
}
