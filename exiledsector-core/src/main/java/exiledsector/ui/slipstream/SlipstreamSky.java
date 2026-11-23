package exiledsector.ui.slipstream;

import exiledsector.ui.TreeViewport;

import java.util.Random;

public final class SlipstreamSky {

    public record Pace(float minFirstSeconds, float maxFirstSeconds, float minRestSeconds, float maxRestSeconds, float viewCrossingSeconds) {
        public static final float BRISK_VIEW_CROSSING_SECONDS = 7f;
        public static final Pace RARE = new Pace(30f, 90f, 120f, 300f, BRISK_VIEW_CROSSING_SECONDS);
        public static final Pace OCCASIONAL = new Pace(15f, 45f, 60f, 150f, BRISK_VIEW_CROSSING_SECONDS);

        public Pace crossingViewIn(float seconds) {
            return new Pace(minFirstSeconds, maxFirstSeconds, minRestSeconds, maxRestSeconds, seconds);
        }
    }

    static final float MIN_RETRY_SECONDS = 8f;
    static final float MAX_RETRY_SECONDS = 15f;

    private final SlipstreamRoutes routes;
    private final Pace pace;
    private final Random random;
    private final SlipstreamPainter painter = new SlipstreamPainter();
    private SlipstreamEvent event;
    private ViewWindow lastView;
    private float untilNext;

    public SlipstreamSky(SlipstreamRoutes routes, Pace pace, Random random) {
        this.routes = routes;
        this.pace = pace;
        this.random = random;
        untilNext = between(pace.minFirstSeconds(), pace.maxFirstSeconds());
    }

    public void advance(float amount) {
        if (event != null) {
            event.advance(amount);
            if (event.isOver()) {
                event = null;
                untilNext = between(pace.minRestSeconds(), pace.maxRestSeconds());
            }
            return;
        }
        untilNext -= amount;
        if (untilNext > 0f) {
            return;
        }
        SlipstreamRoute route = lastView == null ? null : routes.plan(lastView, random);
        if (route == null) {
            untilNext = between(MIN_RETRY_SECONDS, MAX_RETRY_SECONDS);
            return;
        }
        event = new SlipstreamEvent(route, Math.max(lastView.width(), lastView.height()) / pace.viewCrossingSeconds(), random);
    }

    public void render(TreeViewport viewport, float alphaMult) {
        render(SlipstreamProjection.flat(viewport), alphaMult);
    }

    public void render(SlipstreamProjection projection, float alphaMult) {
        lastView = ViewWindow.of(projection.view());
        if (event != null) {
            event.render(painter, projection, alphaMult);
        }
    }

    public boolean isActive() {
        return event != null;
    }

    public void clear() {
        event = null;
        untilNext = between(pace.minFirstSeconds(), pace.maxFirstSeconds());
    }

    SlipstreamEvent event() {
        return event;
    }

    private float between(float minimum, float maximum) {
        return minimum + random.nextFloat() * (maximum - minimum);
    }
}
