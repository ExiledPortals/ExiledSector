package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.StyledText;
import exiledsector.items.CraftingMaterialFamily;
import exiledsector.items.ItemDefinition;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.List;

public interface BenchTrayEntry {

    record RowIcon(String iconPath, CargoIconLook look, String badge, String materialId) {

        public static RowIcon material(String materialId, String badge) {
            return new RowIcon(CraftingItemTrayEntry.iconPath(materialId), CargoIconLooks.forMaterial(materialId), badge, materialId);
        }
    }

    List<String> iconPaths();

    CargoIconLook look();

    String badge(BenchTrayContext context);

    default List<RowIcon> rowIcons(BenchTrayContext context) {
        return List.of(new RowIcon(shownIcons(iconPaths()).get(0), look(), badge(context), null));
    }

    static List<String> shownIcons(List<String> iconPaths) {
        return iconPaths == null || iconPaths.isEmpty() ? List.of(ItemDefinition.FALLBACK_ICON) : iconPaths;
    }

    default StyledText rowText(BenchTrayContext context) {
        return StyledText.EMPTY;
    }

    default String searchText(BenchTrayContext context) {
        return rowText(context).plain();
    }

    default boolean available(BenchTrayContext context) {
        return true;
    }

    default boolean lit(BenchTrayContext context) {
        return false;
    }

    default boolean hasTooltip() {
        return true;
    }

    default void writeTooltip(TooltipMakerAPI tooltip, BenchTrayContext context) {
    }

    void use(BenchTrayContext context, boolean skipConfirm);

    default void useAlternate(BenchTrayContext context, boolean skipConfirm) {
    }

    static BenchTrayEntry craftingItem(CraftingMaterialFamily craftingItem) {
        return new CraftingItemTrayEntry(craftingItem);
    }
}
