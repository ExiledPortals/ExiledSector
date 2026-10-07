package exiledsector.i18n;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Catalogue {

    static final String METADATA_PREFIX = "meta.";

    private static final Logger LOG = Logger.getLogger(Catalogue.class);

    private final String locale;
    private final boolean pseudo;
    private final Map<String, String> stringsByKey;
    private final Map<String, Template> templates = new ConcurrentHashMap<>();
    private final Set<String> reportedMissing = ConcurrentHashMap.newKeySet();

    public Catalogue(String locale, Map<String, String> stringsByKey) {
        this(locale, stringsByKey, false);
    }

    Catalogue(String locale, Map<String, String> stringsByKey, boolean pseudo) {
        this.locale = locale;
        this.pseudo = pseudo;
        this.stringsByKey = Collections.unmodifiableMap(new LinkedHashMap<>(stringsByKey));
    }

    public static Catalogue compose(String locale, Function<String, Map<String, String>> files) {
        if (PseudoLocale.LOCALE.equals(locale)) {
            return new Catalogue(locale, PseudoLocale.apply(compose(LocaleChain.ENGLISH, files).stringsByKey), true);
        }
        List<Map<String, String>> layers = new ArrayList<>();
        for (String candidate : LocaleChain.highestPriorityFirst(locale)) {
            layers.add(files.apply(candidate));
        }
        Collections.reverse(layers);
        return layered(locale, layers);
    }

    public static Catalogue layered(String locale, List<Map<String, String>> lowestPriorityFirst) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (Map<String, String> layer : lowestPriorityFirst) {
            merged.putAll(layer);
        }
        return new Catalogue(locale, merged);
    }

    public String locale() {
        return locale;
    }

    public boolean isPseudo() {
        return pseudo;
    }

    public boolean has(String key) {
        return stringsByKey.containsKey(key);
    }

    public String raw(String key) {
        return stringsByKey.get(key);
    }

    public int size() {
        return stringsByKey.size();
    }

    Template template(String key) {
        return templates.computeIfAbsent(key, this::parseOrMissing);
    }

    private Template parseOrMissing(String key) {
        String raw = stringsByKey.get(key);
        if (raw == null) {
            if (reportedMissing.add(key)) {
                LOG.warn("Missing Exiled Sector string for locale " + locale + ": " + key);
            }
            return Template.parse("[[" + key + "]]");
        }
        return Template.parse(raw);
    }
}
