package exiledsector.charts.loot;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.ModCsv;
import exiledsector.charts.atlas.AtlasRuntime;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.data.CsvRows;
import exiledsector.items.AffixLayout;
import exiledsector.items.GrantedModifiers;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ItemType;
import exiledsector.items.ModifierScope;
import exiledsector.items.ModifierSources;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierSlot;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class BiomeModifiers {

    public static final String PATH = "data/config/exiledSector/biome_modifiers.csv";
    public static final String ID_COLUMN = "modifier";
    public static final String OWNER_PREFIX = "biome:";
    public static final String HEADER_KEY_PREFIX = "chart.biomeModifier.header.";
    static final float DROP_CHANCE = 0.1f;
    private static final long GRANT_SEED_SALT = 0x62696F6D65L;
    private static final Logger LOG = Logger.getLogger(BiomeModifiers.class);

    private record Key(String biomeId, ItemKind kind) {
    }

    private static volatile Map<Key, String> modifiersByKey = Map.of();
    private static volatile Set<String> biomeIds = Set.of();

    private BiomeModifiers() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, BiomeModifiers::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        unregisterAll();
        CsvRows csv = new CsvRows(PATH);
        Map<Key, String> registered = new LinkedHashMap<>();
        CsvRows.forEachIdRow(rows, ID_COLUMN, (modifierId, row) -> {
            Key key = parse(csv, row, modifierId);
            if (key == null) {
                return;
            }
            if (registered.containsKey(key)) {
                csv.skipRow(modifierId, key.biomeId() + " already gives " + key.kind().id() + " " + registered.get(key));
                return;
            }
            try {
                ModifierSources.reserve(modifierId, OWNER_PREFIX + key.biomeId(), Set.of(key.kind()), headerKey(key.biomeId()));
                registered.put(key, modifierId);
            } catch (IllegalArgumentException e) {
                csv.skipRow(modifierId, e.getMessage());
            }
        });
        modifiersByKey = Map.copyOf(registered);
        Set<String> registeredBiomes = new LinkedHashSet<>();
        registered.keySet().forEach(key -> registeredBiomes.add(key.biomeId()));
        biomeIds = Set.copyOf(registeredBiomes);
        csv.report(LOG);
        LOG.info("[ExiledSector] " + registered.size() + " biome modifier(s) loaded for " + registeredBiomes.size() + " biome(s)");
    }

    private static Key parse(CsvRows csv, JSONObject row, String modifierId) {
        Modifier modifier = ItemModifiers.get(modifierId);
        if (modifier == null || modifier.slot() == ModifierSlot.IMPLICIT || !ItemModifiers.scopes(modifierId).contains(ModifierScope.HULL_ITEM)) {
            csv.skipRow(modifierId, "it is not a hull item prefix or suffix in " + ItemModifiers.dataPath());
            return null;
        }
        String biomeId = ModCsv.text(row, "biome");
        if (biomeId.isEmpty()) {
            csv.skipRow(modifierId, "it names no biome");
            return null;
        }
        ItemKind kind = ItemKind.byIdOrNull(ModCsv.text(row, "item"));
        if (kind == null || kind.type() != ItemType.HULL_ITEM) {
            csv.skipRow(modifierId, "\"" + ModCsv.text(row, "item") + "\" is not a hull item part");
            return null;
        }
        return new Key(biomeId, kind);
    }

    public static String headerKey(String biomeId) {
        return HEADER_KEY_PREFIX + biomeId;
    }

    public static void unregisterAll() {
        biomeIds.forEach(biomeId -> ModifierSources.release(OWNER_PREFIX + biomeId));
        modifiersByKey = Map.of();
        biomeIds = Set.of();
    }

    public static String modifierFor(String biomeId, ItemKind kind) {
        return biomeId == null || kind == null ? null : modifiersByKey.get(new Key(biomeId, kind));
    }

    public static ItemData onDrop(ItemData item, String biomeId) {
        ItemDefinition definition = item == null ? null : item.definition();
        String modifierId = definition == null ? null : modifierFor(biomeId, definition.kind());
        Random random = new Random(item.seed() ^ GRANT_SEED_SALT);
        if (modifierId == null || random.nextFloat() >= DROP_CHANCE) {
            return item;
        }
        return GrantedModifiers.add(item, modifierId, AffixLayout.dropLimits(definition.type()), random);
    }

    public static ItemData onDrop(SectorAPI sector, ItemData item, LocationAPI location) {
        if (modifiersByKey.isEmpty()) {
            return item;
        }
        ChartRun run = ChartRuns.liveIn(location);
        return run == null ? item : onDrop(item, AtlasRuntime.regionOf(sector, run.atlasNode()));
    }
}
