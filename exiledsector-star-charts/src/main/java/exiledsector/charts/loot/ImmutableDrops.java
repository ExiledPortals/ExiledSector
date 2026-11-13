package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.Seeds;
import exiledsector.items.CraftingContext;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemType;
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
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || definition.type() != ItemType.HULL_ITEM) {
            return item;
        }
        Random random = Seeds.derived(item.seed(), SEED_SALT);
        if (!(random.nextFloat() < chance)) {
            return item;
        }
        Item preview = item.preview();
        if (preview == null || !Nanoswarm.IMMUTABLE.canUse(preview, NO_INHIBITORS)) {
            return item;
        }
        Item locked = Nanoswarm.IMMUTABLE.use(preview, NO_INHIBITORS, random);
        return locked == null ? item : new ItemData(item.definitionId(), item.seed(), locked.explicits(), locked.frozenName(), item.traits());
    }
}
