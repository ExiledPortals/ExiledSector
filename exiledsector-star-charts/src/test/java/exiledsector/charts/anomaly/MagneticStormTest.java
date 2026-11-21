package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.PlanetAPI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagneticStormTest {

    private static PlanetAPI star(boolean blackHole) {
        PlanetAPI star = mock(PlanetAPI.class);
        when(star.isBlackHole()).thenReturn(blackHole);
        return star;
    }

    @Test
    void theStarFieldRingsTheFirstStarThatIsNotABlackHole() {
        PlanetAPI blackHole = star(true);
        PlanetAPI companion = star(false);

        assertSame(companion, MagneticStorm.fieldStar(List.of(blackHole, companion)));
    }

    @Test
    void aBlackHoleOrAnEmptyCentreGetsNoStarField() {
        assertNull(MagneticStorm.fieldStar(List.of(star(true))));
        assertNull(MagneticStorm.fieldStar(List.of()));
    }
}
