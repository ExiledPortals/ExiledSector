package exiledsector.ui.socket;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.BiomeCraft;
import exiledsector.items.HullItemCraft;

final class BiomeCraftEntry extends HullItemCraftEntry {

    private final BiomeCraft craft;

    BiomeCraftEntry(BiomeCraft craft) {
        this.craft = craft;
    }

    @Override
    HullItemCraft craft() {
        return craft;
    }

    @Override
    String keyPrefix() {
        return "chart.biomeCraft.";
    }

    @Override
    public InfusedCraftingTray.Action action() {
        return craft == BiomeCraft.GRAFTING ? InfusedCraftingTray.Action.BIOME_GRAFT : InfusedCraftingTray.Action.BIOME_REFORGE;
    }

    @Override
    public StyledText rowText(BenchTrayContext context) {
        return Translation.msg(keyPrefix() + craft.id() + ".row").styled();
    }

    @Override
    String costText(BenchTrayContext context, String key) {
        return Translation.msg(key).count(cost().reshapingCost()).arg("cost", cost().feedstockAmount()).text();
    }
}
