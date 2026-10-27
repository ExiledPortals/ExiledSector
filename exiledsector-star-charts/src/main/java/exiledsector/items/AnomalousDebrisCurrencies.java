package exiledsector.items;

import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import exiledlib.ModCsv;
import exiledlib.i18n.Translation;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierSlot;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class AnomalousDebrisCurrencies {

    public static final String PATH = "data/config/exiledSector/anomalous_debris_currencies.csv";
    static final String ID_COLUMN = "id";
    static final Set<ModifierScope> TAKEN_SCOPES = Set.of(ModifierScope.HULL_ITEM, ModifierScope.SUBROUTINE);
    private static final Logger LOG = Logger.getLogger(AnomalousDebrisCurrencies.class);
    private static volatile List<AnomalousDebrisCurrency> loaded = List.of();

    private AnomalousDebrisCurrencies() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, AnomalousDebrisCurrencies::register);
    }

    public static List<String> register(JSONArray rows) throws JSONException {
        unregisterAll();
        CsvRows csv = new CsvRows(PATH);
        List<AnomalousDebrisCurrency> registered = new ArrayList<>();
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            AnomalousDebrisCurrency currency = parse(csv, row, id);
            if (currency == null) {
                return;
            }
            try {
                ItemCurrencies.register(currency);
                ReservedModifiers.reserve(currency.modifierId(), currency.itemId());
                registered.add(currency);
            } catch (IllegalArgumentException e) {
                ItemCurrencies.unregister(currency);
                csv.skipRow(id, e.getMessage());
            }
        });
        loaded = List.copyOf(registered);
        csv.report(LOG);
        LOG.info("[ExiledSector] Corrupted dormant fleets can drop " + registered.size() + " currency item(s)");
        return csv.problems();
    }

    static AnomalousDebrisCurrency parse(CsvRows csv, JSONObject row, String id) {
        String modifierId = ModCsv.text(row, "modifier");
        Modifier modifier = ItemModifiers.get(modifierId);
        if (modifier == null || modifier.slot() == ModifierSlot.IMPLICIT) {
            csv.skipRow(id, "modifier " + modifierId + " is not a prefix or suffix in " + ItemModifiers.dataPath());
            return null;
        }
        Set<ItemType> itemTypes = EnumSet.noneOf(ItemType.class);
        for (String entry : CsvRows.entries(ModCsv.text(row, "item_types"))) {
            ModifierScope scope = ModifierScope.byIdOrNull(entry.trim());
            if (scope == null || !TAKEN_SCOPES.contains(scope) || !ItemModifiers.scopes(modifierId).contains(scope)) {
                csv.problem(id, "item_types", entry.trim());
            } else {
                itemTypes.add(scope.itemType());
            }
        }
        if (itemTypes.isEmpty()) {
            csv.skipRow(id, "no item type takes " + modifierId);
            return null;
        }
        int minTier = ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, id));
        return new AnomalousDebrisCurrency(id, ModCsv.text(row, "name"), modifierId, itemTypes, minTier, csv.number(row, "weight", 1f, id));
    }

    public static void unregisterAll() {
        for (AnomalousDebrisCurrency currency : loaded) {
            ItemCurrencies.unregister(currency);
            ReservedModifiers.release(currency.itemId());
        }
        loaded = List.of();
    }

    public static List<AnomalousDebrisCurrency> all() {
        return loaded;
    }

    public static AnomalousDebrisCurrency pick(int tier, Random random) {
        List<AnomalousDebrisCurrency> dropping = loaded.stream().filter(currency -> currency.dropsAt(tier)).toList();
        return dropping.isEmpty() ? null : WeightedPick.pick(dropping, AnomalousDebrisCurrency::dropWeight, random);
    }

    public static void localise() {
        for (AnomalousDebrisCurrency currency : loaded) {
            SpecialItemSpecAPI spec = MaterialItems.spec(currency.itemId());
            if (spec != null) {
                String englishName = currency.englishName().isEmpty() ? spec.getName() : currency.englishName();
                spec.setName(Translation.data("material." + currency.itemId() + ".name", englishName));
                spec.setDesc(currency.description());
            }
        }
    }

    public static void checkData(ItemDataIntegrity integrity) throws IOException, JSONException {
        checkRows(integrity, ItemDataIntegrity.orEmpty(ModCsv.rows(ID_COLUMN, PATH)), PATH);
    }

    static void checkRows(ItemDataIntegrity integrity, JSONArray rows, String file) throws JSONException {
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + ItemDataIntegrity.FIRST_DATA_LINE;
            String currencyId = ModCsv.text(row, ID_COLUMN);
            if (currencyId.isEmpty()) {
                integrity.error(file, line, "the row has no id");
                return;
            }
            Set<ModifierScope> scopes = EnumSet.noneOf(ModifierScope.class);
            for (String entry : CsvRows.entries(ModCsv.text(row, "item_types"))) {
                ModifierScope scope = ModifierScope.byIdOrNull(entry.trim());
                if (scope == null || !TAKEN_SCOPES.contains(scope)) {
                    integrity.error(file, line, currencyId + " lists item type \"" + entry.trim() + "\"; expected hull_item or subroutine");
                } else {
                    scopes.add(scope);
                }
            }
            if (scopes.isEmpty()) {
                integrity.error(file, line, currencyId + " needs at least one item type in item_types");
            }
            integrity.checkReservedModifier(file, line, currencyId, ModCsv.text(row, "modifier"), scopes);
        });
    }

    static void resetForTests() {
        unregisterAll();
    }
}
