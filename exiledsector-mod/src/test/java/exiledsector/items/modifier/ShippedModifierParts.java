package exiledsector.items.modifier;

import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.items.HullItemRows;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.crafting.material.Engrams;
import exiledsector.items.hull.HullItem;
import exiledsector.items.subroutine.SubroutineDefinitions;
import exiledsector.skills.skilleffect.EffectAliases;

public final class ShippedModifierParts {

    private ShippedModifierParts() {
    }

    public static void load() throws Exception {
        EffectAliases.register(HullItemRows.shippedRows(EffectAliases.dataPath()));
        ItemModifiers.register(HullItemRows.shippedRows(ItemModifiers.dataPath()));
        ItemDefinitions.register(HullItemRows.shippedRows(SubroutineDefinitions.dataPath()), HullItemRows.shippedRows(HullItem.definitionsPath()));
        ItemImplicits.register(HullItemRows.shippedRows(ItemImplicits.dataPath()));
        ItemImplicits.registerCorruptionPool(HullItemRows.shippedRows(ItemImplicits.corruptionDataPath()));
        PrototypeImplicits.register(HullItemRows.shippedRows(PrototypeImplicits.dataPath()));
        Engrams.register(HullItemRows.shippedRows(Engrams.PATH));
        BiomeModifiers.register(HullItemRows.shippedRows(BiomeModifiers.PATH));
    }

    public static void clear() {
        BiomeModifiers.unregisterAll();
        Engrams.resetForTests();
        ModifierSources.clear();
        ItemImplicits.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }
}
