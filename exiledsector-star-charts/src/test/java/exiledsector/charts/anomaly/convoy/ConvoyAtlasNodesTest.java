package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.FlagshipCatalogue;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.items.InfusedFeedstock;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyAtlasNodesTest {

    private static final String THREAT = InfusedFeedstock.THREAT.itemId();
    private static final String PHASE = InfusedFeedstock.PHASE.itemId();
    private static final long BOARD_SEED = 0x5C1A7L;
    private static final List<OfficerRank> RANKS = List.of(new OfficerRank("lieutenant", 10, 0, 1, 3));
    private static final FlagshipCatalogue CATALOGUE = (factionId, rankOrder, rankCount, random) ->
            new FlagshipCatalogue.Flagship("buffalo", "buffalo_Standard");

    private static ChartGenContext lines(ChartModifier effect, float magnitude) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(effect, magnitude)));
    }

    private static ConvoyState waitingConvoy(int index, String destinationId, List<String> waypointIds) {
        return new ConvoyState(index, waypointIds, destinationId, 0f, 0f, 0f, 0f, index);
    }

    @Test
    void theAdditionalConvoyChanceAddsAConvoyOnASuccessfulRollStillCappedAtTheMaximum() {
        Random random = new Random(1L);
        int extra = 0;
        for (int roll = 0; roll < 2000; roll++) {
            extra += ConvoyRules.convoyCount(0f, 30f, random) - 1;
        }
        assertEquals(600, extra, 80);
        assertEquals(1, ConvoyRules.convoyCount(0f, 0f, new Random(1L)));
        assertEquals(2, ConvoyRules.convoyCount(0f, 100f, new Random(1L)));
        assertEquals(3, ConvoyRules.convoyCount(1f, 100f, new Random(1L)));
        assertEquals(ConvoyRules.MAX_CONVOYS, ConvoyRules.convoyCount(2f, 100f, new Random(1L)));
        assertEquals(1, ConvoyRules.convoyCount(0f, -50f, new Random(1L)));
    }

    @Test
    void theKeystonesFleetSizeAddsToTheEscortSize() {
        assertEquals(1f, ConvoyRules.escortMult(0f, 0f), 0.001f);
        assertEquals(1.2f, ConvoyRules.escortMult(0f, 20f), 0.001f);
        assertEquals(0.7f, ConvoyRules.escortMult(-50f, 20f), 0.001f);
        assertEquals(0f, ConvoyRules.escortMult(-200f, 0f), 0.001f);
    }

    @Test
    void feedstockQuantityLinesAddToTheCraftingItemQuantity() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 50f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CONVOY_FEEDSTOCK_PERCENT, 20f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CONVOY_BREACH_FEEDSTOCK_PERCENT, 50f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CONVOY_EXCAVATION_LOOT_PERCENT, 30f)));

        assertEquals(1.7f, FeedstockBonus.rollMult(context), 0.001f);
        assertEquals(2.2f / 1.7f, FeedstockBonus.breachShare(context), 0.001f);
        assertEquals(2.0f / 1.7f, FeedstockBonus.excavationShare(context), 0.001f);
        assertEquals(1f, FeedstockBonus.rollMult(null), 0.001f);
        assertEquals(1f, FeedstockBonus.breachShare(ChartGenContext.none()), 0.001f);
    }

    @Test
    void theFlagLinesTurnTheirMechanicsOn() {
        assertTrue(ConvoyFleets.huntsFromTheStart(lines(ChartModifier.CHART_HOSTILE_CONVOYS, 1f)));
        assertFalse(ConvoyFleets.huntsFromTheStart(ChartGenContext.none()));
        assertTrue(ConvoyBoltHole.isOn(lines(ChartModifier.CHART_CONVOY_EXCAVATION_DELIVERY, 1f)));
        assertFalse(ConvoyBoltHole.isOn(null));
        assertTrue(ConvoySupplyLines.isOn(lines(ChartModifier.CHART_CONVOY_OFFICER_FEEDSTOCK, 1f)));
        assertFalse(ConvoySupplyLines.isOn(ChartGenContext.none()));
    }

    @Test
    void boltHoleDropSpreadsWaitingConvoysOverTheExcavationSitesAndLeavesTheRestAlone() {
        ConvoyState first = waitingConvoy(0, "planet_a", List.of("site_1", "planet_x"));
        ConvoyState second = waitingConvoy(1, "planet_b", List.of());
        ConvoyState third = waitingConvoy(2, "planet_c", List.of());
        ConvoyState alreadyThere = waitingConvoy(3, "site_2", List.of());
        ConvoyState departed = waitingConvoy(4, "planet_d", List.of());
        departed.setPhase(ConvoyState.Phase.EN_ROUTE);

        ConvoyBoltHole.reroute(List.of(first, second, third, alreadyThere, departed), List.of("site_1", "site_2"));

        assertEquals("site_1", first.destinationId());
        assertEquals(List.of("planet_x"), first.waypointIds(), "the new destination is no longer a stop on the way");
        assertEquals("site_1", second.destinationId());
        assertEquals("site_2", third.destinationId());
        assertEquals("site_2", alreadyThere.destinationId());
        assertEquals("planet_d", departed.destinationId());

        ConvoyState noSites = waitingConvoy(5, "planet_e", List.of());
        ConvoyBoltHole.reroute(List.of(noSites), List.of());
        assertEquals("planet_e", noSites.destinationId());
    }

    @Test
    void supplyLinesCarriesTheConvoysFeedstockAsWholeCounts() {
        Map<String, Float> feedstock = new LinkedHashMap<>();
        feedstock.put(THREAT, 1234.6f);
        feedstock.put(PHASE, 0.2f);

        assertEquals(List.of(new CarriedItem(RewardKind.FEEDSTOCK, THREAT, null, 1235)), ConvoySupplyLines.carriedFeedstock(feedstock));
        assertEquals(List.of(), ConvoySupplyLines.carriedFeedstock(Map.of()));
    }

    @Test
    void supplyLinesHandsTheFeedstockToARandomActiveOfficer() {
        AtlasState state = new AtlasState(BOARD_SEED);
        List<AtlasOfficer> board = OfficerBoardGenerator.generate(BOARD_SEED, RANKS, Map.of("hegemony", 1f), CATALOGUE);
        board.forEach(state::putOfficer);
        ConvoyState convoy = waitingConvoy(0, "planet_a", List.of());
        convoy.loadFeedstock(Map.of(THREAT, 500f));

        assertNull(ConvoySupplyLines.handOver(state, convoy, new Random(1L)), "an atlas with no officer in command gets nothing");

        AtlasOfficer commanding = state.officers().get(0).takingCommand(new AtlasCell(3, 2), "");
        state.putOfficer(commanding);
        AtlasOfficer supplied = ConvoySupplyLines.handOver(state, convoy, new Random(1L));

        assertNotNull(supplied);
        assertEquals(commanding.recordId(), supplied.recordId());
        CarriedItem carried = state.officer(commanding.recordId()).carried().get(state.officer(commanding.recordId()).carried().size() - 1);
        assertEquals(new CarriedItem(RewardKind.FEEDSTOCK, THREAT, null, 500), carried);
        assertEquals(commanding.carried().size() + 1, state.officer(commanding.recordId()).carried().size());
        assertNull(ConvoySupplyLines.handOver(null, convoy, new Random(1L)));
        assertNull(ConvoySupplyLines.handOver(state, waitingConvoy(1, "planet_b", List.of()), new Random(1L)));
    }
}
