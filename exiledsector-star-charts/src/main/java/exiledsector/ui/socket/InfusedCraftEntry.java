package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.InfusedCraft;
import exiledsector.items.InfusedCraftCosts;
import exiledsector.items.Item;
import exiledsector.items.MaterialItems;
import exiledsector.items.SocketCustody;
import exiledsector.items.UseVerdict;
import exiledsector.skills.tags.ThemeTags;
import exiledsector.ui.TooltipTextSize;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

record InfusedCraftEntry(InfusedCraft craft, String tag) implements InfusedCraftingTray.CraftRow {

    InfusedCraftCosts.Cost cost() {
        return craft.cost(tag);
    }

    @Override
    public int feedstockAmount() {
        return cost().amount();
    }

    @Override
    public InfusedCraftingTray.Action action() {
        return craft == InfusedCraft.ADD_AND_REMOVE ? InfusedCraftingTray.Action.EXCHANGE : InfusedCraftingTray.Action.REFORGE;
    }

    @Override
    public List<String> iconPaths() {
        return cost().feedstockIds().stream().map(CraftingItemTrayEntry::iconPath).toList();
    }

    @Override
    public CargoIconLook look() {
        return CargoIconLooks.forMaterial(cost().feedstockIds().get(0));
    }

    @Override
    public String badge(BenchTrayContext context) {
        return String.valueOf(cost().amount());
    }

    @Override
    public List<RowIcon> rowIcons(BenchTrayContext context) {
        String badge = badge(context);
        List<RowIcon> icons = new ArrayList<>(cost().feedstockIds().stream()
                .map(feedstockId -> new RowIcon(CraftingItemTrayEntry.iconPath(feedstockId), CargoIconLooks.forMaterial(feedstockId), badge))
                .toList());
        if (craft.reshapingCost() > 0) {
            String reshapingId = InfusedCraft.reshapingId();
            icons.add(new RowIcon(CraftingItemTrayEntry.iconPath(reshapingId), CargoIconLooks.forMaterial(reshapingId),
                    String.valueOf(craft.reshapingCost())));
        }
        return List.copyOf(icons);
    }

    @Override
    public StyledText rowText(BenchTrayContext context) {
        return Translation.msg("ui.infusedCraft." + craft.id() + ".row").arg("article", InfusedCraft.article(tag))
                .arg("tag", ThemeTags.displayName(tag)).styled();
    }

    @Override
    public String searchText(BenchTrayContext context) {
        String feedstockNames = cost().feedstockIds().stream().map(MaterialItems::displayName).collect(Collectors.joining(" "));
        return rowText(context).plain() + " " + feedstockNames;
    }

    @Override
    public boolean available(BenchTrayContext context) {
        return craft.verdict(context.loaded(), tag, inhibitors(context)).allowed() && craft.canAfford(tag, context.materials());
    }

    @Override
    public void writeTooltip(TooltipMakerAPI tooltip, BenchTrayContext context) {
        TooltipTextSize.applyNameFont(tooltip);
        VanillaText.plainPara(tooltip, craftName(), 0f, Misc.getBasePlayerColor());
        TooltipTextSize.applyBodyFont(tooltip);
        VanillaText.addPara(tooltip, rowText(context), HoloPanel.LINE_PAD * 2f, Misc.getTextColor());
        VanillaText.plainPara(tooltip, costText(context, "ui.infusedCraft.cost"), HoloPanel.LINE_PAD * 2f, Misc.getTextColor());
        UseVerdict verdict = craft.verdict(context.loaded(), tag, inhibitors(context));
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
        UseVerdict verdict = craft.verdict(item, tag, inhibitors);
        if (!verdict.allowed()) {
            context.notice(refusalText(verdict, item));
            return;
        }
        if (!craft.canAfford(tag, context.materials())) {
            context.notice(costText(context, "ui.infusedCraft.notice.notEnough"));
            return;
        }
        if (skipConfirm) {
            apply(context);
            return;
        }
        String title = Translation.msg("ui.craftingBench.confirm.useTitle").arg("name", craftName()).arg("target", item.name()).text();
        StyledText body = rowText(context).append(" ").append(costText(context, "ui.infusedCraft.cost"));
        context.confirmCraft(title, body, craft.inhibitorsSpent(item, tag, inhibitors), () -> apply(context));
    }

    private void apply(BenchTrayContext context) {
        if (context.droppedMissingInhibitors()) {
            return;
        }
        Item item = context.loaded();
        String nameBefore = item == null ? "" : item.name();
        Optional<Set<CraftingInhibitor>> spent = craft.use(item, tag, context.materials(), inhibitors(context), context.random());
        if (spent.isEmpty()) {
            UseVerdict verdict = craft.verdict(item, tag, inhibitors(context));
            context.notice(verdict.allowed() ? costText(context, "ui.infusedCraft.notice.notEnough") : refusalText(verdict, item));
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
        return Translation.text("ui.infusedCraft." + craft.id() + ".name");
    }

    private String costText(BenchTrayContext context, String key) {
        List<String> feedstockIds = cost().feedstockIds();
        return feedstockIds.size() > 1 ? multiFeedstockCostText(context, key + "Each", feedstockIds) : singleFeedstockCostText(context, key, feedstockIds.get(0));
    }

    private String singleFeedstockCostText(BenchTrayContext context, String key, String feedstockId) {
        int reshapingCost = craft.reshapingCost();
        Message message = reshapingCost <= 0 ? Translation.msg(key) : Translation.msg(key + "WithSwarms").count(reshapingCost)
                .arg("swarmsHeld", context.materials().count(InfusedCraft.reshapingId()));
        return message.arg("cost", cost().amount()).arg("feedstock", MaterialItems.displayName(feedstockId))
                .arg("held", context.materials().count(feedstockId)).text();
    }

    private String multiFeedstockCostText(BenchTrayContext context, String key, List<String> feedstockIds) {
        int reshapingCost = craft.reshapingCost();
        List<String> heldIds = new ArrayList<>(feedstockIds);
        if (reshapingCost > 0) {
            heldIds.add(InfusedCraft.reshapingId());
        }
        Message message = reshapingCost <= 0 ? Translation.msg(key) : Translation.msg(key + "WithSwarms").count(reshapingCost);
        return message.arg("cost", cost().amount()).arg("feedstock", listText(feedstockIds, MaterialItems::displayName))
                .arg("held", listText(heldIds, materialId -> Translation.msg("ui.infusedCraft.heldMaterial")
                        .arg("held", context.materials().count(materialId)).arg("name", MaterialItems.displayName(materialId)).text()))
                .text();
    }

    private static String listText(List<String> materialIds, Function<String, String> partText) {
        return Translation.list(materialIds.stream().map(materialId -> StyledText.of(partText.apply(materialId))).toList()).plain();
    }

    private String refusalText(UseVerdict verdict, Item item) {
        return verdict.text(craftName(), item == null ? "" : item.name());
    }
}
