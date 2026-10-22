package exiledsector.ui.cargo;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.HullUpgradeData;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.ItemRarity;
import exiledsector.items.MaterialItems;

import java.util.Set;

public enum LootAlert {

    STANDARD("exiledSector_loot_alert"),
    VALUABLE("exiledSector_loot_alert_valuable"),
    HULL_UPGRADE("exiledSector_loot_alert_hull_upgrade"),
    STAR_CHART("exiledSector_loot_alert_star_chart");

    private static final Set<String> VALUABLE_CURRENCY_IDS = Set.of(CraftingCurrency.RECALIBRATION.itemId(),
            CraftingCurrency.FIXATION.itemId(), CraftingCurrency.REPLICATION.itemId());

    private final String soundId;

    LootAlert(String soundId) {
        this.soundId = soundId;
    }

    public String soundId() {
        return soundId;
    }

    public static LootAlert of(CargoStackAPI stack) {
        SpecialItemData special = stack == null ? null : stack.getSpecialDataIfSpecial();
        return special == null ? null : of(special);
    }

    static LootAlert of(SpecialItemData special) {
        String itemId = special.getId();
        if (HullUpgradeData.of(special) != null) {
            return HULL_UPGRADE;
        }
        if (ItemData.STAR_CHART_ITEM_ID.equals(itemId)) {
            return STAR_CHART;
        }
        if (VALUABLE_CURRENCY_IDS.contains(itemId)) {
            return VALUABLE;
        }
        if (MaterialItems.isMaterial(itemId)) {
            return ItemDisassembly.PARTS_ITEM_ID.equals(itemId) ? null : STANDARD;
        }
        ItemData itemData = ItemData.of(special);
        Item preview = itemData == null ? null : itemData.preview();
        return preview == null ? null : of(preview.rarity());
    }

    static LootAlert of(ItemRarity rarity) {
        return switch (rarity) {
            case RARE -> STANDARD;
            case UNIQUE -> VALUABLE;
            default -> null;
        };
    }
}
