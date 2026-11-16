package exiledsector.charts.anomaly.breach;

import exiledsector.Seeds;
import exiledsector.items.ItemData;
import exiledsector.items.WarpedModifiers;

import java.util.Random;

final class BreachLoot {

    private static final long WARP_SEED_SALT = 0x77617270656469L;

    private BreachLoot() {
    }

    static ItemData warped(ItemData item) {
        return warped(item, BreachRules.WARPED_CHANCE);
    }

    static ItemData warped(ItemData item, float chance) {
        if (item == null) {
            return null;
        }
        Random random = Seeds.derived(item.seed(), WARP_SEED_SALT);
        return random.nextFloat() < chance ? WarpedModifiers.warp(item, random) : item;
    }
}
