package exiledsector.domain;

import exiledsector.ExiledSector;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.List;

public class DomainsExtension implements BeforeAllCallback {

    public static final String DOMAINS_PROPERTY = "exiledsector.test.domains";

    @Override
    public void beforeAll(ExtensionContext context) throws ReflectiveOperationException {
        ExiledSector.install(domains());
    }

    static List<ExiledSectorDomain> domains() throws ReflectiveOperationException {
        List<ExiledSectorDomain> domains = new ArrayList<>();
        for (String className : System.getProperty(DOMAINS_PROPERTY, "").split(",")) {
            if (!className.isBlank()) {
                domains.add((ExiledSectorDomain) Class.forName(className.trim()).getDeclaredConstructor().newInstance());
            }
        }
        return domains;
    }
}
