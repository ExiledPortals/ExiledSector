package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierSlot;
import com.fs.starfarer.api.Global;
import exiledsector.Seeds;
import exiledsector.i18n.Message;
import exiledsector.i18n.Translation;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class ItemNames {

    static final String NAMES_PATH = "data/config/exiledSector/item_names.json";
    static final float COMPOUND_CHANCE = 0.3f;
    static final float MODEL_CHANCE = 0.4f;
    private static final long NAME_SALT = 0x6E616D6573L;
    private static final Logger LOG = Logger.getLogger(ItemNames.class);
    private static final AtomicReference<Map<String, NameWords>> WORDS = new AtomicReference<>(Map.of());

    enum Style {CODENAME, PRODUCT}

    record NameWords(Style style, List<String> firstWords, List<String> secondWords, List<String> models) {
    }

    private ItemNames() {
    }

    public static void load() {
        try {
            registerWords(Global.getSettings().getMergedJSONForMod(NAMES_PATH, MOD_ID));
        } catch (IOException | JSONException e) {
            LOG.error("Failed to load " + NAMES_PATH, e);
        }
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

    public static void clear() {
        WORDS.set(Map.of());
    }

    static boolean hasName(ItemDefinition definition, String modifierId, ModifierSlot slot) {
        return namedModifier(definition, modifierId, slot) != null;
    }

    static ItemName nameFor(ItemDefinition definition, ItemKind kind, long seed, List<RolledModifier> modifiers) {
        return render(definition, kind, modifiers, freeze(definition, seed, modifiers));
    }

    static FrozenName freeze(ItemDefinition definition, long seed, List<RolledModifier> modifiers) {
        if (definition == null) {
            return null;
        }
        return switch (AffixLayout.rarityFor(definition, modifiers.size())) {
            case UNIQUE -> FrozenName.NONE;
            case COMMON -> new FrozenName(firstInSlot(definition, modifiers, ModifierSlot.PREFIX), firstInSlot(definition, modifiers, ModifierSlot.SUFFIX), null, null);
            case RARE -> rareWords(definition.id(), seed);
        };
    }

    static ItemName render(ItemDefinition definition, ItemKind kind, List<RolledModifier> modifiers, FrozenName frozen) {
        ItemRarity rarity = AffixLayout.rarityFor(definition, modifiers.size());
        if (definition == null) {
            return new ItemName(Translation.text("socketable.unknown"), null, rarity);
        }
        FrozenName nameParts = frozen == null ? FrozenName.NONE : frozen;
        return switch (rarity) {
            case UNIQUE -> new ItemName(definition.displayName(), null, rarity);
            case COMMON -> new ItemName(commonName(definition, kind, modifiers, frozen), definition.displayName(), rarity);
            case RARE -> {
                String rareText = rareName(nameParts);
                yield rareText == null ? new ItemName(definition.displayName(), null, rarity)
                        : new ItemName(rareText, definition.displayName(), rarity);
            }
        };
    }

    private static String commonName(ItemDefinition definition, ItemKind kind, List<RolledModifier> modifiers, FrozenName frozen) {
        String prefix = frozenWord(definition, modifiers, frozen == null ? null : frozen.prefixModifierId(), ModifierSlot.PREFIX);
        String suffix = frozenWord(definition, modifiers, frozen == null ? null : frozen.suffixModifierId(), ModifierSlot.SUFFIX);
        if (prefix == null && suffix == null) {
            return definition.displayName();
        }
        String affixForm = prefix == null ? "suffix" : suffix == null ? "prefix" : "both";
        Message nameMessage = Translation.msg("socketable.commonName." + affixForm)
                .arg("noun", kind != null ? kind.noun() : definition.noun());
        if (prefix != null) {
            nameMessage.arg("prefix", prefix);
        }
        if (suffix != null) {
            nameMessage.arg("suffix", suffix);
        }
        String commonText = nameMessage.text().trim().replaceAll("\\s+", " ");
        return commonText.isEmpty() ? commonText : commonText.substring(0, 1).toUpperCase(Locale.ROOT) + commonText.substring(1);
    }

    private static String firstInSlot(ItemDefinition definition, List<RolledModifier> modifiers, ModifierSlot slot) {
        for (RolledModifier modifier : modifiers) {
            if (modifier.slot() == slot && definition.poolEntryFor(modifier.modifierId()) != null) {
                return modifier.modifierId();
            }
        }
        return null;
    }

    private static String frozenWord(ItemDefinition definition, List<RolledModifier> modifiers, String frozenModifierId, ModifierSlot slot) {
        if (frozenModifierId == null) {
            return null;
        }
        String word = modifierName(definition, frozenModifierId, slot);
        return word != null ? word : modifierName(definition, firstInSlot(definition, modifiers, slot), slot);
    }

    private static String modifierName(ItemDefinition definition, String modifierId, ModifierSlot slot) {
        Modifier named = namedModifier(definition, modifierId, slot);
        return named == null ? null : ItemModifiers.name(named.id());
    }

    private static Modifier namedModifier(ItemDefinition definition, String modifierId, ModifierSlot slot) {
        if (modifierId == null) {
            return null;
        }
        PoolEntry entry = definition.poolEntryFor(modifierId, slot);
        if (entry != null && ItemModifiers.hasName(entry.modifier().id())) {
            return entry.modifier();
        }
        Modifier listed = ItemModifiers.get(modifierId);
        if (listed != null && listed.slot() == slot && ItemModifiers.hasName(listed.id())) {
            return listed;
        }
        return ItemModifiers.namedForEffect(modifierId, slot);
    }

    private static FrozenName rareWords(String definitionId, long seed) {
        NameWords nameWords = WORDS.get().get(definitionId);
        if (nameWords == null || nameWords.firstWords().isEmpty() || nameWords.secondWords().isEmpty()) {
            return null;
        }
        Random random = Seeds.random(seed ^ NAME_SALT);
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
        Set<String> values = new LinkedHashSet<>();
        for (int i = 0; wordsArray != null && i < wordsArray.length(); i++) {
            String word = wordsArray.getString(i).trim();
            if (!word.isEmpty()) {
                values.add(word);
            }
        }
        return List.copyOf(values);
    }
}
