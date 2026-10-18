package exiledsector.items;

import java.awt.Color;
import java.util.Random;

public interface ItemCurrency {

    String itemId();

    String key();

    float dropWeight();

    MaterialTier tier();

    Color color();

    boolean synthesisable();

    int partsCost();

    boolean appliesTo(ItemType type);

    boolean canUse(Item item, CraftingContext context);

    Item use(Item item, CraftingContext context, Random random);

    default boolean respectsWards() {
        return false;
    }

    default boolean wouldChange(Item item, RolledModifier rolled) {
        return false;
    }

    default boolean createsCopy() {
        return false;
    }

    default boolean mayChangeDefinition() {
        return false;
    }
}
