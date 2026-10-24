package exiledsector;

import exiledlib.HostMod;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemTypes;
import exiledsector.items.RefitSchematic;

import java.util.List;

public final class ExiledSector {

    public static final String LOG_TAG = "ExiledSector";
    public static final String MOD_ID = "exiledSector";
    public static final HostMod HOST = new HostMod(MOD_ID, "data/config/exiledSector/", List.of("data/strings/exiledSector/core/",
            "data/strings/exiledSector/skillTree/", "data/strings/exiledSector/hullUpgrades/", "data/strings/exiledSector/starCharts/"),
            "exiledSector_");

    private static final DomainContributions<ExiledSectorDomain> ENABLED_DOMAINS = new DomainContributions<>(List::of);
    private static volatile List<ExiledSectorDomain> installedDomains;

    private ExiledSector() {
    }

    public static synchronized void install(List<ExiledSectorDomain> domains) {
        if (installedDomains != null) {
            if (!domainClasses(installedDomains).equals(domainClasses(domains))) {
                throw new IllegalStateException("Exiled Sector's domains are already installed as " + domainClasses(installedDomains));
            }
            return;
        }
        HostMod.register(HOST);
        List<ExiledSectorDomain> installing = List.copyOf(domains);
        installing.forEach(domain -> domain.itemTypes().forEach(ItemTypes::register));
        ItemStore.absorbStacksWith(RefitSchematic::absorbInto);
        installedDomains = installing;
    }

    public static List<ExiledSectorDomain> domains() {
        List<ExiledSectorDomain> domains = installedDomains;
        return domains == null ? List.of() : domains;
    }

    public static List<ExiledSectorDomain> enabledDomains() {
        return ENABLED_DOMAINS.all();
    }

    private static List<Class<?>> domainClasses(List<ExiledSectorDomain> domains) {
        return domains.stream().<Class<?>>map(Object::getClass).toList();
    }
}
