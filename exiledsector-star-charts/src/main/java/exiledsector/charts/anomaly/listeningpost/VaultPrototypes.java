package exiledsector.charts.anomaly.listeningpost;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.ItemUnlock;
import exiledsector.items.PrototypeImplicits;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

public final class VaultPrototypes {

    static final int PICKS = 3;
    private static final long PROTOTYPE_SEED_SALT = 0x2F6B3C1D9A4E5871L;

    private VaultPrototypes() {
    }

    static List<ItemData> offered(SectorAPI sector, long vaultSeed) {
        return ItemUnlock.available(ItemType.HULL_ITEM, sector) ? roll(PICKS, vaultSeed, definition -> ItemUnlock.canDrop(definition, sector)) : List.of();
    }

    static ItemData leftAfterAlarm(SectorAPI sector, long vaultSeed) {
        List<ItemData> left = ItemUnlock.available(ItemType.HULL_ITEM, sector) ? roll(1, vaultSeed, definition -> ItemUnlock.canDrop(definition, sector))
                : List.of();
        return left.isEmpty() ? null : left.get(0);
    }

    static List<ItemData> roll(int count, long vaultSeed) {
        return roll(count, vaultSeed, definition -> true);
    }

    static List<ItemData> roll(int count, long vaultSeed, Predicate<ItemDefinition> canDrop) {
        Random random = new Random(vaultSeed ^ PROTOTYPE_SEED_SALT);
        List<ItemDefinition> bases = new ArrayList<>(PrototypeImplicits.bases().stream().map(ItemDefinitions::get).filter(Objects::nonNull)
                .filter(definition -> definition.type() == ItemType.HULL_ITEM && !definition.unique() && canDrop.test(definition)).toList());
        List<ItemData> prototypes = new ArrayList<>(count);
        while (prototypes.size() < count && !bases.isEmpty()) {
            ItemData prototype = PrototypeImplicits.create(bases.remove(random.nextInt(bases.size())), random.nextLong(), random);
            if (prototype != null) {
                prototypes.add(prototype);
            }
        }
        return prototypes;
    }
}
