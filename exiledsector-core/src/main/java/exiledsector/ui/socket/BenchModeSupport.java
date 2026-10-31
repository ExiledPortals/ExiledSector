package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingWard;
import exiledsector.items.Item;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemType;
import exiledsector.items.UseVerdict;
import exiledsector.ui.SkillTreeSounds;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface BenchModeSupport {

    record Refusal(UseVerdict verdict, boolean checksWards) {

        public boolean allowed() {
            return verdict.allowed();
        }
    }

    record Use(Item result, Item selectedAfter, String notice) {
    }

    record OpenCheck(boolean allowsOpening, String reasonKey) {
    }

    record OpenOutcome(boolean opened, String messageKey) {
    }

    record OpenTexts(String buttonKey, String tooltipKey, String confirmTitleKey, String confirmBodyKey, String confirmEntriesKey,
                     String confirmYesKey, String confirmNoKey) {
    }

    interface PickerListener {

        boolean accepting();

        void picked(Item item);

        void closed();

        void pressedInside();

        default void storeChanged() {
        }
    }

    record OpenTarget(Object target, Supplier<List<String>> confirmLines, Supplier<List<OpenChoice>> choices) {

        public OpenTarget(Object target, Supplier<List<String>> confirmLines) {
            this(target, confirmLines, List::of);
        }
    }

    record OpenChoice(String label, String description, Object target) {
    }

    record DebugAction(String buttonKey, String tooltipKey) {
    }

    interface PickerSession {

        void open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, Item loaded, PickerListener listener);

        boolean isOpen();

        void changed(Item loaded);

        void refresh();

        default boolean escape() {
            return false;
        }

        void close();

        boolean contains(float x, float y);
    }

    BenchMode mode();

    ItemType itemType();

    default boolean accepts(Item item) {
        return item != null && item.type() == itemType();
    }

    default boolean usesInhibitors() {
        return true;
    }

    boolean enabled();

    ItemStore store();

    String titleKey();

    String emptyKey();

    String useHintKey();

    PickerSession pickerSession();

    List<ItemCurrency> currencies();

    String currencyDescriptionKey(ItemCurrency currency);

    default String currencyDescription(ItemCurrency currency) {
        return Translation.text(currencyDescriptionKey(currency));
    }

    default List<StyledText> currencyLines(ItemCurrency currency) {
        return List.of();
    }

    default boolean inOrbit(ItemCurrency currency) {
        return true;
    }

    Refusal refusal(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Predicate<ItemDefinition> allowedUniques,
                    Set<CraftingWard> wards);

    Use use(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Random random,
            Predicate<ItemDefinition> allowedUniques, Set<CraftingWard> wards, String nameBefore);

    UseVerdict cannotUse();

    default List<StyledText> headerLines(Item item) {
        return item.headerLines();
    }

    List<StyledText> detailLines(Item item);

    OpenTexts openTexts();

    OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded);

    default OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded, Object target) {
        return openCheck(sector, store, loaded);
    }

    default List<String> confirmLines(Item loaded, OpenTarget target) {
        return target == null ? List.of() : target.confirmLines().get();
    }

    default List<OpenChoice> openChoices(Item loaded, OpenTarget target) {
        return target == null ? List.of() : target.choices().get();
    }

    default void playOpened() {
        SkillTreeSounds.wormholeJumped();
    }

    int openEntries();

    OpenOutcome open(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random);

    default OpenOutcome openAt(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random, Object target) {
        return open(sector, store, loaded, entries, random);
    }

    default DebugAction debugAction() {
        return null;
    }

    default void runDebugAction(SectorAPI sector, Item loaded, Object target) {
    }
}
