package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.items.CraftingMaterialFamily;
import exiledsector.items.ItemDefinition;
import exiledsector.items.MaterialItems;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.List;

record CraftingItemTrayEntry(CraftingMaterialFamily craftingItem) implements BenchTrayEntry {

    @Override
    public List<String> iconPaths() {
        return List.of(iconPath(craftingItem.itemId()));
    }

    static String iconPath(String itemId) {
        String iconPath = MaterialItems.loadedIconPath(itemId);
        return iconPath == null ? ItemDefinition.FALLBACK_ICON : iconPath;
    }

    @Override
    public CargoIconLook look() {
        return CargoIconLooks.forMaterial(craftingItem.itemId());
    }

    @Override
    public String badge(BenchTrayContext context) {
        return String.valueOf(context.materials().count(craftingItem.itemId()));
    }

    @Override
    public boolean available(BenchTrayContext context) {
        return context.materials().count(craftingItem.itemId()) > 0;
    }

    @Override
    public void writeTooltip(TooltipMakerAPI tooltip, BenchTrayContext context) {
        context.writeCraftingItemTooltip(tooltip, craftingItem);
    }

    @Override
    public void use(BenchTrayContext context, boolean skipConfirm) {
        context.useCraftingItem(craftingItem, skipConfirm);
    }

    @Override
    public void useAlternate(BenchTrayContext context, boolean skipConfirm) {
        context.growMaterial(craftingItem, skipConfirm);
    }
}
