package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.skills.skilleffect.EffectAliases;
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
        EffectAliases.register(HullItemRows.shippedRows(EffectAliases.dataPath()));
        ItemModifiers.register(HullItemRows.shippedRows(ItemModifiers.dataPath()));
        ItemDefinitions.register(HullItemRows.shippedRows(SubroutineDefinitions.dataPath()), HullItemRows.shippedRows(HullItem.definitionsPath()));
        ItemImplicits.register(HullItemRows.shippedRows(ItemImplicits.dataPath()));
        ItemImplicits.registerCorruptionPool(HullItemRows.shippedRows(ItemImplicits.corruptionDataPath()));
    }

    @AfterEach
    void clear() {
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static Set<String> hullItemStats() {
        Set<String> stats = new LinkedHashSet<>();
        for (Modifier modifier : ItemModifiers.all()) {
            if (ItemModifiers.itemTypes(modifier.id()).contains(ItemType.HULL_ITEM)) {
                modifier.lines().stream().map(ModifierLine::effectName).forEach(stats::add);
            }
        }
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            kind.implicits().forEach(implicit -> implicit.lines().stream().map(ModifierLine::effectName).forEach(stats::add));
        }
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.HULL_ITEM)) {
            definition.pool().forEach(entry -> entry.modifier().lines().stream().map(ModifierLine::effectName).forEach(stats::add));
        }
        ItemImplicits.corruptionPool().forEach(implicit -> implicit.lines().stream().map(ModifierLine::effectName).forEach(stats::add));
        return stats;
    }

    @Test
    void everyHullItemStatIsInAtLeastOneSocketTypesPool() {
        Set<String> pooled = new LinkedHashSet<>();
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            pooled.addAll(ItemStatPool.stats(List.of(kind)));
        }
        Set<String> missing = new LinkedHashSet<>(hullItemStats());
        missing.removeAll(pooled);

        assertTrue(missing.isEmpty(), "stats no socket type lists: " + missing);
        assertEquals(pooled, new LinkedHashSet<>(ItemStatPool.stats(List.of())), "no chosen type lists every stat");
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
