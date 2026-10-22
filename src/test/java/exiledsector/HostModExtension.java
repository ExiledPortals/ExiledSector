package exiledsector;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class HostModExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        HostMod.register(ExiledSectorModPlugin.HOST);
    }
}
