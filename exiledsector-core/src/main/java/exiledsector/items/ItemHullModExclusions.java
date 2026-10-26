package exiledsector.items;

import exiledlib.skills.SkillTypeEffect;
import exiledsector.ExiledSector;
import exiledsector.skills.ShipSkillData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class ItemHullModExclusions {

    private ItemHullModExclusions() {
    }

    public static Map<String, Item> of(ShipSkillData shipData) {
        if (shipData == null) {
            return Map.of();
        }
        Map<String, Item> excluded = new LinkedHashMap<>();
        ExiledSector.enabledDomains().forEach(domain -> domain.excludedHullMods(shipData).forEach(excluded::putIfAbsent));
        return excluded;
    }

    public static Set<String> exclusiveHullModIds(Item item) {
        Set<String> hullModIds = new LinkedHashSet<>();
        for (SkillTypeEffect effect : item.skillEffects()) {
            hullModIds.addAll(effect.effect().exclusiveHullModIds());
        }
        return hullModIds;
    }

    public static String fittedConflict(Item item, Predicate<String> hasHullMod) {
        for (String hullModId : exclusiveHullModIds(item)) {
            if (hasHullMod.test(hullModId)) {
                return hullModId;
            }
        }
        return null;
    }
}
