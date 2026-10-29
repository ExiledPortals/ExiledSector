package exiledsector;

import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemTypes;
import exiledsector.items.MasterSchematic;
import exiledsector.items.RefitSchematic;

import java.util.List;

public final class ExiledSector {

    public static final String LOG_TAG = "ExiledSector";
    public static final String MOD_ID = "exiledSector";
    public static final String CONFIG_DIRECTORY = "data/config/exiledSector/";
    public static final List<String> STRINGS_DIRECTORIES = List.of("data/strings/exiledSector/core/",
            "data/strings/exiledSector/skillTree/", "data/strings/exiledSector/hullUpgrades/", "data/strings/exiledSector/starCharts/");
    private static final String KEY_PREFIX = "exiledSector_";

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
        List<ExiledSectorDomain> installing = List.copyOf(domains);
        installing.forEach(domain -> domain.itemTypes().forEach(ItemTypes::register));
        ItemStore.absorbStacksWith(RefitSchematic::absorbInto);
        ItemStore.absorbStacksWith(MasterSchematic::absorbInto);
        installedDomains = installing;
    }

    public static String configPath(String fileName) {
        return CONFIG_DIRECTORY + fileName;
    }

    public static List<String> stringsPaths(String fileName) {
        return STRINGS_DIRECTORIES.stream().map(directory -> directory + fileName).toList();
    }

    public static String key(String name) {
        return KEY_PREFIX + name;
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
