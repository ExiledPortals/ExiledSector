package exiledsector.ui.decoration;

record HullBounds(float minForward, float maxForward, float minLeft, float maxLeft) {

    static final HullBounds EMPTY = new HullBounds(Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE);

    HullBounds withSprite(float spriteWidth, float spriteHeight, float centerX, float centerY, float forwardOffset, float leftOffset,
                          float angleDeg) {
        float pivotX = Float.isNaN(centerX) ? spriteWidth / 2f : centerX;
        float pivotY = Float.isNaN(centerY) ? spriteHeight / 2f : centerY;
        double angleRad = Math.toRadians(angleDeg);
        float angleCos = (float) Math.cos(angleRad);
        float angleSin = (float) Math.sin(angleRad);
        HullBounds bounds = this;
        for (float imageX : new float[]{0f, spriteWidth}) {
            for (float imageY : new float[]{0f, spriteHeight}) {
                float forward = imageY - pivotY;
                float left = pivotX - imageX;
                bounds = bounds.with(forwardOffset + HullFrame.rotatedX(forward, left, angleCos, angleSin),
                        leftOffset + HullFrame.rotatedY(forward, left, angleCos, angleSin));
            }
        }
        return bounds;
    }

    private HullBounds with(float forward, float left) {
        return new HullBounds(Math.min(minForward, forward), Math.max(maxForward, forward), Math.min(minLeft, left), Math.max(maxLeft, left));
    }

    float halfLengthAround(float forward) {
        return Math.max(maxForward - forward, forward - minForward);
    }

    float halfWidthAround(float left) {
        return Math.max(maxLeft - left, left - minLeft);
    }
}
