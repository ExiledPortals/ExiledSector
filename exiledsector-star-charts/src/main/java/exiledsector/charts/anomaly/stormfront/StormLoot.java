package exiledsector.charts.anomaly.stormfront;

import exiledsector.items.GrantedModifiers;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemType;
import exiledsector.items.ModifierSources;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class StormLoot {

    public static final List<String> MODIFIER_IDS = List.of("EMP_DAMAGE_TAKEN_PERCENT_STORM_FRONT",
            "NON_BEAM_ENERGY_WEAPON_REFRACTION_CHANCE_PERCENT_STORM_FRONT");

    private StormLoot() {
    }

    public static void reserveModifiers() {
        for (String modifierId : MODIFIER_IDS) {
            ModifierSources.reserve(modifierId, StormFront.ANOMALY_ID, ItemKind.sectionItemTypes());
        }
    }

    public static ItemData guaranteedItem(Random random, Predicate<ItemDefinition> allowed) {
        ItemDefinition definition = ItemDrops.pickOfType(random, ItemType.HULL_ITEM,
                candidate -> !candidate.unique() && allowed.test(candidate) && !grantable(candidate).isEmpty());
        if (definition == null) {
            return null;
        }
        List<String> grantable = grantable(definition);
        return GrantedModifiers.grant(ItemData.rolled(definition, random.nextLong()), grantable.get(random.nextInt(grantable.size())), random);
    }

    private static List<String> grantable(ItemDefinition definition) {
        return MODIFIER_IDS.stream().filter(modifierId -> GrantedModifiers.canGrant(definition, modifierId)).toList();
    }
}
