package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.OfficerRewardDrop;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.i18n.StyledText;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.MaterialItems;

import java.awt.Color;
import java.util.List;
import java.util.Random;

record RewardFace(Item preview, String iconPath, Color frameColour) {

    static final Color MATERIAL_FRAME = new Color(170, 170, 170);
    private static final long PREVIEW_SEED = 0L;

    static RewardFace of(CarriedItem offer) {
        if (offer.kind().craftingItem()) {
            Color materialColour = MaterialItems.color(offer.id());
            return new RewardFace(null, MaterialItems.loadedIconPath(offer.id()), materialColour == null ? MATERIAL_FRAME : materialColour);
        }
        Item preview = preview(offer);
        return new RewardFace(preview, preview == null ? null : preview.iconPath(), preview == null ? MATERIAL_FRAME : preview.frameColor());
    }

    static StyledText description(CarriedItem offer) {
        if (offer.kind().craftingItem()) {
            SpecialItemSpecAPI spec = MaterialItems.spec(offer.id());
            String description = spec == null ? null : spec.getDesc();
            return description == null || description.isEmpty() ? null : StyledText.of(description);
        }
        ItemDefinition definition = ItemDefinitions.get(offer.id());
        return definition == null ? null : definition.descriptionText();
    }

    private static Item preview(CarriedItem offer) {
        ItemDefinition definition = ItemDefinitions.get(offer.id());
        if (definition == null) {
            return null;
        }
        if (offer.kind() == RewardKind.UNIQUE) {
            return ItemData.rolled(definition, PREVIEW_SEED).preview();
        }
        ItemData base = new ItemData(definition.id(), PREVIEW_SEED, List.of(), null);
        if (offer.kind() == RewardKind.BIOME_HULL_ITEM) {
            base = OfficerRewardDrop.withBiomeModifier(base, definition, offer.variant(), new Random(PREVIEW_SEED));
        }
        return base.preview();
    }
}
