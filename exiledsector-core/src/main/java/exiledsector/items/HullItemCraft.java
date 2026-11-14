package exiledsector.items;

import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public interface HullItemCraft {

    String id();

    CraftCost cost();

    UseVerdict verdict(Item item, Collection<CraftingInhibitor> inhibitors);

    Set<CraftingInhibitor> inhibitorsSpent(Item item, Collection<CraftingInhibitor> inhibitors);

    boolean changeModifiers(Item item, Collection<CraftingInhibitor> inhibitors, Random random);

    default boolean canAfford(CraftingMaterials materials) {
        CraftCost cost = cost();
        return cost != null && cost.canAfford(materials);
    }

    default Optional<Set<CraftingInhibitor>> use(Item item, CraftingMaterials materials, Collection<CraftingInhibitor> inhibitors, Random random) {
        if (!canAfford(materials) || inhibitors.stream().anyMatch(inhibitor -> materials.count(inhibitor.itemId()) < 1)
                || !verdict(item, inhibitors).allowed()) {
            return Optional.empty();
        }
        Set<CraftingInhibitor> spent = inhibitorsSpent(item, inhibitors);
        if (!changeModifiers(item, inhibitors, random)) {
            return Optional.empty();
        }
        cost().takeFrom(materials);
        spent.forEach(inhibitor -> materials.take(inhibitor.itemId(), 1));
        return Optional.of(spent);
    }
}
