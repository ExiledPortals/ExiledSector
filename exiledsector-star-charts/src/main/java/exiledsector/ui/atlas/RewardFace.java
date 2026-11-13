package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardPool;
import exiledsector.charts.officer.reward.RewardGroup;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemUnlock;
import exiledsector.items.MaterialItems;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

record RewardFace(List<String> iconPaths, Color frameColour) {

    static final Color LOW_CRAFTING_FRAME = new Color(170, 170, 170);
    static final Color MID_CRAFTING_FRAME = new Color(185, 125, 255);
    static final Color ENGRAM_FRAME = new Color(230, 80, 175);
    static final Color STAR_CHART_FRAME = new Color(225, 240, 255);
    static final Color BIOME_HULL_ITEM_FRAME = new Color(110, 235, 140);

    static RewardFace of(RewardGroup group) {
        List<String> iconPaths = OfficerRewardPool.possible(group, definition -> ItemUnlock.available(definition.type(), Global.getSector()))
                .stream().map(RewardFace::iconPath).filter(Objects::nonNull).distinct().toList();
        return new RewardFace(iconPaths, frameColour(group));
    }

    static Color frameColour(RewardGroup group) {
        return switch (group) {
            case LOW_CRAFTING -> LOW_CRAFTING_FRAME;
            case MID_CRAFTING -> MID_CRAFTING_FRAME;
            case HIGH_CRAFTING -> ItemRarity.RARE.color();
            case INHIBITOR -> CraftingInhibitor.PREFIX.color();
            case ENGRAM -> ENGRAM_FRAME;
            case FEEDSTOCK -> MaterialItems.color(ItemDisassembly.FEEDSTOCK_ITEM_ID);
            case STAR_CHART -> STAR_CHART_FRAME;
            case HULL_ITEM -> ItemRarity.COMMON.color();
            case BIOME_HULL_ITEM -> BIOME_HULL_ITEM_FRAME;
            case UNIQUE -> ItemRarity.UNIQUE.color();
        };
    }

    private static String iconPath(CarriedItem item) {
        if (item.kind().craftingItem()) {
            return MaterialItems.loadedIconPath(item.id());
        }
        ItemDefinition definition = ItemDefinitions.get(item.id());
        return definition == null ? null : definition.icon();
    }
}
