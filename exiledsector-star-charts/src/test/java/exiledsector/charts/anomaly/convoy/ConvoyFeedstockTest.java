package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.charts.ChartTables;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.items.InfusedFeedstock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
        assertEquals(ConvoyFeedstock.sharedAmount(THREAT, ChartTiers.HIGHEST_TIER), ConvoyFeedstock.sharedAmount(THREAT, 20), 0.01f);
    }

    @Test
    void tierSixteenCarriesMoreThanTierFifteenUnlessItsOwnColumnSaysOtherwise() throws Exception {
        registerAll("150", "1000");
        assertTrue(ConvoyFeedstock.sharedAmount(THREAT, ChartTiers.HIGHEST_TIER) > ConvoyFeedstock.sharedAmount(THREAT, ChartTiers.HIGHEST_DROP_TIER));
        assertEquals(150f + 850f / 14f * 15f, ConvoyFeedstock.sharedAmount(THREAT, ChartTiers.HIGHEST_TIER), 0.01f);

        ConvoyFeedstock.register(new JSONArray().put(row(THREAT, "150", "1000").put("tier_16", "2000")));
        assertEquals(1000f, ConvoyFeedstock.sharedAmount(THREAT, ChartTiers.HIGHEST_DROP_TIER), 0.01f);
        assertEquals(2000f, ConvoyFeedstock.sharedAmount(THREAT, ChartTiers.HIGHEST_TIER), 0.01f);
    }

    @Test
    void theShippedTableRisesFromTierFifteenToTierSixteen() throws Exception {
        ConvoyFeedstock.register(ChartTables.csvRows(ConvoyFeedstock.PATH));
        for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
            assertTrue(ConvoyFeedstock.sharedAmount(feedstock.itemId(), ChartTiers.HIGHEST_TIER)
                    > ConvoyFeedstock.sharedAmount(feedstock.itemId(), ChartTiers.HIGHEST_DROP_TIER), feedstock.itemId());
        }
    }

    @Test
    void onlyInfusedFeedstockRowsLoad() throws Exception {
        List<String> problems = ConvoyFeedstock.register(new JSONArray().put(row(THREAT, "150", "1000"))
                .put(row("exiledSector_nanite_feedstock", "150", "1000")));

        assertEquals(1, problems.size());
        assertEquals(0f, ConvoyFeedstock.sharedAmount("exiledSector_nanite_feedstock", 1), 0.001f);
        assertEquals(Set.of(THREAT), ConvoyFeedstock.roll(1, 1f, FeedstockOdds.EVEN, new Random(1L)).keySet());
    }

    @Test
    void aConvoyCarriesOneKindHoldingThreeSharedAmountsWithinFifteenPercentScaledByTheQuantityLines() throws Exception {
        registerAll("150", "1000");
        Map<String, Integer> kindsRolled = new HashMap<>();
        Random random = new Random(1L);
        for (int roll = 0; roll < 300; roll++) {
            Map<String, Float> rolled = ConvoyFeedstock.roll(8, 2f, FeedstockOdds.EVEN, random);
            assertEquals(1, rolled.size());
            float amount = rolled.values().iterator().next();
            assertTrue(amount >= 3f * 575f * 2f * (1f - ConvoyFeedstock.SPREAD) - 0.01f, "rolled " + amount);
            assertTrue(amount <= 3f * 575f * 2f * (1f + ConvoyFeedstock.SPREAD) + 0.01f, "rolled " + amount);
            kindsRolled.merge(rolled.keySet().iterator().next(), 1, Integer::sum);
        }
        assertEquals(Set.of(THREAT, SHROUD, PHASE), kindsRolled.keySet());
        kindsRolled.values().forEach(count -> assertTrue(count > 70, "each kind is about as likely, rolled " + kindsRolled));
        assertEquals(0f, ConvoyFeedstock.roll(8, 0f, FeedstockOdds.EVEN, new Random(1L)).values().iterator().next(), 0.001f);
    }

    @Test
    void reducingAKindsChanceMakesTheOthersLikelierAndReducingAllEquallyChangesNothing() throws Exception {
        registerAll("150", "1000");
        FeedstockOdds noThreat = new FeedstockOdds(Map.of(InfusedFeedstock.THREAT, 100f), false, 0f);
        FeedstockOdds onlyPhase = new FeedstockOdds(Map.of(InfusedFeedstock.THREAT, 100f, InfusedFeedstock.SHROUD, 100f), false, 0f);
        FeedstockOdds allReduced = new FeedstockOdds(Map.of(InfusedFeedstock.THREAT, 25f, InfusedFeedstock.SHROUD, 25f, InfusedFeedstock.PHASE, 25f), false, 0f);
        for (long seed = 0; seed < 200; seed++) {
            assertTrue(!ConvoyFeedstock.roll(8, 1f, noThreat, new Random(seed)).containsKey(THREAT));
            assertEquals(Set.of(PHASE), ConvoyFeedstock.roll(8, 1f, onlyPhase, new Random(seed)).keySet());
            assertEquals(ConvoyFeedstock.roll(8, 1f, FeedstockOdds.EVEN, new Random(seed)), ConvoyFeedstock.roll(8, 1f, allReduced, new Random(seed)));
        }
        assertEquals(0.75f, new FeedstockOdds(Map.of(InfusedFeedstock.SHROUD, 25f), false, 0f).weight(InfusedFeedstock.SHROUD), 0.001f);
        assertEquals(1.1f, new FeedstockOdds(Map.of(InfusedFeedstock.SHROUD, -10f), false, 0f).weight(InfusedFeedstock.SHROUD), 0.001f);
    }

    @Test
    void whenEveryKindIsRuledOutTheKindIsPickedEvenly() throws Exception {
        registerAll("150", "1000");
        FeedstockOdds none = new FeedstockOdds(Map.of(InfusedFeedstock.THREAT, 100f, InfusedFeedstock.SHROUD, 100f, InfusedFeedstock.PHASE, 100f), false, 0f);
        Set<String> kinds = new HashSet<>();
        Random random = new Random(1L);
        for (int roll = 0; roll < 100; roll++) {
            kinds.addAll(ConvoyFeedstock.roll(8, 1f, none, random).keySet());
        }
        assertEquals(Set.of(THREAT, SHROUD, PHASE), kinds);
    }

    @Test
    void aMixedManifestSplitsTheTotalEquallyOverAllThreeKindsAndIgnoresTheChanceLines() throws Exception {
        registerAll("150", "1000");
        FeedstockOdds mixed = new FeedstockOdds(Map.of(InfusedFeedstock.THREAT, 100f), true, 0f);
        Map<String, Float> rolled = ConvoyFeedstock.roll(8, 1f, mixed, new Random(3L));
        float single = ConvoyFeedstock.roll(8, 1f, FeedstockOdds.EVEN, new Random(3L)).values().iterator().next();

        assertEquals(Set.of(THREAT, SHROUD, PHASE), rolled.keySet());
        rolled.values().forEach(amount -> assertEquals(rolled.get(THREAT), amount, 0.001f));
        assertEquals(3f * 575f, ConvoyFeedstock.total(rolled), 3f * 575f * ConvoyFeedstock.SPREAD + 0.01f);
        assertTrue(single > 0f);
    }

    @Test
    void aDuplicationRollDoublesTheFeedstock() throws Exception {
        registerAll("150", "1000");
        for (long seed = 0; seed < 50; seed++) {
            Map<String, Float> plain = ConvoyFeedstock.roll(8, 1f, FeedstockOdds.EVEN, new Random(seed));
            Map<String, Float> doubled = ConvoyFeedstock.roll(8, 1f, new FeedstockOdds(Map.of(), false, 1f), new Random(seed));
            assertEquals(plain.keySet(), doubled.keySet());
            assertEquals(2f * ConvoyFeedstock.total(plain), ConvoyFeedstock.total(doubled), 0.01f);
        }
    }

    @Test
    void theOddsReadTheChartsLines() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CONVOY_SHROUD_FEEDSTOCK_CHANCE_PERCENT, 35f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CONVOY_DOUBLE_FEEDSTOCK_CHANCE_PERCENT, 25f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CONVOY_MIXED_MANIFEST, 1f)));
        FeedstockOdds odds = FeedstockOdds.of(context);

        assertEquals(0.65f, odds.weight(InfusedFeedstock.SHROUD), 0.001f);
        assertEquals(1f, odds.weight(InfusedFeedstock.THREAT), 0.001f);
        assertEquals(0.25f, odds.doubleChance(), 0.001f);
        assertTrue(odds.mixed());
        assertEquals(FeedstockOdds.EVEN.mixed(), FeedstockOdds.of(null).mixed());
        assertEquals(1f, FeedstockOdds.of(null).weight(InfusedFeedstock.PHASE), 0.001f);
    }

    @Test
    void thePodsTakeTheLostShareAndTheCacheKeepsEightyPercentOfTheDeliveredShare() {
        ConvoyState convoy = new ConvoyState(0, List.of(), "dest", 0f, 0f, 0f, 0f, 1L);
        convoy.loadCargo(100f);
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
        assertTrue(ConvoyCargo.manifestKeys(4, Set.of(SHROUD)).contains(ConvoyCargo.CATEGORY_KEY_PREFIX + "feedstock.shroud"));
        assertTrue(ConvoyCargo.manifestKeys(4, Set.of(SHROUD, PHASE)).contains(ConvoyCargo.CATEGORY_KEY_PREFIX + "feedstock"));
    }

    private static boolean isItem(SpecialItemData data, String itemId) {
        return data != null && itemId.equals(data.getId());
    }
}
