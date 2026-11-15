package exiledsector.ui.cargo;

import exiledsector.items.InfusedFeedstock;
import exiledsector.items.MaterialTier;
import exiledsector.items.Nanoswarm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfusedFeedstockIconLooksTest {

    @Test
    void infusedFeedstockAnimatesAsARareMaterialInItsOwnColour() {
        CargoIconLook rare = CargoIconLooks.forMaterial(MaterialTier.RARE, InfusedFeedstock.THREAT.color());
        for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
            CargoIconLook look = InfusedFeedstockIconLooks.INSTANCE.materialLook(feedstock.itemId());

            assertEquals(rare.glow(), look.glow(), feedstock.name());
            assertEquals(SwarmMotion.ORBIT, look.swarm(), feedstock.name());
            assertTrue(look.rings() && !look.arcs(), feedstock.name());
            assertEquals(feedstock.color(), look.color(), feedstock.name());
        }
    }

    @Test
    void otherMaterialsKeepTheirOwnLook() {
        assertNull(InfusedFeedstockIconLooks.INSTANCE.materialLook(Nanoswarm.TUNING.itemId()));
    }
}
