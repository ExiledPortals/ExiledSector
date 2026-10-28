package exiledsector.charts.anomaly.storm;

import exiledsector.items.GrantedModifiers;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemType;
import exiledsector.items.ReservedModifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StormLoot {

    public static final List<String> MODIFIER_IDS = List.of("EMP_DAMAGE_TAKEN_PERCENT_STORM_FRONT",
            "NON_BEAM_ENERGY_WEAPON_CHAIN_CHANCE_PERCENT_STORM_FRONT");

    private StormLoot() {
    }

    public static void reserveModifiers() {
        for (String modifierId : MODIFIER_IDS) {
            ReservedModifiers.reserve(modifierId, StormFront.ID);
        }
    }

    public static ItemDrops.SiteLoot touch(ItemDrops.SiteLoot loot, Random random) {
        if (loot == null || loot.items().isEmpty()) {
            return loot;
        }
        List<ItemData> touched = new ArrayList<>(loot.items().size());
        for (ItemData item : loot.items()) {
            touched.add(touch(item, random));
        }
        return new ItemDrops.SiteLoot(touched, loot.upgrades(), loot.materials());
    }

    static ItemData touch(ItemData item, Random random) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || definition.type() != ItemType.HULL_ITEM || random.nextFloat() >= StormRules.STORM_MODIFIER_CHANCE) {
            return item;
        }
        List<String> grantable = MODIFIER_IDS.stream().filter(modifierId -> GrantedModifiers.canGrant(definition, modifierId)).toList();
        if (grantable.isEmpty()) {
            return item;
        }
        return GrantedModifiers.grant(item, grantable.get(random.nextInt(grantable.size())), random);
    }
}
