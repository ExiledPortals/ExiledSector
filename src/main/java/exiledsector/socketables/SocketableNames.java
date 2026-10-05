package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import exiledsector.i18n.Message;
import exiledsector.i18n.Translation;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class SocketableNames {

    static final String NAMES_PATH = "data/config/exiledSector/socketable_names.json";
    static final String AFFIXES_PATH = "data/config/exiledSector/socketable_affixes.csv";
    static final float COMPOUND_CHANCE = 0.3f;
    static final float MODEL_CHANCE = 0.4f;
    private static final long NAME_SALT = 0x6E616D6573L;
    private static final int DEFAULT_REVISION_MIN = 1;
    private static final int DEFAULT_REVISION_MAX = 20;
    private static final Logger LOG = Logger.getLogger(SocketableNames.class);
    private static final AtomicReference<Map<String, GradeWords>> WORDS = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<String, Affix>> AFFIXES = new AtomicReference<>(Map.of());

    enum Style {CODENAME, REVISION, PRODUCT}

    record GradeWords(Style style, List<String> first, List<String> second, int revisionMin, int revisionMax, List<String> models) {
    }

    record Affix(String prefix, String suffix) {
    }

    private SocketableNames() {
    }

    public static void load() {
        try {
            registerWords(Global.getSettings().getMergedJSONForMod(NAMES_PATH, MOD_ID));
        } catch (IOException | JSONException e) {
            LOG.error("Failed to load " + NAMES_PATH, e);
        }
        try {
            registerAffixes(Global.getSettings().getMergedSpreadsheetDataForMod("effect", AFFIXES_PATH, MOD_ID));
        } catch (IOException | JSONException e) {
            LOG.error("Failed to load " + AFFIXES_PATH, e);
        }
    }

    public static void registerWords(JSONObject root) throws JSONException {
        Map<String, GradeWords> loaded = new HashMap<>();
        Iterator<?> grades = root.keys();
        while (grades.hasNext()) {
            String grade = String.valueOf(grades.next());
            JSONObject words = root.getJSONObject(grade);
            loaded.put(grade, new GradeWords(style(grade, words.optString("style", "codename")), strings(words.optJSONArray("first")),
                    strings(words.optJSONArray("second")), words.optInt("revisionMin", DEFAULT_REVISION_MIN),
                    Math.max(words.optInt("revisionMin", DEFAULT_REVISION_MIN), words.optInt("revisionMax", DEFAULT_REVISION_MAX)),
                    strings(words.optJSONArray("models"))));
        }
        WORDS.set(Map.copyOf(loaded));
    }

    public static void registerAffixes(JSONArray rows) throws JSONException {
        Map<String, Affix> loaded = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String effect = row.optString("effect", "").trim();
            if (!effect.isEmpty()) {
                loaded.put(effect, new Affix(row.optString("prefix", "").trim(), row.optString("suffix", "").trim()));
            }
        }
        AFFIXES.set(Map.copyOf(loaded));
    }

    public static void clear() {
        WORDS.set(Map.of());
        AFFIXES.set(Map.of());
    }

    static boolean hasAffix(String effectName) {
        return AFFIXES.get().containsKey(effectName);
    }

    static SocketableName nameFor(SocketableDefinition definition, SocketableKind kind, long seed, List<RolledEffect> effects) {
        SocketableRarity rarity = SocketableRarity.of(definition, effects.size());
        if (definition == null) {
            return new SocketableName(Translation.text("socketable.unknown"), null, rarity);
        }
        return switch (rarity) {
            case UNIQUE -> new SocketableName(definition.displayName(), null, rarity);
            case MAGIC -> new SocketableName(magicName(definition, kind, effects), null, rarity);
            case RARE -> {
                String rare = rareName(definition.grade(), seed);
                yield rare == null ? new SocketableName(definition.displayName(), null, rarity)
                        : new SocketableName(rare, definition.displayName(), rarity);
            }
        };
    }

    private static String magicName(SocketableDefinition definition, SocketableKind kind, List<RolledEffect> effects) {
        String prefix = firstAffix(definition, effects, true);
        String suffix = firstAffix(definition, effects, false);
        if (prefix == null && suffix == null) {
            return definition.displayName();
        }
        String form = prefix == null ? "suffix" : suffix == null ? "prefix" : "both";
        Message message = Translation.msg("socketable.magicName." + form)
                .arg("grade", definition.gradeName())
                .arg("noun", Translation.text("socketable.noun." + kind.id()));
        if (prefix != null) {
            message.arg("prefix", prefix);
        }
        if (suffix != null) {
            message.arg("suffix", suffix);
        }
        return message.text().trim().replaceAll("\\s+", " ");
    }

    private static String firstAffix(SocketableDefinition definition, List<RolledEffect> effects, boolean prefix) {
        for (RolledEffect effect : effects) {
            String effectName = effect.effectName();
            if (prefix ? definition.isPrefix(effectName) : definition.isSuffix(effectName)) {
                return affix(effectName, prefix);
            }
        }
        return null;
    }

    private static String affix(String effectName, boolean prefix) {
        Affix affix = AFFIXES.get().get(effectName);
        String english = affix == null ? "" : prefix ? affix.prefix() : affix.suffix();
        if (english.isEmpty()) {
            return null;
        }
        return Translation.data("socketable.affix." + effectName + (prefix ? ".prefix" : ".suffix"), english);
    }

    private static String rareName(String grade, long seed) {
        GradeWords words = WORDS.get().get(grade);
        if (words == null || words.first().isEmpty() || words.second().isEmpty()) {
            return null;
        }
        Random random = new Random(SocketableRoller.scramble(seed ^ NAME_SALT));
        String first = pick(words.first(), random);
        String second = pick(words.second(), random);
        return switch (words.style()) {
            case CODENAME -> Translation.msg("socketable.rareName.codename").arg("first", first).arg("second", second).text();
            case REVISION -> Translation.msg("socketable.rareName.revision").arg("first", first).arg("second", second)
                    .arg("revision", words.revisionMin() + random.nextInt(words.revisionMax() - words.revisionMin() + 1)).text();
            case PRODUCT -> productName(words, first, second, random);
        };
    }

    private static String productName(GradeWords words, String first, String second, Random random) {
        String brand = first;
        if (words.first().size() > 1 && random.nextFloat() < COMPOUND_CHANCE) {
            String other = pick(words.first(), random);
            while (other.equals(first)) {
                other = pick(words.first(), random);
            }
            brand = first + other;
        }
        String name = brand + " " + second;
        if (!words.models().isEmpty() && random.nextFloat() < MODEL_CHANCE) {
            String model = pick(words.models(), random);
            if (!model.equals(second)) {
                name += " " + model;
            }
        }
        return name;
    }

    private static String pick(List<String> words, Random random) {
        return words.get(random.nextInt(words.size()));
    }

    private static Style style(String grade, String name) {
        try {
            return Style.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown name style \"" + name + "\" for grade " + grade + " in " + NAMES_PATH + " - using codename");
            return Style.CODENAME;
        }
    }

    private static List<String> strings(JSONArray array) throws JSONException {
        List<String> values = new ArrayList<>();
        for (int i = 0; array != null && i < array.length(); i++) {
            String value = array.getString(i).trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }
}
