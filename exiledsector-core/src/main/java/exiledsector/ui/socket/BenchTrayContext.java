package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.i18n.StyledText;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.crafting.material.CraftingInhibitor;
import exiledsector.items.crafting.material.CraftingMaterialFamily;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.items.crafting.material.GrowableMaterial;

import java.awt.Color;
import java.util.Random;
import java.util.Set;

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

    boolean droppedMissingInhibitors();

    void confirmCraft(String title, StyledText body, Set<CraftingInhibitor> spentInhibitors, Runnable confirmed);

    void crafted(Item selectedAfter, Color flash, String notice, Set<CraftingInhibitor> spentInhibitors);

    void notice(String text);
}
