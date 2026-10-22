package exiledlib.ui.util;

public final class WobblyOrbit {

    public static final class Point {
        private float pointX;
        private float pointY;

        public float x() {
            return pointX;
        }

        public float y() {
            return pointY;
        }
    }

    private final float wobbleShare;
    private final int wobbleWaves;
    private final float wobblePhaseRad;
    private final float squash;
    private final float tiltCos;
    private final float tiltSin;

    public WobblyOrbit(float wobbleShare, int wobbleWaves, float wobblePhaseRad, float squash, float tiltDeg) {
        this.wobbleShare = wobbleShare;
        this.wobbleWaves = wobbleWaves;
        this.wobblePhaseRad = wobblePhaseRad;
        this.squash = squash;
        double tilt = Math.toRadians(tiltDeg);
        this.tiltCos = (float) Math.cos(tilt);
        this.tiltSin = (float) Math.sin(tilt);
    }

    public float wobbleShare() {
        return wobbleShare;
    }

    public Point locate(float centerX, float centerY, float orbitRadius, float angleRad, Point into) {
        float radius = orbitRadius * (1f + wobbleShare * (float) Math.sin(wobbleWaves * angleRad + wobblePhaseRad));
        float localX = radius * (float) Math.cos(angleRad);
        float localY = radius * squash * (float) Math.sin(angleRad);
        into.pointX = centerX + localX * tiltCos - localY * tiltSin;
        into.pointY = centerY + localX * tiltSin + localY * tiltCos;
        return into;
    }
}
