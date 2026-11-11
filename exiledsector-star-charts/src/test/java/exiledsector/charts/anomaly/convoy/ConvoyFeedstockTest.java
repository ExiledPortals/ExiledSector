package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.items.InfusedFeedstock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvoyFeedstockTest {

    private static final String THREAT = InfusedFeedstock.THREAT.itemId();
    private static final String SHROUD = InfusedFeedstock.SHROUD.itemId();
    private static final String PHASE = InfusedFeedstock.PHASE.itemId();

    private static JSONObject row(String feedstock, String lowestTier, String highestTier) throws Exception {
        return new JSONObject().put("feedstock", feedstock).put("tier_1", lowestTier).put("tier_15", highestTier);
    }

    private static void registerAll(String lowestTier, String highestTier) throws Exception {
        ConvoyFeedstock.register(new JSONArray().put(row(THREAT, lowestTier, highestTier)).put(row(SHROUD, lowestTier, highestTier))
                .put(row(PHASE, lowestTier, highestTier)));
    }

    @AfterEach
    void tearDown() {
        ConvoyFeedstock.clear();
    }

    @Test
    void theSharedAmountRisesLinearlyFromTheLowestToTheHighestTier() throws Exception {
        registerAll("150", "1000");

        assertEquals(150f, ConvoyFeedstock.sharedAmount(THREAT, 1), 0.01f);
        assertEquals(575f, ConvoyFeedstock.sharedAmount(SHROUD, 8), 0.01f);
        assertEquals(1000f, ConvoyFeedstock.sharedAmount(PHASE, 15), 0.01f);
        assertEquals(150f, ConvoyFeedstock.sharedAmount(THREAT, 0), 0.01f, "tiers outside the table clamp to its ends");
        assertEquals(ConvoyFeedstock.sharedAmount(THREAT, ChartTiers.CORRUPTED_TIER), ConvoyFeedstock.sharedAmount(THREAT, 20), 0.01f);
    }

    @Test
    void onlyInfusedFeedstockRowsLoad() throws Exception {
        List<String> problems = ConvoyFeedstock.register(new JSONArray().put(row(THREAT, "150", "1000"))
                .put(row("exiledSector_nanite_feedstock", "150", "1000")));

        assertEquals(1, problems.size());
        assertEquals(0f, ConvoyFeedstock.sharedAmount("exiledSector_nanite_feedstock", 1), 0.001f);
        assertEquals(Set.of(THREAT), ConvoyFeedstock.roll(1, 1f, new Random(1L)).keySet());
    }

    @Test
    void eachFeedstockRollsWithinFifteenPercentOfTheSharedAmountAndScalesWithTheQuantityLines() throws Exception {
        registerAll("150", "1000");
        boolean anyUneven = false;
        for (long seed = 0; seed < 200; seed++) {
            Map<String, Float> rolled = ConvoyFeedstock.roll(8, 2f, new Random(seed));
            assertEquals(Set.of(THREAT, SHROUD, PHASE), rolled.keySet());
            for (float amount : rolled.values()) {
                assertTrue(amount >= 575f * 2f * (1f - ConvoyFeedstock.SPREAD) - 0.01f, "rolled " + amount);
                assertTrue(amount <= 575f * 2f * (1f + ConvoyFeedstock.SPREAD) + 0.01f, "rolled " + amount);
            }
            anyUneven |= new HashSet<>(rolled.values()).size() == 3;
        }
        assertTrue(anyUneven, "the three feedstocks roll independently");
        assertEquals(0f, ConvoyFeedstock.roll(8, 0f, new Random(1L)).get(THREAT), 0.001f);
    }

    @Test
    void thePodsTakeTheLostShareAndTheCacheKeepsEightyPercentOfTheDeliveredShare() {
        ConvoyState convoy = new ConvoyState(0, List.of(), "dest", 0f, 0f, 0f, 0f, 1L);
        convoy.loadCargo(100f, 3f, 1f);
        convoy.loadFeedstock(Map.of(THREAT, 200f));
        convoy.setPhase(ConvoyState.Phase.EN_ROUTE);

        float lostShare = convoy.recordFreighterPoints(25f);
        CargoAPI pods = mock(CargoAPI.class);
        ConvoyCargo.withFeedstock(pods, convoy, lostShare * ConvoyRules.POD_SHARE, new Random(1L));
        verify(pods).addSpecial(argThat(data -> isItem(data, THREAT)), eq(150f));

        convoy.deliver();
        CargoAPI cache = mock(CargoAPI.class);
        ConvoyCargo.withFeedstock(cache, convoy, convoy.deliveredShare(), new Random(1L));
        verify(cache).addSpecial(argThat(data -> isItem(data, THREAT)), eq(50f));

        CargoStackAPI stack = mock(CargoStackAPI.class);
        SpecialItemData threat = new SpecialItemData(THREAT, null);
        when(stack.getType()).thenReturn(CargoAPI.CargoItemType.SPECIAL);
        when(stack.getData()).thenReturn(threat);
        when(stack.getSize()).thenReturn(50f);
        when(cache.getStacksCopy()).thenReturn(List.of(stack));
        ConvoyCargo.keepShare(cache, ConvoyRules.DELIVERED_SHARE, new Random(1L));
        verify(cache).removeItems(CargoAPI.CargoItemType.SPECIAL, threat, 10f);
    }

    @Test
    void aConvoyFromBeforeTheFeedstockCarriesNone() {
        ConvoyState convoy = new ConvoyState(0, List.of(), "dest", 0f, 0f, 0f, 0f, 1L);

        assertEquals(Map.of(), convoy.feedstock());
        CargoAPI cargo = mock(CargoAPI.class);
        ConvoyCargo.withFeedstock(cargo, convoy, 1f, new Random(1L));
        verify(cargo).sort();
    }

    @Test
    void theShippedTableCoversEveryFeedstockWithASpecialItemRowAndShowsInTheManifest() throws Exception {
        assertEquals(List.of(), ConvoyFeedstock.register(ChartTables.csvRows(ConvoyFeedstock.PATH)));

        JSONArray specialItems = ChartTables.csvRows("data/campaign/special_items.csv");
        Set<String> specialItemIds = new HashSet<>();
        for (int index = 0; index < specialItems.length(); index++) {
            specialItemIds.add(specialItems.getJSONObject(index).optString("id"));
        }
        for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
            assertNotEquals(0f, ConvoyFeedstock.sharedAmount(feedstock.itemId(), 1), feedstock.itemId() + " needs a convoy_feedstock.csv row");
            assertTrue(specialItemIds.contains(feedstock.itemId()), feedstock.itemId() + " needs a special_items.csv row");
        }
        assertTrue(ConvoyCargo.manifestKeys(4).contains(ConvoyCargo.CATEGORY_KEY_PREFIX + "feedstock"));
    }

    private static boolean isItem(SpecialItemData data, String itemId) {
        return data != null && itemId.equals(data.getId());
    }
}
