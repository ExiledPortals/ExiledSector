package exiledsector.ui.socket;

import exiledsector.items.CorruptionOutcome;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledsector.items.ItemNature;
import exiledsector.items.MaterialItems;
import exiledsector.skills.npc.RealSkillData;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkbenchStringKeysTest {

    @ParameterizedTest
    @ValueSource(strings = {"en", "zh_CN"})
    void everyKeyTheWorkbenchBuildsForAKernelOrOutcomeExists(String language) throws Exception {
        JSONObject catalogue = new JSONObject(Files.readString(
                RealSkillData.projectRoot().resolve("data/strings/exiledSector/" + language + ".json"), StandardCharsets.UTF_8));
        List<String> missing = new ArrayList<>();
        for (String itemId : MaterialItems.ids()) {
            require(catalogue, "material." + itemId + ".name", missing);
            require(catalogue, "material." + itemId + ".description", missing);
        }
        for (CraftingCurrency currency : CraftingCurrency.values()) {
            require(catalogue, SocketWorkbenchPanel.descriptionKey(currency), missing);
            if (currency == CraftingCurrency.CORRUPTION) {
                for (CorruptionOutcome outcome : CorruptionOutcome.NANOSWARM_OUTCOMES) {
                    for (ItemNature nature : ItemNature.values()) {
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
        assertEquals(List.of(), missing);
    }

    private static void require(JSONObject catalogue, String key, List<String> missing) {
        if (!catalogue.has(key)) {
            missing.add(key);
        }
    }
}
