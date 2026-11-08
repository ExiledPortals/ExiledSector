package exiledsector.persistence;

import java.util.HashMap;
import java.util.Map;

public final class InMemoryTemplateFile implements SkillTreeTemplateStore.TemplateFile {

    public final Map<String, String> contentsByFileName = new HashMap<>();

    public static InMemoryTemplateFile install() {
        InMemoryTemplateFile file = new InMemoryTemplateFile();
        SkillTreeTemplateStore.useFile(file);
        return file;
    }

    public String templates() {
        return contentsByFileName.get(SkillTreeTemplateStore.COMMON_FILE);
    }

    public void setTemplates(String contents) {
        contentsByFileName.put(SkillTreeTemplateStore.COMMON_FILE, contents);
    }

    @Override
    public String read() {
        return templates();
    }

    @Override
    public void write(String fileName, String contents) {
        contentsByFileName.put(fileName, contents);
    }
}
