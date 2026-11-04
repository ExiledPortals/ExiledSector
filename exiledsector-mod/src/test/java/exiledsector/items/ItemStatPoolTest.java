package exiledsector.items;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemStatPoolTest {

    @BeforeEach
    void loadRealData() throws Exception {
        ShippedModifierParts.load();
    }

    @AfterEach
    void clear() {
        ShippedModifierParts.clear();
    }

    private static Set<String> statsOf(List<Modifier> modifiers) {
        Set<String> stats = new LinkedHashSet<>();
        modifiers.forEach(modifier -> modifier.lines().stream().map(ModifierLine::effectName).forEach(stats::add));
        return stats;
    }

    @Test
    void eachPartListsTheStatsOfEveryModifierItCanCarry() {
        Set<String> listed = new LinkedHashSet<>();
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            List<String> kindStats = ItemStatPool.stats(List.of(kind));
            Set<String> missing = statsOf(ModifierParts.modifiers(kind));
            kindStats.forEach(missing::remove);

            assertTrue(missing.isEmpty(), kind.id() + " does not list " + missing);
            listed.addAll(kindStats);
        }
        assertEquals(listed, new LinkedHashSet<>(ItemStatPool.stats(List.of())), "no chosen type lists every stat");
    }

    @Test
    void aBiomeStatShowsOnlyOnThePartsThatCanCarryIt() throws Exception {
        JSONArray biomeRows = HullItemRows.shippedRows(BiomeModifiers.PATH);
        int absentChecks = 0;
        for (int i = 0; i < biomeRows.length(); i++) {
            JSONObject row = biomeRows.getJSONObject(i);
            ItemKind biomePart = ItemKind.byId(row.getString("item"));
            Set<String> biomeStats = statsOf(List.of(ItemModifiers.get(row.getString(BiomeModifiers.ID_COLUMN))));

            assertTrue(ItemStatPool.stats(List.of(biomePart)).containsAll(biomeStats), row.toString());
            for (ItemKind otherPart : ItemKind.sectionItemTypes()) {
                Set<String> carried = statsOf(ModifierParts.modifiers(otherPart));
                List<String> otherStats = ItemStatPool.stats(List.of(otherPart));
                for (String stat : biomeStats) {
                    if (!carried.contains(stat)) {
                        assertFalse(otherStats.contains(stat), stat + " shows on " + otherPart.id());
                        absentChecks++;
                    }
                }
            }
        }
        assertTrue(absentChecks > 0, "every biome stat can appear on every part");
    }

    @Test
    void choosingSocketTypesNarrowsTheListToWhatTheyCanRoll() {
        List<String> everyStat = ItemStatPool.stats(List.of());
        List<String> reactorStats = ItemStatPool.stats(List.of(ItemKind.REACTOR));
        List<String> reactorOrBridgeStats = ItemStatPool.stats(List.of(ItemKind.REACTOR, ItemKind.BRIDGE));

        assertTrue(reactorStats.size() < everyStat.size(), reactorStats.size() + " of " + everyStat.size());
        assertTrue(reactorOrBridgeStats.containsAll(reactorStats));
        assertTrue(reactorOrBridgeStats.containsAll(ItemStatPool.stats(List.of(ItemKind.BRIDGE))));
        ItemKind.REACTOR.implicits().forEach(implicit -> assertTrue(reactorStats.contains(implicit.line().effectName()), implicit.id()));
    }

    @Test
    void eachStatIsListedOnceWithItsOwnLineAndAMarkForTheNumber() {
        List<String> everyStat = ItemStatPool.stats(List.of());

        assertEquals(everyStat.size(), new LinkedHashSet<>(everyStat).size(), "hybrids don't add entries");
        String fluxLabel = ItemStatPool.label("FLUX_DISSIPATION_PERCENT");
        assertTrue(fluxLabel.contains(ItemStatPool.VALUE_MARK), fluxLabel);
        assertTrue(fluxLabel.contains("flux dissipation"), fluxLabel);
        assertFalse(fluxLabel.endsWith("."), fluxLabel);
        assertFalse(fluxLabel.matches(".*\\d.*"), fluxLabel);
    }
}
