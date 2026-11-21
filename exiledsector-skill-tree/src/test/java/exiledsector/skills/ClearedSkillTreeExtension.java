package exiledsector.skills;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ClearedSkillTreeExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        TestTrees.clear();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        TestTrees.clear();
    }
}
