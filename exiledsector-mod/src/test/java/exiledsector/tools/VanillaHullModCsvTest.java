package exiledsector.tools;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.i18n.StyledText;
import exiledsector.skills.VanillaHullModLines;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VanillaHullModCsvTest {

    @Test
    void describesAHullModFromItsCsvTextAndItsScriptsParameters(@TempDir Path vanillaCoreDir) throws Exception {
        Path hullMods = vanillaCoreDir.resolve("data/hullmods/hull_mods.csv");
        Files.createDirectories(hullMods.getParent());
        Files.writeString(hullMods, "name,id,script,desc\r\n"
                + "Defensive Targeting Array,defensive_targeting_array,com.fs.starfarer.api.impl.hullmods.DefensiveTargetingArray,"
                + "\"Links are re-purposed.\r\n\r\nIncreases damage by %s.\"\r\n"
                + "Missing,missing_script,com.example.NoSuchHullMod,Text.\r\n", StandardCharsets.UTF_8);

        VanillaHullModLines.install(VanillaHullModCsv.read(vanillaCoreDir));
        try {
            List<String> lines = VanillaHullModLines.describe("defensive_targeting_array", HullSize.CRUISER).stream()
                    .map(StyledText::toMarkup).toList();

            assertEquals(List.of("Links are re-purposed.", "Increases damage by <hl>50%</hl>."), lines);
            assertEquals(List.of(), VanillaHullModLines.describe("missing_script", HullSize.CRUISER));
            assertNull(VanillaHullModCsv.read(vanillaCoreDir).descriptionFormat("unknown"));
        } finally {
            VanillaHullModLines.install(null);
        }
    }
}
