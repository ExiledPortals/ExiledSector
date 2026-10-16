package exiledsector.tools;

import exiledsector.i18n.HeadlessGameText;
import exiledsector.i18n.I18n;
import exiledsector.i18n.RealCatalogue;
import exiledsector.skills.npc.RealSkillData;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class EditorDescribeServer {

    private EditorDescribeServer() {
    }

    public static void main(String[] args) throws Exception {
        PrintStream protocolOut = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        System.setOut(System.err);
        try (HeadlessGameText ignored = HeadlessGameText.install(() -> true)) {
            I18n.install(RealCatalogue.english());
            RealSkillData.load();
            EditorDescriber describer = new EditorDescriber();
            BufferedReader requests = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            protocolOut.println("{\"ready\":true}");
            for (String requestLine = requests.readLine(); requestLine != null; requestLine = requests.readLine()) {
                protocolOut.println(describer.respond(requestLine));
            }
        }
    }
}
