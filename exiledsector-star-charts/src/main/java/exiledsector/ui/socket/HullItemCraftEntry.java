package exiledsector.ui.socket;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftCost;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.HullItemCraft;
import exiledsector.items.Item;
import exiledsector.items.MaterialItems;
import exiledsector.items.SocketCustody;
import exiledsector.items.UseVerdict;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.cargo.CargoIconLooks;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

abstract class HullItemCraftEntry implements InfusedCraftingTray.CraftRow {

    abstract HullItemCraft craft();

    abstract String keyPrefix();

    abstract String costText(BenchTrayContext context, String key);

    @Override
    public abstract StyledText rowText(BenchTrayContext context);

    CraftCost cost() {
        return craft().cost();
    }

    @Override
    public int feedstockAmount() {
        return cost().feedstockAmount();
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
        return String.valueOf(cost().feedstockAmount());
    }

    @Override
    public List<RowIcon> rowIcons(BenchTrayContext context) {
        String badge = badge(context);
        CraftCost cost = cost();
        List<RowIcon> icons = new ArrayList<>(cost.feedstockIds().stream()
                .map(feedstockId -> RowIcon.material(feedstockId, badge)).toList());
        if (cost.reshapingCost() > 0) {
            icons.add(RowIcon.material(CraftCost.reshapingId(), String.valueOf(cost.reshapingCost())));
        }
        return List.copyOf(icons);
    }

    @Override
    public String searchText(BenchTrayContext context) {
        String feedstockNames = cost().feedstockIds().stream().map(MaterialItems::displayName).collect(Collectors.joining(" "));
        return rowText(context).plain() + " " + feedstockNames;
    }

    @Override
    public boolean available(BenchTrayContext context) {
        return craft().verdict(context.loaded(), inhibitors(context)).allowed() && craft().canAfford(context.materials());
    }

    @Override
    public void use(BenchTrayContext context, boolean skipConfirm) {
        if (context.droppedMissingInhibitors()) {
            return;
        }
        Item item = context.loaded();
        Set<CraftingInhibitor> inhibitors = inhibitors(context);
        UseVerdict verdict = craft().verdict(item, inhibitors);
        if (!verdict.allowed()) {
            context.notice(refusalText(verdict, item));
            return;
        }
        if (!craft().canAfford(context.materials())) {
            context.notice(costText(context, notEnoughKey()));
            return;
        }
        if (skipConfirm) {
            apply(context);
            return;
        }
        String title = Translation.msg("ui.craftingBench.confirm.useTitle").arg("name", craftName()).arg("target", item.name()).text();
        StyledText body = rowText(context).append(" ").append(costText(context, keyPrefix() + "cost"));
        context.confirmCraft(title, body, craft().inhibitorsSpent(item, inhibitors), () -> apply(context));
    }

    private void apply(BenchTrayContext context) {
        if (context.droppedMissingInhibitors()) {
            return;
        }
        Item item = context.loaded();
        String nameBefore = item == null ? "" : item.name();
        Optional<Set<CraftingInhibitor>> spent = craft().use(item, context.materials(), inhibitors(context), context.random());
        if (spent.isEmpty()) {
            UseVerdict verdict = craft().verdict(item, inhibitors(context));
            context.notice(verdict.allowed() ? costText(context, notEnoughKey()) : refusalText(verdict, item));
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

    private String notEnoughKey() {
        return keyPrefix() + "notice.notEnough";
    }

    private String craftName() {
        return Translation.text(keyPrefix() + craft().id() + ".name");
    }

    private String refusalText(UseVerdict verdict, Item item) {
        return verdict.text(craftName(), item == null ? "" : item.name());
    }
}
