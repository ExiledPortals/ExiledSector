package exiledsector;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PomVersionsTest {

    private static Element pom(String path) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ProjectFiles.projectRoot().resolve(path).toFile()).getDocumentElement();
    }

    private static String child(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) {
                return element.getTextContent().trim();
            }
        }
        return null;
    }

    private static Element childElement(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) {
                return element;
            }
        }
        return null;
    }

    private static String modVersion(Path modInfo) throws Exception {
        JSONObject version = new JSONObject(Files.readString(ProjectFiles.projectRoot().resolve(modInfo), StandardCharsets.UTF_8)).getJSONObject("version");
        return version.getInt("major") + "." + version.getInt("minor") + "." + version.getInt("patch");
    }

    private static List<String> modules(Element root) {
        List<String> modules = new ArrayList<>();
        Element moduleList = childElement(root, "modules");
        for (Node node = moduleList.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element) {
                modules.add(element.getTextContent().trim());
            }
        }
        return modules;
    }

    @Test
    void theParentPomCarriesExiledSectorsVersionAndEveryModuleInheritsIt() throws Exception {
        Element root = pom("pom.xml");
        String exiledSectorVersion = child(root, "version");

        assertEquals(modVersion(Path.of("mod_info.json")), exiledSectorVersion);
        for (String module : modules(root)) {
            Element modulePom = pom(module + "/pom.xml");
            assertEquals(exiledSectorVersion, child(childElement(modulePom, "parent"), "version"), module);
            assertEquals(null, child(modulePom, "version"), module + " takes its version from the parent");
        }
    }
}
