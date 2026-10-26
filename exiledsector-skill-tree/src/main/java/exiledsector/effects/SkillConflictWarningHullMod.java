package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Message;
import exiledlib.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.skills.HullModNames;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.ui.VanillaText;

import java.awt.Color;

public class SkillConflictWarningHullMod extends BaseHullMod {

    public static final String ID = "exiledSector_conflictWarning";

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship == null) return;
        SkillConflictWarnings.Removal removal = SkillConflictWarnings.get(ship.getVariant());
        if (removal == null) return;

        String removedName = HullModNames.displayName(removal.removedHullModId());
        Color highlightColor = Global.getSettings().getColor("hColor");
        I18n.forGameText(() -> {
            tooltip.addSectionHeading(Translation.text("hullmod.conflict.title"), Alignment.MID, 15);
            String causeItemId = removal.causeItemId();
            Message message = causeItemId == null
                    ? Translation.msg("hullmod.conflict.text").arg("cause", causeName(removal.causeTypeId()))
                    : Translation.msg("hullmod.conflict.itemText").arg("cause", itemName(causeItemId));
            VanillaText.addPara(tooltip, message.arg("removed", removedName).styled(), 10, Misc.getTextColor(), style -> highlightColor);
        });
    }

    private static String itemName(String itemId) {
        Item item = ItemStore.lookup(itemId);
        return item == null ? itemId : item.name();
    }

    private static String causeName(String causeTypeId) {
        SkillType causeType = SkillTree.getType(causeTypeId);
        return causeType == null ? causeTypeId : causeType.getDisplayName();
    }
}
