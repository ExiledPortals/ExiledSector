package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.BattleLootRates;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DepotLootTest {

    private static final float EPSILON = 1e-4f;
    private static final ChartGenContext HUNTING_PARTY = ChartGenContext.of(List.of(
            new AppliedEffect(ChartModifier.CHART_LISTENING_POST_HUNTING_PICKETS, 1f),
            new AppliedEffect(ChartModifier.CHART_LISTENING_POST_PICKET_PROTOTYPES, 1f)));

    @AfterEach
    void tearDown() {
        BattleLootRates.clear();
        ChartContentGenerator.resetForTests();
    }

    @Test
    void picketsAndTheGuardDropPrototypesOnlyWithTheHuntingPartyLine() {
        try (MockSector ignored = new MockSector()) {
            PostSystem postSystem = new PostSystem();
            PostSystem.placeIn(postSystem.site(HUNTING_PARTY, new Vector2f(9000f, 0f), 4L),
                    new ChartRun(PostSystem.RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1));
            CampaignFleetAPI picket = postSystem.pickets().get(0);
            CampaignFleetAPI stranger = FakeChartContentEngine.offeringNothing().createFleet(null);

            assertTrue(PicketPrototypes.dropsFrom(picket, HUNTING_PARTY));
            assertFalse(PicketPrototypes.dropsFrom(picket, ChartGenContext.none()));
            assertFalse(PicketPrototypes.dropsFrom(stranger, HUNTING_PARTY));
            assertFalse(PicketPrototypes.dropsFrom(mock(CampaignFleetAPI.class), HUNTING_PARTY));
        }
    }

    @Test
    void picketPrototypesFollowTheirBattleLootRate() throws Exception {
        assertEquals(0f, PicketPrototypes.expected(HUNTING_PARTY, 100f, 100f, 1f));

        BattleLootRates.register(new JSONArray().put(new JSONObject().put("drop", ListeningPostRules.PICKET_PROTOTYPE_RATE)
                .put("per_100_dp", "0.12").put("curve", "linear").put("difficulty_scaling", "1")));

        assertEquals(0.12f, PicketPrototypes.expected(HUNTING_PARTY, 100f, 100f, 1f), EPSILON);
        assertEquals(0.24f, PicketPrototypes.expected(HUNTING_PARTY, 200f, 200f, 1f), EPSILON);
        assertEquals(0.24f, PicketPrototypes.expected(HUNTING_PARTY, 100f, 100f, 2f), EPSILON);
        assertEquals(0f, PicketPrototypes.expected(HUNTING_PARTY, 0f, 100f, 1f));
    }

    @Test
    void theLedgerPicksDistinctRewardsUpToItsCount() {
        List<CarriedItem> rewards = List.of(CarriedItem.of(RewardKind.HULL_ITEM, "a"), CarriedItem.of(RewardKind.HULL_ITEM, "b"),
                CarriedItem.of(RewardKind.CRAFTING_ITEM, "c"));

        for (long seed = 0; seed < 20; seed++) {
            List<CarriedItem> one = VaultExtras.ledgerPicks(rewards, 1, new Random(seed));
            assertEquals(1, one.size());
            assertTrue(rewards.contains(one.get(0)));
            List<CarriedItem> all = VaultExtras.ledgerPicks(rewards, 5, new Random(seed));
            assertEquals(rewards.size(), new HashSet<>(all).size());
        }
        assertTrue(VaultExtras.ledgerPicks(rewards, 0, new Random(1L)).isEmpty());
        assertTrue(VaultExtras.ledgerPicks(List.of(), 1, new Random(1L)).isEmpty());
    }

    @Test
    void aRunOutsideTheAtlasHasNoOfficerTerritoryToCopy() {
        try (MockSector mockSector = new MockSector()) {
            ChartRun run = new ChartRun(PostSystem.RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);

            assertTrue(VaultExtras.territoryRewards(mockSector.sector, run).isEmpty());
            assertTrue(VaultExtras.territoryRewards(mockSector.sector, null).isEmpty());
        }
    }
}
