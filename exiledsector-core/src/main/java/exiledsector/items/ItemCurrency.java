package exiledsector.items;

import exiledsector.i18n.Translation;

import java.awt.Color;
import java.util.Locale;
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

    default boolean dropsFromShipLoot() {
        return appliesTo(ItemType.SUBROUTINE) || appliesTo(ItemType.HULL_ITEM);
    }

    default String refusalNoticeKey(Item item) {
        return null;
    }

    default String description() {
        return Translation.text(MaterialItems.descriptionKey(itemId()));
    }

    default String usedNoticeKey(ItemNature nature) {
        return "ui.workbench.notice.used." + key() + "." + nature.name().toLowerCase(Locale.ROOT);
    }
}
