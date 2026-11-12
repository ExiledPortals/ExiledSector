package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.BiomeCraft;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.InfusedCraft;
import exiledsector.items.Item;
import exiledsector.items.MaterialItems;
import exiledsector.items.SocketCustody;
import exiledsector.items.UseVerdict;
import exiledsector.ui.TooltipTextSize;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

record BiomeCraftEntry(BiomeCraft craft) implements InfusedCraftingTray.CraftRow {

    @Override
    public int feedstockAmount() {
        return craft.feedstockCost();
    }

    @Override
    public InfusedCraftingTray.Action action() {
        return craft == BiomeCraft.GRAFTING ? InfusedCraftingTray.Action.BIOME_GRAFT : InfusedCraftingTray.Action.BIOME_REFORGE;
    }

    @Override
    public List<String> iconPaths() {
        return BiomeCraft.feedstockIds().stream().map(CraftingItemTrayEntry::iconPath).toList();
    }

    @Override
    public CargoIconLook look() {
        return CargoIconLooks.forMaterial(BiomeCraft.feedstockIds().get(0));
    }

    @Override
    public String badge(BenchTrayContext context) {
        return String.valueOf(craft.feedstockCost());
    }

    @Override
    public List<RowIcon> rowIcons(BenchTrayContext context) {
        String badge = badge(context);
        List<RowIcon> icons = new ArrayList<>(BiomeCraft.feedstockIds().stream()
                .map(feedstockId -> new RowIcon(CraftingItemTrayEntry.iconPath(feedstockId), CargoIconLooks.forMaterial(feedstockId), badge))
                .toList());
        String reshapingId = InfusedCraft.reshapingId();
        icons.add(new RowIcon(CraftingItemTrayEntry.iconPath(reshapingId), CargoIconLooks.forMaterial(reshapingId),
                String.valueOf(craft.reshapingCost())));
        return List.copyOf(icons);
    }

    @Override
    public StyledText rowText(BenchTrayContext context) {
        return Translation.msg("chart.biomeCraft." + craft.id() + ".row").styled();
    }

    @Override
    public String searchText(BenchTrayContext context) {
        String feedstockNames = BiomeCraft.feedstockIds().stream().map(MaterialItems::displayName).collect(Collectors.joining(" "));
        return rowText(context).plain() + " " + feedstockNames;
    }

    @Override
    public boolean available(BenchTrayContext context) {
        return craft.verdict(context.loaded(), inhibitors(context)).allowed() && craft.canAfford(context.materials());
    }

    @Override
    public void writeTooltip(TooltipMakerAPI tooltip, BenchTrayContext context) {
        TooltipTextSize.applyNameFont(tooltip);
        VanillaText.plainPara(tooltip, craftName(), 0f, Misc.getBasePlayerColor());
        TooltipTextSize.applyBodyFont(tooltip);
        VanillaText.addPara(tooltip, rowText(context), HoloPanel.LINE_PAD * 2f, Misc.getTextColor());
        VanillaText.plainPara(tooltip, costText("chart.biomeCraft.cost"), HoloPanel.LINE_PAD * 2f, Misc.getTextColor());
        UseVerdict verdict = craft.verdict(context.loaded(), inhibitors(context));
        if (!verdict.allowed()) {
            VanillaText.plainPara(tooltip, refusalText(verdict, context.loaded()), HoloPanel.LINE_PAD * 2f, Misc.getNegativeHighlightColor());
        }
    }

    @Override
    public void use(BenchTrayContext context, boolean skipConfirm) {
        if (context.droppedMissingInhibitors()) {
            return;
        }
        Item item = context.loaded();
        Set<CraftingInhibitor> inhibitors = inhibitors(context);
        UseVerdict verdict = craft.verdict(item, inhibitors);
        if (!verdict.allowed()) {
            context.notice(refusalText(verdict, item));
            return;
        }
        if (!craft.canAfford(context.materials())) {
            context.notice(costText("chart.biomeCraft.notice.notEnough"));
            return;
        }
        if (skipConfirm) {
            apply(context);
            return;
        }
        String title = Translation.msg("ui.craftingBench.confirm.useTitle").arg("name", craftName()).arg("target", item.name()).text();
        StyledText body = rowText(context).append(" ").append(costText("chart.biomeCraft.cost"));
        context.confirmCraft(title, body, craft.inhibitorsSpent(item, inhibitors), () -> apply(context));
    }

    private void apply(BenchTrayContext context) {
        if (context.droppedMissingInhibitors()) {
            return;
        }
        Item item = context.loaded();
        String nameBefore = item == null ? "" : item.name();
        Optional<Set<CraftingInhibitor>> spent = craft.use(item, context.materials(), inhibitors(context), context.random());
        if (spent.isEmpty()) {
            UseVerdict verdict = craft.verdict(item, inhibitors(context));
            context.notice(verdict.allowed() ? costText("chart.biomeCraft.notice.notEnough") : refusalText(verdict, item));
            return;
        }
        SocketCustody.markHostsChanged(item);
        String notice = Translation.msg("ui.infusedCraft.notice.crafted").arg("before", nameBefore).arg("name", item.name()).text();
        context.crafted(item, item.frameColor(), notice, spent.get());
    }

    private static Set<CraftingInhibitor> inhibitors(BenchTrayContext context) {
        Set<CraftingInhibitor> active = context.activeInhibitors();
        return active.isEmpty() ? EnumSet.noneOf(CraftingInhibitor.class) : EnumSet.copyOf(active);
    }

    private String craftName() {
        return Translation.text("chart.biomeCraft." + craft.id() + ".name");
    }

    private String costText(String key) {
        return Translation.msg(key).count(craft.reshapingCost()).arg("cost", craft.feedstockCost()).text();
    }

    private String refusalText(UseVerdict verdict, Item item) {
        return verdict.text(craftName(), item == null ? "" : item.name());
    }
}
