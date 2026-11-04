package exiledsector.items;

import exiledsector.charts.anomaly.stormfront.StormLoot;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.skills.skilleffect.EffectAliases;

final class ShippedModifierParts {

    private ShippedModifierParts() {
    }

    static void load() throws Exception {
        EffectAliases.register(HullItemRows.shippedRows(EffectAliases.dataPath()));
        ItemModifiers.register(HullItemRows.shippedRows(ItemModifiers.dataPath()));
        ItemDefinitions.register(HullItemRows.shippedRows(SubroutineDefinitions.dataPath()), HullItemRows.shippedRows(HullItem.definitionsPath()));
        ItemImplicits.register(HullItemRows.shippedRows(ItemImplicits.dataPath()));
        ItemImplicits.registerCorruptionPool(HullItemRows.shippedRows(ItemImplicits.corruptionDataPath()));
        Engrams.register(HullItemRows.shippedRows(Engrams.PATH));
        BiomeModifiers.register(HullItemRows.shippedRows(BiomeModifiers.PATH));
        StormLoot.reserveModifiers();
    }

    static void clear() {
        BiomeModifiers.unregisterAll();
        Engrams.resetForTests();
        ModifierSources.clear();
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }
}
