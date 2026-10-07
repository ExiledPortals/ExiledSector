package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.skills.HullModNames;
import exiledsector.ui.VanillaText;

import java.awt.Color;

public class SkillConflictWarningHullMod extends BaseHullMod {

    public static final String ID = "exiledSector_conflictWarning";

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        SkillConflictWarnings.Removal removal = SkillConflictWarnings.get(ship.getVariant());
        if (removal == null) return;

        String removedName = HullModNames.displayName(removal.removedHullModId);
        Color highlightColor = Global.getSettings().getColor("hColor");
        I18n.forGameText(() -> {
            tooltip.addSectionHeading(Translation.text("hullmod.conflict.title"), Alignment.MID, 15);
            VanillaText.addPara(tooltip, Translation.msg("hullmod.conflict.text").arg("removed", removedName)
                    .arg("cause", removal.causeSkillDisplayName).styled(), 10, Misc.getTextColor(), style -> highlightColor);
        });
    }
}
