package exiledsector.console;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleCommandsCsvTest {

    private static final Path COMMANDS = Path.of("data/console/commands.csv");

    private static List<String> rows() throws IOException {
        List<String> lines = Files.readAllLines(COMMANDS, StandardCharsets.UTF_8);
        return lines.subList(1, lines.size()).stream().filter(line -> !line.isBlank()).toList();
    }

    @Test
    void everyListedCommandClassExists() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String row : rows()) {
            String className = row.split(",", 3)[1];
            try {
                Class.forName(className, false, ConsoleCommandsCsvTest.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                missing.add(className);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void theStarChartScreenCommandIsGoneBecauseTheItemsPanelHasAStarChartsTab() throws IOException {
        assertFalse(rows().stream().anyMatch(row -> row.startsWith("ExiledSectorStarCharts,")));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("exiledsector.console.StarChartsCommand", false, ConsoleCommandsCsvTest.class.getClassLoader()));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("exiledsector.ui.chart.StarChartScreen", false, ConsoleCommandsCsvTest.class.getClassLoader()));
        String grantRow = rows().stream().filter(row -> row.startsWith("ExiledSectorGrantStarCharts,")).findFirst().orElseThrow();
        assertTrue(grantRow.contains("Star Charts tab"), grantRow);
    }
}
