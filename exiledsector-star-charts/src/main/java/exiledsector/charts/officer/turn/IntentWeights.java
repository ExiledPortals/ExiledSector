package exiledsector.charts.officer.turn;

public record IntentWeights(float expand, float push, float challenge, float defend, float hold) {

    public static final IntentWeights EVEN = new IntentWeights(1f, 1f, 1f, 1f, 1f);

    public float of(IntentKind kind) {
        return switch (kind) {
            case EXPAND -> expand;
            case PUSH -> push;
            case CHALLENGE -> challenge;
            case DEFEND -> defend;
            case HOLD -> hold;
        };
    }
}
