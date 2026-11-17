package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasCourseArrowsTest {

    private static final float SIZE = 10f;
    private static final float SPACING = SIZE * (1f + AtlasCourseArrows.GAP_SHARE);

    private record Arrow(float x, float y, float heading, float alphaShare) {
    }

    private static List<Arrow> place(float[] xs, float[] ys, float startTrim, float endTrim, double travelled) {
        List<Arrow> arrows = new ArrayList<>();
        new AtlasCourseArrows().place(xs, ys, xs.length, startTrim, endTrim, SIZE, travelled,
                (x, y, heading, share) -> arrows.add(new Arrow(x, y, heading, share)));
        return arrows;
    }

    @Test
    void arrowsAreSpacedAlongTheVisiblePartOfTheConnectorPointingAtItsEnd() {
        List<Arrow> arrows = place(new float[]{0f, 200f}, new float[]{0f, 0f}, 10f, 10f, 0.0);

        assertEquals((int) Math.ceil(180f / SPACING), arrows.size());
        assertEquals(10f, arrows.get(0).x(), 0.01f);
        assertEquals(10f + SPACING, arrows.get(1).x(), 0.01f);
        assertTrue(arrows.stream().allMatch(arrow -> arrow.x() < 190f && arrow.heading() == 0f));
    }

    @Test
    void arrowsFadeInAtTheStartAndOutAtTheEnd() {
        List<Arrow> arrows = place(new float[]{0f, 200f}, new float[]{0f, 0f}, 10f, 10f, 0.0);

        assertEquals(0f, arrows.get(0).alphaShare(), 0.001f);
        assertEquals(SPACING / (AtlasCourseArrows.FADE_SIZES * SIZE), arrows.get(1).alphaShare(), 0.001f);
        assertEquals(1f, arrows.get(arrows.size() / 2).alphaShare(), 0.001f);
        assertTrue(arrows.get(arrows.size() - 1).alphaShare() < 0.5f);
    }

    @Test
    void arrowsTravelTowardTheEndAndWrapEverySpacing() {
        float[] xs = {0f, 200f};
        float[] ys = {0f, 0f};

        assertEquals(15f, place(xs, ys, 10f, 10f, 5.0).get(0).x(), 0.01f);
        assertEquals(10f, place(xs, ys, 10f, 10f, SPACING).get(0).x(), 0.01f);
    }

    @Test
    void arrowsOnABentConnectorFaceAlongTheirOwnStretch() {
        List<Arrow> arrows = place(new float[]{0f, 100f, 100f}, new float[]{0f, 0f, 100f}, 0f, 0f, 0.0);

        Arrow early = arrows.get(1);
        Arrow late = arrows.get(arrows.size() - 2);
        assertEquals(0f, early.heading(), 0.01f);
        assertEquals(0f, early.y(), 0.01f);
        assertEquals(90f, late.heading(), 0.01f);
        assertEquals(100f, late.x(), 0.01f);
    }

    @Test
    void aConnectorShorterThanOneArrowDrawsNone() {
        assertTrue(place(new float[]{0f, 25f}, new float[]{0f, 0f}, 10f, 10f, 0.0).isEmpty());
    }
}
