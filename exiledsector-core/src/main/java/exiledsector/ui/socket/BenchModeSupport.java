package exiledsector.ui.socket;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import exiledsector.i18n.StyledText;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingWard;
import exiledsector.items.Item;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemStore;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public interface BenchModeSupport {

    record Refusal(boolean allowed, boolean checksWards, String noticeKey) {
    }

    record Use(Item result, Item selectedAfter, String notice) {
    }

    record OpenCheck(boolean allowsOpening, String reasonKey) {
    }

    record OpenOutcome(boolean opened, String messageKey) {
    }

    interface PickerListener {

        boolean accepting();

        void picked(Item item);

        void closed();

        void pressedInside();
    }

    interface PickerSession {

        void open(CustomPanelAPI hostPanel, SocketPanelPlacement placement, Item loaded, PickerListener listener);

        boolean isOpen();

        void changed(Item loaded);

        void refresh();

        void swapOut();

        void close();

        boolean contains(float x, float y);
    }

    BenchMode mode();

    boolean enabled();

    PickerSession pickerSession();

    List<ItemCurrency> currencies();

    String currencyDescriptionKey(ItemCurrency currency);

    Refusal refusal(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Predicate<ItemDefinition> allowedUniques,
                    Set<CraftingWard> wards);

    Use use(ItemCurrency currency, Item loaded, ItemStore store, CraftingMaterials materials, Random random,
            Predicate<ItemDefinition> allowedUniques, Set<CraftingWard> wards, String nameBefore);

    String cannotUseNoticeKey();

    List<StyledText> detailLines(Item item);

    OpenCheck openCheck(SectorAPI sector, ItemStore store, Item loaded);

    int openEntries();

    OpenOutcome open(SectorAPI sector, ItemStore store, Item loaded, int entries, Random random);
}
