package exiledsector.ui.socket;

import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.crafting.CraftCost;
import exiledsector.items.crafting.HullItemCraft;
import exiledsector.items.crafting.InfusedCraft;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.skills.tags.ThemeTags;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class InfusedCraftEntry extends HullItemCraftEntry {

    private final InfusedCraft craft;
    private final String tag;
    private final HullItemCraft taggedCraft;

    InfusedCraftEntry(InfusedCraft craft, String tag) {
        this.craft = craft;
        this.tag = tag;
        this.taggedCraft = craft.forTag(tag);
    }

    @Override
    HullItemCraft craft() {
        return taggedCraft;
    }

    @Override
    String keyPrefix() {
        return "ui.infusedCraft.";
    }

    @Override
    public InfusedCraftingTray.Action action() {
        return craft == InfusedCraft.ADD_AND_REMOVE ? InfusedCraftingTray.Action.EXCHANGE : InfusedCraftingTray.Action.REFORGE;
    }

    @Override
    public StyledText rowText(BenchTrayContext context) {
        return Translation.msg(keyPrefix() + craft.id() + ".row").arg("article", InfusedCraft.article(tag))
                .arg("tag", ThemeTags.displayName(tag)).styled();
    }

    @Override
    String costText(BenchTrayContext context, String key) {
        List<String> feedstockIds = cost().feedstockIds();
        return feedstockIds.size() > 1 ? multiFeedstockCostText(context, key + "Each", feedstockIds) : singleFeedstockCostText(context, key, feedstockIds.get(0));
    }

    private String singleFeedstockCostText(BenchTrayContext context, String key, String feedstockId) {
        CraftCost cost = cost();
        int reshapingCost = cost.reshapingCost();
        Message message = reshapingCost <= 0 ? Translation.msg(key) : Translation.msg(key + "WithSwarms").count(reshapingCost)
                .arg("swarmsHeld", context.materials().count(CraftCost.reshapingId()));
        return message.arg("cost", cost.feedstockAmount()).arg("feedstock", MaterialItems.displayName(feedstockId))
                .arg("held", context.materials().count(feedstockId)).text();
    }

    private String multiFeedstockCostText(BenchTrayContext context, String key, List<String> feedstockIds) {
        CraftCost cost = cost();
        int reshapingCost = cost.reshapingCost();
        List<String> heldIds = new ArrayList<>(feedstockIds);
        if (reshapingCost > 0) {
            heldIds.add(CraftCost.reshapingId());
        }
        Message message = reshapingCost <= 0 ? Translation.msg(key) : Translation.msg(key + "WithSwarms").count(reshapingCost);
        return message.arg("cost", cost.feedstockAmount()).arg("feedstock", listText(feedstockIds, MaterialItems::displayName))
                .arg("held", listText(heldIds, materialId -> Translation.msg("ui.infusedCraft.heldMaterial")
                        .arg("held", context.materials().count(materialId)).arg("name", MaterialItems.displayName(materialId)).text()))
                .text();
    }

    private static String listText(List<String> materialIds, Function<String, String> partText) {
        return Translation.list(materialIds.stream().map(materialId -> StyledText.of(partText.apply(materialId))).toList()).plain();
    }
}
