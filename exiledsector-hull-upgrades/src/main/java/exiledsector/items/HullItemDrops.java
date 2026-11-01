package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class HullItemDrops {

    public record SectionItemLoot(List<ItemData> items, List<ShipCurrency> upgrades) {

        public static final SectionItemLoot NONE = new SectionItemLoot(List.of(), List.of());

        public SectionItemLoot {
            items = List.copyOf(items);
            upgrades = List.copyOf(upgrades);
        }

        public boolean isEmpty() {
            return items.isEmpty() && upgrades.isEmpty();
        }

        public void addTo(CargoAPI cargo) {
            items.forEach(item -> cargo.addSpecial(item.toSpecialItem(), 1f));
            upgrades.forEach(upgrade -> cargo.addSpecial(upgrade.toSpecialItem(), 1f));
        }
    }

    private HullItemDrops() {
    }

    public static List<ShipCurrency> rollMasterSchematic(String siteId, Random random, float chanceMult) {
        ItemDrops.Rule rule = ItemDrops.activeRule(siteId);
        return rule == null || rule.chances().isEmpty() ? List.of() : MasterSchematic.rollSalvage(rule.chances().get(0) * chanceMult, random);
    }

    public static SectionItemLoot rollSectionItemLoot(String siteId, Random random, float chanceMult) {
        return rollSectionItemLoot(siteId, random, ItemChances.of(chanceMult));
    }

    public static SectionItemLoot rollSectionItemLoot(String siteId, Random random, ItemChances chances) {
        ItemDrops.Rule rule = ItemDrops.activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return SectionItemLoot.NONE;
        }
        List<ItemData> droppedItems = ItemDrops.rollCascade(rule, random, chances.general(), chances.rarity(), HullItemDrops::pickSectionItemBasic);
        List<ShipCurrency> droppedUpgrades = new ArrayList<>();
        if (random.nextFloat() < rule.chances().get(0) * chances.general() && RefitSchematic.dropsAllowed()) {
            droppedUpgrades.add(RefitSchematic.roll(random));
        }
        List<ItemKind> kindsWithBasics = sectionKindsWithBasics();
        for (ItemKind kind : kindsWithBasics) {
            ItemDrops.rollTargeted(rule, random, chances.targeted(kind), chances.rarity(), kindsWithBasics.size(), kind, droppedItems);
        }
        return new SectionItemLoot(droppedItems, droppedUpgrades);
    }

    static List<ItemKind> sectionKindsWithBasics() {
        return sectionKindsWithBasics(definition -> true);
    }

    private static List<ItemKind> sectionKindsWithBasics(Predicate<ItemDefinition> filter) {
        List<ItemKind> kindsWithBasics = new ArrayList<>();
        for (ItemKind kind : ItemKind.sectionItemTypes()) {
            if (ItemDrops.hasDroppableBasic(kind, filter)) {
                kindsWithBasics.add(kind);
            }
        }
        return kindsWithBasics;
    }

    public static ItemDefinition pickSectionItemBasic(Random random) {
        return pickSectionItemBasic(random, definition -> true);
    }

    public static ItemDefinition pickSectionItemBasic(Random random, Predicate<ItemDefinition> filter) {
        List<ItemKind> typesWithBasics = sectionKindsWithBasics(filter);
        return typesWithBasics.isEmpty() ? null
                : ItemDrops.pickSectionItemBasic(typesWithBasics.get(random.nextInt(typesWithBasics.size())), random, filter);
    }
}
