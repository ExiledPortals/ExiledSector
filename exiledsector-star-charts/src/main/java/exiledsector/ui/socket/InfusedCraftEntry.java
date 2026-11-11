package exiledsector.ui.socket;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ForgeChoice;
import exiledsector.items.InfusedCraft;
import exiledsector.items.InfusedCraftCosts;
import exiledsector.items.Item;
import exiledsector.items.MaterialItems;
import exiledsector.items.SocketCustody;
import exiledsector.items.UseVerdict;
import exiledsector.skills.tags.ThemeTags;
import exiledsector.ui.VanillaText;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

record InfusedCraftEntry(InfusedCraft craft, String tag) implements BenchTrayEntry {

    InfusedCraftCosts.Cost cost() {
        return craft.cost(tag);
    }

    @Override
    public List<String> iconPaths() {
        return List.of(CraftingItemTrayEntry.iconPath(cost().feedstockId()));
    }

    @Override
    public CargoIconLook look() {
        return CargoIconLooks.forMaterial(cost().feedstockId());
    }

    @Override
    public String badge(BenchTrayContext context) {
        return String.valueOf(cost().amount());
    }

    @Override
    public StyledText rowText(BenchTrayContext context) {
        return Translation.msg("ui.infusedCraft." + craft.id() + ".row").arg("article", InfusedCraft.article(tag))
                .arg("tag", ThemeTags.displayName(tag)).styled();
    }

    @Override
    public String searchText(BenchTrayContext context) {
        return rowText(context).plain() + " " + feedstockName();
    }

    @Override
    public boolean available(BenchTrayContext context) {
        return craft.verdict(context.loaded(), tag, inhibitors(context)).allowed() && craft.canAfford(tag, context.materials());
    }

    @Override
    public void writeTooltip(TooltipMakerAPI tooltip, BenchTrayContext context) {
        VanillaText.plainPara(tooltip, craftName(), 0f, Misc.getBasePlayerColor());
        VanillaText.addPara(tooltip, rowText(context), HoloPanel.LINE_PAD * 2f, Misc.getTextColor());
        VanillaText.plainPara(tooltip, costText(context, "ui.infusedCraft.cost"), HoloPanel.LINE_PAD * 2f, Misc.getGrayColor());
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
        Optional<Set<CraftingInhibitor>> spent = craft.use(item, tag, ForgeChoice.WHOLE_ITEM, context.materials(), inhibitors(context),
                context.random());
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

    private String feedstockName() {
        return MaterialItems.displayName(cost().feedstockId());
    }

    private String costText(BenchTrayContext context, String key) {
        return Translation.msg(key).arg("cost", cost().amount()).arg("feedstock", feedstockName())
                .arg("held", context.materials().count(cost().feedstockId())).text();
    }

    private String refusalText(UseVerdict verdict, Item item) {
        return verdict.text(craftName(), item == null ? "" : item.name());
    }
}
