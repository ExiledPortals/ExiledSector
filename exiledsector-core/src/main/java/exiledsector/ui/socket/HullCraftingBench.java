package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.ExiledSector;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.CorruptionOutcome;
import exiledsector.items.Nanoswarm;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.Item;
import exiledsector.items.ItemCrafting;
import exiledsector.items.CraftingMaterialFamily;
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

public final class HullCraftingBench implements BenchModeSupport {

    public static final HullCraftingBench INSTANCE = new HullCraftingBench();
    private static final UseVerdict LOAD_FIRST = UseVerdict.refused("ui.craftingBench.notice.loadFirst");
    private static final UseVerdict CANNOT_USE = UseVerdict.refused("ui.craftingBench.notice.cannotUse");
    private static final DomainContributions<BenchTray> DOMAIN_TRAYS = new DomainContributions<>(ExiledSectorDomain::hullCraftingBenchTrays);

    private HullCraftingBench() {
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
        return "ui.craftingBench.title";
    }

    @Override
    public String emptyKey() {
        return "ui.craftingBench.empty";
    }

    @Override
    public String useHintKey() {
        return "ui.craftingBench.tile.use";
    }

    @Override
    public PickerSession pickerSession() {
        return null;
    }

    @Override
    public List<CraftingMaterialFamily> craftingItems() {
        List<CraftingMaterialFamily> offered = new ArrayList<>();
        ExiledSector.enabledDomains().forEach(domain -> offered.addAll(domain.hullCraftingBenchCraftingItems()));
        return hullCraftingBenchCraftingItems(offered, HoloPanel.playerMaterials()::count);
    }

    @Override
    public List<BenchTray> trays() {
        return hullCraftingBenchTrays(DOMAIN_TRAYS.all());
    }

    static List<BenchTray> hullCraftingBenchTrays(List<BenchTray> domainTrays) {
        List<BenchTray> trays = new ArrayList<>();
        trays.add(InhibitorTray.INSTANCE);
        trays.addAll(domainTrays);
        return trays;
    }

    static List<CraftingMaterialFamily> hullCraftingBenchCraftingItems(List<CraftingMaterialFamily> registered, ToIntFunction<String> owned) {
        List<CraftingMaterialFamily> shown = new ArrayList<>(List.of(Nanoswarm.values()));
        for (CraftingMaterialFamily craftingItem : registered) {
            if (columnCandidate(craftingItem) && owned.applyAsInt(craftingItem.itemId()) > 0) {
                shown.add(craftingItem);
            }
        }
        return shown;
    }

    private static boolean columnCandidate(CraftingMaterialFamily craftingItem) {
        return !(craftingItem instanceof Nanoswarm) && craftingItem.appliesToShipItems();
    }

    @Override
    public boolean inOrbit(CraftingMaterialFamily craftingItem) {
        return craftingItem instanceof Nanoswarm;
    }

    @Override
    public boolean reservesColumn() {
        return ExiledSector.enabledDomains().stream().flatMap(domain -> domain.hullCraftingBenchCraftingItems().stream())
                .anyMatch(HullCraftingBench::columnCandidate);
    }

    @Override
    public String craftingItemDescriptionKey(CraftingMaterialFamily craftingItem) {
        return MaterialItems.descriptionKey(craftingItem.itemId());
    }

    @Override
    public StyledText craftingItemDescription(CraftingMaterialFamily craftingItem) {
        return craftingItem.description();
    }

    @Override
    public List<StyledText> craftingItemLines(CraftingMaterialFamily craftingItem) {
        return craftingItem.grantedLines();
    }

    @Override
    public Refusal refusal(CraftingMaterialFamily craftingItem, Item loaded, ItemStore store, CraftingMaterials materials, Predicate<ItemDefinition> allowedUniques,
                           Set<CraftingInhibitor> inhibitors) {
        if (materials.count(craftingItem.itemId()) < 1) {
            return new Refusal(UseVerdict.refused(craftingItem.growable() ? "ui.craftingBench.notice.noneOwned" : "ui.craftingBench.notice.noneFound"), false);
        }
        if (loaded == null) {
            return new Refusal(LOAD_FIRST, false);
        }
        return new Refusal(ItemCrafting.verdict(craftingItem, loaded, allowedUniques, inhibitors), true);
    }

    @Override
    public Use use(CraftingMaterialFamily craftingItem, Item loaded, ItemStore store, CraftingMaterials materials, Random random,
                   Predicate<ItemDefinition> allowedUniques, Set<CraftingInhibitor> inhibitors, String nameBefore) {
        Item result = ItemCrafting.use(craftingItem, loaded, materials, store, random, allowedUniques, inhibitors);
        if (result == null) {
            return null;
        }
        String notice = Translation.msg(usedNoticeKey(craftingItem, result.corruption(), ItemNature.of(result.kind())))
                .arg("before", nameBefore).arg("name", result.name()).text();
        SocketCustody.markHostsChanged(result);
        if (craftingItem.mayChangeDefinition() && SocketCustody.unsocketIfMisfit(result)) {
            String movedNotice = Translation.msg("ui.craftingBench.notice.movedToStorage").arg("name", result.name()).arg("type", result.kindName()).text();
            notice = Translation.msg("format.sentences").arg("a", notice).arg("b", movedNotice).text();
        }
        return new Use(result, craftingItem.createsCopy() ? loaded : result, notice);
    }

    static String usedNoticeKey(CraftingMaterialFamily craftingItem, CorruptionOutcome outcome, ItemNature nature) {
        if (craftingItem == Nanoswarm.CORRUPTION && outcome != null) {
            return "ui.craftingBench.notice.corrupted." + outcome.name().toLowerCase(Locale.ROOT) + "." + nature.name().toLowerCase(Locale.ROOT);
        }
        return craftingItem.usedNoticeKey(nature);
    }

    @Override
    public UseVerdict cannotUse() {
        return CANNOT_USE;
    }

    @Override
    public StyledText flavourLine(Item item) {
        return null;
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
    public int openEntries(Item loaded) {
        return 0;
    }

    @Override
    public OpenOutcome open(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random) {
        return new OpenOutcome(false, null);
    }
}
