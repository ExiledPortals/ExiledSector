package exiledsector.charts.atlas;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasRegionTest {

    private static final String RAT = "assortment_of_things";
    private static final AtlasUnlocks RAT_INSTALLED = new AtlasUnlocks(RAT::equals, Set.of());

    private static AtlasRegion ratRegion(String id) {
        return new AtlasRegion(id, 1f, 0, RAT, "", Map.of(), Map.of(), Map.of());
    }

    @Test
    void aRegionHiddenUntilApprovedNeverAppearsEvenWithItsModInstalled() {
        for (String hiddenId : AtlasRegion.HIDDEN_UNTIL_APPROVED) {
            AtlasRegion hidden = ratRegion(hiddenId);

            assertFalse(hidden.availableWith(RAT_INSTALLED), hiddenId);
            assertFalse(hidden.keptWith(RAT_INSTALLED), hiddenId);
        }
    }

    @Test
    void theAbyssalRiftStaysHiddenUntilApproved() {
        assertTrue(AtlasRegion.HIDDEN_UNTIL_APPROVED.contains("abyssal_rift"));
    }

    @Test
    void otherRegionsOfTheSameModStillAppear() {
        AtlasRegion shown = ratRegion("rift");

        assertTrue(shown.availableWith(RAT_INSTALLED));
        assertTrue(shown.keptWith(RAT_INSTALLED));
        assertFalse(shown.availableWith(AtlasUnlocks.NONE));
    }
}
