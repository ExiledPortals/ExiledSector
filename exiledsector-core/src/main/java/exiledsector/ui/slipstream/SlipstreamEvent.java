package exiledsector.ui.slipstream;

import java.util.Random;

final class SlipstreamEvent {

    static final float LINGER_SECONDS = 30f;
    static final float LINGER_JITTER_SECONDS = 3f;

    private final Random random;
    private final Slipstream stream;
    private final StreamParticles particles = new StreamParticles();
    private final SensorGhostPack ghosts;
    private final float speed;
    private final float startDistance;
    private final float dissolveAfter;
    private float elapsed;

    SlipstreamEvent(SlipstreamRoute route, float speed, float startDistance, Random random) {
        this.random = random;
        this.speed = speed;
        this.startDistance = startDistance;
        stream = new Slipstream(route, random);
        ghosts = new SensorGhostPack(route, random);
        dissolveAfter = (route.length() - startDistance) / speed + LINGER_SECONDS + (random.nextFloat() * 2f - 1f) * LINGER_JITTER_SECONDS;
    }

    Slipstream stream() {
        return stream;
    }

    void advance(float amount) {
        elapsed += amount;
        float headDistance = startDistance + speed * elapsed;
        stream.extendTo(headDistance);
        stream.advance(amount);
        ghosts.advance(amount, headDistance);
        particles.advance(amount, stream, random);
        if (elapsed >= dissolveAfter) {
            stream.dissolve();
        }
    }

    boolean isOver() {
        return stream.isGone() && ghosts.isDone();
    }

    void render(SlipstreamPainter painter, SlipstreamProjection projection, float alphaMult) {
        if (alphaMult <= 0f) {
            return;
        }
        painter.paintStream(stream, projection, alphaMult);
        painter.paintParticles(stream, particles, projection, alphaMult);
        painter.paintGhosts(ghosts, projection, alphaMult);
    }
}
