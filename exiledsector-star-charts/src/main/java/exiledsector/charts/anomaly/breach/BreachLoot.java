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
        if (item == null) {
            return null;
        }
        Random random = Seeds.derived(item.seed(), WARP_SEED_SALT);
        return random.nextFloat() < BreachRules.WARPED_CHANCE ? WarpedModifiers.warp(item, random) : item;
    }
}
