package exiledsector.tools;

import exiledsector.i18n.HeadlessGameText;
import exiledsector.i18n.I18n;
import exiledsector.i18n.RealCatalogue;
import exiledsector.ExiledSector;
import exiledsector.charts.StarChartsDomain;
import exiledsector.items.HullUpgradesDomain;
import exiledsector.skills.SkillTreeDomain;
import exiledsector.skills.VanillaHullModLines;
import exiledsector.skills.npc.RealSkillData;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EditorDescribeServer {

    private EditorDescribeServer() {
    }

    public static void main(String[] args) throws Exception {
        PrintStream protocolOut = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        System.setOut(System.err);
        ExiledSector.install(List.of(new SkillTreeDomain(), new HullUpgradesDomain(), new StarChartsDomain()));
        try (HeadlessGameText ignored = HeadlessGameText.install(() -> true)) {
            I18n.install(RealCatalogue.english());
            RealSkillData.load();
            if (args.length > 0 && Files.isRegularFile(Path.of(args[0], "data/hullmods/hull_mods.csv"))) {
                VanillaHullModLines.install(VanillaHullModCsv.read(Path.of(args[0])));
            }
            EditorDescriber describer = new EditorDescriber();
            BufferedReader requests = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            protocolOut.println("{\"ready\":true}");
            for (String requestLine = requests.readLine(); requestLine != null; requestLine = requests.readLine()) {
                protocolOut.println(describer.respond(requestLine));
            }
        }
    }
}
