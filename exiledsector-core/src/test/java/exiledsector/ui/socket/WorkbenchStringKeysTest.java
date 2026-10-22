package exiledsector.ui.socket;

import exiledlib.i18n.RealCatalogue;
import exiledlib.items.CorruptionOutcome;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledlib.items.ItemNature;
import exiledsector.items.MaterialItems;
import exiledlib.modifiers.ModifierSlot;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkbenchStringKeysTest {

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh_CN"})
    void everyKeyTheWorkbenchBuildsForAKernelOrOutcomeExists(String language) throws Exception {
        JSONObject catalogue = new JSONObject(RealCatalogue.entries(language));
        List<String> missing = new ArrayList<>();
        for (String itemId : MaterialItems.ids()) {
            require(catalogue, "material." + itemId + ".name", missing);
            require(catalogue, "material." + itemId + ".description", missing);
        }
        for (CraftingCurrency currency : CraftingCurrency.values()) {
            require(catalogue, SocketWorkbenchPanel.descriptionKey(currency), missing);
            if (currency == CraftingCurrency.CORRUPTION) {
                for (ItemNature nature : ItemNature.values()) {
                    for (CorruptionOutcome outcome : shownOutcomes(nature)) {
                        require(catalogue, SocketWorkbenchPanel.usedNoticeKey(currency, outcome, nature), missing);
                    }
                }
            } else {
                for (ItemNature nature : ItemNature.values()) {
                    require(catalogue, SocketWorkbenchPanel.usedNoticeKey(currency, null, nature), missing);
                }
            }
        }
        for (CraftingWard ward : CraftingWard.values()) {
            require(catalogue, SocketWorkbenchPanel.wardDescriptionKey(ward), missing);
            require(catalogue, "ui.workbench.notice.wardOn." + ward.name().toLowerCase(java.util.Locale.ROOT), missing);
        }
        for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
            require(catalogue, SocketWorkbenchPanel.slotTagKey(slot), missing);
        }
        assertEquals(List.of(), missing);
    }

    private static List<CorruptionOutcome> shownOutcomes(ItemNature nature) {
        if (nature != ItemNature.SOFTWARE) {
            return CorruptionOutcome.UNIQUE_HULL_ITEM_OUTCOMES;
        }
        return CorruptionOutcome.SUBROUTINE_OUTCOMES.stream().filter(outcome -> outcome != CorruptionOutcome.BECAME_RARE).toList();
    }

    private static void require(JSONObject catalogue, String key, List<String> missing) {
        if (!catalogue.has(key)) {
            missing.add(key);
        }
    }
}
