package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.Seeds;
import exiledsector.items.CraftingContext;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.Nanoswarm;

import java.util.Random;
import java.util.Set;

public final class ImmutableDrops {

    public static final String CHANCE_KEY = "$exiledSector_chartImmutableDropChance";
    static final long SEED_SALT = 0x696d6d757461626cL;
    private static final CraftingContext NO_INHIBITORS = new CraftingContext(null, definition -> true, Set.of());

    private ImmutableDrops() {
    }

    public static void mark(SectorEntityToken site, float chance) {
        site.getMemoryWithoutUpdate().set(CHANCE_KEY, chance);
    }

    public static ItemData onSiteDrop(ItemData item, MemoryAPI siteMemory) {
        return siteMemory == null || !siteMemory.contains(CHANCE_KEY) ? item : withChance(item, siteMemory.getFloat(CHANCE_KEY));
    }

    static ItemData withChance(ItemData item, float chance) {
        if (item == null || !item.isHullItem()) {
            return item;
        }
        Random random = Seeds.derived(item.seed(), SEED_SALT);
        if (!(random.nextFloat() < chance)) {
            return item;
        }
        Item preview = item.preview();
        if (preview == null) {
            return item;
        }
        fillModifiers(preview, random);
        if (!Nanoswarm.IMMUTABLE.canUse(preview, NO_INHIBITORS)) {
            return item;
        }
        Item locked = Nanoswarm.IMMUTABLE.use(preview, NO_INHIBITORS, random);
        return locked == null ? item : item.withExplicits(locked.explicits(), locked.frozenName());
    }

    private static void fillModifiers(Item item, Random random) {
        boolean augmented = true;
        while (augmented && Nanoswarm.AUGMENTATION.canUse(item, NO_INHIBITORS)) {
            augmented = Nanoswarm.AUGMENTATION.use(item, NO_INHIBITORS, random) != null;
        }
    }
}
