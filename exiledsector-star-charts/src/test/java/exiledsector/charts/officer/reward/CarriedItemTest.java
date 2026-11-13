package exiledsector.charts.officer.reward;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void joinedRewardsKeepEveryRepeatAsItsOwnEntry() {
        CarriedItem swarm = new CarriedItem(RewardKind.CRAFTING_ITEM, "swarm", null, 4);
        CarriedItem riftBridge = new CarriedItem(RewardKind.BIOME_HULL_ITEM, "bridge", "abyssal_rift", 1);
        CarriedItem beltBridge = new CarriedItem(RewardKind.BIOME_HULL_ITEM, "bridge", "storm_belt", 1);

        List<CarriedItem> joined = CarriedItem.joined(List.of(swarm, riftBridge), List.of(swarm.withCount(3), beltBridge, riftBridge));

        assertEquals(List.of(swarm, riftBridge, swarm.withCount(3), beltBridge, riftBridge), joined);
        assertTrue(swarm.sameItemAs(swarm.withCount(3)));
        assertFalse(riftBridge.sameItemAs(beltBridge));
    }

    @Test
    void aCraftingStackIsOneEntryWhileAnItemStackCountsEachItem() {
        assertEquals(1, new CarriedItem(RewardKind.CRAFTING_ITEM, "swarm", null, 5).entries());
        assertEquals(1, new CarriedItem(RewardKind.FEEDSTOCK, "exiledSector_nanite_feedstock", null, 60).entries());
        assertEquals(2, new CarriedItem(RewardKind.HULL_ITEM, "bridge", null, 2).entries());
        assertEquals(1, CarriedItem.of(RewardKind.STAR_CHART, "chart").entries());
    }

    @Test
    void tokensSavedBeforeRewardGroupsStillDecode() {
        assertEquals(new CarriedItem(RewardKind.CRAFTING_ITEM, "exiledSector_kernel_nanoswarm", null, 7),
                CarriedItem.fromToken("crafting_item:7:exiledSector_kernel_nanoswarm"));
        assertEquals(new CarriedItem(RewardKind.HULL_ITEM, "es_bridge.mk-2", null, 2), CarriedItem.fromToken("hull_item:2:es_bridge.mk-2"));
        assertEquals(new CarriedItem(RewardKind.BIOME_HULL_ITEM, "es_bridge.mk-2", "abyssal_rift", 1),
                CarriedItem.fromToken("biome_hull_item:1:es_bridge.mk-2:abyssal_rift"));
        assertEquals(CarriedItem.of(RewardKind.INHIBITOR, "inhibitor_a"), CarriedItem.fromToken("inhibitor:1:inhibitor_a"));
        assertEquals(CarriedItem.of(RewardKind.ENGRAM, "engram_a"), CarriedItem.fromToken("engram:1:engram_a"));
        assertEquals(CarriedItem.of(RewardKind.UNIQUE, "u_x"), CarriedItem.fromToken("unique:1:u_x"));
        assertNull(CarriedItem.fromToken("subroutine:1:old_subroutine"));
    }

    @Test
    void theNewKindsRoundTrip() {
        CarriedItem feedstock = new CarriedItem(RewardKind.FEEDSTOCK, "exiledSector_nanite_feedstock", null, 45);
        CarriedItem chart = CarriedItem.of(RewardKind.STAR_CHART, "chart_tier_5");

        assertEquals("feedstock:45:exiledSector_nanite_feedstock", feedstock.toToken());
        assertEquals("star_chart:1:chart_tier_5", chart.toToken());
        assertEquals(feedstock, CarriedItem.fromToken(feedstock.toToken()));
        assertEquals(chart, CarriedItem.fromToken(chart.toToken()));
    }
}
