package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasAbyssShadeTest {

    @Test
    void abyssalSpaceIsFullyDarkInsideClearOutsideAndFadesAcrossTheBorder() {
        AtlasAbyssShade shade = new AtlasAbyssShade();
        shade.attach(cell -> cell.gridX() >= 3, List.of(new AtlasCell(4, 0)));
        assertTrue(shade.hasAbyss());
        int perCell = Math.round(1f / AtlasAbyssShade.TILE_CELLS);

        assertEquals(1f, shade.shade(5 * perCell, 0));
        assertEquals(0f, shade.shade(0, 7));
        float border = shade.shade(3 * perCell - 1, 0);
        assertTrue(border > 0f && border < 1f, "border " + border);
        assertTrue(shade.shade(3 * perCell, 0) > border);
    }

    @Test
    void shadesAreSampledOnceUntilTheSceneryIsAttachedAgain() {
        AtomicInteger samples = new AtomicInteger();
        AtlasAbyssShade shade = new AtlasAbyssShade();
        shade.attach(cell -> {
            samples.incrementAndGet();
            return cell.equals(new AtlasCell(0, 0));
        }, List.of());
        assertFalse(shade.hasAbyss());

        shade.shade(0, 0);
        int first = samples.get();
        shade.shade(0, 0);
        assertEquals(first, samples.get());
        shade.attach(cell -> false, List.of(new AtlasCell(0, 0)));
        assertFalse(shade.hasAbyss());
        assertEquals(0f, shade.shade(0, 0));
    }
}
