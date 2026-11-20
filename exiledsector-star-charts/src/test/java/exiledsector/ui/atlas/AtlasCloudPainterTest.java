package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Field;
import exiledsector.charts.tree.AtlasBodyArt.Storm;
import exiledsector.ui.atlas.AtlasCloudPainter.Cover;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasCloudPainterTest {

    private static final String BOLT_HOLE_LIGHT = "#ffbe5a";

    @Test
    void stormFrontPlanetsGetStormCloudsWhetherOrNotThePlanetHasCloudsOfItsOwn() {
        for (Storm storm : EnumSet.of(Storm.GENERIC, Storm.LIGHTNING_ROD, Storm.SUPERCELL, Storm.EYE, Storm.CAPTURED, Storm.DUEL)) {
            assertEquals(Cover.STORM, AtlasCloudPainter.coverOf(storm, Field.NONE, null, true), storm.name());
            assertEquals(Cover.STORM, AtlasCloudPainter.coverOf(storm, Field.NONE, null, false), storm.name());
        }
    }

    @Test
    void calmPlanetsGetCalmCloudsOnlyWhenThePlanetTypeHasThem() {
        assertEquals(Cover.CALM, AtlasCloudPainter.coverOf(Storm.NONE, Field.NONE, null, true));
        assertEquals(Cover.NONE, AtlasCloudPainter.coverOf(Storm.NONE, Field.NONE, null, false));
    }

    @Test
    void boltHolePlanetsStayClear() {
        assertEquals(Cover.NONE, AtlasCloudPainter.coverOf(Storm.NONE, Field.NONE, BOLT_HOLE_LIGHT, true));
        assertEquals(Cover.NONE, AtlasCloudPainter.coverOf(Storm.SUPERCELL, Field.NONE, BOLT_HOLE_LIGHT, true));
    }

    @Test
    void magneticFieldPlanetsStayClearEvenUnderAStorm() {
        for (Field field : EnumSet.complementOf(EnumSet.of(Field.NONE))) {
            assertEquals(Cover.NONE, AtlasCloudPainter.coverOf(Storm.NONE, field, null, true), field.name());
            assertEquals(Cover.NONE, AtlasCloudPainter.coverOf(Storm.GENERIC, field, null, true), field.name());
        }
    }
}
