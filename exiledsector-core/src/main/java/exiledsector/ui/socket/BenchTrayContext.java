package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.CraftingMaterialFamily;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.GrowableMaterial;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;

import java.awt.Color;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public interface BenchTrayContext {

    Item loaded();

    ItemStore store();

    CraftingMaterials materials();

    Set<CraftingInhibitor> activeInhibitors();

    Random random();

    boolean busy();

    void useCraftingItem(CraftingMaterialFamily craftingItem, boolean skipConfirm);

    void growMaterial(GrowableMaterial material, boolean skipConfirm);

    void toggleInhibitor(CraftingInhibitor inhibitor);

    void writeCraftingItemTooltip(TooltipMakerAPI tooltip, CraftingMaterialFamily craftingItem);

    void confirm(float height, Consumer<TooltipMakerAPI> body, String confirmKey, Runnable confirmed);

    void changed(Item selectedAfter, Color flash, String notice);

    void notice(String text);
}
