package exiledsector.charts;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.skills.npc.NpcTreeSize;
import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartContentDataTest {

    @AfterEach
    void tearDown() {
        ChartTiers.resetForTests();
        ChartPois.resetForTests();
        ChartFactions.resetForTests();
    }

    static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    static void loadShippedTables() throws Exception {
        ChartTiers.register(csvRows(ChartTiers.PATH));
        ChartPois.register(csvRows(ChartPois.PATH));
        ChartFactions.register(csvRows(ChartFactions.PATH));
    }

    @Test
    void theShippedTierTableCoversEveryTierAndGetsHarderEachTier() throws Exception {
        ChartTiers.register(csvRows(ChartTiers.PATH));

        ChartTier previous = null;
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier current = ChartTiers.forTier(tier);
            assertEquals(tier, current.tier());
            assertNotEquals(ChartTier.fallback(tier), current, "tier " + tier + " should come from the CSV");
            if (previous != null) {
                assertTrue(current.fleetSize() > previous.fleetSize(), "fleet size rises at tier " + tier);
                assertTrue(current.lootBudget() >= previous.lootBudget(), "loot budget never drops at tier " + tier);
                assertTrue(current.fleetStrengthMax() >= previous.fleetStrengthMax(), "fleet strength never drops at tier " + tier);
            }
            assertTrue(current.fleetStrengthMax() <= NpcTreeSize.HIGHEST_PERCENT);
            assertTrue(current.fleetSize() <= ChartContentPlanner.FLEET_SIZE_CAP, "tier " + tier + " fleets fit under the size cap");
            assertTrue(current.wardenSize() <= ChartContentPlanner.WARDEN_SIZE_CAP, "tier " + tier + " warden fits under the size cap");
            previous = current;
        }
        assertFalse(ChartTiers.forTier(1).boss());
        assertEquals(0f, ChartTiers.forTier(1).weightFor(ChartObjective.VAULT));
        assertTrue(ChartTiers.forTier(15).boss());
        assertTrue(ChartTiers.forTier(15).wardenSize() > 0f);
    }

    @Test
    void badTierRowsFallBackPerValueAndOutOfRangeTiersAreSkipped() throws Exception {
        ChartTiers.register(new JSONArray("[{\"tier\": \"2\", \"fleet_size\": \"lots\", \"fleets_min\": \"3\", \"boss\": \"maybe\"},"
                + " {\"tier\": \"99\", \"fleet_size\": \"10\"}, {\"tier\": \"x\"}, {\"tier\": \"\"}]"));

        ChartTier second = ChartTiers.forTier(2);
        assertEquals(ChartTier.fallback(2).fleetSize(), second.fleetSize());
        assertEquals(3, second.fleetsMin());
        assertEquals(ChartTier.fallback(2).boss(), second.boss());
        assertEquals(ChartTier.fallback(7), ChartTiers.forTier(7));
        assertEquals(ChartTiers.forTier(ChartTiers.HIGHEST_TIER), ChartTiers.forTier(99));
        assertEquals(ChartTiers.forTier(ChartTiers.LOWEST_TIER), ChartTiers.forTier(-3));
    }

    @Test
    void anUnloadedTableStillGivesEveryTierUsableValues() throws Exception {
        ChartTiers.register(null);

        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier fallback = ChartTiers.forTier(tier);
            assertTrue(fallback.fleetSize() > 0f);
            assertTrue(fallback.fleetsMax() >= fallback.fleetsMin());
            assertTrue(fallback.fleetStrengthMin() <= fallback.fleetStrengthMax());
        }
    }

    @Test
    void aFleetsChartTierPicksItsFleetStrengthRange() throws Exception {
        ChartTiers.register(csvRows(ChartTiers.PATH));
        MemoryAPI tierFifteen = mock(MemoryAPI.class);
        when(tierFifteen.get(ChartIds.TIER_MEMORY_KEY)).thenReturn(15);
        MemoryAPI notAChartFleet = mock(MemoryAPI.class);
        MemoryAPI garbled = mock(MemoryAPI.class);
        when(garbled.get(ChartIds.TIER_MEMORY_KEY)).thenReturn("fifteen");

        assertEquals(new NpcTreeSize.PercentRange(200, 300), ChartTiers.fleetStrengthRangeFor(tierFifteen));
        assertNull(ChartTiers.fleetStrengthRangeFor(notAChartFleet));
        assertNull(ChartTiers.fleetStrengthRangeFor(garbled));
        assertNull(ChartTiers.fleetStrengthRangeFor(null));
    }

    @Test
    void anOversizedFleetsStrengthBonusRaisesItsRangeUpToTheTreeCap() throws Exception {
        ChartTiers.register(csvRows(ChartTiers.PATH));
        ChartTier seventh = ChartTiers.forTier(7);
        MemoryAPI tierSeven = mock(MemoryAPI.class);
        when(tierSeven.get(ChartIds.TIER_MEMORY_KEY)).thenReturn(7);
        when(tierSeven.get(ChartIds.STRENGTH_BONUS_MEMORY_KEY)).thenReturn(20);
        MemoryAPI tierFifteen = mock(MemoryAPI.class);
        when(tierFifteen.get(ChartIds.TIER_MEMORY_KEY)).thenReturn(15);
        when(tierFifteen.get(ChartIds.STRENGTH_BONUS_MEMORY_KEY)).thenReturn(20);

        assertEquals(new NpcTreeSize.PercentRange(seventh.fleetStrengthMin() + 20, seventh.fleetStrengthMax() + 20),
                ChartTiers.fleetStrengthRangeFor(tierSeven));
        assertEquals(new NpcTreeSize.PercentRange(220, NpcTreeSize.HIGHEST_PERCENT), ChartTiers.fleetStrengthRangeFor(tierFifteen));
    }

    @Test
    void theShippedPointsOfInterestAllParseAndIncludeAGuardedVault() throws Exception {
        JSONArray rows = csvRows(ChartPois.PATH);
        ChartPois.register(rows);

        assertEquals(rows.length(), ChartPois.all().size());
        List<ChartPoi> vaults = ChartPois.vaultsAt(ChartPois.all(), 15);
        assertEquals(1, vaults.size());
        assertTrue(vaults.get(0).guarded());
        assertEquals("exiledSector_chart_vault", vaults.get(0).specForSalvage());
        for (ChartPoi poi : ChartPois.all()) {
            assertTrue(poi.cost() > 0f, poi.id());
            if (poi.kind() == ChartPoi.Kind.DERELICT) {
                assertTrue(ChartPois.DERELICT_TYPES.contains(poi.derelictType()), poi.id());
            }
        }
        assertTrue(ChartPois.lootAt(ChartPois.all(), 1).stream().noneMatch(ChartPoi::isVault));
        assertTrue(ChartPois.lootAt(ChartPois.all(), 1).stream().allMatch(poi -> poi.allowedAt(1)));
        assertTrue(ChartPois.vaultsAt(ChartPois.all(), 1).isEmpty());
    }

    @Test
    void everyChartSalvageSpecHasAHullItemDropRuleAndTheVaultSpecExists() throws Exception {
        ChartPois.register(csvRows(ChartPois.PATH));
        Set<String> dropSites = new HashSet<>();
        JSONArray salvageRows = csvRows("data/config/exiledSector/item_salvage.csv");
        for (int i = 0; i < salvageRows.length(); i++) {
            dropSites.add(salvageRows.getJSONObject(i).optString("site"));
        }
        String chartSalvageSpecs = Files.readString(RealSkillData.projectRoot().resolve("data/campaign/procgen/salvage_entity_gen_data.csv"));

        for (ChartPoi poi : ChartPois.all()) {
            assertTrue(dropSites.contains(poi.specForSalvage()), poi.id() + " drops no hull items: " + poi.specForSalvage());
        }
        assertTrue(dropSites.contains(ChartRewards.REWARD_SITE));
        assertTrue(chartSalvageSpecs.contains("\nexiledSector_chart_vault,"));
    }

    @Test
    void badPointOfInterestRowsAreSkipped() throws Exception {
        ChartPois.register(new JSONArray("[{\"id\": \"nothing\", \"kind\": \"cave\", \"entity\": \"supply_cache\"},"
                + " {\"id\": \"no_type\", \"kind\": \"derelict\", \"entity\": \"wreck\"},"
                + " {\"id\": \"no_entity\", \"kind\": \"cache\"},"
                + " {\"id\": \"kept\", \"kind\": \"cache\", \"entity\": \"supply_cache\", \"cost\": \"oops\", \"min_tier\": \"9\", \"max_tier\": \"2\"}]"));

        assertEquals(1, ChartPois.all().size());
        ChartPoi kept = ChartPois.all().get(0);
        assertEquals("kept", kept.id());
        assertEquals(1f, kept.cost());
        assertEquals(2, kept.minTier());
        assertEquals(9, kept.maxTier());
    }

    @Test
    void factionsAreFilteredByTierAndByWhetherTheirModIsLoaded() throws Exception {
        ChartFactions.register(csvRows(ChartFactions.PATH));
        Set<String> loaded = Set.of("pirates", "hegemony", "remnant", "luddic_path", "independent", "mercenary", "sindrian_diktat",
                "persean", "luddic_church", "tritachyon", "derelict");

        List<ChartFaction> atOne = ChartFactions.availableAt(ChartFactions.all(), 1, loaded::contains);
        List<ChartFaction> atFifteen = ChartFactions.availableAt(ChartFactions.all(), 15, loaded::contains);

        assertTrue(atOne.stream().anyMatch(faction -> faction.factionId().equals("pirates")));
        assertTrue(atOne.stream().noneMatch(faction -> faction.factionId().equals("remnant")));
        assertTrue(atFifteen.stream().anyMatch(faction -> faction.factionId().equals("remnant")));
        assertEquals(loaded, atFifteen.stream().map(ChartFaction::factionId).collect(Collectors.toSet()), "every loaded faction can guard tier 15");
        assertTrue(atFifteen.stream().noneMatch(faction -> faction.factionId().equals("diableavionics")));
        assertEquals(ChartFaction.FALLBACK, ChartFactions.pick(ChartFactions.all(), 5, id -> false, new Random(1)));
        for (int seed = 0; seed < 50; seed++) {
            assertTrue(loaded.contains(ChartFactions.pick(ChartFactions.all(), 8, loaded::contains, new Random(seed)).factionId()));
        }
    }
}
