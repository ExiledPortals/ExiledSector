package exiledsector.i18n;

import exiledsector.ExiledSectorModPlugin;
import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueLintTest {

    private static final Path REPORT_DIRECTORY = Path.of("target/i18n");
    private static final Set<String> REPORTED_LOCALES = Set.of(LocaleChain.SIMPLIFIED_CHINESE);
    private static final String HULL_MODS_FILE = "data/hullmods/hull_mods.csv";

    static Map<String, String> dataSources() throws IOException, JSONException {
        Path root = RealSkillData.projectRoot();
        Map<String, String> sources = new LinkedHashMap<>();
        JSONArray types = RealSkillData.readJson(root.resolve(RealSkillData.TYPES_FILE)).getJSONArray("skillTypes");
        for (int i = 0; i < types.length(); i++) {
            JSONObject type = types.getJSONObject(i);
            String id = type.getString("id");
            sources.put("skillType." + id + ".name", type.getString("name"));
            String description = type.optString("description", "");
            if (!description.isEmpty()) {
                sources.put("skillType." + id + ".description", description);
            }
        }
        String hullModRows = Files.readString(root.resolve(HULL_MODS_FILE), StandardCharsets.UTF_8).replace("\r\n", "\n");
        JSONArray hullMods = CDL.toJSONArray(hullModRows);
        for (int i = 0; i < hullMods.length(); i++) {
            JSONObject hullMod = hullMods.getJSONObject(i);
            String id = hullMod.optString("id");
            if (ExiledSectorModPlugin.LOCALISED_HULLMODS.contains(id)) {
                sources.put("hullmod." + id + ".name", hullMod.getString("name"));
                sources.put("hullmod." + id + ".description", hullMod.getString("desc"));
            }
        }
        return sources;
    }

    private static Set<String> localesOnDisk() throws IOException {
        Set<String> locales = new TreeSet<>();
        Path directory = RealSkillData.projectRoot().resolve(I18n.CATALOGUE_DIRECTORY);
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : files) {
                locales.add(file.getFileName().toString().replaceFirst("\\.json$", ""));
            }
        }
        return locales;
    }

    @Test
    void everyCatalogueFileIsStructurallySound() throws Exception {
        Map<String, String> english = RealCatalogue.entries(LocaleChain.ENGLISH);
        Map<String, String> sources = dataSources();
        List<String> problems = new ArrayList<>(CatalogueLint.englishProblems(english));
        for (String locale : localesOnDisk()) {
            String content = Files.readString(RealCatalogue.file(locale), StandardCharsets.UTF_8);
            problems.addAll(CatalogueLint.rawFileProblems(locale + ".json", content));
            if (!locale.equals(LocaleChain.ENGLISH)) {
                problems.addAll(CatalogueLint.translationProblems(locale, english, RealCatalogue.entries(locale), sources));
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void writesAReportOfUntranslatedStringsForEachLanguage() throws Exception {
        Map<String, String> english = RealCatalogue.entries(LocaleChain.ENGLISH);
        Map<String, String> sources = dataSources();
        Set<String> locales = new TreeSet<>(REPORTED_LOCALES);
        locales.addAll(localesOnDisk());
        locales.remove(LocaleChain.ENGLISH);
        Path reports = RealSkillData.projectRoot().resolve(REPORT_DIRECTORY);
        Files.createDirectories(reports);
        for (String locale : locales) {
            Set<String> missing = CatalogueLint.missing(english, RealCatalogue.entries(locale), sources);
            Path report = reports.resolve("missing-" + locale + ".txt");
            Files.writeString(report, missing.isEmpty() ? "" : String.join("\n", missing) + "\n", StandardCharsets.UTF_8);
            assertTrue(Files.isRegularFile(report));
        }
    }

    @Test
    void flagsTranslationsWhosePlaceholdersOrTagsDifferFromEnglish() {
        Map<String, String> english = Map.of("a", "Increases {stat} by <good>{value}%</good>.");

        assertEquals(List.of(), CatalogueLint.translationProblems("zh_CN", english,
                Map.of("a", "{stat}提高<good>{value}%</good>。"), Map.of()));
        assertEquals(1, CatalogueLint.translationProblems("zh_CN", english,
                Map.of("a", "{stat}提高<good>{amount}%</good>。"), Map.of()).size());
        assertEquals(1, CatalogueLint.translationProblems("zh_CN", english,
                Map.of("a", "{stat}提高<bad>{value}%</bad>。"), Map.of()).size());
        assertEquals(1, CatalogueLint.translationProblems("zh_CN", english,
                Map.of("a", "{stat}提高{value}%。"), Map.of()).size());
    }

    @Test
    void flagsUnbalancedTagsOrphanKeysAndPercentSignsInSettings() {
        Map<String, String> english = Map.of("settings.x.name", "Chance", "b", "<hl>b</hl>");

        assertEquals(List.of(), CatalogueLint.englishProblems(english));
        assertEquals(1, CatalogueLint.englishProblems(Map.of("b", "<hl>b")).size());
        assertEquals(1, CatalogueLint.englishProblems(Map.of("settings.x.name", "Chance (%)")).size());
        assertEquals(1, CatalogueLint.translationProblems("zh_CN", english, Map.of("gone", "x"), Map.of()).size());
        assertEquals(1, CatalogueLint.translationProblems("zh_CN", english, Map.of("settings.x.name", "几率(%)"), Map.of()).size());
    }

    @Test
    void flagsDistancesWrittenWithoutASpaceBeforeSu() {
        assertEquals(List.of(), CatalogueLint.englishProblems(Map.of("a", "Within {range} su and 1000 su/second; a subsystem.")));
        assertEquals(1, CatalogueLint.englishProblems(Map.of("b", "Within {range}su.")).size());
        assertEquals(1, CatalogueLint.englishProblems(Map.of("c", "Within 1500su.")).size());
    }

    @Test
    void metadataKeysNeedNoEnglishSource() {
        assertEquals(List.of(), CatalogueLint.translationProblems("zh_CN", Map.of(),
                Map.of("meta.font", "graphics/fonts/exiledSector/notosanssc.fnt", "meta.fakeBold", "false"), Map.of()));
    }

    @Test
    void checksDataTranslationsAgainstTheEnglishDataText() {
        Map<String, String> sources = Map.of("skillType.armor.description", "Adds <good>armor</good>.");

        assertEquals(List.of(), CatalogueLint.translationProblems("zh_CN", Map.of(),
                Map.of("skillType.armor.description", "增加<good>装甲</good>。"), sources));
        assertEquals(1, CatalogueLint.translationProblems("zh_CN", Map.of(),
                Map.of("skillType.armor.description", "增加装甲。"), sources).size());
    }

    @Test
    void pluralFormsCompareAgainstTheEnglishOtherForm() {
        Map<String, String> english = Map.of("n.one", "{count} node", "n.other", "{count} nodes");

        assertEquals(List.of(), CatalogueLint.englishProblems(english));
        assertEquals(1, CatalogueLint.englishProblems(Map.of("n.one", "{count} node")).size());
        assertEquals(List.of(), CatalogueLint.translationProblems("zh_CN", english, Map.of("n.other", "{count}个节点"), Map.of()));
        assertEquals(Set.of(), CatalogueLint.missing(english, Map.of("n.other", "{count}个节点"), Map.of()));
    }

    @Test
    void reportsMissingStringsAndDataText() {
        Set<String> missing = CatalogueLint.missing(Map.of("a", "A", "b", "B"), Map.of("a", "甲"),
                Map.of("skillType.x.name", "X"));

        assertEquals(Set.of("b", "skillType.x.name"), missing);
    }

    @Test
    void rejectsByteOrderMarksAndHashSigns() {
        assertEquals(1, CatalogueLint.rawFileProblems("x.json", "﻿{}").size());
        assertEquals(1, CatalogueLint.rawFileProblems("x.json", "{\"a\":\"#1\"}").size());
        assertEquals(List.of(), CatalogueLint.rawFileProblems("x.json", "{\"a\":\"b\"}"));
    }
}
