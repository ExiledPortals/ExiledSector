package exiledsector.ui.decoration;

record HullModule(String spritePath, float spriteWidth, float spriteHeight, float centerX, float centerY, float forwardOffset, float leftOffset,
                  float angleDeg, float sizeNum, boolean underParent) {

    static HullModule attached(String spritePath, float spriteWidth, float spriteHeight, float centerX, float centerY, float slotForward,
                               float slotLeft, float slotAngleDeg, float anchorForward, float anchorLeft, float sizeNum, boolean underParent) {
        double slotAngleRad = Math.toRadians(slotAngleDeg);
        float slotCos = (float) Math.cos(slotAngleRad);
        float slotSin = (float) Math.sin(slotAngleRad);
        float forwardOffset = slotForward - HullFrame.rotatedX(anchorForward, anchorLeft, slotCos, slotSin);
        float leftOffset = slotLeft - HullFrame.rotatedY(anchorForward, anchorLeft, slotCos, slotSin);
        return new HullModule(spritePath, spriteWidth, spriteHeight, centerX, centerY, forwardOffset, leftOffset, slotAngleDeg, sizeNum,
                underParent);
    }

    float screenX(float parentScreenX, float facingCos, float facingSin, float screenScale) {
        return parentScreenX + HullFrame.rotatedX(forwardOffset, leftOffset, facingCos, facingSin) * screenScale;
    }

    float screenY(float parentScreenY, float facingCos, float facingSin, float screenScale) {
        return parentScreenY + HullFrame.rotatedY(forwardOffset, leftOffset, facingCos, facingSin) * screenScale;
    }

    float facingDeg(float parentFacingDeg) {
        return parentFacingDeg + angleDeg;
    }
}
