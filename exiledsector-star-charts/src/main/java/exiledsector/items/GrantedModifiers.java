package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;

import java.util.List;
import java.util.Random;

public final class GrantedModifiers {

    private GrantedModifiers() {
    }

    public static boolean canGrant(ItemDefinition definition, String modifierId) {
        Modifier granted = ItemModifiers.get(modifierId);
        return definition != null && granted != null && !definition.unique() && !definition.keepsOneModifier()
                && granted.slot() != ModifierSlot.IMPLICIT && ItemModifiers.itemTypes(modifierId).contains(definition.type())
                && AffixLayout.rollableAt(definition.type(), List.of(new PoolEntry(granted, 1f)), TierRange.NO_TIER).size() == 1;
    }

    public static ItemData grant(ItemData item, String modifierId, Random random) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (!canGrant(definition, modifierId)) {
            return item;
        }
        List<RolledModifier> explicits = AnomalousDebrisCurrency.reroll(definition, ItemModifiers.get(modifierId), null, random);
        return new ItemData(item.definitionId(), item.seed(), explicits, ItemNames.freeze(definition, item.seed(), explicits), item.traits());
    }
}
