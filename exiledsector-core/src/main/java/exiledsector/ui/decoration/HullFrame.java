package exiledsector.ui.decoration;

final class HullFrame {

    private HullFrame() {
    }

    static float rotatedX(float forward, float left, float facingCos, float facingSin) {
        return forward * facingCos - left * facingSin;
    }

    static float rotatedY(float forward, float left, float facingCos, float facingSin) {
        return forward * facingSin + left * facingCos;
    }
}
