package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.items.CraftingMaterialFamily;
import exiledsector.ui.cargo.CargoIconLook;

import java.util.List;

public interface BenchTrayEntry {

    List<String> iconPaths();

    CargoIconLook look();

    String badge(BenchTrayContext context);

    default boolean available(BenchTrayContext context) {
        return true;
    }

    default boolean lit(BenchTrayContext context) {
        return false;
    }

    void writeTooltip(TooltipMakerAPI tooltip, BenchTrayContext context);

    void use(BenchTrayContext context, boolean skipConfirm);

    default void useAlternate(BenchTrayContext context, boolean skipConfirm) {
    }

    static BenchTrayEntry craftingItem(CraftingMaterialFamily craftingItem) {
        return new CraftingItemTrayEntry(craftingItem);
    }
}
