package exiledsector.ui.slipstream;

import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Slipstream {

    public static final float CLEARANCE_WIDTHS = 0.85f;
    static final float SEGMENT_SPACING_WIDTHS = 0.5f;
    static final float MAX_START_WIDTH_SHARE = 1.15f;
    static final float MAX_WIDTH_SHARE = 1.45f;
    static final float WIDEN_SHARE_PER_SECOND = 0.01f;
    static final float WOBBLE_SHARE = 0.05f;
    static final float HEAD_FADE_WIDTHS = 3f;
    static final float SEGMENT_FADE_IN_SECONDS = 1.5f;
    static final float TEXTURE_REPEAT_WIDTHS = 4f / 3f;
    static final float EDGE_WIDTH_SHARE = 1f / 3f;
    static final float SCROLL_REPEATS_PER_SECOND = 0.3f;
    static final float MIDDLE_LAYER_SCROLL = -1.13f;
    static final float TOP_LAYER_SCROLL = -0.28f;
    static final float DISSOLVE_SECONDS = 4f;
    static final float MIN_SEGMENT_FADE_OUT_SECONDS = 1f;
    static final int SEGMENTS_PER_DISSOLVE_KNOT = 3;

    private final SlipstreamRoute route;
    private final Random random;
    private final float baseWidth;
    private final float spacing;
    private final List<StreamSegment> segments = new ArrayList<>();
    private final List<StreamSegment> segmentView = Collections.unmodifiableList(segments);
    private float headDistance;
    private float middleLayerScroll;
    private float topLayerScroll;
    private float[] dissolveOrder;
    private float dissolveElapsed;

    public Slipstream(SlipstreamRoute route, Random random) {
        this.route = route;
        this.random = random;
        baseWidth = route.width();
        spacing = Math.max(1f, baseWidth * SEGMENT_SPACING_WIDTHS);
    }

    public SlipstreamRoute route() {
        return route;
    }

    public float baseWidth() {
        return baseWidth;
    }

    public float edgeWidth() {
        return baseWidth * EDGE_WIDTH_SHARE;
    }

    public float textureRepeatLength() {
        return baseWidth * TEXTURE_REPEAT_WIDTHS;
    }

    public float headDistance() {
        return headDistance;
    }

    public float laidLength() {
        return segments.isEmpty() ? 0f : segments.get(segments.size() - 1).distance;
    }

    public float middleLayerScroll() {
        return middleLayerScroll;
    }

    public float topLayerScroll() {
        return topLayerScroll;
    }

    List<StreamSegment> segments() {
        return segmentView;
    }

    public void extendTo(float distance) {
        headDistance = Math.max(headDistance, distance);
        if (dissolveOrder != null) {
            return;
        }
        boolean added = false;
        float routeLength = route.length();
        while (true) {
            float next = segments.isEmpty() ? 0f : Math.min(routeLength, laidLength() + spacing);
            if (next > headDistance || (!segments.isEmpty() && laidLength() >= routeLength)) {
                break;
            }
            addSegment(next);
            added = true;
        }
        if (added) {
            recomputeNormals();
        }
    }

    public void advance(float amount) {
        float scroll = SCROLL_REPEATS_PER_SECOND * amount;
        middleLayerScroll = wrapped(middleLayerScroll + scroll * MIDDLE_LAYER_SCROLL);
        topLayerScroll = wrapped(topLayerScroll + scroll * TOP_LAYER_SCROLL);
        float widenPerSecond = baseWidth * WIDEN_SHARE_PER_SECOND;
        for (StreamSegment segment : segments) {
            segment.advance(amount, widenPerSecond, SEGMENT_FADE_IN_SECONDS);
        }
        if (dissolveOrder != null) {
            advanceDissolve(amount);
        }
    }

    public void dissolve() {
        if (dissolveOrder == null) {
            dissolveOrder = dissolveOrder(segments.size(), random);
            dissolveElapsed = 0f;
        }
    }

    public boolean isDissolving() {
        return dissolveOrder != null;
    }

    public boolean isGone() {
        if (dissolveOrder == null) {
            return false;
        }
        for (StreamSegment segment : segments) {
            if (!segment.isGone()) {
                return false;
            }
        }
        return true;
    }

    public float alphaOf(int index) {
        if (index <= 0 || index >= segments.size() - 1) {
            return 0f;
        }
        StreamSegment segment = segments.get(index);
        return segment.brightness * headFade(segment.distance);
    }

    public float alphaAt(float distance) {
        if (segments.size() < 3 || distance <= 0f || distance >= laidLength()) {
            return 0f;
        }
        int index = Math.min(segments.size() - 2, (int) (distance / spacing));
        float along = Math.max(0f, Math.min(1f, (distance - segments.get(index).distance) / spacing));
        return alphaOf(index) * (1f - along) + alphaOf(index + 1) * along;
    }

    public float widthAt(float distance) {
        if (segments.isEmpty()) {
            return baseWidth;
        }
        int index = Math.max(0, Math.min(segments.size() - 1, (int) (distance / spacing)));
        return segments.get(index).width;
    }

    private float headFade(float distance) {
        return Math.max(0f, Math.min(1f, (headDistance - distance) / (baseWidth * HEAD_FADE_WIDTHS)));
    }

    private void addSegment(float distance) {
        Vector2f center = route.pointAt(distance);
        float width = baseWidth * (1f + random.nextFloat() * (MAX_START_WIDTH_SHARE - 1f));
        segments.add(new StreamSegment(distance, center.x, center.y, width, baseWidth * MAX_WIDTH_SHARE, baseWidth * WOBBLE_SHARE, random));
    }

    private void recomputeNormals() {
        int count = segments.size();
        float previousX = 0f;
        float previousY = 0f;
        for (int i = 0; i < count; i++) {
            StreamSegment segment = segments.get(i);
            Vector2f heading = route.headingAt(i < count - 1 ? (segment.distance + segments.get(i + 1).distance) / 2f : segment.distance);
            float averageX = i == 0 ? heading.x : (previousX + heading.x) / 2f;
            float averageY = i == 0 ? heading.y : (previousY + heading.y) / 2f;
            float length = (float) Math.hypot(averageX, averageY);
            if (length <= 0f) {
                averageX = heading.x;
                averageY = heading.y;
                length = 1f;
            }
            segment.normalX = -averageY / length;
            segment.normalY = averageX / length;
            previousX = heading.x;
            previousY = heading.y;
        }
    }

    private void advanceDissolve(float amount) {
        dissolveElapsed += amount;
        float progress = dissolveElapsed / DISSOLVE_SECONDS;
        float fadeSeconds = Math.max(MIN_SEGMENT_FADE_OUT_SECONDS, (DISSOLVE_SECONDS - dissolveElapsed) / 2f);
        for (int i = 0; i < segments.size(); i++) {
            if (i >= dissolveOrder.length || dissolveOrder[i] <= progress) {
                segments.get(i).fadeOut(fadeSeconds);
            }
        }
    }

    static float[] dissolveOrder(int count, Random random) {
        float[] order = new float[count];
        if (count == 0) {
            return order;
        }
        int knots = Math.max(2, count / SEGMENTS_PER_DISSOLVE_KNOT + 2);
        float[] knotValues = new float[knots];
        for (int i = 0; i < knots; i++) {
            knotValues[i] = random.nextFloat();
        }
        float minimum = Float.MAX_VALUE;
        float maximum = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            float position = count == 1 ? 0f : i / (float) (count - 1) * (knots - 1);
            int knot = Math.min(knots - 2, (int) position);
            float along = position - knot;
            float eased = (1f - (float) Math.cos(along * Math.PI)) / 2f;
            order[i] = knotValues[knot] * (1f - eased) + knotValues[knot + 1] * eased;
            minimum = Math.min(minimum, order[i]);
            maximum = Math.max(maximum, order[i]);
        }
        float range = maximum - minimum;
        for (int i = 0; i < count; i++) {
            order[i] = range <= 0f ? 0f : (order[i] - minimum) / range;
        }
        return order;
    }

    private static float wrapped(float scroll) {
        return scroll - (float) Math.floor(scroll);
    }
}
