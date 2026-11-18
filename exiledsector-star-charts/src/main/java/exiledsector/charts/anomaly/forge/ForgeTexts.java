package exiledsector.charts.anomaly.forge;

import exiledsector.i18n.I18n;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.crafting.ForgeChoice;
import exiledsector.items.crafting.ForgeOperation;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.items.crafting.material.InfusedFeedstock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ForgeTexts {

    static final String KEY_PREFIX = "chart.anomaly.derelict_forge.";

    private ForgeTexts() {
    }

    public static String programName(ForgePrograms.Program program) {
        return Translation.text(KEY_PREFIX + "program." + program.id() + ".name");
    }

    public static String programDescription(ForgePrograms.Program program) {
        return Translation.text(KEY_PREFIX + "program." + program.id() + ".description");
    }

    static String routeKey(ForgePrograms.Program program) {
        return KEY_PREFIX + "route." + program.id();
    }

    public static String costText(ForgeRules.Cost cost) {
        List<String> parts = new ArrayList<>();
        addCostPart(parts, "feedstock", cost.feedstock());
        addCostPart(parts, "infusedFeedstock", cost.infusedFeedstockEach());
        if (parts.isEmpty()) {
            return Translation.text(KEY_PREFIX + "cost.free");
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        String rest = String.join(Translation.text(KEY_PREFIX + "cost.sep"), parts.subList(0, parts.size() - 1));
        return Translation.msg(KEY_PREFIX + "cost.and").arg("rest", rest).arg("last", parts.get(parts.size() - 1)).text();
    }

    private static void addCostPart(List<String> parts, String material, int amount) {
        if (amount > 0) {
            parts.add(Translation.msg(KEY_PREFIX + "cost." + material).arg("count", amount).text());
        }
    }

    static StyledText stockLine(CraftingMaterials materials, boolean withInfusedFeedstock) {
        return I18n.forGameText(() -> {
            Message stock = Translation.msg(KEY_PREFIX + (withInfusedFeedstock ? "stock.infused" : "stock"))
                    .arg("feedstock", held(materials, ChartForge.FEEDSTOCK_ITEM_ID));
            for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
                stock.arg(feedstock.name().toLowerCase(Locale.ROOT), held(materials, feedstock.itemId()));
            }
            return stock.styled();
        });
    }

    private static int held(CraftingMaterials materials, String itemId) {
        return materials == null ? 0 : materials.count(itemId);
    }

    public static String partName(String part) {
        return Translation.text(KEY_PREFIX + "part." + part);
    }

    public static String costLine(ForgeRules.Cost cost) {
        return Translation.msg(KEY_PREFIX + "costLine").arg("cost", costText(cost)).text();
    }

    public static String choicePrompt(ForgeOperation operation) {
        return Translation.text(KEY_PREFIX + "choose." + operation.id());
    }

    public static String choiceLabel(Item item, ForgeChoice choice) {
        if (choice.part() != null) {
            return partName(choice.part());
        }
        if (choice.modifier() == null || item == null) {
            return "";
        }
        return Translation.list(item.explicitLines(choice.modifier())).plain();
    }
}
