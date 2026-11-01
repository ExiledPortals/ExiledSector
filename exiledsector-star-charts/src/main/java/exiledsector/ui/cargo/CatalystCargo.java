package exiledsector.ui.cargo;

import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.i18n.Translation;
import exiledsector.items.AtlasCatalystItems;
import exiledsector.items.MaterialItems;
import exiledsector.ui.VanillaText;

public final class CatalystCargo {

    private static final float CATEGORY_PAD = 2f;

    private CatalystCargo() {
    }

    public static CargoIconLook look(AtlasCatalysts.Family family) {
        return CargoIconLook.withArt(AtlasCatalystItems.color(family), new CatalystLook(family));
    }

    public static void writeTooltip(TooltipMakerAPI tooltip, String itemId, float pad) {
        AtlasCatalysts.Catalyst catalyst = AtlasCatalysts.byItemId(itemId);
        tooltip.addTitle(MaterialItems.displayName(itemId), AtlasCatalystItems.color(catalyst == null ? null : catalyst.family()));
        VanillaText.plainPara(tooltip, Translation.text("catalyst.category"), CATEGORY_PAD, Misc.getGrayColor());
        if (catalyst != null) {
            VanillaText.plainPara(tooltip, AtlasCatalystItems.addsLine(catalyst), pad, Misc.getTextColor());
        }
        SpecialItemSpecAPI spec = MaterialItems.spec(itemId);
        String flavour = spec == null ? null : spec.getDesc();
        if (flavour != null && !flavour.isEmpty()) {
            VanillaText.plainPara(tooltip, flavour, pad, Misc.getGrayColor());
        }
    }
}
