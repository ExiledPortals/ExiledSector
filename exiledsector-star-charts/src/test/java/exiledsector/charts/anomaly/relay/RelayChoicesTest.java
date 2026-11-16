package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.ExiledSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTables;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.items.CraftingDrops;
import exiledsector.charts.officer.reward.CraftingTier;
import exiledsector.charts.officer.reward.OfficerRewardTable;
import exiledsector.charts.officer.reward.RewardContext;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarMemory;
import exiledsector.charts.officer.scar.ScarTable;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.charts.run.ChartRun;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ModifierSources;
import exiledsector.items.PoolEntry;
import exiledsector.items.StarChartCipher;
import exiledsector.items.StarChartFixtures;
import exiledsector.loot.Loot;
import exiledsector.modifiers.ModifierSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelayChoicesTest {

    private static final HullFacts SHIELDED_WARSHIP = new HullFacts(true, true, false, false, true, false, false, false, false, false);
    private static final String SCARS_PATH = ExiledSector.configPath("atlas_officer_scars.csv");
    private static final String BRIDGE = "test_bridge";
    private static final List<RelayChoice> STORED_OFFERS = List.of(new RelayChoice(RewardGroup.LOW_CRAFTING, "kited"),
            new RelayChoice(RewardGroup.ENGRAM, "rammed"), new RelayChoice(RewardGroup.UNIQUE, "overloaded"));

    @BeforeEach
    void setUp() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        StarChartCipher.registerAll();
        CraftingDrops.register(ChartTables.csvRows(CraftingDrops.DATA_PATH));
        ScarTable.register(ChartTables.csvRows(SCARS_PATH));
    }

    @AfterEach
    void tearDown() {
        OfficerRewardTable.clear();
        CraftingDrops.clear();
        ScarTable.clear();
        StarChartFixtures.unregisterCraftingItems();
        ModifierSources.clear();
        ItemDefinitions.clear();
    }

    private static RewardContext context() {
        return new RewardContext(5, null, null, biomeId -> false, definition -> true, itemId -> true, ChartTiers.bandShift(5));
    }

    private static ChartRun wokenRun(String runId, RelayKind... woken) {
        ChartRun run = new ChartRun(runId, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(4, "pirates", "boss", 3, 2, List.of(RelayNetwork.ANOMALY_ID)));
        run.recordArrival();
        for (RelayKind kind : woken) {
            RelayStates.add(run, kind, RelayStates.WOKEN);
        }
        return run;
    }

    @Test
    void aRelayOffersTheSameThreeDistinctPairsFromItsSeedWithScarsFittingTheBossHull() {
        ChartRun run = wokenRun("run1", RelayKind.NAV_BUOY);

        List<RelayChoice> offers = RelayChoices.roll(context(), SHIELDED_WARSHIP, RelayChoices.offerRandom(run, RelayKind.NAV_BUOY));

        assertEquals(offers, RelayChoices.roll(context(), SHIELDED_WARSHIP, RelayChoices.offerRandom(run, RelayKind.NAV_BUOY)));
        assertEquals(3, offers.size());
        assertEquals(3, new HashSet<>(offers.stream().map(RelayChoice::group).toList()).size());
        assertEquals(3, new HashSet<>(offers.stream().map(RelayChoice::scarId).toList()).size());
        for (RelayChoice offer : offers) {
            assertFalse(ScarTable.row(offer.scarId()).fittingEffects(SHIELDED_WARSHIP).isEmpty(), offer.scarId());
        }
    }

    @Test
    void pairingNeverRepeatsAScar() {
        List<RelayChoice> paired = RelayChoices.paired(List.of(RewardGroup.LOW_CRAFTING, RewardGroup.ENGRAM, RewardGroup.UNIQUE),
                List.of("kited", "rammed"), new Random(3L));

        assertEquals(2, paired.size());
        assertEquals(Set.of("kited", "rammed"), new HashSet<>(paired.stream().map(RelayChoice::scarId).toList()));
    }

    @Test
    void storedOffersComeBackUnchangedAndAPickClosesTheRelaysChoice() {
        ChartRun run = wokenRun("run1", RelayKind.NAV_BUOY);
        RelayChoices.storeOffers(run, RelayKind.NAV_BUOY, STORED_OFFERS);

        assertEquals(STORED_OFFERS, RelayChoices.offers(null, run, RelayKind.NAV_BUOY));
        assertTrue(RelayChoices.pick(null, run, RelayKind.NAV_BUOY, 1));

        assertEquals(STORED_OFFERS.get(1), RelayChoices.picked(run, RelayKind.NAV_BUOY));
        assertFalse(RelayChoices.open(run, RelayKind.NAV_BUOY));
        assertEquals(List.of(), RelayChoices.offers(null, run, RelayKind.NAV_BUOY));
        assertFalse(RelayChoices.pick(null, run, RelayKind.NAV_BUOY, 0));
        assertEquals(List.of(STORED_OFFERS.get(1)), RelayChoices.picks(run));
    }

    @Test
    void noChoiceIsOpenForADormantRelayARunWithoutABossOrOnceTheBossIsDestroyed() {
        ChartRun run = wokenRun("run1", RelayKind.NAV_BUOY);
        assertTrue(RelayChoices.open(run, RelayKind.NAV_BUOY));
        assertFalse(RelayChoices.open(run, RelayKind.COMM_RELAY));

        ChartRun bossless = new ChartRun("run2", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        bossless.recordContent(new ChartContent(4, "pirates", null, 3, 2, List.of(RelayNetwork.ANOMALY_ID)));
        RelayStates.add(bossless, RelayKind.NAV_BUOY, RelayStates.WOKEN);
        assertFalse(RelayChoices.open(bossless, RelayKind.NAV_BUOY));

        run.markObjectiveComplete();
        assertFalse(RelayChoices.open(run, RelayKind.NAV_BUOY));
    }

    @Test
    void picksBelongOnlyToTheRunTheyWereMadeIn() {
        ChartRun run = wokenRun("run1", RelayKind.NAV_BUOY, RelayKind.SENSOR_ARRAY);
        RelayChoices.storeOffers(run, RelayKind.NAV_BUOY, STORED_OFFERS);
        RelayChoices.storeOffers(run, RelayKind.SENSOR_ARRAY, STORED_OFFERS);
        RelayChoices.pick(null, run, RelayKind.NAV_BUOY, 0);
        RelayChoices.pick(null, run, RelayKind.SENSOR_ARRAY, 0);

        ChartRun nextStage = wokenRun("run2", RelayKind.NAV_BUOY);

        assertEquals(Map.of("kited", 2), RelayChoices.scarStacks(RelayChoices.picks(run)));
        assertEquals(List.of(), RelayChoices.picks(nextStage));
        assertEquals(List.of(), RelayChoices.storedOffers(nextStage, RelayKind.NAV_BUOY));
        assertSame(Loot.NONE, RelayChoices.bossReward(null, nextStage));
    }

    @Test
    void pickingAChoiceScarsTheBossFlagshipAndAgainForEachPickOfTheSameScar() {
        ChartRun run = wokenRun("run1", RelayKind.NAV_BUOY, RelayKind.COMM_RELAY);
        MemoryAPI bossMemory = RelayFixtures.memory();
        FleetMemberAPI flagship = mock(FleetMemberAPI.class);
        ShipVariantAPI flagshipVariant = mock(ShipVariantAPI.class);
        when(flagship.getId()).thenReturn("boss-flagship");
        when(flagship.getVariant()).thenReturn(flagshipVariant);
        when(flagshipVariant.getSource()).thenReturn(VariantSource.REFIT);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(flagship));
        CampaignFleetAPI boss = mock(CampaignFleetAPI.class);
        when(boss.getId()).thenReturn("boss");
        when(boss.getMemoryWithoutUpdate()).thenReturn(bossMemory);
        when(boss.getFleetData()).thenReturn(fleetData);
        when(boss.getFlagship()).thenReturn(flagship);
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.getFleets()).thenReturn(List.of(boss));
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getStarSystem(run.systemId())).thenReturn(system);
        RelayChoices.storeOffers(run, RelayKind.NAV_BUOY, STORED_OFFERS);
        RelayChoices.storeOffers(run, RelayKind.COMM_RELAY, STORED_OFFERS);

        RelayChoices.pick(sector, run, RelayKind.NAV_BUOY, 0);
        assertEquals(Map.of("kited", 1), ScarMemory.scarsOn(bossMemory, "boss-flagship"));
        RelayChoices.pick(sector, run, RelayKind.COMM_RELAY, 0);

        assertEquals("boss-flagship", ScarMemory.runFlagshipId(bossMemory));
        assertNull(Scars.flagshipIdOf(boss));
        assertEquals(Map.of("kited", 2), ScarMemory.scarsOn(bossMemory, "boss-flagship"));
        assertEquals(Map.of(), Scars.scarsOf(boss));
        verify(flagshipVariant, atLeastOnce()).addMod(SkillTreeHullMod.ID);
    }

    @Test
    void theBossDropsAnItemOfEachPickedGroupAsAnOfficerRewardOfThatGroupDrops() {
        ItemDefinitions.register(List.of(new ItemDefinition(BRIDGE, ItemKind.BRIDGE, BRIDGE, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f)), List.of(), "")));
        ChartRun run = wokenRun("run1");
        List<RelayChoice> picks = List.of(new RelayChoice(RewardGroup.LOW_CRAFTING, "kited"), new RelayChoice(RewardGroup.FEEDSTOCK, "rammed"),
                new RelayChoice(RewardGroup.HULL_ITEM, "overloaded"));

        Loot loot = RelayChoices.bossReward(picks, context(), run, new Random(7L));

        assertEquals(loot.materials(), RelayChoices.bossReward(picks, context(), run, new Random(7L)).materials());
        assertEquals(2, loot.materials().size());
        assertTrue(loot.materials().keySet().stream().anyMatch(itemId -> OfficerRewardTable.categoryOf(itemId) == RewardKind.CRAFTING_ITEM
                && OfficerRewardTable.tierOf(itemId) == CraftingTier.LOW), loot.materials().toString());
        assertTrue(loot.materials().keySet().stream().anyMatch(itemId -> OfficerRewardTable.categoryOf(itemId) == RewardKind.FEEDSTOCK),
                loot.materials().toString());
        assertTrue(loot.materials().values().stream().allMatch(count -> count >= 1));
        assertEquals(List.of(BRIDGE), loot.items().stream().map(ItemData::definitionId).toList());
    }
}
