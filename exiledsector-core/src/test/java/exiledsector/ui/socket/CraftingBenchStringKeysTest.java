package exiledsector.ui.socket;

import exiledsector.i18n.RealCatalogue;
import exiledsector.items.CorruptionOutcome;
import exiledsector.items.Nanoswarm;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ItemNature;
import exiledsector.items.MaterialItems;
import exiledsector.items.SlottedRows;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftingBenchStringKeysTest {

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh_CN"})
    void everyKeyTheCraftingBenchBuildsForAKernelOrOutcomeExists(String language) throws Exception {
        JSONObject catalogue = new JSONObject(RealCatalogue.entries(language));
        List<String> missing = new ArrayList<>();
        for (String itemId : MaterialItems.ids()) {
            require(catalogue, "material." + itemId + ".name", missing);
            require(catalogue, "material." + itemId + ".description", missing);
        }
        for (Nanoswarm craftingItem : Nanoswarm.values()) {
            require(catalogue, HullCraftingBench.INSTANCE.craftingItemDescriptionKey(craftingItem), missing);
            if (craftingItem == Nanoswarm.CORRUPTION) {
                for (ItemNature nature : ItemNature.values()) {
                    for (CorruptionOutcome outcome : shownOutcomes(nature)) {
                        require(catalogue, HullCraftingBench.usedNoticeKey(craftingItem, outcome, nature), missing);
                    }
                }
            } else {
                for (ItemNature nature : ItemNature.values()) {
                    require(catalogue, HullCraftingBench.usedNoticeKey(craftingItem, null, nature), missing);
                }
            }
        }
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            require(catalogue, InhibitorTrayEntry.descriptionKey(inhibitor), missing);
            require(catalogue, "ui.craftingBench.notice.inhibitorOn." + inhibitor.name().toLowerCase(java.util.Locale.ROOT), missing);
        }
        require(catalogue, InhibitorTray.INSTANCE.titleKey(), missing);
        require(catalogue, InhibitorTray.INSTANCE.emptyKey(), missing);
        for (ModifierSlot slot : List.of(ModifierSlot.PREFIX, ModifierSlot.SUFFIX)) {
            require(catalogue, SlottedRows.slotTagKey(slot), missing);
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
