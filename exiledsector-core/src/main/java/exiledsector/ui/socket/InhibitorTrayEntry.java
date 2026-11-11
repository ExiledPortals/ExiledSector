package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.MaterialItems;
import exiledsector.ui.TooltipTextSize;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.List;

record InhibitorTrayEntry(CraftingInhibitor inhibitor) implements BenchTrayEntry {

    static String descriptionKey(CraftingInhibitor inhibitor) {
        return MaterialItems.descriptionKey(inhibitor.itemId());
    }

    @Override
    public List<String> iconPaths() {
        return List.of(CraftingItemTrayEntry.iconPath(inhibitor.itemId()));
    }

    @Override
    public CargoIconLook look() {
        return CargoIconLooks.forMaterial(inhibitor.itemId());
    }

    @Override
    public String badge(BenchTrayContext context) {
        return String.valueOf(context.materials().count(inhibitor.itemId()));
    }

    @Override
    public boolean lit(BenchTrayContext context) {
        return context.activeInhibitors().contains(inhibitor);
    }

    @Override
    public void writeTooltip(TooltipMakerAPI tooltip, BenchTrayContext context) {
        TooltipTextSize.applyNameFont(tooltip);
        VanillaText.plainPara(tooltip, MaterialItems.displayName(inhibitor.itemId()), 0f, inhibitor.color());
        TooltipTextSize.applyBodyFont(tooltip);
        VanillaText.addPara(tooltip, Translation.styled(descriptionKey(inhibitor)), HoloPanel.GAP, Misc.getTextColor());
        int ownedCount = context.materials().count(inhibitor.itemId());
        VanillaText.plainPara(tooltip, Translation.msg("ui.craftingBench.tile.inStorage").arg("count", ownedCount).text(), HoloPanel.GAP,
                Misc.getGrayColor());
        if (lit(context)) {
            VanillaText.plainPara(tooltip, Translation.text("ui.craftingBench.inhibitor.on"), HoloPanel.GAP, Misc.getGrayColor());
        } else if (ownedCount > 0) {
            VanillaText.plainPara(tooltip, Translation.text("ui.craftingBench.inhibitor.off"), HoloPanel.GAP, Misc.getGrayColor());
        }
        VanillaText.plainPara(tooltip, CraftingBenchPanel.growHint(inhibitor), HoloPanel.GAP, Misc.getGrayColor());
    }

    @Override
    public void use(BenchTrayContext context, boolean skipConfirm) {
        context.toggleInhibitor(inhibitor);
    }

    @Override
    public void useAlternate(BenchTrayContext context, boolean skipConfirm) {
        context.growMaterial(inhibitor, skipConfirm);
    }
}
