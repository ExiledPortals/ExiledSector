package exiledsector.charts.anomaly.forge;

import exiledsector.i18n.Translation;
import exiledsector.items.ForgeChoice;
import exiledsector.items.ForgeOperation;
import exiledsector.items.Item;

public final class ForgeTexts {

    static final String KEY_PREFIX = "chart.forge.";

    private ForgeTexts() {
    }

    public static String programName(ForgePrograms.Program program) {
        return Translation.text(KEY_PREFIX + "program." + program.id() + ".name");
    }

    public static String programDescription(ForgePrograms.Program program) {
        return Translation.text(KEY_PREFIX + "program." + program.id() + ".description");
    }

    public static String costText(ForgeRules.Cost cost) {
        return Translation.msg(KEY_PREFIX + "cost").arg("metals", cost.metals()).arg("rareMetals", cost.rareMetals()).arg("feedstock", cost.feedstock())
                .text();
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

    public static String choiceLabel(Item item, ForgeChoice choice, ForgeOperation operation) {
        if (choice.part() != null) {
            return partName(choice.part());
        }
        if (choice.modifier() == null || item == null) {
            return "";
        }
        String modifierText = Translation.list(item.explicitLines(choice.modifier())).plain();
        if (operation != ForgeOperation.ANCHOR_REROLL) {
            return modifierText;
        }
        return Translation.msg(KEY_PREFIX + "keep").arg("modifier", modifierText).text();
    }
}
