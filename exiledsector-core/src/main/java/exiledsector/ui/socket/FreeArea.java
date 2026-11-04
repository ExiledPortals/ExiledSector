package exiledsector.ui.socket;

public record FreeArea(float left, float top, float width, float height) {

    public float right() {
        return left + width;
    }

    public float bottom() {
        return top + height;
    }
}
