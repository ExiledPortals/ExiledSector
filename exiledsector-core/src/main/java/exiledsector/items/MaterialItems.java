package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class MaterialItems {

    private static final SpriteCache SPRITES = new SpriteCache(MaterialItems.class);
    private static final Color PARTS_COLOR = new Color(200, 170, 130);

    private MaterialItems() {
    }

    public static List<String> ids() {
        List<String> itemIds = new ArrayList<>();
        itemIds.add(ItemDisassembly.PARTS_ITEM_ID);
        for (ItemCurrency currency : ItemCurrencies.all()) {
            itemIds.add(currency.itemId());
        }
        for (CraftingWard ward : CraftingWard.values()) {
            itemIds.add(ward.itemId());
        }
        return itemIds;
    }

    public static boolean isMaterial(String itemId) {
        return itemId != null && ids().contains(itemId);
    }

    public static MaterialTier tier(String itemId) {
        ItemCurrency currency = ItemCurrencies.byItemId(itemId);
        if (currency != null) {
            return currency.tier();
        }
        CraftingWard ward = CraftingWard.byItemId(itemId);
        return ward == null ? MaterialTier.BASIC : ward.tier();
    }

    public static Color color(String itemId) {
        ItemCurrency currency = ItemCurrencies.byItemId(itemId);
        if (currency != null) {
            return currency.color();
        }
        CraftingWard ward = CraftingWard.byItemId(itemId);
        return ward == null ? PARTS_COLOR : ward.color();
    }

    public static SpecialItemData data(String itemId) {
        return new SpecialItemData(itemId, null);
    }

    public static int count(CargoAPI cargo, String itemId) {
        return cargo == null ? 0 : Math.round(cargo.getQuantity(CargoAPI.CargoItemType.SPECIAL, data(itemId)));
    }

    public static void add(CargoAPI cargo, String itemId, int quantity) {
        if (cargo != null && quantity > 0) {
            cargo.addSpecial(data(itemId), quantity);
        }
    }

    public static void remove(CargoAPI cargo, String itemId, int quantity) {
        if (cargo != null && quantity > 0) {
            cargo.removeItems(CargoAPI.CargoItemType.SPECIAL, data(itemId), quantity);
        }
    }

    public static SpecialItemSpecAPI spec(String itemId) {
        try {
            return Global.getSettings().getSpecialItemSpec(itemId);
        } catch (RuntimeException missingSpec) {
            return null;
        }
    }

    public static void localise(String itemId) {
        SpecialItemSpecAPI materialSpec = spec(itemId);
        if (materialSpec != null) {
            materialSpec.setName(Translation.data("material." + itemId + ".name", materialSpec.getName()));
            materialSpec.setDesc(Translation.data(descriptionKey(itemId), materialSpec.getDesc()));
        }
    }

    public static String descriptionKey(String itemId) {
        return "material." + itemId + ".description";
    }

    public static String displayName(String itemId) {
        SpecialItemSpecAPI spec = spec(itemId);
        return spec != null ? spec.getName() : itemId;
    }

    public static String iconPath(String itemId) {
        SpecialItemSpecAPI spec = spec(itemId);
        String params = spec == null ? null : spec.getParams();
        return params == null || params.isBlank() ? null : params.trim();
    }

    public static String loadedIconPath(String itemId) {
        String iconPath = iconPath(itemId);
        return iconPath != null && SPRITES.ensureLoaded(iconPath) ? iconPath : null;
    }
}
