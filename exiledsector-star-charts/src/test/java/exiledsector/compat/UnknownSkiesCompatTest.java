package exiledsector.compat;

import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnknownSkiesCompatTest {

    @Test
    void backgroundAgesFollowUnknownSkiesNaming() {
        assertTrue(UnknownSkiesCompat.fits("ALL", StarAge.OLD));
        assertTrue(UnknownSkiesCompat.fits("YOUNGAVERAGE", StarAge.AVERAGE));
        assertTrue(UnknownSkiesCompat.fits("AVERAGEOLD", StarAge.OLD));
        assertFalse(UnknownSkiesCompat.fits("YOUNG", StarAge.OLD));
    }

    @Test
    void withoutTheModThereAreNoExtraBackgrounds() {
        assertEquals(java.util.List.of(), UnknownSkiesCompat.backgrounds(StarAge.YOUNG, false));
    }
}
