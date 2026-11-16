package exiledsector.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class HullItemDrops {

    public record SectionItemLoot(List<ItemData> items, List<ShipSchematic> upgrades) {

        public static final SectionItemLoot NONE = new SectionItemLoot(List.of(), List.of());

        public SectionItemLoot {
            items = List.copyOf(items);
            upgrades = List.copyOf(upgrades);
        }

        public boolean isEmpty() {
            return items.isEmpty() && upgrades.isEmpty();
        }
    }

    static final float SALVAGE_SHARE = 0.5f;
    static final AffixLayout.Limits DROP_LIMITS = new AffixLayout.Limits(2, 2, AffixLayout.MAX_PER_SIDE, List.of(0.64f));

    private HullItemDrops() {
    }

    public static List<ShipSchematic> rollMasterSchematic(String siteId, Random random, float chanceMult) {
        ItemDrops.Rule rule = ItemDrops.activeRule(siteId);
        return rule == null || rule.chances().isEmpty() ? List.of() : MasterSchematic.rollSalvage(rule.chances().get(0) * chanceMult, random);
    }

    public static SectionItemLoot rollHullItemLoot(String siteId, Random random, ItemChances chances, float basicItemMult) {
        ItemDrops.Rule rule = ItemDrops.activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return SectionItemLoot.NONE;
        }
        float basicShare = SALVAGE_SHARE * rule.hullItemShare();
        List<ItemData> droppedItems = ItemDrops.rollCascade(rule, random, chances.general() * basicItemMult, chances.rarity(),
                ItemDrops::pickHullItemBasic);
        droppedItems.removeIf(item -> random.nextFloat() >= basicShare);
        List<ShipSchematic> droppedUpgrades = new ArrayList<>();
        if (random.nextFloat() < rule.chances().get(0) * chances.general() && RefitSchematic.dropsAllowed()) {
            droppedUpgrades.add(RefitSchematic.roll(random));
        }
        List<ItemKind> kindsWithBasics = ItemDrops.hullItemKindsWithBasics();
        for (ItemKind kind : kindsWithBasics) {
            ItemDrops.rollTargeted(rule, random, chances.targeted(kind) * basicShare * basicItemMult, chances.rarity(), kindsWithBasics.size(), kind,
                    droppedItems);
        }
        return new SectionItemLoot(droppedItems, droppedUpgrades);
    }
}
