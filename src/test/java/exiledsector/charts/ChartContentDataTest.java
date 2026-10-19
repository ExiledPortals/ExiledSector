package exiledsector.charts;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.items.StarChartImplicits;
import exiledsector.modifiers.Modifier;
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
import java.util.function.Predicate;
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
    void eachShippedSiteBudgetStartsAtItsFirstSiteAndTheDomainBudgetAffordsTheMothership() throws Exception {
        loadShippedTables();

        for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
            List<ChartPoi> members = ChartPois.ofKind(ChartPois.all(), kind);
            assertFalse(members.isEmpty(), kind + " has no point of interest");
            int firstTier = members.stream().mapToInt(ChartPoi::minTier).min().orElseThrow();
            for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
                float budget = ChartTiers.forTier(tier).siteBudget(kind);
                assertTrue(tier < firstTier ? budget == 0f : budget > 0f, kind + " budget " + budget + " at tier " + tier);
            }
        }
        ChartPoi mothership = ChartPois.all().stream().filter(poi -> poi.id().equals("mothership")).findFirst().orElseThrow();
        assertEquals(ChartPoi.Kind.DOMAIN, mothership.kind());
        assertTrue(ChartTiers.forTier(mothership.minTier()).domainBudget() >= mothership.cost(), "the domain budget can buy a mothership at its first tier");
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
                for (ChartPoi.Kind kind : ChartPoi.Kind.values()) {
                    assertTrue(current.siteBudget(kind) >= previous.siteBudget(kind), kind + " budget never drops at tier " + tier);
                }
                assertTrue(current.fleetStrengthMax() >= previous.fleetStrengthMax(), "fleet strength never drops at tier " + tier);
            }
            assertTrue(current.fleetStrengthMax() <= NpcTreeSize.HIGHEST_PERCENT);
            assertTrue(current.fleetSize() <= ChartContentPlanner.FLEET_SIZE_CAP, "tier " + tier + " fleets fit under the size cap");
            assertTrue(current.wardenSize() <= ChartContentPlanner.WARDEN_SIZE_CAP, "tier " + tier + " warden fits under the size cap");
            previous = current;
        }
        assertFalse(ChartTiers.forTier(1).boss());
        assertTrue(ChartTiers.forTier(3).weightFor(ChartObjective.CLEAR) > 0f);
        assertEquals(0f, ChartTiers.forTier(3).weightFor(ChartObjective.BOSS), "tier 3 has no warden, so it is Clear only");
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

        ChartTier fifteen = ChartTiers.forTier(15);
        assertEquals(new NpcTreeSize.PercentRange(fifteen.fleetStrengthMin(), fifteen.fleetStrengthMax()), ChartTiers.fleetStrengthRangeFor(tierFifteen));
        assertTrue(fifteen.fleetStrengthMax() < NpcTreeSize.HIGHEST_PERCENT, "tier 15 leaves room for overflow strength");
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
        when(tierFifteen.get(ChartIds.STRENGTH_BONUS_MEMORY_KEY)).thenReturn(80);

        assertEquals(new NpcTreeSize.PercentRange(seventh.fleetStrengthMin() + 20, seventh.fleetStrengthMax() + 20),
                ChartTiers.fleetStrengthRangeFor(tierSeven));
        ChartTier fifteen = ChartTiers.forTier(15);
        assertEquals(new NpcTreeSize.PercentRange(fifteen.fleetStrengthMin() + 80, NpcTreeSize.HIGHEST_PERCENT), ChartTiers.fleetStrengthRangeFor(tierFifteen));
    }

    @Test
    void theShippedPointsOfInterestAllParse() throws Exception {
        JSONArray rows = csvRows(ChartPois.PATH);
        ChartPois.register(rows);

        assertEquals(rows.length(), ChartPois.all().size());
        for (ChartPoi poi : ChartPois.all()) {
            assertTrue(poi.cost() > 0f, poi.id());
            if (poi.kind() == ChartPoi.Kind.DERELICT) {
                assertTrue(ChartPois.DERELICT_TYPES.contains(poi.derelictType()), poi.id());
            }
        }
        assertTrue(ChartPois.lootAt(ChartPois.all(), 1).stream().allMatch(poi -> poi.allowedAt(1)));
    }

    @Test
    void everyChartSalvageSpecHasAHullItemDropRule() throws Exception {
        ChartPois.register(csvRows(ChartPois.PATH));
        Set<String> dropSites = new HashSet<>();
        JSONArray salvageRows = csvRows("data/config/exiledSector/item_salvage.csv");
        for (int i = 0; i < salvageRows.length(); i++) {
            dropSites.add(salvageRows.getJSONObject(i).optString("site"));
        }

        for (ChartPoi poi : ChartPois.all()) {
            assertTrue(dropSites.contains(poi.specForSalvage()), poi.id() + " drops no hull items: " + poi.specForSalvage());
        }
        assertTrue(dropSites.contains(ChartRewards.REWARD_SITE));
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
    void onlyVanillaFactionsAreListedAndUnloadedOnesAreSkipped() throws Exception {
        ChartFactions.register(csvRows(ChartFactions.PATH));
        Set<String> vanilla = Set.of("pirates", "hegemony", "remnant", "luddic_path", "independent", "mercenary", "sindrian_diktat",
                "persean", "luddic_church", "tritachyon", "derelict");

        assertEquals(vanilla, ChartFactions.all().stream().map(ChartFaction::factionId).collect(Collectors.toSet()));
        assertEquals(vanilla, ChartFactions.available(ChartFactions.all(), vanilla::contains).stream().map(ChartFaction::factionId)
                .collect(Collectors.toSet()));
        assertEquals(ChartFaction.FALLBACK, ChartFactions.pick(ChartFactions.all(), id -> false, new Random(1)));
        for (int seed = 0; seed < 50; seed++) {
            assertTrue(vanilla.contains(ChartFactions.pick(ChartFactions.all(), vanilla::contains, new Random(seed)).factionId()));
        }
    }

    @Test
    void aChartsFactionIsUsedWhenLoadedAndRandomOtherwise() throws Exception {
        ChartFactions.register(csvRows(ChartFactions.PATH));
        Predicate<String> everyFaction = id -> true;

        ChartFaction remnant = ChartFactions.resolve("remnant", ChartFactions.all(), everyFaction, new Random(1));
        assertEquals("remnant", remnant.factionId());
        assertEquals(15f, remnant.fleetQualityBonus());
        assertEquals(ChartFaction.unlisted("diableavionics"), ChartFactions.resolve("diableavionics", ChartFactions.all(), everyFaction, new Random(1)));
        assertNotEquals("diableavionics", ChartFactions.resolve("diableavionics", ChartFactions.all(), id -> !id.equals("diableavionics"),
                new Random(1)).factionId());
        assertTrue(ChartFactions.all().contains(ChartFactions.resolve(null, ChartFactions.all(), everyFaction, new Random(1))));
    }

    @Test
    void everyListedFactionIsAWeightedChartImplicit() throws Exception {
        ChartFactions.register(csvRows(ChartFactions.PATH));

        List<String> implicitIds = StarChartImplicits.sharedChoices().stream().map(Modifier::id).toList();
        assertEquals(ChartFactions.all().size(), implicitIds.size());
        assertTrue(implicitIds.contains("CHART_FACTION_LUDDIC_PATH"));
        assertEquals(new ChartFactionEffect("luddic_path"), ChartEffects.RESOLVER.byName("CHART_FACTION_LUDDIC_PATH"));
        assertEquals(new ChartFactionEffect("diableavionics"), ChartEffects.RESOLVER.byName("CHART_FACTION_DIABLEAVIONICS"),
                "a faction removed from the list still reads, so old charts fall back to a random faction instead of an unreadable line");
        ChartFactions.resetForTests();
        assertTrue(StarChartImplicits.sharedChoices().isEmpty());
    }
}
