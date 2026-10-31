package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.ExiledSector;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.CorruptionOutcome;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingWard;
import exiledsector.items.Item;
import exiledsector.items.ItemCrafting;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemNature;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemType;
import exiledsector.items.MaterialItems;
import exiledsector.items.SocketCustody;
import exiledsector.items.UseVerdict;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class HullBench implements BenchModeSupport {

    public static final HullBench INSTANCE = new HullBench();
    private static final UseVerdict LOAD_FIRST = UseVerdict.refused("ui.workbench.notice.loadFirst");
    private static final UseVerdict CANNOT_USE = UseVerdict.refused("ui.workbench.notice.cannotUse");

    private HullBench() {
    }

    @Override
    public BenchMode mode() {
        return BenchMode.HULL_ITEMS;
    }

    @Override
    public ItemType itemType() {
        return null;
    }

    @Override
    public boolean accepts(Item item) {
        return item != null && BenchModes.modeFor(item.type()) == BenchMode.HULL_ITEMS;
    }

    @Override
    public ItemStore store() {
        return ItemStore.get();
    }

    @Override
    public String titleKey() {
        return "ui.workbench.title";
    }

    @Override
    public String emptyKey() {
        return "ui.workbench.empty";
    }

    @Override
    public String useHintKey() {
        return "ui.workbench.tile.use";
    }

    @Override
    public PickerSession pickerSession() {
        return null;
    }

    @Override
    public List<ItemCurrency> currencies() {
        List<ItemCurrency> offered = new ArrayList<>();
        ExiledSector.enabledDomains().forEach(domain -> offered.addAll(domain.hullBenchCurrencies()));
        return hullBenchCurrencies(offered, HoloPanel.playerMaterials()::count);
    }

    static List<ItemCurrency> hullBenchCurrencies(List<ItemCurrency> registered, ToIntFunction<String> owned) {
        List<ItemCurrency> shown = new ArrayList<>(List.of(CraftingCurrency.values()));
        for (ItemCurrency currency : registered) {
            if (!(currency instanceof CraftingCurrency) && currency.appliesToShipItems() && owned.applyAsInt(currency.itemId()) > 0) {
                shown.add(currency);
            }
        }
        return shown;
    }

    @Override
    public boolean inOrbit(ItemCurrency currency) {
        return currency instanceof CraftingCurrency;
    }

    @Override
    public String currencyDescriptionKey(ItemCurrency currency) {
        return MaterialItems.descriptionKey(currency.itemId());
    }

    @Override
    public String currencyDescription(ItemCurrency currency) {
        return currency.description();
    }

    @Override
    public List<StyledText> currencyLines(ItemCurrency currency) {
        return currency.grantedLines();
    }

    @Override
    public Refusal refusal(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Predicate<ItemDefinition> allowedUniques,
                           Set<CraftingWard> wards) {
        if (materials.count(currency.itemId()) < 1) {
            return new Refusal(UseVerdict.refused(currency.synthesisable() ? "ui.workbench.notice.noneOwned" : "ui.workbench.notice.noneFound"), false);
        }
        if (loaded == null) {
            return new Refusal(LOAD_FIRST, false);
        }
        return new Refusal(ItemCrafting.verdict(currency, loaded, allowedUniques, wards), true);
    }

    @Override
    public Use use(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Random random,
                   Predicate<ItemDefinition> allowedUniques, Set<CraftingWard> wards, String nameBefore) {
        Item result = ItemCrafting.use(currency, loaded, materials, store, random, allowedUniques, wards);
        if (result == null) {
            return null;
        }
        String notice = Translation.msg(usedNoticeKey(currency, result.corruption(), ItemNature.of(result.kind())))
                .arg("before", nameBefore).arg("name", result.name()).text();
        SocketCustody.markHostsChanged(result);
        if (currency.mayChangeDefinition() && SocketCustody.unsocketIfMisfit(result)) {
            String movedNotice = Translation.msg("ui.workbench.notice.movedToStorage").arg("name", result.name()).arg("type", result.kindName()).text();
            notice = Translation.msg("format.sentences").arg("a", notice).arg("b", movedNotice).text();
        }
        return new Use(result, currency.createsCopy() ? loaded : result, notice);
    }

    static String usedNoticeKey(ItemCurrency currency, CorruptionOutcome outcome, ItemNature nature) {
        if (currency == CraftingCurrency.CORRUPTION && outcome != null) {
            return "ui.workbench.notice.corrupted." + outcome.name().toLowerCase(Locale.ROOT) + "." + nature.name().toLowerCase(Locale.ROOT);
        }
        return currency.usedNoticeKey(nature);
    }

    @Override
    public UseVerdict cannotUse() {
        return CANNOT_USE;
    }

    @Override
    public List<StyledText> headerLines(Item item) {
        List<StyledText> lines = new ArrayList<>(item.conditionLines());
        lines.addAll(item.kindLines());
        return lines;
    }

    @Override
    public List<StyledText> detailLines(Item item) {
        return List.of();
    }

    @Override
    public OpenTexts openTexts() {
        return null;
    }

    @Override
    public OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded) {
        return new OpenCheck(false, null);
    }

    @Override
    public int openEntries() {
        return 0;
    }

    @Override
    public OpenOutcome open(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random) {
        return new OpenOutcome(false, null);
    }
}
