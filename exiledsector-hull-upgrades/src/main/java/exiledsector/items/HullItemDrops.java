package exiledsector.items;

import exiledsector.loot.DropType;
import exiledsector.loot.Loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class HullItemDrops {

    static final float HULL_ITEM_SALVAGE_SHARE = 0.5f;
    static final AffixLayout.Limits DROP_LIMITS = new AffixLayout.Limits(2, 2, AffixLayout.MAX_PER_SIDE, List.of(0.64f));

    private HullItemDrops() {
    }

    public static List<ShipSchematic> rollMasterSchematic(String siteId, Random random, float chanceMult) {
        SalvageSiteDrops.Rule rule = SalvageSiteDrops.activeRule(siteId);
        return rule == null || rule.chances().isEmpty() ? List.of() : MasterSchematic.rollSalvage(rule.chances().get(0) * chanceMult, random);
    }

    public static Loot rollHullItemLoot(String siteId, Random random, ItemChances chances) {
        SalvageSiteDrops.Rule rule = SalvageSiteDrops.activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return Loot.NONE;
        }
        List<ItemData> droppedItems = SalvageSiteDrops.rollCascade(rule, random, chances.quantity(DropType.HULL_ITEM), chances,
                DroppableDefinitions::pickHullItemBasic);
        droppedItems.removeIf(item -> random.nextFloat() >= HULL_ITEM_SALVAGE_SHARE);
        List<ShipSchematic> droppedSchematics = new ArrayList<>();
        if (random.nextFloat() < rule.chances().get(0) * chances.quantity(DropType.SCHEMATIC) && RefitSchematic.dropsAllowed()) {
            droppedSchematics.add(RefitSchematic.roll(random));
        }
        List<ItemKind> kindsWithBasics = DroppableDefinitions.hullItemKindsWithBasics();
        for (ItemKind kind : kindsWithBasics) {
            SalvageSiteDrops.rollTargeted(rule, random, chances.extraOverParent(DropType.forItemKind(kind)) * HULL_ITEM_SALVAGE_SHARE, chances, kindsWithBasics.size(),
                    kind, droppedItems);
        }
        return new Loot(droppedItems, droppedSchematics, Map.of());
    }
}
