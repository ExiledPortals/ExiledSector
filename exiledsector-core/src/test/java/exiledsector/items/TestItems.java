package exiledsector.items;

import exiledsector.items.modifier.PoolEntry;
import exiledsector.items.modifier.RolledLine;
import exiledsector.items.modifier.RolledModifier;
import exiledsector.modifiers.ModifierSlot;

import java.util.ArrayList;
import java.util.List;

public final class TestItems {

    private TestItems() {
    }

    public static Item withLines(String id, String definitionId, long seed, List<RolledLine> lines) {
        return Item.create(id, definitionId, seed, explicits(ItemDefinitions.get(definitionId), lines));
    }

    public static List<RolledModifier> explicits(ItemDefinition definition, List<RolledLine> lines) {
        List<RolledModifier> modifiers = new ArrayList<>(lines.size());
        for (RolledLine line : lines) {
            PoolEntry entry = definition == null ? null : definition.poolEntry(line.effectName());
            modifiers.add(RolledModifier.single(entry == null ? ModifierSlot.SUFFIX : entry.slot(), line));
        }
        return definition == null ? modifiers : definition.withCurrentIds(modifiers);
    }
}
