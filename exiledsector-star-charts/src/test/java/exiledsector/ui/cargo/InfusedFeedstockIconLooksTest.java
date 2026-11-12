package exiledsector.ui.cargo;

import exiledsector.items.InfusedFeedstock;
import exiledsector.items.Nanoswarm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfusedFeedstockIconLooksTest {

    @Test
    void infusedFeedstockAnimatesLikeTuningInItsOwnColour() {
        CargoIconLook tuning = CargoIconLooks.forMaterial(Nanoswarm.TUNING.itemId());
        for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
            CargoIconLook look = InfusedFeedstockIconLooks.INSTANCE.materialLook(feedstock.itemId());

            assertEquals(tuning.glow(), look.glow(), feedstock.name());
            assertEquals(tuning.swarm(), look.swarm(), feedstock.name());
            assertTrue(look.prismatic() && look.arcs() && look.heartbeat(), feedstock.name());
            assertEquals(feedstock.color(), look.color(), feedstock.name());
        }
    }

    @Test
    void otherMaterialsKeepTheirOwnLook() {
        assertNull(InfusedFeedstockIconLooks.INSTANCE.materialLook(Nanoswarm.TUNING.itemId()));
    }
}
