package exiledsector.charts.officer.reward;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarriedItemTest {

    @Test
    void tokensRoundTripWithAndWithoutAVariant() {
        CarriedItem plain = new CarriedItem(RewardKind.CRAFTING_ITEM, "exiledSector_augmentation_nanoswarm", null, 5);
        CarriedItem biome = new CarriedItem(RewardKind.BIOME_HULL_ITEM, "es_bridge.mk-2", "abyssal_rift", 1);

        assertEquals("crafting_item:5:exiledSector_augmentation_nanoswarm", plain.toToken());
        assertEquals("biome_hull_item:1:es_bridge.mk-2:abyssal_rift", biome.toToken());
        for (RewardKind kind : RewardKind.values()) {
            CarriedItem item = new CarriedItem(kind, "some_item", null, 3);
            assertEquals(item, CarriedItem.fromToken(item.toToken()));
        }
        assertEquals(plain, CarriedItem.fromToken(plain.toToken()));
        assertEquals(biome, CarriedItem.fromToken(biome.toToken()));
    }

    @Test
    void malformedTokensReadAsNothing() {
        for (String token : new String[]{null, "", "hull_item:1", "hull_item:1:a:b:c", "nothing:1:item", "hull_item:0:item", "hull_item:-1:item",
                "hull_item:x:item", "hull_item:1234567890:item", "hull_item:1:", "hull_item:1:bad id", "hull_item:1:item:", "hull_item:1:item:bad|biome"}) {
            assertNull(CarriedItem.fromToken(token), String.valueOf(token));
        }
    }

    @Test
    void unsafeIdsAndCountsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> CarriedItem.of(RewardKind.HULL_ITEM, "has:colon"));
        assertThrows(IllegalArgumentException.class, () -> new CarriedItem(RewardKind.HULL_ITEM, "item", "bad biome", 1));
        assertThrows(IllegalArgumentException.class, () -> new CarriedItem(RewardKind.HULL_ITEM, "item", null, 0));
        assertThrows(NullPointerException.class, () -> new CarriedItem(null, "item", null, 1));
    }

    @Test
    void stacksOfOneItemAddTheirCountsAndVariantsStayApart() {
        CarriedItem swarm = new CarriedItem(RewardKind.CRAFTING_ITEM, "swarm", null, 4);
        CarriedItem riftBridge = new CarriedItem(RewardKind.BIOME_HULL_ITEM, "bridge", "abyssal_rift", 1);
        CarriedItem beltBridge = new CarriedItem(RewardKind.BIOME_HULL_ITEM, "bridge", "storm_belt", 1);

        List<CarriedItem> stacked = CarriedItem.stack(List.of(swarm, riftBridge), List.of(swarm.withCount(3), beltBridge, riftBridge));

        assertEquals(List.of(swarm.withCount(7), riftBridge.withCount(2), beltBridge), stacked);
    }
}
