package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UniqueSubroutinesTest {

    private static final String FLEET_RELIC = "unique_fleet_relic";
    private static final String SITE_RELIC = "unique_site_relic";
    private static final String HULL_RELIC_A = "hull_relic_a";
    private static final String HULL_RELIC_B = "hull_relic_b";
    private static final int TRIALS = 20000;

    private final ItemStore store = new ItemStore();

    @BeforeEach
    void setUp() throws Exception {
        registerModifiers("TOP_SPEED_PERCENT:8:12");
        registerDefinitions();
        ItemDrops.register(new JSONArray()
                .put(new JSONObject().put("site", "station").put("chances", "0").put("uniqueChance", "1"))
                .put(new JSONObject().put("site", "elsewhere").put("chances", "0").put("uniqueChance", "1")));
    }

    @AfterEach
    void tearDown() {
        ItemDrops.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
        NpcItems.clearCache();
    }

    private static void registerModifiers(String topSpeedLine) throws JSONException {
        ItemModifiers.register(new JSONArray()
                .put(ItemFixtures.modifierRow("TOP_SPEED_PERCENT_RELIC", "prefix", "unique_subroutine", topSpeedLine, ""))
                .put(ItemFixtures.modifierRow("SENSOR_PROFILE_PERCENT_RELIC", "prefix", "unique_subroutine", "SENSOR_PROFILE_PERCENT:15:15", ""))
                .put(ItemFixtures.modifierRow("HULL_MULT", "prefix", "hull_item", "HULL_MULT:4:6", "Reinforced")));
    }

    private static void registerDefinitions() throws JSONException {
        ItemDefinitions.register(new JSONArray()
                .put(uniqueRow(FLEET_RELIC, "@TOP_SPEED_PERCENT_RELIC; @SENSOR_PROFILE_PERCENT_RELIC; HULL_PERCENT:3:3", "faction:pirates"))
                .put(uniqueRow(SITE_RELIC, "@TOP_SPEED_PERCENT_RELIC", "site:station"))
                .put(ItemFixtures.subroutineRow(ItemFixtures.SUBROUTINE))
                .put(ItemFixtures.row(HULL_RELIC_A, "reactor", "@HULL_MULT").put("unique", "TRUE"))
                .put(ItemFixtures.row(HULL_RELIC_B, "crew_quarters", "FLUX_CAPACITY_MULT:4:6").put("unique", "TRUE")));
    }

    private static JSONObject uniqueRow(String id, String lines, String source) throws JSONException {
        return ItemFixtures.subroutineRow(id, SubroutineKind.UNIQUE).put("name", "Relic").put("unique", "true")
                .put(ItemDefinition.UNIQUE_LINES_COLUMN, lines).put(ItemDefinition.UNIQUE_SOURCE_COLUMN, source);
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);
        return xstream;
    }

    private Item fleetRelic(long seed) {
        return store.add(ItemData.rolled(ItemDefinitions.get(FLEET_RELIC), seed));
    }

    @Test
    void aUniqueSubroutineLoadsItsLinesAndSources() {
        ItemDefinition definition = ItemDefinitions.get(FLEET_RELIC);

        assertTrue(definition.unique());
        assertTrue(definition.isUniqueSubroutine());
        assertEquals(ItemType.SUBROUTINE, definition.type());
        assertEquals(List.of("TOP_SPEED_PERCENT", "SENSOR_PROFILE_PERCENT", "HULL_PERCENT"), definition.pool().stream().map(PoolEntry::effectName).toList());
        assertTrue(definition.suffixes().isEmpty());
        assertEquals(List.of(new UniqueSource(UniqueSource.Kind.FACTION, "pirates")), definition.uniqueSources());
        assertFalse(ItemDefinitions.get(ItemFixtures.SUBROUTINE).isUniqueSubroutine());
        assertTrue(ItemDefinitions.get(HULL_RELIC_A).uniqueSources().isEmpty());
    }

    @Test
    void onlyUniqueSubroutinesFlaggedUniqueLoadAndTheirLinesMustBeScopedToThem() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(uniqueRow("hull_scoped", "@HULL_MULT; TOP_SPEED_PERCENT:1:1", "site:station"))
                .put(uniqueRow("flagless", "TOP_SPEED_PERCENT:1:1", "site:station").put("unique", ""))
                .put(ItemFixtures.subroutineRow("volume_lines").put(ItemDefinition.UNIQUE_LINES_COLUMN, "TOP_SPEED_PERCENT:1:1"))
                .put(uniqueRow("badly_sourced", "TOP_SPEED_PERCENT:1:1", "colony:jangala")));

        assertEquals(List.of("TOP_SPEED_PERCENT"), ItemDefinitions.get("hull_scoped").pool().stream().map(PoolEntry::effectName).toList());
        assertNull(ItemDefinitions.get("flagless"));
        assertNull(ItemDefinitions.get("volume_lines"));
        assertNull(ItemDefinitions.get("badly_sourced"));
    }

    @Test
    void theSocketLimitColumnLoadsAndABadValueSkipsTheRow() throws Exception {
        ItemDefinitions.register(new JSONArray()
                .put(uniqueRow("once", "TOP_SPEED_PERCENT:1:1", "site:station").put(ItemDefinition.SOCKET_LIMIT_COLUMN, " 1 "))
                .put(uniqueRow("blank", "TOP_SPEED_PERCENT:1:1", "site:station"))
                .put(uniqueRow("zero", "TOP_SPEED_PERCENT:1:1", "site:station").put(ItemDefinition.SOCKET_LIMIT_COLUMN, "0"))
                .put(uniqueRow("negative", "TOP_SPEED_PERCENT:1:1", "site:station").put(ItemDefinition.SOCKET_LIMIT_COLUMN, "-1"))
                .put(uniqueRow("fractional", "TOP_SPEED_PERCENT:1:1", "site:station").put(ItemDefinition.SOCKET_LIMIT_COLUMN, "1.5")));

        assertEquals(1, ItemDefinitions.get("once").socketLimit());
        assertTrue(ItemDefinitions.get("once").limitsSocketsPerShip());
        assertFalse(ItemDefinitions.get("blank").limitsSocketsPerShip());
        assertFalse(ItemDefinitions.get("zero").limitsSocketsPerShip());
        assertNull(ItemDefinitions.get("negative"));
        assertNull(ItemDefinitions.get("fractional"));
    }

    @Test
    void everyLineRollsOnceAsAPrefixWithinItsRangeAndShowsInTheTooltip() {
        Item item = fleetRelic(7L);

        assertEquals(List.of(ModifierSlot.PREFIX, ModifierSlot.PREFIX, ModifierSlot.PREFIX), item.explicits().stream().map(RolledModifier::slot).toList());
        float topSpeed = item.effects().get(0).magnitude();
        assertTrue(topSpeed >= 8f && topSpeed <= 12f, String.valueOf(topSpeed));
        assertEquals(15f, item.effects().get(1).magnitude());
        assertEquals(3f, item.effects().get(2).magnitude());
        assertEquals(ItemRarity.UNIQUE, item.rarity());
        assertEquals(3, item.effectLines().size());
        assertTrue(item.tooltipLines().containsAll(item.effectLines()));
        assertEquals(3, item.skillEffects().size());
    }

    @Test
    void rolledValuesAreFrozenThroughASaveAndLoadAndARebalance() throws Exception {
        Item item = fleetRelic(7L);
        List<RolledModifier> rolled = item.explicits();
        String xml = xstream().toXML(store);
        String cargoData = ItemData.of(item).encode();

        registerModifiers("TOP_SPEED_PERCENT:30:40");
        registerDefinitions();
        Item loaded = ((ItemStore) xstream().fromXML(xml)).owned().get(0);

        assertEquals(Subroutine.class, loaded.getClass());
        assertEquals(rolled, loaded.explicits());
        assertEquals(rolled, ItemData.parse(cargoData).explicits());
        assertEquals(rolled.get(0).lines().get(0).magnitude(), loaded.skillEffects().get(0).magnitude());
    }

    @Test
    void everyCurrencyRefusesAUniqueSubroutine() {
        Item item = fleetRelic(7L);
        List<RolledModifier> rolled = item.explicits();
        CargoAPI cargo = mock(CargoAPI.class);
        for (ItemCurrency currency : ItemCurrencies.all()) {
            when(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, MaterialItems.data(currency.itemId()))).thenReturn(5f);
        }
        CraftingMaterials materials = CraftingMaterials.cargoOnly(cargo);

        for (ItemCurrency currency : ItemCurrencies.all()) {
            assertFalse(ItemCrafting.canUse(currency, item, definition -> true), currency.key());
            assertNull(ItemCrafting.use(currency, item, materials, store, new Random(1L), definition -> true), currency.key());
        }

        assertTrue(ItemCrafting.isLockedFromCurrency(item.definition()));
        assertFalse(ItemCrafting.canTranspose(item.definition(), item.tier(), definition -> true));
        assertEquals(rolled, item.explicits());
        assertFalse(item.corrupted());
        assertEquals(List.of(item), store.owned());
    }

    @Test
    void aTransposedHullUniqueNeverBecomesAUniqueSubroutine() {
        Random random = new Random(4L);
        for (int trial = 0; trial < 200; trial++) {
            Item hullUnique = store.add(ItemData.rolled(ItemDefinitions.get(HULL_RELIC_A), random.nextLong()));

            Item replacement = ItemCrafting.transpose(store, hullUnique, random, definition -> true);

            assertNotNull(replacement);
            assertEquals(HULL_RELIC_B, replacement.definitionId());
            store.remove(replacement);
        }
    }

    @Test
    void genericPickersNeverPickASourcedUnique() {
        Random random = new Random(3L);
        Set<String> picked = new HashSet<>();
        for (int trial = 0; trial < TRIALS; trial++) {
            picked.add(ItemDrops.pickBasic(random).id());
            picked.add(NpcItems.pickDefinition(random, definition -> true).id());
            ItemDefinition unique = ItemDrops.pickUnique(random, definition -> true);
            picked.add(unique == null ? "" : unique.id());
        }

        assertEquals(Set.of(ItemFixtures.SUBROUTINE, HULL_RELIC_A, HULL_RELIC_B), picked);
    }

    @Test
    void fleetsOfTheSourceFactionCarryTheirUniqueAtTheUsualShare() {
        Random random = new Random(5L);
        int carried = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            String pickedId = NpcItems.pickDefinition(random, "pirates", definition -> true).id();
            assertFalse(pickedId.equals(SITE_RELIC) || pickedId.equals(HULL_RELIC_A), pickedId);
            if (pickedId.equals(FLEET_RELIC)) {
                carried++;
            }
            assertEquals(ItemFixtures.SUBROUTINE, NpcItems.pickDefinition(random, "hegemony", definition -> true).id());
            assertEquals(ItemFixtures.SUBROUTINE, NpcItems.pickDefinition(random, "pirates", definition -> false).id());
        }

        assertEquals(NpcItems.UNIQUE_SHARE, carried / (float) TRIALS, 0.004f);
    }

    @Test
    void onlyTheSourceSiteDropsASiteUnique() {
        Random random = new Random(6L);
        for (int trial = 0; trial < 200; trial++) {
            List<ItemData> fromStation = ItemDrops.roll("station", random, 1f, ItemDefinition::isUniqueSubroutine);
            List<ItemData> fromElsewhere = ItemDrops.roll("elsewhere", random, 1f, ItemDefinition::isUniqueSubroutine);

            assertEquals(List.of(SITE_RELIC), fromStation.stream().map(ItemData::definitionId).toList());
            assertTrue(fromElsewhere.isEmpty(), fromElsewhere.toString());
        }
        Set<String> hullSiteDrops = new HashSet<>();
        for (int trial = 0; trial < 200; trial++) {
            ItemDrops.roll("elsewhere", random, 1f, definition -> true).forEach(drop -> hullSiteDrops.add(drop.definitionId()));
        }
        assertEquals(Set.of(HULL_RELIC_A, HULL_RELIC_B), hullSiteDrops);
    }

    @Test
    void sourcesParseAsFactionOrSiteAndAnEmptyListAllowsEverySource() {
        assertEquals(List.of(new UniqueSource(UniqueSource.Kind.FACTION, "tritachyon"), new UniqueSource(UniqueSource.Kind.SITE, "station_research")),
                UniqueSource.parseList(" faction:tritachyon ; SITE:station_research "));
        assertEquals("site:station_research", UniqueSource.parse("site:station_research").text());
        assertTrue(UniqueSource.parseList("").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> UniqueSource.parse("tritachyon"));
        assertThrows(IllegalArgumentException.class, () -> UniqueSource.parse("faction:"));
        assertThrows(IllegalArgumentException.class, () -> UniqueSource.parseList("site:a; site:a"));

        List<UniqueSource> pirateSources = List.of(new UniqueSource(UniqueSource.Kind.FACTION, "pirates"));
        assertTrue(UniqueSource.allows(List.of(), UniqueSource.Kind.SITE, null));
        assertTrue(UniqueSource.allows(pirateSources, UniqueSource.Kind.FACTION, "pirates"));
        assertFalse(UniqueSource.allows(pirateSources, UniqueSource.Kind.SITE, "pirates"));
        assertFalse(UniqueSource.allows(pirateSources, UniqueSource.Kind.FACTION, null));
    }

    @Test
    void theShippedUniqueSubroutinesLoadWithLinesAndASource() throws Exception {
        ItemModifiers.register(ItemFixtures.shippedRows(ItemModifiers.DATA_PATH));
        ItemDefinitions.register(ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH), ItemFixtures.shippedRows(ItemDefinitions.HULL_ITEMS_PATH));
        JSONArray shippedRows = ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH);

        List<ItemDefinition> uniques = ItemDefinitions.ofType(ItemType.SUBROUTINE).stream().filter(ItemDefinition::isUniqueSubroutine).toList();

        assertEquals(shippedRows.length(), uniques.size());
        assertTrue(uniques.size() >= 4 && uniques.size() <= 6, String.valueOf(uniques.size()));
        for (ItemDefinition unique : uniques) {
            assertTrue(unique.unique(), unique.id());
            assertTrue(unique.pool().size() >= 2, unique.id());
            assertFalse(unique.uniqueSources().isEmpty(), unique.id());
            assertEquals(1, unique.socketLimit(), unique.id() + " is limited to one per ship");
            assertFalse(unique.description().isEmpty(), unique.id());
            assertFalse(unique.description().matches(".*\\d.*"), unique.id() + " has a number in its description");
            Item item = store.add(ItemData.rolled(unique, 11L));
            assertEquals(unique.pool().size(), item.skillEffects().size(), unique.id());
        }
    }
}
