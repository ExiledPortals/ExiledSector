package exiledsector.ui.socket;

public record BenchLayout(Rect panel, float socketX, float socketY, float socketRadius, Rect details, Rect notice, Rect openButton, Rect materials) {

    public record Rect(float left, float top, float width, float height) {

        public float bottom() {
            return top + height;
        }
    }
}
