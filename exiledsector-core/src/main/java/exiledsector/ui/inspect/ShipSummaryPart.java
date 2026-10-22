package exiledsector.ui.inspect;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledlib.i18n.StyledText;
import exiledsector.items.Item;
import exiledlib.skills.EffectContribution;
import exiledsector.skills.ShipSkillData;

import java.util.List;

public interface ShipSummaryPart {

    default List<EffectContribution> contributions(ShipSkillData shipData, ShipVariantAPI variant, HullSize hullSize) {
        return List.of();
    }

    default StyledText line(ShipSkillData shipData, ShipVariantAPI variant) {
        return null;
    }

    default List<Item> installedItems(ShipSkillData shipData, ShipVariantAPI variant) {
        return List.of();
    }
}
