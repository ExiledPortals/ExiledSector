package exiledsector.skills.tags;

import org.json.JSONException;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;

public class ThemeTagsExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws IOException, JSONException {
        RealThemeTags.load();
    }
}
