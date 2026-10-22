package exiledsector.tools;

import exiledsector.items.CraftingCurrency;
import exiledsector.items.ItemCurrency;
import exiledsector.items.StarChartCurrency;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorCraftingItemsTest {

    private static final Path CRAFTING_MODEL = Path.of("tools/editor/src/pages/items/model/crafting.ts");
    private static final Pattern CRAFTING_ITEM = Pattern.compile("\\{\\s*id:\\s*'([^']+)',[^}]*?\\bcost:\\s*(true|false)([^}]*)}");

    @Test
    void theCraftingTabListsEveryCurrencyWithTheGameSynthesisRule() throws IOException {
        Map<String, Boolean> editorCosts = new LinkedHashMap<>();
        Map<String, Boolean> editorStarChartFlags = new LinkedHashMap<>();
        Matcher item = CRAFTING_ITEM.matcher(Files.readString(CRAFTING_MODEL, StandardCharsets.UTF_8));
        while (item.find()) {
            editorCosts.put(item.group(1), Boolean.parseBoolean(item.group(2)));
            editorStarChartFlags.put(item.group(1), item.group(3).contains("starChart: true"));
        }
        List<String> problems = new ArrayList<>();
        Stream.<ItemCurrency>concat(Stream.of(CraftingCurrency.values()), Stream.of(StarChartCurrency.values())).forEach(currency -> {
            Boolean editorCost = editorCosts.get(currency.itemId());
            if (editorCost == null) {
                problems.add(currency.itemId() + " is missing from CRAFTING_ITEMS");
            } else if (editorCost != currency.synthesisable()) {
                problems.add(currency.itemId() + " has cost: " + editorCost + " but synthesisable() is " + currency.synthesisable());
            }
        });
        for (StarChartCurrency cipher : StarChartCurrency.values()) {
            if (!Boolean.TRUE.equals(editorStarChartFlags.get(cipher.itemId()))) {
                problems.add(cipher.itemId() + " should be marked starChart: true");
            }
        }
        assertTrue(problems.isEmpty(), "Update CRAFTING_ITEMS in " + CRAFTING_MODEL + ":\n" + String.join("\n", problems));
    }
}
