package exiledsector.ui.decoration;

record TowLine(float length, float lagSeconds) {

    float xAt(GroupPose tug, float baseHeadingDeg, float seconds) {
        return tug.x() - length * (float) Math.cos(Math.toRadians(tug.headingAt(baseHeadingDeg, seconds - lagSeconds)));
    }

    float yAt(GroupPose tug, float baseHeadingDeg, float seconds) {
        return tug.y() - length * (float) Math.sin(Math.toRadians(tug.headingAt(baseHeadingDeg, seconds - lagSeconds)));
    }
}
