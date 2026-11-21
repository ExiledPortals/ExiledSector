package exiledsector.items.drop;

import com.fs.starfarer.api.Global;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemUnlock;
import exiledsector.items.modifier.AffixLayout;
import exiledsector.items.schematic.MasterSchematic;
import exiledsector.items.schematic.RefitSchematic;
import exiledsector.items.schematic.ShipSchematic;
import exiledsector.loot.DropType;
import exiledsector.loot.Loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class HullItemDrops {

    public static final float HULL_ITEM_SALVAGE_SHARE = 0.5f;
    public static final AffixLayout.Limits DROP_LIMITS = new AffixLayout.Limits(2, 2, AffixLayout.MAX_PER_SIDE, List.of(0.64f));

    private HullItemDrops() {
    }

    public static List<ShipSchematic> rollMasterSchematic(String siteId, Random random, float chanceMult) {
        SalvageSiteDrops.Rule rule = SalvageSiteDrops.activeRule(siteId);
        return rule == null || rule.chances().isEmpty() ? List.of() : MasterSchematic.rollSalvage(rule.chances().get(0) * chanceMult, random);
    }

    public static Loot rollHullItemLoot(String siteId, Random random, ItemChances chances) {
        return rollHullItemLoot(siteId, random, chances, definition -> ItemUnlock.canDrop(definition, Global.getSector()));
    }

    public static Loot rollHullItemLoot(String siteId, Random random, ItemChances chances, Predicate<ItemDefinition> uniqueAllowed) {
        SalvageSiteDrops.Rule rule = SalvageSiteDrops.activeRule(siteId);
        if (rule == null || rule.chances().isEmpty()) {
            return Loot.NONE;
        }
        List<ItemData> cascadeItems = SalvageSiteDrops.rollCascade(rule, random, chances.quantity(DropType.HULL_ITEM), chances,
                DroppableDefinitions::pickHullItemBasic);
        cascadeItems.removeIf(item -> random.nextFloat() >= HULL_ITEM_SALVAGE_SHARE);
        List<ItemData> droppedItems = new ArrayList<>();
        for (ItemData item : cascadeItems) {
            droppedItems.add(uniqueOrCommon(item, siteId, null, rule, chances, random, uniqueAllowed));
        }
        List<ShipSchematic> droppedSchematics = new ArrayList<>();
        if (random.nextFloat() < rule.chances().get(0) * chances.quantity(DropType.SCHEMATIC) && RefitSchematic.dropsAllowed()) {
            droppedSchematics.add(RefitSchematic.roll(random));
        }
        List<ItemKind> kindsWithBasics = DroppableDefinitions.hullItemKindsWithBasics();
        for (ItemKind kind : kindsWithBasics) {
            List<ItemData> targetedItems = new ArrayList<>();
            SalvageSiteDrops.rollTargeted(rule, random, chances.extraOverParent(DropType.forItemKind(kind)) * HULL_ITEM_SALVAGE_SHARE, chances, kindsWithBasics.size(),
                    kind, targetedItems);
            for (ItemData item : targetedItems) {
                droppedItems.add(uniqueOrCommon(item, siteId, kind, rule, chances, random, uniqueAllowed));
            }
        }
        return new Loot(droppedItems, droppedSchematics, Map.of());
    }

    private static ItemData uniqueOrCommon(ItemData common, String siteId, ItemKind kind, SalvageSiteDrops.Rule rule, ItemChances chances,
                                           Random random, Predicate<ItemDefinition> uniqueAllowed) {
        Predicate<ItemDefinition> candidates = candidate -> candidate.isHullItem() && (kind == null || candidate.kind() == kind)
                && uniqueAllowed.test(candidate);
        ItemDefinition unique = UniqueDropRoll.rollConversion(random, rule.hullItemUniqueShareOfCascade(), chances,
                picker -> DroppableDefinitions.pickUniqueAtSite(siteId, picker, candidates));
        return unique == null ? common : ItemData.rolled(unique, random.nextLong());
    }
}
