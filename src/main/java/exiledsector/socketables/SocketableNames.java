package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import exiledsector.ModCsv;
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
    private static final Logger LOG = Logger.getLogger(SocketableNames.class);
    private static final AtomicReference<Map<String, NameWords>> WORDS = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<String, Affix>> AFFIXES = new AtomicReference<>(Map.of());

    enum Style {CODENAME, PRODUCT}

    record NameWords(Style style, List<String> firstWords, List<String> secondWords, List<String> models) {
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
        ModCsv.load("effect", AFFIXES_PATH, LOG, SocketableNames::registerAffixes);
    }

    public static void registerWords(JSONObject namesJson) throws JSONException {
        Map<String, NameWords> loaded = new HashMap<>();
        Iterator<?> definitionIds = namesJson.keys();
        while (definitionIds.hasNext()) {
            String definitionId = String.valueOf(definitionIds.next());
            JSONObject wordsJson = namesJson.getJSONObject(definitionId);
            loaded.put(definitionId, new NameWords(style(definitionId, wordsJson.optString("style", "codename")), strings(wordsJson.optJSONArray("first")),
                    strings(wordsJson.optJSONArray("second")), strings(wordsJson.optJSONArray("models"))));
        }
        WORDS.set(Map.copyOf(loaded));
    }

    public static void registerAffixes(JSONArray rows) throws JSONException {
        Map<String, Affix> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String effectName = ModCsv.text(row, "effect");
            if (!effectName.isEmpty()) {
                loaded.put(effectName, new Affix(ModCsv.text(row, "prefix"), ModCsv.text(row, "suffix")));
            }
        });
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
        return render(definition, kind, effects, freeze(definition, seed, effects));
    }

    static FrozenName freeze(SocketableDefinition definition, long seed, List<RolledEffect> effects) {
        if (definition == null) {
            return null;
        }
        return switch (SocketableRarity.of(definition, effects.size())) {
            case UNIQUE -> FrozenName.NONE;
            case COMMON -> new FrozenName(firstInRole(definition, effects, true), firstInRole(definition, effects, false), null, null);
            case RARE -> rareWords(definition.id(), seed);
        };
    }

    static SocketableName render(SocketableDefinition definition, SocketableKind kind, List<RolledEffect> effects, FrozenName frozen) {
        SocketableRarity rarity = SocketableRarity.of(definition, effects.size());
        if (definition == null) {
            return new SocketableName(Translation.text("socketable.unknown"), null, rarity);
        }
        FrozenName nameParts = frozen == null ? FrozenName.NONE : frozen;
        return switch (rarity) {
            case UNIQUE -> new SocketableName(definition.displayName(), null, rarity);
            case COMMON -> new SocketableName(commonName(definition, kind, frozen), definition.displayName(), rarity);
            case RARE -> {
                String rareText = rareName(nameParts);
                yield rareText == null ? new SocketableName(definition.displayName(), null, rarity)
                        : new SocketableName(rareText, definition.displayName(), rarity);
            }
        };
    }

    private static String commonName(SocketableDefinition definition, SocketableKind kind, FrozenName frozen) {
        String prefixEffect = frozen == null ? null : frozen.prefixEffect();
        String suffixEffect = frozen == null ? null : frozen.suffixEffect();
        String prefix = prefixEffect == null ? null : affix(prefixEffect, true);
        String suffix = suffixEffect == null ? null : affix(suffixEffect, false);
        if (prefix == null && suffix == null) {
            return definition.displayName();
        }
        String affixForm = prefix == null ? "suffix" : suffix == null ? "prefix" : "both";
        Message nameMessage = Translation.msg("socketable.commonName." + affixForm)
                .arg("noun", Translation.text("socketable.noun." + kind.id()));
        if (prefix != null) {
            nameMessage.arg("prefix", prefix);
        }
        if (suffix != null) {
            nameMessage.arg("suffix", suffix);
        }
        String commonText = nameMessage.text().trim().replaceAll("\\s+", " ");
        return commonText.isEmpty() ? commonText : commonText.substring(0, 1).toUpperCase(Locale.ROOT) + commonText.substring(1);
    }

    private static String firstInRole(SocketableDefinition definition, List<RolledEffect> effects, boolean asPrefix) {
        for (RolledEffect effect : effects) {
            String effectName = effect.effectName();
            if (asPrefix ? definition.isPrefix(effectName) : definition.isSuffix(effectName)) {
                return effectName;
            }
        }
        return null;
    }

    private static String affix(String effectName, boolean asPrefix) {
        Affix effectAffix = AFFIXES.get().get(effectName);
        String englishAffix = effectAffix == null ? "" : asPrefix ? effectAffix.prefix() : effectAffix.suffix();
        if (englishAffix.isEmpty()) {
            return null;
        }
        return Translation.data("socketable.affix." + effectName + (asPrefix ? ".prefix" : ".suffix"), englishAffix);
    }

    private static FrozenName rareWords(String definitionId, long seed) {
        NameWords nameWords = WORDS.get().get(definitionId);
        if (nameWords == null || nameWords.firstWords().isEmpty() || nameWords.secondWords().isEmpty()) {
            return null;
        }
        Random random = new Random(SocketableRoller.scramble(seed ^ NAME_SALT));
        String firstWord = pick(nameWords.firstWords(), random);
        String secondWord = pick(nameWords.secondWords(), random);
        return switch (nameWords.style()) {
            case CODENAME -> new FrozenName(null, null, firstWord, secondWord);
            case PRODUCT -> productName(nameWords, firstWord, secondWord, random);
        };
    }

    private static String rareName(FrozenName nameParts) {
        if (nameParts.rareFirst() == null) {
            return null;
        }
        if (nameParts.isAssembledText()) {
            return nameParts.rareFirst();
        }
        String firstWord = word(nameParts.rareFirst());
        String secondWord = word(nameParts.rareSecond());
        if (!nameParts.isProduct()) {
            return Translation.msg("socketable.rareName.codename").arg("first", firstWord).arg("second", secondWord).text();
        }
        String brand = nameParts.rareBrand() == null ? firstWord : firstWord + word(nameParts.rareBrand());
        Message nameMessage = Translation.msg(nameParts.rareModel() == null ? "socketable.rareName.product" : "socketable.rareName.productModel")
                .arg("brand", brand).arg("second", secondWord);
        if (nameParts.rareModel() != null) {
            nameMessage.arg("model", word(nameParts.rareModel()));
        }
        return nameMessage.text();
    }

    private static String word(String word) {
        return Translation.data("socketable.nameWord." + word, word);
    }

    private static FrozenName productName(NameWords nameWords, String firstWord, String secondWord, Random random) {
        String brand = null;
        if (nameWords.firstWords().size() > 1 && random.nextFloat() < COMPOUND_CHANCE) {
            String otherFirstWord = pick(nameWords.firstWords(), random);
            while (otherFirstWord.equals(firstWord)) {
                otherFirstWord = pick(nameWords.firstWords(), random);
            }
            brand = otherFirstWord;
        }
        String model = null;
        if (!nameWords.models().isEmpty() && random.nextFloat() < MODEL_CHANCE) {
            String pickedModel = pick(nameWords.models(), random);
            if (!pickedModel.equals(secondWord)) {
                model = pickedModel;
            }
        }
        return FrozenName.product(firstWord, brand, secondWord, model);
    }

    private static String pick(List<String> words, Random random) {
        return words.get(random.nextInt(words.size()));
    }

    private static Style style(String definitionId, String styleName) {
        try {
            return Style.valueOf(styleName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown name style \"" + styleName + "\" for " + definitionId + " in " + NAMES_PATH + " - using codename");
            return Style.CODENAME;
        }
    }

    private static List<String> strings(JSONArray wordsArray) throws JSONException {
        List<String> values = new ArrayList<>();
        for (int i = 0; wordsArray != null && i < wordsArray.length(); i++) {
            String word = wordsArray.getString(i).trim();
            if (!word.isEmpty()) {
                values.add(word);
            }
        }
        return List.copyOf(values);
    }
}
