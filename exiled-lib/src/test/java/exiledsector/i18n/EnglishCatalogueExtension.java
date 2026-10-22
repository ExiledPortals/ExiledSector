package exiledsector.i18n;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EnglishCatalogueExtension implements BeforeAllCallback, BeforeEachCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        I18n.install(RealCatalogue.english());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        I18n.install(RealCatalogue.english());
    }
}
