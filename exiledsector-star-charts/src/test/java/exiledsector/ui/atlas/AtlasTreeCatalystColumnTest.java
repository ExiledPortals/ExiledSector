package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCatalysts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeCatalystColumnTest {

    private static final float DELTA = 0.001f;
    private static final AtlasCatalysts.Catalyst BREACH = new AtlasCatalysts.Catalyst("breach", "spatial_breach", AtlasCatalysts.Family.TERMINUS, 1f, 1);
    private static final AtlasCatalysts.Catalyst RELAY = new AtlasCatalysts.Catalyst("relay", "relay_network", AtlasCatalysts.Family.SURVEY, 1f, 1);

    @Test
    void cellsKeepTheirFullSizeWhenThereIsRoomAndShrinkToFitWhenThereIsNot() {
        assertEquals(AtlasTreeCatalystColumn.MAX_CELL_SIZE, AtlasTreeCatalystColumn.cellSizeFor(3, 1000f), DELTA);
        assertEquals((200f - AtlasTreeCatalystColumn.PADDING * 2f) / 10f, AtlasTreeCatalystColumn.cellSizeFor(10, 200f), DELTA);
        assertEquals(0f, AtlasTreeCatalystColumn.cellSizeFor(0, 200f), DELTA);
    }

    @Test
    void theCursorPicksTheCatalystInItsRowFromTheTop() {
        AtlasTreeCatalystColumn column = new AtlasTreeCatalystColumn(List.of(BREACH, RELAY));
        column.place(0f, 500f, 0f);
        float cellCentreX = AtlasTreeCatalystColumn.PADDING + AtlasTreeCatalystColumn.MAX_CELL_SIZE / 2f;
        float firstRowY = 500f - AtlasTreeCatalystColumn.PADDING - AtlasTreeCatalystColumn.MAX_CELL_SIZE / 2f;

        assertSame(BREACH, column.catalystAt(cellCentreX, firstRowY));
        assertSame(RELAY, column.catalystAt(cellCentreX, firstRowY - AtlasTreeCatalystColumn.MAX_CELL_SIZE));
        assertNull(column.catalystAt(cellCentreX, 100f));
        assertTrue(column.contains(cellCentreX, firstRowY));
        assertFalse(column.contains(cellCentreX + 200f, firstRowY));
    }

    @Test
    void aHoveredCatalystFocusesItsAnomalyAndNoHoverFocusesNothing() {
        assertEquals(Set.of("spatial_breach"), AtlasTreeCatalystColumn.anomaliesOf(BREACH));
        assertEquals(Set.of(), AtlasTreeCatalystColumn.anomaliesOf(null));
    }
}
